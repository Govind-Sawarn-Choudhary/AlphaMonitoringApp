package com.example.alphamonitoringapp.monitoring.observer

import android.content.Context
import android.util.Log
import com.example.alphamonitoringapp.monitoring.GoogleDataUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PhotosObserver {

    fun onTrigger(context: Context) {
        Log.d("PhotosObserver", "Google Photos sync triggered")

        CoroutineScope(Dispatchers.IO).launch {
            GoogleDataUploader.uploadAll(context) { _: android.content.Intent ->
                Log.e("PhotosObserver", "Token expired. Ask user to re-open app.")
            }
        }
    }
}
