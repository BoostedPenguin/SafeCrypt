package com.penguinstudio.safecrypt.services

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class MediaFetchingService @Inject constructor(
    @ApplicationContext private val context: Context
    ) {
    suspend fun getAllVideosWithAlbums(): ArrayList<AlbumModel> {
        return withContext(Dispatchers.IO) {

            val allAlbums: ArrayList<AlbumModel> = ArrayList()
            val albumsNames: ArrayList<String> = ArrayList()


            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.RELATIVE_PATH
            )
            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val queryUri = MediaStore.Files.getContentUri("external")

            // Make the query.
            val query = context.contentResolver.query(
                queryUri,
                projection,  // Which columns to return
                selection,  // Which rows to return (all rows)
                null,  // Selection arguments (none)
                MediaStore.Files.FileColumns.DATE_TAKEN + " DESC" // Sort order.
            )

            query?.use {
                val bucketNameColumn: Int = it.getColumnIndex(
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
                )
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                val mediaNameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationColumn: Int =
                    it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

                val itemSizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val itemWidthColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val itemHeightColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val relativePathColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)


                while (it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)
                    val mediaName = it.getString(mediaNameColumn)
                    val dateAdded = it.getLongOrNull(dateAddedColumn)
                    val size = it.getStringOrNull(itemSizeColumn)
                    val width = it.getStringOrNull(itemWidthColumn)
                    val height = it.getStringOrNull(itemHeightColumn)
                    val relativePath = it.getStringOrNull(relativePathColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"),
                        imageId
                    )

                    var media: MediaModel

                    when(it.getInt(mediaTypeColumn)) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {

                            media = MediaModel(
                                imageId,
                                contentUri,
                                bucketName,
                                MediaType.IMAGE,
                                null,
                                mediaName,
                                MediaModelDetails(dateAdded, relativePath, size, width, height)
                                )
                        }
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                            media = MediaModel(
                                imageId,
                                contentUri,
                                bucketName,
                                MediaType.VIDEO,
                                it.getLong(durationColumn),
                                mediaName,
                                MediaModelDetails(dateAdded, relativePath, size)
                            )
                        }
                        else -> {
                            throw IllegalArgumentException("Something wrong happened.")
                        }
                    }

                    if (albumsNames.contains(bucketName)) {
                        for (album in allAlbums) {
                            if (album.name == bucketName) {
                                album.albumMedia.add(media)
                                break
                            }
                        }
                    } else {
                        val album = AlbumModel()

                        album.id = media.id
                        album.name = bucketName
                        album.coverUri = media.uri
                        album.albumMedia.add(media)

                        allAlbums.add(album)
                        albumsNames.add(bucketName)
                    }
                }
            }
            return@withContext allAlbums
        }
    }

    suspend fun getAllEncryptedMedia() : ArrayList<EncryptedModel> = withContext(Dispatchers.IO) {
        val sp: SharedPreferences = context.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
        val uriTree = sp.getString("uriTree", "")

        if (TextUtils.isEmpty(uriTree)) {
            return@withContext ArrayList()
        } else {
            val uri: Uri = Uri.parse(uriTree)

            // Root directory > where to look for encrypted files
            val root = DocumentFile.fromTreeUri(context, uri)

            val uris = arrayListOf<EncryptedModel>()

            root?.listFiles()?.forEach lit@ {

                // If it's not an encoded file continue
                if(it.name == null || it.name!!.indexOf(".${GCMEncryptionService.ENC_FILE_EXTENSION}") <= 0) return@lit

                uris.add(EncryptedModel(it.uri))
            }
            return@withContext uris
        }
    }
}