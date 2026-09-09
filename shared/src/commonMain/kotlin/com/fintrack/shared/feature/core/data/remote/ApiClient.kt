package com.fintrack.shared.feature.core.data.remote

import com.fintrack.shared.feature.auth.data.model.AuthResponseDto
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class ApiClient(
    private val tokenDataSource: TokenDataSource,
    private val baseUrl: String,
) {
    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30000L
                connectTimeoutMillis = 10000L
                socketTimeoutMillis = 30000L
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenDataSource.accessToken.firstOrNull()
                        val refreshToken = tokenDataSource.refreshToken.firstOrNull()
                        
                        if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
                            // logger.debug(LogTags.AUTH, "Loading tokens for request: ${accessToken.take(10)}...")
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            // logger.debug(LogTags.AUTH, "No tokens available for request")
                            null
                        }
                    }

                    refreshTokens {
                        val refreshToken =
                            tokenDataSource.refreshToken.firstOrNull() ?: return@refreshTokens null

                        try {
                            // Note: Ktor's bearer auth automatically excludes the refresh request 
                            // from further auth interceptors to avoid infinite loops.
                            val response = client.post("auth/refresh") {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("refreshToken" to refreshToken))
                            }.body<AuthResponseDto>()

                            tokenDataSource.saveTokens(response.accessToken, response.refreshToken)
                            BearerTokens(response.accessToken, response.refreshToken)
                        } catch (e: Exception) {
                            val isAuthError = when (e) {
                                is ClientRequestException -> {
                                    val status = e.response.status.value
                                    status == 401 || status == 403
                                }
                                else -> e.message?.contains("401") == true || e.message?.contains("403") == true
                            }

                            if (isAuthError) {
                                tokenDataSource.clearTokens()
                            }
                            null
                        }
                    }
                    
                    sendWithoutRequest { request ->
                        val path = request.url.build().encodedPath
                        // Send credentials for everything EXCEPT public auth endpoints
                        !(path.contains("/auth/login") || 
                          path.contains("/auth/register") || 
                          path.contains("/auth/refresh"))
                    }
                }
            }

            expectSuccess = true
            defaultRequest { 
                url(baseUrl.takeIf { it.endsWith("/") } ?: "$baseUrl/")
                contentType(ContentType.Application.Json) 
            }
        }
    }
}