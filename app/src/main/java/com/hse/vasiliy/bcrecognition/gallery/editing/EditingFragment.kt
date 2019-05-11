package com.hse.vasiliy.bcrecognition.gallery.editing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.content.Context
import android.widget.ImageView
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import com.hse.vasiliy.bcrecognition.BITMAP_TMP
import com.hse.vasiliy.bcrecognition.MainActivity
import com.hse.vasiliy.bcrecognition.PARCELABLE_ITEM
import com.hse.vasiliy.bcrecognition.R
import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent


open class EditingFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    protected lateinit var activity: MainActivity

    private var applicationTag = "EditingFragment"
    private var isNeedToReset = false
    protected var item: CardGalleryContent.ParcelableJsonItem? = null

    private lateinit var cardPreview: ImageView
    protected lateinit var previewBitmap: Bitmap
    protected lateinit var nameText: TextInputEditText
    protected lateinit var organizationText: TextInputEditText
    protected lateinit var phoneText: TextInputEditText
    protected lateinit var emailText: TextInputEditText
    protected lateinit var addressText: TextInputEditText

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
        val view = inflater.inflate(R.layout.editing_fragment, container, false)
        nameText = view.findViewById(R.id.name_edit_text)
        organizationText = view.findViewById(R.id.organization_edit_text)
        phoneText = view.findViewById(R.id.phone_edit_text)
        emailText = view.findViewById(R.id.email_edit_text)
        addressText = view.findViewById(R.id.address_edit_text)

        cardPreview = view.findViewById(R.id.card_preview)

        val bitmapFile = activity.openFileInput(BITMAP_TMP)
        previewBitmap = BitmapFactory.decodeStream(bitmapFile)

        cardPreview.setImageBitmap(previewBitmap)

        return view
    }

    override fun onResume() {
        super.onResume()
        if (isNeedToReset) {
            item = null
            if (arguments != null) {
                item = arguments!!.getParcelable(PARCELABLE_ITEM)
            }
            if (item != null) {
                nameText.setText(item!!.name)
                organizationText.setText(item!!.company)
                phoneText.setText(item!!.phone)
                emailText.setText(item!!.email)
                addressText.setText(item!!.address)
            }
            isNeedToReset = false
        }
    }

    fun upResetFlag() {
        isNeedToReset = true
    }
}
