package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class PatientAnamnesis(
    @SerializedName("anamnesis_id")     val anamnesisId: Int = 0,
    @SerializedName("patient_id")       val patientId: Int = 0,
    @SerializedName("anam_type")        val anamType: Int? = null,
    @SerializedName("anam_description") val anamDescription: String = "",
    @SerializedName("treatment")        val treatment: String? = null,
    @SerializedName("source_name")      val sourceName: String? = null,
    @SerializedName("severity_name")    val severityName: String? = null,
    @SerializedName("family_inherited") val familyInherited: Boolean = false,
    @SerializedName("date_start")       val dateStart: String? = null,
    @SerializedName("date_end")         val dateEnd: String? = null,
    @SerializedName("created_at")       val createdAt: String? = null
)
