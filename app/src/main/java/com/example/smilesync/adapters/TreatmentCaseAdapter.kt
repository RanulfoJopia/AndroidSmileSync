package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.TreatmentCase
import java.text.SimpleDateFormat
import java.util.Locale

class TreatmentCaseAdapter(
    private val cases: List<TreatmentCase>,
    private val onClick: (TreatmentCase) -> Unit
) : RecyclerView.Adapter<TreatmentCaseAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvPatient     : TextView = v.findViewById(R.id.tv_case_patient)
        val tvTitle       : TextView = v.findViewById(R.id.tv_case_title)
        val tvDate        : TextView = v.findViewById(R.id.tv_case_date)
        val tvStatus      : TextView = v.findViewById(R.id.tv_case_status)
        val tvPlanStatus  : TextView = v.findViewById(R.id.tv_case_plan_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_treatment_case, parent, false))

    override fun getItemCount() = cases.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = cases[position]
        
        // Patient name
        holder.tvPatient.text = c.patientFullName
        
        // Case title
        holder.tvTitle.text = c.caseTitle
        
        // Created date
        holder.tvDate.text = "Created: ${formatDate(c.createdAt)}"
        
        // Status badge (top right)
        when {
            c.consentGivenAt != null -> {
                holder.tvStatus.text = "Consented"
                holder.tvStatus.setBackgroundColor(0xFF10B981.toInt()) // Green
                holder.tvStatus.visibility = View.VISIBLE
            }
            c.planId != null -> {
                holder.tvStatus.text = "Has Plan"
                holder.tvStatus.setBackgroundColor(0xFF2196F3.toInt()) // Blue
                holder.tvStatus.visibility = View.VISIBLE
            }
            else -> {
                holder.tvStatus.text = "No Plan"
                holder.tvStatus.setBackgroundColor(0xFFF59E0B.toInt()) // Amber
                holder.tvStatus.visibility = View.VISIBLE
            }
        }
        
        // Hide plan status for now (not in model)
        holder.tvPlanStatus.visibility = View.GONE
        
        holder.itemView.setOnClickListener { onClick(c) }
    }

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            val date = sdf.parse(iso) ?: return iso.take(10)
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) { 
            try {
                iso.take(10)
            } catch (e: Exception) { "—" }
        }
    }
}
