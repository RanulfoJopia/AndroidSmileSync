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

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etLicenseNumber: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var btnRegister: AppCompatButton
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    private var showPw = false
    private var showConfirmPw = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        etFullName            = findViewById(R.id.etFullName)
        etLicenseNumber       = findViewById(R.id.etLicenseNumber)
        etEmail               = findViewById(R.id.etEmail)
        etPhone               = findViewById(R.id.etPhone)
        etPassword            = findViewById(R.id.etPassword)
        etConfirmPassword     = findViewById(R.id.etConfirmPassword)
        ivTogglePassword      = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        btnRegister           = findViewById(R.id.btnRegister)
        tvLogin               = findViewById(R.id.tvLogin)
        progressBar           = findViewById(R.id.registerProgressBar)
    }

    private fun setupListeners() {
        ivTogglePassword.setOnClickListener {
            showPw = !showPw
            etPassword.transformationMethod = if (showPw)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            ivTogglePassword.setImageResource(if (showPw) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            etPassword.setSelection(etPassword.text.length)
        }
        ivToggleConfirmPassword.setOnClickListener {
            showConfirmPw = !showConfirmPw
            etConfirmPassword.transformationMethod = if (showConfirmPw)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            ivToggleConfirmPassword.setImageResource(if (showConfirmPw) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }
        btnRegister.setOnClickListener { doRegister() }
        tvLogin.setOnClickListener { finish() }
    }

    private fun doRegister() {
        val fullName  = etFullName.text.toString().trim()
        val email     = etEmail.text.toString().trim()
        val password  = etPassword.text.toString()
        val confirm   = etConfirmPassword.text.toString()

        if (fullName.isEmpty())  { etFullName.error = "Required"; return }
        if (email.isEmpty())     { etEmail.error = "Required"; return }
        if (password.length < 8) { etPassword.error = "Min 8 characters"; return }
        if (password != confirm) { etConfirmPassword.error = "Passwords do not match"; return }

        // Split full name into first/last for the API
        val parts     = fullName.split(" ", limit = 2)
        val firstName = parts[0]
        val lastName  = if (parts.size > 1) parts[1] else "."

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(mapOf(
                    "first_name"            to firstName,
                    "last_name"             to lastName,
                    "email"                 to email,
                    "password"              to password,
                    "password_confirmation" to confirm
                ))
                if (response.isSuccessful) {
                    val result  = response.body()?.result
                    val token   = result?.token
                    val dentist = result?.dentist
                    if (!token.isNullOrBlank() && dentist != null) {
                        val tm = TokenManager.getInstance(this@RegisterActivity)
                        tm.saveToken(token)
                        tm.saveDentist(dentist)
                        RetrofitClient.resetClient()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    } else {
                        UiUtils.toast(this@RegisterActivity, "Registration failed")
                    }
                } else {
                    UiUtils.toast(this@RegisterActivity, UiUtils.errorMessage(response))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@RegisterActivity, "Network error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled  = !loading
    }
}
