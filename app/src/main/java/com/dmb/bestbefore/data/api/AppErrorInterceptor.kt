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
        val response = try {
            chain.proceed(chain.request())
        } catch (e: UnknownHostException) {
            throw NoInternetException()
        } catch (e: ConnectException) {
            throw NoInternetException()
        } catch (e: SocketTimeoutException) {
            throw NoInternetException()
        } catch (e: IOException) {
            if (e.message?.contains("Canceled", ignoreCase = true) == true) throw e
            throw NoInternetException()
        }

        if (response.code in 500..599) {
            response.close()
            throw BackendLoadingException()
        }

        return response
    }
}
