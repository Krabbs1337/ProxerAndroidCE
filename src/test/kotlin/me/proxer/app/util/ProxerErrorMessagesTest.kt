package me.proxer.app.util

import me.proxer.app.R
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ErrorType.CANCELLED
import me.proxer.library.ProxerException.ErrorType.IO
import me.proxer.library.ProxerException.ErrorType.PARSING
import me.proxer.library.ProxerException.ErrorType.SERVER
import me.proxer.library.ProxerException.ErrorType.TIMEOUT
import me.proxer.library.ProxerException.ErrorType.UNKNOWN
import me.proxer.library.ProxerException.ServerErrorType.IP_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.RATE_LIMIT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [getMessageForProxerException].
 */
class ProxerErrorMessagesTest {
    @Test
    fun `timeout maps to timeout error`() {
        assertEquals(R.string.error_timeout, getMessageForProxerException(ProxerException(TIMEOUT)))
    }

    @Test
    fun `io maps to io error`() {
        assertEquals(R.string.error_io, getMessageForProxerException(ProxerException(IO)))
    }

    @Test
    fun `parsing maps to parsing error`() {
        assertEquals(R.string.error_parsing, getMessageForProxerException(ProxerException(PARSING)))
    }

    @Test
    fun `unknown and cancelled map to unknown error`() {
        assertEquals(R.string.error_unknown, getMessageForProxerException(ProxerException(UNKNOWN)))
        assertEquals(R.string.error_unknown, getMessageForProxerException(ProxerException(CANCELLED)))
    }

    @Test
    fun `server rate limit maps to rate limit error`() {
        assertEquals(
            R.string.error_rate_limit,
            getMessageForProxerException(ProxerException(SERVER, RATE_LIMIT)),
        )
    }

    @Test
    fun `server ip blocked maps to captcha error`() {
        assertEquals(
            R.string.error_captcha,
            getMessageForProxerException(ProxerException(SERVER, IP_BLOCKED)),
        )
    }
}
