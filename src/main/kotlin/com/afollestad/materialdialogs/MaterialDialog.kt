package com.afollestad.materialdialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * In-repo replacement for `com.afollestad.material-dialogs:core` (deprecated, jitpack-era).
 * Backed by [AlertDialog]; only the fluent API surface actually used by the app is implemented.
 * Used as the [android.app.Dialog] returned from `DialogFragment.onCreateDialog`.
 */
class MaterialDialog(
    context: Context,
) : AlertDialog(context) {
    private var noAutoDismiss = false
    private val buttonHandlers = HashMap<Int, ((MaterialDialog) -> Unit)?>()

    // Custom content set via customView()/customListAdapter() before show(); findViewById must
    // see it already in onDialogCreated, which runs before the dialog window is installed.
    internal var customContentView: android.view.View? = null

    override fun <T : android.view.View> findViewById(id: Int): T? =
        super.findViewById(id) ?: customContentView?.findViewById(id)

    fun noAutoDismiss() = apply { noAutoDismiss = true }

    fun title(
        res: Int = 0,
        text: CharSequence? = null,
    ) = apply {
        when {
            res != 0 -> setTitle(res)
            text != null -> setTitle(text)
        }
    }

    fun message(
        res: Int = 0,
        text: CharSequence? = null,
    ) = apply {
        when {
            res != 0 -> setMessage(context.getString(res))
            text != null -> setMessage(text)
        }
    }

    fun positiveButton(
        res: Int = 0,
        text: String? = null,
        handler: ((MaterialDialog) -> Unit)? = null,
    ) = button(BUTTON_POSITIVE, res, text, handler)

    fun negativeButton(
        res: Int = 0,
        text: String? = null,
        handler: ((MaterialDialog) -> Unit)? = null,
    ) = button(BUTTON_NEGATIVE, res, text, handler)

    fun neutralButton(
        res: Int = 0,
        text: String? = null,
        handler: ((MaterialDialog) -> Unit)? = null,
    ) = button(BUTTON_NEUTRAL, res, text, handler)

    private fun button(
        which: Int,
        res: Int,
        text: String?,
        handler: ((MaterialDialog) -> Unit)?,
    ) = apply {
        val label = text ?: context.getString(res)
        setButton(which, label) { _, _ -> handler?.invoke(this) }
        buttonHandlers[which] = handler
    }

    override fun show() {
        super.show()

        if (noAutoDismiss) {
            // Replace the default button listeners (which auto-dismiss) so the
            // handler alone decides, matching afollestad's noAutoDismiss().
            for ((which, handler) in buttonHandlers) {
                getButton(which)?.setOnClickListener { handler?.invoke(this) }
            }
        }
    }
}
