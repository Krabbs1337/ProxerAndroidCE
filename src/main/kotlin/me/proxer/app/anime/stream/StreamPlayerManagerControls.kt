package me.proxer.app.anime.stream

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.ads.AdsMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.google.android.gms.cast.MediaMetadata.KEY_EPISODE_NUMBER
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.util.DefaultActivityLifecycleCallbacks
import me.proxer.app.util.ErrorUtils
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

internal fun StreamPlayerManager.play(position: Long? = null) {
    if (isFirstStart && position != null) {
        lastPosition = position
    }

    if (currentPlayer.currentPosition <= 0 && lastPosition > 0) {
        currentPlayer.seekTo(lastPosition)
    }

    if (isFirstStart || wasPlaying) {
        currentPlayer.playWhenReady = true
    }
}

internal fun StreamPlayerManager.pause() {
    wasPlaying = currentPlayer.playWhenReady == true && currentPlayer.playbackState == Player.STATE_READY
    lastPosition = currentPlayer.currentPosition

    localPlayer.playWhenReady = false
}

internal fun StreamPlayerManager.toggle() {
    currentPlayer.playWhenReady = currentPlayer.playWhenReady.not()
}

internal fun StreamPlayerManager.retry() {
    if (currentPlayer == localPlayer) {
        localPlayer.setMediaSource(localMediaSource, false)
        localPlayer.prepare()
    } else if (currentPlayer == castPlayer) {
        castPlayer.setMediaItem(castMediaItem, lastPosition)
        castPlayer.prepare()
    }
}

internal fun StreamPlayerManager.reset() {
    currentPlayer.playWhenReady = false

    wasPlaying = false
    lastPosition = -1

    localMediaSource = buildLocalMediaSourceWithAds(client, uri)
    castMediaItem = buildCastMediaItem(name, episode, coverUri, uri)

    retry()

    currentPlayer.playWhenReady = true
}
