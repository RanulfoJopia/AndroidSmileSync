package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/mobile/webview/generate-token
 * Backend returns: { token, purpose, payload: { patient_id }, expires_at }
 */
data class WebViewTokenResult(
    @SerializedName("token")      val token: String = "",
    @SerializedName("purpose")    val purpose: String = "",
    @SerializedName("payload")    val payload: WebViewPayload? = null,
    @SerializedName("expires_at") val expiresAt: String? = null
) {
    /** Convenience — extracts patient_id from the nested payload object */
    val patientId: Int get() = payload?.patientId ?: 0
}

data class WebViewPayload(
    @SerializedName("patient_id") val patientId: Int = 0
)
