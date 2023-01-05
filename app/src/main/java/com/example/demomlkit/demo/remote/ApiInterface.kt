package com.google.mlkit.vision.demo.remote

import com.google.gson.GsonBuilder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit


interface ApiInterface {

    @Multipart
    @POST("video")
    fun uploadVideo(
        @Part video: MultipartBody.Part
    ): Call<BaseBean>

    companion object {
        fun createApi(): ApiInterface {
            val httpClient = OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()


            return Retrofit.Builder()
                .baseUrl("http://18.222.151.161/api/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiInterface::class.java)
        }
    }
}