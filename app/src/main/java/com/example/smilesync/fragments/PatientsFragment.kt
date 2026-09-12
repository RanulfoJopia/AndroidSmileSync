package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
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
import com.example.smilesync.utils.UiUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class PatientsFragment : Fragment() {

    private lateinit var etSearch       : EditText
    private lateinit var rvPatients     : RecyclerView
    private lateinit var swipeRefresh   : SwipeRefreshLayout
    private lateinit var progressBar    : ProgressBar
    private lateinit var tvEmpty        : TextView
    private lateinit var fabAdd         : FloatingActionButton
    private lateinit var adapter        : PatientAdapter

    private var searchJob: Job? = null
    private var currentSearch = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_patients, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch     = view.findViewById(R.id.et_search_patients)
        rvPatients   = view.findViewById(R.id.rv_patients)
        swipeRefresh = view.findViewById(R.id.swipe_refresh_patients)
        progressBar  = view.findViewById(R.id.patients_progress)
        tvEmpty      = view.findViewById(R.id.tv_empty_patients)
        fabAdd       = view.findViewById(R.id.fab_add_patient)

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
                    loadPatients(currentSearch)
                }
            }
        })

        loadPatients()
    }

    override fun onResume() {
        super.onResume()
        loadPatients(currentSearch)
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
                    adapter.update(list)
                    tvEmpty.visibility    = if (list.isEmpty()) View.VISIBLE else View.GONE
                    rvPatients.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
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
