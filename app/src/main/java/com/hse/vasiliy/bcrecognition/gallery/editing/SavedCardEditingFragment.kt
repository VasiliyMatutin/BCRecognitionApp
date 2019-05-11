package com.hse.vasiliy.bcrecognition.gallery.editing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.hse.vasiliy.bcrecognition.R
import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent

class SavedCardEditingFragment : EditingFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        if (view != null) {
            view.findViewById<Button>(R.id.create_contact_button).visibility = View.INVISIBLE
            view.findViewById<Button>(R.id.save_to_gallery_button).visibility = View.INVISIBLE

            val saveEditBtn = view.findViewById(R.id.save_edit_button) as Button
            saveEditBtn.setOnClickListener {
                saveEditButtonClicked()
            }
            view.findViewById<Button>(R.id.save_edit_button).visibility = View.VISIBLE
        }

        return view
    }

    private fun saveEditButtonClicked() {
        if (item != null) {
            item!!.name = nameText.text.toString()
            item!!.company = organizationText.text.toString()
            item!!.phone = phoneText.text.toString()
            item!!.email = emailText.text.toString()
            item!!.address = addressText.text.toString()
            CardGalleryContent.rewriteElement(item!!)
        }
        activity.supportFragmentManager.popBackStackImmediate()
    }

}