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
        val serverIp = prefs.getString("server_ip", "10.0.2.2") ?: "10.0.2.2"
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

            for (event in pendingEvents) {
                val jsonObj = JSONObject(event.payloadJson)
                jsonObj.put("id", event.id)
                jsonArray.put(jsonObj)
                idsToMark.add(event.id)
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
}
