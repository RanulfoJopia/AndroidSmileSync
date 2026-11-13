package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.Patient
import java.text.SimpleDateFormat
import java.util.*

class PatientAdapter(
    private var patients: List<Patient>,
    private val onClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitials     : TextView = v.findViewById(R.id.tv_patient_initials)
        val tvName         : TextView = v.findViewById(R.id.tv_patient_name)
        val tvLastExam     : TextView = v.findViewById(R.id.tv_last_exam)
        val tvStatus       : TextView = v.findViewById(R.id.tv_status)
        val tvPatientSince : TextView = v.findViewById(R.id.tv_patient_since)
        val tvRecentCase   : TextView = v.findViewById(R.id.tv_recent_case)
        val tvSex          : TextView = v.findViewById(R.id.tv_sex)
        val ivView         : ImageView = v.findViewById(R.id.tv_view_patient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recent_patient, parent, false))

    override fun getItemCount() = patients.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = patients[position]
        
        // Initials and Name
        holder.tvInitials.text = p.initials
        holder.tvName.text = p.fullName
        
        // Status (cases)
        holder.tvLastExam.text = p.caseTitle?.let { "Recent: $it" } ?: "No cases yet"
        
        // Sex and Age badge
        holder.tvStatus.text = p.sexLabel + (p.ageYears()?.let { " · $it yrs" } ?: "")
        
        // Patient Since (format date to MM/DD/YYYY without time)
        holder.tvPatientSince.text = formatDate(p.createdAt)
        
        // Recent Case
        holder.tvRecentCase.text = p.caseTitle ?: "N/A"
        
        // Sex
        holder.tvSex.text = p.sexLabel
        
        // Click handlers
        holder.ivView.setOnClickListener { onClick(p) }
        holder.itemView.setOnClickListener { onClick(p) }
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString.substring(0, 10)
        } catch (e: Exception) {
            // Fallback to just the date part if parsing fails
            try {
                dateString.substring(0, 10).let {
                    val parts = it.split("-")
                    if (parts.size == 3) "${parts[1]}/${parts[2]}/${parts[0]}" else it
                } 
            } catch (e2: Exception) {
                "N/A"
            }
        }
    }

    fun update(newList: List<Patient>) {
        patients = newList
        notifyDataSetChanged()
    }
}
