package com.cadence.player

import android.app.Application
import com.cadence.player.di.AppContainer

class MusicApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
