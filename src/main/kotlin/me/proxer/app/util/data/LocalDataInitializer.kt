package me.proxer.app.util.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * @author Ruben Gees
 */
class LocalDataInitializer(
    private val context: Context,
    private val preferences: SharedPreferences,
) {
    private companion object {
        private const val VERSION = "version"

        private const val CURRENT_VERSION = 8
    }

    @Volatile
    private var isInitialized = false

    fun initAndMigrateIfNecessary() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    val previousVersion = preferences.getInt(VERSION, 0)

                    if (previousVersion <= 7) {
                        // Hawk was removed in v8. Its values were already migrated to the
                        // EncryptedSharedPreferences in app versions 1.11.x (schema v6/v7),
                        // so only the leftover files need to be deleted here.
                        deleteLegacyHawkFiles()
                    }

                    if (previousVersion != CURRENT_VERSION) {
                        preferences.edit(commit = true) { putInt(VERSION, CURRENT_VERSION) }
                    }

                    isInitialized = true
                }
            }
        }
    }

    private fun deleteLegacyHawkFiles() {
        context.deleteSharedPreferences("Hawk2")
        context.deleteSharedPreferences("crypto.KEY_256")
    }
}
