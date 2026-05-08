package com.example.guttrace.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.guttrace.data.AppDatabase
import com.example.guttrace.data.EventEntity
import com.example.guttrace.sync.SyncWorker
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.eventDao()

    fun saveMedication(context: Context, genericName: String, alias: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val isoNow = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val id = UUID.randomUUID().toString().take(8)

            val payload = JSONObject().apply {
                put("type", "medication")
                put("created_at_utc", isoNow)
                put("local_datetime", isoNow)
                put("medication", genericName)
                put("alias", alias)
                put("notes", "")
            }

            val event = EventEntity(
                id = id,
                type = "medication",
                createdAtUtc = isoNow,
                localDatetime = isoNow,
                payloadJson = payload.toString(),
                syncStatus = "pending"
            )

            dao.insertEvent(event)
            
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("gut_sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
