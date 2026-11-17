package me.proxer.app.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi

/**
 * @author Ruben Gees
 */
class LoginHandler(
    private val api: ProxerApi,
    private val storageHelper: StorageHelper,
    private val preferenceHelper: PreferenceHelper,
) {
    @SuppressLint("CheckResult")
    fun listen(context: Context) {
        storageHelper.isLoggedInObservable
            .subscribe { isLoggedIn ->
                if (isLoggedIn) {
                    onLogin()
                } else {
                    onLogout(context)
                }
            }
    }

    private fun onLogin() {
        NotificationWorker.enqueueIfPossible()
    }

    private fun onLogout(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)

        AccountNotifications.cancel(context)

        if (!preferenceHelper.areNewsNotificationsEnabled) {
            NotificationWorker.cancel()
        }

        Completable
            .fromAction {
                storageHelper.reset()
            }.subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
