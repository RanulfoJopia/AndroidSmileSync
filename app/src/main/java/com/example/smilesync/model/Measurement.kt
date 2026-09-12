package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class Measurement(
    @SerializedName("measure_id")       val measureId: Int = 0,
    @SerializedName("patient_id")       val patientId: Int = 0,
    @SerializedName("measure_notes")    val measureNotes: String? = null,
    @SerializedName("created_at")       val createdAt: String? = null,
    @SerializedName("updated_at")       val updatedAt: String? = null,
    @SerializedName("tooth_records")    val toothRecords: List<ToothRecord>? = null,
    @SerializedName("odontograms")      val odontograms: List<Odontogram>? = null,
    @SerializedName("tooth_notes")      val toothNotes: List<ToothNote>? = null
)

data class ToothRecord(
    @SerializedName("record_id")    val recordId: Int = 0,
    @SerializedName("measure_id")   val measureId: Int = 0,
    @SerializedName("tooth_number") val toothNumber: Int = 0,
    @SerializedName("status")       val status: String = "present",
    @SerializedName("procedures")   val procedures: List<String>? = null,
    @SerializedName("record_notes") val recordNotes: String? = null
)

data class Odontogram(
    @SerializedName("odont_id")      val odontId: Int = 0,
    @SerializedName("measure_id")    val measureId: Int = 0,
    @SerializedName("tooth_number")  val toothNumber: Int = 0,
    @SerializedName("condition_type") val conditionType: Int = 0,
    @SerializedName("odont_notes")   val odontNotes: String? = null
)

data class ToothNote(
    @SerializedName("note_id")         val noteId: Int = 0,
    @SerializedName("measure_id")      val measureId: Int = 0,
    @SerializedName("tooth_number")    val toothNumber: Int = 0,
    @SerializedName("highlight_color") val highlightColor: String? = null,
    @SerializedName("note_notes")      val noteNotes: String? = null
)
