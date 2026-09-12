package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

data class Dentist(
    @SerializedName("dentist_id")  val dentistId: Int = 0,
    @SerializedName("first_name")  val firstName: String = "",
    @SerializedName("last_name")   val lastName: String = "",
    @SerializedName("email")       val email: String = "",
    @SerializedName("created_at")  val createdAt: String? = null,
    @SerializedName("updated_at")  val updatedAt: String? = null
) {
    val fullName get() = "$firstName $lastName".trim()
    val initials  get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
}
