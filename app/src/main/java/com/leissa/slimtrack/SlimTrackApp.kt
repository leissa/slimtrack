package com.leissa.slimtrack

import android.app.Application
import com.leissa.slimtrack.data.AppContainer

class SlimTrackApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
