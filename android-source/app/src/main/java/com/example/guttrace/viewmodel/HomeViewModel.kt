package com.example.guttrace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.guttrace.data.AppDatabase
import androidx.lifecycle.viewModelScope
import com.example.guttrace.data.EventEntity
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).eventDao()
    val recentEvents = dao.getRecentEvents()

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
