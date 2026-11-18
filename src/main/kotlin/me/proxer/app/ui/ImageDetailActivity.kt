package me.proxer.app.ui

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.bumptech.glide.request.target.ImageViewTarget
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.logErrors
import okhttp3.HttpUrl
import me.proxer.app.databinding.ActivityImageDetailBinding

/**
 * @author Ruben Gees
 */
class ImageDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityImageDetailBinding

    companion object {
        private const val URL_EXTRA = "url"

        fun navigateTo(
            context: Activity,
            url: HttpUrl,
            imageView: ImageView? = null,
        ) {
            context.intentFor<ImageDetailActivity>(URL_EXTRA to url.toString()).let {
                ActivityUtils.navigateToWithImageTransition(it, context, imageView)
            }
        }
    }

    override val theme: Int
        get() = preferenceHelper.themeContainer.theme.noBackground

    private val url: String
        get() = intent.getSafeStringExtra(URL_EXTRA)

    private val image: ImageView get() = binding.image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportPostponeEnterTransition()

        ViewCompat.setTransitionName(image, ActivityUtils.getTransitionName(this))

        GlideApp
            .with(this)
            .load(url)
            .logErrors()
            .into(
                object : ImageViewTarget<Drawable>(image) {
                    override fun setResource(resource: Drawable?) {
                        image.setImageDrawable(resource)

                        if (resource != null) {
                            supportStartPostponedEnterTransition()
                        }
                    }
                },
            )

        root
            .clicks()
            .autoDispose(this.scope())
            .subscribe { supportFinishAfterTransition() }
    }
}
