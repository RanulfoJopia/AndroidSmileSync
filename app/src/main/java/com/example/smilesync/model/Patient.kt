package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class Patient(
    @SerializedName("patient_id")            val patientId: Int = 0,
    @SerializedName("dentist_id")            val dentistId: Int = 0,
    @SerializedName("first_name")            val firstName: String = "",
    @SerializedName("middle_name")           val middleName: String? = null,
    @SerializedName("last_name")             val lastName: String = "",
    @SerializedName("name_suffix")           val nameSuffix: String? = null,
    @SerializedName("birthdate")             val birthdate: String? = null,
    @SerializedName("sex")                   val sex: String? = null,
    @SerializedName("address")              val address: String? = null,
    @SerializedName("created_at")            val createdAt: String? = null,
    @SerializedName("updated_at")            val updatedAt: String? = null,
    // Joined/computed fields from API
    @SerializedName("case_title")            val caseTitle: String? = null,
    @SerializedName("case_type")             val caseType: Int? = null,
    @SerializedName("cases_count")           val casesCount: Int? = null,
    @SerializedName("completed_cases_count") val completedCasesCount: Int? = null,
    @SerializedName("latest_case")           val latestCase: TreatmentCase? = null
) {
    /** "Last, First Middle Suffix" */
    val fullName: String get() {
        val parts = listOfNotNull(
            lastName.ifBlank { null },
            firstName.ifBlank { null }?.let { fn ->
                fn + (middleName?.let { " $it" } ?: "") + (nameSuffix?.let { " $it" } ?: "")
            }
        )
        return parts.joinToString(", ")
    }

    val initials: String get() =
        "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()

    val sexLabel: String get() = when (sex) {
        "M" -> "Male"
        "F" -> "Female"
        else -> "N/A"
    }

    fun ageYears(): Int? {
        if (birthdate.isNullOrBlank()) return null
        return try {
            val cal = java.util.Calendar.getInstance()
            val now = cal.get(java.util.Calendar.YEAR)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val bd  = java.util.Calendar.getInstance().also { it.time = sdf.parse(birthdate)!! }
            now - bd.get(java.util.Calendar.YEAR)
        } catch (e: Exception) { null }
    }
}
