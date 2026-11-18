package me.proxer.app.util.wrapper

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.backgroundColorInt
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import io.reactivex.subjects.PublishSubject
import me.proxer.app.R
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.safeInject
import me.proxer.library.util.ProxerUrls

/**
 * Navigation drawer based on the Material [NavigationView]. Replaces the former
 * mikepenz materialdrawer integration, keeping the same public API.
 *
 * @author Ruben Gees
 */
class DrawerWrapper(
    private val activity: Activity,
    savedInstanceState: Bundle?,
    private val isMain: Boolean,
) {
    val itemClickSubject: PublishSubject<DrawerItem> = PublishSubject.create()
    val profileClickSubject: PublishSubject<ProfileItem> = PublishSubject.create()

    val currentItem: DrawerItem?
        get() = selectedItem

    private val storageHelper by safeInject<StorageHelper>()

    private val drawerLayout: DrawerLayout = activity.findViewById(R.id.root)
    private val navigationView: NavigationView = activity.findViewById(R.id.navigation_view)
    private val headerView: View = navigationView.getHeaderView(0)

    private var selectedItem: DrawerItem? = null

    init {
        navigationView.menu.apply {
            findItem(R.id.drawer_news)?.icon = icon(CommunityMaterial.Icon3.cmd_newspaper, R.attr.colorIcon)
            findItem(R.id.drawer_bookmarks)?.icon = icon(CommunityMaterial.Icon.cmd_bookmark, R.attr.colorIcon)
            findItem(R.id.drawer_anime)?.icon = icon(CommunityMaterial.Icon3.cmd_television, R.attr.colorIcon)
            findItem(R.id.drawer_schedule)?.icon = icon(CommunityMaterial.Icon.cmd_calendar, R.attr.colorIcon)
            findItem(R.id.drawer_manga)?.icon =
                icon(CommunityMaterial.Icon.cmd_book_open_page_variant, R.attr.colorIcon)
        }

        activity.findViewById<TextView>(R.id.drawer_item_info).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon(CommunityMaterial.Icon2.cmd_information_outline, R.attr.colorIcon),
                null,
                null,
                null,
            )

            setOnClickListener { onItemClick(DrawerItem.INFO) }
        }

        activity.findViewById<TextView>(R.id.drawer_item_settings).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon(CommunityMaterial.Icon.cmd_cog, R.attr.colorIcon),
                null,
                null,
                null,
            )

            setOnClickListener { onItemClick(DrawerItem.SETTINGS) }
        }

        headerView.findViewById<View>(R.id.profile_container).setOnClickListener {
            profileClickSubject.onNext(
                if (storageHelper.user != null) ProfileItem.USER else ProfileItem.GUEST,
            )
        }

        headerView.findViewById<TextView>(R.id.profile_action_login).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon(CommunityMaterial.Icon.cmd_account_key, R.attr.colorOnPrimary),
                null,
                null,
                null,
            )

            setOnClickListener { profileClickSubject.onNext(ProfileItem.LOGIN) }
        }

        headerView.findViewById<TextView>(R.id.profile_action_notifications).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon(CommunityMaterial.Icon.cmd_bell_outline, R.attr.colorOnPrimary),
                null,
                null,
                null,
            )

            setOnClickListener { profileClickSubject.onNext(ProfileItem.NOTIFICATIONS) }
        }

        headerView.findViewById<TextView>(R.id.profile_action_settings).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon(CommunityMaterial.Icon.cmd_account_settings, R.attr.colorOnPrimary),
                null,
                null,
                null,
            )

            setOnClickListener { profileClickSubject.onNext(ProfileItem.PROFILE_SETTINGS) }
        }

        headerView.findViewById<TextView>(R.id.profile_action_logout).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon(CommunityMaterial.Icon.cmd_account_remove, R.attr.colorOnPrimary),
                null,
                null,
                null,
            )

            setOnClickListener { profileClickSubject.onNext(ProfileItem.LOGOUT) }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            onItemClick(DrawerItem.fromMenuId(menuItem.itemId), menuItem)

            true
        }

        refreshHeader(activity)

        if (isMain) {
            selectedItem =
                savedInstanceState
                    ?.let { DrawerItem.fromIdOrNull(it.getLong(STATE_SELECTED_ITEM)) }
                    ?: DrawerItem.NEWS

            selectedItem?.let { setCheckedItem(it) }
        }
    }

    fun onBackPressed(): Boolean =
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)

                true
            }

            else -> {
                false
            }
        }

    fun saveInstanceState(outState: Bundle) {
        selectedItem?.let { outState.putLong(STATE_SELECTED_ITEM, it.id) }
    }

    fun select(
        item: DrawerItem,
        fireOnClick: Boolean = true,
    ) {
        if (isMain) {
            selectedItem = item

            setCheckedItem(item)
        }

        if (fireOnClick) {
            itemClickSubject.onNext(item)
        }
    }

    fun refreshHeader(context: Context) {
        val user = storageHelper.user

        headerView.findViewById<TextView>(R.id.profile_name).text =
            user?.name ?: context.getString(R.string.section_guest)

        headerView.findViewById<TextView>(R.id.profile_subtitle).text =
            when {
                user != null -> context.getString(R.string.section_user_subtitle)
                else -> context.getString(R.string.section_login)
            }

        val image = headerView.findViewById<ImageView>(R.id.profile_image)

        when {
            user != null && user.image.isNotBlank() -> {
                Glide
                    .with(context)
                    .load(ProxerUrls.userImage(user.image))
                    .centerCrop()
                    .placeholder(accountPlaceholder(context))
                    .into(image)
            }

            user != null -> {
                image.setImageDrawable(accountPlaceholder(context))
            }

            else -> {
                image.setImageResource(R.mipmap.ic_launcher)
            }
        }

        headerView.findViewById<View>(R.id.profile_action_login).isVisible = user == null
        headerView.findViewById<View>(R.id.profile_action_notifications).isVisible = user != null
        headerView.findViewById<View>(R.id.profile_action_settings).isVisible = user != null
        headerView.findViewById<View>(R.id.profile_action_logout).isVisible = user != null
    }

    private fun onItemClick(
        item: DrawerItem,
        menuItem: MenuItem? = null,
    ) {
        if (isMain) {
            selectedItem = item

            if (menuItem != null) {
                menuItem.isChecked = true
            } else {
                setCheckedItem(item)
            }
        }

        itemClickSubject.onNext(item)

        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setCheckedItem(item: DrawerItem) {
        val menuId = DrawerItem.toMenuId(item) ?: return

        navigationView.menu.findItem(menuId)?.isChecked = true
    }

    private fun icon(
        icon: IIcon,
        colorAttr: Int,
    ) = IconicsDrawable(activity, icon).apply {
        colorInt = activity.resolveColor(colorAttr)
        sizeDp = 24
    }

    private fun accountPlaceholder(context: Context) =
        IconicsDrawable(context, CommunityMaterial.Icon.cmd_account).apply {
            backgroundColorInt = context.resolveColor(R.attr.colorPrimary)
            colorInt = context.resolveColor(R.attr.colorOnPrimary)
            sizeDp = 48
        }

    enum class DrawerItem(
        val id: Long,
    ) {
        NEWS(0L),
        BOOKMARKS(2L),
        ANIME(3L),
        SCHEDULE(4L),
        MANGA(5L),
        INFO(10L),
        SETTINGS(11L),
        ;

        companion object {
            fun fromIdOrNull(id: Long?) = values().firstOrNull { it.id == id }

            fun fromIdOrDefault(id: Long?) = fromIdOrNull(id) ?: NEWS

            fun fromMenuId(menuId: Int) =
                when (menuId) {
                    R.id.drawer_bookmarks -> BOOKMARKS
                    R.id.drawer_anime -> ANIME
                    R.id.drawer_schedule -> SCHEDULE
                    R.id.drawer_manga -> MANGA
                    else -> NEWS
                }

            fun toMenuId(item: DrawerItem) =
                when (item) {
                    NEWS -> R.id.drawer_news
                    BOOKMARKS -> R.id.drawer_bookmarks
                    ANIME -> R.id.drawer_anime
                    SCHEDULE -> R.id.drawer_schedule
                    MANGA -> R.id.drawer_manga
                    INFO, SETTINGS -> null
                }
        }
    }

    enum class ProfileItem(
        val id: Long,
    ) {
        GUEST(100L),
        LOGIN(101L),
        USER(102L),
        LOGOUT(103L),
        NOTIFICATIONS(104L),
        PROFILE_SETTINGS(106L),
        ;

        companion object {
            fun fromIdOrNull(id: Long?) = values().firstOrNull { it.id == id }

            fun fromIdOrDefault(id: Long?) = fromIdOrNull(id) ?: USER
        }
    }

    private companion object {
        private const val STATE_SELECTED_ITEM = "drawer_selected_item"
    }
}
