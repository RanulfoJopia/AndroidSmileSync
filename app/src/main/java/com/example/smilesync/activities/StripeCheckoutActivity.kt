package com.example.smilesync.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smilesync.R
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Full-screen WebView that opens the web app's subscription page.
 *
 * Returns RESULT_OK when the user navigates back so the caller
 * (SettingsFragment) can refresh the subscription status.
 */
class StripeCheckoutActivity : AppCompatActivity() {

    private lateinit var webView  : WebView
    private lateinit var progress : LinearProgressIndicator

    companion object {
        const val EXTRA_CHECKOUT_URL = "checkout_url"
        const val EXTRA_SESSION_ID   = "session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stripe_checkout)

        webView  = findViewById(R.id.checkout_webview)
        progress = findViewById(R.id.checkout_progress)

        val url = intent.getStringExtra(EXTRA_CHECKOUT_URL) ?: ""

        // Set contextual title
        val title = when {
            url.contains("/subscription") -> "Subscription"
            url.contains("/payment")      -> "Payment"
            else                          -> "SmileSync"
        }
        findViewById<TextView>(R.id.tv_checkout_title)?.text = title

        // Back → return OK so caller refreshes subscription status
        findViewById<View>(R.id.iv_checkout_back).setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        if (url.isBlank()) { finish(); return }

        setupWebView()
        webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Clear cookies and cache so a previous account's session never bleeds in.
        // The bridge token logs in the current user fresh each time via sessionStorage.
        android.webkit.CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        webView.clearCache(true)
        webView.clearHistory()

        with(webView.settings) {
            javaScriptEnabled                     = true
            domStorageEnabled                     = true
            mixedContentMode                      = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode                             = WebSettings.LOAD_NO_CACHE
            useWideViewPort                       = true
            loadWithOverviewMode                  = true
            javaScriptCanOpenWindowsAutomatically = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        // Allow all navigation — user can browse freely within the web app
        webView.webViewClient = WebViewClient()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else {
            setResult(RESULT_OK) // refresh subscription on return
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
