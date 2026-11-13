package com.example.smilesync.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.adapters.SequenceAdapter
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.model.CasePlan
import com.example.smilesync.model.PlanSequence
import com.example.smilesync.model.TreatmentCase
import com.example.smilesync.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class CaseSingleActivity : AppCompatActivity() {

    private var caseId: Int = -1
    private var currentCase: TreatmentCase? = null
    private var currentPlan: CasePlan? = null

    // Header
    private lateinit var progressBar      : ProgressBar
    private lateinit var tvCaseTitle      : TextView
    private lateinit var tvPatientName    : TextView
    private lateinit var tvCaseType       : TextView

    // Case detail fields
    private lateinit var tvChiefComplaint : TextView
    private lateinit var tvDiagnosis      : TextView
    private lateinit var tvFindings       : TextView
    private lateinit var tvVitalSigns     : TextView
    private lateinit var tvDescription    : TextView
    private lateinit var tvNotes          : TextView

    // Plan section
    private lateinit var layoutPlan        : View
    private lateinit var tvNoPlaceholder   : TextView
    private lateinit var tvPlanResult      : TextView
    private lateinit var tvJustification   : TextView
    private lateinit var tvRisk            : TextView
    private lateinit var tvPrognosis       : TextView
    private lateinit var tvConsentDate     : TextView
    private lateinit var btnEditPlan       : View
    private lateinit var btnGeneratePlan   : View

    // Sequence section
    private lateinit var rvSequences       : RecyclerView
    private lateinit var tvNoSequences     : TextView
    private lateinit var btnAddSeq         : View
    private lateinit var btnGenerateSeq    : View
        // Mutable sequence list for in-place updates
    private val sequenceList = mutableListOf<PlanSequence>()

    // Phase/status labels for bottom sheet spinners
    private val phaseOptions   = listOf("— None —", "Diagnostic", "Preventive", "Restorative",
        "Surgical", "Prosthodontic", "Orthodontic", "Periodontic", "Endodontic", "Maintenance")
    private val statusOptions  = listOf("Planned", "In Progress", "Done")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_case_single)

        caseId = intent.getIntExtra("case_id", -1)
        if (caseId == -1) { finish(); return }

        bindViews()

        findViewById<View>(R.id.btn_back_case).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_edit_case).setOnClickListener {
            startActivity(Intent(this, CaseFormActivity::class.java).apply {
                putExtra("case_id", caseId)
            })
        }
        // Delete case
        findViewById<View>(R.id.btn_delete_case).setOnClickListener {
            UiUtils.confirm(this, "Delete Case",
                "Are you sure? This will also delete the plan and all sequences.") {
                deleteTreatmentCase()
            }
        }

        btnEditPlan.setOnClickListener     { showPlanBottomSheet() }
        btnGeneratePlan.setOnClickListener { generatePlan() }
        btnAddSeq.setOnClickListener       { showSequenceBottomSheet(null) }
        btnGenerateSeq.setOnClickListener  { generateSequences() }

        loadAll()
    }

    override fun onResume() {
        super.onResume()
        loadAll()
    }

    private fun bindViews() {
        progressBar       = findViewById(R.id.case_single_progress)
        tvCaseTitle       = findViewById(R.id.tv_case_single_title)
        tvPatientName     = findViewById(R.id.tv_case_single_patient)
        tvCaseType        = findViewById(R.id.tv_case_single_type)
        tvChiefComplaint  = findViewById(R.id.tv_case_chief_complaint)
        tvDiagnosis       = findViewById(R.id.tv_case_diagnosis)
        tvFindings        = findViewById(R.id.tv_case_findings)
        tvVitalSigns      = findViewById(R.id.tv_case_vital_signs)
        tvDescription     = findViewById(R.id.tv_case_description)
        tvNotes           = findViewById(R.id.tv_case_notes)
        layoutPlan        = findViewById(R.id.layout_plan_details)
        tvNoPlaceholder   = findViewById(R.id.tv_no_plan)
        tvJustification   = findViewById(R.id.tv_plan_justification)
        tvRisk            = findViewById(R.id.tv_plan_risk)
        tvPrognosis       = findViewById(R.id.tv_plan_prognosis)
        tvConsentDate     = findViewById(R.id.tv_plan_consent_date)
        tvPlanResult      = findViewById(R.id.tv_plan_result)
        btnEditPlan       = findViewById(R.id.btn_edit_plan)
        btnGeneratePlan   = findViewById(R.id.btn_generate_plan)
        rvSequences       = findViewById(R.id.rv_sequences)
        tvNoSequences     = findViewById(R.id.tv_no_sequences)
        btnAddSeq         = findViewById(R.id.btn_add_sequence)
        btnGenerateSeq    = findViewById(R.id.btn_generate_sequence)

        rvSequences.layoutManager = LinearLayoutManager(this)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOAD ALL DATA
    // ═══════════════════════════════════════════════════════════════════════

    private fun loadAll() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val caseDeferred = async { RetrofitClient.api.getTreatmentCase(caseId) }
                val caseResp = caseDeferred.await()

                if (caseResp.isSuccessful) {
                    currentCase = caseResp.body()?.result
                    currentCase?.let { bindCase(it) }
                } else {
                    UiUtils.toast(this@CaseSingleActivity, "Case not found")
                    finish(); return@launch
                }

                // Plan
                val planResp = RetrofitClient.api.getCasePlan(caseId)
                if (planResp.isSuccessful) {
                    currentPlan = planResp.body()?.result
                    currentPlan?.let { bindPlan(it) } ?: run {
                        layoutPlan.visibility    = View.GONE
                        tvNoPlaceholder.visibility = View.VISIBLE
                    }
                    loadSequences()
                } else {
                    layoutPlan.visibility    = View.GONE
                    tvNoPlaceholder.visibility = View.VISIBLE
                    tvNoSequences.visibility = View.VISIBLE
                    rvSequences.visibility   = View.GONE
                }
            } catch (e: Exception) {
                UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun bindCase(c: TreatmentCase) {
        tvCaseTitle.text      = c.caseTitle
        tvPatientName.text    = c.patientFullName
        tvCaseType.text       = "Type: ${c.caseType ?: "—"}"
        tvChiefComplaint.text = c.chiefComplaint ?: "N/A"
        tvDiagnosis.text      = c.dentistDiagnosis ?: "N/A"
        tvFindings.text       = c.clinicalFindings ?: "N/A"
        tvVitalSigns.text     = c.vitalSigns ?: "N/A"
        tvDescription.text    = c.caseDescription ?: ""
        tvNotes.text          = c.caseNotes ?: ""

        val descParent  = findViewById<View>(R.id.card_description)
        val notesParent = findViewById<View>(R.id.card_notes)
        descParent.visibility  = if (c.caseDescription.isNullOrBlank()) View.GONE else View.VISIBLE
        notesParent.visibility = if (c.caseNotes.isNullOrBlank())       View.GONE else View.VISIBLE
    }

    private fun bindPlan(p: CasePlan) {
        layoutPlan.visibility        = View.VISIBLE
        tvNoPlaceholder.visibility   = View.GONE
        tvJustification.text         = p.clinicalJustification.ifBlank { "N/A" }
        tvRisk.text                  = p.riskComplications.ifBlank { "N/A" }
        tvPrognosis.text             = p.prognosis.ifBlank { "N/A" }
        tvConsentDate.text           = p.consentGivenAt?.take(10) ?: "Not yet given"
        tvPlanResult.text            = p.planResult?.ifBlank { "N/A" } ?: "N/A"
        // Update button label based on whether plan exists
        val editPlanTv = btnEditPlan as? TextView
        editPlanTv?.text = "✏ Edit Plan"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SEQUENCES
    // ═══════════════════════════════════════════════════════════════════════

    private fun loadSequences() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getSequences(caseId, mapOf("limit" to "100"))
                if (r.isSuccessful) {
                    sequenceList.clear()
                    sequenceList.addAll(r.body()?.result?.data ?: emptyList())
                    renderSequences()
                }
            } catch (e: Exception) { /* silent */ }
        }
    }

    private fun renderSequences() {
        if (sequenceList.isEmpty()) {
            tvNoSequences.visibility = View.VISIBLE
            rvSequences.visibility   = View.GONE
        } else {
            tvNoSequences.visibility = View.GONE
            rvSequences.visibility   = View.VISIBLE
            rvSequences.adapter = SequenceAdapter(
                sequenceList,
                onEdit   = { seq -> showSequenceBottomSheet(seq) },
                onDelete = { seq -> confirmDeleteSequence(seq) }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PLAN BOTTOM SHEET
    // ═══════════════════════════════════════════════════════════════════════

    private fun showPlanBottomSheet() {
        val sheet = BottomSheetDialog(this)
        val view  = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_plan_form, null)
        sheet.setContentView(view)
        sheet.behavior.peekHeight = 700

        val tvTitle       = view.findViewById<TextView>(R.id.tv_plan_sheet_title)
        val etJustif      = view.findViewById<EditText>(R.id.et_plan_justification)
        val etRisk        = view.findViewById<EditText>(R.id.et_plan_risk)
        val etPrognosis   = view.findViewById<EditText>(R.id.et_plan_prognosis)
        val etResult      = view.findViewById<EditText>(R.id.et_plan_result)
        val btnCancel     = view.findViewById<Button>(R.id.btn_plan_cancel)
        val btnSave       = view.findViewById<Button>(R.id.btn_plan_save)

        val isNew = (currentPlan == null)
        tvTitle.text = if (isNew) "Create Treatment Plan" else "Edit Treatment Plan"
        btnSave.text = if (isNew) "Create Plan" else "Save Plan"

        currentPlan?.let { p ->
            etJustif.setText(p.clinicalJustification)
            etRisk.setText(p.riskComplications)
            etPrognosis.setText(p.prognosis)
            etResult.setText(p.planResult ?: "")
        }

        btnCancel.setOnClickListener { sheet.dismiss() }
        btnSave.setOnClickListener {
            val justif    = etJustif.text.toString().trim()
            val risk      = etRisk.text.toString().trim()
            val prognosis = etPrognosis.text.toString().trim()
            val result    = etResult.text.toString().trim()

            if (justif.isEmpty())    { etJustif.error = "Required";    return@setOnClickListener }
            if (risk.isEmpty())      { etRisk.error   = "Required";    return@setOnClickListener }
            if (prognosis.isEmpty()) { etPrognosis.error = "Required"; return@setOnClickListener }

            val body = mapOf<String, Any?>(
                "clinical_justification" to justif,
                "risk_complications"     to risk,
                "prognosis"              to prognosis,
                "plan_result"            to result.ifBlank { null },
                "plan_status"            to "draft"
            )

            btnSave.isEnabled = false
            lifecycleScope.launch {
                try {
                    val r = if (isNew)
                        RetrofitClient.api.createCasePlan(caseId, body)
                    else
                        RetrofitClient.api.updateCasePlan(caseId, body)

                    if (r.isSuccessful) {
                        UiUtils.toast(this@CaseSingleActivity,
                            if (isNew) "Plan created!" else "Plan updated!")
                        currentPlan = r.body()?.result
                        currentPlan?.let { bindPlan(it) }
                        // Enable sequence button now that plan exists
                        btnAddSeq.isEnabled        = true
                        btnGenerateSeq.isEnabled   = true
                        sheet.dismiss()
                    } else {
                        UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                        btnSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
                    btnSave.isEnabled = true
                }
            }
        }
        sheet.show()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SEQUENCE BOTTOM SHEET
    // ═══════════════════════════════════════════════════════════════════════

    private fun showSequenceBottomSheet(existing: PlanSequence?) {
        if (currentPlan == null) {
            UiUtils.toast(this, "Create a treatment plan first")
            return
        }
        val sheet = BottomSheetDialog(this)
        val view  = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_sequence_form, null)
        sheet.setContentView(view)
        sheet.behavior.peekHeight = 650

        val tvTitle     = view.findViewById<TextView>(R.id.tv_seq_sheet_title)
        val etDesc      = view.findViewById<EditText>(R.id.et_seq_description)
        val etCode      = view.findViewById<EditText>(R.id.et_seq_proc_code)
        val spPhase     = view.findViewById<Spinner>(R.id.sp_seq_phase)
        val spStatus    = view.findViewById<Spinner>(R.id.sp_seq_status)
        val etNotes     = view.findViewById<EditText>(R.id.et_seq_notes)
        val btnCancel   = view.findViewById<Button>(R.id.btn_seq_cancel)
        val btnSave     = view.findViewById<Button>(R.id.btn_seq_save)

        val isNew = (existing == null)
        tvTitle.text = if (isNew) "Add Sequence Step" else "Edit Sequence Step"
        btnSave.text = if (isNew) "Add Step" else "Save Changes"

        spPhase.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, phaseOptions)
        spStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusOptions)

        existing?.let { s ->
            etDesc.setText(s.description)
            etCode.setText(s.procedureCode ?: "")
            etNotes.setText(s.seqNotes ?: "")
            // Phase: map stored int to spinner index (stored as 1-based, spinner 1-based after "None")
            val phaseIdx = if (s.phaseType > 0 && s.phaseType < phaseOptions.size) s.phaseType else 0
            spPhase.setSelection(phaseIdx)
            val statusIdx = when (s.seqStatus) { 2 -> 1; 3 -> 2; else -> 0 }
            spStatus.setSelection(statusIdx)
        }

        btnCancel.setOnClickListener { sheet.dismiss() }
        btnSave.setOnClickListener {
            val desc = etDesc.text.toString().trim()
            if (desc.isEmpty()) { etDesc.error = "Required"; return@setOnClickListener }

            val phaseVal  = spPhase.selectedItemPosition   // 0 = none, 1..N = phases
            val statusVal = when (spStatus.selectedItemPosition) { 1 -> 2; 2 -> 3; else -> 1 }

            if (isNew) {
                val newOrder = (sequenceList.maxOfOrNull { it.orderNumber } ?: 0) + 1
                val body = mapOf<String, Any?>(
                    "sequences" to listOf(mapOf(
                        "description"    to desc,
                        "procedure_code" to etCode.text.toString().trim().ifBlank { null },
                        "phase_type"     to phaseVal,
                        "seq_status"     to statusVal,
                        "seq_notes"      to etNotes.text.toString().trim().ifBlank { null },
                        "order_number"   to newOrder
                    ))
                )
                btnSave.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val r = RetrofitClient.api.addSequences(caseId, body)
                        if (r.isSuccessful) {
                            UiUtils.toast(this@CaseSingleActivity, "Step added!")
                            sheet.dismiss()
                            loadSequences()
                        } else {
                            UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                            btnSave.isEnabled = true
                        }
                    } catch (e: Exception) {
                        UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
                        btnSave.isEnabled = true
                    }
                }
            } else {
                // Edit: patch sequences bulk endpoint with updated item
                val updated = sequenceList.map { s ->
                    if (s.seqId == existing!!.seqId) {
                        mapOf<String, Any?>(
                            "seq_id"         to s.seqId,
                            "description"    to desc,
                            "procedure_code" to etCode.text.toString().trim().ifBlank { null },
                            "phase_type"     to phaseVal,
                            "seq_status"     to statusVal,
                            "seq_notes"      to etNotes.text.toString().trim().ifBlank { null },
                            "order_number"   to s.orderNumber
                        )
                    } else {
                        mapOf<String, Any?>(
                            "seq_id"         to s.seqId,
                            "description"    to s.description,
                            "procedure_code" to s.procedureCode,
                            "phase_type"     to s.phaseType,
                            "seq_status"     to s.seqStatus,
                            "seq_notes"      to s.seqNotes,
                            "order_number"   to s.orderNumber
                        )
                    }
                }
                val body = mapOf<String, Any?>("sequences" to updated)
                btnSave.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val r = RetrofitClient.api.updateSequences(caseId, body)
                        if (r.isSuccessful) {
                            UiUtils.toast(this@CaseSingleActivity, "Step updated!")
                            sheet.dismiss()
                            loadSequences()
                        } else {
                            UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                            btnSave.isEnabled = true
                        }
                    } catch (e: Exception) {
                        UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
                        btnSave.isEnabled = true
                    }
                }
            }
        }
        sheet.show()
    }

    private fun confirmDeleteSequence(seq: PlanSequence) {
        UiUtils.confirm(this, "Delete Step",
            "Delete step: \"${seq.description.take(40)}\"?") {
            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.api.deleteSequence(caseId, seq.seqId)
                    if (r.isSuccessful) {
                        UiUtils.toast(this@CaseSingleActivity, "Step deleted")
                        loadSequences()
                    } else {
                        UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AI GENERATION
    // ═══════════════════════════════════════════════════════════════════════

    private fun generatePlan() {
        val c = currentCase ?: run {
            UiUtils.toast(this, "Case data not loaded yet"); return
        }
        UiUtils.confirm(this, "AI Generate Plan",
            "Auto-generate a treatment plan from this case's clinical data?") {

            progressBar.visibility  = View.VISIBLE
            btnGeneratePlan.isEnabled = false

            val body = mapOf<String, Any?>(
                "case_id"           to c.caseId,
                "patient_id"        to c.patientId,
                "case_description"  to c.caseDescription,
                "chief_complaint"   to c.chiefComplaint,
                "dentist_diagnosis" to c.dentistDiagnosis,
                "clinical_findings" to c.clinicalFindings,
                "vital_signs"       to c.vitalSigns,
                "case_notes"        to c.caseNotes
            )

            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.api.generatePlan(body)
                    if (r.isSuccessful) {
                        val generated = r.body()?.result
                        if (generated != null) {
                            showGeneratedPlanPreview(generated)
                        } else {
                            UiUtils.toast(this@CaseSingleActivity, "No plan data generated")
                        }
                    } else {
                        UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
                } finally {
                    progressBar.visibility    = View.GONE
                    btnGeneratePlan.isEnabled = true
                }
            }
        }
    }

    private fun showGeneratedPlanPreview(plan: CasePlan) {
        val msg = buildString {
            appendLine("Clinical Justification:")
            appendLine(plan.clinicalJustification.take(120) + if (plan.clinicalJustification.length > 120) "…" else "")
            appendLine()
            appendLine("Risks & Complications:")
            appendLine(plan.riskComplications.take(80) + if (plan.riskComplications.length > 80) "…" else "")
            appendLine()
            appendLine("Prognosis:")
            appendLine(plan.prognosis.take(80) + if (plan.prognosis.length > 80) "…" else "")
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("✨ AI Generated Plan")
            .setMessage(msg)
            .setPositiveButton("Accept & Save") { _, _ -> savePlanFromGeneration(plan) }
            .setNegativeButton("Discard", null)
            .show()
    }

    private fun savePlanFromGeneration(plan: CasePlan) {
        val isNew = (currentPlan == null)
        val body = mapOf<String, Any?>(
            "clinical_justification" to plan.clinicalJustification,
            "risk_complications"     to plan.riskComplications,
            "prognosis"              to plan.prognosis,
            "plan_result"            to plan.planResult,
            "plan_status"            to "draft"
        )
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = if (isNew)
                    RetrofitClient.api.createCasePlan(caseId, body)
                else
                    RetrofitClient.api.updateCasePlan(caseId, body)

                if (r.isSuccessful) {
                    currentPlan = r.body()?.result
                    currentPlan?.let { bindPlan(it) }
                    UiUtils.toast(this@CaseSingleActivity, "Plan saved!")
                } else {
                    UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun generateSequences() {
        val plan = currentPlan ?: run {
            UiUtils.toast(this, "Create a treatment plan first"); return
        }
        UiUtils.confirm(this, "AI Generate Sequences",
            "Auto-generate treatment sequences from the plan?") {

            progressBar.visibility     = View.VISIBLE
            btnGenerateSeq.isEnabled   = false

            val body = mapOf<String, Any?>(
                "case_id"                to caseId,
                "clinical_justification" to plan.clinicalJustification,
                "risk_complications"     to plan.riskComplications,
                "prognosis"              to plan.prognosis
            )

            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.api.generateSequence(body)
                    if (r.isSuccessful) {
                        val generatedList = r.body()?.result
                        if (!generatedList.isNullOrEmpty()) {
                            showGeneratedSequencesPreview(generatedList)
                        } else {
                            UiUtils.toast(this@CaseSingleActivity, "No sequences generated")
                        }
                    } else {
                        UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                    }
                } catch (e: Exception) {
                    UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
                } finally {
                    progressBar.visibility   = View.GONE
                    btnGenerateSeq.isEnabled = true
                }
            }
        }
    }

    private fun showGeneratedSequencesPreview(sequences: List<PlanSequence>) {
        val preview = sequences.take(5).mapIndexed { i, s ->
            "${i + 1}. ${s.description.take(60)}"
        }.joinToString("\n")
        val suffix = if (sequences.size > 5) "\n… and ${sequences.size - 5} more" else ""

        android.app.AlertDialog.Builder(this)
            .setTitle("✨ AI Generated Sequences (${sequences.size} steps)")
            .setMessage(preview + suffix)
            .setPositiveButton("Accept & Save") { _, _ -> saveGeneratedSequences(sequences) }
            .setNegativeButton("Discard", null)
            .show()
    }

    private fun saveGeneratedSequences(sequences: List<PlanSequence>) {
        val payload = sequences.mapIndexed { i, s ->
            mapOf<String, Any?>(
                "description"    to s.description,
                "procedure_code" to s.procedureCode,
                "phase_type"     to s.phaseType,
                "seq_status"     to s.seqStatus,
                "seq_notes"      to s.seqNotes,
                "order_number"   to (i + 1)
            )
        }
        val body = mapOf<String, Any?>("sequences" to payload)
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.addSequences(caseId, body)
                if (r.isSuccessful) {
                    UiUtils.toast(this@CaseSingleActivity, "Sequences saved!")
                    loadSequences()
                } else {
                    UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE CASE
    // ═══════════════════════════════════════════════════════════════════════

    private fun deleteTreatmentCase() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.deleteTreatmentCase(caseId)
                if (r.isSuccessful) {
                    UiUtils.toast(this@CaseSingleActivity, "Case deleted")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    UiUtils.toast(this@CaseSingleActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@CaseSingleActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}
