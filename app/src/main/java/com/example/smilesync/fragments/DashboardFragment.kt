package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.activities.CaseSingleActivity
import com.example.smilesync.activities.PatientDetailActivity
import com.example.smilesync.adapters.TreatmentCaseAdapter
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var tvDoctorName: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvPendingPlans: TextView
    private lateinit var tvCompletedCases: TextView
    private lateinit var rvLatestCases: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyCases: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDoctorName    = view.findViewById(R.id.tv_doctor_name)
        tvTotalPatients = view.findViewById(R.id.tv_stat_total_patients)
        tvPendingPlans  = view.findViewById(R.id.tv_stat_pending_plans)
        tvCompletedCases= view.findViewById(R.id.tv_stat_completed_cases)
        rvLatestCases   = view.findViewById(R.id.rv_latest_cases)
        progressBar     = view.findViewById(R.id.dashboard_progress)
        tvEmptyCases    = view.findViewById(R.id.tv_empty_cases)

        // Show dentist name from cache immediately
        val dentist = TokenManager.getInstance(requireContext()).getDentist()
        tvDoctorName.text = "Welcome back, Dr. ${dentist?.lastName ?: ""}"

        rvLatestCases.layoutManager = LinearLayoutManager(requireContext())

        loadDashboard()
    }

    private fun loadDashboard() {
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch patients count + latest cases in parallel
                val patientsDeferred = async { RetrofitClient.api.getPatients(mapOf("limit" to "1")) }
                val casesDeferred    = async { RetrofitClient.api.getTreatmentCases(mapOf("limit" to "10")) }

                val patientsResp = patientsDeferred.await()
                val casesResp    = casesDeferred.await()

                if (!isAdded) return@launch

                if (patientsResp.isSuccessful) {
                    val count = patientsResp.body()?.result?.count ?: 0
                    tvTotalPatients.text = count.toString()
                }

                if (casesResp.isSuccessful) {
                    val cases = casesResp.body()?.result?.data ?: emptyList()
                    val total = casesResp.body()?.result?.count ?: 0

                    // Pending = cases with no plan; Completed = cases with plan_result
                    val pending   = cases.count { !it.hasPlan }
                    val completed = cases.count { it.planId != null && it.consentGivenAt != null }

                    tvPendingPlans.text   = pending.toString()
                    tvCompletedCases.text = completed.toString()

                    if (cases.isEmpty()) {
                        tvEmptyCases.visibility = View.VISIBLE
                        rvLatestCases.visibility = View.GONE
                    } else {
                        tvEmptyCases.visibility = View.GONE
                        rvLatestCases.visibility = View.VISIBLE
                        rvLatestCases.adapter = TreatmentCaseAdapter(cases) { case ->
                            startActivity(
                                Intent(requireContext(), CaseSingleActivity::class.java).apply {
                                    putExtra("case_id", case.caseId)
                                }
                            )
                        }
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
