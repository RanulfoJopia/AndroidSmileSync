package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class PatientDocument(
    @SerializedName("document_id")   val documentId: Int = 0,
    @SerializedName("patient_id")    val patientId: Int = 0,
    @SerializedName("doc_title")     val docTitle: String = "",
    @SerializedName("doc_type")      val docType: Int? = null,
    @SerializedName("doc_notes")     val docNotes: String? = null,
    @SerializedName("file_path")     val filePath: String? = null,
    @SerializedName("created_at")    val createdAt: String? = null
)
