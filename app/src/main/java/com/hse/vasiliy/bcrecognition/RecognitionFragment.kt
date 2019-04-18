package com.hse.vasiliy.bcrecognition

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


class RecognitionFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    private lateinit var activity: MainActivity

    private var applicationTag = "RecognitionFragment"

    private lateinit var cardPreview: ImageView
    private lateinit var previewBitmap: Bitmap
    private lateinit var nameText: TextInputEditText
    private lateinit var organizationText: TextInputEditText
    private lateinit var phoneText: TextInputEditText
    private lateinit var emailText: TextInputEditText
    private lateinit var addressText: TextInputEditText

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
        nameText = view.findViewById(R.id.name_edit_text)
        organizationText = view.findViewById(R.id.organization_edit_text)
        phoneText = view.findViewById(R.id.phone_edit_text)
        emailText = view.findViewById(R.id.email_edit_text)
        addressText = view.findViewById(R.id.address_edit_text)

        cardPreview = view.findViewById(R.id.card_preview)

        val bitmapFile = activity.openFileInput(BITMAP_TMP)
        previewBitmap = BitmapFactory.decodeStream(bitmapFile)

        cardPreview.setImageBitmap(previewBitmap)
        val exportToContactBtn = view.findViewById(R.id.create_contact_button) as Button
        exportToContactBtn.setOnClickListener{
            createContactButtonClicked()
        }

        val saveToGalleryBtn = view.findViewById(R.id.save_to_gallery_button) as Button
        saveToGalleryBtn.setOnClickListener{
            saveToGalleryButtonClicked()
        }

        val task = CardProcessor(attachedActivityContext, view, previewBitmap)
        task.execute()
        return view
    }

    private fun createContactButtonClicked() {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, nameText.text.toString())
            putExtra(ContactsContract.Intents.Insert.COMPANY, organizationText.text.toString())
            putExtra(ContactsContract.Intents.Insert.PHONE, phoneText.text.toString())
            putExtra(ContactsContract.Intents.Insert.EMAIL, emailText.text.toString())
            putExtra(ContactsContract.Intents.Insert.POSTAL, addressText.text.toString())
        }
        activity.addContact(intent)
    }

    private fun saveToGalleryButtonClicked() {
        val newItem = CardGalleryContent.CardContactItem(previewBitmap)
        newItem.setName(nameText.text.toString()).
            setCompany(organizationText.text.toString()).
            setPhone(phoneText.text.toString()).
            setEmail(emailText.text.toString()).
            setAddress(addressText.text.toString())
        CardGalleryContent.addItem(newItem)
    }
}
