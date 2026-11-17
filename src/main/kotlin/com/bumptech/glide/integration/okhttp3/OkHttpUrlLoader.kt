package com.bumptech.glide.integration.okhttp3

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher.DataCallback
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.util.ContentLengthInputStream
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.io.InputStream

/**
 * Vendored replacement for `com.github.bumptech.glide:okhttp3-integration` (Apache-2.0, © Bumptech).
 * The integration artifact cannot be used with Glide's KSP processor because its prebuilt indexer
 * classes break KSP, so the loader is provided in-repo instead.
 */
class OkHttpUrlLoader(
    private val client: Call.Factory,
) : ModelLoader<GlideUrl, InputStream> {
    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options,
    ): LoadData<InputStream>? = LoadData(model, OkHttpStreamFetcher(client, model))

    override fun handles(model: GlideUrl) = true

    class Factory
        @JvmOverloads
        constructor(
            private val client: Call.Factory = OkHttpClient(),
        ) : ModelLoaderFactory<GlideUrl, InputStream> {
            override fun build(multiFactory: MultiModelLoaderFactory) = OkHttpUrlLoader(client)

            override fun teardown() = Unit
        }
}

private class OkHttpStreamFetcher(
    private val client: Call.Factory,
    private val url: GlideUrl,
) : DataFetcher<InputStream> {
    private var stream: InputStream? = null
    private var responseBody: ResponseBody? = null

    @Volatile
    private var call: Call? = null

    override fun loadData(
        priority: Priority,
        callback: DataCallback<in InputStream>,
    ) {
        val requestBuilder = Request.Builder().url(url.toStringUrl())

        for ((key, value) in url.headers) {
            requestBuilder.addHeader(key, value)
        }

        call = client.newCall(requestBuilder.build())

        call!!.enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    callback.onLoadFailed(e)
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    responseBody = response.body

                    if (response.isSuccessful) {
                        val contentLength = requireNotNull(responseBody).contentLength()
                        stream =
                            ContentLengthInputStream.obtain(requireNotNull(responseBody).byteStream(), contentLength)

                        callback.onDataReady(stream)
                    } else {
                        callback.onLoadFailed(HttpException(response.message, response.code))
                    }
                }
            },
        )
    }

    override fun cleanup() {
        try {
            stream?.close()
        } catch (ignored: IOException) {
            // Ignored.
        }

        responseBody?.close()
        call = null
    }

    override fun cancel() {
        call?.cancel()
    }

    override fun getDataClass() = InputStream::class.java

    override fun getDataSource() = DataSource.REMOTE
}
