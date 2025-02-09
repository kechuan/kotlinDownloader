package com.example.kotlinstart.internal

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url


class HttpRequestClient{

    companion object{
        val client =
            Retrofit
                .Builder()
                .baseUrl(ImageServerLink.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}

object ImageServerLink{
    const val baseUrl = "https://api.lolicon.app/setu/";

    const val defaultImage = "v2"

}

interface ImageApi{

    @GET(ImageServerLink.defaultImage)
    fun getDefaultData(): Call<Map<String,Any>>

    @GET
    fun getCustomUrl(@Url url: String)

}
