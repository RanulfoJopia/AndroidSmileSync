package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class CasePlan(
    @SerializedName("plan_id")                val planId: Int = 0,
    @SerializedName("case_id")                val caseId: Int = 0,
    @SerializedName("clinical_justification") val clinicalJustification: String = "",
    @SerializedName("risk_complications")     val riskComplications: String = "",
    @SerializedName("prognosis")              val prognosis: String = "",
    @SerializedName("plan_status")            val planStatus: String? = null,
    @SerializedName("consent_given_at")       val consentGivenAt: String? = null,
    @SerializedName("plan_result")            val planResult: String? = null,
    @SerializedName("created_at")             val createdAt: String? = null,
    @SerializedName("updated_at")             val updatedAt: String? = null
)