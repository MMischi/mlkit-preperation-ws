package com.example.ml_kit

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class WhichPlant : Fragment(), View.OnClickListener {

    private var colorList_COLOR: ArrayList<Int> = arrayListOf(
        Color.BLUE,
        Color.RED,
        Color.YELLOW,
        Color.GREEN
    )
    private var colorList_TEXT: ArrayList<String> = arrayListOf(
        "BLUE",
        "RED",
        "YELLOW",
        "GREEN"
    )

    private lateinit var rootView: View
    private lateinit var imageBitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var takeImgBtn: Button
    private lateinit var outputSting: TextView
    private lateinit var localModel: LocalModel

    // needed for objectDetection
    private lateinit var objOptions: CustomObjectDetectorOptions

    // needed for image
    private val REQUEST_IMAGE_CAPTURE: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_which_plant, container, false)

        takeImgBtn = rootView.findViewById(R.id.whichplant_createImg)
        imageView = rootView.findViewById(R.id.whichplant_img)
        outputSting = rootView.findViewById(R.id.whichplant_textOutput)

        // create local tf model
        localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_aiy_vision_classifier_plants_V1_3.tflite")
            .build()

        takeImgBtn.setOnClickListener(this)

        return rootView
    }

    override fun onClick(v: View) {
        dispatchTakePictureIntent()
    }

    // ============================================================================================
    // Main helper-function
    // ============================================================================================
    /**
     * dispatchTakePictureIntent
     *
     * Method to open camera app and another to place it to imageView.
     *
     * see resource: https://developer.android.com/training/camera/photobasics
     * -> startActivityForResult method for camera; newer versions are camera2 or cameraX
     */
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            println("Camera not working!")
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)

            objectDetection()
        }
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // Object Detection
    // ============================================================================================
    /**
     * objectDetection
     *
     * This method forms the structure of Object Detection.
     */
    private fun objectDetection() {
        defineDetector()

        // create object detector
        val objectDetector =
            ObjectDetection.getClient(objOptions)

        // set up input img
        val image = InputImage.fromBitmap(imageBitmap, 0)

        // process image
        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                /* processing successful */
                handleObjectDetector(detectedObjects)
            }
            .addOnFailureListener { _ ->
                /* processing failed */
                outputSting.text = "Something went wrong"
            }
    }

    /**
     * defineDetector
     *
     * Set detector options
     */
    private fun defineDetector() {
        objOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0f)
                .setMaxPerObjectLabelCount(3)
                .build()
    }

    /**
     * handleObjectDetector
     *      Input-val: detectedObjects: List = detected objects from the image
     *
     * Processes the detected items of the image. If no item is detected, an appropriate
     * message is output.
     */
    private fun handleObjectDetector(detectedObjects: List<DetectedObject>) {
        var output: String = ""

        if (detectedObjects.isNotEmpty()) {
            for ((idx, detectedObject) in detectedObjects.withIndex()) {
                if (detectedObject.labels.isNotEmpty()) {
                    output += "Detection ${ idx + 1 } (${ colorList_TEXT[idx] }):"
                    output += "\n ${identifyEntity(detectedObject)} \n"

                    val boundingBox: Rect = detectedObject.boundingBox
                    outlineEntity(boundingBox, idx)
                }
            }
        } else {
            output = "No items detected"
        }
        outputSting.text = output
    }

    /**
     * identifyEntity
     *      Input-val: detectedObject: DetectedObject = detected object in image
     *
     * Extracts the class of the detected object.
     */
    private fun identifyEntity(detectedObject: DetectedObject): String {
        var output: String = ""

        for (label in detectedObject.labels) {
            output += if (label.text == "None") {
                "Not a plant \n"
            } else {
                "${label.text} | ${label.confidence} \n"
            }
        }

        return output
    }

    /**
     * outlineEntity
     *      Input-val: boundingBox: Rect = Rectangle object that frames the detected object
     *
     * The rectangle that frames the element is already given - so we don't need to create it
     * separately. In this method we set the drawing styles and then draw the rectangle on
     * the image.
     */
    private fun outlineEntity(boundingBox: Rect, idx: Int) {
        // create drawing board
        imageBitmap = imageBitmap.copy(Bitmap.Config.RGB_565, true)
        val canvas: Canvas = Canvas(imageBitmap)

        // set painting options
        val paint = Paint()
        paint.alpha = 0xA0                      // the transparency
        paint.color = colorList_COLOR[idx]      // color is red
        paint.style = Paint.Style.STROKE        // stroke or fill or ...
        paint.strokeWidth = 1F                  // the stroke width

        // create  rectangle - you need left-, top-, right-, bottom-value
        // val rec = Rect(left, top, right, bottom)

        // draw image
        canvas.drawRect(boundingBox, paint);
        imageView.setImageBitmap(imageBitmap);
    }
}