package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class TreatmentCase(
    @SerializedName("case_id")           val caseId: Int = 0,
    @SerializedName("patient_id")        val patientId: Int = 0,
    @SerializedName("dentist_id")        val dentistId: Int = 0,
    @SerializedName("case_title")        val caseTitle: String = "",
    @SerializedName("case_description")  val caseDescription: String? = null,
    @SerializedName("chief_complaint")   val chiefComplaint: String? = null,
    @SerializedName("dentist_diagnosis") val dentistDiagnosis: String? = null,
    @SerializedName("clinical_findings") val clinicalFindings: String? = null,
    @SerializedName("case_type")         val caseType: Int? = null,
    @SerializedName("vital_signs")       val vitalSigns: String? = null,
    @SerializedName("case_notes")        val caseNotes: String? = null,
    @SerializedName("created_at")        val createdAt: String? = null,
    @SerializedName("updated_at")        val updatedAt: String? = null,
    // Joined fields (from list endpoints)
    @SerializedName("first_name")        val firstName: String? = null,
    @SerializedName("last_name")         val lastName: String? = null,
    @SerializedName("middle_name")       val middleName: String? = null,
    @SerializedName("name_suffix")       val nameSuffix: String? = null,
    @SerializedName("plan_id")           val planId: Int? = null,
    @SerializedName("consent_given_at")  val consentGivenAt: String? = null,
    @SerializedName("sequences_count")   val sequencesCount: Int? = null
) {
    val patientFullName: String get() {
        val parts = listOfNotNull(
            lastName?.ifBlank { null },
            firstName?.ifBlank { null }
        )
        return if (parts.isEmpty()) "Unknown Patient" else parts.joinToString(", ")
    }
    val hasPlan: Boolean get() = planId != null
}

