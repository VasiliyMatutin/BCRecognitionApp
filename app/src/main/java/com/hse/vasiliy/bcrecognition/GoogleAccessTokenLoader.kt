package com.hse.vasiliy.bcrecognition

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.language.v1.CloudNaturalLanguageScopes
import java.io.IOException
import java.lang.ref.WeakReference


class GoogleAccessTokenLoader(context: Context): AsyncTask<Void, Void, Void>() {

    private var contextRef: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg params: Void?): Void? {

        val mContext = contextRef.get()
        if (mContext != null) {
            val prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val currentToken = prefs.getString(PREF_ACCESS_TOKEN, null)
            if (currentToken != null) {
                val credential = GoogleCredential()
                    .setAccessToken(currentToken)
                    .createScoped(CloudNaturalLanguageScopes.all())
                val seconds = credential.expiresInSeconds
                if (seconds != null && seconds > 3600) {
                    return null
                }
            }
            val stream = mContext.resources.openRawResource(R.raw.credential)
            try {
                val credential = GoogleCredential.fromStream(stream)
                    .createScoped(CloudNaturalLanguageScopes.all())
                credential.refreshToken()
                val accessToken = credential.accessToken
                prefs.edit().putString(PREF_ACCESS_TOKEN, accessToken).apply()
            } catch (e: IOException) {
                Log.e("AccessTokenLoader", "Failed to obtain access token.", e)
            }
        }
        return null
    }
}