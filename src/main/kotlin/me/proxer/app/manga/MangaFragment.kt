package me.proxer.app.manga

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.PagerSnapHelper
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.autoDispose
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.databinding.FragmentMangaBinding
import me.proxer.app.ui.view.MediaControlView
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Language
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaFragment : BaseContentFragment<MangaChapterInfo>(R.layout.fragment_manga) {
    private val binding: FragmentMangaBinding
        get() = FragmentMangaBinding.bind(requireView())

    companion object {
        private const val LAST_POSITION_STATE = "fragment_manga_last_position"
        private const val LOW_MEMORY_STATE = "fragment_manga_low_memory"

        fun newInstance() =
            MangaFragment().apply {
                arguments = bundleOf()
            }
    }

    override val viewModel by activityViewModel<MangaViewModel> { parametersOf(id, language, episode) }

    override val hostingActivity: MangaActivity
        get() = activity as MangaActivity

    internal val id: String
        get() = hostingActivity.id

    internal var episode: Int
        get() = hostingActivity.episode
        set(value) {
            hostingActivity.episode = value

            viewModel.setEpisode(value)
        }

    internal val language: Language
        get() = hostingActivity.language

    internal var chapterTitle: String?
        get() = hostingActivity.chapterTitle
        set(value) {
            hostingActivity.chapterTitle = value
        }

    internal var name: String?
        get() = hostingActivity.name
        set(value) {
            hostingActivity.name = value
        }

    internal var episodeAmount: Int?
        get() = hostingActivity.episodeAmount
        set(value) {
            hostingActivity.episodeAmount = value
        }

    internal var hasLowMemory = false
    internal var lastPosition: Parcelable? = null

    internal val mediaControlTextResolver =
        object : MediaControlView.TextResourceResolver {
            override fun next() = requireContext().getString(R.string.fragment_manga_next_chapter)

            override fun previous() = requireContext().getString(R.string.fragment_manga_previous_chapter)

            override fun bookmarkThis() = requireContext().getString(R.string.fragment_manga_bookmark_this_chapter)

            override fun bookmarkNext() = requireContext().getString(R.string.fragment_manga_bookmark_next_chapter)
        }

    internal var preloader by Delegates.notNull<MangaPreloader>()
    internal var innerAdapter by Delegates.notNull<MangaAdapter>()
    internal var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    internal var header by Delegates.notNull<MediaControlView>()
    internal var footer by Delegates.notNull<MediaControlView>()

    internal var snapHelper: PagerSnapHelper? = null

    internal var readerOrientation = preferenceHelper.mangaReaderOrientation
        set(value) {
            preferenceHelper.mangaReaderOrientation = value

            field = value
        }

    internal val isVertical get() = readerOrientation == MangaReaderOrientation.VERTICAL
    internal val areBookmarksAutomatic by unsafeLazy { preferenceHelper.areBookmarksAutomatic }
    internal val mediumAnimationTime by unsafeLazy { resources.getInteger(android.R.integer.config_mediumAnimTime) }

    override val contentContainer: ViewGroup
        get() = recyclerView

    internal val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    internal val recyclerView: RecyclerView get() = binding.recyclerView

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastPosition =
            savedInstanceState?.let { BundleCompat.getParcelable(it, LAST_POSITION_STATE, Parcelable::class.java) }
        hasLowMemory = savedInstanceState?.getByte(LOW_MEMORY_STATE) == 1.toByte()

        preloader = MangaPreloader()
        innerAdapter = MangaAdapter(isVertical)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        setupClickHandling()

        viewModel.setEpisode(episode, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        header = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView
        footer = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupRecyclerView()
    }

    override fun onStop() {
        (recyclerView.safeLayoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()?.let {
            if (it > 0 || storageHelper.getLastMangaPage(id, episode, language) != null) {
                storageHelper.putLastMangaPage(id, episode, language, it)
            }
        }

        super.onStop()
    }

    override fun onDestroyView() {
        snapHelper?.attachToRecyclerView(null)
        snapHelper = null

        recyclerView.layoutManager = null
        recyclerView.adapter = null

        preloader.glide = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)

        outState.putParcelable(LAST_POSITION_STATE, recyclerView.safeLayoutManager.onSaveInstanceState())
        outState.putByte(LOW_MEMORY_STATE, if (hasLowMemory) 1 else 0)
    }

    override fun showData(data: MangaChapterInfo) {
        super.showData(data)

        showMangaData(data)
    }

    override fun hideData() {
        hideMangaData { super.hideData() }
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        showMangaError(action)
    }

    override fun hideError() {
        super.hideError()

        hideMangaError()
    }
}
