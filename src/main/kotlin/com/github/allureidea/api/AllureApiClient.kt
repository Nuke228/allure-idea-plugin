package com.github.allureidea.api

import com.github.allureidea.api.dto.ProjectDto
import com.github.allureidea.api.dto.ProjectListResponse
import com.github.allureidea.api.dto.TestCaseRequest
import com.github.allureidea.api.dto.TestCaseResponse
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

class AllureApiClient(private val baseUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getProjects(jwt: String): List<ProjectDto> {
        val conn = URI("$baseUrl/api/rs/project?page=0&size=500&sort=name,ASC").toURL()
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $jwt")
        conn.setRequestProperty("Accept", "application/json")

        try {
            val code = conn.responseCode
            if (code != 200) {
                val error = runCatching { conn.errorStream?.bufferedReader()?.readText() }
                    .getOrNull() ?: "HTTP $code"
                throw AllureApiException("Failed to load projects (HTTP $code): $error", code)
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            return json.decodeFromString<ProjectListResponse>(responseBody).content
        } finally {
            conn.disconnect()
        }
    }

    fun createTestCase(jwt: String, request: TestCaseRequest): TestCaseResponse {
        val requestBody = json.encodeToString(TestCaseRequest.serializer(), request)

        val conn = URI("$baseUrl/api/rs/testcase").toURL()
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $jwt")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        try {
            conn.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = runCatching { conn.errorStream?.bufferedReader()?.readText() }
                    .getOrNull() ?: "HTTP $code"
                throw AllureApiException("Failed to create test case (HTTP $code): $error", code)
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            return json.decodeFromString<TestCaseResponse>(responseBody)
        } finally {
            conn.disconnect()
        }
    }
}
