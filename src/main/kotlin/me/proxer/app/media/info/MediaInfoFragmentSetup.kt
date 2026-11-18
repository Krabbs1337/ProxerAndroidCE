package me.proxer.app.media.info

import androidx.core.view.isNotEmpty
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.industry.IndustryActivity
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.ui.view.MaxLineFlexboxLayout
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toAppStringDescription
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEndAppString
import me.proxer.app.util.extension.toStartAppString
import me.proxer.app.util.extension.toTypeAppString
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo
import me.proxer.library.enums.IndustryType
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import me.proxer.app.databinding.FragmentMediaInfoBinding

internal fun MediaInfoFragment.setupView() {
    updateUnratedButton()
    updateSpoilerButton()
    bindUserInfo(null)

    noteContainer
        .clicks()
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { viewModel.note() }

    favorContainer
        .clicks()
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { viewModel.toggleFavorite() }

    finishContainer
        .clicks()
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { viewModel.markAsFinished() }

    subscribeContainer
        .clicks()
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { viewModel.toggleSubscription() }

    unratedTags
        .clicks()
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe {
            showUnratedTags = !showUnratedTags

            viewModel.data.value?.let { entry -> bindTags(entry) }
        }

    spoilerTags
        .clicks()
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe {
            showSpoilerTags = !showSpoilerTags

            viewModel.data.value?.let { entry -> bindTags(entry) }
        }

    viewModel.userInfoData.observe(
        viewLifecycleOwner,
        Observer {
            bindUserInfo(it)
        },
    )

    viewModel.userInfoUpdateData.observe(
        viewLifecycleOwner,
        Observer {
            it?.let {
                hostingActivity.snackbar(R.string.fragment_set_user_info_success)
            }
        },
    )

    viewModel.userInfoUpdateError.observe(
        viewLifecycleOwner,
        Observer {
            it?.let {
                hostingActivity.multilineSnackbar(
                    getString(R.string.error_set_user_info, getString(it.message)),
                    Snackbar.LENGTH_LONG,
                    it.buttonMessage,
                    it.toClickListener(hostingActivity),
                )
            }
        },
    )
}
