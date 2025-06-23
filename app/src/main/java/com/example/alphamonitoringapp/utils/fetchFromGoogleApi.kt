package com.example.alphamonitoringapp.utils

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

fun fetchFromGoogleApi(url: URL, token: String): JSONObject? {
    return try {
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response)
        } else {
            Log.e("fetchFromGoogleApi", "⚠️ HTTP $responseCode: ${connection.responseMessage}")
            null
        }
    } catch (e: Exception) {
        Log.e("fetchFromGoogleApi", "❌ Network error: ${e.localizedMessage}")
        null
    }
}
