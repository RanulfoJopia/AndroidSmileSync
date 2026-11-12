package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.TreatmentCase
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardCaseAdapter(
    private var cases: List<TreatmentCase>,
    private val onClick: (TreatmentCase) -> Unit
) : RecyclerView.Adapter<DashboardCaseAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitials   : TextView  = v.findViewById(R.id.tv_initials)
        val tvName       : TextView  = v.findViewById(R.id.tv_patient_name)
        val tvType       : TextView  = v.findViewById(R.id.tv_case_type)
        val tvDate       : TextView  = v.findViewById(R.id.tv_case_date)
        val tvStatus     : TextView  = v.findViewById(R.id.tv_status_pill)
        val divider      : View      = v.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_case_dashboard, parent, false))

    override fun getItemCount() = cases.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = cases[position]
        val ctx = holder.itemView.context

        // Initials: "Last, First" → "LF"
        val first = c.firstName?.firstOrNull()?.uppercaseChar() ?: ' '
        val last  = c.lastName?.firstOrNull()?.uppercaseChar() ?: ' '
        holder.tvInitials.text = "$last$first"

        // Avatar background color cycles through a set of soft colors
        val avatarColors = listOf(
            R.drawable.bg_initials_blue,
            R.drawable.bg_stat_icon_green,
            R.drawable.bg_stat_icon_purple,
            R.drawable.bg_stat_icon_orange,
        )
        val avatarTextColors = listOf(
            0xFF0066CC.toInt(),
            0xFF2E7D32.toInt(),
            0xFF6A1B9A.toInt(),
            0xFFE65100.toInt(),
        )
        val colorIdx = position % avatarColors.size
        holder.tvInitials.background = ContextCompat.getDrawable(ctx, avatarColors[colorIdx])
        holder.tvInitials.setTextColor(avatarTextColors[colorIdx])

        // Patient name in "Last, First" format
        holder.tvName.text = c.patientFullName

        // Case title as type
        holder.tvType.text = c.caseTitle.ifBlank { "—" }

        // Date
        holder.tvDate.text = formatDate(c.createdAt)

        // Status pill
        when {
            c.consentGivenAt != null -> {
                holder.tvStatus.text = "Completed"
                holder.tvStatus.setTextColor(0xFF1B5E20.toInt())
                holder.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_has_plan)
            }
            c.planId != null -> {
                holder.tvStatus.text = "Has Plan"
                holder.tvStatus.setTextColor(0xFF2E7D32.toInt())
                holder.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_has_plan)
            }
            else -> {
                holder.tvStatus.text = "No Plan"
                holder.tvStatus.setTextColor(0xFFE65100.toInt())
                holder.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_no_plan)
            }
        }

        // Hide divider on last item
        holder.divider.visibility = if (position == cases.size - 1) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onClick(c) }
    }

    fun update(newCases: List<TreatmentCase>) {
        cases = newCases
        notifyDataSetChanged()
    }

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            val date = sdf.parse(iso) ?: return iso.take(10)
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) {
            try {
                // Fallback for other date formats
                val sdf2 = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = sdf2.parse(iso.take(10)) ?: return iso.take(10)
                SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
            } catch (e2: Exception) { iso.take(10) }
        }
    }
}
