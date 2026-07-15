package com.flowtime.app.di

import com.flowtime.app.BuildConfig
import com.flowtime.app.data.remote.ApiService
import com.flowtime.app.data.remote.HttpClient
import com.flowtime.app.data.repository.FlowtimeRepository

/** Manual dependency graph — one instance per process, held by the Application. */
class AppContainer {
    private val api: ApiService = HttpClient.create(BuildConfig.API_BASE_URL)
    val repository: FlowtimeRepository = FlowtimeRepository(api)
}
