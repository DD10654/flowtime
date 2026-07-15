package com.flowtime.app.ui.common

import com.flowtime.app.data.remote.HttpClient
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Serializable
private data class ErrorBody(val error: String? = null)

/** Turn an exception from the API into a human-friendly message. */
fun errorMessage(t: Throwable): String = when (t) {
    is HttpException -> {
        val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body
            ?.let { runCatching { HttpClient.json.decodeFromString<ErrorBody>(it).error }.getOrNull() }
        parsed ?: "Request failed (${t.code()})."
    }
    is ConnectException, is UnknownHostException ->
        "Can't reach the server. Make sure the web app is running (npm run dev)."
    is SocketTimeoutException -> "The server took too long to respond."
    is IOException -> "Network error. Check your connection."
    else -> t.message ?: "Something went wrong."
}
