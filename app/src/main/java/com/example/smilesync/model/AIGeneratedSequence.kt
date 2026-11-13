package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

/**
 * Model for AI-generated sequences where phase_type comes as a string.
 * This is converted to proper integer codes before saving to the backend.
 */
data class AIGeneratedSequence(
    @SerializedName("order_number")    val orderNumber: Int = 0,
    @SerializedName("description")     val description: String = "",
    @SerializedName("phase_type")      val phaseType: String? = null,  // String from AI
    @SerializedName("procedure_code")  val procedureCode: String? = null,
    @SerializedName("dependency_json") val dependencyJson: List<Int>? = null,
    @SerializedName("seq_notes")       val seqNotes: String? = null
) {
    /**
     * Convert AI string phase to integer code matching the phase options in CaseSingleActivity.
     * Phase options: "— None —", "Diagnostic", "Preventive", "Restorative",
     *                "Surgical", "Prosthodontic", "Orthodontic", "Periodontic", "Endodontic", "Maintenance"
     */
    fun toPhaseInt(): Int {
        return when (phaseType?.trim()) {
            "Diagnostic"     -> 1
            "Preventive"     -> 2
            "Restorative"    -> 3
            "Surgical"       -> 4
            "Prosthodontic"  -> 5
            "Orthodontic"    -> 6
            "Periodontic"    -> 7
            "Endodontic"     -> 8
            "Maintenance"    -> 9
            else             -> 0  // None
        }
    }
}
