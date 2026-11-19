package me.proxer.app.util.http

import io.mockk.every
import io.mockk.mockk
import me.proxer.library.util.ProxerUrls
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [HttpsUpgradeInterceptor].
 */
class HttpsUpgradeInterceptorTest {
    private val interceptor = HttpsUpgradeInterceptor()

    private fun chainFor(
        url: String,
        captured: MutableList<Request>,
    ): Interceptor.Chain {
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url(url).build()

        every { chain.request() } returns request
        every { chain.proceed(capture(captured)) } answers {
            Response
                .Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("ok".toResponseBody())
                .build()
        }

        return chain
    }

    @Test
    fun `http url on proxer host is upgraded to https`() {
        val captured = mutableListOf<Request>()

        interceptor.intercept(chainFor("http://${ProxerUrls.webBase.host}/news", captured))

        assertEquals(1, captured.size)
        assertEquals("https", captured.single().url.scheme)
        assertEquals(ProxerUrls.webBase.host, captured.single().url.host)
    }

    @Test
    fun `http url on non-upgradable host stays http`() {
        val captured = mutableListOf<Request>()

        interceptor.intercept(chainFor("http://example.com/image.png", captured))

        assertEquals("http", captured.single().url.scheme)
    }

    @Test
    fun `https url is passed through unchanged`() {
        val captured = mutableListOf<Request>()

        interceptor.intercept(chainFor("https://${ProxerUrls.cdnBase.host}/image.png", captured))

        assertEquals("https", captured.single().url.scheme)
    }
}
