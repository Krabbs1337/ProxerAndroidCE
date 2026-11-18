package me.proxer.app.manga

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import me.proxer.app.R
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * Setup of [MangaFragment]: click handling, options menu and recycler view wiring.
 *
 * @author Ruben Gees
 */
internal fun MangaFragment.setupClickHandling() {
    innerAdapter.clickSubject
        .autoDispose(this.scope())
        .subscribeAndLogErrors { (_, coordinates, position) ->
            val (xCoordinate, yCoordinate) = coordinates
            val parentHeight = recyclerView.height
            val parentWidth = recyclerView.width
            val normalizedParentHeight = parentHeight / 2

            val widthPart = parentWidth / 4
            val heightPart = parentHeight / 4

            // Add one to the position to account for the header.
            when (readerOrientation) {
                MangaReaderOrientation.LEFT_TO_RIGHT -> {
                    when {
                        xCoordinate < widthPart -> {
                            recyclerView.smoothScrollToPosition(position - 1 + 1)
                        }

                        xCoordinate >= widthPart && xCoordinate < widthPart * 3 -> {
                            hostingActivity
                                .toggleFullscreen()
                        }

                        else -> {
                            recyclerView.smoothScrollToPosition(position + 1 + 1)
                        }
                    }
                }

                MangaReaderOrientation.RIGHT_TO_LEFT -> {
                    when {
                        xCoordinate < widthPart -> {
                            recyclerView.smoothScrollToPosition(position + 1 + 1)
                        }

                        xCoordinate >= widthPart && xCoordinate < widthPart * 3 -> {
                            hostingActivity
                                .toggleFullscreen()
                        }

                        else -> {
                            recyclerView.smoothScrollToPosition(position - 1 + 1)
                        }
                    }
                }

                MangaReaderOrientation.VERTICAL -> {
                    when {
                        yCoordinate < heightPart -> {
                            recyclerView.smoothScrollBy(0, -normalizedParentHeight)
                        }

                        yCoordinate >= heightPart && yCoordinate < heightPart * 3 -> {
                            hostingActivity
                                .toggleFullscreen()
                        }

                        else -> {
                            recyclerView.smoothScrollBy(0, normalizedParentHeight)
                        }
                    }
                }
            }
        }

    innerAdapter.lowMemorySubject
        .autoDispose(this.scope())
        .subscribe {
            if (!hasLowMemory) {
                hostingActivity.multilineSnackbar(R.string.fragment_manga_low_memory)

                hasLowMemory = true
            }
        }
}

internal fun MangaFragment.setupMenu() {
    requireActivity().addMenuProvider(
        object : MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                inflater: MenuInflater,
            ) {
                IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_manga, menu, true)

                toolbar.doOnLayout {
                    toolbar.findViewById<View>(R.id.toggle_orientation).rotation =
                        when (readerOrientation) {
                            MangaReaderOrientation.LEFT_TO_RIGHT -> 90f
                            MangaReaderOrientation.RIGHT_TO_LEFT -> 270f
                            MangaReaderOrientation.VERTICAL -> 0.0f
                        }
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean =
                when (item.itemId) {
                    R.id.toggle_orientation -> {
                        readerOrientation =
                            when (readerOrientation) {
                                MangaReaderOrientation.LEFT_TO_RIGHT -> MangaReaderOrientation.RIGHT_TO_LEFT
                                MangaReaderOrientation.RIGHT_TO_LEFT -> MangaReaderOrientation.VERTICAL
                                MangaReaderOrientation.VERTICAL -> MangaReaderOrientation.LEFT_TO_RIGHT
                            }

                        bindOrientationOptionsItem()
                        bindHeaderAndFooterHeight()
                        bindLayoutManager()

                        hostingActivity.multilineSnackbar(
                            when (readerOrientation) {
                                MangaReaderOrientation.LEFT_TO_RIGHT -> R.string.fragment_manga_left_to_right
                                MangaReaderOrientation.RIGHT_TO_LEFT -> R.string.fragment_manga_right_to_left
                                MangaReaderOrientation.VERTICAL -> R.string.fragment_manga_vertical
                            },
                        )

                        true
                    }

                    else -> {
                        false
                    }
                }
        },
        viewLifecycleOwner,
    )
}

internal fun MangaFragment.setupRecyclerView() {
    initHeaderAndFooter()

    preloader.glide =
        com.bumptech.glide.Glide
            .with(this)
    innerAdapter.glide =
        com.bumptech.glide.Glide
            .with(this)

    bindLayoutManager()

    recyclerView.setItemViewCacheSize(1)
    recyclerView.setHasFixedSize(true)
    recyclerView.enableFastScroll()

    recyclerView.itemAnimator = null
    recyclerView.adapter = adapter

    viewModel.userStateData.observe(
        viewLifecycleOwner,
        Observer {
            it?.let {
                hostingActivity.snackbar(R.string.fragment_set_user_info_success)
            }
        },
    )

    viewModel.userStateError.observe(
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
