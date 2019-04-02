package com.hse.vasiliy.bcrecognition

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class PermissionRequestConfirmationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(R.string.camera_access_required)
                .setPositiveButton(R.string.ok
                ) { _, _ ->
                    (activity as MainActivity).requestPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_STARTUP_PERMISSIONS)
                }
                .setNegativeButton(R.string.cancel
                ) { _, _ ->
                    (activity as MainActivity).finish()
                }
                .setTitle(R.string.camera_access_required_title)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}