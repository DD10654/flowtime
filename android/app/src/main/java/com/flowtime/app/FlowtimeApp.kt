package com.flowtime.app

import android.app.Application
import com.flowtime.app.di.AppContainer

/**
 * Application entry point. Owns the [AppContainer] that wires the HTTP client,
 * API service, and repository for the whole process.
 */
class FlowtimeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
