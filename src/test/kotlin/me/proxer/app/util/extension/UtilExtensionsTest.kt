package me.proxer.app.util.extension

import me.proxer.library.ProxerException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for the URL-prefixing helpers in [UtilExtensions.kt].
 */
class UtilExtensionsTest {
    @Test
    fun `toPrefixedUrlOrNull keeps absolute http urls unchanged`() {
        assertEquals("https://proxer.me/x", "https://proxer.me/x".toPrefixedUrlOrNull()?.toString())
        assertEquals("http://proxer.me/x", "http://proxer.me/x".toPrefixedUrlOrNull()?.toString())
    }

    @Test
    fun `toPrefixedUrlOrNull prefixes bare hosts with http`() {
        assertEquals("http://example.com/x", "example.com/x".toPrefixedUrlOrNull()?.toString())
    }

    @Test
    fun `toPrefixedUrlOrNull handles protocol-relative urls`() {
        assertEquals("http://example.com/x", "//example.com/x".toPrefixedUrlOrNull()?.toString())
    }

    @Test
    fun `toPrefixedUrlOrNull returns null for invalid urls`() {
        assertNull("".toPrefixedUrlOrNull())
        assertNull("http://".toPrefixedUrlOrNull())
    }

    @Test
    fun `toPrefixedHttpUrl throws ProxerException with PARSING type for invalid urls`() {
        val error = assertThrows<ProxerException> { "http://".toPrefixedHttpUrl() }

        assertEquals(ProxerException.ErrorType.PARSING, error.errorType)
    }
}
