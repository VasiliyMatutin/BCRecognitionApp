package com.hse.vasiliy.bcrecognition

import android.Manifest
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

        fun newInstance(dialogID: Int, title: String, text: String, positiveButtonText: String, negativeButtonText: String) : ConfirmationDialog {
            val args: Bundle = Bundle()
            args.putInt(DIALOG_ID, dialogID)
            args.putString(TITLE_TAG, title)
            args.putString(TEXT_TAG, text)
            args.putString(POS_BTN_TEXT_TAG, positiveButtonText)
            args.putString(NEG_BTN_TEXT_TAG, negativeButtonText)
            val confirmationDialogInstance = ConfirmationDialog()
            confirmationDialogInstance.arguments = args
            return confirmationDialogInstance
        }
    }
}