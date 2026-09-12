package com.example.smilesync.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.adapters.AnamnesisAdapter
import com.example.smilesync.adapters.DocumentAdapter
import com.example.smilesync.adapters.TreatmentCaseAdapter
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.model.Patient
import com.example.smilesync.model.PatientAnamnesis
import com.example.smilesync.model.PatientDocument
import com.example.smilesync.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class PatientDetailActivity : AppCompatActivity() {

    private var patientId: Int = -1
    private var patient: Patient? = null

    // Header
    private lateinit var tvPatientName    : TextView
    private lateinit var tvCasesCount     : TextView
    private lateinit var tvCompletedCount : TextView

    // Info tab
    private lateinit var tvBirthdate      : TextView
    private lateinit var tvAge            : TextView
    private lateinit var tvSex            : TextView
    private lateinit var tvAddress        : TextView
    private lateinit var tvLatestCase     : TextView

    // Tabs
    private lateinit var tabInfo      : TextView
    private lateinit var tabAnamnesis : TextView
    private lateinit var tabDocuments : TextView
    private lateinit var tabCases     : TextView

    // Panels
    private lateinit var panelInfo     : View
    private lateinit var panelAnamnesis: LinearLayout
    private lateinit var panelDocuments: LinearLayout
    private lateinit var panelCases    : View

    private lateinit var progressBar   : ProgressBar

    // Lists
    private lateinit var rvAnamnesis  : RecyclerView
    private lateinit var rvDocuments  : RecyclerView
    private lateinit var rvCases      : RecyclerView

    // FABs
    private lateinit var fabAddAnamnesis : View
    private lateinit var fabUploadDoc    : View

    // Anamnesis sheet state
    private var editingAnamnesisId: Int? = null
    private var anamDateStart: String? = null
    private var anamDateEnd:   String? = null

    // Document upload state
    private var pendingDocUri: Uri? = null

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingDocUri = it
            showDocumentTitleDialog(it)
        }
    }

    // Severity options (matches Laravel)
    private val severityOptions = listOf("— None —", "MILD", "MODERATE", "SEVERE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)

        patientId = intent.getIntExtra("patient_id", -1)
        if (patientId == -1) { finish(); return }

        bindViews()
        setupTabs()
        setupActionButtons()
        loadPatient()
    }

    private fun bindViews() {
        tvPatientName    = findViewById(R.id.tv_detail_name)
        tvBirthdate      = findViewById(R.id.tv_detail_birthdate)
        tvAge            = findViewById(R.id.tv_detail_age)
        tvSex            = findViewById(R.id.tv_detail_sex)
        tvAddress        = findViewById(R.id.tv_detail_address)
        tvCasesCount     = findViewById(R.id.tv_detail_cases_count)
        tvCompletedCount = findViewById(R.id.tv_detail_completed_count)
        tvLatestCase     = findViewById(R.id.tv_detail_latest_case)
        progressBar      = findViewById(R.id.patient_detail_progress)

        tabInfo          = findViewById(R.id.tab_info)
        tabAnamnesis     = findViewById(R.id.tab_anamnesis)
        tabDocuments     = findViewById(R.id.tab_documents)
        tabCases         = findViewById(R.id.tab_cases)

        panelInfo        = findViewById(R.id.panel_info)
        panelAnamnesis   = findViewById(R.id.panel_anamnesis)
        panelDocuments   = findViewById(R.id.panel_documents)
        panelCases       = findViewById(R.id.panel_cases)

        rvAnamnesis      = findViewById(R.id.rv_anamnesis)
        rvDocuments      = findViewById(R.id.rv_documents)
        rvCases          = findViewById(R.id.rv_cases)
        fabAddAnamnesis  = findViewById(R.id.fab_add_anamnesis)
        fabUploadDoc     = findViewById(R.id.fab_upload_doc)

        rvAnamnesis.layoutManager = LinearLayoutManager(this)
        rvDocuments.layoutManager = LinearLayoutManager(this)
        rvCases.layoutManager     = LinearLayoutManager(this)
    }

    private fun setupTabs() {
        showTab("info")
        tabInfo.setOnClickListener      { showTab("info") }
        tabAnamnesis.setOnClickListener { showTab("anamnesis"); loadAnamnesis() }
        tabDocuments.setOnClickListener { showTab("documents"); loadDocuments() }
        tabCases.setOnClickListener     { showTab("cases"); loadCases() }

        fabAddAnamnesis.setOnClickListener { showAnamnesisBottomSheet(null) }
        fabUploadDoc.setOnClickListener    { pickFileLauncher.launch("*/*") }
    }

    private fun showTab(tab: String) {
        panelInfo.visibility      = if (tab == "info")      View.VISIBLE else View.GONE
        panelAnamnesis.visibility = if (tab == "anamnesis") View.VISIBLE else View.GONE
        panelDocuments.visibility = if (tab == "documents") View.VISIBLE else View.GONE
        panelCases.visibility     = if (tab == "cases")     View.VISIBLE else View.GONE

        fabAddAnamnesis.visibility = if (tab == "anamnesis") View.VISIBLE else View.GONE
        fabUploadDoc.visibility    = if (tab == "documents") View.VISIBLE else View.GONE

        val active   = getColor(R.color.accent_blue)
        val inactive = getColor(R.color.text_light_gray)
        tabInfo.setTextColor(      if (tab == "info")      active else inactive)
        tabAnamnesis.setTextColor( if (tab == "anamnesis") active else inactive)
        tabDocuments.setTextColor( if (tab == "documents") active else inactive)
        tabCases.setTextColor(     if (tab == "cases")     active else inactive)
    }

    private fun setupActionButtons() {
        findViewById<View>(R.id.btn_add_case).setOnClickListener {
            startActivity(Intent(this, CaseFormActivity::class.java).apply {
                putExtra("patient_id", patientId)
            })
        }
        findViewById<View>(R.id.btn_dental_exam).setOnClickListener {
            startActivity(Intent(this, DentalExamActivity::class.java).apply {
                putExtra("patient_id", patientId)
                putExtra("patient_name", patient?.fullName ?: "")
            })
        }
        findViewById<View>(R.id.btn_edit_patient).setOnClickListener {
            startActivity(Intent(this, PatientFormActivity::class.java).apply {
                putExtra("patient_id", patientId)
            })
        }
        // Delete patient
        findViewById<View>(R.id.btn_delete_patient).setOnClickListener {
            UiUtils.confirm(this, "Delete Patient",
                "Are you sure you want to delete this patient? This action cannot be undone.") {
                deletePatient()
            }
        }
        findViewById<View>(R.id.btn_back_patient).setOnClickListener { finish() }
    }

    private fun loadPatient() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getPatient(patientId)
                if (response.isSuccessful) {
                    patient = response.body()?.result
                    patient?.let { bindPatientData(it) }
                } else {
                    UiUtils.toast(this@PatientDetailActivity, "Patient not found")
                    finish()
                }
            } catch (e: Exception) {
                UiUtils.toast(this@PatientDetailActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun bindPatientData(p: Patient) {
        tvPatientName.text    = p.fullName
        tvBirthdate.text      = p.birthdate?.take(10) ?: "N/A"
        tvAge.text            = p.ageYears()?.toString() ?: "N/A"
        tvSex.text            = p.sexLabel
        tvAddress.text        = p.address ?: "N/A"
        tvCasesCount.text     = (p.casesCount ?: 0).toString()
        tvCompletedCount.text = (p.completedCasesCount ?: 0).toString()
        tvLatestCase.text     = p.latestCase?.caseTitle ?: "No active cases"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ANAMNESIS
    // ═══════════════════════════════════════════════════════════════════════

    private fun loadAnamnesis() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getAnamnesis(patientId, mapOf("limit" to "50"))
                if (r.isSuccessful) {
                    val list = r.body()?.result?.data?.toMutableList() ?: mutableListOf()
                    if (list.isEmpty()) {
                        showEmptyAnamnesis()
                    } else {
                        rvAnamnesis.visibility = View.VISIBLE
                        findViewById<View>(R.id.tv_no_anamnesis)?.visibility = View.GONE
                        rvAnamnesis.adapter = AnamnesisAdapter(list,
                            onEdit   = { showAnamnesisBottomSheet(it) },
                            onDelete = { confirmDeleteAnamnesis(it) }
                        )
                    }
                }
            } catch (e: Exception) { /* silent */ }
        }
    }

    private fun showEmptyAnamnesis() {
        rvAnamnesis.visibility = View.GONE
        findViewById<View>(R.id.tv_no_anamnesis)?.visibility = View.VISIBLE
    }

    private fun showAnamnesisBottomSheet(existing: PatientAnamnesis?) {
        val sheet = BottomSheetDialog(this)
        val view  = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_anamnesis_form, null)
        sheet.setContentView(view)
        sheet.behavior.peekHeight = 600

        val tvTitle        = view.findViewById<TextView>(R.id.tv_anam_sheet_title)
        val etDesc         = view.findViewById<EditText>(R.id.et_anam_desc)
        val etTreatment    = view.findViewById<EditText>(R.id.et_anam_treatment)
        val spSeverity     = view.findViewById<Spinner>(R.id.sp_anam_severity)
        val tvDateStart    = view.findViewById<TextView>(R.id.tv_anam_date_start)
        val tvDateEnd      = view.findViewById<TextView>(R.id.tv_anam_date_end)
        val cbFamilyInher  = view.findViewById<CheckBox>(R.id.cb_family_inherited)
        val btnCancel      = view.findViewById<Button>(R.id.btn_anam_cancel)
        val btnSave        = view.findViewById<Button>(R.id.btn_anam_save)

        // Severity spinner
        val severityAdapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, severityOptions)
        spSeverity.adapter = severityAdapter

        // Reset sheet state
        anamDateStart = null
        anamDateEnd   = null

        tvTitle.text = if (existing != null) "Edit Anamnesis" else "Add Anamnesis"
        editingAnamnesisId = existing?.anamnesisId

        // Pre-fill if editing
        existing?.let { a ->
            etDesc.setText(a.anamDescription)
            etTreatment.setText(a.treatment ?: "")
            cbFamilyInher.isChecked = a.familyInherited
            anamDateStart = a.dateStart?.take(10)
            anamDateEnd   = a.dateEnd?.take(10)
            tvDateStart.text = anamDateStart ?: "Tap to set"
            tvDateEnd.text   = anamDateEnd   ?: "Tap to set (optional)"
            val sevIdx = severityOptions.indexOfFirst {
                it.equals(a.severityName, ignoreCase = true) }.coerceAtLeast(0)
            spSeverity.setSelection(sevIdx)
        }

        tvDateStart.setOnClickListener {
            showDatePicker { y, m, d ->
                anamDateStart = "%04d-%02d-%02d".format(y, m + 1, d)
                tvDateStart.text = anamDateStart
            }
        }
        tvDateEnd.setOnClickListener {
            showDatePicker { y, m, d ->
                anamDateEnd = "%04d-%02d-%02d".format(y, m + 1, d)
                tvDateEnd.text = anamDateEnd
            }
        }

        btnCancel.setOnClickListener { sheet.dismiss() }
        btnSave.setOnClickListener {
            val desc = etDesc.text.toString().trim()
            if (desc.isEmpty()) { etDesc.error = "Required"; return@setOnClickListener }

            val selectedSeverity = severityOptions[spSeverity.selectedItemPosition]
            val severityName = if (selectedSeverity == "— None —") null else selectedSeverity

            val body = mapOf<String, Any?>(
                "anam_description" to desc,
                "treatment"        to etTreatment.text.toString().trim().ifBlank { null },
                "severity_name"    to severityName,
                "family_inherited" to cbFamilyInher.isChecked,
                "date_start"       to anamDateStart,
                "date_end"         to anamDateEnd
            )

            btnSave.isEnabled = false
            lifecycleScope.launch {
                try {
                    val r = if (editingAnamnesisId != null)
                        RetrofitClient.api.updateAnamnesis(patientId, editingAnamnesisId!!, body)
                    else
                        RetrofitClient.api.createAnamnesis(patientId, body)

                    if (r.isSuccessful) {
                        UiUtils.toast(this@PatientDetailActivity,
                            if (editingAnamnesisId != null) "Anamnesis updated!" else "Anamnesis added!")
                        sheet.dismiss()
                        loadAnamnesis()
                    } else {
                        UiUtils.toast(this@PatientDetailActivity, UiUtils.errorMessage(r))
                        btnSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@PatientDetailActivity, "Error: ${e.message}")
                    btnSave.isEnabled = true
                }
            }
        }
        sheet.show()
    }

    private fun confirmDeleteAnamnesis(a: PatientAnamnesis) {
        UiUtils.confirm(this, "Delete Anamnesis",
            "Delete \"${a.anamDescription.take(40)}\"?") {
            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.api.deleteAnamnesis(patientId, a.anamnesisId)
                    if (r.isSuccessful) {
                        UiUtils.toast(this@PatientDetailActivity, "Deleted")
                        loadAnamnesis()
                    } else {
                        UiUtils.toast(this@PatientDetailActivity, UiUtils.errorMessage(r))
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@PatientDetailActivity, "Error: ${e.message}")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOCUMENTS
    // ═══════════════════════════════════════════════════════════════════════

    private fun loadDocuments() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getDocuments(patientId, mapOf("limit" to "50"))
                if (r.isSuccessful) {
                    val list = r.body()?.result?.data?.toMutableList() ?: mutableListOf()
                    if (list.isEmpty()) {
                        rvDocuments.visibility = View.GONE
                        findViewById<View>(R.id.tv_no_documents)?.visibility = View.VISIBLE
                    } else {
                        rvDocuments.visibility = View.VISIBLE
                        findViewById<View>(R.id.tv_no_documents)?.visibility = View.GONE
                        rvDocuments.adapter = DocumentAdapter(list) { doc ->
                            confirmDeleteDocument(doc)
                        }
                    }
                }
            } catch (e: Exception) { /* silent */ }
        }
    }

    private fun showDocumentTitleDialog(uri: Uri) {
        val defaultName = getFileName(uri) ?: "Document"
        val et = EditText(this).apply {
            setText(defaultName)
            setPadding(48, 32, 48, 8)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Document Title")
            .setMessage("Enter a title for this document:")
            .setView(et)
            .setPositiveButton("Upload") { _, _ ->
                val title = et.text.toString().trim().ifBlank { defaultName }
                uploadDocument(uri, title)
            }
            .setNegativeButton("Cancel") { _, _ -> pendingDocUri = null }
            .show()
    }

    private fun uploadDocument(uri: Uri, title: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val file = uriToTempFile(uri) ?: run {
                    UiUtils.toast(this@PatientDetailActivity, "Could not read file")
                    return@launch
                }
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val reqFile  = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part     = MultipartBody.Part.createFormData("file", file.name, reqFile)
                val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())

                val r = RetrofitClient.api.uploadDocument(patientId, titlePart, part)
                file.delete()

                if (r.isSuccessful) {
                    UiUtils.toast(this@PatientDetailActivity, "Document uploaded!")
                    loadDocuments()
                    showTab("documents")
                } else {
                    UiUtils.toast(this@PatientDetailActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@PatientDetailActivity, "Upload error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                pendingDocUri = null
            }
        }
    }

    private fun confirmDeleteDocument(doc: PatientDocument) {
        UiUtils.confirm(this, "Delete Document",
            "Delete \"${doc.docTitle.take(40)}\"?") {
            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.api.deleteDocument(patientId, doc.documentId)
                    if (r.isSuccessful) {
                        UiUtils.toast(this@PatientDetailActivity, "Deleted")
                        loadDocuments()
                    } else {
                        UiUtils.toast(this@PatientDetailActivity, UiUtils.errorMessage(r))
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@PatientDetailActivity, "Error: ${e.message}")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CASES
    // ═══════════════════════════════════════════════════════════════════════

    private fun loadCases() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getTreatmentCases(
                    mapOf("patient_id" to patientId.toString(), "limit" to "50")
                )
                if (r.isSuccessful) {
                    val list = r.body()?.result?.data ?: emptyList()
                    rvCases.adapter = TreatmentCaseAdapter(list) { case ->
                        startActivity(Intent(this@PatientDetailActivity,
                            CaseSingleActivity::class.java).apply {
                            putExtra("case_id", case.caseId)
                        })
                    }
                }
            } catch (e: Exception) { /* silent */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE PATIENT
    // ═══════════════════════════════════════════════════════════════════════

    private fun deletePatient() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.deletePatient(patientId)
                if (r.isSuccessful) {
                    UiUtils.toast(this@PatientDetailActivity, "Patient deleted")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    UiUtils.toast(this@PatientDetailActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@PatientDetailActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun showDatePicker(onPicked: (Int, Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> onPicked(y, m, d) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun uriToTempFile(uri: Uri): File? {
        return try {
            val name = getFileName(uri) ?: "upload_${System.currentTimeMillis()}"
            val tmp  = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            tmp
        } catch (e: Exception) { null }
    }
}
