package com.zackjp.devicedx.network

import com.zackjp.devicedx.concurrency.DispatcherProvider
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtility @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun calculateLatency(): Long = withContext(dispatcherProvider.io) {
        try {
            val start = System.currentTimeMillis()
            performHeadRequest("https://www.google.com")
            System.currentTimeMillis() - start
        } catch(_: Exception) {
            -1
        }
    }

    private fun performHeadRequest(url: String, timeout: Int = 4000) {
        val url = URL(url)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connectTimeout = timeout
        connection.readTimeout = timeout

        connection.responseCode // executes the request
    }

}
