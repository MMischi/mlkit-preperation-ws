package com.example.ml_kit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ThisIsMe : Fragment(), View.OnClickListener {

    private lateinit var rootView: View
    private lateinit var context: Activity

    private var REQUEST_CODE_PERMISSIONS = 123
    private var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    // cameraX
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraSelector: CameraSelector
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()

    // Face Properties
    private lateinit var currFace: Face

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_this_is_me, container, false)
        context = this.requireActivity()

        var glassesBtn: Button = rootView.findViewById(R.id.me_glassesBtn)
        glassesBtn.setOnClickListener(this)

        return rootView
    }

    override fun onStart() {
        super.onStart()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                context,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onPause() {
        super.onPause()
        cameraProvider.unbindAll();
    }

    override fun onClick(v: View) {
        var glassesImg: ImageView = rootView.findViewById(R.id.me_glassesImg)
        if (glassesImg.visibility == View.VISIBLE) {
            glassesImg.visibility = View.INVISIBLE
            return
        }

        glassesImg.visibility = View.VISIBLE
        placeGlasses()
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

            cameraProvider = cameraProviderFuture.get()
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
        var pView: PreviewView = rootView.findViewById(R.id.me_preview)

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(pView.width, pView.height))
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
    /**
     * faceDetection
     *      Input-val: imageProxy: ImageProxy = frame
     *
     * This method forms the structure of Face Detection.
     */
    private fun faceDetection(imageProxy: ImageProxy) {
        // set detector options
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        @SuppressLint("UnsafeOptInUsageError")
        val mediaImage = imageProxy.image   // create image media
        if (mediaImage !== null) {
            // prepare image
            val image =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // create instance of face detector
            val detector = FaceDetection.getClient(realTimeOpts)

            // process image
            detector.process(image)
                .addOnSuccessListener { faces ->
                    /* task success */
                    if (faces.isNotEmpty()) {
                        for (face in faces) {
                            currFace = face     // save current face information

                            outlineFaceRect()
                            outlineFacePoly()
                            placeGlasses()
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

    /**
     * outlineFacePoly
     *
     * Outlines face contour.
     */
    private fun outlineFacePoly() {
        var contourPoints = currFace.getContour(1)!!.points

        val imageV: ImageView = rootView.findViewById(R.id.me_imageVPoint)
        val bitmap: Bitmap =
            Bitmap.createBitmap(imageV.width, imageV.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)

        val face_xcenter = currFace.boundingBox.centerX() * 0.5
        val face_ycenter = currFace.boundingBox.centerY() * 0.5

        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = Color.BLUE                // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 10F                 // the stroke width

        // create Polygon
        var pol: Path = Path()
        pol.moveTo(
            (contourPoints[0].x + face_xcenter.toFloat()),
            (contourPoints[0].y + face_ycenter.toFloat())
        )
        contourPoints.drop(0)   // already done
        for(contourPoint in contourPoints) {
            pol.lineTo(
                (contourPoint.x + face_xcenter.toFloat()),
                (contourPoint.y + face_ycenter.toFloat())
            )
        }

        // draw Polygon
        canvas.drawPath(pol, paint)

        // set bitmap as background to ImageView
        imageV.background = BitmapDrawable(resources, bitmap)
    }

    /**
     * outlineFacePoly
     *
     * Outlines face with rectangle.
     */
    private fun outlineFaceRect() {
        val imageV: ImageView = rootView.findViewById(R.id.me_imageV)
        val bitmap: Bitmap =
            Bitmap.createBitmap(imageV.width, imageV.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)

        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = Color.RED                 // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 10F                 // the stroke width

        val face_xcenter = currFace.boundingBox.centerX() * 0.5
        val face_ycenter = currFace.boundingBox.centerY() * 0.5

        // Calculate positions.
        val left = currFace.boundingBox.left + face_xcenter
        val top = currFace.boundingBox.top + face_ycenter
        val right = currFace.boundingBox.right + face_xcenter
        val bottom = currFace.boundingBox.bottom + face_ycenter


        val rec: Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        val rec2: Rect = Rect(
            currFace.boundingBox.left,
            currFace.boundingBox.bottom,
            currFace.boundingBox.right,
            currFace.boundingBox.top
        )

        // draw Rect
        canvas.drawRect(rec, paint)

        // set bitmap as background to ImageView
        imageV.background = BitmapDrawable(resources, bitmap)
    }

    /**
     * placeGlasses
     *
     * Something creative ;D Shows glasses placed over the eyes.
     */
    private fun placeGlasses() {
        var glassesImg: ImageView = rootView.findViewById(R.id.me_glassesImg)
        if (!this::currFace.isInitialized) {
            Toast.makeText(
                context,
                "No face detected yet!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // size image view
            glassesImg.layoutParams.width = currFace.boundingBox.width()
            glassesImg.requestLayout()

            val eyeContour = currFace.getContour(6)!!.points

            // calculate position
            val face_xcenter = currFace.boundingBox.centerX() * 0.5
            val left = currFace.boundingBox.left + face_xcenter
            val top = eyeContour[0].y + (glassesImg.height * 0.25)

            // place image view
            glassesImg.x = left.toFloat()
            glassesImg.y = top.toFloat()
        }
    }
}