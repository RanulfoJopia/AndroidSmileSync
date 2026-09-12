package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.PatientAnamnesis

class AnamnesisAdapter(
    private val items: List<PatientAnamnesis>,
    /** null = read-only mode (no edit/delete icons shown) */
    private val onEdit:   ((PatientAnamnesis) -> Unit)? = null,
    private val onDelete: ((PatientAnamnesis) -> Unit)? = null
) : RecyclerView.Adapter<AnamnesisAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvDesc      : TextView  = v.findViewById(R.id.tv_anam_description)
        val tvTreatment : TextView  = v.findViewById(R.id.tv_anam_treatment)
        val tvSource    : TextView  = v.findViewById(R.id.tv_anam_source)
        val tvDate      : TextView  = v.findViewById(R.id.tv_anam_date)
        val tvSeverity  : TextView  = v.findViewById(R.id.tv_anam_severity)
        val btnEdit     : ImageView = v.findViewById(R.id.btn_edit_anam)
        val btnDelete   : ImageView = v.findViewById(R.id.btn_delete_anam)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_anamnesis, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        holder.tvDesc.text      = a.anamDescription
        holder.tvTreatment.text = a.treatment?.let { "Treatment: $it" } ?: ""
        holder.tvTreatment.visibility = if (a.treatment.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.tvSource.text    = "Source: ${a.sourceName ?: "N/A"}"
        val dateRange = when {
            a.dateStart != null && a.dateEnd != null ->
                "${a.dateStart.take(10)} – ${a.dateEnd.take(10)}"
            a.dateStart != null -> "${a.dateStart.take(10)} – present"
            else                -> "N/A"
        }
        holder.tvDate.text     = dateRange
        holder.tvSeverity.text = a.severityName ?: ""
        holder.tvSeverity.visibility = if (a.severityName.isNullOrBlank()) View.GONE else View.VISIBLE

        // Show action buttons only when callbacks are provided
        val editable = onEdit != null
        holder.btnEdit.visibility   = if (editable) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (editable) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener   { onEdit?.invoke(a) }
        holder.btnDelete.setOnClickListener { onDelete?.invoke(a) }
    }
}
