package com.example.alphamonitoringapp.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d("CallReceiver", "📞 Call Started")
                // You can trigger any action here if needed
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d("CallReceiver", "📴 Call Ended")
                // You can trigger data upload or other events here
            }
        }
    }
}
