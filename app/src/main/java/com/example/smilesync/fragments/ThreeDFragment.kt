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
import com.example.smilesync.activities.WebView3DActivity
import com.example.smilesync.adapters.PatientPickerAdapter
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.launch

class ThreeDFragment : Fragment() {

    private lateinit var rvPatients  : RecyclerView
    private lateinit var progressBar : ProgressBar
    private lateinit var tvEmpty     : TextView
    private lateinit var adapter     : PatientPickerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_3d, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvPatients  = view.findViewById(R.id.rv_3d_patients)
        progressBar = view.findViewById(R.id.progress_3d_patients)
        tvEmpty     = view.findViewById(R.id.tv_empty_3d_patients)
        
        adapter = PatientPickerAdapter { patient ->
            // Launch WebView with selected patient
            startActivity(Intent(requireContext(), WebView3DActivity::class.java).apply {
                putExtra(WebView3DActivity.EXTRA_PATIENT_ID, patient.patientId)
            })
        }
        
        rvPatients.layoutManager = LinearLayoutManager(requireContext())
        rvPatients.adapter = adapter
        
        loadPatients()
    }

    private fun loadPatients() {
        progressBar.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Use the paginated endpoint with a high limit to get all patients
                val params = mapOf("limit" to "1000")
                val response = RetrofitClient.api.getPatients(params)
                if (!isAdded) return@launch
                
                if (response.isSuccessful) {
                    val list = response.body()?.result?.data ?: emptyList()
                    adapter.update(list)
                    tvEmpty.visibility    = if (list.isEmpty()) View.VISIBLE else View.GONE
                    rvPatients.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    UiUtils.toast(requireContext(), UiUtils.errorMessage(response))
                }
            } catch (e: Exception) {
                if (isAdded) UiUtils.toast(requireContext(), "Error: ${e.message}")
            } finally {
                if (isAdded) progressBar.visibility = View.GONE
            }
        }
    }
}
