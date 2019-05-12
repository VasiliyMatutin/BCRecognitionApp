package com.hse.vasiliy.bcrecognition.recognition

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.language.v1.CloudNaturalLanguage
import com.google.api.services.language.v1.CloudNaturalLanguageScopes
import com.google.api.services.language.v1.model.AnalyzeEntitiesRequest
import com.google.api.services.language.v1.model.AnalyzeEntitiesResponse
import com.google.api.services.language.v1.model.Document
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.googlecode.tesseract.android.TessBaseAPI
import com.hse.vasiliy.bcrecognition.*
import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch


class CardProcessor(context: Context, private val previewBitmap: Bitmap) : AsyncTask<Void, Void, Void>() {

    private val contextRef = WeakReference(context)

    private var isOffline = true

    private var nameText = ""
    private var organizationText = ""
    private var phoneText = ""
    private var emailText = ""
    private var addressText = ""

    private val applicationTag = "RECOGNITION_ASYNC"

    override fun doInBackground(vararg params: Void?): Void? {
        val extractedText = extractText(previewBitmap)
        analyzeEntities(extractedText)
        analyzeTextByRegex(extractedText)
        return null
    }

    override fun onPostExecute(void: Void?) {
        super.onPostExecute(void)
        fillContactVisibleFields()
    }

    private fun extractText(previewBitmap: Bitmap): String {
        var extractedText = ""

        val mContext = contextRef.get()
        if (mContext != null) {
            val prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(OFFLINE_MODE, false)) {
                isOffline = false
            }
        }

        if (isOffline) {
            val tessBaseApi: TessBaseAPI?
            try {
                tessBaseApi = TessBaseAPI()
            } catch (exc: Exception) {
                Log.e(applicationTag, exc.toString())
                return ""
            }
            if (mContext != null) {
                val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(mContext)
                when (settingsPrefs.getString("offline_language", "en")){
                    "en" -> {
                        tessBaseApi.init("${mContext.getExternalFilesDir(null)}", "eng")
                    }
                    "ru" -> {
                        tessBaseApi.init("${mContext.getExternalFilesDir(null)}", "rus+eng")
                    }
                }
            } else {
                throw Exception("Cannot get access to application dir")
            }
            tessBaseApi.setImage(previewBitmap)
            try {
                extractedText = tessBaseApi.utF8Text
                Log.d(applicationTag, extractedText.toString())
            } catch (exc: Exception) {
                Log.e(applicationTag, exc.toString())
                return ""
            }
            tessBaseApi.end()
        } else {
            val textProcessedLatch = CountDownLatch(1)
            val image = FirebaseVisionImage.fromBitmap(previewBitmap)
            val detector = FirebaseVision.getInstance().cloudTextRecognizer
            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    var text = ""
                    for (block in firebaseVisionText.textBlocks) text += block.text + "\n"
                    extractedText = text
                    textProcessedLatch.countDown()
                }
                .addOnFailureListener {
                    val appContext = contextRef.get()
                    if (appContext != null) {
                        (appContext as MainActivity).showErrorByRequest(appContext.getString(
                            R.string.recognition_error
                        ), false)
                    }
                    Log.e(applicationTag, it.toString())
                    textProcessedLatch.countDown()
                }
            textProcessedLatch.await()
        }
        return extractedText
    }

    private fun analyzeEntities(text: String) {
        if (isOffline){
            return
        }
        try {
            val mContext = contextRef.get()
            val currentToken: String?
            if (mContext != null) {
                val prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                currentToken = prefs.getString(PREF_ACCESS_TOKEN, null)
            } else {
                throw Exception("Activity doesn't exists anymore")
            }
            //initialize credential for cloud access
            val googleCredential = GoogleCredential()
                .setAccessToken(currentToken)
                .createScoped(CloudNaturalLanguageScopes.all())
            //create special builder for HTTP JSON request
            val requestBuilder = CloudNaturalLanguage.Builder(
                NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                HttpRequestInitializer { request -> googleCredential.initialize(request) }).build()
            //fill JSON request
            val request = requestBuilder
                .documents()
                .analyzeEntities(
                    AnalyzeEntitiesRequest()
                        .setDocument(
                            Document()
                                .setContent(text)
                                .setType("PLAIN_TEXT")
                        )
                )
            val requestResult = request.execute()
            if (requestResult is AnalyzeEntitiesResponse) {
                requestResult.entities.forEach {
                    when(it.type) {
                        "PERSON" -> nameText += it.name + " "
                        "ORGANIZATION" -> organizationText += it.name + " "
                        "PHONE_NUMBER" -> phoneText += it.name + " "
                        "ADDRESS", "LOCATION" -> addressText += it.name + " "
                    }
                }
            }
        } catch (exc: Exception) {
            Log.e(applicationTag, exc.toString())
        }
    }

    private fun analyzeTextByRegex(text : String){
        if (phoneText.isEmpty() || emailText.isEmpty()) {
            val words = text.lines().joinToString(" ").split(" ")
            if (emailText.isEmpty()) {
                val regex = EMAIL_REGEX.toRegex()
                for (word in words) {
                    if (regex.matches(word)) {
                        emailText = word
                        break
                    }
                }
            }
            if (phoneText.isEmpty()) {
                val regex = PHONE_REGEX.toRegex()
                for (word in words) {
                    if (regex.matches(word)) {
                        phoneText = word
                        break
                    }
                }
            }
        }
    }

    private fun fillContactVisibleFields() {
        val mContext = contextRef.get()
        try {
            if (mContext != null) {
                val tmpRecognizedInfo = CardGalleryContent.ParcelableJsonItem("none",
                    nameText,
                    organizationText,
                    phoneText,
                    emailText,
                    addressText
                )

                (mContext as MainActivity).openEditing(true, tmpRecognizedInfo)
            } else {
                throw Exception("Activity doesn't exists anymore")
            }
        } catch (exc: Exception) {
            Log.e(applicationTag, exc.toString())
        }
    }
}