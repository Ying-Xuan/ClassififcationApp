package com.hfad.classififcationapp

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import com.hfad.classififcationapp.data.Answer
import com.hfad.classififcationapp.data.DefaultAppContainer
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call

class ImageViewModel:ViewModel() {

    val container = DefaultAppContainer()

    fun getAnswer() : Call<Answer> {
        var result: Call<Answer>
        result = container.repository.getAnswer()
        return result
    }

    fun postImage(file : MultipartBody.Part): Call<ResponseBody> {
        return container.repository.postImage(file)
    }

    fun getFileFromContentUri(uri: Uri, contentResolver: ContentResolver): String? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        if (cursor == null) {
            Log.d("FindImagePath", " cursor is null. ")
        }

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = it.getString(columnIndex)
            }
        }
        cursor?.close()

        return filePath
    }
}