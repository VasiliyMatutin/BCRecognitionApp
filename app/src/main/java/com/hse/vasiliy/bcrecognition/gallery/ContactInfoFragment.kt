package com.hse.vasiliy.bcrecognition.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.hse.vasiliy.bcrecognition.MainActivity

import com.hse.vasiliy.bcrecognition.R
import com.hse.vasiliy.bcrecognition.POSITION


class ContactInfoFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    private lateinit var activity: MainActivity

    private var applicationTag = "CardInfoFragment"

    private lateinit var cardPreview: ImageView
    private lateinit var nameText: TextView
    private lateinit var organizationText: TextView
    private lateinit var phoneText: TextView
    private lateinit var emailText: TextView
    private lateinit var addressText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.contact_info_fragment, container, false)
        nameText = view.findViewById(R.id.name_contact_field)
        organizationText = view.findViewById(R.id.company_contact_field)
        phoneText = view.findViewById(R.id.phone_contact_field)
        emailText = view.findViewById(R.id.email_contact_field)
        addressText = view.findViewById(R.id.address_contact_field)
        cardPreview = view.findViewById(R.id.contact_info_image_representation)

        setupCardInfo(arguments!!.getInt(POSITION))

        val exportToContactBtn = view.findViewById(R.id.export_to_contact) as Button
        exportToContactBtn.setOnClickListener{
            createContactButtonClicked()
        }
        return view
    }

    private fun setupCardInfo(num: Int) {
        val item = CardGalleryContent.ITEMS[num]
        cardPreview.setImageBitmap(item.image)
        nameText.text = item.data.name
        organizationText.text = item.data.company
        phoneText.text = item.data.phone
        emailText.text = item.data.email
        addressText.text = item.data.address
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            attachedActivityContext = context
            activity = attachedActivityContext as MainActivity
        } else {
            Log.e(applicationTag, "Cannot attach fragment to activity")
        }
    }
}
