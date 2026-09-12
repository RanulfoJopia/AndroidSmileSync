package com.example.smilesync.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.ApiConfig
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import com.google.gson.Gson
import kotlinx.coroutines.launch

/**
 * 3D Dental Examination screen.
 *
 * Hosts a WebView that loads dental_exam.html from assets.
 * The HTML page uses Three.js + GLTFLoader to render teeth_separated.glb.
 *
 * Communication:
 *   Android → JS:  webView.evaluateJavascript("window.Android.sendToPage(...)", null)
 *   JS → Android:  @JavascriptInterface methods on JsBridge
 */
class DentalExamActivity : AppCompatActivity() {

    private var patientId   : Int    = -1
    private var patientName : String = ""

    private lateinit var webView      : WebView
    private lateinit var progressBar  : ProgressBar
    private lateinit var tvPatientName: TextView
    private lateinit var tvSessionId  : TextView
    private lateinit var btnUndo      : TextView
    private lateinit var btnRedo      : TextView
    private lateinit var btnSave      : TextView

    private val gson = Gson()
    private var currentMeasureId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dental_exam)

        patientId   = intent.getIntExtra("patient_id",   -1)
        patientName = intent.getStringExtra("patient_name") ?: ""

        if (patientId == -1) { finish(); return }

        bindViews()
        setupTopBar()
        setupWebView()
        loadLatestMeasurement()
    }

    private fun bindViews() {
        webView       = findViewById(R.id.dental_webview)
        progressBar   = findViewById(R.id.dental_exam_progress)
        tvPatientName = findViewById(R.id.tv_dental_patient_name)
        tvSessionId   = findViewById(R.id.tv_dental_session_id)
        btnUndo       = findViewById(R.id.btn_undo)
        btnRedo       = findViewById(R.id.btn_redo)
        btnSave       = findViewById(R.id.btn_save_exam)

        tvPatientName.text = patientName
    }

    private fun setupTopBar() {
        findViewById<View>(R.id.btn_back_dental).setOnClickListener { finish() }

        btnUndo.setOnClickListener {
            webView.evaluateJavascript("if(window.smileUndo) window.smileUndo();", null)
        }
        btnRedo.setOnClickListener {
            webView.evaluateJavascript("if(window.smileRedo) window.smileRedo();", null)
        }
        btnSave.setOnClickListener {
            // Ask the WebView to gather and send its current state
            webView.evaluateJavascript("if(window.smileRequestSave) window.smileRequestSave();", null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled          = true
            domStorageEnabled          = true
            allowFileAccess            = true
            allowContentAccess         = true
            mixedContentMode           = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode                  = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
        }

        // Attach the JS→Android bridge
        webView.addJavascriptInterface(JsBridge(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                android.util.Log.d("DentalExam/JS", "[${m.messageLevel()}] ${m.message()} (${m.sourceId()}:${m.lineNumber()})")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Inject patient context into the page after it loads
                val token = TokenManager.getInstance(this@DentalExamActivity).getToken() ?: ""
                val apiBase = ApiConfig.BASE_URL
                val cmd = """
                    window.smileInit({
                        patientId: $patientId,
                        patientName: '${patientName.replace("'", "\\'")}',
                        measureId: ${currentMeasureId ?: "null"},
                        token: '$token',
                        apiBase: '$apiBase'
                    });
                """.trimIndent()
                view.evaluateJavascript(cmd, null)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                android.util.Log.e("DentalExam", "WebView error: ${err.description}")
            }
        }

        webView.loadUrl("file:///android_asset/dental_exam.html")
    }

    /** Load the latest measurement session so we can resume it */
    private fun loadLatestMeasurement() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getLatestMeasurement(patientId)
                if (r.isSuccessful) {
                    currentMeasureId = r.body()?.result?.measureId
                    tvSessionId.text = currentMeasureId?.let { "Session #$it" } ?: "New session"
                }
            } catch (e: Exception) {
                tvSessionId.text = "New session"
            }
            // progressBar hidden in onPageFinished
        }
    }

    /** Saves the full examination payload received from the WebView to Laravel */
    private fun saveExamination(payloadJson: String) {
        lifecycleScope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                val body = gson.fromJson(payloadJson, Map::class.java) as Map<String, Any?>

                val r = RetrofitClient.api.saveMeasurement(patientId, body)
                if (r.isSuccessful) {
                    val mid = r.body()?.result?.measureId
                    if (mid != null) {
                        currentMeasureId = mid
                        runOnUiThread {
                            tvSessionId.text = "Session #$mid"
                            UiUtils.toast(this@DentalExamActivity, "Examination saved ✓")
                            // Notify the page of the new measureId so it can use it for future patches
                            webView.evaluateJavascript(
                                "if(window.smileOnSaved) window.smileOnSaved($mid);", null
                            )
                        }
                    }
                } else {
                    runOnUiThread {
                        UiUtils.toast(this@DentalExamActivity, UiUtils.errorMessage(r))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    UiUtils.toast(this@DentalExamActivity, "Save failed: ${e.message}")
                }
            }
        }
    }

    /** Handles a single tooth upsert (called frequently from the WebView during interaction) */
    private fun upsertTooth(toothNumber: Int, status: String, procedures: String, notes: String) {
        val measureId = currentMeasureId ?: return   // need a session first; page should call full save first

        val procList = procedures.split(",").filter { it.isNotBlank() }
        val body = mapOf<String, Any?>(
            "status"       to status,
            "procedures"   to procList,
            "record_notes" to notes.ifBlank { null }
        )
        lifecycleScope.launch {
            try {
                RetrofitClient.api.upsertToothRecord(patientId, measureId, toothNumber, body)
            } catch (e: Exception) { /* silent — background patch */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JavaScript → Android bridge
    // ═══════════════════════════════════════════════════════════════════════

    inner class JsBridge {

        /** Called by the page when the user taps Save / the Android Save button triggers it */
        @JavascriptInterface
        fun onSaveRequested(payloadJson: String) {
            saveExamination(payloadJson)
        }

        /** Called after every tooth interaction for incremental background saves */
        @JavascriptInterface
        fun onToothUpdated(toothNumber: Int, status: String, procedures: String, notes: String) {
            upsertTooth(toothNumber, status, procedures, notes)
        }

        /** Called when the page is fully initialised */
        @JavascriptInterface
        fun onReady() {
            runOnUiThread {
                progressBar.visibility = View.GONE
                UiUtils.toast(this@DentalExamActivity, "3D model loaded")
            }
        }

        /** Log from JS — useful during development */
        @JavascriptInterface
        fun log(message: String) {
            android.util.Log.d("DentalExam/Bridge", message)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
