package com.penguinstudio.safecrypt.models

import android.net.Uri
import com.penguinstudio.safecrypt.services.glide_service.IPicture

enum class MediaType {
    IMAGE, VIDEO
}
data class MediaModel(
    var id: Long,
    override val uri: Uri,
    var albumName: String?,
    override var mediaType: MediaType,
    var videoDuration: Long?,
    override var mediaName: String,

    override val size: String? = null,
    val details: MediaModelDetails,

    override var isSelected: Boolean = false,
) : IPicture

data class MediaModelDetails(
    val dateAdded: Long? = null,
    val relativePath: String? = null,
    val width: String? = null,
    val height: String? = null,
)