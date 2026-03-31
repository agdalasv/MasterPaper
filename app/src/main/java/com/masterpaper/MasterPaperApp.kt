package com.masterpaper

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.masterpaper.data.repository.PreferencesRepository
import com.masterpaper.worker.VersionCheckWorker
import java.util.concurrent.TimeUnit

class MasterPaperApp : Application() {
    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
        scheduleVersionCheck()
    }

    private fun scheduleVersionCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<VersionCheckWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            VersionCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
