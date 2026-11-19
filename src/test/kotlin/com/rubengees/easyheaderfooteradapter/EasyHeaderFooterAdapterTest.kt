package com.rubengees.easyheaderfooteradapter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for [EasyHeaderFooterAdapter], including a regression test for the ROADMAP 11.6 bug:
 * setting `footer = null` when it is already null on an empty adapter must not notify
 * `notifyItemChanged(-1)` (that crashed layout in `LazySpanLookup.invalidateAfter`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EasyHeaderFooterAdapterTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun innerAdapter(count: Int): RecyclerView.Adapter<RecyclerView.ViewHolder> =
        object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(View(context)) {}

            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder,
                position: Int,
            ) = Unit

            override fun getItemCount() = count
        }

    private class RecordingObserver : RecyclerView.AdapterDataObserver() {
        val events = mutableListOf<String>()

        override fun onItemRangeInserted(
            positionStart: Int,
            itemCount: Int,
        ) {
            events += "inserted($positionStart,$itemCount)"
        }

        override fun onItemRangeRemoved(
            positionStart: Int,
            itemCount: Int,
        ) {
            events += "removed($positionStart,$itemCount)"
        }

        override fun onItemRangeChanged(
            positionStart: Int,
            itemCount: Int,
        ) {
            events += "changed($positionStart,$itemCount)"
        }
    }

    @Test
    fun `setting footer to null when already null on empty adapter fires no events`() {
        val adapter = EasyHeaderFooterAdapter(innerAdapter(0))
        val observer = RecordingObserver()
        adapter.registerAdapterDataObserver(observer)

        adapter.footer = null

        assertTrue(observer.events.isEmpty())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `footer add and remove on empty adapter notify position zero`() {
        val adapter = EasyHeaderFooterAdapter(innerAdapter(0))
        val observer = RecordingObserver()
        adapter.registerAdapterDataObserver(observer)

        adapter.footer = View(context)
        assertEquals(1, adapter.itemCount)
        assertEquals(listOf("inserted(0,1)"), observer.events)

        adapter.footer = null
        assertEquals(0, adapter.itemCount)
        assertEquals(listOf("inserted(0,1)", "removed(0,1)"), observer.events)
    }

    @Test
    fun `footer positions account for inner item count`() {
        val adapter = EasyHeaderFooterAdapter(innerAdapter(2))
        val observer = RecordingObserver()
        adapter.registerAdapterDataObserver(observer)

        adapter.footer = View(context)
        assertEquals(3, adapter.itemCount)
        assertEquals(listOf("inserted(2,1)"), observer.events)

        adapter.footer = null
        assertEquals(2, adapter.itemCount)
        assertEquals(listOf("inserted(2,1)", "removed(2,1)"), observer.events)
    }

    @Test
    fun `header add replace and remove notify position zero`() {
        val adapter = EasyHeaderFooterAdapter(innerAdapter(1))
        val observer = RecordingObserver()
        adapter.registerAdapterDataObserver(observer)

        adapter.header = View(context)
        assertEquals(2, adapter.itemCount)

        adapter.header = View(context)
        assertEquals(2, adapter.itemCount)

        adapter.header = null
        assertEquals(1, adapter.itemCount)

        assertEquals(listOf("inserted(0,1)", "changed(0,1)", "removed(0,1)"), observer.events)
    }

    @Test
    fun `layout with staggered grid does not crash when footer is unset on empty adapter`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val recycler = RecyclerView(activity)
        recycler.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        val adapter = EasyHeaderFooterAdapter(innerAdapter(0))
        recycler.adapter = adapter
        activity.setContentView(recycler)

        adapter.footer = null

        recycler.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        recycler.layout(0, 0, 1080, 1920)

        assertEquals(0, adapter.itemCount)
    }
}
