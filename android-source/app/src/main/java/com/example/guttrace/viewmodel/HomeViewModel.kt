package com.example.guttrace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.guttrace.data.AppDatabase
import androidx.lifecycle.viewModelScope
import com.example.guttrace.data.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).eventDao()
    val recentEvents = dao.getRecentEvents()

    private val _serverStatus = MutableStateFlow<Boolean>(false)
    val serverStatus: StateFlow<Boolean> = _serverStatus.asStateFlow()

    init {
        startHealthCheck(application)
        
        // One-time patch to fix the incorrect future dates on the device
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sqliteDb = AppDatabase.getDatabase(application).openHelper.writableDatabase
                sqliteDb.execSQL("UPDATE events SET localDatetime = REPLACE(localDatetime, '2026-05-15T2', '2026-05-14T2'), payloadJson = REPLACE(payloadJson, '2026-05-15T2', '2026-05-14T2') WHERE localDatetime LIKE '2026-05-15T2%'")
            } catch (e: Exception) {
                // Ignore if it fails
            }
        }
    }

    private fun startHealthCheck(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val prefs = context.getSharedPreferences("gut_prefs", android.content.Context.MODE_PRIVATE)
                val serverIp = prefs.getString("server_ip", "192.168.0.200") ?: "192.168.0.200"
                _serverStatus.value = checkServerHealth(serverIp)
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun checkServerHealth(serverIp: String): Boolean {
        return try {
            val url = URL("http://$serverIp:8000/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun forceSync(context: android.content.Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<com.example.guttrace.sync.SyncWorker>()
            .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "gut_sync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun deleteEvent(event: EventEntity, context: android.content.Context) {
        viewModelScope.launch {
            if (event.syncStatus == "pending") {
                dao.deleteEventById(event.id)
            } else {
                val newPayload = JSONObject(event.payloadJson).apply {
                    put("type", "deleted")
                }
                val updatedEvent = event.copy(
                    type = "deleted",
                    payloadJson = newPayload.toString(),
                    syncStatus = "pending"
                )
                dao.insertEvent(updatedEvent)
                forceSync(context)
            }
        }
    }
}
