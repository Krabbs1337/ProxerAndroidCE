package com.afollestad.materialdialogs.callbacks

import com.afollestad.materialdialogs.MaterialDialog

/**
 * In-repo replacement for the `callbacks` extensions of the deprecated material-dialogs lib.
 */
fun MaterialDialog.onCancel(listener: () -> Unit): MaterialDialog {
    setOnCancelListener { listener() }

    return this
}
