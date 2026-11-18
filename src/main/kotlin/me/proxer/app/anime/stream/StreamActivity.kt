package me.proxer.app.anime.stream

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.BaseActivity
import me.proxer.app.databinding.ActivityStreamBinding
import me.proxer.app.util.extension.newTask
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toast
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.AnimeLanguage
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import me.zhanghai.android.materialprogressbar.ThinCircularProgressDrawable

/**
 * @author Ruben Gees
 */
@Suppress("DEPRECATION") // TODO: Wait for androidx fullscreen support library.
class StreamActivity : BaseActivity() {
    private lateinit var binding: ActivityStreamBinding

    // Fullscreen player handles window insets itself.
    override val applyEdgeToEdgeInsets = false

    companion object {
        internal const val PREVIEW_MIME_TYPE = "video/mp4"
    }

    private val args
        get() = StreamActivityIntent(intent)

    internal val id: String
        get() = args.id

    internal val name: String
        get() = args.name

    internal val episode: Int
        get() = args.episode

    internal val language: AnimeLanguage
        get() = args.language

    internal val coverUri: Uri?
        get() = args.coverUri

    internal val referer: String?
        get() = args.referer

    internal val uri: Uri
        get() = args.uri

    internal val isProxerStream: Boolean
        get() = args.isProxerStream

    internal val mimeType: String
        get() = args.mimeType

    internal val isInternalPlayerOnly: Boolean
        get() = args.isInternalPlayerOnly

    private val adTag: Uri?
        get() = args.adTag

    private val client by safeInject<OkHttpClient>()
    internal val playerManager by unsafeLazy { StreamPlayerManager(this, client, adTag) }
    private val castHelper by unsafeLazy { StreamCastHelper(this, preferenceHelper) }

    internal fun getSafeCastContext() = castHelper.getSafeCastContext()

    internal val playerView: TouchablePlayerView get() = binding.player

    internal val toolbar: Toolbar get() = binding.toolbar

    internal val loading: ProgressBar get() = binding.loading
    internal val progress: PreviewTimeBar get() = findViewById(R.id.exo_progress)
    internal val rewindIndicator: TextView get() = binding.rewindIndicator
    internal val fastForwardIndicator: TextView get() = binding.fastForwardIndicator

    internal val play: ImageButton get() = findViewById(R.id.play)
    internal val fullscreen: ImageButton get() = findViewById(R.id.fullscreen)
    internal val controlIcon: ImageView get() = binding.controlIcon
    internal val controlProgress: MaterialProgressBar get() = binding.controlProgress
    internal val preview: ImageView get() = findViewById(R.id.preview)

    internal val hideIndicatorHandler = Handler(Looper.getMainLooper())
    internal val adFullscreenHandler = Handler(Looper.getMainLooper())
    internal val hideControlHandler = Handler(Looper.getMainLooper())
    internal val animationHandler = Handler(Looper.getMainLooper())

    internal val animationTime by unsafeLazy { resources.getInteger(android.R.integer.config_shortAnimTime).toLong() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        binding = ActivityStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        setupUi()

        fullscreen.setImageDrawable(generateControllerIcon(CommunityMaterial.Icon2.cmd_fullscreen))
        controlProgress.progressDrawable = ThinCircularProgressDrawable(this)
        controlProgress.showProgressBackground = false

        rewindIndicator.setCompoundDrawables(
            null,
            generateIndicatorIcon(CommunityMaterial.Icon3.cmd_rewind),
            null,
            null,
        )

        fastForwardIndicator.setCompoundDrawables(
            null,
            generateIndicatorIcon(CommunityMaterial.Icon2.cmd_fast_forward),
            null,
            null,
        )

        setupPlayerCallbacks()
        loadPreviewFrames()

        if (savedInstanceState == null) {
            toggleOrientation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_stream, menu, true)

        castHelper.setupMediaRouteButton(menu)

        if (isInternalPlayerOnly) {
            menu.findItem(R.id.action_open_in_other_app).isVisible = false
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val indicatorMargin = resources.getDimensionPixelSize(R.dimen.stream_indicator_margin)

        rewindIndicator.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginStart = indicatorMargin }
        fastForwardIndicator.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginEnd = indicatorMargin }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_open_in_other_app -> openInOtherApp()
            else -> super.onOptionsItemSelected(item)
        }

    override fun onStart() {
        super.onStart()

        castHelper.attach()

        playerManager.play(storageHelper.getLastAnimePosition(id, episode, language))
    }

    override fun onStop() {
        castHelper.detach()

        playerManager.pause()

        val lastPosition = playerManager.currentPlayer.currentPosition

        if (lastPosition > 0) {
            storageHelper.putLastAnimePosition(id, episode, language, lastPosition)
        }

        super.onStop()
    }

    override fun onDestroy() {
        castHelper.onDestroy()

        playerView.player = null

        hideIndicatorHandler.removeCallbacksAndMessages(null)
        adFullscreenHandler.removeCallbacksAndMessages(null)
        hideControlHandler.removeCallbacksAndMessages(null)
        animationHandler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.data != null && intent.data != this.intent.data) {
            this.intent = intent

            setupUi()

            playerManager.reset()
        }
    }

    override fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)

        handleUIChange()
    }

    private fun openInOtherApp(): Boolean =
        try {
            val intent =
                StreamResolutionResult
                    .Video(uri.toString().toHttpUrl(), mimeType, referer)
                    .makeIntent(this)
                    .newTask()

            startActivity(intent)
            finish()

            true
        } catch (ignored: ActivityNotFoundException) {
            toast(R.string.activity_stream_open_no_app)

            false
        }
}
