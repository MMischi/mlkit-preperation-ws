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

