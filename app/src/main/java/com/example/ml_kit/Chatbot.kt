package com.example.ml_kit

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.mlkit.nl.entityextraction.*

class Chatbot : Fragment(), View.OnClickListener {

    private lateinit var rootView: View
    private lateinit var submitButton: Button
    private lateinit var msgHistory: TextView
    private lateinit var outputText: String

    // needed for entityExtractor
    private lateinit var entityExtractor: EntityExtractor


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        submitButton = view.findViewById(R.id.chatbot_submit)
        submitButton.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View) {
        rootView = v.rootView
        msgHistory = rootView.findViewById(R.id.chatbot_msgHistory)

        // 1. get inserted text
        var textInput: String = getInsertedText(rootView)
        if (textInput == "no input") {
            msgHistory.text = msgHistory.text.toString() + "\n" + "How  can I help you?"
            return
        }

        // 2. EntityExtractor
        entityExtractor(textInput)

        msgHistory.text = msgHistory.text.toString() + "\n" +  textInput
    }

    /**
     * getInsertedText
     *      Input-val:  v:View = Chatbot-View
     *      Output-Val: text:String = contains input of EditText-View
     *                  returns "no input" at no user input
     *
     * Reads text from the input field and returns its content
     */
    private fun getInsertedText(v: View): String {
        var textInputElem: EditText = v.findViewById(R.id.chatbot_enterMessage)
        var text: String = textInputElem.text.toString()

        if (text == "" || text.isEmpty()) {
            return "no input"
        }

        return text
    }

    private fun entityExtractor(input: String) {
        createEntityExtractor()

        // Ensure that the ee-model is downloaded
        entityExtractor
            .downloadModelIfNeeded()
            .addOnSuccessListener { _ ->
                /* Model download succeed */
                // call ee-api
                extractEntities(input)
            }
            .addOnFailureListener { _ ->
                /* Model download failed */
                outputText = "Something went wrong"
            }
    }

    private fun createEntityExtractor() {
        entityExtractor =
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )
    }

    private fun extractEntities(input: String) {
        // create params object
        val params = getEntityParams(input)

        // extract information of params
        entityExtractor
            .annotate(params)
            .addOnSuccessListener { entityAnnotations: List<EntityAnnotation> ->
                /* Annotation successful */
                if (entityAnnotations.isEmpty()) {
                    println("No entity detected!")
                    // TODO: react with SmartReplay
                } else {
                    handleEntity(entityAnnotations)
                    println("finished entity extraction \n \n")
                }
            }
            .addOnFailureListener {
                /* Annotation failed */
                println("Annotation failed!")
            }
    }

    private fun getEntityParams(input: String): EntityExtractionParams {
        return EntityExtractionParams.Builder(input).build()

        /*
         * specify params:
         *      .setEntityTypesFilter((/* optional entity type filter */)
         *      .setPreferredLocale(/* optional preferred locale */)
         *      .setReferenceTime(/* optional reference date-time */)
         *      .setReferenceTimeZone(/* optional reference timezone */)
         *
         */
    }

    private fun handleEntity(entityAnnotations: List<EntityAnnotation>) {
        for (entityAnnotation in entityAnnotations) {

            val entities = entityAnnotation.entities
            val annotatedText = entityAnnotation.annotatedText

            for (entity in entities) {
                // TODO: make prettier output displayEntityInfo
                displayEntityInfo(annotatedText, entity)
            }
        }
    }

    private fun displayEntityInfo(annotatedText: String, entity: Entity) {
        when (entity) {
            is DateTimeEntity -> {
                Log.d(TAG, "Granularity: ${entity.dateTimeGranularity}")
                Log.d(TAG, "TimeStamp: ${entity.timestampMillis}")
            }
            is FlightNumberEntity -> {
                Log.d(TAG, "Airline Code: ${entity.airlineCode}")
                Log.d(TAG, "Flight number: ${entity.flightNumber}")
            }
            is MoneyEntity -> {
                Log.d(TAG, "Currency: ${entity.unnormalizedCurrency}")
                Log.d(TAG, "Integer part: ${entity.integerPart}")
                Log.d(TAG, "Fractional Part: ${entity.fractionalPart}")
            }
            else -> {
                Log.d(TAG, "  $entity")
            }
        }
    }
}



