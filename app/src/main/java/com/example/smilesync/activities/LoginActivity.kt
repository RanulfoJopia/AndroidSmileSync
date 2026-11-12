package com.example.smilesync.activities

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var cbRemember: CheckBox
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvSignUp: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: View
    
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-redirect if already logged in
        if (TokenManager.getInstance(this).isLoggedIn()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        ivTogglePassword = findViewById(R.id.iv_toggle_password)
        cbRemember = findViewById(R.id.cb_remember)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        btnLogin = findViewById(R.id.btn_login)
        tvSignUp = findViewById(R.id.tv_sign_up)
        progressBar = findViewById(R.id.login_progress)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnLogin.setOnClickListener { doLogin() }
        
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }
        
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun doLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(
                    mapOf("email" to email, "password" to password)
                )
                if (response.isSuccessful) {
                    val result = response.body()?.result
                    val token = result?.token
                    val dentist = result?.dentist
                    if (!token.isNullOrBlank() && dentist != null) {
                        val tm = TokenManager.getInstance(this@LoginActivity)
                        tm.saveToken(token)
                        tm.saveDentist(dentist)
                        goToMain()
                    } else {
                        UiUtils.toast(this@LoginActivity, "Login failed: no token returned")
                        setLoading(false)
                    }
                } else {
                    UiUtils.toast(this@LoginActivity, "Login failed: ${response.message()}")
                    setLoading(false)
                }
            } catch (e: Exception) {
                UiUtils.toast(this@LoginActivity, "Login error: ${e.message}")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}