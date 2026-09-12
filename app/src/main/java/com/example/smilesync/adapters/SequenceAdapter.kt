package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.PlanSequence

class SequenceAdapter(
    private val sequences: List<PlanSequence>,
    /** null = read-only (no edit/delete shown) */
    private val onEdit:   ((PlanSequence) -> Unit)? = null,
    private val onDelete: ((PlanSequence) -> Unit)? = null
) : RecyclerView.Adapter<SequenceAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvOrder  : TextView  = v.findViewById(R.id.tv_seq_order)
        val tvDesc   : TextView  = v.findViewById(R.id.tv_seq_description)
        val tvCode   : TextView  = v.findViewById(R.id.tv_seq_code)
        val tvStatus : TextView  = v.findViewById(R.id.tv_seq_status)
        val tvNotes  : TextView  = v.findViewById(R.id.tv_seq_notes)
        val btnEdit  : ImageView = v.findViewById(R.id.btn_edit_seq)
        val btnDelete: ImageView = v.findViewById(R.id.btn_delete_seq)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sequence, parent, false))

    override fun getItemCount() = sequences.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = sequences[position]
        holder.tvOrder.text = "#${s.orderNumber}"
        holder.tvDesc.text  = s.description
        holder.tvCode.text  = s.procedureCode ?: ""
        holder.tvCode.visibility = if (s.procedureCode.isNullOrBlank()) View.GONE else View.VISIBLE

        val statusLabel = when (s.seqStatus) {
            2    -> "In Progress"
            3    -> "Done"
            else -> "Planned"
        }
        holder.tvStatus.text = statusLabel
        val statusBg = when (s.seqStatus) {
            3    -> 0xFF1B5E20.toInt()   // dark green
            2    -> 0xFF1565C0.toInt()   // dark blue
            else -> 0xFF424242.toInt()   // grey
        }
        holder.tvStatus.setBackgroundColor(statusBg)

        holder.tvNotes.text = s.seqNotes ?: ""
        holder.tvNotes.visibility = if (s.seqNotes.isNullOrBlank()) View.GONE else View.VISIBLE

        val editable = onEdit != null
        holder.btnEdit.visibility   = if (editable) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (editable) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener   { onEdit?.invoke(s) }
        holder.btnDelete.setOnClickListener { onDelete?.invoke(s) }
    }
}
