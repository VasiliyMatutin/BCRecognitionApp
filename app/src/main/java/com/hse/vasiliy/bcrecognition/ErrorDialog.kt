package com.hse.vasiliy.bcrecognition

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment

class ErrorDialog() : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(arguments.getString(ERROR_MESSAGE))
                .setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    activity.finish()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object{
        private const val ERROR_MESSAGE = "error_message"

        fun newInstance(errorMessage: String): ErrorDialog = ErrorDialog().apply{
            arguments = Bundle().apply{
                putString(ERROR_MESSAGE, errorMessage)
            }
        }
    }
}