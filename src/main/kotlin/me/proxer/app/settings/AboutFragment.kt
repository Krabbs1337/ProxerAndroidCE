package me.proxer.app.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.settings.status.ServerStatusActivity
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toPrefixedHttpUrl
import me.proxer.app.util.extension.toast
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class AboutFragment :
    PreferenceFragmentCompat(),
    CustomTabsAware {
    companion object {
        private val teamLink = "https://proxer.me/team?device=default".toPrefixedHttpUrl()
        private val facebookLink = "https://facebook.com/Anime.Proxer.Me".toPrefixedHttpUrl()
        private val twitterLink = "https://twitter.com/proxerme".toPrefixedHttpUrl()
        private val youtubeLink = "https://youtube.com/channel/UC7h-fT9Y9XFxuZ5GZpbcrtA".toPrefixedHttpUrl()
        private val discordLink = "https://discord.gg/XwrEDmA".toPrefixedHttpUrl()
        private val repositoryLink = "https://github.com/proxer/ProxerAndroid".toPrefixedHttpUrl()

        private const val SUPPORT_PROXER_MAIL = "appsupport@proxer.de"
        private const val DEVELOPER_GITHUB_NAME = "rubengees"

        fun newInstance() =
            AboutFragment().apply {
                arguments = bundleOf()
            }
    }

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)

        setLikelyUrl(teamLink)
        setLikelyUrl(facebookLink)
        setLikelyUrl(twitterLink)
        setLikelyUrl(youtubeLink)
        setLikelyUrl(discordLink)
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

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        val context = requireContext()
        val screen = preferenceManager.createPreferenceScreen(context)

        buildInfoItems(context).forEach { screen.addPreference(it) }

        addCategory(screen, context, R.string.about_social_media_title) { buildSocialMediaItems(context) }
        addCategory(screen, context, R.string.about_support_title) { buildSupportItems(context) }
        addCategory(screen, context, R.string.about_developer_title) { buildDeveloperItems(context) }

        preferenceScreen = screen
    }

    private fun addCategory(
        screen: PreferenceScreen,
        context: Context,
        titleRes: Int,
        items: () -> List<Preference>,
    ) {
        val category = PreferenceCategory(context).apply { title = getString(titleRes) }

        screen.addPreference(category)

        items().forEach { category.addPreference(it) }
    }

    private fun icon(
        context: Context,
        icon: IIcon,
    ) = IconicsDrawable(context, icon).apply {
        colorInt = context.resolveColor(R.attr.colorIcon)
    }

    private fun actionItem(
        context: Context,
        icon: IIcon,
        titleRes: Int? = null,
        subTextRes: Int? = null,
        subText: String? = null,
        onClick: () -> Unit,
    ) = Preference(context).apply {
        this.icon = this@AboutFragment.icon(context, icon)
        if (titleRes != null) {
            setTitle(titleRes)
        }

        when {
            subTextRes != null -> setSummary(subTextRes)
            subText != null -> summary = subText
        }

        setOnPreferenceClickListener {
            onClick()

            true
        }
    }

    @Suppress("DEPRECATION")
    private fun buildInfoItems(context: Context) =
        listOf(
            Preference(context).apply {
                icon =
                    context.packageManager.getApplicationIcon(context.packageName)

                title = context.packageManager.getApplicationLabel(context.applicationInfo)

                isSelectable = false
            },
            actionItem(
                context,
                CommunityMaterial.Icon3.cmd_tag,
                R.string.about_version_title,
                subText = BuildConfig.VERSION_NAME,
            ) {
                val title = getString(R.string.clipboard_title)

                requireContext().getSystemService<ClipboardManager>()?.setPrimaryClip(
                    ClipData.newPlainText(title, BuildConfig.VERSION_NAME),
                )

                requireContext().toast(R.string.clipboard_status)
            },
            actionItem(
                context,
                CommunityMaterial.Icon.cmd_clipboard_text,
                R.string.about_licenses_title,
                subTextRes = R.string.about_licenses_description,
            ) {
                requireActivity().startActivity(
                    Intent(requireContext(), ProxerLibsActivity::class.java)
                        .putExtra(LibsBuilder.BUNDLE_TITLE, getString(R.string.about_licenses_activity_title)),
                )
            },
            actionItem(
                context,
                CommunityMaterial.Icon.cmd_code_braces,
                R.string.about_source_code,
                subTextRes = R.string.about_source_code_description,
            ) {
                showPage(repositoryLink, skipCheck = true)
            },
            actionItem(
                context,
                CommunityMaterial.Icon3.cmd_server,
                R.string.about_server_status,
                subTextRes = R.string.about_server_status_description,
            ) {
                ServerStatusActivity.navigateTo(requireActivity())
            },
        )

    private fun buildSocialMediaItems(context: Context) =
        listOf(
            actionItem(
                context,
                CommunityMaterial.Icon2.cmd_facebook,
                R.string.about_facebook_title,
                subTextRes = R.string.about_facebook_description,
            ) {
                showPage(facebookLink, skipCheck = true)
            },
            actionItem(
                context,
                CommunityMaterial.Icon3.cmd_twitter,
                R.string.about_twitter_title,
                subTextRes = R.string.about_twitter_description,
            ) {
                showPage(twitterLink, skipCheck = true)
            },
            actionItem(
                context,
                CommunityMaterial.Icon3.cmd_youtube,
                R.string.about_youtube_title,
                subTextRes = R.string.about_youtube_description,
            ) {
                showPage(youtubeLink, skipCheck = true)
            },
            actionItem(
                context,
                CommunityMaterial.Icon.cmd_discord,
                R.string.about_discord_title,
                subTextRes = R.string.about_discord_description,
            ) {
                showPage(discordLink, skipCheck = true)
            },
        )

    private fun buildSupportItems(context: Context) =
        listOf(
            actionItem(
                context,
                CommunityMaterial.Icon2.cmd_information,
                subTextRes = R.string.about_support_info,
            ) {
                showPage(teamLink, skipCheck = true)
            },
            actionItem(
                context,
                CommunityMaterial.Icon.cmd_email,
                R.string.about_support_mail_title,
                subTextRes = R.string.about_support_mail_description,
            ) {
                val intent =
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()

                        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_PROXER_MAIL))
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_support_mail_subject))
                    }

                try {
                    startActivity(intent)
                } catch (error: ActivityNotFoundException) {
                    requireContext().toast(R.string.about_error_mail_no_activity)
                }
            },
        )

    private fun buildDeveloperItems(context: Context) =
        listOf(
            actionItem(
                context,
                CommunityMaterial.Icon2.cmd_github,
                R.string.about_developer_github_title,
                subText = DEVELOPER_GITHUB_NAME,
            ) {
                showPage(
                    "https://github.com/$DEVELOPER_GITHUB_NAME".toPrefixedHttpUrl(),
                    skipCheck = true,
                )
            },
        )
}
