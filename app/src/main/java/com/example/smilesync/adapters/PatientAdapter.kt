package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.Patient

class PatientAdapter(
    private var patients: List<Patient>,
    private val onClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitials : TextView = v.findViewById(R.id.tv_patient_initials)
        val tvName     : TextView = v.findViewById(R.id.tv_patient_name)
        val tvLastExam : TextView = v.findViewById(R.id.tv_last_exam)
        val tvStatus   : TextView = v.findViewById(R.id.tv_status)
        val tvView     : TextView = v.findViewById(R.id.tv_view_patient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recent_patient, parent, false))

    override fun getItemCount() = patients.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = patients[position]
        holder.tvInitials.text  = p.initials
        holder.tvName.text      = p.fullName
        holder.tvLastExam.text  = p.caseTitle?.let { "Recent: $it" } ?: "No cases yet"
        holder.tvStatus.text    = p.sexLabel + (p.ageYears()?.let { " · $it yrs" } ?: "")
        holder.tvView.setOnClickListener { onClick(p) }
        holder.itemView.setOnClickListener { onClick(p) }
    }

    fun update(newList: List<Patient>) {
        patients = newList
        notifyDataSetChanged()
    }
}
