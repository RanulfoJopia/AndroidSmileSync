package com.example.smilesync.fragments

import android.app.DatePickerDialog
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
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ThreeDFragment : Fragment() {

    private lateinit var rvPatients  : RecyclerView
    private lateinit var progressBar : ProgressBar
    private lateinit var tvEmpty     : TextView
    private lateinit var adapter     : PatientPickerAdapter
    
    private lateinit var cardDateFrom : MaterialCardView
    private lateinit var cardDateTo   : MaterialCardView
    private lateinit var tvDateFrom   : TextView
    private lateinit var tvDateTo     : TextView
    
    private var selectedDateFrom: String? = null
    private var selectedDateTo: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

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
        
        cardDateFrom = view.findViewById(R.id.card_date_from)
        cardDateTo   = view.findViewById(R.id.card_date_to)
        tvDateFrom   = view.findViewById(R.id.tv_date_from)
        tvDateTo     = view.findViewById(R.id.tv_date_to)
        
        adapter = PatientPickerAdapter { patient ->
            // Launch WebView with selected patient
            startActivity(Intent(requireContext(), WebView3DActivity::class.java).apply {
                putExtra(WebView3DActivity.EXTRA_PATIENT_ID, patient.patientId)
                putExtra(WebView3DActivity.EXTRA_PATIENT_NAME, patient.fullName)
            })
        }
        
        rvPatients.layoutManager = LinearLayoutManager(requireContext())
        rvPatients.adapter = adapter
        
        setupFilters()
        loadPatients()
    }
    
    private fun setupFilters() {
        cardDateFrom.setOnClickListener { showDateFromPicker() }
        cardDateTo.setOnClickListener { showDateToPicker() }
    }
    
    private fun showDateFromPicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateFrom = dateFormat.format(calendar.time)
                tvDateFrom.text = displayFormat.format(calendar.time)
                loadPatients()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showDateToPicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateTo = dateFormat.format(calendar.time)
                tvDateTo.text = displayFormat.format(calendar.time)
                loadPatients()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadPatients() {
        progressBar.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Build params with date filters for measurement creation date
                val params = mutableMapOf<String, String>(
                    "limit" to "1000",
                    "hasMeasurement" to "true"  // Only get patients with measurements
                )
                selectedDateFrom?.let { params["measurementDateFrom"] = it }
                selectedDateTo?.let { params["measurementDateTo"] = it }
                
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
