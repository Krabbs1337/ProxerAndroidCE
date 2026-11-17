package com.rubengees.easyheaderfooteradapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * In-repo replacement for `com.github.rubengees:easy-header-footer-adapter` (the published versions
 * still depend on the removed support library), implemented on top of AndroidX RecyclerView.
 *
 * Wraps an inner adapter and optionally prepends a header and/or appends a footer view.
 */
class EasyHeaderFooterAdapter(
    private val inner: RecyclerView.Adapter<*>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var header: View? = null
        set(value) {
            val old = field
            field = value

            when {
                old == null && value != null -> notifyItemInserted(0)
                old != null && value == null -> notifyItemRemoved(0)
                else -> notifyItemChanged(0)
            }
        }

    var footer: View? = null
        set(value) {
            val old = field
            field = value

            when {
                old == null && value != null -> notifyItemInserted(itemCount - 1)
                old != null && value == null -> notifyItemRemoved(itemCount)
                else -> notifyItemChanged(itemCount - 1)
            }
        }

    init {
        @Suppress("UNCHECKED_CAST")
        (inner as RecyclerView.Adapter<RecyclerView.ViewHolder>).registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() = notifyDataSetChanged()

                override fun onItemRangeChanged(
                    positionStart: Int,
                    itemCount: Int,
                ) = notifyItemRangeChanged(positionStart + headerOffset, itemCount)

                override fun onItemRangeChanged(
                    positionStart: Int,
                    itemCount: Int,
                    payload: Any?,
                ) = notifyItemRangeChanged(positionStart + headerOffset, itemCount, payload)

                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int,
                ) = notifyItemRangeInserted(positionStart + headerOffset, itemCount)

                override fun onItemRangeRemoved(
                    positionStart: Int,
                    itemCount: Int,
                ) = notifyItemRangeRemoved(positionStart + headerOffset, itemCount)

                override fun onItemRangeMoved(
                    fromPosition: Int,
                    toPosition: Int,
                    itemCount: Int,
                ) = notifyItemMoved(fromPosition + headerOffset, toPosition + headerOffset)
            },
        )
    }

    fun getRealPosition(position: Int) = position - headerOffset

    private val headerOffset: Int get() = if (header != null) 1 else 0

    private val hasFooter: Boolean get() = footer != null

    override fun getItemCount() = inner.itemCount + headerOffset + if (hasFooter) 1 else 0

    override fun getItemViewType(position: Int): Int =
        when {
            header != null && position == 0 -> TYPE_HEADER
            hasFooter && position == itemCount - 1 -> TYPE_FOOTER
            else -> inner.getItemViewType(getRealPosition(position))
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> ViewHolder(header!!)
            TYPE_FOOTER -> ViewHolder(footer!!)
            else -> createInnerViewHolder(parent, viewType)
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (getItemViewType(position) == TYPE_HEADER || getItemViewType(position) == TYPE_FOOTER) {
            return
        }

        bindInnerViewHolder(holder, getRealPosition(position))
    }

    @Suppress("UNCHECKED_CAST")
    private fun createInnerViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        (inner as RecyclerView.Adapter<RecyclerView.ViewHolder>).onCreateViewHolder(parent, viewType)

    @Suppress("UNCHECKED_CAST")
    private fun bindInnerViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) = (inner as RecyclerView.Adapter<RecyclerView.ViewHolder>).onBindViewHolder(holder, position)

    private class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view)

    private companion object {
        const val TYPE_HEADER = Int.MIN_VALUE
        const val TYPE_FOOTER = Int.MIN_VALUE + 1
    }
}
