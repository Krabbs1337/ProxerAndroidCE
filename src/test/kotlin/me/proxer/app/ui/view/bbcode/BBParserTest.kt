package me.proxer.app.ui.view.bbcode

import android.app.Application
import me.proxer.app.ui.view.bbcode.prototype.BoldPrototype
import me.proxer.app.ui.view.bbcode.prototype.ImagePrototype
import me.proxer.app.ui.view.bbcode.prototype.ItalicPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [BBParser]. Runs on Robolectric because [TextPrototype.construct] linkifies
 * text via androidx.core, which needs a real android.text implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BBParserTest {
    private fun flatten(tree: BBTree): String =
        buildString {
            if (tree.prototype is TextPrototype) {
                append(tree.args.safeText)
            }

            tree.children.forEach { append(flatten(it)) }
        }

    @Test
    fun `plain text becomes a single text node`() {
        val tree = BBParser.parse("Hallo Welt")
        val child = tree.children.single()

        assertEquals(1, tree.children.size)
        assertSame(TextPrototype, child.prototype)
        assertEquals("Hallo Welt", child.args.safeText.toString())
    }

    @Test
    fun `bold tag creates a bold node containing the text`() {
        val tree = BBParser.parse("[b]Hallo[/b]")
        val bold = tree.children.single()

        assertSame(BoldPrototype, bold.prototype)
        assertEquals("Hallo", flatten(bold))
    }

    @Test
    fun `nested tags keep their text in order`() {
        val tree = BBParser.parse("[b]a[i]b[/i]c[/b]")
        val bold = tree.children.single()

        assertSame(BoldPrototype, bold.prototype)
        assertEquals("abc", flatten(bold))
        assertSame(ItalicPrototype, bold.children[1].prototype)
    }

    @Test
    fun `image tag is parsed in default mode`() {
        val tree = BBParser.parse("[img]https://example.com/x.png[/img]")

        assertSame(ImagePrototype, tree.children.single().prototype)
    }

    @Test
    fun `unknown tag stays literal text in simple mode`() {
        val tree = BBParser.parseSimple("[img]https://example.com/x.png[/img]")

        assertEquals("[img]https://example.com/x.png[/img]", flatten(tree))
    }
}
