package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.smilesync.model.Patient
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.launch

class ThreeDFragment : Fragment() {

    private lateinit var rvPatients  : RecyclerView
    private lateinit var progressBar : ProgressBar
    private lateinit var tvEmpty     : TextView
    private lateinit var etSearch    : EditText
    private lateinit var adapter     : PatientPickerAdapter
    
    private var allPatients = listOf<Patient>()

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
        etSearch    = view.findViewById(R.id.et_search_patient)
        
        adapter = PatientPickerAdapter { patient ->
            // Launch WebView with selected patient
            startActivity(Intent(requireContext(), WebView3DActivity::class.java).apply {
                putExtra(WebView3DActivity.EXTRA_PATIENT_ID, patient.patientId)
                putExtra(WebView3DActivity.EXTRA_PATIENT_NAME, patient.fullName)
            })
        }
        
        rvPatients.layoutManager = LinearLayoutManager(requireContext())
        rvPatients.adapter = adapter
        
        setupSearch()
        loadPatients()
    }
    
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPatients(s?.toString() ?: "")
            }
        })
    }
    
    private fun filterPatients(query: String) {
        val filtered = if (query.isBlank()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.fullName.contains(query, ignoreCase = true) ||
                patient.firstName.contains(query, ignoreCase = true) ||
                patient.lastName.contains(query, ignoreCase = true)
            }
        }
        
        adapter.update(filtered)
        tvEmpty.visibility    = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvPatients.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        
        if (query.isNotBlank() && filtered.isEmpty()) {
            tvEmpty.text = "No patients found matching \"$query\""
        } else if (filtered.isEmpty()) {
            tvEmpty.text = "No patients found"
        }
    }

    private fun loadPatients() {
        progressBar.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Get all patients with a high limit
                val params = mapOf("limit" to "1000")
                val response = RetrofitClient.api.getPatients(params)
                if (!isAdded) return@launch
                
                if (response.isSuccessful) {
                    allPatients = response.body()?.result?.data ?: emptyList()
                    adapter.update(allPatients)
                    tvEmpty.visibility    = if (allPatients.isEmpty()) View.VISIBLE else View.GONE
                    rvPatients.visibility = if (allPatients.isEmpty()) View.GONE else View.VISIBLE
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
