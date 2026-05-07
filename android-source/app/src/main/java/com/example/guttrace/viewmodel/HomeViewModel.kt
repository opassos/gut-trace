package com.example.guttrace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.guttrace.data.AppDatabase

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).eventDao()
    val recentEvents = dao.getRecentEvents()
}
