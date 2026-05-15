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

    fun saveMedication(context: Context, genericName: String, alias: String, eventTime: LocalDateTime? = null) {
        viewModelScope.launch {
            val createdTime = LocalDateTime.now()
            val actualTime = eventTime ?: createdTime
            
            val isoCreated = createdTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val isoEvent = actualTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val id = UUID.randomUUID().toString().take(8)

            val payload = JSONObject().apply {
                put("type", "medication")
                put("created_at_utc", isoCreated)
                put("local_datetime", isoEvent)
                put("medication", genericName)
                put("alias", alias)
                put("notes", "")
            }

            val event = EventEntity(
                id = id,
                type = "medication",
                createdAtUtc = isoCreated,
                localDatetime = isoEvent,
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
