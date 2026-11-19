package me.proxer.app.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rubengees.rxbus.RxBus
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import me.proxer.app.R
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
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
 * Tests for the load/reload/error machinery of [BaseViewModel]. Runs on the JVM with
 * trampoline schedulers (io + main) and a Koin test module providing mocked collaborators.
 */
class BaseViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val storageHelper = mockk<StorageHelper>()
    private val preferenceHelper = mockk<PreferenceHelper>()

    private class TestViewModel(
        var single: Single<String>,
    ) : BaseViewModel<String>() {
        override val dataSingle: Single<String>
            get() = single
    }

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
    fun `load emits data and resets loading and error state`() {
        val viewModel = TestViewModel(Single.just("hello"))

        viewModel.load()

        assertEquals("hello", viewModel.data.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `load maps NotLoggedInException to the login error action`() {
        val viewModel = TestViewModel(Single.error(NotLoggedInException()))

        viewModel.load()

        assertNull(viewModel.data.value)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(R.string.error_login_required, viewModel.error.value?.message)
        assertEquals(ErrorUtils.ErrorAction.ButtonAction.LOGIN, viewModel.error.value?.buttonAction)
    }

    @Test
    fun `reload clears a previous error and loads again`() {
        val viewModel = TestViewModel(Single.error(NotLoggedInException()))
        viewModel.load()
        assertEquals(R.string.error_login_required, viewModel.error.value?.message)

        viewModel.single = Single.just("recovered")
        viewModel.reload()

        assertEquals("recovered", viewModel.data.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadIfPossible does not reload while an error is present`() {
        val viewModel = TestViewModel(Single.error(NotLoggedInException()))
        viewModel.load()

        viewModel.single = Single.just("ignored")
        viewModel.loadIfPossible()

        assertNull(viewModel.data.value)
        assertEquals(R.string.error_login_required, viewModel.error.value?.message)
    }
}
