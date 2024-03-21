package com.hfad.classififcationapp.network


import com.hfad.classififcationapp.data.Answer
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface ApiService {
    @GET("get/result.json")
    fun getAnswer(): Call<Answer>

    @Multipart
    @POST("upload")
    fun postImage(
        @Part image: MultipartBody.Part?
    ): Call<ResponseBody>
}
