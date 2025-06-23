package com.example.alphamonitoringapp.monitoring.observer

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.alphamonitoringapp.monitoring.GoogleDataUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GmailObserver {

    fun onTrigger(context: Context) {
        Log.d("GmailObserver", "Gmail sync triggered")
        //Toast.makeText(context, "Checking Gmail...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            GoogleDataUploader.uploadAll(context) { _: android.content.Intent ->
                Log.e("GmailObserver", "Token expired. Ask user to re-open app.")
            }
        }
    }
}
