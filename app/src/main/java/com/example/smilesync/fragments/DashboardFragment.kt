package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.activities.CaseSingleActivity
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvDoctorName: TextView
    private lateinit var tvDashboardDate: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvPendingPlans: TextView
    private lateinit var tvPendingSequencing: TextView
    private lateinit var tvCompletedCases: TextView
    private lateinit var cvRecentCase: MaterialCardView
    private lateinit var tvCaseInitials: TextView
    private lateinit var tvCasePatientName: TextView
    private lateinit var tvCasePlanName: TextView
    private lateinit var tvCaseDate: TextView
    private lateinit var tvCaseStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyCases: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDoctorName         = view.findViewById(R.id.tv_doctor_name)
        tvDashboardDate      = view.findViewById(R.id.tv_dashboard_date)
        tvTotalPatients      = view.findViewById(R.id.tv_stat_total_patients)
        tvPendingPlans       = view.findViewById(R.id.tv_stat_pending_plans)
        tvPendingSequencing  = view.findViewById(R.id.tv_stat_pending_sequencing)
        tvCompletedCases     = view.findViewById(R.id.tv_stat_completed_cases)
        cvRecentCase         = view.findViewById(R.id.cv_recent_case)
        tvCaseInitials       = view.findViewById(R.id.tv_case_initials)
        tvCasePatientName    = view.findViewById(R.id.tv_case_patient_name)
        tvCasePlanName       = view.findViewById(R.id.tv_case_plan_name)
        tvCaseDate           = view.findViewById(R.id.tv_case_date)
        tvCaseStatus         = view.findViewById(R.id.tv_case_status)
        progressBar          = view.findViewById(R.id.dashboard_progress)
        tvEmptyCases         = view.findViewById(R.id.tv_empty_cases)

        // Show dentist name from cache immediately
        val dentist = TokenManager.getInstance(requireContext()).getDentist()
        tvDoctorName.text = "Dr. ${dentist?.lastName ?: "Ongbak"}"

        // Display current date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvDashboardDate.text = dateFormat.format(Date())

        loadDashboard()
    }

    private fun loadDashboard() {
        progressBar.visibility = View.VISIBLE
        cvRecentCase.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch patients count + latest cases in parallel
                val patientsDeferred = async { RetrofitClient.api.getPatients(mapOf("limit" to "1")) }
                val casesDeferred    = async { RetrofitClient.api.getTreatmentCases(mapOf("limit" to "1")) }

                val patientsResp = patientsDeferred.await()
                val casesResp    = casesDeferred.await()

                if (!isAdded) return@launch

                if (patientsResp.isSuccessful) {
                    val count = patientsResp.body()?.result?.count ?: 0
                    tvTotalPatients.text = count.toString()
                }

                if (casesResp.isSuccessful) {
                    val allCasesResp = RetrofitClient.api.getTreatmentCases(mapOf("limit" to "100"))
                    val cases = if (allCasesResp.isSuccessful) {
                        allCasesResp.body()?.result?.data ?: emptyList()
                    } else {
                        emptyList()
                    }

                    // Calculate stats
                    val pending   = cases.count { !it.hasPlan }
                    val completed = cases.count { it.planId != null && it.consentGivenAt != null }
                    val pendingSeq = 0 // TODO: Add sequencing logic when field available

                    tvPendingPlans.text       = pending.toString()
                    tvCompletedCases.text     = completed.toString()
                    tvPendingSequencing.text  = pendingSeq.toString()

                    // Show only the first case in the card
                    val latestCase = casesResp.body()?.result?.data?.firstOrNull()
                    
                    if (latestCase != null) {
                        tvEmptyCases.visibility = View.GONE
                        cvRecentCase.visibility = View.VISIBLE

                        // Get initials from patient name
                        val names = latestCase.patientFullName.split(",").map { it.trim() }
                        val initials = if (names.size >= 2) {
                            "${names[1].firstOrNull()?.uppercaseChar() ?: ""}${names[0].firstOrNull()?.uppercaseChar() ?: ""}"
                        } else {
                            latestCase.patientFullName.take(2).uppercase()
                        }
                        tvCaseInitials.text = initials

                        tvCasePatientName.text = latestCase.patientFullName
                        tvCasePlanName.text = latestCase.caseTitle

                        // Format date
                        val caseDate = latestCase.createdAt ?: ""
                        try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            val date = inputFormat.parse(caseDate.substring(0, 10))
                            tvCaseDate.text = if (date != null) outputFormat.format(date) else caseDate
                        } catch (e: Exception) {
                            tvCaseDate.text = caseDate
                        }

                        // Status badge
                        if (latestCase.hasPlan) {
                            tvCaseStatus.text = "Active"
                            tvCaseStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        } else {
                            tvCaseStatus.text = "No Plan"
                            tvCaseStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                        }

                        // Click listener
                        cvRecentCase.setOnClickListener {
                            startActivity(
                                Intent(requireContext(), CaseSingleActivity::class.java).apply {
                                    putExtra("case_id", latestCase.caseId)
                                }
                            )
                        }
                    } else {
                        tvEmptyCases.visibility = View.VISIBLE
                        cvRecentCase.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                if (isAdded) UiUtils.toast(requireContext(), "Failed to load dashboard: ${e.message}")
            } finally {
                if (isAdded) progressBar.visibility = View.GONE
            }
        }
    }
}