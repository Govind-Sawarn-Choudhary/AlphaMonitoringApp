package com.example.alphamonitoringapp.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alphamonitoringapp.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        greetingTextView = findViewById(R.id.greetingTextView)

        // 👤 Get current user
        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName
        val email = user?.email

        val greeting = if (!name.isNullOrEmpty()) {
            "Hi, $name"
        } else {
            "Hi, $email"
        }

        greetingTextView.text = greeting
    }
}
