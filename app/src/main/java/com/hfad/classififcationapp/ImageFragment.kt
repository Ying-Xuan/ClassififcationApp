package com.hfad.classififcationapp

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.hfad.classififcationapp.data.Answer
import com.hfad.classififcationapp.databinding.FragmentCameraBinding
import com.hfad.classififcationapp.databinding.FragmentImageBinding
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.Executors



class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null
    private val binding get()= _binding!!
    lateinit var viewModel: ImageViewModel
    private lateinit var safeContext: Context
    private lateinit var uri: Uri
    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentImageBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ImageViewModel::class.java)

        val imagePath = ImageFragmentArgs.fromBundle(requireArguments()).imagePath
        Log.d("SetImage", imagePath)
        uri = Uri.parse(imagePath)

        try {
            binding.imageView.setImageURI(uri)
        } catch (e: Exception) {
            Log.e("SetImage", "Can't set Image to imageView. ${e.printStackTrace()}")
        }

        binding.sentImage.setOnClickListener {
            if (!PermissionsGranted())
                requestPermissions()
            else
                ActionRequiredPermission()
        }


        binding.getAnswer.setOnClickListener {
            getAnswer()
        }

        return binding.root

    }

    private fun postImage(uri:Uri) {

        val contentResolver: ContentResolver = safeContext.contentResolver
        val imageFilePath = viewModel.getFileFromContentUri(uri, contentResolver)
        val file = File(imageFilePath)
        val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val call: Call<ResponseBody> = viewModel.postImage(body)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    binding.showPrediction.text = "Wait one second before pressing the 'Get Predict' button."
                    Log.e("Post", "response is Successful")
                } else {
                    Log.e("Post", "response is not Successful")
                }
            }
            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                Toast.makeText(safeContext ,"Connected Error. Please check your network", Toast.LENGTH_SHORT).show()
                Log.e("Post", "${t?.message}")
            }
        })
    }

    private fun getAnswer() {
        val answer = viewModel.getAnswer()
        answer.enqueue( object : Callback<Answer> {
            override fun onResponse(call: Call<Answer>, response: Response<Answer>) {
                var answerString : String = ""
                var answer : Answer? = null
                Log.d("Get", "status code is ${response.code()}")
                if (response.isSuccessful) {
                    val answerJson = response.body()
                    val gson = Gson()
                    answerString = gson.toJson(answerJson)
                    answer = gson.fromJson(answerString, Answer::class.java)
                    Log.d("Get", "json is ${answer.answer}")
                } else {
                    Log.e("Get", "response is not Successful")
                }

                binding.showPrediction.text = answer!!.answer
            }

            override fun onFailure(call: Call<Answer>, t: Throwable) {
                Toast.makeText(safeContext ,"Connected Error. Please check your network", Toast.LENGTH_SHORT).show()
                Log.e("Get", "${t.message}")
            }
        })
    }

    // Actions that require permission
    private fun ActionRequiredPermission() {
        postImage(uri)
    }

    private val activityResultLauncher =
        registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions() ) { permissions ->
            var permissionGranted = true
            Log.d("check permission", " ")
            permissions.entries.forEach {
                if (it.key in ImageFragment.REQUIRED_PERMISSIONS && it.value == false){
                    Log.d("check permission", "1")
                    permissionGranted = false
                }
            }
            if (!permissionGranted) {
                Toast.makeText(safeContext ,"Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                ActionRequiredPermission()
            }
        }

    private fun requestPermissions() {
        activityResultLauncher.launch(ImageFragment.REQUIRED_PERMISSIONS)
    }

    private fun PermissionsGranted() =
        ( ContextCompat.checkSelfPermission(safeContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.READ_EXTERNAL_STORAGE,  // Android 12
            ).toTypedArray()
    }
}