package com.dmb.bestbefore.utils

import com.dmb.bestbefore.data.api.ApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ServerWarmupHelperTest {

    private val apiService = mockk<ApiService>()
    private val helper = ServerWarmupHelper(apiService = apiService)

    @Test
    fun `performWarmup with fast response goes straight to READY without WARMING_UP`() = runTest {
        coEvery { apiService.health() } returns Response.success(mapOf("status" to "ok"))

        var isConnecting = true
        var isWarmingUp = false
        var isReady = false

        helper.performWarmup(
            scope = this,
            isConnecting = { isConnecting },
            onWarmingUp = { isWarmingUp = true },
            onReady = {
                isConnecting = false
                isReady = true
            }
        )

        assertTrue(isReady)
        assertFalse(isWarmingUp)
    }

    @Test
    fun `performWarmup with slow response transitions to WARMING_UP after 2s and READY on finish`() = runTest {
        coEvery { apiService.health() } coAnswers {
            delay(5_000)
            Response.success(mapOf("status" to "ok"))
        }

        var isConnecting = true
        var isWarmingUp = false
        var isReady = false

        helper.performWarmup(
            scope = this,
            isConnecting = { isConnecting },
            onWarmingUp = { isWarmingUp = true },
            onReady = {
                isConnecting = false
                isReady = true
            }
        )

        assertTrue(isWarmingUp)
        assertTrue(isReady)
    }

    @Test
    fun `performWarmup with network exception calls onError and completes with READY`() = runTest {
        val testException = RuntimeException("Connection refused")
        coEvery { apiService.health() } throws testException

        var caughtError: Exception? = null
        var isReady = false

        helper.performWarmup(
            scope = this,
            isConnecting = { true },
            onWarmingUp = {},
            onReady = { isReady = true },
            onError = { caughtError = it }
        )

        assertTrue(isReady)
        assertNotNull(caughtError)
        assertEquals("Connection refused", caughtError?.message)
    }

    @Test
    fun `performWarmup with HTTP 500 error proceeds and completes with READY`() = runTest {
        coEvery { apiService.health() } returns Response.error(500, "Internal Server Error".toResponseBody())

        var isReady = false

        helper.performWarmup(
            scope = this,
            isConnecting = { true },
            onWarmingUp = {},
            onReady = { isReady = true }
        )

        assertTrue(isReady)
    }
}
