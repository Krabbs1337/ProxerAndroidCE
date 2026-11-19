package me.proxer.app.util.data

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.Instant
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [InstantJsonAdapter].
 */
class InstantJsonAdapterTest {
    private val adapter = InstantJsonAdapter()

    @Test
    fun `fromJson reads epoch milliseconds`() {
        val reader = JsonReader.of(Buffer().writeUtf8("1700000000000"))

        assertEquals(Instant.ofEpochMilli(1700000000000L), adapter.fromJson(reader))
    }

    @Test
    fun `fromJson reads null`() {
        val reader = JsonReader.of(Buffer().writeUtf8("null"))

        assertNull(adapter.fromJson(reader))
    }

    @Test
    fun `toJson writes epoch milliseconds`() {
        val buffer = Buffer()

        adapter.toJson(JsonWriter.of(buffer), Instant.ofEpochMilli(42L))

        assertEquals("42", buffer.readUtf8())
    }

    @Test
    fun `toJson writes null`() {
        val buffer = Buffer()

        adapter.toJson(JsonWriter.of(buffer), null)

        assertEquals("null", buffer.readUtf8())
    }
}
