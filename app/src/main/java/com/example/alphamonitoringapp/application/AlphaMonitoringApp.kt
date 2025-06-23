package com.example.alphamonitoringapp.application

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AlphaMonitoringApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize FirebaseApp
        val firebaseApp = FirebaseApp.initializeApp(this)

        if (firebaseApp == null) {
            Log.e("AlphaMonitoringApp", "Firebase initialization failed.")
        } else {
            Log.d("AlphaMonitoringApp", "Firebase initialized successfully.")
        }
    }
}
