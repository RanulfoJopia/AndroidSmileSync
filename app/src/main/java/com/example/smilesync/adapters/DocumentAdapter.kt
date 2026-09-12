package com.example.smilesync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.model.PatientDocument

class DocumentAdapter(
    private val items: MutableList<PatientDocument>,
    private val onDelete: (PatientDocument) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle  : TextView  = v.findViewById(R.id.tv_doc_title)
        val tvDate   : TextView  = v.findViewById(R.id.tv_doc_date)
        val btnDelete: ImageView = v.findViewById(R.id.btn_delete_doc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.tvTitle.text = d.docTitle.ifBlank { d.filePath?.substringAfterLast('/') ?: "Document" }
        holder.tvDate.text  = d.createdAt?.take(10) ?: ""
        holder.btnDelete.setOnClickListener { onDelete(d) }
    }

    fun removeItem(doc: PatientDocument) {
        val idx = items.indexOfFirst { it.documentId == doc.documentId }
        if (idx >= 0) { items.removeAt(idx); notifyItemRemoved(idx) }
    }
}
