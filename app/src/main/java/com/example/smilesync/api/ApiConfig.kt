package com.example.smilesync.api

/**
 * Central API configuration.
 *
 * EMULATOR:  use 10.0.2.2 (maps to host machine's localhost)
 * PHYSICAL:  use your machine's LAN IP, e.g. "http://192.168.1.X:8000/api/"
 *
 * Change BASE_URL here to switch environments.
 */
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8000/api/"
    const val TIMEOUT_SECONDS = 30L
}
