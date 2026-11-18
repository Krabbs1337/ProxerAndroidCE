package com.afollestad.materialdialogs.list

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog

/**
 * In-repo replacement for the `list` extension of the deprecated material-dialogs lib.
 */
fun MaterialDialog.customListAdapter(adapter: RecyclerView.Adapter<*>): MaterialDialog {
    val recyclerView =
        RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

    customContentView = recyclerView
    setView(recyclerView)

    return this
}
