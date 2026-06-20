package com.example.guttrace.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.guttrace.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("gut_prefs", Context.MODE_PRIVATE)
        val serverIp = prefs.getString("server_ip", "192.168.0.200") ?: "192.168.0.200"
        val dynamicServerUrl = "http://$serverIp:8000/sync/events"

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.eventDao()
        val pendingEvents = dao.getPendingEvents()

        if (pendingEvents.isEmpty()) {
            return@withContext Result.success()
        }

        try {
            val jsonArray = JSONArray()
            val idsToMark = mutableListOf<String>()
            var allPhotosUploaded = true

            for (event in pendingEvents) {
                val photoFile = java.io.File(applicationContext.filesDir, "${event.id}.jpg")
                if (photoFile.exists()) {
                    if (!uploadPhoto(photoFile, event.id, serverIp)) {
                        allPhotosUploaded = false
                        continue // Skip syncing JSON if photo failed
                    } else {
                        photoFile.delete() // Cleanup
                    }
                }
                
                val jsonObj = JSONObject(event.payloadJson)
                jsonObj.put("id", event.id)
                jsonArray.put(jsonObj)
                idsToMark.add(event.id)
            }

            if (jsonArray.length() == 0) {
                return@withContext if (allPhotosUploaded) Result.success() else Result.retry()
            }

            val url = URL(dynamicServerUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonArray.toString())
            }

            if (connection.responseCode == 200) {
                dao.markAsSynced(idsToMark)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun uploadPhoto(photoFile: java.io.File, photoId: String, serverIp: String): Boolean {
        if (!photoFile.exists()) return true
        
        val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
        val url = java.net.URL("http://$serverIp:8000/sync/photos")
        val connection = url.openConnection() as java.net.HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            
            val outputStream = java.io.DataOutputStream(connection.outputStream)
            
            outputStream.writeBytes("--$boundary\r\n")
            outputStream.writeBytes("Content-Disposition: form-data; name=\"photo_id\"\r\n\r\n")
            outputStream.writeBytes("$photoId\r\n")
            
            outputStream.writeBytes("--$boundary\r\n")
            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$photoId.jpg\"\r\n")
            outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")
            
            photoFile.inputStream().use { it.copyTo(outputStream) }
            
            outputStream.writeBytes("\r\n--$boundary--\r\n")
            outputStream.flush()
            outputStream.close()
            
            return connection.responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
