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

internal fun StreamPlayerManager.buildClient(rawClient: OkHttpClient): OkHttpClient =
    referer.let { referer ->
        if (referer == null) {
            rawClient
        } else {
            rawClient
                .newBuilder()
                .addInterceptor {
                    val requestWithReferer =
                        it
                            .request()
                            .newBuilder()
                            .header("Referer", referer)
                            .build()

                    it.proceed(requestWithReferer)
                }.build()
        }
    }

internal fun StreamPlayerManager.buildLocalMediaSourceWithAds(
    client: OkHttpClient,
    uri: Uri,
): MediaSource {
    val context = requireNotNull(weakContext.get())

    val okHttpDataSourceFactory = OkHttpDataSource.Factory(client).setUserAgent(USER_AGENT)
    val imaFactory = ImaMediaSourceFactory(okHttpDataSourceFactory, this::buildLocalMediaSource)
    val localMediaSource = buildLocalMediaSource(okHttpDataSourceFactory, uri)

    val safeAdsLoader = adsLoader

    return if (safeAdsLoader != null) {
        AdsMediaSource(
            localMediaSource,
            DataSpec(requireNotNull(adTag)),
            requireNotNull(adTag).toString(),
            imaFactory,
            safeAdsLoader,
            context.playerView,
        )
    } else {
        localMediaSource
    }
}

internal fun StreamPlayerManager.buildLocalMediaSource(
    dataSourceFactory: DataSource.Factory,
    uri: Uri,
): MediaSource {
    val mediaItem = MediaItem.fromUri(uri)

    return when (Util.inferContentType(uri)) {
        C.CONTENT_TYPE_SS -> {
            SsMediaSource
                .Factory(DefaultSsChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        C.CONTENT_TYPE_DASH -> {
            DashMediaSource
                .Factory(DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        C.CONTENT_TYPE_HLS -> {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        C.CONTENT_TYPE_OTHER -> {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        else -> {
            error("Unknown streamType for uri: $uri")
        }
    }
}

internal fun StreamPlayerManager.buildCastMediaItem(
    name: String?,
    episode: Int?,
    coverUri: Uri?,
    uri: Uri,
): MediaItem {
    val mediaMetadata =
        MediaMetadata
            .Builder()
            .apply {
                if (name != null) {
                    setTitle(name)
                }

                if (episode != null) {
                    setExtras(Bundle().apply { putInt(KEY_EPISODE_NUMBER, episode) })
                }

                if (coverUri != null) {
                    setArtworkUri(coverUri)
                }
            }.build()

    return MediaItem
        .Builder()
        .setUri(uri)
        .setMimeType(MimeTypes.VIDEO_MP4)
        .setMediaMetadata(mediaMetadata)
        .build()
}

internal fun StreamPlayerManager.buildLocalPlayer(context: StreamActivity): ExoPlayer =
    ExoPlayer.Builder(context).build().apply {
        val audioAttributes =
            AudioAttributes
                .Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

        setWakeMode(C.WAKE_MODE_NETWORK)
        setHandleAudioBecomingNoisy(true)
        setAudioAttributes(audioAttributes, true)
    }

@Suppress("DEPRECATION")
internal fun StreamPlayerManager.buildCastPlayer(context: StreamActivity): CastPlayer? =
    context
        .getSafeCastContext()
        ?.let { CastPlayer(it, DefaultMediaItemConverter()) }
        ?.apply { setSessionAvailabilityListener(castSessionAvailabilityListener) }
