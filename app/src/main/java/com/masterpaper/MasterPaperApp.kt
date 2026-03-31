package com.masterpaper

import android.app.Application
import com.masterpaper.data.repository.PreferencesRepository

class MasterPaperApp : Application() {
    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
    }
}
