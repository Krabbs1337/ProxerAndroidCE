package me.proxer.app.bookmark

import com.rubengees.rxbus.RxBus
import io.mockk.every
import io.mockk.mockk
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.Medium
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Tests for the client-side category filter in [BookmarkViewModel] (ROADMAP 11.11):
 * the server only sorts by the category parameter, so non-matching entries must be
 * hidden locally instead of being pushed to the bottom.
 */
class BookmarkViewModelTest {
    private val storageHelper = mockk<StorageHelper>()
    private val preferenceHelper = mockk<PreferenceHelper>()

    private class TestableBookmarkViewModel(
        category: Category?,
    ) : BookmarkViewModel(null, category, false) {
        public override fun mergeNewDataWithExistingData(
            existingData: List<Bookmark>,
            newData: List<Bookmark>,
            currentPage: Int,
        ) = super.mergeNewDataWithExistingData(existingData, newData, currentPage)
    }

    private fun bookmark(
        id: String,
        category: Category,
    ) = Bookmark(
        id,
        "entry$id",
        category,
        "name$id",
        1,
        MediaLanguage.GERMAN,
        Medium.MANGASERIES,
        MediaState.AIRING,
        null,
        true,
    )

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        every { storageHelper.isLoggedInObservable } returns PublishSubject.create()
        every { preferenceHelper.isAgeRestrictedMediaAllowedObservable } returns PublishSubject.create()

        startKoin {
            modules(
                module {
                    single { RxBus() }
                    single { mockk<ProxerApi>() }
                    single { storageHelper }
                    single { preferenceHelper }
                    single { Validators(storageHelper, preferenceHelper) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `null category keeps all entries`() {
        val viewModel = TestableBookmarkViewModel(null)
        val anime = bookmark("1", Category.ANIME)
        val manga = bookmark("2", Category.MANGA)

        assertEquals(listOf(anime, manga), viewModel.mergeNewDataWithExistingData(emptyList(), listOf(anime, manga), 0))
    }

    @Test
    fun `anime filter hides manga and novel entries`() {
        val viewModel = TestableBookmarkViewModel(Category.ANIME)
        val anime = bookmark("1", Category.ANIME)
        val manga = bookmark("2", Category.MANGA)
        val novel = bookmark("3", Category.NOVEL)

        assertEquals(listOf(anime), viewModel.mergeNewDataWithExistingData(emptyList(), listOf(anime, manga, novel), 0))
    }

    @Test
    fun `manga filter hides anime entries`() {
        val viewModel = TestableBookmarkViewModel(Category.MANGA)
        val anime = bookmark("1", Category.ANIME)
        val manga = bookmark("2", Category.MANGA)

        assertEquals(listOf(manga), viewModel.mergeNewDataWithExistingData(emptyList(), listOf(anime, manga), 0))
    }

    @Test
    fun `filter change drops stale entries of other categories on first page`() {
        val viewModel = TestableBookmarkViewModel(Category.ANIME)
        val staleManga = bookmark("1", Category.MANGA)
        val anime = bookmark("2", Category.ANIME)

        assertEquals(listOf(anime), viewModel.mergeNewDataWithExistingData(listOf(staleManga), listOf(anime), 0))
    }

    @Test
    fun `filter applies to later pages and keeps existing matching entries`() {
        val viewModel = TestableBookmarkViewModel(Category.ANIME)
        val existingAnime = bookmark("1", Category.ANIME)
        val manga = bookmark("2", Category.MANGA)
        val newAnime = bookmark("3", Category.ANIME)

        assertEquals(
            listOf(existingAnime, newAnime),
            viewModel.mergeNewDataWithExistingData(listOf(existingAnime), listOf(manga, newAnime), 1),
        )
    }
}
