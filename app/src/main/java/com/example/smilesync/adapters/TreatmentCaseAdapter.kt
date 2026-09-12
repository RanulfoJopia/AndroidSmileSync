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
        val tvPatient  : TextView = v.findViewById(R.id.tv_case_patient)
        val tvTitle    : TextView = v.findViewById(R.id.tv_case_title)
        val tvDate     : TextView = v.findViewById(R.id.tv_case_date)
        val tvStatus   : TextView = v.findViewById(R.id.tv_case_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_treatment_case, parent, false))

    override fun getItemCount() = cases.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = cases[position]
        holder.tvPatient.text = c.patientFullName
        holder.tvTitle.text   = c.caseTitle
        holder.tvDate.text    = formatDate(c.createdAt)
        holder.tvStatus.text  = when {
            c.consentGivenAt != null -> "Consented"
            c.planId != null         -> "Has Plan"
            else                     -> "No Plan"
        }
        val statusBg = when {
            c.consentGivenAt != null -> 0xFF1B5E20.toInt()
            c.planId != null         -> 0xFF1565C0.toInt()
            else                     -> 0xFFE65100.toInt()
        }
        holder.tvStatus.setBackgroundColor(statusBg)
        holder.itemView.setOnClickListener { onClick(c) }
    }

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            val date = sdf.parse(iso) ?: return iso.take(10)
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) { iso.take(10) }
    }
}
