package com.omega7.messenger.security

import android.content.Context

/** User-visible security preferences kept separately from authentication material. */
class SecuritySettings(context: Context) {
    private val prefs = context.getSharedPreferences("omega7_settings", Context.MODE_PRIVATE)

    var lockOnBackground: Boolean
        get() = prefs.getBoolean("lock_on_background", true)
        set(value) { prefs.edit().putBoolean("lock_on_background", value).apply() }

    var hideFromRecents: Boolean
        get() = prefs.getBoolean("hide_from_recents", true)
        set(value) { prefs.edit().putBoolean("hide_from_recents", value).apply() }
}
