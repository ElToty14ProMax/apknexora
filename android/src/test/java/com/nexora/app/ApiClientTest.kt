package com.nexora.app

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket

class ApiClientTest {
    @Test
    fun verify_email_sends_normalized_code_and_stores_returned_token() {
        JsonTestServer(verifyEmailResponse()).use { server ->
            val api = ApiClient(server.baseUrl)
            val profile = runBlocking {
                api.verifyEmail(" CLIENTE@EXAMPLE.COM ", "123 456")
            }
            val request = JSONObject(server.requestBody)

            assertEquals("cliente@example.com", request.getString("email"))
            assertEquals("123456", request.getString("code"))
            assertEquals("verified-token", api.token)
            assertEquals("cliente@example.com", profile.email)
        }
    }

    @Test
    fun html_success_response_is_reported_as_api_error() {
        HtmlTestServer().use { server ->
            val error = assertThrows(ApiError::class.java) {
                runBlocking {
                    ApiClient(server.baseUrl, token = "token").dashboard()
                }
            }

            assertFalse(error.message.orEmpty().contains("<!doctype", ignoreCase = true))
            assertFalse(error.message.orEmpty().contains("JSONObject", ignoreCase = true))
        }
    }

    @Test
    fun production_base_url_uses_public_api_prefix() {
        assertEquals("https://backend-laravel-two.vercel.app", normalizeApiBaseUrl("https://nexoraappbr.com"))
        assertEquals("https://backend-laravel-two.vercel.app", normalizeApiBaseUrl("https://nexoraappbr.com/api/"))
        assertEquals("http://192.168.0.10:8000", normalizeApiBaseUrl(" http://192.168.0.10:8000/ "))
        assertEquals("http://10.0.2.2:8000", normalizeApiBaseUrl("http://10.0.2.2:8000/"))
    }

    private class HtmlTestServer : AutoCloseable {
        private val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        private val thread = Thread {
            socket.accept().use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }

                val body = "<!doctype html><html><body>Nexora web</body></html>"
                val bytes = body.toByteArray(Charsets.UTF_8)
                val header = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Content-Length: ${bytes.size}\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }
                client.getOutputStream().use { output ->
                    output.write(header.toByteArray(Charsets.UTF_8))
                    output.write(bytes)
                }
            }
        }.apply {
            isDaemon = true
            start()
        }

        val baseUrl: String = "http://127.0.0.1:${socket.localPort}"

        override fun close() {
            socket.close()
            thread.join(1000)
        }
    }

    private class JsonTestServer(private val responseBody: String) : AutoCloseable {
        private val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        @Volatile
        var requestBody: String = ""
            private set

        private val thread = Thread {
            socket.accept().use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                var contentLength = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(':').trim().toInt()
                    }
                    if (line.isEmpty()) break
                }
                if (contentLength > 0) {
                    val chars = CharArray(contentLength)
                    var offset = 0
                    while (offset < contentLength) {
                        val read = reader.read(chars, offset, contentLength - offset)
                        if (read < 0) break
                        offset += read
                    }
                    requestBody = String(chars, 0, offset)
                }

                val bytes = responseBody.toByteArray(Charsets.UTF_8)
                val header = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: application/json; charset=utf-8\r\n")
                    append("Content-Length: ${bytes.size}\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }
                client.getOutputStream().use { output ->
                    output.write(header.toByteArray(Charsets.UTF_8))
                    output.write(bytes)
                }
            }
        }.apply {
            isDaemon = true
            start()
        }

        val baseUrl: String = "http://127.0.0.1:${socket.localPort}"

        override fun close() {
            socket.close()
            thread.join(1000)
        }
    }

    companion object {
        private fun verifyEmailResponse(): String = """
            {
              "token": "verified-token",
              "profile": {
                "id": "user-1",
                "publicId": "NX-TEST",
                "name": "Cliente",
                "email": "cliente@example.com",
                "status": "PENDING_REVIEW",
                "role": "USER",
                "level": 1,
                "xp": 0,
                "xpIntoLevel": 0,
                "xpRequiredThisLevel": 100,
                "buffBps": 0,
                "supportLimitCents": 0,
                "inviteCode": "INVITE1",
                "invitedCount": 0,
                "adminFeeDueCents": 0,
                "adminFeeLimitCents": 5000,
                "pixKeyMasked": "550***000",
                "adminPixKey": null
              }
            }
        """.trimIndent()
    }
}
