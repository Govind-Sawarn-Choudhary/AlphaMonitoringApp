package com.example.alphamonitoringapp.monitoring.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.example.alphamonitoringapp.monitoring.DataUploader

class GalleryObserver(
    private val context: Context,
    handler: Handler
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d("GalleryObserver", "📸 Gallery content changed: $uri")
        DataUploader.uploadGallery(context)
    }
}
