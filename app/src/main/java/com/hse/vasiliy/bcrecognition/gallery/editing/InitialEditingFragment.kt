package com.hse.vasiliy.bcrecognition.gallery.editing

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.hse.vasiliy.bcrecognition.R
import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent

class InitialEditingFragment : EditingFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        if (view != null) {
            val exportToContactBtn = view.findViewById(R.id.create_contact_button) as Button
            exportToContactBtn.setOnClickListener {
                createContactButtonClicked()
            }
            exportToContactBtn.visibility = View.VISIBLE

            val saveToGalleryBtn = view.findViewById(R.id.save_to_gallery_button) as Button
            saveToGalleryBtn.setOnClickListener {
                saveToGalleryButtonClicked()
            }
            saveToGalleryBtn.visibility = View.VISIBLE

            view.findViewById<Button>(R.id.save_edit_button).visibility = View.INVISIBLE
        }

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
        activity.openGallery()
    }

}