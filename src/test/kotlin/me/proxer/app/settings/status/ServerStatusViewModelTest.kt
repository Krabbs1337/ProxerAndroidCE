package me.proxer.app.settings.status

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rubengees.rxbus.RxBus
import io.mockk.every
import io.mockk.mockk
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * API-mocking test for [ServerStatusViewModel]: the injected [OkHttpClient] rewrites every
 * request to a [MockWebServer], which serves a status-table HTML page that the ViewModel
 * scrapes with jsoup.
 */
class ServerStatusViewModelTest {
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

        every { storageHelper.isLoggedInObservable } returns PublishSubject.create()
        every { preferenceHelper.isAgeRestrictedMediaAllowedObservable } returns PublishSubject.create()

        val client =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    val rewritten =
                        chain
                            .request()
                            .newBuilder()
                            .url(mockWebServer.url("/status"))
                            .build()

                    chain.proceed(rewritten)
                }.build()

        startKoin {
            modules(
                module {
                    single { RxBus() }
                    single { mockk<ProxerApi>() }
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

    @Test
    fun `load scrapes the server status table and skips image cells`() {
        mockWebServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody(
                    """
                    <html><body><table>
                        <tr><td>Server 1: <span>online</span></td></tr>
                        <tr><td>Server 2 Stream: <span>offline</span></td></tr>
                        <tr><td>Server 3 Manga: <span>online</span></td></tr>
                        <tr><td><img src="icon.png"/> Server 4: <span>online</span></td></tr>
                    </table></body></html>
                    """.trimIndent(),
                ),
        )

        val viewModel = ServerStatusViewModel()
        viewModel.load()

        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/status", request?.path)

        assertEquals(
            listOf(
                ServerStatus("Server 1", 1, ServerType.MAIN, true),
                ServerStatus("Server 2 Stream", 2, ServerType.STREAM, false),
                ServerStatus("Server 3 Manga", 3, ServerType.MANGA, true),
            ),
            viewModel.data.value,
        )
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `load maps a connection failure to an error action`() {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val viewModel = ServerStatusViewModel()
        viewModel.load()

        assertNull(viewModel.data.value)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(true, viewModel.error.value != null)
    }
}
