package com.hse.vasiliy.bcrecognition.helper_dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.content.Context
import android.graphics.Color

class ConfirmationDialog() : DialogFragment() {

    private lateinit var mListener: ConfirmationDialogListener

    interface ConfirmationDialogListener {
        fun onDialogPositiveClick(dialogId: Int)
        fun onDialogNegativeClick(dialogId: Int)
    }

    override fun onStart() {
        super.onStart()
        (dialog as AlertDialog)
            .getButton(AlertDialog.BUTTON_POSITIVE)
            .setBackgroundColor(Color.WHITE)
        (dialog as AlertDialog)
            .getButton(AlertDialog.BUTTON_NEGATIVE)
            .setBackgroundColor(Color.WHITE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = activity as ConfirmationDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement ConfirmationDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(activity)
            builder
                .setMessage(arguments?.getString(TEXT_TAG))
                .setPositiveButton(arguments?.getString(POS_BTN_TEXT_TAG)
                ) { _, _ ->
                    arguments?.getInt(DIALOG_ID)?.let { id -> mListener.onDialogPositiveClick(id) }
                }.setNegativeButton(arguments?.getString(NEG_BTN_TEXT_TAG)
                ) { _, _ ->
                    arguments?.getInt(DIALOG_ID)?.let { id -> mListener.onDialogNegativeClick(id) }
                }.setTitle(arguments?.getString(TITLE_TAG))
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object{

        const val DIALOG_ID = "id"
        const val TITLE_TAG = "title"
        const val TEXT_TAG = "text"
        const val POS_BTN_TEXT_TAG = "positive"
        const val NEG_BTN_TEXT_TAG = "negative"

        fun newInstance(dialogID: Int,
                        title: String,
                        text: String,
                        positiveButtonText: String,
                        negativeButtonText: String
        ) : ConfirmationDialog = ConfirmationDialog().apply {
            arguments = Bundle().apply{
                putInt(DIALOG_ID, dialogID)
                putString(TITLE_TAG, title)
                putString(TEXT_TAG, text)
                putString(POS_BTN_TEXT_TAG, positiveButtonText)
                putString(NEG_BTN_TEXT_TAG, negativeButtonText)
            }
        }
    }
}