package com.dmb.bestbefore.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NoInternetException : IOException("No internet connection")

class BackendLoadingException : IOException("Loading error")

object AppErrorUtils {
    const val NO_INTERNET = "No internet connection"
    const val LOADING_ERROR = "Loading error"

    fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun userMessage(error: Throwable?, fallback: String = LOADING_ERROR): String {
        if (error == null) return fallback

        if (isBackendLoadingError(error)) return LOADING_ERROR
        if (isNoInternet(error)) return NO_INTERNET

        val message = error.message.orEmpty()
        return when {
            message.contains(NO_INTERNET, ignoreCase = true) -> NO_INTERNET
            message.contains(LOADING_ERROR, ignoreCase = true) -> LOADING_ERROR
            Regex("""\b5\d\d\b""").containsMatchIn(message) -> LOADING_ERROR
            message.contains("timeout", ignoreCase = true) -> NO_INTERNET
            message.contains("Unable to resolve host", ignoreCase = true) -> NO_INTERNET
            message.contains("Failed to connect", ignoreCase = true) -> NO_INTERNET
            else -> fallback
        }
    }

    fun isNoInternet(error: Throwable): Boolean {
        return error is NoInternetException ||
            error is UnknownHostException ||
            (
                error !is BackendLoadingException &&
                    (
                        error is ConnectException ||
                            error is SocketTimeoutException ||
                            (error is IOException && error.message?.contains("Canceled", ignoreCase = true) != true)
                    )
            )
    }

    fun isBackendLoadingError(error: Throwable): Boolean {
        return error is BackendLoadingException ||
            Regex("""\b5\d\d\b""").containsMatchIn(error.message.orEmpty())
    }
}
