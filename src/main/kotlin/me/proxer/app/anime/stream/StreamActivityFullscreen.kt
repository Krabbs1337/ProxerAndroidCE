package me.proxer.app.anime.stream

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import android.view.View.SYSTEM_UI_FLAG_VISIBLE
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial

/**
 * Fullscreen and system-ui handling of [StreamActivity].
 *
 * @author Ruben Gees
 */
@SuppressLint("SourceLockedOrientationActivity")
internal fun StreamActivity.toggleOrientation() {
    if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        fullscreen.setImageDrawable(generateControllerIcon(CommunityMaterial.Icon2.cmd_fullscreen))
    } else {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        fullscreen.setImageDrawable(generateControllerIcon(CommunityMaterial.Icon2.cmd_fullscreen_exit))
    }
}

@Suppress("DEPRECATION") // TODO: Wait for androidx fullscreen support library.
internal fun StreamActivity.handleUIChange() {
    val isInFullscreenMode = window.decorView.systemUiVisibility and SYSTEM_UI_FLAG_FULLSCREEN != 0

    if (playerManager.isPlayingAd.not()) {
        // If true, no flags for hiding system UI are set. Show the controls.
        if (isInFullscreenMode) {
            playerView.hideController()
            toolbar.isVisible = false
        } else {
            playerView.showController()
            toolbar.isVisible = true
        }
    } else {
        adFullscreenHandler.postDelayed(3_000) {
            toggleFullscreen(true)
        }
    }
}

@Suppress("DEPRECATION") // TODO: Wait for androidx fullscreen support library.
internal fun StreamActivity.toggleFullscreen(fullscreen: Boolean) {
    val isInMultiWindowMode = this.isInMultiWindowMode

    window.decorView.systemUiVisibility =
        when {
            fullscreen && !isInMultiWindowMode -> {
                SYSTEM_UI_FLAG_LOW_PROFILE or
                    SYSTEM_UI_FLAG_FULLSCREEN or
                    SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    SYSTEM_UI_FLAG_IMMERSIVE
            }

            else -> {
                SYSTEM_UI_FLAG_VISIBLE
            }
        }
}

internal fun StreamActivity.toggleStableControls(stable: Boolean) {
    if (stable) {
        playerView.controllerHideOnTouch = false
        playerView.controllerShowTimeoutMs = 0
        playerView.showController()
    } else {
        playerView.controllerHideOnTouch = true
        playerView.controllerShowTimeoutMs = 2_000
    }
}
