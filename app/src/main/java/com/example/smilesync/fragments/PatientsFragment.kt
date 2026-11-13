package com.example.smilesync.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.smilesync.R
import com.example.smilesync.activities.PatientDetailActivity
import com.example.smilesync.activities.PatientFormActivity
import com.example.smilesync.adapters.PatientAdapter
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.model.Patient
import com.example.smilesync.utils.UiUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class PatientsFragment : Fragment() {

    private lateinit var etSearch       : EditText
    private lateinit var rvPatients     : RecyclerView
    private lateinit var swipeRefresh   : SwipeRefreshLayout
    private lateinit var progressBar    : ProgressBar
    private lateinit var tvEmpty        : TextView
    private lateinit var fabAdd         : FloatingActionButton
    private lateinit var tvFilterDate   : TextView
    private lateinit var tvFilterSex    : TextView
    private lateinit var tvFilterAge    : TextView
    private lateinit var adapter        : PatientAdapter

    private var searchJob: Job? = null
    private var currentSearch = ""
    private var filterDate: String? = null
    private var filterSex: String? = null
    private var filterAge: String? = null
    private var allPatients: List<Patient> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_patients, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch      = view.findViewById(R.id.et_search_patients)
        rvPatients    = view.findViewById(R.id.rv_patients)
        swipeRefresh  = view.findViewById(R.id.swipe_refresh_patients)
        progressBar   = view.findViewById(R.id.patients_progress)
        tvEmpty       = view.findViewById(R.id.tv_empty_patients)
        fabAdd        = view.findViewById(R.id.fab_add_patient)
        tvFilterDate  = view.findViewById(R.id.tv_filter_date)
        tvFilterSex   = view.findViewById(R.id.tv_filter_sex)
        tvFilterAge   = view.findViewById(R.id.tv_filter_age)

        adapter = PatientAdapter(emptyList()) { patient ->
            startActivity(Intent(requireContext(), PatientDetailActivity::class.java).apply {
                putExtra("patient_id", patient.patientId)
            })
        }
        rvPatients.layoutManager = LinearLayoutManager(requireContext())
        rvPatients.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadPatients(currentSearch) }

        fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), PatientFormActivity::class.java))
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(350)
                    currentSearch = s?.toString()?.trim() ?: ""
                    applyFilters()
                }
            }
        })

        // Date filter click
        tvFilterDate.setOnClickListener {
            showDatePicker()
        }

        // Sex filter click
        tvFilterSex.setOnClickListener {
            showSexPicker()
        }

        // Age filter click
        tvFilterAge.setOnClickListener {
            showAgePicker()
        }

        loadPatients()
    }

    override fun onResume() {
        super.onResume()
        loadPatients(currentSearch)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                filterDate = String.format("%02d/%02d/%04d", month + 1, day, year)
                tvFilterDate.text = filterDate
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showSexPicker() {
        val options = arrayOf("All Sex", "Male", "Female")
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Sex")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        filterSex = null
                        tvFilterSex.text = "All Sex"
                    }
                    1 -> {
                        filterSex = "M"
                        tvFilterSex.text = "Male"
                    }
                    2 -> {
                        filterSex = "F"
                        tvFilterSex.text = "Female"
                    }
                }
                applyFilters()
            }
            .show()
    }

    private fun showAgePicker() {
        val options = arrayOf("All Ages", "0-18", "19-30", "31-50", "51-70", "71+")
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Age")
            .setItems(options) { _, which ->
                filterAge = when (which) {
                    0 -> null
                    1 -> "0-18"
                    2 -> "19-30"
                    3 -> "31-50"
                    4 -> "51-70"
                    5 -> "71+"
                    else -> null
                }
                tvFilterAge.text = options[which]
                applyFilters()
            }
            .show()
    }

    private fun applyFilters() {
        var filtered = allPatients

        // Filter by search text
        if (currentSearch.isNotBlank()) {
            filtered = filtered.filter {
                it.fullName.contains(currentSearch, ignoreCase = true) ||
                it.firstName.contains(currentSearch, ignoreCase = true) ||
                it.lastName.contains(currentSearch, ignoreCase = true)
            }
        }

        // Filter by date (patient since)
        if (filterDate != null) {
            filtered = filtered.filter { patient ->
                val patientDate = formatCreatedDate(patient.createdAt)
                patientDate == filterDate
            }
        }

        // Filter by sex
        if (filterSex != null) {
            filtered = filtered.filter { it.sex == filterSex }
        }

        // Filter by age
        if (filterAge != null) {
            filtered = filtered.filter { patient ->
                val age = patient.ageYears()
                age?.let {
                    when (filterAge) {
                        "0-18" -> it in 0..18
                        "19-30" -> it in 19..30
                        "31-50" -> it in 31..50
                        "51-70" -> it in 51..70
                        "71+" -> it >= 71
                        else -> true
                    }
                } ?: false
            }
        }

        adapter.update(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvPatients.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun formatCreatedDate(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            try {
                dateString.substring(0, 10).let {
                    val parts = it.split("-")
                    if (parts.size == 3) "${parts[1]}/${parts[2]}/${parts[0]}" else null
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun loadPatients(search: String = "") {
        progressBar.visibility = View.VISIBLE
        val params = mutableMapOf<String, String>("limit" to "50")
        if (search.isNotBlank()) params["search"] = search

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getPatients(params)
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val list = response.body()?.result?.data ?: emptyList()
                    allPatients = list
                    applyFilters()
                } else {
                    UiUtils.toast(requireContext(), UiUtils.errorMessage(response))
                }
            } catch (e: Exception) {
                if (isAdded) UiUtils.toast(requireContext(), "Error: ${e.message}")
            } finally {
                if (isAdded) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }
}
