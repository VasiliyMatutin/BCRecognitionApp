package com.hse.vasiliy.bcrecognition

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.language.v1.CloudNaturalLanguage
import com.google.api.services.language.v1.CloudNaturalLanguageScopes
import com.google.api.services.language.v1.model.AnalyzeEntitiesRequest
import com.google.api.services.language.v1.model.AnalyzeEntitiesResponse
import com.google.api.services.language.v1.model.Document
import java.lang.ref.WeakReference

class AutomaticOnlineEntityAnalyzer(context: Context, private var text: String) : AsyncTask<Void, Void, Void>() {

    private var contextRef: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg params: Void?): Void? {
        try {
            val mContext = contextRef.get()
            if (mContext != null) {
                val prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val currentToken = prefs.getString(PREF_ACCESS_TOKEN, null)
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
            }
        } catch (exc: Exception) {
            //TODO: add appropriate catch
        }
        return null
    }
}