package me.proxer.app.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.rubengees.rxbus.RxBus
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.safeInject
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseFragment(
    @LayoutRes contentLayoutId: Int,
) : Fragment(contentLayoutId),
    CustomTabsAware {
    val bus by safeInject<RxBus>()
    val storageHelper by safeInject<StorageHelper>()
    val preferenceHelper by safeInject<PreferenceHelper>()

    open val hostingActivity: BaseActivity
        get() = requireActivity() as BaseActivity

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean =
        customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())

    override fun showPage(
        url: HttpUrl,
        forceBrowser: Boolean,
        skipCheck: Boolean,
    ) {
        customTabsHelper.fallbackHandleLink(requireActivity(), url, forceBrowser, skipCheck)
    }
}
