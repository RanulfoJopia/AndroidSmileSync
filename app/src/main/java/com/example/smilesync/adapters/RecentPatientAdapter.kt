package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.models.RecentPatient

class RecentPatientAdapter(private val patients: List<RecentPatient>) :
    RecyclerView.Adapter<RecentPatientAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tv_patient_initials)
        val tvName: TextView = view.findViewById(R.id.tv_patient_name)
        val tvLastExam: TextView = view.findViewById(R.id.tv_last_exam)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val tvViewPatient: TextView = view.findViewById(R.id.tv_view_patient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_patient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patient = patients[position]

        // Generate initials from name (e.g. "Mike Smith" → "MS")
        val initials = patient.name
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        holder.tvInitials.text = initials

        holder.tvName.text = patient.name
        holder.tvLastExam.text = "Last Exam: ${patient.lastExam}"
        holder.tvStatus.text = patient.status

        holder.tvViewPatient.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Viewing ${patient.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = patients.size
}
