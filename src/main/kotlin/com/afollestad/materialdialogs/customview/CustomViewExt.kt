package com.afollestad.materialdialogs.customview

import android.view.LayoutInflater
import android.widget.ScrollView
import androidx.annotation.LayoutRes
import com.afollestad.materialdialogs.MaterialDialog

/**
 * In-repo replacement for the `customview` extension of the deprecated material-dialogs lib.
 */
fun MaterialDialog.customView(
    @LayoutRes layoutRes: Int,
    scrollable: Boolean = false,
): MaterialDialog {
    val view = LayoutInflater.from(context).inflate(layoutRes, null)
    val content = if (scrollable) ScrollView(context).apply { addView(view) } else view

    customContentView = content
    setView(content)

    return this
}
