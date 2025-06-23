package com.example.alphamonitoringapp.monitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast
import com.example.alphamonitoringapp.monitoring.DataUploader

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Toast.makeText(context, "Call state changed. Uploading...", Toast.LENGTH_SHORT).show()
            DataUploader.uploadAllData(context)
        }
    }
}
