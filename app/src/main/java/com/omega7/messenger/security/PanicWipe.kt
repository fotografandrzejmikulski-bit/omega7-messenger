package com.omega7.messenger.security

import android.content.Context
import com.omega7.messenger.crypto.LocalKeyManager
import com.omega7.messenger.crypto.AuthKeyManager
import java.io.File

/** Usuwa lokalny stan Ω7. Nie wykonuje resetu całego urządzenia. */
object PanicWipe {
    fun execute(context: Context) {
        runCatching { LocalKeyManager().destroyKey() }
        runCatching { AuthKeyManager().destroyKey() }
        runCatching { DeviceKeyManager().destroy() }
        context.deleteDatabase("omega7.db")
        context.getSharedPreferences("omega7_auth", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("omega7_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("omega7_device_trust", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("omega7_identity", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("omega7_pairing", Context.MODE_PRIVATE).edit().clear().commit()
        deleteChildren(context.filesDir)
        deleteChildren(context.cacheDir)
        context.codeCacheDir?.let(::deleteChildren)
    }

    private fun deleteChildren(dir: File) {
        dir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
