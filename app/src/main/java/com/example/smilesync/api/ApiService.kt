package com.example.smilesync.api

import com.example.smilesync.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // -----------------------------------------------------------------------
    // AUTH
    // -----------------------------------------------------------------------

    @POST("login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiResponse<AuthResult>>

    @POST("register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiResponse<AuthResult>>

    @POST("logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("user")
    suspend fun getUser(): Response<ApiResponse<Dentist>>

    @PATCH("profile")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<ApiResponse<AuthResult>>

    // -----------------------------------------------------------------------
    // MOBILE WEBVIEW BRIDGE
    // -----------------------------------------------------------------------

    /** Generate a one-time WebView bridge token for 3D viewer */
    @GET("mobile/webview/generate-token")
    suspend fun generateWebViewToken(
        @Query("purpose") purpose: String,
        @Query("patient_id") patientId: Int? = null
    ): Response<ApiResponse<WebViewTokenResult>>

    // -----------------------------------------------------------------------
    // PATIENTS
    // -----------------------------------------------------------------------

    /** Paginated/searchable list ? DataTables style: ?search=...&page=...&limit=... */
    @GET("patients")
    suspend fun getPatients(@QueryMap params: Map<String, String> = emptyMap()): Response<PagedResponse<Patient>>

    @GET("patients/{id}")
    suspend fun getPatient(@Path("id") id: Int): Response<ApiResponse<Patient>>

    @POST("patients")
    suspend fun createPatient(@Body body: Map<String, String?>): Response<ApiResponse<Patient>>

    @PATCH("patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: Int,
        @Body body: Map<String, String?>
    ): Response<ApiResponse<Patient>>

    @DELETE("patients/{id}")
    suspend fun deletePatient(@Path("id") id: Int): Response<ApiResponse<Unit>>

    /** Full list (no pagination) ? used for Dental 3D picker */
    @GET("patients-all")
    suspend fun getAllPatients(): Response<ApiResponse<List<Patient>>>

    /** Dropdown list { value, label } */
    @GET("patients-list")
    suspend fun getPatientsList(): Response<ApiResponse<List<PatientOption>>>

    // -----------------------------------------------------------------------
    // ANAMNESIS
    // -----------------------------------------------------------------------

    @GET("patients/{id}/anamnesis")
    suspend fun getAnamnesis(
        @Path("id") patientId: Int,
        @QueryMap params: Map<String, String> = emptyMap()
    ): Response<PagedResponse<PatientAnamnesis>>

    @POST("patients/{id}/anamnesis")
    suspend fun createAnamnesis(
        @Path("id") patientId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<PatientAnamnesis>>

    @PATCH("patients/{id}/anamnesis/{anamId}")
    suspend fun updateAnamnesis(
        @Path("id") patientId: Int,
        @Path("anamId") anamId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<PatientAnamnesis>>

    @DELETE("patients/{id}/anamnesis/{anamId}")
    suspend fun deleteAnamnesis(
        @Path("id") patientId: Int,
        @Path("anamId") anamId: Int
    ): Response<ApiResponse<Unit>>

    // -----------------------------------------------------------------------
    // DOCUMENTS
    // -----------------------------------------------------------------------

    @GET("patients/{id}/documents")
    suspend fun getDocuments(
        @Path("id") patientId: Int,
        @QueryMap params: Map<String, String> = emptyMap()
    ): Response<PagedResponse<PatientDocument>>

    @Multipart
    @POST("patients/{id}/documents")
    suspend fun uploadDocument(
        @Path("id") patientId: Int,
        @Part("doc_title") docTitle: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PatientDocument>>

    @DELETE("patients/{id}/documents/{docId}")
    suspend fun deleteDocument(
        @Path("id") patientId: Int,
        @Path("docId") docId: Int
    ): Response<ApiResponse<Unit>>

    // -----------------------------------------------------------------------
    // TREATMENT CASES
    // -----------------------------------------------------------------------

    @GET("treatment-cases")
    suspend fun getTreatmentCases(
        @QueryMap params: Map<String, String> = emptyMap()
    ): Response<PagedResponse<TreatmentCase>>

    @GET("treatment-cases/{id}")
    suspend fun getTreatmentCase(@Path("id") id: Int): Response<ApiResponse<TreatmentCase>>

    @POST("treatment-cases")
    suspend fun createTreatmentCase(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<TreatmentCase>>

    @PATCH("treatment-cases/{id}")
    suspend fun updateTreatmentCase(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<TreatmentCase>>

    @DELETE("treatment-cases/{id}")
    suspend fun deleteTreatmentCase(@Path("id") id: Int): Response<ApiResponse<Unit>>

    // -----------------------------------------------------------------------
    // CASE PLAN
    // -----------------------------------------------------------------------

    @GET("treatment-cases/{id}/plan")
    suspend fun getCasePlan(@Path("id") caseId: Int): Response<ApiResponse<CasePlan>>

    @POST("treatment-cases/{id}/plan")
    suspend fun createCasePlan(
        @Path("id") caseId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<CasePlan>>

    @PATCH("treatment-cases/{id}/plan")
    suspend fun updateCasePlan(
        @Path("id") caseId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<CasePlan>>

    // -----------------------------------------------------------------------
    // PLAN SEQUENCES
    // -----------------------------------------------------------------------

    @GET("treatment-cases/{id}/sequences")
    suspend fun getSequences(
        @Path("id") caseId: Int,
        @QueryMap params: Map<String, String> = emptyMap()
    ): Response<PagedResponse<PlanSequence>>

    @POST("treatment-cases/{id}/sequences")
    suspend fun addSequences(
        @Path("id") caseId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<List<PlanSequence>>>

    @PATCH("treatment-cases/{id}/sequences")
    suspend fun updateSequences(
        @Path("id") caseId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<List<PlanSequence>>>

    @DELETE("treatment-cases/{id}/sequence/{seqId}")
    suspend fun deleteSequence(
        @Path("id") caseId: Int,
        @Path("seqId") seqId: Int
    ): Response<ApiResponse<Unit>>

    // -----------------------------------------------------------------------
    // MEASUREMENTS (Dental Charting)
    // -----------------------------------------------------------------------

    @GET("patients/{patientId}/measurements/latest")
    suspend fun getLatestMeasurement(
        @Path("patientId") patientId: Int
    ): Response<ApiResponse<Measurement>>

    @GET("patients/{patientId}/measurements")
    suspend fun getMeasurements(
        @Path("patientId") patientId: Int
    ): Response<ApiResponse<List<Measurement>>>

    @POST("patients/{patientId}/measurements")
    suspend fun saveMeasurement(
        @Path("patientId") patientId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<Measurement>>

    @PATCH("patients/{patientId}/measurements/{measureId}/teeth/{toothNumber}")
    suspend fun upsertToothRecord(
        @Path("patientId") patientId: Int,
        @Path("measureId") measureId: Int,
        @Path("toothNumber") toothNumber: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<ToothRecord>>

    @DELETE("patients/{patientId}/measurements/{measureId}")
    suspend fun deleteMeasurement(
        @Path("patientId") patientId: Int,
        @Path("measureId") measureId: Int
    ): Response<ApiResponse<Unit>>

    // -----------------------------------------------------------------------
    // AI GENERATION
    // -----------------------------------------------------------------------

    @POST("generate/plan")
    suspend fun generatePlan(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<CasePlan>>

    @POST("generate/sequence")
    suspend fun generateSequence(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiResponse<List<AIGeneratedSequence>>>

    // -----------------------------------------------------------------------
    // SUBSCRIPTION & PAYMENTS
    // -----------------------------------------------------------------------

    @GET("subscription/status")
    suspend fun getSubscriptionStatus(): Response<ApiResponse<Map<String, Any?>>>

    @POST("subscription/create-checkout-session")
    suspend fun createCheckoutSession(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Map<String, String>>>

    @GET("subscription/payment-history")
    suspend fun getPaymentHistory(): Response<ApiResponse<Map<String, Any?>>>

    @POST("subscription/cancel")
    suspend fun cancelSubscription(): Response<ApiResponse<Map<String, String>>>

    // -----------------------------------------------------------------------
    // MISC
    // -----------------------------------------------------------------------

    @GET("categories")
    suspend fun getCategories(): Response<ApiResponse<CategoriesResult>>
}
