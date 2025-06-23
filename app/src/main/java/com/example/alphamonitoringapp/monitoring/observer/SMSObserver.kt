package com.example.alphamonitoringapp.monitoring.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import com.example.alphamonitoringapp.monitoring.DataUploader

class SMSObserver(
    private val context: Context,
    handler: Handler
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Toast.makeText(context, "SMS changed, uploading...", Toast.LENGTH_SHORT).show()
        DataUploader.uploadAllData(context)
    }
}
