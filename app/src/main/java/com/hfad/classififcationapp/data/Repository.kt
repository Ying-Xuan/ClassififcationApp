package com.hfad.classififcationapp.data

import com.hfad.classififcationapp.data.Answer
import com.hfad.classififcationapp.network.ApiService
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call


interface Repository {
    fun getAnswer() : Call<Answer>
    fun postImage(files : MultipartBody.Part) : Call<ResponseBody>
}

class NetworkRepository(private val apiService: ApiService): Repository {
    override fun getAnswer(): Call<Answer>  {
        return apiService.getAnswer()
    }

    override fun postImage(files : MultipartBody.Part): Call<ResponseBody> {
        return apiService.postImage(files)
    }

}