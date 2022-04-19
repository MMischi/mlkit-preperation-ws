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

        handleInput(textInput)
    }

    /**
     * getInsertedText
     *      Input-val:  v:View = Chatbot-View
     *      Output-val: text:String = contains input of EditText-View
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

    /**
     * entityExtractor
     *      Input-val: input:String = (user) input
     *
     * First make ensure that ee-model is downloaded before extract entities.
     */
    private fun handleInput(input: String) {
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

    /**
     * createEntityExtractor
     *
     * We need an EntityExtractor object for extracting later.
     */
    private fun createEntityExtractor() {
        entityExtractor =
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )
    }

    /**
     * extractEntities
     *      Input-val: input:String = (user) input
     *
     * Now we can try to extract entities.First we check if the input-text contains entities,
     * if not SmartReply is called. If entities are contained, they are handled further.
     */
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

                    // smartReply if no entity detected
                    smartReply(input)
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

    /** TODO: Documentation
     *
     */
    private fun smartReply(input: String) {
        addMessageToHistory(input)

        // import SmartReplyGenerator
        smartReplyGenerator = SmartReply.getClient()
        smartReplyGenerator.suggestReplies(conversation)
            .addOnSuccessListener { result ->
                handleSuggestions(result)
            }
            .addOnFailureListener {
                println("No output")
            }
    }

    /**
     * handleSuggestions
     *      Input-val: result:SmartReplySuggestionResult = generated suggestions of smart reply
     *
     * Creates output of suggestions.
     */
    private fun handleSuggestions(result: SmartReplySuggestionResult) {
        if (result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
            println("it's not working")
        } else if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
            msgSuggestions.text = "Suggestions:"
            for (suggestion in result.suggestions) {
                msgSuggestions.text =
                    msgSuggestions.text.toString() + "\n${suggestion.text}"
            }

            msgHistory.text =
                msgHistory.text.toString() + "\nChatbot: ${result.suggestions[0].text}"
        }
    }


    /**
     * addMessageToHistory
     *      Input-val: input:String = (user) input
     *
     * Add user message to a history (List Array). This is used to generate context based
     * smart reply suggestions later.
     */
    private fun addMessageToHistory(input: String) {
        conversation.add(TextMessage.createForRemoteUser
            (input, System.currentTimeMillis(), userId))
    }


    /**
     * getEntityParams
     *      Input-val: input:String = (user) input
     *
     * To execute entities we have to convert the input text
     */
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

    /**
     * handleEntity
     *      Input-val: entityAnnotations:List<EntityAnnotation> = entities which are contained
     *          in the user input
     *
     * In this method, the individual entities are processed.
     */
    private fun handleEntity(entityAnnotations: List<EntityAnnotation>) {
        for (entityAnnotation in entityAnnotations) {

            val entities = entityAnnotation.entities
            val annotatedText = entityAnnotation.annotatedText

            for (entity in entities) {
                createEntityText(annotatedText, entity)
            }
        }
    }

    /**
     * createEntityText
     *      Input-val:  annotatedText:String = Text passage containing the entity
     *                  entity: Entity = contains entity type-id
     *
     * The entity's text is passed into the appropriate method, which produces the chatbot's output.
     */
    private fun createEntityText(annotatedText: String, entity: Entity) {
        when (entity.type) {
            Entity.TYPE_URL -> displayUrlInfo(annotatedText)
            Entity.TYPE_PHONE -> displayPhoneInfo(annotatedText)
            Entity.TYPE_EMAIL -> displayEmailInfo(annotatedText)
            Entity.TYPE_ADDRESS -> displayAddressInfo(annotatedText)
            else -> displayDefaultInfo(annotatedText, entity)

            /*
             * further entities
             * Entity.TYPE_FLIGHT_NUMBER -> displayFlightNoInfo(entity, annotatedText)
             * Entity.TYPE_IBAN -> displayIbanInfo(entity, annotatedText)
             * Entity.TYPE_ISBN -> displayIsbnInfo(entity, annotatedText)
             * Entity.TYPE_MONEY -> displayMoneyEntityInfo(entity, annotatedText)
             * Entity.TYPE_PAYMENT_CARD -> displayPaymentCardInfo(entity, annotatedText)
             * Entity.TYPE_TRACKING_NUMBER -> displayTrackingNoInfo(entity, annotatedText)
             * Entity.TYPE_DATE_TIME -> displayDateTimeInfo(entity)
             */
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
}



