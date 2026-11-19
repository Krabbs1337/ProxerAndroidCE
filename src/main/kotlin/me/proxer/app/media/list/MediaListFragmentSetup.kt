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
import androidx.core.view.MenuProvider

internal fun MediaListFragment.setupView() {
    requireActivity().addMenuProvider(
        object : MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                inflater: MenuInflater,
            ) {
                IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_media_list, menu, true)

                when (sortCriteria) {
                    MediaSearchSortCriteria.RATING -> menu.findItem(R.id.rating).isChecked = true
                    MediaSearchSortCriteria.CLICKS -> menu.findItem(R.id.clicks).isChecked = true
                    MediaSearchSortCriteria.EPISODE_AMOUNT -> menu.findItem(R.id.episodeAmount).isChecked = true
                    MediaSearchSortCriteria.NAME -> menu.findItem(R.id.name).isChecked = true
                    else -> error("Unsupported sort criteria: $sortCriteria")
                }

                val filterSubMenu = requireNotNull(menu.findItem(R.id.filter).subMenu)

                when (category) {
                    Category.ANIME -> filterSubMenu.setGroupVisible(R.id.filterManga, false)
                    Category.MANGA, Category.NOVEL -> filterSubMenu.setGroupVisible(R.id.filterAnime, false)
                }

                when (type) {
                    MediaType.ALL_ANIME -> filterSubMenu.findItem(R.id.all_anime).isChecked = true
                    MediaType.ANIMESERIES -> filterSubMenu.findItem(R.id.animeseries).isChecked = true
                    MediaType.MOVIE -> filterSubMenu.findItem(R.id.movies).isChecked = true
                    MediaType.OVA -> filterSubMenu.findItem(R.id.ova).isChecked = true
                    MediaType.HENTAI -> filterSubMenu.findItem(R.id.hentai).isChecked = true
                    MediaType.ALL_MANGA -> filterSubMenu.findItem(R.id.all_manga).isChecked = true
                    MediaType.MANGASERIES -> filterSubMenu.findItem(R.id.mangaseries).isChecked = true
                    MediaType.ONESHOT -> filterSubMenu.findItem(R.id.oneshot).isChecked = true
                    MediaType.DOUJIN -> filterSubMenu.findItem(R.id.doujin).isChecked = true
                    MediaType.HMANGA -> filterSubMenu.findItem(R.id.hmanga).isChecked = true
                    else -> error("Unsupported type: $type")
                }

                menu.findItem(R.id.search).let { searchItem ->
                    searchView = searchItem.actionView as SearchView

                    searchItem
                        .actionViewEvents()
                        .autoDispose(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe { event ->
                            if (event.menuItem.isActionViewExpanded) {
                                searchQuery = null

                                viewModel.reload()
                            }

                            TransitionManager.endTransitions(toolbar)
                            TransitionManager.beginDelayedTransition(toolbar)
                        }

                    searchView
                        .queryTextChangeEvents()
                        .skipInitialValue()
                        .autoDispose(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe { event ->
                            searchQuery = event.queryText.toString().trim()

                            if (event.isSubmitted) {
                                searchView.clearFocus()

                                viewModel.reload()
                            }
                        }

                    searchQuery?.let {
                        searchItem.expandActionView()
                        searchView.setQuery(it, false)
                        searchView.clearFocus()
                    }
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.rating -> sortCriteria = MediaSearchSortCriteria.RATING
                    R.id.clicks -> sortCriteria = MediaSearchSortCriteria.CLICKS
                    R.id.episodeAmount -> sortCriteria = MediaSearchSortCriteria.EPISODE_AMOUNT
                    R.id.name -> sortCriteria = MediaSearchSortCriteria.NAME
                    R.id.all_anime -> type = MediaType.ALL_ANIME
                    R.id.animeseries -> type = MediaType.ANIMESERIES
                    R.id.movies -> type = MediaType.MOVIE
                    R.id.ova -> type = MediaType.OVA
                    R.id.hentai -> type = MediaType.HENTAI
                    R.id.all_manga -> type = MediaType.ALL_MANGA
                    R.id.mangaseries -> type = MediaType.MANGASERIES
                    R.id.oneshot -> type = MediaType.ONESHOT
                    R.id.doujin -> type = MediaType.DOUJIN
                    R.id.hmanga -> type = MediaType.HMANGA
                }

                item.isChecked = true

                return false
            }
        },
        viewLifecycleOwner,
    )

    innerAdapter.glide = Glide.with(this)

    searchBottomSheetManager = MediaListSearchBottomSheet.bindTo(this, viewModel)
}

internal fun MediaListFragment.setInitialType() {
    if (requireActivity().intent.action == Intent.ACTION_VIEW) {
        if (category == Category.ANIME) {
            when (
                requireActivity()
                    .intent.data
                    ?.pathSegments
                    ?.getOrNull(1) ?: 1
            ) {
                "animeseries" -> type = MediaType.ANIMESERIES
                "movie" -> type = MediaType.MOVIE
                "ova" -> type = MediaType.OVA
                "hentai" -> type = MediaType.HENTAI
            }
        } else if (category == Category.MANGA) {
            when (
                requireActivity()
                    .intent.data
                    ?.pathSegments
                    ?.getOrNull(1) ?: 1
            ) {
                "mangaseries" -> type = MediaType.MANGASERIES
                "oneshot" -> type = MediaType.ONESHOT
                "doujin" -> type = MediaType.DOUJIN
                "hmanga" -> type = MediaType.HMANGA
            }
        }
    }
}

internal fun MediaListFragment.setInitialSortCriteria() {
    if (requireActivity().intent.action == Intent.ACTION_VIEW) {
        when (
            requireActivity()
                .intent.data
                ?.pathSegments
                ?.getOrNull(2) ?: 2
        ) {
            "rating" -> sortCriteria = MediaSearchSortCriteria.RATING
            "clicks" -> sortCriteria = MediaSearchSortCriteria.CLICKS
        }
    }
}

internal fun MediaListFragment.setContentFooterIfNeeded() {
    if (adapter.footer == null) {
        adapter.footer =
            View(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        searchBottomSheetTitle.height,
                    )
            }
    }
}
