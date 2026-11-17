package me.zhanghai.android.customtabshelper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.fragment.app.Fragment

/**
 * In-repo replacement for `me.zhanghai.android.customtabshelper:library` (which still depends on the
 * removed support library), implemented on top of `androidx.browser`.
 */
class CustomTabsHelperFragment private constructor(
    private val context: Context,
) {
    val session: CustomTabsSession? = null

    fun mayLaunchUrl(
        uri: Uri,
        extras: Bundle? = null,
        otherLikelyBundles: List<Bundle>? = null,
    ): Boolean = CustomTabsClient.getPackageName(context, null) != null

    companion object {
        @JvmStatic
        fun attachTo(activity: Activity): CustomTabsHelperFragment = CustomTabsHelperFragment(activity)

        @JvmStatic
        fun attachTo(fragment: Fragment): CustomTabsHelperFragment = CustomTabsHelperFragment(fragment.requireContext())

        @JvmStatic
        fun open(
            context: Context,
            customTabsIntent: CustomTabsIntent,
            uri: Uri,
            fallback: (Context, Uri) -> Unit,
        ) {
            if (CustomTabsClient.getPackageName(context, null) == null) {
                fallback(context, uri)

                return
            }

            try {
                customTabsIntent.launchUrl(context, uri)
            } catch (ignored: ActivityNotFoundException) {
                fallback(context, uri)
            } catch (ignored: SecurityException) {
                fallback(context, uri)
            }
        }
    }
}
