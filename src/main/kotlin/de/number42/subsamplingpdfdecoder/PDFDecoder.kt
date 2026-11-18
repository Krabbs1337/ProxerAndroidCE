package de.number42.subsamplingpdfdecoder

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder
import java.io.File

/**
 * In-repo replacement for the jitpack `subsampling-pdf-decoder` artifact, backed by the
 * platform [PdfRenderer] (API 21+). Same public API as the original class.
 */
class PDFDecoder(
    private val position: Int,
    private val file: File,
    private val scale: Float,
) : ImageDecoder {
    override fun decode(
        context: Context,
        uri: Uri,
    ): Bitmap {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(position).use { page ->
                    return createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888,
                    ).also { bitmap ->
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }
}
