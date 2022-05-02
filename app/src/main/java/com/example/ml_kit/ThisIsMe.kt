package com.example.ml_kit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.w3c.dom.Text
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ThisIsMe : Fragment() {

    private lateinit var rootView: View
    private lateinit var context: Activity

    private var REQUEST_CODE_PERMISSIONS = 123
    private var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    // cameraX
    private lateinit var preview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraSelector: CameraSelector
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()



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
            bindPreview()
            imageAnalysis()

            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, imageAnalysis, preview
            )

        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * bindPreview
     *
     * Set up camera, and execute camera activity.
     */
    private fun bindPreview() {
        preview = Preview.Builder()
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        var previewView: PreviewView = rootView.findViewById(R.id.me_preview)

        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    /**
     * imageAnalysis
     *
     * ImageAnalysis gives us the possibility to edit each frame individually.
     */
    private fun imageAnalysis() {
        imageAnalysis = ImageAnalysis.Builder()
            // enable the following line if RGBA output is needed.
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->

            // start face detection api
            faceDetection(imageProxy)
        })

    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // Face Detection
    // ============================================================================================

    private fun faceDetection(imageProxy: ImageProxy) {
        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        @SuppressLint("UnsafeOptInUsageError")
        val mediaImage = imageProxy.image
        if (mediaImage !== null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val commentbox: TextView = rootView.findViewById(R.id.me_commentbox)

            val detector = FaceDetection.getClient(realTimeOpts)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        outlineFace(face, imageProxy)
                        commentbox.text = "yess"
                    }
                }
                .addOnFailureListener { _ ->

                }

            // after done, release the ImageProxy object
            imageProxy.close()
        }
    }

    private fun outlineFace(face: Face, imageProxy: ImageProxy) {
        val imageV: ImageView = rootView.findViewById(R.id.me_imageV)
        val bitmap: Bitmap = Bitmap.createBitmap(imageV.width, imageV.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)

        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = Color.RED                 // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 10F                 // the stroke width

        val preview_xcenter = imageV.width / 2F
        val preview_ycenter = imageV.height / 2F

        val face_xcenter = face.boundingBox.centerX()
        val face_ycenter = face.boundingBox.centerY()

        val xcenter = preview_xcenter - face_xcenter + face.boundingBox.width()
        val ycenter = preview_ycenter - face_ycenter + face.boundingBox.height()

        // Calculate positions.
        val left = xcenter - face.boundingBox.width()
        val top = ycenter + face.boundingBox.height()
        val right = xcenter + face.boundingBox.width()
        val bottom = ycenter - face.boundingBox.height()


        val rec: Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

        // draw Rect
        canvas.drawRect(rec, paint)

        // set bitmap as background to ImageView
        imageV.background = BitmapDrawable(resources, bitmap)
    }
}