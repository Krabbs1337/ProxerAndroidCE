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

/**
 * @author Ruben Gees
 */
class MediaInfoFragment : BaseContentFragment<Entry>(R.layout.fragment_media_info) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
    }

    private val binding: FragmentMediaInfoBinding
        get() = FragmentMediaInfoBinding.bind(requireView())

    companion object {
        private const val SHOW_UNRATED_TAGS_ARGUMENT = "show_unrated_tags"
        private const val SHOW_SPOILER_TAGS_ARGUMENT = "show_spoiler_tags"

        fun newInstance() =
            MediaInfoFragment().apply {
                arguments = bundleOf()
            }
    }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    override val viewModel by activityViewModel<MediaInfoViewModel> { parametersOf(id) }

    private val id: String
        get() = hostingActivity.id

    internal var showUnratedTags: Boolean
        get() = requireArguments().getBoolean(SHOW_UNRATED_TAGS_ARGUMENT, false)
        set(value) = requireArguments().putBoolean(SHOW_UNRATED_TAGS_ARGUMENT, value)

    internal var showSpoilerTags: Boolean
        get() = requireArguments().getBoolean(SHOW_SPOILER_TAGS_ARGUMENT, false)
        set(value) = requireArguments().putBoolean(SHOW_SPOILER_TAGS_ARGUMENT, value)

    internal val ratingContainer: ViewGroup get() = binding.ratingContainer
    internal val rating: RatingBar get() = binding.rating
    internal val ratingAmount: TextView get() = binding.ratingAmount

    internal val infoTable: TableLayout get() = binding.infoTable

    internal val genresTitle: TextView get() = binding.genresTitle
    internal val genres: FlexboxLayout get() = binding.genres
    internal val tagsTitle: TextView get() = binding.tagsTitle
    internal val tags: FlexboxLayout get() = binding.tags
    internal val unratedTags: Button get() = binding.unratedTags
    internal val spoilerTags: Button get() = binding.spoilerTags
    internal val fskConstraintsTitle: TextView get() = binding.fskConstraintsTitle
    internal val fskConstraints: FlexboxLayout get() = binding.fskConstraints
    internal val translatorGroupsTitle: TextView get() = binding.translatorGroupsTitle
    internal val translatorGroups: FlexboxLayout get() = binding.translatorGroups
    internal val industriesTitle: TextView get() = binding.industriesTitle
    internal val industries: FlexboxLayout get() = binding.industries

    internal val noteContainer: ViewGroup get() = binding.noteContainer
    internal val note: ImageView get() = binding.note
    internal val favorContainer: ViewGroup get() = binding.favorContainer
    internal val favor: ImageView get() = binding.favor
    internal val finishContainer: ViewGroup get() = binding.finishContainer
    internal val finish: ImageView get() = binding.finish
    internal val subscribeContainer: ViewGroup get() = binding.subscribeContainer
    internal val subscribe: ImageView get() = binding.subscribe

    internal val description: TextView get() = binding.description

    override fun showData(data: Entry) {
        super.showData(data)

        bind(data)
    }

    private fun bind(entry: Entry) {
        infoTable.removeAllViews()

        bindRating(entry)
        bindSynonyms(entry)
        bindSeasons(entry)
        bindStatus(entry)
        bindLicense(entry)
        bindAdaption(entry)
        bindGenres(entry)
        bindTags(entry)
        bindFskConstraints(entry)
        bindTranslatorGroups(entry)
        bindIndustries(entry)

        description.text = entry.description
    }
}
