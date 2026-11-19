package me.proxer.app.util.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [UniqueQueue].
 */
class UniqueQueueTest {
    @Test
    fun `offer returns false for duplicates and keeps size`() {
        val queue = UniqueQueue<String>()

        assertTrue(queue.offer("a"))
        assertFalse(queue.offer("a"))
        assertEquals(1, queue.size)
    }

    @Test
    fun `addAll deduplicates`() {
        val queue = UniqueQueue<Int>()

        queue.addAll(listOf(1, 2, 2, 3, 3, 3))

        assertEquals(3, queue.size)
    }

    @Test
    fun `poll returns elements in insertion order`() {
        val queue = UniqueQueue<Int>()
        queue.addAll(listOf(1, 2, 3))

        assertEquals(1, queue.poll())
        assertEquals(2, queue.poll())
        assertEquals(3, queue.poll())
        assertNull(queue.poll())
    }

    @Test
    fun `element throws on empty queue`() {
        assertThrows<NoSuchElementException> { UniqueQueue<String>().element() }
    }

    @Test
    fun `peek returns head without removing`() {
        val queue = UniqueQueue<String>()
        queue.offer("a")

        assertEquals("a", queue.peek())
        assertEquals(1, queue.size)
    }
}
