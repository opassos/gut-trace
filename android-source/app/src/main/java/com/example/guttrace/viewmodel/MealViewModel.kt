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
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.eventDao()

    fun saveMeal(context: Context, photoFile: File?, tags: List<String>, eventTime: LocalDateTime? = null) {
        viewModelScope.launch {
            val createdTime = LocalDateTime.now()
            val actualTime = eventTime ?: createdTime
            
            val isoCreated = createdTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val isoEvent = actualTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val id = UUID.randomUUID().toString().take(8)

            val payload = JSONObject().apply {
                put("type", "meal")
                put("created_at_utc", isoCreated)
                put("local_datetime", isoEvent)
                put("tags", org.json.JSONArray(tags))
                put("notes", "")
                put("meal_type_inferred", inferMealType(actualTime.hour))
                
                if (photoFile != null && photoFile.exists()) {
                    val finalFile = File(context.filesDir, "${id}.jpg")
                    photoFile.copyTo(finalFile, overwrite = true)
                    put("photo_ids", org.json.JSONArray(listOf(id)))
                }
            }

            val event = EventEntity(
                id = id,
                type = "meal",
                createdAtUtc = isoCreated,
                localDatetime = isoEvent,
                payloadJson = payload.toString(),
                syncStatus = "pending"
            )

            dao.insertEvent(event)
            scheduleSyncIfNeeded(context)
        }
    }

    private fun inferMealType(hour: Int) = when (hour) {
        in 5..10 -> "breakfast"
        in 11..14 -> "lunch"
        in 15..17 -> "snack"
        else -> "dinner"
    }

    private fun scheduleSyncIfNeeded(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "gut_sync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
