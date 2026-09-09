package com.fintrack.shared.feature.core.data.remote

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

object ApiConfig {
    private var currentEnvironment: Environment = Environment.STAGING

    // Change this to your laptop's local IP address (e.g., "192.168.100.96")
    private const val LOCAL_HOST = "192.168.100.96" 
    private const val RENDER_URL = "https://fintrack-ktor.onrender.com"

    val BASE_URL: String
        get() = when (currentEnvironment) {
            Environment.DEVELOPMENT -> "http://$LOCAL_HOST:8080"
            Environment.STAGING -> RENDER_URL
            Environment.PRODUCTION -> RENDER_URL
        }

    fun initialize(environment: Environment) {
        currentEnvironment = environment
    }
}
