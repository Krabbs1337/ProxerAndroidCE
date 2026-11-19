package me.proxer.app.media

import android.app.Application
import androidx.room.Room
import me.proxer.library.enums.TagSubType
import me.proxer.library.enums.TagType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Room tests for [TagDatabase]/[TagDao] on an in-memory database (Robolectric provides
 * a working SQLite implementation).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TagDatabaseTest {
    private lateinit var db: TagDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TagDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tag(
        id: String,
        name: String = "name-$id",
        isSpoiler: Boolean = false,
    ) = LocalTag(id, TagType.GENRE, name, "description-$id", TagSubType.SUPERNATURAL, isSpoiler)

    @Test
    fun `insert and query tags roundtrip all fields`() {
        val tags = listOf(tag("1"), tag("2", isSpoiler = true))

        db.dao().insertTags(tags)

        assertEquals(tags.sortedBy { it.id }, db.dao().getTags().sortedBy { it.id })
    }

    @Test
    fun `insert with same primary key replaces the row`() {
        db.dao().insertTags(listOf(tag("1", "old")))
        db.dao().insertTags(listOf(tag("1", "new")))

        val result = db.dao().getTags()

        assertEquals(1, result.size)
        assertEquals("new", result.single().name)
    }

    @Test
    fun `replaceTags clears existing tags first`() {
        db.dao().insertTags(listOf(tag("1"), tag("2")))

        db.dao().replaceTags(listOf(tag("3")))

        assertEquals(listOf("3"), db.dao().getTags().map { it.id })
    }

    @Test
    fun `clear removes all tags`() {
        db.dao().insertTags(listOf(tag("1"), tag("2")))

        db.dao().clear()

        assertTrue(db.dao().getTags().isEmpty())
    }
}
