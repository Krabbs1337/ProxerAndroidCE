package me.proxer.app.anime.stream

import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.media3.ui.PlayerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.systemUiVisibilityChanges
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import me.proxer.app.R
import me.proxer.app.anime.stream.StreamPlayerManager.PlayerState
import me.proxer.app.util.extension.loadRequests
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.enums.Category
import me.proxer.app.util.extension.toEpisodeAppString

/**
 * View setup and player callback wiring of [StreamActivity].
 *
 * @author Ruben Gees
 */
@Suppress("DEPRECATION") // TODO: Wait for androidx fullscreen support library.
internal fun StreamActivity.setupUi() {
    title = name
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.subtitle = Category.ANIME.toEpisodeAppString(this, episode)

    coverUri?.also {
        Glide
            .with(playerView)
            .load(coverUri)
            .logErrors()
            .into(
                object : CustomViewTarget<PlayerView, Drawable>(playerView) {
                    override fun onLoadFailed(errorDrawable: Drawable?) = Unit

                    override fun onResourceCleared(placeholder: Drawable?) {
                        playerView.defaultArtwork = null
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?,
                    ) {
                        playerView.defaultArtwork = resource
                    }
                },
            )
    }

    playerView.setControllerVisibilityListener(
        PlayerView.ControllerVisibilityListener {
            toggleFullscreen(it == View.GONE)

            toolbar.isVisible = it == View.VISIBLE
        },
    )

    // Nobody understands fitsSystemWindows so this can probably be done better, but seems to work for now.
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = systemInsets.top - toolbar.paddingTop
            leftMargin = systemInsets.left - toolbar.paddingLeft
            rightMargin = systemInsets.right - toolbar.paddingRight
            bottomMargin = systemInsets.bottom - toolbar.paddingBottom
        }

        insets
    }

    window.decorView
        .systemUiVisibilityChanges()
        .autoDispose(this.scope())
        .subscribe { handleUIChange() }

    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

    toggleFullscreen(true)
}

internal fun StreamActivity.setupPlayerCallbacks() {
    playerManager.playerReadySubject
        .autoDispose(this.scope())
        .subscribe {
            toggleStableControls(it is androidx.media3.cast.CastPlayer)

            playerView.player = it
        }

    playerManager.playerStateSubject
        .autoDispose(this.scope())
        .subscribe {
            handlePlayerState(it)
        }

    playerManager.errorSubject
        .autoDispose(this.scope())
        .subscribe {
            MaterialDialog(this)
                .message(it.message)
                .positiveButton(R.string.error_action_retry) { playerManager.retry() }
                .negativeButton(R.string.error_action_finish) { finish() }
                .onCancel { finish() }
                .show()
        }

    playerView.rewindSubject
        .autoDispose(this.scope())
        .subscribe {
            resetIndicator(fastForwardIndicator)
            updateIndicator(rewindIndicator)
        }

    playerView.fastForwardSubject
        .autoDispose(this.scope())
        .subscribe {
            resetIndicator(rewindIndicator)
            updateIndicator(fastForwardIndicator)
        }

    playerView.volumeChangeSubject
        .autoDispose(this.scope())
        .subscribe {
            val icon =
                when {
                    it <= 0 -> CommunityMaterial.Icon3.cmd_volume_mute
                    it <= 33 -> CommunityMaterial.Icon3.cmd_volume_low
                    it <= 66 -> CommunityMaterial.Icon3.cmd_volume_medium
                    else -> CommunityMaterial.Icon3.cmd_volume_high
                }

            updateControl(it, icon)
        }

    playerView.brightnessChangeSubject
        .autoDispose(this.scope())
        .subscribe {
            val icon =
                when {
                    it <= 33 -> CommunityMaterial.Icon.cmd_brightness_5
                    it <= 66 -> CommunityMaterial.Icon.cmd_brightness_6
                    else -> CommunityMaterial.Icon.cmd_brightness_7
                }

            updateControl(it, icon)
        }

    play
        .clicks()
        .autoDispose(this.scope())
        .subscribe { playerManager.toggle() }

    rewindIndicator
        .clicks()
        .autoDispose(this.scope())
        .subscribe {
            playerView.rewind()

            updateIndicator(rewindIndicator, animate = false)
        }

    fastForwardIndicator
        .clicks()
        .autoDispose(this.scope())
        .subscribe {
            playerView.fastForward()

            updateIndicator(fastForwardIndicator, animate = false)
        }

    fullscreen
        .clicks()
        .autoDispose(this.scope())
        .subscribe { toggleOrientation() }
}

internal fun StreamActivity.handlePlayerState(state: PlayerState) {
    when (state) {
        PlayerState.PLAYING -> {
            playerView.keepScreenOn = true
            play.contentDescription = getString(R.string.exoplayer_pause_description)
            play.setImageState(intArrayOf(R.attr.state_pause), true)

            loading.isVisible = false
            play.isVisible = true
        }

        PlayerState.PAUSING -> {
            playerView.keepScreenOn = false
            play.contentDescription = getString(R.string.exoplayer_play_description)
            play.setImageState(intArrayOf(-R.attr.state_pause), true)

            loading.isVisible = false
            play.isVisible = true
        }

        PlayerState.LOADING -> {
            playerView.keepScreenOn = true
            loading.isVisible = true
            play.isVisible = false
        }
    }
}

internal fun StreamActivity.loadPreviewFrames() {
    if (mimeType == StreamActivity.PREVIEW_MIME_TYPE) {
        PreviewLoader
            .loadFrames(
                progress.loadRequests(),
                { Size(preview.width, preview.height) },
                PreviewLoader.PreviewMetaData(uri, referer, isProxerStream),
            ).autoDispose(this.scope())
            .subscribeAndLogErrors { preview.setImageBitmap(it) }
    }
}
