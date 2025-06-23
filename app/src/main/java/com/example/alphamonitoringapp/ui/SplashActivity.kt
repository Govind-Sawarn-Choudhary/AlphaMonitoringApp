package com.example.alphamonitoringapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.alphamonitoringapp.auth.LoginScreen
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // ✅ Already logged in, go to PermissionActivity
            startActivity(Intent(this, PermissionActivity::class.java))
        } else {
            // ❌ Not logged in, go to LoginScreen
            startActivity(Intent(this, LoginScreen::class.java))
        }

        finish()
    }
}
