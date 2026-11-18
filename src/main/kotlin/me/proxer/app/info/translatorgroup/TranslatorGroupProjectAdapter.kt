package me.proxer.app.info.translatorgroup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import io.reactivex.subjects.PublishSubject
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.info.translatorgroup.TranslatorGroupProjectAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.list.TranslatorGroupProject
import me.proxer.library.util.ProxerUrls
import me.proxer.app.databinding.ItemProjectBinding

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectAdapter : BaseAdapter<TranslatorGroupProject, ViewHolder>() {
    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, TranslatorGroupProject>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))

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
        private val binding = ItemProjectBinding.bind(itemView)

        internal val container: ViewGroup get() = binding.container
        internal val title: TextView get() = binding.title
        internal val medium: TextView get() = binding.medium
        internal val image: ImageView get() = binding.image
        internal val ratingContainer: ViewGroup get() = binding.ratingContainer
        internal val rating: RatingBar get() = binding.rating
        internal val status: TextView get() = binding.status

        fun bind(item: TranslatorGroupProject) {
            container
                .clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDispose(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "translator_group_project_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = item.state.toAppString(status.context)

            if (item.rating > 0) {
                ratingContainer.isVisible = true
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.isGone = true
            }

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
