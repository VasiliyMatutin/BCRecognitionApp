package com.hse.vasiliy.bcrecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.TextView
import android.content.Context
import android.widget.ImageView
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.googlecode.tesseract.android.TessBaseAPI


class RecognitionFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    private lateinit var activity: MainActivity

    private var applicationTag = "RecognitionFragment"

    private lateinit var tmpText: TextView
    private lateinit var cardPreview: ImageView
    private lateinit var previewBitmap: Bitmap
    private lateinit var tessBaseApi: TessBaseAPI

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
        //tmpText = view.findViewById(R.id.tmp_text_window)
        cardPreview = view.findViewById(R.id.card_preview)

        val bitmapFile = activity.openFileInput(BITMAP_TMP)
        previewBitmap = BitmapFactory.decodeStream(bitmapFile)

        cardPreview.setImageBitmap(previewBitmap)
        //tmpText.text = extractText(previewBitmap)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val image = FirebaseVisionImage.fromBitmap(previewBitmap)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
        val result = detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                var text = ""
                for (block in firebaseVisionText.textBlocks) text += block.text + "\n"
                tmpText.text = text
            }
            .addOnFailureListener {
                val toast = Toast.makeText(
                    applicationContext,
                    "This is not ok", //TODO:add error handler
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }*/

    }

    private fun extractText(bitmap: Bitmap): String {

        try {
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
        tessBaseApi.end()
        return extractedText
    }
}
