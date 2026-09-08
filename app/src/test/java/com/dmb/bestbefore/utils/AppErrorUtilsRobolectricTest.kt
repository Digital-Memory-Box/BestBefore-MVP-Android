package com.dmb.bestbefore.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppErrorUtilsRobolectricTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        shadowConnectivityManager = shadowOf(connectivityManager)
    }

    @Test
    fun hasInternetConnectionWithActiveInternetCapabilityReturnsTrue() {
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        
        shadowConnectivityManager.setNetworkCapabilities(connectivityManager.activeNetwork, networkCapabilities)

        val result = AppErrorUtils.hasInternetConnection(context)
        assertTrue(result)
    }

    @Test
    fun hasInternetConnectionWithoutActiveNetworkReturnsFalse() {
        shadowConnectivityManager.setActiveNetworkInfo(null)
        val result = AppErrorUtils.hasInternetConnection(context)
        assertFalse(result)
    }

    @Test
    fun userMessageIdentifies5xxStatusCodesAsLoadingError() {
        val serverError = Exception("HTTP 500 Server Error")
        assertEquals(AppErrorUtils.LOADING_ERROR, AppErrorUtils.userMessage(serverError))

        val gatewayError = Exception("502 Bad Gateway")
        assertEquals(AppErrorUtils.LOADING_ERROR, AppErrorUtils.userMessage(gatewayError))
    }

    @Test
    fun userMessageIdentifiesNetworkExceptionsAsNoInternet() {
        assertEquals(AppErrorUtils.NO_INTERNET, AppErrorUtils.userMessage(UnknownHostException()))
        assertEquals(AppErrorUtils.NO_INTERNET, AppErrorUtils.userMessage(ConnectException()))
        assertEquals(AppErrorUtils.NO_INTERNET, AppErrorUtils.userMessage(SocketTimeoutException()))
        assertEquals(AppErrorUtils.NO_INTERNET, AppErrorUtils.userMessage(NoInternetException()))
    }

    @Test
    fun userMessageFallbackReturnedForGenericErrors() {
        val genericError = IllegalStateException("Unexpected field validation error")
        assertEquals("Fallback error message", AppErrorUtils.userMessage(genericError, "Fallback error message"))
    }
}
