package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.PlanSequence
import com.google.android.material.card.MaterialCardView

class SequenceAdapter(
    private val sequences: List<PlanSequence>,
    /** null = read-only (no edit/delete shown) */
    private val onEdit:   ((PlanSequence) -> Unit)? = null,
    private val onDelete: ((PlanSequence) -> Unit)? = null
) : RecyclerView.Adapter<SequenceAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivStatusIcon  : ImageView          = v.findViewById(R.id.iv_status_icon)
        val cardStatusCircle: MaterialCardView = v.findViewById(R.id.card_status_circle)
        val tvDesc        : TextView           = v.findViewById(R.id.tv_seq_description)
        val tvCode        : TextView           = v.findViewById(R.id.tv_seq_code)
        val tvStatus      : TextView           = v.findViewById(R.id.tv_seq_status)
        val tvNotes       : TextView           = v.findViewById(R.id.tv_seq_notes)
        val btnEdit       : ImageView          = v.findViewById(R.id.btn_edit_seq)
        val btnDelete     : ImageView          = v.findViewById(R.id.btn_delete_seq)
        val layoutDuration: View               = v.findViewById(R.id.layout_duration)
        val layoutStatus  : MaterialCardView   = v.findViewById(R.id.layout_status)
        val timelineLine  : View               = v.findViewById(R.id.view_timeline_line)
        val timelineDot   : View               = v.findViewById(R.id.view_timeline_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sequence, parent, false))

    override fun getItemCount() = sequences.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = sequences[position]
        val isLast = position == sequences.size - 1
        
        // Description (title)
        holder.tvDesc.text = s.description
        
        // Procedure code as duration
        if (!s.procedureCode.isNullOrBlank()) {
            holder.tvCode.text = s.procedureCode
            holder.layoutDuration.visibility = View.VISIBLE
        } else {
            holder.layoutDuration.visibility = View.GONE
        }
        
        // Notes
        if (!s.seqNotes.isNullOrBlank()) {
            holder.tvNotes.text = s.seqNotes
            holder.tvNotes.visibility = View.VISIBLE
        } else {
            holder.tvNotes.visibility = View.GONE
        }
        
        // Status badge text and colors
        val statusLabel: String
        val iconRes: Int
        val circleBgColor: Int
        val badgeBgColor: Int
        val textColor: Int
        
        when (s.seqStatus) {
            3 -> {
                // Done: green with checkmark
                statusLabel = "Done"
                iconRes = R.drawable.ic_check
                circleBgColor = 0xFF4CAF50.toInt()
                badgeBgColor = 0xFFC8E6C9.toInt()
                textColor = 0xFF2E7D32.toInt()
            }
            2 -> {
                // In Progress: blue with progress icon
                statusLabel = "In Progress"
                iconRes = R.drawable.ic_progress
                circleBgColor = 0xFF2196F3.toInt()
                badgeBgColor = 0xFFBBDEFB.toInt()
                textColor = 0xFF1565C0.toInt()
            }
            else -> {
                // Planned: yellow/orange with clock
                statusLabel = "Planned"
                iconRes = R.drawable.ic_time
                circleBgColor = 0xFFFFA726.toInt()
                badgeBgColor = 0xFFFFF9C4.toInt()
                textColor = 0xFFF57F17.toInt()
            }
        }
        
        holder.tvStatus.text = statusLabel
        
        // Set status icon and circle color
        holder.ivStatusIcon.setImageResource(iconRes)
        holder.cardStatusCircle.setCardBackgroundColor(circleBgColor)
        
        // Set badge colors
        holder.layoutStatus.setCardBackgroundColor(badgeBgColor)
        holder.tvStatus.setTextColor(textColor)
        
        // Timeline visibility - hide line and dot for last item
        if (isLast) {
            holder.timelineLine.visibility = View.GONE
            holder.timelineDot.visibility = View.GONE
        } else {
            holder.timelineLine.visibility = View.VISIBLE
            holder.timelineDot.visibility = View.VISIBLE
        }
        
        // Edit/Delete buttons
        val editable = onEdit != null
        holder.btnEdit.visibility   = if (editable) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (editable) View.VISIBLE else View.GONE
        
        holder.btnEdit.setOnClickListener   { onEdit?.invoke(s) }
        holder.btnDelete.setOnClickListener { onDelete?.invoke(s) }
    }
}
