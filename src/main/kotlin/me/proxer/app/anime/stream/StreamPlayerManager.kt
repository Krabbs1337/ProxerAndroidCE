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

/**
 * @author Ruben Gees
 */
class StreamPlayerManager(
    context: StreamActivity,
    rawClient: OkHttpClient,
    internal val adTag: Uri?,
) {
    private companion object {
        private const val WAS_PLAYING_EXTRA = "was_playing"
        private const val LAST_POSITION_EXTRA = "last_position"
    }

    internal val weakContext = WeakReference(context)

    internal val castSessionAvailabilityListener =
        object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                if (castPlayer != null) {
                    castPlayer.setMediaItem(castMediaItem, localPlayer.currentPosition)
                    castPlayer.prepare()

                    currentPlayer = castPlayer
                }
            }

            override fun onCastSessionUnavailable() {
                currentPlayer = localPlayer

                if (!isResumed) {
                    wasPlaying = false
                }
            }
        }

    private val eventListener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING, Player.STATE_IDLE -> {
                        playerStateSubject.onNext(PlayerState.LOADING)
                    }

                    Player.STATE_ENDED -> {
                        playerStateSubject.onNext(PlayerState.PAUSING)
                    }

                    Player.STATE_READY -> {
                        playerStateSubject.onNext(
                            when (currentPlayer.playWhenReady) {
                                true -> PlayerState.PLAYING
                                false -> PlayerState.PAUSING
                            },
                        )
                    }
                }
            }

            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                if (currentPlayer.playbackState == Player.STATE_READY) {
                    playerStateSubject.onNext(
                        when (playWhenReady) {
                            true -> PlayerState.PLAYING
                            false -> PlayerState.PAUSING
                        },
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                lastPosition = currentPlayer.currentPosition

                errorSubject.onNext(ErrorUtils.handle(error))
            }
        }

    private val lifecycleCallbacks =
        object : DefaultActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity == weakContext.get()) {
                    isResumed = true
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity == weakContext.get()) {
                    isFirstStart = false
                    isResumed = false
                }
            }

            @Suppress("DEPRECATION")
            override fun onActivityDestroyed(activity: Activity) {
                if (activity == weakContext.get()) {
                    activity.application.unregisterActivityLifecycleCallbacks(this)

                    localPlayer.release()
                    castPlayer?.release()

                    localPlayer.removeListener(eventListener)
                    castPlayer?.removeListener(eventListener)

                    castPlayer?.setSessionAvailabilityListener(null)

                    adsLoader?.release()
                    adsLoader = null
                }
            }
        }

    internal val client = buildClient(rawClient)

    internal val localPlayer = buildLocalPlayer(context)
    internal val castPlayer = buildCastPlayer(context)

    internal var adsLoader: ImaAdsLoader? =
        when {
            adTag != null -> {
                ImaAdsLoader.Builder(context).build().apply {
                    setPlayer(localPlayer)
                }
            }

            else -> {
                null
            }
        }

    internal var localMediaSource = buildLocalMediaSourceWithAds(client, uri)
    internal var castMediaItem = buildCastMediaItem(name, episode, coverUri, uri)

    internal val uri get() = requireNotNull(weakContext.get()?.uri)
    internal val name: String? get() = weakContext.get()?.name
    internal val episode: Int? get() = weakContext.get()?.episode
    internal val coverUri: Uri? get() = weakContext.get()?.coverUri
    internal val referer: String? get() = weakContext.get()?.referer

    internal var lastPosition: Long
        get() = weakContext.get()?.intent?.getLongExtra(LAST_POSITION_EXTRA, -1) ?: -1
        set(value) {
            weakContext.get()?.intent?.putExtra(LAST_POSITION_EXTRA, value)
        }

    internal var wasPlaying: Boolean
        get() = weakContext.get()?.intent?.getBooleanExtra(WAS_PLAYING_EXTRA, false) ?: false
        set(value) {
            weakContext.get()?.intent?.putExtra(WAS_PLAYING_EXTRA, value)
        }

    internal var isResumed = false
    internal var isFirstStart = true

    var currentPlayer by Delegates.observable<Player>(localPlayer) { _, old, new ->
        old.playWhenReady = false
        new.playWhenReady = isResumed

        new.seekTo(old.currentPosition)

        playerReadySubject.onNext(new)
    }
        private set

    val isPlayingAd: Boolean
        get() = localPlayer.isPlayingAd

    val playerReadySubject = BehaviorSubject.createDefault<Player>(localPlayer)
    val playerStateSubject = PublishSubject.create<PlayerState>()
    val errorSubject = PublishSubject.create<ErrorUtils.ErrorAction>()

    init {
        localPlayer.addListener(eventListener)
        castPlayer?.addListener(eventListener)

        localPlayer.setMediaSource(localMediaSource)
        localPlayer.prepare()

        context.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    enum class PlayerState {
        PLAYING,
        PAUSING,
        LOADING,
    }
}
