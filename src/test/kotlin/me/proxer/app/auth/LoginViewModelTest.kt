package me.proxer.app.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rubengees.rxbus.RxBus
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.time.Instant
import java.util.concurrent.TimeUnit
import me.proxer.app.util.data.InstantJsonAdapter
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * API-mocking tests for [LoginViewModel], covering the 2FA flow: a login attempt without a
 * secret key must answer with server error code 3038 (USER_2FA_SECRET_REQUIRED), which has to
 * enable the two-factor prompt in the dialog.
 */
class LoginViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer

    private val storageHelper = mockk<StorageHelper>(relaxed = true)
    private val preferenceHelper = mockk<PreferenceHelper>(relaxed = true)

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        mockWebServer = MockWebServer()
        mockWebServer.start()

        every { storageHelper.isLoggedInObservable } returns PublishSubject.create()
        every { preferenceHelper.isTwoFactorAuthenticationEnabled } returns false

        val moshi = Moshi.Builder().add(Instant::class.java, InstantJsonAdapter()).build()
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        val rewritten =
                            chain
                                .request()
                                .url
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
                    },
                ).build()
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
                    single { storageHelper }
                    single { preferenceHelper }
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
    fun `login without secret key enables the two factor prompt on server code 3038`() {
        mockWebServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": 1, "message": "2FA secret required", "code": 3038}"""),
        )

        val viewModel = LoginViewModel()
        viewModel.login("someuser", "somepassword", "")

        assertEquals(true, viewModel.isTwoFactorAuthenticationEnabled.value)
        assertNotNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.success.value)
        verify { preferenceHelper.isTwoFactorAuthenticationEnabled = true }
    }

    @Test
    fun `login without secret key omits the secretkey field from the request body`() {
        mockWebServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": 1, "message": "2FA secret required", "code": 3038}"""),
        )

        val viewModel = LoginViewModel()
        viewModel.login("someuser", "somepassword", null)

        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        val body = request?.body?.readUtf8() ?: ""

        assertTrue(body.contains("username=someuser"))
        assertFalse(body.contains("secretkey"))
    }

    @Test
    fun `login with secret key sends the secretkey field`() {
        mockWebServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": 1, "message": "invalid code", "code": 3039}"""),
        )

        val viewModel = LoginViewModel()
        viewModel.login("someuser", "somepassword", "123456")

        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        val body = request?.body?.readUtf8() ?: ""

        assertTrue(body.contains("secretkey=123456"))
    }

    @Test
    fun `login with invalid credentials does not enable the two factor prompt`() {
        mockWebServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": 1, "message": "Username and password do not match", "code": 3001}"""),
        )

        val viewModel = LoginViewModel()
        viewModel.login("someuser", "wrongpassword", "")

        assertEquals(false, viewModel.isTwoFactorAuthenticationEnabled.value)
        assertNotNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
        verify(exactly = 0) { preferenceHelper.isTwoFactorAuthenticationEnabled = true }
    }
}
