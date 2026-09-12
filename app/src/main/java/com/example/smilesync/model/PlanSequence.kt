package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class PlanSequence(
    @SerializedName("seq_id")          val seqId: Int = 0,
    @SerializedName("plan_id")         val planId: Int = 0,
    @SerializedName("order_number")    val orderNumber: Int = 0,
    @SerializedName("description")     val description: String = "",
    @SerializedName("phase_type")      val phaseType: Int = 0,
    @SerializedName("procedure_code")  val procedureCode: String? = null,
    @SerializedName("seq_status")      val seqStatus: Int = 0,
    @SerializedName("accomplished_at") val accomplishedAt: String? = null,
    @SerializedName("seq_notes")       val seqNotes: String? = null,
    @SerializedName("created_at")      val createdAt: String? = null
)
