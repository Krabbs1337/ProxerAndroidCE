package me.proxer.app.manga

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import me.proxer.app.R
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.MediaControlView.SimpleEpisodeInfo
import me.proxer.app.ui.view.MediaControlView.SimpleTranslatorGroup
import me.proxer.app.ui.view.MediaControlView.Uploader
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.util.extension.toLocalDateTimeBP

/**
 * Header/footer (MediaControlView) handling and layout manager of [MangaFragment].
 *
 * @author Ruben Gees
 */
internal fun MangaFragment.initHeaderAndFooter() {
    val horizontalMargin = DeviceUtils.getHorizontalMargin(requireContext(), true)
    val verticalMargin = DeviceUtils.getVerticalMargin(requireContext(), true)

    header.apply {
        textResolver = mediaControlTextResolver

        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(
                horizontalMargin,
                verticalMargin,
                horizontalMargin,
                verticalMargin,
            )
        }
    }

    footer.apply {
        textResolver = mediaControlTextResolver

        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(
                horizontalMargin,
                verticalMargin,
                horizontalMargin,
                verticalMargin,
            )
        }
    }

    Observable
        .merge(header.uploaderClickSubject, footer.uploaderClickSubject)
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { ProfileActivity.navigateTo(requireActivity(), it.id, it.name) }

    Observable
        .merge(header.translatorGroupClickSubject, footer.translatorGroupClickSubject)
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { TranslatorGroupActivity.navigateTo(requireActivity(), it.id, it.name) }

    Observable
        .merge(header.episodeSwitchSubject, footer.episodeSwitchSubject)
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe {
            if (areBookmarksAutomatic && it > episode && storageHelper.isLoggedIn) {
                viewModel.bookmark(it)
            }

            episode = it
        }

    Observable
        .merge(header.bookmarkSetSubject, footer.bookmarkSetSubject)
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { viewModel.bookmark(it) }

    Observable
        .merge(header.finishClickSubject, footer.finishClickSubject)
        .autoDispose(viewLifecycleOwner.scope())
        .subscribe { viewModel.markAsFinished() }
}

internal fun MangaFragment.showHeaderAndFooter(data: MangaChapterInfo) {
    bindHeaderAndFooterHeight()

    val translatorGroup =
        data.chapter.scanGroupId?.let { id ->
            data.chapter.scanGroupName?.let { name ->
                SimpleTranslatorGroup(id, name)
            }
        }

    header.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)
    header.uploader = Uploader(data.chapter.uploaderId, data.chapter.uploaderName)
    header.dateTime = data.chapter.date.toLocalDateTimeBP()
    header.translatorGroup = translatorGroup

    footer.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)

    adapter.header = header
    adapter.footer = footer
}

internal fun MangaFragment.bindOrientationOptionsItem() {
    val rotation =
        when (readerOrientation) {
            MangaReaderOrientation.LEFT_TO_RIGHT -> 90f
            MangaReaderOrientation.RIGHT_TO_LEFT -> 270f
            MangaReaderOrientation.VERTICAL -> 0.0f
        }

    toolbar
        .findViewById<View>(R.id.toggle_orientation)
        .animate()
        .rotation(rotation)
        .setDuration(mediumAnimationTime.toLong())
        .start()
}

internal fun MangaFragment.bindLayoutManager() {
    val state = recyclerView.layoutManager?.onSaveInstanceState()

    snapHelper?.attachToRecyclerView(null)
    snapHelper = null

    recyclerView.recycledViewPool.clear()

    recyclerView.layoutManager = MangaLinearLayoutManger(requireContext(), readerOrientation)

    if (!isVertical) {
        snapHelper = PagerSnapHelper().apply { attachToRecyclerView(recyclerView) }
    }

    innerAdapter.isVertical = isVertical

    recyclerView.safeLayoutManager.onRestoreInstanceState(state)
}

internal fun MangaFragment.bindHeaderAndFooterHeight() {
    if (isVertical || viewModel.error.value != null) {
        header.layoutParams.height = WRAP_CONTENT
        footer.layoutParams.height = WRAP_CONTENT
    } else {
        header.layoutParams.height = MATCH_PARENT
        footer.layoutParams.height = MATCH_PARENT
    }
}
