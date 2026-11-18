package me.proxer.app.media.list

import android.content.Intent
import androidx.core.os.BundleCompat
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar

import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import androidx.transition.TransitionManager
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import com.jakewharton.rxbinding3.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import com.bumptech.glide.Glide
import me.proxer.app.R
import me.proxer.app.base.BackPressAware
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.LocalTag
import me.proxer.app.media.MediaActivity
import me.proxer.app.ui.view.ExpandableSelectionView
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.getEnumSet
import me.proxer.app.util.extension.putEnumSet
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.app.util.extension.unsafeParametersOf
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.TagRateFilter
import me.proxer.library.enums.TagSpoilerFilter
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.EnumSet
import kotlin.properties.Delegates
import me.proxer.app.databinding.FragmentMediaListBinding
import androidx.core.view.MenuProvider

/**
 * @author Ruben Gees
 */
class MediaListFragment :
    PagedContentFragment<MediaListEntry>(R.layout.fragment_media_list),
    BackPressAware {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
    }

    private val binding: FragmentMediaListBinding
        get() = FragmentMediaListBinding.bind(requireView())

    companion object {
        internal const val CATEGORY_ARGUMENT = "category"
        internal const val SORT_CRITERIA_ARGUMENT = "sort_criteria"
        internal const val TYPE_ARGUMENT = "type"
        internal const val SEARCH_QUERY_ARGUMENT = "search_query"
        internal const val LANGUAGE_ARGUMENT = "language"
        internal const val GENRES_ARGUMENT = "genres"
        internal const val EXCLUDED_GENRES_ARGUMENT = "excluded_genres"
        internal const val FSK_CONSTRAINTS_ARGUMENT = "fsk_constraints"
        internal const val TAGS_ARGUMENT = "tags"
        internal const val EXCLUDED_TAGS_ARGUMENT = "excluded_tags"
        internal const val TAG_RATE_FILTER_ARGUMENT = "tag_rate_filter"
        internal const val TAG_SPOILER_FILTER_ARGUMENT = "tag_spoiler_filter"
        internal const val HIDE_FINISHED_ARGUMENT = "hide_finished"

        fun newInstance(category: Category) =
            MediaListFragment().apply {
                arguments = Bundle().apply { putSerializable(CATEGORY_ARGUMENT, category) }
            }
    }

    override val isSwipeToRefreshEnabled = false
    override val emptyDataMessage = R.string.error_no_data_search

    override val viewModel by viewModel<MediaListViewModel> {
        unsafeParametersOf(
            sortCriteria,
            type,
            searchQuery,
            language,
            genres,
            excludedGenres,
            fskConstraints,
            tags,
            excludedTags,
            tagRateFilter,
            tagSpoilerFilter,
            hideFinished,
        )
    }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1, VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<MediaAdapter>()

    internal val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }

    internal val searchBottomSheet: ViewGroup get() = requireView().findViewById(R.id.searchBottomSheet)
    internal val searchBottomSheetTitle: ViewGroup get() = requireView().findViewById(R.id.titleContainer)
    internal val search: Button get() = requireView().findViewById(R.id.search)
    internal val languageSelector: ExpandableSelectionView get() = requireView().findViewById(R.id.languageSelector)
    internal val genreSelector: ExpandableSelectionView get() = requireView().findViewById(R.id.genreSelector)
    internal val excludedGenreSelector: ExpandableSelectionView get() =
        requireView().findViewById(
            R.id.excludedGenreSelector,
        )
    internal val fskSelector: ExpandableSelectionView get() = requireView().findViewById(R.id.fskSelector)
    internal val tagSelector: ExpandableSelectionView get() = requireView().findViewById(R.id.tagSelector)
    internal val excludedTagSelector: ExpandableSelectionView get() =
        requireView().findViewById(
            R.id.excludedTagSelector,
        )
    internal val includeUnratedTags: CheckBox get() = requireView().findViewById(R.id.includeUnratedTags)
    internal val includeSpoilerTags: CheckBox get() = requireView().findViewById(R.id.includeSpoilerTags)
    internal val hideFinishedCheckBox: CheckBox get() = requireView().findViewById(R.id.hideFinished)

    internal var searchView by Delegates.notNull<SearchView>()

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = MediaAdapter(category)

        if (savedInstanceState == null) {
            setInitialType()
            setInitialSortCriteria()
        }

        innerAdapter.clickSubject
            .autoDispose(this.scope())
            .subscribe { (view, entry) ->
                MediaActivity.navigateTo(requireActivity(), entry.id, entry.name, entry.medium.toCategory(), view)
            }
    }

    override fun onBackPressed(): Boolean = searchBottomSheetManager.onBackPressed()

    override fun showData(data: List<MediaListEntry>) {
        super.showData(data)

        setContentFooterIfNeeded()
    }

    override fun hideData() {
        adapter.footer = null

        super.hideData()
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        adapter.footer?.updatePadding(bottom = searchBottomSheetTitle.height)
    }

    override fun hideError() {
        super.hideError()

        setContentFooterIfNeeded()
    }
}
