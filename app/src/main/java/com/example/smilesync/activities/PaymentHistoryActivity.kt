package com.example.smilesync.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.launch

class PaymentHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        loadInvoices()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadInvoices() {
        val progress = findViewById<ProgressBar>(R.id.progress)
        val tvEmpty  = findViewById<TextView>(R.id.tv_empty)
        val rv       = findViewById<RecyclerView>(R.id.rv_invoices)

        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getPaymentHistory()
                progress.visibility = View.GONE

                if (r.isSuccessful) {
                    val result   = r.body()?.result
                    val invoices = result?.get("invoices") as? List<Map<String, Any?>> ?: emptyList()

                    if (invoices.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rv.visibility      = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rv.visibility      = View.VISIBLE
                        rv.layoutManager   = LinearLayoutManager(this@PaymentHistoryActivity)
                        rv.adapter         = InvoiceAdapter(invoices)
                    }
                } else {
                    tvEmpty.text       = "Could not load payment history."
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                tvEmpty.text        = "Error: ${e.message}"
                tvEmpty.visibility  = View.VISIBLE
            }
        }
    }

    // ── Inline adapter ────────────────────────────────────────────────────

    private inner class InvoiceAdapter(
        private val items: List<Map<String, Any?>>
    ) : RecyclerView.Adapter<InvoiceAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvDesc   : TextView = v.findViewById(R.id.tv_invoice_desc)
            val tvDate   : TextView = v.findViewById(R.id.tv_invoice_date)
            val tvAmount : TextView = v.findViewById(R.id.tv_invoice_amount)
            val tvStatus : TextView = v.findViewById(R.id.tv_invoice_status)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_invoice, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val inv = items[pos]

            h.tvDesc.text   = inv["description"]?.toString() ?: "Subscription renewal"
            h.tvAmount.text = "$${inv["amount"] ?: "0.00"} ${inv["currency"] ?: "USD"}"

            // Format date from ISO string
            val rawDate = inv["date"]?.toString()
            h.tvDate.text = rawDate?.take(10) ?: "—"

            // Status color
            val status = inv["status"]?.toString() ?: "paid"
            h.tvStatus.text = status.replaceFirstChar { it.uppercase() }
            h.tvStatus.setTextColor(
                when (status) {
                    "paid"       -> android.graphics.Color.parseColor("#2E7D32")
                    "open"       -> android.graphics.Color.parseColor("#1565C0")
                    "void",
                    "uncollectible" -> android.graphics.Color.parseColor("#D32F2F")
                    else         -> android.graphics.Color.parseColor("#888888")
                }
            )
        }
    }
}
