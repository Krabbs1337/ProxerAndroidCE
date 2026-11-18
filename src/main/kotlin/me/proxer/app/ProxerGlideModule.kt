package me.proxer.app

import android.content.Context
import android.os.Environment
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import java.io.File
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.safeInject
import okhttp3.OkHttpClient
import java.io.InputStream

/**
 * @author Ruben Gees
 */
@GlideModule
class ProxerGlideModule : AppGlideModule() {
    private companion object {
        private const val CACHE_SIZE = 1_024L * 1_024L * 250L
        private const val CACHE_DIR = "glide"
    }

    private val client by safeInject<OkHttpClient>()
    private val preferenceHelper by safeInject<PreferenceHelper>()

    override fun applyOptions(
        context: Context,
        builder: GlideBuilder,
    ) {
        // Hardware bitmaps and ARGB_8888 are Glide's defaults since 4.x; keeping them saves
        // RAM/GPU memory pressure compared to disallowHardwareConfig() + PREFER_RGB_565.
        builder.setLogRequestOrigins(BuildConfig.DEBUG)

        val useExternalCache = !Environment.isExternalStorageEmulated() && preferenceHelper.shouldCacheExternally

        val diskCacheFactory: DiskCache.Factory =
            when (useExternalCache) {
                true -> {
                    DiskLruCacheFactory(
                        { File(context.externalCacheDir ?: context.cacheDir, CACHE_DIR) },
                        CACHE_SIZE,
                    )
                }

                false -> {
                    InternalCacheDiskCacheFactory(context, CACHE_DIR, CACHE_SIZE)
                }
            }

        builder.setDiskCache(diskCacheFactory)
    }

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry,
    ) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
    }

    override fun isManifestParsingEnabled() = false
}
