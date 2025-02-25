package com.example.kotlinstart.internal.android



import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer


class ProgressInterceptor(
    val progressListener: (bytesRead: Long, contentLength: Long) -> Unit
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val body = originalResponse.body

        return originalResponse.newBuilder()
            .body(
                ProgressResponseBody(body, progressListener)
            )
            .build()
    }
}

class ProgressResponseBody(
        val responseBody: ResponseBody?,
        val progressListener: (bytesRead: Long, contentLength: Long) -> Unit
) : ResponseBody() {
        val bufferedSource by lazy {
        source(responseBody?.source()!!).buffer()
    }

    override fun contentLength(): Long = responseBody?.contentLength() ?: 0

    override fun contentType() = responseBody?.contentType()

    override fun source() = bufferedSource

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            private var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener(totalBytesRead, contentLength())
                return bytesRead
            }
        }
    }
}

fun createImageLoaderWithProgress(context: Context, progressListener: (Float) -> Unit): ImageLoader {

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            ProgressInterceptor { bytesRead, contentLength ->
                val progress =

                    if (contentLength > 0) {
                        bytesRead / contentLength.toFloat()
                    }

                    else {
                        0f
                    }

                progressListener(progress)
            }
        )
        .build()

    return ImageLoader.Builder(context)

        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        okHttpClient
                    }
                )
            )

        }
        .build()
}
