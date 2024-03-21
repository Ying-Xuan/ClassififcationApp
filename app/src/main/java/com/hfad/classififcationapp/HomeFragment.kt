package com.hfad.classififcationapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.hfad.classififcationapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var safeContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding =  FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.cameraButton.setOnClickListener{
            view.findNavController().navigate(R.id.action_homeFragment_to_cameraFragment)
        }

        binding.albumButton.setOnClickListener{
            Log.d("ddd", "${REQUIRED_PERMISSIONS.size}")
            if (allPermissionsGranted()) {
                ActionRequiredPermission()
            } else {
                requestPermissions()
            }
        }

        return view
    }

    // Actions that require permission
    private fun ActionRequiredPermission() {
        pickImage()
    }

    private val activityResultLauncher =
        registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions() ) { permissions ->
            var permissionGranted = true
            Log.d("check permission", " ")
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false){
                    Log.d("check permission", "1")
                    permissionGranted = false
                }
            }
            if (!permissionGranted) {
                Toast.makeText(safeContext ,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                ActionRequiredPermission()
            }
        }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(safeContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Registers a photo picker activity launcher in single-select mode.
    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            handleSelectedImage(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private fun pickImage() {
        // Launch the photo picker and let the user choose only images.
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun handleSelectedImage(uri: Uri) {
        var imagePath = uri.toString()
        val action = HomeFragmentDirections.actionHomeFragmentToImageFragment(imagePath)
        view?.findNavController()?.navigate(action)
    }

    companion object {
        private val REQUIRED_PERMISSIONS: Array<String> by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13 (API level 33) and higher
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }
}

