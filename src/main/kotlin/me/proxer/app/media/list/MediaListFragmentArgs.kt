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

internal val MediaListFragment.category
    get() =
        BundleCompat.getSerializable(
            requireArguments(),
            MediaListFragment.CATEGORY_ARGUMENT,
            Category::class.java,
        ) as Category

internal var MediaListFragment.sortCriteria: MediaSearchSortCriteria
    get() =
        BundleCompat.getSerializable(
            requireArguments(),
            MediaListFragment.SORT_CRITERIA_ARGUMENT,
            MediaSearchSortCriteria::class.java,
        )
            ?: MediaSearchSortCriteria.RATING
    set(value) {
        requireArguments().putSerializable(MediaListFragment.SORT_CRITERIA_ARGUMENT, value)

        viewModel.sortCriteria = value
    }

internal var MediaListFragment.type: MediaType
    get() =
        BundleCompat.getSerializable(requireArguments(), MediaListFragment.TYPE_ARGUMENT, MediaType::class.java)
            ?: when (category) {
                Category.ANIME -> MediaType.ALL_ANIME
                Category.MANGA -> MediaType.ALL_MANGA
                else -> error("Unknown value for category")
            }
    set(value) {
        requireArguments().putSerializable(MediaListFragment.TYPE_ARGUMENT, value)

        viewModel.type = value
    }

internal var MediaListFragment.searchQuery: String?
    get() = requireArguments().getString(MediaListFragment.SEARCH_QUERY_ARGUMENT, null)
    set(value) {
        requireArguments().putString(MediaListFragment.SEARCH_QUERY_ARGUMENT, value)

        viewModel.searchQuery = value
    }

internal var MediaListFragment.language: Language?
    get() = BundleCompat.getSerializable(requireArguments(), MediaListFragment.LANGUAGE_ARGUMENT, Language::class.java)
    set(value) {
        requireArguments().putSerializable(MediaListFragment.LANGUAGE_ARGUMENT, language)

        viewModel.language = value
    }

internal var MediaListFragment.genres: List<LocalTag>
    get() =
        BundleCompat.getParcelableArrayList(requireArguments(), MediaListFragment.GENRES_ARGUMENT, LocalTag::class.java)
            ?: emptyList()
    set(value) {
        requireArguments().putParcelableArrayList(MediaListFragment.GENRES_ARGUMENT, ArrayList(value))

        viewModel.genres = value
    }

internal var MediaListFragment.excludedGenres: List<LocalTag>
    get() =
        BundleCompat.getParcelableArrayList(
            requireArguments(),
            MediaListFragment.EXCLUDED_GENRES_ARGUMENT,
            LocalTag::class.java,
        )
            ?: emptyList()
    set(value) {
        requireArguments().putParcelableArrayList(MediaListFragment.EXCLUDED_GENRES_ARGUMENT, ArrayList(value))

        viewModel.excludedGenres = value
    }

internal var MediaListFragment.fskConstraints: EnumSet<FskConstraint>
    get() = requireArguments().getEnumSet(MediaListFragment.FSK_CONSTRAINTS_ARGUMENT, FskConstraint::class.java)
    set(value) {
        requireArguments().putEnumSet(MediaListFragment.FSK_CONSTRAINTS_ARGUMENT, value)

        viewModel.fskConstraints = value
    }

internal var MediaListFragment.tags: List<LocalTag>
    get() =
        BundleCompat.getParcelableArrayList(requireArguments(), MediaListFragment.TAGS_ARGUMENT, LocalTag::class.java)
            ?: emptyList()
    set(value) {
        requireArguments().putParcelableArrayList(MediaListFragment.TAGS_ARGUMENT, ArrayList(value))

        viewModel.tags = value
    }

internal var MediaListFragment.excludedTags: List<LocalTag>
    get() =
        BundleCompat.getParcelableArrayList(
            requireArguments(),
            MediaListFragment.EXCLUDED_TAGS_ARGUMENT,
            LocalTag::class.java,
        )
            ?: emptyList()
    set(value) {
        requireArguments().putParcelableArrayList(MediaListFragment.EXCLUDED_TAGS_ARGUMENT, ArrayList(value))

        viewModel.excludedTags = value
    }

internal var MediaListFragment.tagRateFilter: TagRateFilter
    get() =
        BundleCompat.getSerializable(
            requireArguments(),
            MediaListFragment.TAG_RATE_FILTER_ARGUMENT,
            TagRateFilter::class.java,
        )
            ?: TagRateFilter.RATED_ONLY
    set(value) {
        requireArguments().putSerializable(MediaListFragment.TAG_RATE_FILTER_ARGUMENT, value)

        viewModel.tagRateFilter = value
    }

internal var MediaListFragment.tagSpoilerFilter: TagSpoilerFilter
    get() =
        BundleCompat.getSerializable(
            requireArguments(),
            MediaListFragment.TAG_SPOILER_FILTER_ARGUMENT,
            TagSpoilerFilter::class.java,
        )
            ?: TagSpoilerFilter.NO_SPOILERS
    set(value) {
        requireArguments().putSerializable(MediaListFragment.TAG_SPOILER_FILTER_ARGUMENT, value)

        viewModel.tagSpoilerFilter = value
    }

internal var MediaListFragment.hideFinished: Boolean
    get() = requireArguments().getBoolean(MediaListFragment.HIDE_FINISHED_ARGUMENT, false)
    set(value) {
        requireArguments().putBoolean(MediaListFragment.HIDE_FINISHED_ARGUMENT, value)

        viewModel.hideFinished = value
    }

internal var MediaListFragment.searchBottomSheetManager by Delegates.notNull<MediaListSearchBottomSheet>()
