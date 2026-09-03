package com.omega7.messenger.data

import android.content.Context
import com.omega7.messenger.crypto.LocalCipher
import java.io.File
import java.io.FileOutputStream

/** Encrypted app-private store with bounded writes and best-effort durable replacement. */
class EncryptedLocalStore(
    context: Context,
    private val cipher: LocalCipher = LocalCipher(),
    fileName: String = "omega7_messages.bin"
) {
    private val file = File(context.filesDir, fileName)
    private val temp = File(context.filesDir, "$fileName.tmp")

    @Synchronized
    fun save(data: ByteArray) {
        val encrypted = cipher.encrypt(data)
        try {
            FileOutputStream(temp).use { output ->
                output.write(encrypted)
                output.fd.sync()
            }
            if (!temp.renameTo(file)) {
                if (!file.delete() || !temp.renameTo(file)) {
                    throw IllegalStateException("Nie można bezpiecznie zastąpić magazynu lokalnego.")
                }
            }
        } finally {
            encrypted.fill(0)
            temp.delete()
        }
    }

    @Synchronized
    fun load(): ByteArray? = if (file.exists()) {
        runCatching { cipher.decrypt(file.readBytes()) }.getOrNull()
    } else null

    @Synchronized
    fun delete() {
        secureDelete(file)
        secureDelete(temp)
    }

    private fun secureDelete(target: File) {
        if (!target.exists()) return
        runCatching { RandomAccessFileCompat.overwrite(target) }
        target.delete()
    }

    private object RandomAccessFileCompat {
        fun overwrite(target: File) {
            val length = target.length()
            if (length <= 0) return
            FileOutputStream(target, false).use { out ->
                val block = ByteArray(4096)
                var remaining = length
                while (remaining > 0) {
                    val n = minOf(remaining, block.size.toLong()).toInt()
                    out.write(block, 0, n)
                    remaining -= n
                }
                out.fd.sync()
            }
        }
    }
}
