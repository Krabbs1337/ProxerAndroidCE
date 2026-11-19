package me.proxer.app.bookmark

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import me.proxer.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression test for the bookmark grid (ROADMAP 11.10): covers must be fully visible.
 *
 * The image uses a portrait aspect box (`ari_ratio` = width / height = 0.7, matching
 * typical Proxer covers) with `fitCenter`, so portrait covers are shown completely
 * instead of being cropped top/bottom by a landscape box with `centerCrop`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BookmarkItemLayoutTest {
    @Test
    fun `bookmark cover is fully visible in a portrait box`() {
        val context = RuntimeEnvironment.getApplication()
        context.setTheme(R.style.Theme_App)

        val view = LayoutInflater.from(context).inflate(R.layout.item_bookmark, null)
        val image = view.findViewById<ImageView>(R.id.image)

        assertEquals(ImageView.ScaleType.FIT_CENTER, image.scaleType)

        val width = 540
        image.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )

        assertEquals((width / 0.7f).toInt(), image.measuredHeight)
        assertTrue("cover box must be portrait, not landscape", image.measuredHeight > width)
    }
}
