package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.activities.WebView3DActivity
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.model.Patient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*

class PatientPickerFragment : BottomSheetDialogFragment() {

    private lateinit var etSearch   : EditText
    private lateinit var rvPatients : RecyclerView
    private lateinit var progress   : ProgressBar
    private lateinit var tvEmpty    : TextView

    private var allPatients  : List<Patient> = emptyList()
    private var searchJob    : Job? = null

    // â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_patient_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch   = view.findViewById(R.id.et_search_patient_picker)
        rvPatients = view.findViewById(R.id.rv_patient_picker)
        progress   = view.findViewById(R.id.picker_progress)
        tvEmpty    = view.findViewById(R.id.tv_picker_empty)

        rvPatients.layoutManager = LinearLayoutManager(requireContext())

        // Expand sheet to half-screen on open
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(250)
                    filterList(s?.toString()?.trim() ?: "")
                }
            }
        })

        loadPatients()
    }

    // â”€â”€ Data â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun loadPatients() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility  = View.GONE
        rvPatients.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Use paginated endpoint with a large limit to get all patients
                val response = RetrofitClient.api.getPatients(mapOf("limit" to "200"))
                if (response.isSuccessful) {
                    allPatients = response.body()?.result?.data ?: emptyList()
                    filterList("")
                } else {
                    showEmpty("Could not load patients")
                }
            } catch (e: Exception) {
                showEmpty("Error: ${e.message}")
            } finally {
                if (isAdded) progress.visibility = View.GONE
            }
        }
    }

    private fun filterList(query: String) {
        if (!isAdded) return
        val filtered = if (query.isBlank()) allPatients
        else allPatients.filter {
            it.fullName.contains(query, ignoreCase = true) ||
            it.firstName.contains(query, ignoreCase = true) ||
            it.lastName.contains(query, ignoreCase = true)
        }
        if (filtered.isEmpty()) {
            showEmpty(if (query.isBlank()) "No patients found" else "No results for \"$query\"")
        } else {
            tvEmpty.visibility    = View.GONE
            rvPatients.visibility = View.VISIBLE
            rvPatients.adapter    = PatientPickerAdapter(filtered) { patient ->
                openDentalExam(patient)
            }
        }
    }

    private fun showEmpty(msg: String) {
        tvEmpty.text          = msg
        tvEmpty.visibility    = View.VISIBLE
        rvPatients.visibility = View.GONE
    }

    // â”€â”€ Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun openDentalExam(patient: Patient) {
        android.util.Log.d("PatientPicker", "Opening 3D exam for patient: id=${patient.patientId} name=${patient.fullName}")
        dismiss()
        startActivity(
            Intent(requireContext(), WebView3DActivity::class.java).apply {
                putExtra("patient_id",   patient.patientId)
                putExtra("patient_name", patient.fullName)
            }
        )
    }

    // â”€â”€ Adapter â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private inner class PatientPickerAdapter(
        private val items    : List<Patient>,
        private val onPick   : (Patient) -> Unit
    ) : RecyclerView.Adapter<PatientPickerAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val initials : TextView = v.findViewById(R.id.tv_picker_initials)
            val name     : TextView = v.findViewById(R.id.tv_picker_name)
            val info     : TextView = v.findViewById(R.id.tv_picker_info)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_patient_picker, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = items[pos]
            h.initials.text = p.initials
            h.name.text     = p.fullName

            // Build subtitle: age Â· sex Â· cases count
            val parts = listOfNotNull(
                p.ageYears()?.let { "$it yrs" },
                p.sexLabel.takeIf { it != "N/A" },
                p.casesCount?.let { "$it case${if (it != 1) "s" else ""}" }
            )
            h.info.text = parts.joinToString(" Â· ").ifBlank { "No additional info" }

            h.itemView.setOnClickListener { onPick(p) }
        }
    }

    companion object {
        const val TAG = "PatientPickerFragment"
        fun newInstance() = PatientPickerFragment()
    }
}
