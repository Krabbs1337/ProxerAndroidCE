package me.proxer.app.anime.stream

import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import java.util.Locale

/**
 * Rewind/fast-forward indicator and volume/brightness overlay of [StreamActivity].
 *
 * @author Ruben Gees
 */
internal fun StreamActivity.resetIndicator(view: TextView) {
    view.background.state = intArrayOf()
    view.isVisible = false
    view.text = ""

    view.jumpDrawablesToCurrentState()

    hideIndicatorHandler.removeCallbacksAndMessages(null)
    animationHandler.removeCallbacksAndMessages(null)
}

internal fun StreamActivity.updateIndicator(
    view: TextView,
    animate: Boolean = true,
) {
    val previousDuration = view.text.toString().toIntOrNull()
    val wasVisible = view.isVisible

    view.isVisible = true
    view.text = String.format(Locale.ROOT, "%d", (previousDuration ?: 0) + 10)

    if (animate) {
        view.background.state =
            intArrayOf(
                android.R.attr.state_pressed,
                android.R.attr.state_enabled,
            )

        animationHandler.postDelayed(animationTime) {
            view.background.state = intArrayOf()
        }
    }

    if (wasVisible) {
        hideIndicatorHandler.removeCallbacksAndMessages(null)
    }

    hideIndicatorHandler.postDelayed(1_000) {
        resetIndicator(view)
    }
}

internal fun StreamActivity.updateControl(
    value: Int,
    icon: IIcon,
) {
    hideControlHandler.removeCallbacksAndMessages(null)

    controlProgress.isVisible = true
    controlIcon.isVisible = true

    controlProgress.progress = value
    controlIcon.setImageDrawable(
        IconicsDrawable(this, icon).apply {
            colorRes = android.R.color.white
            sizeDp = 64
            paddingDp = 12
        },
    )

    hideControlHandler.postDelayed(1_000) {
        controlProgress.isVisible = false
        controlIcon.isVisible = false
    }
}

internal fun StreamActivity.generateIndicatorIcon(icon: IIcon) =
    IconicsDrawable(this, icon).apply {
        colorRes = android.R.color.white
        paddingDp = 4
        sizeDp = 36
    }

internal fun StreamActivity.generateControllerIcon(icon: IIcon) =
    IconicsDrawable(this, icon).apply {
        colorRes = android.R.color.white
        paddingDp = 8
        sizeDp = 44
    }
