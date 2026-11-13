package com.example.smilesync.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.UiUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

/**
 * Full-screen WebView for displaying the 3D dental viewer.
 * 
 * Receives patient_id as an extra, generates a WebView bridge token,
 * and loads the web app's /mobile/webview?token=<token> endpoint.
 * 
 * The web app validates the token and routes to /3D/{patient_id}/embed.
 */
class WebView3DActivity : AppCompatActivity() {

    private lateinit var webView  : WebView
    private lateinit var progress : LinearProgressIndicator
    
    companion object {
        const val EXTRA_PATIENT_ID = "patient_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide action bar for full-screen experience
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_webview_3d)
        
        webView  = findViewById(R.id.webview_3d)
        progress = findViewById(R.id.webview_3d_progress)
        
        val patientId = intent.getIntExtra(EXTRA_PATIENT_ID, 0)
        
        if (patientId == 0) {
            UiUtils.toast(this, "Invalid patient ID")
            finish()
            return
        }
        
        setupWebView()
        generateTokenAndLoad(patientId)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Clear cookies and cache for fresh session
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        webView.clearCache(true)
        webView.clearHistory()
        
        with(webView.settings) {
            javaScriptEnabled                     = true
            domStorageEnabled                     = true
            databaseEnabled                       = true
            cacheMode                             = WebSettings.LOAD_NO_CACHE
            mixedContentMode                      = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                progress.progress = newProgress
            }
        }
        
        // Allow all navigation within the web app
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                // Inject CSS to center the 3D canvas after page loads
                Handler(Looper.getMainLooper()).postDelayed({
                    injectCenteringCSS()
                }, 500)
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    UiUtils.toast(this@WebView3DActivity, "Failed to load 3D viewer")
                }
            }
        }
    }
    
    private fun injectCenteringCSS() {
        val js = """
            javascript:(function() {
                var style = document.createElement('style');
                style.innerHTML = 'canvas { position: absolute !important; left: 50% !important; top: 50% !important; transform: translate(-50%, -50%) !important; }';
                document.head.appendChild(style);
            })()
        """.trimIndent()
        
        webView.loadUrl(js)
    }

    private fun generateTokenAndLoad(patientId: Int) {
        progress.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.generateWebViewToken(
                    purpose = "web3d",
                    patientId = patientId
                )
                
                if (response.isSuccessful) {
                    val token = response.body()?.result?.token
                    if (token != null) {
                        // Load web app with bridge token
                        val webUrl = "http://35.212.220.242/mobile/webview?token=$token"
                        webView.loadUrl(webUrl)
                    } else {
                        UiUtils.toast(this@WebView3DActivity, "Failed to generate access token")
                        finish()
                    }
                } else {
                    UiUtils.toast(this@WebView3DActivity, UiUtils.errorMessage(response))
                    finish()
                }
            } catch (e: Exception) {
                UiUtils.toast(this@WebView3DActivity, "Error: ${e.message}")
                finish()
            }
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
