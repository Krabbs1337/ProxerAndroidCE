@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.github.rubensousa.previewseekbar.media3

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.github.rubensousa.previewseekbar.PreviewDelegate
import com.github.rubensousa.previewseekbar.PreviewLoader
import com.github.rubensousa.previewseekbar.PreviewView
import com.github.rubensousa.previewseekbar.R as PreviewSeekBarR

/**
 * Media3 port of the `previewseekbar-exoplayer` module of rubengees' PreviewSeekBar
 * (upstream has no media3 artifact). Behavior is identical: extends [DefaultTimeBar],
 * shows the preview frame while scrubbing via [PreviewDelegate].
 */
class PreviewTimeBar(
    context: Context,
    attrs: AttributeSet?,
) : DefaultTimeBar(context, attrs),
    PreviewView,
    TimeBar.OnScrubListener {
    private val listeners = mutableListOf<PreviewView.OnPreviewChangeListener>()
    private val delegate: PreviewDelegate
    private var scrubProgress = 0
    private var duration = 0
    private val scrubberColor: Int
    private val frameLayoutId: Int
    private val scrubberDiameter: Int

    init {
        val a =
            context.theme.obtainStyledAttributes(
                attrs,
                androidx.media3.ui.R.styleable.DefaultTimeBar,
                0,
                0,
            )

        scrubberColor =
            a.getInt(
                androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_color,
                DEFAULT_SCRUBBER_COLOR,
            )

        val defaultScrubberDraggedSize =
            dpToPx(context.resources.displayMetrics, DEFAULT_SCRUBBER_DRAGGED_SIZE_DP)

        scrubberDiameter =
            a.getDimensionPixelSize(
                androidx.media3.ui.R.styleable.DefaultTimeBar_scrubber_dragged_size,
                defaultScrubberDraggedSize,
            )

        a.recycle()

        val b = context.theme.obtainStyledAttributes(attrs, PreviewSeekBarR.styleable.PreviewSeekBar, 0, 0)
        frameLayoutId = b.getResourceId(PreviewSeekBarR.styleable.PreviewSeekBar_previewFrameLayout, View.NO_ID)
        b.recycle()

        delegate = PreviewDelegate(this, scrubberColor)
        delegate.setEnabled(isEnabled)
        addListener(this)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)

        if (!delegate.isSetup() && width != 0 && height != 0 && !isInEditMode) {
            delegate.onLayout(parent as ViewGroup, frameLayoutId)
        }
    }

    override fun setPreviewColorTint(color: Int) {
        delegate.setPreviewColorTint(color)
    }

    override fun setPreviewColorResourceTint(color: Int) {
        delegate.setPreviewColorResourceTint(color)
    }

    override fun setPreviewLoader(previewLoader: PreviewLoader?) {
        delegate.setPreviewLoader(previewLoader)
    }

    override fun attachPreviewFrameLayout(frameLayout: FrameLayout) {
        delegate.attachPreviewFrameLayout(frameLayout)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        delegate.setEnabled(enabled)
    }

    override fun setDuration(duration: Long) {
        super.setDuration(duration)
        this.duration = duration.toInt()
    }

    override fun setPosition(position: Long) {
        super.setPosition(position)
        scrubProgress = position.toInt()
    }

    override fun isShowingPreview(): Boolean = delegate.isShowing()

    override fun showPreview() {
        if (isEnabled) {
            delegate.show()
        }
    }

    override fun hidePreview() {
        if (isEnabled) {
            delegate.hide()
        }
    }

    override fun getProgress(): Int = scrubProgress

    override fun getMax(): Int = duration

    override fun getThumbOffset(): Int = scrubberDiameter / 2

    override fun getDefaultColor(): Int = scrubberColor

    override fun addOnPreviewChangeListener(listener: PreviewView.OnPreviewChangeListener) {
        listeners.add(listener)
    }

    override fun removeOnPreviewChangeListener(listener: PreviewView.OnPreviewChangeListener) {
        listeners.remove(listener)
    }

    override fun onScrubStart(
        timeBar: TimeBar,
        position: Long,
    ) {
        for (listener in listeners) {
            scrubProgress = position.toInt()
            listener.onStartPreview(this, position.toInt())
        }
    }

    override fun onScrubMove(
        timeBar: TimeBar,
        position: Long,
    ) {
        for (listener in listeners) {
            scrubProgress = position.toInt()
            listener.onPreview(this, position.toInt(), true)
        }
    }

    override fun onScrubStop(
        timeBar: TimeBar,
        position: Long,
        canceled: Boolean,
    ) {
        for (listener in listeners) {
            listener.onStopPreview(this, position.toInt())
        }
    }

    private fun dpToPx(
        displayMetrics: DisplayMetrics,
        dps: Int,
    ): Int = (dps * displayMetrics.density + 0.5f).toInt()
}
