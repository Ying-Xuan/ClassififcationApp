package com.hfad.classififcationapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hfad.classififcationapp.databinding.FragmentCameraBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get()= _binding!!
    lateinit var viewModel: CameraViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var safeContext: Context

    private var fusedLocationClient: FusedLocationProviderClient? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            ActionRequiredPermission()
        } else {
            requestPermissions()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(safeContext)


        // Setup the listener for take photo button
        binding.imageCaptureButton.setOnClickListener { takePhoto() }

        // CameraX requires developer to set a thread for the execution
        cameraExecutor = Executors.newSingleThreadExecutor()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The fragment can NOT access its layout after onDestroyView() is called, so _binding is set to null
        _binding = null
        cameraExecutor.shutdown()
    }

    // Actions that require permission
    private fun ActionRequiredPermission() {
        startCamera()
    }

    private val activityResultLauncher =
        registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions() ) { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in NECESSARY_PERMISSIONS && it.value == false)
                    permissionGranted = false
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
        activityResultLauncher.launch(REQUEST_PERMISSIONS)
    }

    // ContextCompat.checkSelfPermission : return permission grant status
    // PackageManager.PERMISSION_GRANTED : a state indicating that the permission has been granted
//    private fun cameraPermissionsGranted() =
//        ( ContextCompat.checkSelfPermission(safeContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED )

    private fun allPermissionsGranted() = NECESSARY_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(safeContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Preview
    private fun startCamera() {

        // used to bind the lifecycle of cameras to the lifecycle owner.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        // bind the lifecycle of cameras to the lifecycle owner
        cameraProviderFuture.addListener({


            // provides most of the CameraX core functionality in a single class.
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview : Initialize our Preview object, call build on it, get a surface provider from viewfinder, and then set it on the preview.
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding : make sure nothing is bound to the cameraProvider
                cameraProvider.unbindAll()

                // Bind use cases to camera : bind our cameraSelector and preview object to the cameraProvider
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))
    }

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return  // null => return
        var resolver = requireActivity().contentResolver
        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // Android 10 and higher => save in specified path in the media library (album named after the app)
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${TAG}-Image")
            }
        }
        // Create output options object which contains file + metadata
        // This object is where we can specify things about how we want our output to be
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(resolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(safeContext),
            // object : ImageCapture.OnImageSavedCallback {}
            object : ImageCapture.OnImageSavedCallback {
                // fail
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                // successful
                override fun onImageSaved(output: ImageCapture.OutputFileResults){

                    fusedLocationClient?.let {
                        setCurrentLocationInExif(
                            requireContext(),
                            output.savedUri!!,
                            it
                        )
                    }

                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    val uriImagePath = getImageUriByFileName(safeContext, name)
                    var imagePath = uriImagePath.toString()
                    Log.d("SetImage", imagePath)
                    val action = CameraFragmentDirections.actionCameraFragmentToImageFragment( imagePath )
                    view?.findNavController()?.navigate(action)
                }
            }

        )
    }


    fun getImageUriByFileName(context: Context, fileName: String): Uri? {

        val contentResolver: ContentResolver = context.contentResolver

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  // Android 10 and higher
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DURATION,
        )

        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} >= ?"
        val selectionArgs = arrayOf(fileName)
        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

        val cursor: Cursor? = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        var imageUri: Uri? = null
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            Log.d("SetImage", "${cursor.count}")
            if (cursor.count > 0) {
                cursor.moveToFirst()
                val id = cursor.getLong(idColumn)
                imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id )
            }
        }
        cursor?.close()

        return imageUri
    }

    fun setCurrentLocationInExif(context: Context, uri : Uri, fusedLocationClient : FusedLocationProviderClient){
        // check location permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {

            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener { location ->
                location?.let {
                    try {
                        val uriFd = context.contentResolver.openFileDescriptor(uri, "rw")
                        uriFd?.use { fd ->
                            val uriExif = ExifInterface(fd.fileDescriptor)
                            viewModel.setLocationInExif(uriExif, it)
                            uriExif.saveAttributes()
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Error updating image location: ${e.message}")
                    }
                }
            }
        } else {
            Log.e(TAG, "no location found")
        }
    }

    companion object {
        private const val TAG = "ClassificationApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private val REQUEST_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)  // Android 8 (API level 33) and lower
                }
            }.toTypedArray()
        private val NECESSARY_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)   // Android 8 (API level 33) and lower
                }
            }.toTypedArray()
    }
}



