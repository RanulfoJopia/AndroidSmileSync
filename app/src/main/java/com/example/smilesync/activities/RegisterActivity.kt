package com.example.smilesync.activities

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import com.example.smilesync.utils.NetworkUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirm: TextInputEditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivTogglePasswordConfirm: ImageView
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvSignIn: TextView
    private lateinit var btnBack: MaterialCardView
    private lateinit var progressBar: ProgressBar
    
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        etFirstName       = findViewById(R.id.et_first_name)
        etLastName        = findViewById(R.id.et_last_name)
        etEmail           = findViewById(R.id.et_email)
        etPassword        = findViewById(R.id.et_password)
        etPasswordConfirm = findViewById(R.id.et_password_confirm)
        ivTogglePassword  = findViewById(R.id.iv_toggle_password)
        ivTogglePasswordConfirm = findViewById(R.id.iv_toggle_password_confirm)
        btnRegister       = findViewById(R.id.btn_register)
        tvSignIn          = findViewById(R.id.tv_sign_in)
        btnBack           = findViewById(R.id.btn_back)
        progressBar       = findViewById(R.id.register_progress)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener { doRegister() }
        tvSignIn.setOnClickListener { 
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        btnBack.setOnClickListener { finish() }
        
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
        
        ivTogglePasswordConfirm.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etPasswordConfirm.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivTogglePasswordConfirm.setImageResource(R.drawable.ic_visibility)
            } else {
                etPasswordConfirm.transformationMethod = PasswordTransformationMethod.getInstance()
                ivTogglePasswordConfirm.setImageResource(R.drawable.ic_visibility_off)
            }
            etPasswordConfirm.setSelection(etPasswordConfirm.text?.length ?: 0)
        }
    }

    private fun doRegister() {
        val firstName = etFirstName.text.toString().trim()
        val lastName  = etLastName.text.toString().trim()
        val email     = etEmail.text.toString().trim()
        val password  = etPassword.text.toString()
        val confirm   = etPasswordConfirm.text.toString()

        if (firstName.isEmpty()) { etFirstName.error = "Required"; return }
        if (lastName.isEmpty())  { etLastName.error = "Required"; return }
        if (email.isEmpty())     { etEmail.error = "Required"; return }
        if (password.length < 8) { etPassword.error = "Min 8 characters"; return }
        if (password != confirm) { etPasswordConfirm.error = "Passwords do not match"; return }

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
                if (!NetworkUtils.isNetworkAvailable(this@RegisterActivity)) {
                    UiUtils.toast(this@RegisterActivity, "No internet connection. Please check your network settings.")
                } else if (NetworkUtils.isNetworkError(e)) {
                    UiUtils.toast(this@RegisterActivity, NetworkUtils.getErrorMessage(e))
                } else {
                    UiUtils.toast(this@RegisterActivity, "Network error: ${e.message}")
                }
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
