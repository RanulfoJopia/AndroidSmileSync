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
import com.example.smilesync.activities.CaseFormActivity
import com.example.smilesync.activities.CaseSingleActivity
import com.example.smilesync.adapters.TreatmentCaseAdapter
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.model.Category
import com.example.smilesync.utils.UiUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

/**
 * Treatment Cases list screen — maps to Vue`s /treatment-cases page.
 * Shown via the "Consultation" bottom-nav tab.
 */
class ConsultationFragment : Fragment() {

    private lateinit var etSearch         : EditText
    private lateinit var spFilterCaseType : Spinner
    private lateinit var spFilterStatus   : Spinner
    private lateinit var rvCases          : RecyclerView
    private lateinit var swipeRefresh     : SwipeRefreshLayout
    private lateinit var progressBar      : ProgressBar
    private lateinit var tvEmpty          : TextView
    private lateinit var fabAdd           : FloatingActionButton

    private var searchJob: Job? = null
    private var currentSearch = ""
    private var selectedCaseType: Int? = null
    private var selectedStatus: Int? = null
    
    private var caseTypes: List<Category> = emptyList()
    private var statuses: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_consultation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch         = view.findViewById(R.id.et_search_cases)
        spFilterCaseType = view.findViewById(R.id.sp_filter_case_type)
        spFilterStatus   = view.findViewById(R.id.sp_filter_status)
        rvCases          = view.findViewById(R.id.rv_consultations)
        swipeRefresh     = view.findViewById(R.id.swipe_refresh_cases)
        progressBar      = view.findViewById(R.id.cases_progress)
        tvEmpty          = view.findViewById(R.id.tv_empty_cases_list)
        fabAdd           = view.findViewById(R.id.fab_add_case)

        rvCases.layoutManager = LinearLayoutManager(requireContext())

        swipeRefresh.setOnRefreshListener { loadCases() }

        fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), CaseFormActivity::class.java))
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(350)
                    currentSearch = s?.toString()?.trim() ?: ""
                    loadCases()
                }
            }
        })

        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        loadCases()
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getCategories()
                if (!isAdded) return@launch
                
                if (r.isSuccessful) {
                    val result = r.body()?.result
                    
                    // Case Types from CASES map
                    result?.cases?.let { casesMap ->
                        caseTypes = casesMap.map { (name, id) ->
                            Category(value = id, label = name)
                        }
                        setupCaseTypeSpinner()
                    }
                    
                    // TODO: Get plan statuses from backend
                    // For now, use hardcoded statuses
                    statuses = listOf(
                        Category(1, "Pending"),
                        Category(2, "In Progress"),
                        Category(3, "Completed"),
                        Category(4, "Cancelled")
                    )
                    setupStatusSpinner()
                }
                
                loadCases()
            } catch (e: Exception) {
                if (isAdded) {
                    UiUtils.toast(requireContext(), "Failed to load filters: ${e.message}")
                    loadCases()
                }
            }
        }
    }

    private fun setupCaseTypeSpinner() {
        val options = mutableListOf("All Case Types")
        options.addAll(caseTypes.map { it.label })
        
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_small, options)
        spFilterCaseType.adapter = adapter
        
        spFilterCaseType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCaseType = if (position == 0) null else caseTypes[position - 1].value
                loadCases()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupStatusSpinner() {
        val options = mutableListOf("All Statuses")
        options.addAll(statuses.map { it.label })
        
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_small, options)
        spFilterStatus.adapter = adapter
        
        spFilterStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatus = if (position == 0) null else statuses[position - 1].value
                loadCases()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCases() {
        progressBar.visibility = View.VISIBLE
        
        val params = mutableMapOf<String, String>("limit" to "100")
        if (currentSearch.isNotBlank()) params["search"] = currentSearch
        if (selectedCaseType != null) params["case_type"] = selectedCaseType.toString()
        if (selectedStatus != null) params["status"] = selectedStatus.toString()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getTreatmentCases(params)
                if (!isAdded) return@launch
                
                if (r.isSuccessful) {
                    val list = r.body()?.result?.data ?: emptyList()
                    tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    rvCases.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                    rvCases.adapter = TreatmentCaseAdapter(list) { case ->
                        startActivity(
                            Intent(requireContext(), CaseSingleActivity::class.java).apply {
                                putExtra("case_id", case.caseId)
                            }
                        )
                    }
                } else {
                    if (isAdded) UiUtils.toast(requireContext(), UiUtils.errorMessage(r))
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
