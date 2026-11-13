package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.activities.CaseFormActivity
import com.example.smilesync.activities.CaseSingleActivity
import com.example.smilesync.activities.MainActivity
import com.example.smilesync.activities.PatientFormActivity
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
    private lateinit var tvProfileInitials: TextView
    private lateinit var cvProfileAvatar: MaterialCardView
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
    
    // Quick action buttons
    private lateinit var btnAddPatient: LinearLayout
    private lateinit var btn3dModel: LinearLayout
    private lateinit var btnTreatmentPlan: LinearLayout
    private lateinit var btnTreatmentCase: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDoctorName         = view.findViewById(R.id.tv_doctor_name)
        tvProfileInitials    = view.findViewById(R.id.tv_profile_initials)
        cvProfileAvatar      = view.findViewById(R.id.cv_profile_avatar)
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
        
        // Quick action buttons
        btnAddPatient        = view.findViewById(R.id.btn_add_patient)
        btn3dModel           = view.findViewById(R.id.btn_3d_model)
        btnTreatmentPlan     = view.findViewById(R.id.btn_treatment_plan)
        btnTreatmentCase     = view.findViewById(R.id.btn_treatment_case)

        // Show dentist name and initials from cache immediately
        val dentist = TokenManager.getInstance(requireContext()).getDentist()
        val fullName = dentist?.fullName?.ifBlank { "Dr. User" } ?: "Dr. User"
        tvDoctorName.text = "Dr. ${dentist?.lastName ?: "Ongbak"}"
        
        // Set profile initials using same logic as SettingsFragment
        tvProfileInitials.text = getInitials(fullName)

        // Display current date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvDashboardDate.text = dateFormat.format(Date())

        // Add click listener to profile avatar for logout popup
        cvProfileAvatar.setOnClickListener {
            showLogoutMenu(it)
        }
        
        // Setup quick action click listeners
        setupQuickActions()

        loadDashboard()
    }
    
    private fun setupQuickActions() {
        // Add Patient - Opens PatientFormActivity
        btnAddPatient.setOnClickListener {
            startActivity(Intent(requireContext(), PatientFormActivity::class.java))
        }
        
        // 3D Model - Navigates to 3D tab
        btn3dModel.setOnClickListener {
            (activity as? MainActivity)?.navigateTo3DTab()
        }
        
        // Treatment Plan - Navigates to Treatment Cases (Consultation) tab
        btnTreatmentPlan.setOnClickListener {
            (activity as? MainActivity)?.navigateToConsultationTab()
        }
        
        // Treatment Case - Opens CaseFormActivity to add new case
        btnTreatmentCase.setOnClickListener {
            startActivity(Intent(requireContext(), CaseFormActivity::class.java))
        }
    }

    private fun getInitials(fullName: String): String {
        val parts = fullName.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            parts.size == 1 -> "${parts[0].take(2).uppercase()}"
            else -> "DR"
        }
    }

    private fun showLogoutMenu(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    handleLogout()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun handleLogout() {
        UiUtils.confirm(
            requireContext(),
            "Logout",
            "Are you sure you want to log out?"
        ) {
            (activity as? MainActivity)?.doLogout()
        }
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

                        // Get initials from patient name (format: "LastName, FirstName")
                        val names = latestCase.patientFullName.split(",").map { it.trim() }
                        val initials = if (names.size >= 2) {
                            // First initial from first name, second initial from last name
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