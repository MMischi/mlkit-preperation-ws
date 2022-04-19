package com.example.ml_kit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

import com.google.mlkit.nl.entityextraction.*

import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplyGenerator
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage


class Chatbot : Fragment(), View.OnClickListener {

    private lateinit var rootView: View
    private lateinit var submitButton: Button
    private lateinit var msgHistory: TextView
    private lateinit var msgSuggestions: TextView

    private var outputText: String = ""

    // needed for entityExtractor
    private lateinit var entityExtractor: EntityExtractor

    // needed for smartReply
    private lateinit var smartReplyGenerator: SmartReplyGenerator
    private var conversation: ArrayList<TextMessage> = ArrayList()
    private var userId: String = "user_1234" // should be replaced by real user id


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        msgHistory = view.findViewById(R.id.chatbot_msgHistory)
        msgSuggestions = view.findViewById(R.id.chatbot_suggestedReply)

        submitButton = view.findViewById(R.id.chatbot_submit)
        submitButton.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View) {
        rootView = v.rootView

        var textInput: String = getInsertedText(rootView)
        if (textInput == "no input") {
            msgHistory.text = msgHistory.text.toString() + "\nChatbot: How  can I help you?"
            return
        } else {
            msgHistory.text = msgHistory.text.toString() + "\nYou: $textInput"
        }

        handleInputDownloadEE(textInput)
    }

    // ============================================================================================
    // Main helper-function
    // ============================================================================================
    /**
     * getInsertedText
     *      Input-val:  v:View = Chatbot-View
     *      Output-val: text:String = contains input of EditText-View
     *                  returns "no input" at no user input
     *
     * Reads text from the input field and returns its content.
     */
    private fun getInsertedText(v: View): String {
        var textInputElem: EditText = v.findViewById(R.id.chatbot_enterMessage)
        var text: String = textInputElem.text.toString()

        if (text == "" || text.isEmpty()) {
            return "no input"
        }

        return text
    }

    /**
     * handleInput_downloadEE
     *      Input-val: input:String = (user) input
     *
     * In this method the EE-model will be downloaded. We need this to be able filtering
     * possible entities from the user input. If the download fails, an appropriate message
     * will be displayed.
     */
    private fun handleInputDownloadEE(input: String) {
        // creates the EntityExtractor, which we need for entity detection
        entityExtractor =
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )

        // Ensure that the ee-model is downloaded
        entityExtractor
            .downloadModelIfNeeded()
            .addOnSuccessListener { _ ->
                /* Model download succeed */

                // try extracting entities of input
                tryExtractEntities(input)
            }
            .addOnFailureListener { _ ->
                /* Model download failed */
                msgHistory.text =
                    msgHistory.text.toString() + "\nChatbot: Sorry something went wrong!"
            }
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // Entity Extraction
    // ============================================================================================

    /**
     * tryExtractEntities
     *      Input-val: input:String = (user) input
     *
     * This method tries to extract entities from the user input. The following cases may
     * occur here:
     *      a) If the text cannot be processed, a suitable message is output.
     *      b.a) If NO entities are detected, the user input is passed to SmartReply.
     *      b.b) If entities are found, they can be processed further.
     */
    private fun tryExtractEntities(input: String) {
        // create params object
        val params = getEntityParams(input)

        // extract information of params
        entityExtractor
            .annotate(params)
            .addOnSuccessListener { entityAnnotations: List<EntityAnnotation> ->
                /* Annotation successful */
                if (entityAnnotations.isEmpty()) {
                    // smartReply if no entity detected
                    smartReply(input)
                } else {
                    // one or more entities detected
                    handleEntities(entityAnnotations)
                }
            }
            .addOnFailureListener { _ ->
                /* Annotation failed */
                msgHistory.text =
                    msgHistory.text.toString() + "\nChatbot: Annotation (EE) failed"
            }
    }

    /**
     * getEntityParams
     *      Input-val: input:String = (user) input
     *
     * To extract entities we have to convert the input text
     */
    private fun getEntityParams(input: String): EntityExtractionParams {
        return EntityExtractionParams.Builder(input).build()

        /*
         * specify params:
         *      .setEntityTypesFilter(optional entity type filter)
         *          A set must be passed eg: var typesSet: Set<Int> = setOf(1) // 1 = address
         *          List of Entity-Types:
         *              https://developers.google.com/android/reference/com/google/mlkit/nl/entityextraction/Entity#TYPE_ADDRESS
         *      .setPreferredLocale(optional preferred locale)
         *      .setReferenceTime(optional reference date-time)
         *      .setReferenceTimeZone(optional reference timezone)
         *
         */
    }

    /**
     * handleEntities
     *      Input-val: entityAnnotations:List<EntityAnnotation> = List of entities
     *          which are contained in the user input
     *
     * In this method, over all entities is iterated. It determines which entity it is and routes
     * it to the suitable display-method (these are listed afterwards, but were not commented on).
     */
    private fun handleEntities(entityAnnotations: List<EntityAnnotation>) {
        for (entityAnnotation in entityAnnotations) {
            val entities = entityAnnotation.entities
            val annotatedText = entityAnnotation.annotatedText

            for (entity in entities) {
                when (entity.type) {
                    Entity.TYPE_URL -> displayUrlInfo(annotatedText)
                    Entity.TYPE_PHONE -> displayPhoneInfo(annotatedText)
                    Entity.TYPE_EMAIL -> displayEmailInfo(annotatedText)
                    Entity.TYPE_ADDRESS -> displayAddressInfo(annotatedText)
                    /*
                     * further entity types:
                     * Entity.TYPE_DATE_TIME -> displayDateTimeInfo(entity)
                     * Entity.TYPE_IBAN -> displayIbanInfo(entity, annotatedText)
                     * Entity.TYPE_ISBN -> displayIsbnInfo(entity, annotatedText)
                     * Entity.TYPE_MONEY -> displayMoneyEntityInfo(entity, annotatedText)
                     * Entity.TYPE_FLIGHT_NUMBER -> displayFlightNoInfo(entity, annotatedText)
                     * Entity.TYPE_PAYMENT_CARD -> displayPaymentCardInfo(entity, annotatedText)
                     * Entity.TYPE_TRACKING_NUMBER -> displayTrackingNoInfo(entity, annotatedText)
                     */
                    else -> displayDefaultInfo(annotatedText, entity)
                }
            }
        }
    }
    private fun displayDefaultInfo(annotatedText: String, entity: Entity) {
        var type: String = when (entity.type) {
            Entity.TYPE_FLIGHT_NUMBER -> "flight number"
            Entity.TYPE_IBAN -> "iban"
            Entity.TYPE_ISBN -> "isbn"
            Entity.TYPE_MONEY -> "money"
            Entity.TYPE_PAYMENT_CARD -> "payment card"
            Entity.TYPE_TRACKING_NUMBER -> "tracking number"
            Entity.TYPE_DATE_TIME -> "date/time"
            else -> "don't know"
        }

        outputText = "Chatbot: Entity ($type) detected here: $annotatedText"
        msgHistory.text = msgHistory.text.toString() + "\n$outputText"
    }
    private fun displayAddressInfo(annotatedText: String) {
        outputText = "Chatbot: I open map client with address: $annotatedText"
        msgHistory.text = msgHistory.text.toString() + "\n$outputText"

        val uri = "http://maps.google.co.in/maps?q=$annotatedText"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }
    private fun displayEmailInfo(annotatedText: String) {
        outputText = "Chatbot: I open mail client. Mail to:  $annotatedText"
        msgHistory.text = msgHistory.text.toString() + "\n$outputText"

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "plain/text"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(annotatedText))
        startActivity(Intent.createChooser(intent, ""))
    }
    private fun displayPhoneInfo(annotatedText: String) {
        outputText = "Chatbot: I open phone with number: $annotatedText"
        msgHistory.text = msgHistory.text.toString() + "\n$outputText"

        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$annotatedText")
        startActivity(intent)
    }
    private fun displayUrlInfo(annotatedText: String) {
        var url = annotatedText
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "http://$url"

        outputText = "Chatbot: I open the link: $url"
        msgHistory.text = msgHistory.text.toString() + "\n$outputText"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

// ================================================================================================
// ================================================================================================
    // ============================================================================================
    // SmartReply
    // ============================================================================================

    /**
     * smartReply
     *      Input-val: input:String = (user) input
     *
     * This method forms the structure of the SmartReplay.
     *      1. first the user input is added to a history to generate later
     *          content-based suggestions
     *      2. the SmartReplayGenerator is imported to be able to use it
     *      3. suggestions are generated on the given message
     */
    private fun smartReply(input: String) {
        // add input to history, to generate content based suggestion
        conversation.add(TextMessage.createForRemoteUser
            (input, System.currentTimeMillis(), userId))

        // import SmartReplyGenerator
        smartReplyGenerator = SmartReply.getClient()

        // create suggestions
        smartReplyGenerator.suggestReplies(conversation)
            .addOnSuccessListener { result ->
                handleSuggestions(result)
            }
            .addOnFailureListener {
                msgHistory.text =
                    msgHistory.text.toString() + "\nChatbot: generating suggestions failed (SR)"
            }
    }

    /**
     * handleSuggestions
     *      Input-val: result:SmartReplySuggestionResult = generated suggestions of smart reply
     *
     * This method generates the display of the suggestions to the message from SmartReplay. It
     * also adds the first suggestion to the chat history.
     */
    private fun handleSuggestions(result: SmartReplySuggestionResult) {
        if (result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
            msgHistory.text =
                msgHistory.text.toString() + "\nChatbot: SR not working (not_supported_language)"
        }

        else if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
            // add suggestions to extra suggestions box
            msgSuggestions.text = "Suggestions:"
            for (suggestion in result.suggestions) {
                msgSuggestions.text =
                    msgSuggestions.text.toString() + "\n${suggestion.text}"
            }
            // only one suggestion will be added to output
            msgHistory.text =
                msgHistory.text.toString() + "\nChatbot: ${result.suggestions[0].text}"
        }
    }
}



