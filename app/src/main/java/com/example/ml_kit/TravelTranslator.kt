package com.example.ml_kit

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class TravelTranslator : Fragment(), View.OnClickListener  {

    private lateinit var rootView: View
    private lateinit var takeImgBtn: Button
    private lateinit var textOutput: TextView
    private lateinit var textOriginal: TextView
    private lateinit var imageView: ImageView

    // needed for image
    private var REQUEST_IMAGE_CAPTURE: Int = 1

    // needed for textRecognizer
    private lateinit var imageBitmap: Bitmap
    private lateinit var recognizer: TextRecognizer

    // needed for languageIdentification
    private lateinit var languageIdentifier: LanguageIdentifier

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_travel_translator, container, false)
        rootView = view

        takeImgBtn = view.findViewById(R.id.travel_createImg)
        textOriginal = view.findViewById(R.id.travel_originalText)
        textOutput = view.findViewById(R.id.travel_textOutput)
        imageView = view.findViewById(R.id.travel_img)

        takeImgBtn.setOnClickListener(this)

        return view
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)

            textRecognition()
        }
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // Text Recognition
    // ============================================================================================
    /**
     * textRecognition
     *
     * This method forms the structure of textRecognition.
     */
    private fun textRecognition() {
        // import recognizer
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // create InputImage
        // You can process the following image types:
        //      Bitmap, media.Image, ByteBuffer, byte array, or a file on the device
        //      Code of different types: https://developers.google.com/ml-kit/vision/text-recognition/android#2_prepare_the_input_image
        // We created a Bitmap Image (see onActivityResult())
        val image = InputImage.fromBitmap(imageBitmap, 0)

        getTextOfImage(image)
    }

    /**
     * getTextOfImage
     *      Input-val: image:InputImage = contains the image from which the text is to be extracted
     *
     * This method attempts to extract text from an image. The extracted text is then displayed
     * to the user. If no text is found, the user receives a suitable message.
     */
    private fun getTextOfImage(image: InputImage) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                textOriginal.text = visionText.text

                languageIdentify(visionText.text)
            }
            .addOnFailureListener { _ ->
                textOriginal.text = "No text detected"
            }
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // Identify Languages
    // ============================================================================================

    /**
     * TODO: DOCUMENTATION
     */
    private fun languageIdentify(text: String) {
        languageIdentifier = LanguageIdentification.getClient()
        /*
         * specify params:
         *      .setConfidenceThreshold(0.34f)
         *          this addition can change the threshold value, which can affect the accuracy
         *          of the result. Default threshold = 0.5
         */

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    textOriginal.text =  "${text}\n( Sprache nicht erkannt )"
                } else {
                    textOriginal.text =  "${text}\n( $languageCode )"
                }
            }
            .addOnFailureListener { _ ->
                textOriginal.text =  "${text}\n( Spracherkennung fehlgeschlagen )"
            }
    }
}