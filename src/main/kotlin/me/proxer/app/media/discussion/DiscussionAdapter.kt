package me.proxer.app.media.discussion

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.set
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import io.reactivex.subjects.PublishSubject
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.media.discussion.DiscussionAdapter.ViewHolder
import me.proxer.app.util.extension.fastText
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.library.entity.info.ForumDiscussion
import me.proxer.app.databinding.ItemDiscussionBinding

/**
 * @author Ruben Gees
 */
class DiscussionAdapter : BaseAdapter<ForumDiscussion, ViewHolder>() {
    val clickSubject: PublishSubject<ForumDiscussion> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_discussion, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) = holder.bind(data[position])

    inner class ViewHolder(
        itemView: View,
    ) : AutoDisposeViewHolder(itemView) {
        private val binding = ItemDiscussionBinding.bind(itemView)

        internal val container: ViewGroup get() = binding.container
        internal val subject: TextView get() = binding.subject
        internal val metaInfo: AppCompatTextView get() = binding.metaInfo as AppCompatTextView

        fun bind(item: ForumDiscussion) {
            container
                .clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[it] }
                .autoDispose(this)
                .subscribe(clickSubject)

            val metaInfoText =
                metaInfo.context.getString(
                    R.string.fragment_discussion_meta_info,
                    item.firstPostUsername,
                    item.categoryName,
                )

            subject.text = item.subject
            metaInfo.fastText =
                SpannableString(metaInfoText).apply {
                    val usernameSpanStart = indexOf(item.firstPostUsername)
                    val usernameSpanEnd = usernameSpanStart + item.firstPostUsername.length
                    val categorySpanStart = indexOf(item.categoryName)
                    val categorySpanEnd = categorySpanStart + item.categoryName.length

                    this[usernameSpanStart..usernameSpanEnd] = StyleSpan(Typeface.BOLD)
                    this[categorySpanStart..categorySpanEnd] = StyleSpan(Typeface.BOLD)
                }
        }
    }
}
