package me.proxer.app.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.backgroundColorInt
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.app.profile.about.ProfileAboutFragment
import me.proxer.app.profile.comment.ProfileCommentFragment
import me.proxer.app.profile.history.HistoryFragment
import me.proxer.app.profile.info.ProfileInfoFragment
import me.proxer.app.profile.media.ProfileMediaListFragment
import me.proxer.app.profile.topten.TopTenFragment
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class ProfileActivity : ImageTabsActivity() {
    companion object {
        private const val USER_ID_EXTRA = "user_id"
        private const val USERNAME_EXTRA = "username"
        private const val IMAGE_ID_EXTRA = "image_id"

        private const val ABOUT_SUB_SECTION = "about"
        private const val ANIME_SUB_SECTION = "anime"
        private const val MANGA_SUB_SECTION = "manga"
        private const val HISTORY_SUB_SECTION = "chronik"

        fun navigateTo(
            context: Activity,
            userId: String? = null,
            username: String? = null,
            image: String? = null,
            imageView: ImageView? = null,
        ) {
            if (userId.isNullOrBlank() && username.isNullOrBlank()) {
                return
            }

            context
                .intentFor<ProfileActivity>(
                    USER_ID_EXTRA to userId,
                    USERNAME_EXTRA to username,
                    IMAGE_ID_EXTRA to image,
                ).let { ActivityUtils.navigateToWithImageTransition(it, context, imageView) }
        }
    }

    var userId: String?
        get() =
            when (intent.hasExtra(USER_ID_EXTRA)) {
                true -> intent.getStringExtra(USER_ID_EXTRA)
                false -> intent.data?.pathSegments?.getOrNull(1)
            }
        set(value) {
            intent.putExtra(USER_ID_EXTRA, value)
        }

    var username: String?
        get() = intent.getStringExtra(USERNAME_EXTRA)
        set(value) {
            intent.putExtra(USERNAME_EXTRA, value)

            title = value
        }

    var image: String?
        get() = intent.getStringExtra(IMAGE_ID_EXTRA)
        set(value) {
            intent.putExtra(IMAGE_ID_EXTRA, value)

            loadImage()
        }

    override val sectionsPagerAdapter: FragmentStateAdapter by unsafeLazy { SectionsPagerAdapter() }
    override val sectionsTabCallback: TabLayoutMediator.TabConfigurationStrategy by unsafeLazy { SectionsTabCallback() }

    private val viewModel by viewModel<ProfileViewModel> { parametersOf(userId, username) }

    override val headerImageUrl: HttpUrl?
        get() =
            image.let { image ->
                if (image == null || image.isBlank()) null else ProxerUrls.userImage(image)
            }

    private val customItemToDisplay: Int
        get() =
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    when (intent.data?.pathSegments?.getOrNull(2)) {
                        ABOUT_SUB_SECTION -> 1
                        ANIME_SUB_SECTION -> 3
                        MANGA_SUB_SECTION -> 4
                        HISTORY_SUB_SECTION -> 5
                        else -> 0
                    }
                }

                else -> {
                    0
                }
            }

    @Suppress("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.data.observe(
            this,
            Observer { data ->
                data?.let { (userInfo) ->
                    userId = userInfo.id
                    username = userInfo.username
                    image = userInfo.image

                    if (viewPager.currentItem == 0) {
                        viewPager.currentItem = customItemToDisplay

                        sectionsPagerAdapter.notifyDataSetChanged()
                    }
                }
            },
        )
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = username
    }

    override fun loadEmptyImage() {
        // If the image is not null, it means the user has none set.
        // If it is null, it means we just have'nt loaded it yet.
        if (image != null) {
            headerImage.setImageDrawable(
                IconicsDrawable(headerImage.context, CommunityMaterial.Icon.cmd_account).apply {
                    backgroundColorInt = headerImage.context.resolveColor(R.attr.colorPrimaryLight)
                    colorInt = headerImage.context.resolveColor(R.attr.colorPrimary)

                    sizeDp = (DeviceUtils.getScreenWidth(headerImage.context) * 0.75).toInt()
                    paddingDp = 32
                },
            )
        }
    }

    private inner class SectionsPagerAdapter : FragmentStateAdapter(supportFragmentManager, lifecycle) {
        override fun getItemCount() = 7

        override fun createFragment(position: Int) =
            when (position) {
                0 -> ProfileInfoFragment.newInstance()
                1 -> ProfileAboutFragment.newInstance()
                2 -> TopTenFragment.newInstance()
                3 -> ProfileMediaListFragment.newInstance(Category.ANIME)
                4 -> ProfileMediaListFragment.newInstance(Category.MANGA)
                5 -> ProfileCommentFragment.newInstance()
                6 -> HistoryFragment.newInstance()
                else -> error("Unknown index passed: $position")
            }
    }

    private inner class SectionsTabCallback : TabLayoutMediator.TabConfigurationStrategy {
        override fun onConfigureTab(
            tab: TabLayout.Tab,
            position: Int,
        ) {
            tab.text =
                when (position) {
                    0 -> getString(R.string.section_profile_info)
                    1 -> getString(R.string.section_profile_about)
                    2 -> getString(R.string.section_top_ten)
                    3 -> getString(R.string.section_user_media_list_anime)
                    4 -> getString(R.string.section_user_media_list_manga)
                    5 -> getString(R.string.section_user_comments)
                    6 -> getString(R.string.section_user_history)
                    else -> error("Unknown index passed: $position")
                }
        }
    }
}
