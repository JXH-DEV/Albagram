package com.albagram.app

import android.app.Application
import com.albagram.app.data.seed.SeedRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AlbagramApp : Application() {

    @Inject lateinit var seedRepository: SeedRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            seedRepository.ensureSeeded()
        }
    }
}
