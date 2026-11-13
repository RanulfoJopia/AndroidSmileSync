package com.example.smilesync.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.model.Category
import com.example.smilesync.model.PatientOption
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class CaseFormActivity : AppCompatActivity() {

    private var caseId    : Int = -1          // -1 = add mode
    private var patientId : Int = -1          // pre-filled from PatientDetailActivity
    private val isEdit get() = caseId != -1

    private lateinit var tvTitle          : TextView
    private lateinit var spPatient        : Spinner
    private lateinit var spCaseType       : Spinner
    private lateinit var etCaseTitle      : EditText
    private lateinit var etDescription    : EditText
    private lateinit var etChiefComplaint : EditText
    private lateinit var etDiagnosis      : EditText
    private lateinit var etFindings       : EditText
    private lateinit var etVitalSigns     : EditText
    private lateinit var etNotes          : EditText
    private lateinit var btnSave          : Button
    private lateinit var progressBar      : ProgressBar

    private var patientOptions : List<PatientOption> = emptyList()
    private var caseTypeOptions: List<Category>      = emptyList()

    private var selectedPatientId : Int = -1
    private var selectedCaseType  : Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_case_form)

        caseId    = intent.getIntExtra("case_id",    -1)
        patientId = intent.getIntExtra("patient_id", -1)

        bindViews()
        tvTitle.text = if (isEdit) "Edit Case" else "Add Treatment Case"

        // Back button
        findViewById<View>(R.id.btn_back_case_form).setOnClickListener { finish() }
        
        btnSave.setOnClickListener { saveCase() }

        loadDropdowns()
    }

    private fun bindViews() {
        tvTitle          = findViewById(R.id.tv_case_form_title)
        spPatient        = findViewById(R.id.sp_patient)
        spCaseType       = findViewById(R.id.sp_case_type)
        etCaseTitle      = findViewById(R.id.et_case_title)
        etDescription    = findViewById(R.id.et_case_description)
        etChiefComplaint = findViewById(R.id.et_chief_complaint)
        etDiagnosis      = findViewById(R.id.et_dentist_diagnosis)
        etFindings       = findViewById(R.id.et_clinical_findings)
        etVitalSigns     = findViewById(R.id.et_vital_signs)
        etNotes          = findViewById(R.id.et_case_notes)
        btnSave          = findViewById(R.id.btn_save_case)
        progressBar      = findViewById(R.id.case_form_progress)
    }

    private fun loadDropdowns() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Load patients and categories in parallel
                val patientsDeferred    = async { RetrofitClient.api.getPatientsList() }
                val categoriesDeferred  = async { RetrofitClient.api.getCategories() }

                val patientsResp    = patientsDeferred.await()
                val categoriesResp  = categoriesDeferred.await()

                // ── Patients spinner ──
                if (patientsResp.isSuccessful) {
                    patientOptions = patientsResp.body()?.result ?: emptyList()
                    val labels = patientOptions.map { it.label }
                    spPatient.adapter = ArrayAdapter(this@CaseFormActivity,
                        android.R.layout.simple_spinner_dropdown_item, labels)

                    if (patientId != -1) {
                        val idx = patientOptions.indexOfFirst { it.value == patientId }
                        if (idx >= 0) spPatient.setSelection(idx)
                        selectedPatientId = patientId
                    }

                    spPatient.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                            selectedPatientId = patientOptions.getOrNull(pos)?.value ?: -1
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }

                // ── Case type spinner ──
                if (categoriesResp.isSuccessful) {
                    // API returns CASES as Map<String, Int> where key=name, value=id
                    // Convert to List<Category> for the spinner
                    val casesMap = categoriesResp.body()?.result?.cases
                    if (casesMap != null) {
                        caseTypeOptions = casesMap.map { (name, id) ->
                            Category(value = id, label = name)
                        }
                    }
                }
                
                // Fallback if categories endpoint returns nothing
                if (caseTypeOptions.isEmpty()) {
                    caseTypeOptions = listOf(
                        Category(1, "Orthodontic"),
                        Category(2, "Restorative"),
                        Category(3, "Surgical"),
                        Category(4, "Preventive"),
                        Category(5, "Prosthodontic")
                    )
                }
                
                val typeLabels = caseTypeOptions.map { it.label }
                spCaseType.adapter = ArrayAdapter(this@CaseFormActivity,
                    android.R.layout.simple_spinner_dropdown_item, typeLabels)
                selectedCaseType = caseTypeOptions.firstOrNull()?.value ?: 1

                spCaseType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedCaseType = caseTypeOptions.getOrNull(pos)?.value ?: 1
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Load existing case data only after dropdowns are ready
                if (isEdit) loadCase()

            } catch (e: Exception) {
                UiUtils.toast(this@CaseFormActivity, "Failed to load options: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadCase() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getTreatmentCase(caseId)
                if (r.isSuccessful) {
                    val c = r.body()?.result ?: return@launch
                    etCaseTitle.setText(c.caseTitle)
                    etDescription.setText(c.caseDescription ?: "")
                    etChiefComplaint.setText(c.chiefComplaint ?: "")
                    etDiagnosis.setText(c.dentistDiagnosis ?: "")
                    etFindings.setText(c.clinicalFindings ?: "")
                    etVitalSigns.setText(c.vitalSigns ?: "")
                    etNotes.setText(c.caseNotes ?: "")
                    selectedPatientId = c.patientId
                    val patIdx = patientOptions.indexOfFirst { it.value == c.patientId }
                    if (patIdx >= 0) spPatient.setSelection(patIdx)

                    c.caseType?.let { ct ->
                        selectedCaseType = ct
                        val typeIdx = caseTypeOptions.indexOfFirst { it.value == ct }
                        if (typeIdx >= 0) spCaseType.setSelection(typeIdx)
                    }
                }
            } catch (e: Exception) {
                UiUtils.toast(this@CaseFormActivity, "Failed to load case")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveCase() {
        val title = etCaseTitle.text.toString().trim()
        val patId = selectedPatientId

        if (title.isEmpty())  { etCaseTitle.error = "Required"; return }
        if (patId == -1)      { UiUtils.toast(this, "Please select a patient"); return }

        val body = mapOf<String, Any?>(
            "patient_id"        to patId,
            "case_title"        to title,
            "case_type"         to selectedCaseType,
            "case_description"  to etDescription.text.toString().trim().ifBlank { null },
            "chief_complaint"   to etChiefComplaint.text.toString().trim().ifBlank { null },
            "dentist_diagnosis" to etDiagnosis.text.toString().trim().ifBlank { null },
            "clinical_findings" to etFindings.text.toString().trim().ifBlank { null },
            "vital_signs"       to etVitalSigns.text.toString().trim().ifBlank { null },
            "case_notes"        to etNotes.text.toString().trim().ifBlank { null }
        )

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val r = if (isEdit)
                    RetrofitClient.api.updateTreatmentCase(caseId, body)
                else
                    RetrofitClient.api.createTreatmentCase(body)

                if (r.isSuccessful) {
                    UiUtils.toast(this@CaseFormActivity,
                        if (isEdit) "Case updated!" else "Case created!")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    UiUtils.toast(this@CaseFormActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@CaseFormActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }
}
