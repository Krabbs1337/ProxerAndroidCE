package me.proxer.app.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.sizeDp
import me.proxer.app.R
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.settings.theme.Theme
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.safeInject

class IntroductionSlideFragment : Fragment() {
    enum class Icon { LOGO, NOTIFICATIONS, THEME }

    enum class SwitchMode { NONE, NOTIFICATIONS, THEME }

    private val preferenceHelper by safeInject<PreferenceHelper>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_introduction_slide, container, false)
        val arguments = requireArguments()
        val context = requireContext()

        view.findViewById<TextView>(R.id.title).setText(arguments.getInt(ARG_TITLE))
        view.findViewById<TextView>(R.id.description).setText(arguments.getInt(ARG_DESCRIPTION))

        view.findViewById<ImageView>(R.id.image).setImageDrawable(
            when (Icon.valueOf(arguments.getString(ARG_ICON)!!)) {
                Icon.LOGO -> {
                    AppCompatResources.getDrawable(context, R.drawable.ic_proxer)
                }

                Icon.NOTIFICATIONS -> {
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_bell_outline).apply {
                        colorRes = R.color.on_primary
                        sizeDp = 128
                    }
                }

                Icon.THEME -> {
                    IconicsDrawable(context, CommunityMaterial.Icon3.cmd_theme_light_dark).apply {
                        colorRes = R.color.on_primary
                        sizeDp = 128
                    }
                }
            },
        )

        val switch = view.findViewById<SwitchCompat>(R.id.switchOption)

        when (SwitchMode.valueOf(arguments.getString(ARG_SWITCH_MODE)!!)) {
            SwitchMode.NONE -> {
                switch.visibility = View.GONE
            }

            SwitchMode.NOTIFICATIONS -> {
                switch.isChecked = preferenceHelper.areNewsNotificationsEnabled

                switch.setOnCheckedChangeListener { _, checked ->
                    preferenceHelper.areNewsNotificationsEnabled = checked
                    preferenceHelper.areAccountNotificationsEnabled = checked

                    if (checked) {
                        NotificationWorker.enqueueIfPossible(delay = true)
                    }
                }
            }

            SwitchMode.THEME -> {
                switch.isChecked = preferenceHelper.themeContainer.variant == ThemeVariant.DARK

                switch.setOnCheckedChangeListener { _, checked ->
                    preferenceHelper.themeContainer =
                        ThemeContainer(
                            Theme.CLASSIC,
                            if (checked) ThemeVariant.DARK else ThemeVariant.LIGHT,
                        )
                }
            }
        }

        return view
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_ICON = "icon"
        private const val ARG_SWITCH_MODE = "switch_mode"

        fun newInstance(
            titleRes: Int,
            descriptionRes: Int,
            icon: Icon,
            switchMode: SwitchMode,
        ) = IntroductionSlideFragment().apply {
            arguments =
                bundleOf(
                    ARG_TITLE to titleRes,
                    ARG_DESCRIPTION to descriptionRes,
                    ARG_ICON to icon.name,
                    ARG_SWITCH_MODE to switchMode.name,
                )
        }
    }
}
