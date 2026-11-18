package me.proxer.app.anime.stream

import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.core.os.postDelayed
import androidx.media3.cast.CastPlayer
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.IntroductoryOverlay
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import me.proxer.app.R
import me.proxer.app.util.data.PreferenceHelper
import timber.log.Timber

/**
 * Chromecast integration of [StreamActivity] (media route button + introductory overlay).
 *
 * @author Ruben Gees
 */
internal class StreamCastHelper(
    private val activity: StreamActivity,
    private val preferenceHelper: PreferenceHelper,
) {
    var mediaRouteButton: MenuItem? = null
        private set

    private var introductoryOverlay: IntroductoryOverlay? = null

    val castStateListener =
        CastStateListener { newState ->
            if (newState != CastState.NO_DEVICES_AVAILABLE && !preferenceHelper.wasCastIntroductoryOverlayShown) {
                showIntroductoryOverlay()
            }
        }

    fun setupMediaRouteButton(menu: Menu) {
        if (activity.isProxerStream) {
            mediaRouteButton = CastButtonFactory.setUpMediaRouteButton(activity, menu, R.id.action_cast)
        }
    }

    fun attach() {
        getSafeCastContext()?.addCastStateListener(castStateListener)
    }

    fun detach() {
        getSafeCastContext()?.removeCastStateListener(castStateListener)
    }

    fun onDestroy() {
        introductoryOverlay?.remove()
        introductoryOverlay = null
    }

    @Suppress("SwallowedException")
    fun getSafeCastContext(): CastContext? {
        val availabilityResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)

        return if (availabilityResult == ConnectionResult.SUCCESS) {
            try {
                CastContext.getSharedInstance(activity)
            } catch (error: Exception) {
                Timber.e(error)

                null
            }
        } else {
            null
        }
    }

    private fun showIntroductoryOverlay() {
        val safeIntroductoryOverlay = introductoryOverlay
        val safeMediaRouteButton = mediaRouteButton

        if (safeIntroductoryOverlay != null) {
            safeIntroductoryOverlay.remove()

            introductoryOverlay = null
        } else if (safeMediaRouteButton != null && safeMediaRouteButton.isVisible) {
            activity.toggleStableControls(true)

            Handler(Looper.getMainLooper()).postDelayed(50) {
                preferenceHelper.wasCastIntroductoryOverlayShown = true

                introductoryOverlay =
                    IntroductoryOverlay
                        .Builder(activity, safeMediaRouteButton)
                        .setTitleText(R.string.activity_stream_cast_introduction)
                        .setOnOverlayDismissedListener {
                            activity.toggleStableControls(activity.playerManager.currentPlayer is CastPlayer)

                            introductoryOverlay = null
                        }.build()
                        .apply { show() }
            }
        }
    }
}
