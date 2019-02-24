package com.hse.vasiliy.bcrecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import android.widget.Toast


class RecognitionActivity : AppCompatActivity() {

    private lateinit var cardPreview: ImageView
    private lateinit var tmpText: TextView
    private lateinit var previewBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.recognition_activity)

        cardPreview = findViewById(R.id.card_preview)
        tmpText = findViewById(R.id.tmp_text_window)
        val bitmapFile = openFileInput(BITMAP_TMP)
        previewBitmap = BitmapFactory.decodeStream(bitmapFile)
        cardPreview.setImageBitmap(previewBitmap)

        val image = FirebaseVisionImage.fromBitmap(previewBitmap)
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
            }
    }
}
