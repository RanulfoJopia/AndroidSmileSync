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

/** 
 * Categories API response structure
 * Note: Backend returns nested objects/maps instead of arrays
 * CASES is a map of category_key -> Category object
 * ANAMNESIS has nested structure we don't currently use
 */
data class CategoriesResult(
    // API returns CASES as Map<String, Category> not List<Category>
    @SerializedName("CASES")    val cases: Map<String, Category>? = null,
    // ANAMNESIS structure varies in API response - not currently used
    @SerializedName("ANAMNESIS") val anamnesis: Any? = null
)
