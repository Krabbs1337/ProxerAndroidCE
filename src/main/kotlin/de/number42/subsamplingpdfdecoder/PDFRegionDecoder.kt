package de.number42.subsamplingpdfdecoder

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import java.io.File

/**
 * In-repo replacement for the jitpack `subsampling-pdf-decoder` artifact, backed by the
 * platform [PdfRenderer] (API 21+). Same public API as the original class.
 */
class PDFRegionDecoder(
    private val position: Int,
    private val file: File,
    private val scale: Float,
    private val backgroundColorPdf: Int = Color.WHITE,
) : ImageRegionDecoder {
    private var renderer: PdfRenderer? = null
    private var page: PdfRenderer.Page? = null

    override fun init(
        context: Context,
        uri: Uri,
    ): Point {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        renderer = PdfRenderer(descriptor)
        page = renderer?.openPage(position)

        return page?.let { Point((it.width * scale).toInt(), (it.height * scale).toInt()) } ?: Point(0, 0)
    }

    override fun decodeRegion(
        sRect: Rect,
        sampleSize: Int,
    ): Bitmap {
        val currentPage = page ?: error("PDFRegionDecoder used before init")

        return createBitmap(
            sRect.width() / sampleSize,
            sRect.height() / sampleSize,
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
            val factor = scale / sampleSize

            Canvas(bitmap).drawColor(backgroundColorPdf)

            currentPage.render(
                bitmap,
                null,
                Matrix().apply {
                    setScale(factor, factor)
                    postTranslate(-sRect.left * factor, -sRect.top * factor)
                },
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
            )
        }
    }

    override fun isReady() = page != null

    override fun recycle() {
        page?.close()
        page = null

        renderer?.close()
        renderer = null
    }
}
