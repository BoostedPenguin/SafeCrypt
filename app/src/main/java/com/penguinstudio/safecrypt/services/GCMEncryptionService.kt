package com.penguinstudio.safecrypt.services

import OptimizedCipherInputStream
import android.content.Context
import android.net.Uri
import android.util.Log
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaType
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.*
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class GCMEncryptionService @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        const val ENC_FILE_EXTENSION = "enc"


        fun generateUuidKey(context: Context) {
            val keyStr = java.util.UUID.randomUUID().toString().substring(0, 32)

            val sharedPref = context.getSharedPreferences(context.getString(R.string.main_shared_pref), Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(context.getString(R.string.ENCRYPT_KEY), keyStr)
                apply()
            }
        }

        fun putUuidKey(key: String, context: Context) {
            val sharedPref = context.getSharedPreferences(context.getString(R.string.main_shared_pref), Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(context.getString(R.string.ENCRYPT_KEY), key)
                apply()
            }
        }
    }




    fun getUuidKey() : Key {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.main_shared_pref), Context.MODE_PRIVATE)

        val keyStr =
            sharedPref.getString(context.getString(R.string.ENCRYPT_KEY), "")

        if(keyStr == null) {
            val newKeyStr = java.util.UUID.randomUUID().toString().substring(0, 32)

            with (sharedPref.edit()) {
                putString(context.getString(R.string.ENCRYPT_KEY), newKeyStr)
                apply()
            }


            return SecretKeySpec(newKeyStr.toByteArray(), "AES")
        }

        return SecretKeySpec(keyStr.toByteArray(), "AES")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ImageCompressorServicePoint {
        fun getService(): ImageCompressor
    }



    fun getCipherInputStream(uri: Uri): InputStream {
        val inputStream = context.contentResolver?.openInputStream(uri)

        val iv = ByteArray(GCM_IV_LENGTH)

        inputStream?.read(iv, 0, iv.size)

        val decryptCipher = Cipher.getInstance(AES_MODE)

        decryptCipher.init(Cipher.DECRYPT_MODE,
            getUuidKey(), GCMParameterSpec(128, iv))

//        return CipherInputStream(inputStream, decryptCipher)

        return OptimizedCipherInputStream(inputStream!!, decryptCipher)
    }

    fun decryptFileToByteArray(uri: Uri): ByteArray {
        // Configure cipher crypt

        val inputStream = context.contentResolver.openInputStream(uri)!!

        val decryptCipher = Cipher.getInstance(AES_MODE)

        val iv = ByteArray(12)

        // Read the IV and use it for init
        inputStream.read(iv, 0, iv.size)

        val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

        decryptCipher.init(Cipher.DECRYPT_MODE, getUuidKey(), gcmIv)

//        val endArray = decryptCipher.doFinal(inputStream.readBytes())

        val buffer = ByteArray(8192)
        val outputStream = ByteArrayOutputStream()

        var read: Int

        val cis = CipherInputStream(inputStream, decryptCipher)

        while (cis.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        cis.close()

        val endArray = outputStream.toByteArray()

        return endArray
    }

    fun decryptData (
        uri: Uri,
        outputStream: OutputStream) : Boolean {
        val startTime = System.currentTimeMillis()

        // Doesn't support videos yet

        val buffer = ByteArray(8192)

        val iv = ByteArray(GCM_IV_LENGTH)

        val inputStream = context.contentResolver.openInputStream(uri)!!

        val decryptCipher = Cipher.getInstance(AES_MODE)

        // Read the IV and use it for init
        inputStream.read(iv, 0, iv.size)

        val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

        decryptCipher.init(Cipher.DECRYPT_MODE, getUuidKey(), gcmIv)

        var read: Int

        val cis = OptimizedCipherInputStream(inputStream, decryptCipher)

//        val cis = DataInputStream(CipherInputStream(inputStream, decryptCipher))
//        cis.buffered(8192)


        while (cis.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        cis.close()
        outputStream.flush()
        outputStream.close()
        Log.d("loadTime", "Time it took to decrypt bytes: ${System.currentTimeMillis() - startTime}ms")

        return true
    }

    /**
    * Do ##NOT Encrypt videos. At the moment is supports only images
    */
    fun encryptData (
        uri: Uri,
        mediaType: MediaType,
        outputStream: OutputStream) : Boolean {

        val imageCompressor = EntryPoints.get(context, ImageCompressorServicePoint::class.java).getService()

        // Read/Write buffer
        val buffer = ByteArray(8192)

        // Configure cipher crypt
        val iv = ByteArray(GCM_IV_LENGTH) //NEVER REUSE THIS IV WITH SAME KEY

        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)
        val encryptCipher = Cipher.getInstance(AES_MODE)
        encryptCipher.init(Cipher.ENCRYPT_MODE, getUuidKey(), parameterSpec)


        val inpStream = when(mediaType) {
            MediaType.IMAGE -> {

                // Compression image before encrypting it
//                val compressed = imageCompressor.getBitmapFormUri(uri)
//                    ?: throw IllegalArgumentException("Couldn't open an input stream")
//
//                val degree = imageCompressor.getBitmapDegree(uri)
//                imageCompressor.rotateBitmapByDegree(compressed, degree.toFloat())

                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Couldn't open an input stream")
            }
            MediaType.VIDEO -> {
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Couldn't open an input stream")
            }
        }



        // Prepend IV
        outputStream.write(iv)
        var nread: Int


        while (inpStream.read(buffer).also { nread = it } > 0) {
            val enc: ByteArray = encryptCipher.update(buffer, 0, nread)
            outputStream.write(enc)
        }
        val enc: ByteArray = encryptCipher.doFinal()
        outputStream.write(enc)
        inpStream.close()

        return true
    }
}