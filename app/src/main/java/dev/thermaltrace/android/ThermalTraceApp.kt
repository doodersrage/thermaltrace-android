package dev.thermaltrace.android

import android.app.Application

class ThermalTraceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
