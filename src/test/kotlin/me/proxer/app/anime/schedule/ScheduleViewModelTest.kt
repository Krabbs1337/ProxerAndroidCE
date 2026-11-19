package me.proxer.app.anime.schedule

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rubengees.rxbus.RxBus
import com.squareup.moshi.Moshi
import io.mockk.every
import okhttp3.Interceptor
import io.mockk.mockk
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.time.Instant
import java.util.concurrent.TimeUnit
import me.proxer.app.util.Validators
import me.proxer.app.util.data.InstantJsonAdapter
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
import me.proxer.library.enums.CalendarDay
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * API-mocking test for [ScheduleViewModel]: a real [ProxerApi] whose OkHttp client rewrites
 * every request to a [MockWebServer] (the lib forces its own Retrofit base URL, so the
 * rewrite happens at the HTTP layer). The mock server serves a calendar envelope and the
 * ViewModel's grouping/sorting is verified end to end.
 */
class ScheduleViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer

    private val storageHelper = mockk<StorageHelper>()
    private val preferenceHelper = mockk<PreferenceHelper>()

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        mockWebServer = MockWebServer()
        mockWebServer.start()

        every { storageHelper.isLoggedIn } returns true
        every { storageHelper.isLoggedInObservable } returns PublishSubject.create()
        every { preferenceHelper.isAgeRestrictedMediaAllowedObservable } returns PublishSubject.create()

        val moshi = Moshi.Builder().add(Instant::class.java, InstantJsonAdapter()).build()
        val redirectToMockServer =
            Interceptor { chain ->
                val original = chain.request().url
                val rewritten =
                    original
                        .newBuilder()
                        .scheme("http")
                        .host(mockWebServer.hostName)
                        .port(mockWebServer.port)
                        .build()

                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .url(rewritten)
                        .build(),
                )
            }
        val client = OkHttpClient.Builder().addInterceptor(redirectToMockServer).build()
        val api =
            ProxerApi
                .Builder("test-api-key")
                .client(client)
                .moshi(moshi)
                .userAgent("proxer-android-ce-test")
                .build()

        startKoin {
            modules(
                module {
                    single { RxBus() }
                    single { api }
                    single { client }
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
        mockWebServer.shutdown()
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    private fun calendarEntry(
        id: String,
        name: String,
        weekday: String,
        time: Long,
        episode: Int,
    ) = """
        {
            "id": "$id",
            "eid": "eid-$id",
            "entryname": "$name",
            "episode": $episode,
            "episodeTitle": "Episode $episode",
            "time": $time,
            "timezone": "Europe/Berlin",
            "iid": "iid-$id",
            "industryname": "Studio $id",
            "weekday": "$weekday",
            "uptime": $time,
            "genre": "Action,Comedy",
            "rate_sum": 100,
            "rate_count": 10,
            "rating": 9.5
        }
        """.trimIndent()

    private fun enqueueCalendar(vararg entries: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": 0, "message": "", "data": [${entries.joinToString(",")}]}"""),
        )
    }

    @Test
    fun `load fetches calendar and groups entries by weekday sorted by date`() {
        enqueueCalendar(
            calendarEntry("1", "Anime A", "sun", 1712516400000L, 2),
            calendarEntry("2", "Anime B", "mon", 1712430000000L, 1),
            calendarEntry("3", "Anime C", "sun", 1712400000000L, 5),
        )

        val viewModel = ScheduleViewModel()
        viewModel.load()

        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/api/v1/media/calendar", request?.path)

        val data = viewModel.data.value
        assertNotNull(data)
        assertEquals(setOf(CalendarDay.MONDAY, CalendarDay.SUNDAY), data!!.keys)
        assertEquals(listOf("Anime C", "Anime A"), data[CalendarDay.SUNDAY]!!.map { it.name })
        assertEquals(listOf("Anime B"), data[CalendarDay.MONDAY]!!.map { it.name })
        assertEquals(5, data[CalendarDay.SUNDAY]!!.first().episode)
        assertEquals(setOf("Action,Comedy"), data[CalendarDay.SUNDAY]!!.first().genres)
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `load maps a failed api response to an error action`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": 1, "message": "internal", "code": 1}"""),
        )

        val viewModel = ScheduleViewModel()
        viewModel.load()

        assertNull(viewModel.data.value)
        assertNotNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }
}
