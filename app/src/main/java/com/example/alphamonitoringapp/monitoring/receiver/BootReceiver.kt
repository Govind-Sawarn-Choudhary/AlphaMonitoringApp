package com.example.alphamonitoringapp.monitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.alphamonitoringapp.monitoring.service.ForegroundUploadService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d("BootReceiver", "Device booted or app updated. Starting service...")

            Toast.makeText(context, "Monitoring service starting...", Toast.LENGTH_SHORT).show()

            val serviceIntent = Intent(context, ForegroundUploadService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
