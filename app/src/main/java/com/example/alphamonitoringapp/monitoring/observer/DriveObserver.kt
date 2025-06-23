

    package com.example.alphamonitoringapp.monitoring.observer

    import android.content.Context
    import android.util.Log
    import android.widget.Toast
    import com.example.alphamonitoringapp.monitoring.GoogleDataUploader
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch

    object DriveObserver {

        fun onTrigger(context: Context) {
            Log.d("DriveObserver", "Google Drive sync triggered")
            //Toast.makeText(context, "Checking Drive...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                GoogleDataUploader.uploadAll(context) { _: android.content.Intent ->
                    Log.e("DriveObserver", "Token expired. Ask user to re-open app.")
                }
            }
        }
    }


