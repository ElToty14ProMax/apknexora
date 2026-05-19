package com.nexora.app

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.security.MessageDigest

fun sha256OfReceiptUri(context: Context, uri: Uri): String {
    val digest = MessageDigest.getInstance("SHA-256")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Não foi possível abrir o comprovante." }
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun receiptUploadFromUri(context: Context, uri: Uri): ReceiptUpload {
    val bytes = context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Não foi possível abrir o comprovante." }
        input.readBytes()
    }
    require(bytes.isNotEmpty()) { "Comprovante vazio." }
    require(bytes.size <= 2_500_000) { "Foto do comprovante deve ter até 2,5 MB." }
    val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    val mime = context.contentResolver.getType(uri)?.lowercase()
        ?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp") }
        ?: "image/jpeg"
    return ReceiptUpload(
        hash = hash,
        imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
        mimeType = mime,
    )
}
