package com.example.ml_kit

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

class ThisIsMe : Fragment() {

    private lateinit var rootView: View
    private lateinit var context: Activity

    private var REQUEST_CODE_PERMISSIONS = 123
    private var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_this_is_me, container, false)

        context = this.requireActivity()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                context,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        return rootView
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // CameraX
    // ============================================================================================
    /**
     * onRequestPermissionsResult
     *      Input-val: requestCode: Int,
     *          permissions: Array<String>,
     *          grantResults: IntArray
     *
     * Executes on requestPermissions. Ask the user for camera permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * allPermissionsGranted
     *
     * Checks permissions.
     */
    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                context, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * startCamera
     *
     * Executes on requestPermissions. Ask the user for camera permissions.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * bindPreview
     *      Input-val: cameraProvider: ProcessCameraProvider
     *
     * Set up camera, and execute camera activity.
     */
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()

        var cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        var previewView: PreviewView = rootView.findViewById(R.id.me_preview)

        preview.setSurfaceProvider(previewView.surfaceProvider)
        var camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner, cameraSelector, preview
        )
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // Face Detection
    // ============================================================================================
}