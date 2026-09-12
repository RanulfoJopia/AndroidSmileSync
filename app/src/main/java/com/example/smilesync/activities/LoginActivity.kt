package com.example.smilesync.activities

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var btnLogin: AppCompatButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-redirect if already logged in
        val token = TokenManager.getInstance(this).getToken()
        if (!token.isNullOrBlank()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail           = findViewById(R.id.etEmail)
        etPassword        = findViewById(R.id.etPassword)
        ivTogglePassword  = findViewById(R.id.ivTogglePassword)
        btnLogin          = findViewById(R.id.btnLogin)
        tvForgotPassword  = findViewById(R.id.tvForgotPassword)
        tvRegister        = findViewById(R.id.tvRegister)
        progressBar       = findViewById(R.id.loginProgressBar)
    }

    private fun setupClickListeners() {
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            ivTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener { doLogin() }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) { etEmail.error = "Email is required"; etEmail.requestFocus(); return }
        if (password.isEmpty()) { etPassword.error = "Password is required"; etPassword.requestFocus(); return }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(
                    mapOf("email" to email, "password" to password)
                )
                if (response.isSuccessful) {
                    val result = response.body()?.result
                    val token  = result?.token
                    val dentist = result?.dentist
                    if (!token.isNullOrBlank() && dentist != null) {
                        val tm = TokenManager.getInstance(this@LoginActivity)
                        tm.saveToken(token)
                        tm.saveDentist(dentist)
                        RetrofitClient.resetClient()
                        goToMain()
                    } else {
                        UiUtils.toast(this@LoginActivity, "Login failed — no token received")
                    }
                } else {
                    val msg = UiUtils.errorMessage(response)
                    UiUtils.toast(this@LoginActivity, msg)
                }
            } catch (e: Exception) {
                UiUtils.toast(this@LoginActivity, "Network error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "Signing in…" else "Sign In"
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
