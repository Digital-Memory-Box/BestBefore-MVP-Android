package com.dmb.bestbefore.data.api

import com.dmb.bestbefore.utils.BackendLoadingException
import com.dmb.bestbefore.utils.NoInternetException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var tryCount = 0
        val maxRetries = 1

        while (tryCount <= maxRetries) {
            try {
                response = chain.proceed(chain.request())
                if (response.code == 429 && tryCount < maxRetries) {
                    response.close()
                    tryCount++
                    continue
                }
                break
            } catch (e: Exception) {
                if (e is UnknownHostException) {
                    throw NoInternetException()
                }
                if (tryCount >= maxRetries) {
                    when (e) {
                        is ConnectException -> throw NoInternetException()
                        is SocketTimeoutException -> throw NoInternetException()
                        is IOException -> {
                            if (e.message?.contains("Canceled", ignoreCase = true) == true) throw e
                            throw NoInternetException()
                        }
                        else -> throw e
                    }
                }
                tryCount++
            }
        }

        val finalResponse = response ?: throw NoInternetException()

        if (finalResponse.code == 503 || finalResponse.code == 504) {
            finalResponse.close()
            throw BackendLoadingException()
        }

        return finalResponse
    }
}
