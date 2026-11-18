package com.santalu.aspectratioimageview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import me.proxer.app.R

/**
 * In-repo replacement for `com.github.santalu:aspect-ratio-imageview` (jitpack-only).
 * Keeps the measured width and derives the height from `ari_ratio` (width / height).
 */
class AspectRatioImageView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AppCompatImageView(context, attrs, defStyleAttr) {
        private var ratio = 1f

        init {
            context.withStyledAttributes(attrs, R.styleable.AspectRatioImageView) {
                ratio = getFloat(R.styleable.AspectRatioImageView_ari_ratio, ratio)
            }
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val width = MeasureSpec.getSize(widthMeasureSpec)

            if (width > 0 && ratio > 0) {
                super.onMeasure(
                    widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec((width / ratio).toInt(), MeasureSpec.EXACTLY),
                )
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }
    }
