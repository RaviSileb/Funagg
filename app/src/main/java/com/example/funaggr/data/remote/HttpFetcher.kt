package com.example.funaggr.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class HttpFetcher @Inject constructor(
    private val client: OkHttpClient
) {
    /** Downloads [url] as text, retrying once on network errors and 5xx responses. */
    suspend fun get(url: String, accept: String = "text/html,application/json;q=0.9,*/*;q=0.8"): String {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RETRY_DELAY_MS)
            try {
                val request = Request.Builder().url(url).header("Accept", accept).build()
                client.newCall(request).await().use { response ->
                    if (response.isSuccessful) {
                        return response.body?.string()
                            ?: throw SourceException("Prázdná odpověď serveru")
                    }
                    val error = SourceException("HTTP ${response.code}")
                    if (response.code < 500) throw error
                    lastError = error
                }
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw SourceException("Síťová chyba: ${lastError?.message ?: "neznámá"}", lastError)
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                cont.resume(response) { _, value, _ -> value.close() }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        })
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 800L
    }
}
