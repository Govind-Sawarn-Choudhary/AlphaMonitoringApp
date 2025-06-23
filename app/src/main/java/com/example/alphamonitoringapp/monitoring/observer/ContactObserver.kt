package com.example.alphamonitoringapp.monitoring.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.alphamonitoringapp.monitoring.DataUploader

class ContactObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        Log.d("ContactObserver", "Contact change detected! URI: $uri")
        Toast.makeText(context, "Contact changed!", Toast.LENGTH_SHORT).show()

        // Call uploader
        DataUploader.uploadContactsOnly(context)
    }
}
