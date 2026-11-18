package me.proxer.app.media.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import io.reactivex.subjects.PublishSubject
import com.bumptech.glide.RequestManager
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls
import me.proxer.app.databinding.ItemMediaEntryBinding

/**
 * @author Ruben Gees
 */
class MediaAdapter(
    private val category: Category,
) : BaseAdapter<MediaListEntry, MediaAdapter.ViewHolder>() {
    var glide: RequestManager? = null
    val clickSubject: PublishSubject<Pair<ImageView, MediaListEntry>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_media_entry, parent, false),
    )

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

    override fun swapDataAndNotifyWithDiffing(newData: List<MediaListEntry>) {
        super.swapDataAndNotifyWithDiffing(newData.distinctBy { it.id })
    }

    inner class ViewHolder(
        itemView: View,
    ) : AutoDisposeViewHolder(itemView) {
        private val binding = ItemMediaEntryBinding.bind(itemView)

        internal val container: ViewGroup get() = binding.container
        internal val title: TextView get() = binding.title
        internal val medium: TextView get() = binding.medium
        internal val image: ImageView get() = binding.image
        internal val ratingContainer: ViewGroup get() = binding.ratingContainer
        internal val rating: RatingBar get() = binding.rating
        internal val state: ImageView get() = binding.state
        internal val episodes: TextView get() = binding.episodes
        internal val english: ImageView get() = binding.english
        internal val german: ImageView get() = binding.german

        fun bind(item: MediaListEntry) {
            container
                .clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDispose(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text =
                episodes.context.getQuantityString(
                    when (category) {
                        Category.ANIME -> R.plurals.media_episode_count
                        Category.MANGA, Category.NOVEL -> R.plurals.media_chapter_count
                    },
                    item.episodeAmount,
                )

            item.languages
                .asSequence()
                .map { it.toGeneralLanguage() }
                .distinct()
                .toList()
                .let { generalLanguages ->
                    english.isVisible = generalLanguages.contains(Language.ENGLISH)
                    german.isVisible = generalLanguages.contains(Language.GERMAN)
                }

            if (item.rating > 0) {
                ratingContainer.isVisible = true
                rating.rating = item.rating / 2.0f

                episodes.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    addRule(RelativeLayout.BELOW, R.id.state)
                }
            } else {
                ratingContainer.isGone = true

                episodes.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.languageContainer)
                    addRule(RelativeLayout.BELOW, R.id.medium)
                }
            }

            state.setImageDrawable(item.state.toAppDrawable(state.context))
            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
