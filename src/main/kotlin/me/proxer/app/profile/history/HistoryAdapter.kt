package me.proxer.app.profile.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.uber.autodispose.autoDispose
import io.reactivex.subjects.PublishSubject
import com.bumptech.glide.RequestManager
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.history.HistoryAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.toAppString
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import me.proxer.app.databinding.ItemHistoryEntryBinding

/**
 * @author Ruben Gees
 */
class HistoryAdapter : BaseAdapter<LocalUserHistoryEntry, ViewHolder>() {
    var glide: RequestManager? = null
    val clickSubject: PublishSubject<Pair<ImageView, LocalUserHistoryEntry>> = PublishSubject.create()
    val longClickSubject: PublishSubject<Pair<ImageView, LocalUserHistoryEntry>> = PublishSubject.create()

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history_entry, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(
        itemView: View,
    ) : AutoDisposeViewHolder(itemView) {
        private val binding = ItemHistoryEntryBinding.bind(itemView)

        internal val container: ViewGroup get() = binding.container
        internal val title: TextView get() = binding.title
        internal val medium: TextView get() = binding.medium
        internal val image: ImageView get() = binding.image
        internal val status: TextView get() = binding.status

        fun bind(item: LocalUserHistoryEntry) {
            container
                .clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDispose(this)
                .subscribe(clickSubject)

            container
                .longClicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDispose(this)
                .subscribe(longClickSubject)

            ViewCompat.setTransitionName(image, "history_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)

            status.text =
                when (item is LocalUserHistoryEntry.Ucp) {
                    true -> {
                        status.context.getString(
                            when (item.category) {
                                Category.ANIME -> R.string.fragment_history_entry_ucp_status_anime
                                Category.MANGA, Category.NOVEL -> R.string.fragment_history_entry_ucp_status_manga
                            },
                            item.episode,
                            item.date.distanceInWordsToNow(status.context),
                        )
                    }

                    false -> {
                        status.context.getString(
                            when (item.category) {
                                Category.ANIME -> R.string.fragment_history_entry_status_anime
                                Category.MANGA, Category.NOVEL -> R.string.fragment_history_entry_status_manga
                            },
                            item.episode,
                        )
                    }
                }

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }
    }
}
