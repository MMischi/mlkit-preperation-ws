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
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL
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
        // set detector options
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        @SuppressLint("UnsafeOptInUsageError")
        val mediaImage = imageProxy.image
        if (mediaImage !== null) {
            // prepare image
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // create instance of face detector
            val detector = FaceDetection.getClient(realTimeOpts)

            // process image
            detector.process(image)
                .addOnSuccessListener { faces ->
                    /* task success */
                    if (faces.isNotEmpty()) {
                        for (face in faces) {
                            outlineFaceRect(face)
                            outlineFacePoly(face)
                        }
                    }
                }
                .addOnFailureListener { _ ->
                    /* task failed */
                }

            // after done, release the ImageProxy object
            imageProxy.close()
        }
    }

    private fun outlineFacePoly(face: Face) {
        var contourPoints = face.getContour(1)!!.points

        val imageV: ImageView = rootView.findViewById(R.id.me_imageVPoint)
        val bitmap: Bitmap = Bitmap.createBitmap(imageV.width, imageV.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)

        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = Color.YELLOW              // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 10F                 // the stroke width

        // create Polygon
        var pol: Path = Path()
        pol.moveTo(contourPoints[0].x, contourPoints[0].y)
        contourPoints.drop(0)   // already done
        for(contourPoint in contourPoints) {
            println(contourPoint)
            pol.lineTo(contourPoint.x, contourPoint.y)
        }

        // draw Polygon
        canvas.drawPath(pol, paint)

        // set bitmap as background to ImageView
        imageV.background = BitmapDrawable(resources, bitmap)
    }

    private fun outlineFaceRect(face: Face) {
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

        val face_xcenter = face.boundingBox.centerX() / 2F
        val face_ycenter = face.boundingBox.centerY() / 2F

        val xcenter = preview_xcenter - face_xcenter
        val ycenter = preview_ycenter - face_ycenter

        // Calculate positions.
        val left = xcenter - face.boundingBox.width()
        val top = ycenter + face.boundingBox.height()
        val right = xcenter + face.boundingBox.width()
        val bottom = ycenter - face.boundingBox.height()


        val rec: Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        val rec2: Rect = Rect(face.boundingBox.left, face.boundingBox.top, face.boundingBox.right, face.boundingBox.bottom)

        // draw Rect
        canvas.drawRect(rec, paint)

        // set bitmap as background to ImageView
        imageV.background = BitmapDrawable(resources, bitmap)
    }
}