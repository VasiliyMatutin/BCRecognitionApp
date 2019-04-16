package com.hse.vasiliy.bcrecognition

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ErrorDialog() : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(arguments?.getString(ERROR_MESSAGE))
                .setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    if (arguments?.getBoolean(IS_CRITICAL) == true) {
                        activity?.finish()
                    }
                }
            if (arguments?.getBoolean(IS_CRITICAL) == true) {
                builder.setTitle(R.string.error_title)
            } else {
                builder.setTitle(R.string.warning_title)
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object{
        private const val ERROR_MESSAGE = "error_message"
        private const val IS_CRITICAL = "is_critical"

        fun newInstance(errorMessage: String, isCritical: Boolean = true): ErrorDialog = ErrorDialog().apply{
            arguments = Bundle().apply{
                putString(ERROR_MESSAGE, errorMessage)
                putBoolean(IS_CRITICAL, isCritical)
            }
        }
    }
}