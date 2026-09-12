package com.example.smilesync.model

import com.google.gson.annotations.SerializedName

/** Generic Laravel API envelope: { message, result } */
data class ApiResponse<T>(
    @SerializedName("message") val message: String? = null,
    @SerializedName("result")  val result: T? = null,
    @SerializedName("error")   val error: ApiError? = null
)

data class ApiError(
    @SerializedName("message") val message: String? = null
)

/** Paginated response: { message, result: { count, data } } */
data class PagedResponse<T>(
    @SerializedName("message") val message: String? = null,
    @SerializedName("result")  val result: PagedResult<T>? = null
)

data class PagedResult<T>(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("data")  val data: List<T> = emptyList()
)

/** Auth login/register result */
data class AuthResult(
    @SerializedName("dentist") val dentist: Dentist? = null,
    @SerializedName("token")   val token: String? = null
)

/** Patient list option (from /patients-list) */
data class PatientOption(
    @SerializedName("value") val value: Int = 0,
    @SerializedName("label") val label: String = ""
)

/** Category entry */
data class Category(
    @SerializedName("value") val value: Int = 0,
    @SerializedName("label") val label: String = ""
)

data class CategoriesResult(
    @SerializedName("CASES")    val cases: List<Category>? = null,
    @SerializedName("ANAMNESIS") val anamnesis: Map<String, List<Category>>? = null
)
