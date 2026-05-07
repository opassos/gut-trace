package com.example.guttrace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.guttrace.data.AppDatabase

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
}
