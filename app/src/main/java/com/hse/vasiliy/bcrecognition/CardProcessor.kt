package com.hse.vasiliy.bcrecognition

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
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
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch


class CardProcessor(context: Context, view: View, private val previewBitmap: Bitmap) : AsyncTask<Void, Void, Void>() {

    private val contextRef = WeakReference(context)
    private val view = WeakReference(view)

    private val appTag = "RECOGNITION_ASYNC"

    override fun onPreExecute() {
        super.onPreExecute()
        val mView = view.get()
        try {
            if (mView != null) {
                mView.findViewById<ConstraintLayout>(R.id.loading_recognition_view).visibility = View.VISIBLE
                mView.findViewById<LinearLayout>(R.id.main_recognition_view).visibility = View.INVISIBLE
            } else {
                throw Exception("Activity doesn't exists anymore")
            }
        } catch (exc: Exception) {
            Log.e(appTag, exc.toString())
        }
    }

    override fun doInBackground(vararg params: Void?): Void? {
        val extractedText = extractText(previewBitmap)
        analyzeEntities(extractedText)
        return null
    }

    override fun onPostExecute(void: Void?) {
        super.onPreExecute()
        val mView = view.get()
        try {
            if (mView != null) {
                mView.findViewById<ConstraintLayout>(R.id.loading_recognition_view).visibility = View.INVISIBLE
                mView.findViewById<LinearLayout>(R.id.main_recognition_view).visibility = View.VISIBLE
            } else {
                throw Exception("Activity doesn't exists anymore")
            }
        } catch (exc: Exception) {
            Log.e(appTag, exc.toString())
        }
    }

    private fun extractText(previewBitmap: Bitmap): String {
        var extractedText = ""

        /*try {
            tessBaseApi = TessBaseAPI()
        } catch (exc: Exception) {
            Log.e(applicationTag, exc.toString())
            activity.showErrorByRequest(getString(R.string.camera_access_error))
        }

        tessBaseApi.init("${attachedActivityContext.getExternalFilesDir(null)}", "eng")
        tessBaseApi.setImage(bitmap)
        var extractedText = getString(R.string.empty_text)
        try {
            extractedText = tessBaseApi.utF8Text
        } catch (exc: Exception) {
            Log.e(applicationTag, exc.toString())
            activity.showErrorByRequest(getString(R.string.camera_access_error))
        }
        tessBaseApi.end()*/
        val textProcessedLatch = CountDownLatch(1)
        val image = FirebaseVisionImage.fromBitmap(previewBitmap)
        val detector = FirebaseVision.getInstance().cloudTextRecognizer
        detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                var text = ""
                for (block in firebaseVisionText.textBlocks) text += block.text + "\n"
                extractedText = text
                Log.v("EXTRACTED_TEXT", extractedText)
                textProcessedLatch.countDown()
            }
            .addOnFailureListener {
                val mContext = contextRef.get()
                if (mContext != null) {
                    (mContext as MainActivity).showErrorByRequest(mContext.getString(R.string.recognition_error))
                } else {
                    Log.e(appTag, it.toString())
                }
                textProcessedLatch.countDown()
            }
        textProcessedLatch.await()
        return extractedText
    }

    private fun analyzeEntities(text: String) {
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
                    Log.v("ENTITY_TYPE", it.type + " " + it.name)
                }
            }
        } catch (exc: Exception) {
            Log.e(appTag, exc.toString())
        }
    }
}