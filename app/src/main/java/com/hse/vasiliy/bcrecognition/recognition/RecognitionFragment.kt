package com.hse.vasiliy.bcrecognition.recognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.widget.ImageView
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.hse.vasiliy.bcrecognition.BITMAP_TMP
import com.hse.vasiliy.bcrecognition.MainActivity
import com.hse.vasiliy.bcrecognition.R
import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent


class RecognitionFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    private lateinit var activity: MainActivity

    private var applicationTag = "RecognitionFragment"

    private lateinit var previewBitmap: Bitmap

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            attachedActivityContext = context
            activity = attachedActivityContext as MainActivity
        } else {
            Log.e(applicationTag, "Cannot attach fragment to activity")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recognition_fragment, container, false)

        val bitmapFile = activity.openFileInput(BITMAP_TMP)
        previewBitmap = BitmapFactory.decodeStream(bitmapFile)

        val task = CardProcessor(attachedActivityContext, previewBitmap)
        task.execute()
        return view
    }

}
