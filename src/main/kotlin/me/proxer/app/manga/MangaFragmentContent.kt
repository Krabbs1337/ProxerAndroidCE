package me.proxer.app.manga

import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.ui.view.MediaControlView.SimpleEpisodeInfo
import me.proxer.library.entity.info.EntryCore

/**
 * Content/error display of [MangaFragment] (bodies behind the BaseContentFragment overrides).
 *
 * @author Ruben Gees
 */
internal fun MangaFragment.showMangaData(data: MangaChapterInfo) {
    hostingActivity.onContentShow()

    chapterTitle = data.chapter.title
    episodeAmount = data.episodeAmount
    name = data.name

    showHeaderAndFooter(data)

    preloader.preload(data.chapter)
    innerAdapter.setChapter(data.chapter)

    data.chapter.pages?.let { pages ->
        innerAdapter.swapDataAndNotifyWithDiffing(pages)
    }

    lastPosition.let { safeLastPosition ->
        if (safeLastPosition != null) {
            recyclerView.safeLayoutManager.onRestoreInstanceState(safeLastPosition)

            lastPosition = null
        } else {
            val lastPage = storageHelper.getLastMangaPage(id, episode, language)

            if (lastPage != null && lastPage > 0) {
                (recyclerView.safeLayoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(lastPage, 0)
            }
        }
    }
}

internal fun MangaFragment.hideMangaData(superCall: () -> Unit) {
    preloader.cancel()
    innerAdapter.swapDataAndNotifyWithDiffing(emptyList())

    if (viewModel.error.value
            ?.data
            ?.get(ErrorUtils.ENTRY_DATA_KEY) !is EntryCore
    ) {
        adapter.header = null
        adapter.footer = null

        superCall()
    }
}

internal fun MangaFragment.showMangaError(action: ErrorUtils.ErrorAction) {
    hostingActivity.onContentHide()

    action.data[ErrorUtils.CHAPTER_TITLE_DATA_KEY].let {
        chapterTitle = it as? String
    }

    action.data[ErrorUtils.ENTRY_DATA_KEY].let {
        if (it is EntryCore) {
            episodeAmount = it.episodeAmount
            name = it.name

            header.episodeInfo = SimpleEpisodeInfo(it.episodeAmount, episode)
            header.translatorGroup = null
            header.uploader = null
            header.dateTime = null

            adapter.header = header
        }
    }

    if (adapter.header != null) {
        bindHeaderAndFooterHeight()

        contentContainer.isVisible = true
        errorContainer.isInvisible = true

        errorInnerContainer.doOnLayout {
            header.doOnLayout {
                val newCenter = root.height / 2f + header.height / 2f
                val containerCenterCorrection = errorInnerContainer.height / 2f

                errorInnerContainer.y = newCenter - containerCenterCorrection
                errorContainer.isVisible = true
            }
        }
    } else {
        errorContainer.translationY = 0f
    }
}

internal fun MangaFragment.hideMangaError() {
    if (viewModel.data.value == null) {
        adapter.header = null
    }
}
