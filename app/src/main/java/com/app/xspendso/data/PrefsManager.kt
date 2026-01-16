package com.app.xspendso.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("xspendso_prefs", Context.MODE_PRIVATE)

    var lastSmsSyncTimestamp: Long
        get() = prefs.getLong("last_sms_sync", 0L)
        set(value) = prefs.edit().putLong("last_sms_sync", value).apply()

    var lastEmailSyncTimestamp: Long
        get() = prefs.getLong("last_email_sync", 0L)
        set(value) = prefs.edit().putLong("last_email_sync", value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", true)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var syncFrequencyHours: Int
        get() = prefs.getInt("sync_frequency", 3)
        set(value) = prefs.edit().putInt("sync_frequency", value).apply()
}
