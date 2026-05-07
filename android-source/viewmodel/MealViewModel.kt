package com.guttrace.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.guttrace.data.AppDatabase
import com.guttrace.data.EventEntity
import com.guttrace.sync.SyncWorker
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

    fun saveMeal(context: Context, photoFile: File?, tags: List<String>) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val isoNow = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val id = UUID.randomUUID().toString().take(8)

            val payload = JSONObject().apply {
                put("type", "meal")
                put("created_at_utc", isoNow)
                put("local_datetime", isoNow)
                put("tags", org.json.JSONArray(tags))
                put("notes", "")
                put("meal_type_inferred", inferMealType(now.hour))
            }

            val event = EventEntity(
                id = id,
                type = "meal",
                createdAtUtc = isoNow,
                localDatetime = isoNow,
                payloadJson = payload.toString(),
                syncStatus = "pending"
            )

            dao.insertEvent(event)
            scheduleSyncIfNeeded(context)

            // TODO: Upload photo separately via /sync/photos if photoFile != null
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
