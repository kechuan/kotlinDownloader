package com.example.kotlinDownloader.internal

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Url


class HttpRequestClient{

    companion object{

        val githubClient: Retrofit =
            Retrofit
                .Builder()
                .baseUrl(GithubServerLink.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}

object GithubServerLink{
    const val baseUrl = "https://github.com/";
}


interface DownloadApi{
    @HEAD
    suspend fun getCustomHead(@Url url: String):Response<Void>

    @GET
    suspend fun getCustomUrl(
        @Url url: String,
        @Header("Range") range: String? = null,
    ):Response<ResponseBody>
}
