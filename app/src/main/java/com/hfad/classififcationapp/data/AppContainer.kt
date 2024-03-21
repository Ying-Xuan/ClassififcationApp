package com.hfad.classififcationapp.data

import com.hfad.classififcationapp.network.ApiService

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface AppContainer {
    val retrofitService: ApiService
    val repository: Repository
}

class DefaultAppContainer : AppContainer {

    private val baseuri = "http://192.168.1.122:5000/"
    private val client = OkHttpClient.Builder().build()
    private var retrofit = Retrofit.Builder()
        .baseUrl(baseuri)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Retrofit service object for creating api calls
     */
    // by lazy : Delay the initialization of attributes until the first time they are accessed,
    // instead of initializing them immediately when the object is created.
    override val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val repository: Repository by lazy {
        NetworkRepository(retrofitService)
    }
}