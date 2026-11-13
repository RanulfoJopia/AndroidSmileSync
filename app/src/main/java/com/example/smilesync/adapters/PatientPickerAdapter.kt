package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.Patient

class PatientPickerAdapter(
    private val onClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientPickerAdapter.VH>() {

    private var patients: List<Patient> = emptyList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName    : TextView = v.findViewById(R.id.tv_picker_name)
        val tvInitials: TextView = v.findViewById(R.id.tv_picker_initials)
        val tvInfo    : TextView = v.findViewById(R.id.tv_picker_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_patient_picker, parent, false))

    override fun getItemCount() = patients.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = patients[position]
        holder.tvName.text = p.fullName
        holder.tvInitials.text = p.initials
        holder.tvInfo.text = p.sexLabel + (p.ageYears()?.let { " · $it yrs" } ?: "")
        holder.itemView.setOnClickListener { onClick(p) }
    }

    fun update(newList: List<Patient>) {
        patients = newList
        notifyDataSetChanged()
    }
}
