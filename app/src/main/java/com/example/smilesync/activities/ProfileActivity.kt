package com.example.smilesync.activities

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvSubtitle       : TextView
    private lateinit var etFirstName      : EditText
    private lateinit var etLastName       : EditText
    private lateinit var etPassword       : EditText
    private lateinit var btnTogglePass    : ImageView
    private lateinit var btnSave          : Button
    private lateinit var progressBar      : ProgressBar

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bindViews()
        prefillFromCache()

        findViewById<View>(R.id.btn_back_profile).setOnClickListener { finish() }

        btnTogglePass.setOnClickListener { togglePasswordVisibility() }
        btnSave.setOnClickListener { saveProfile() }
    }

    private fun bindViews() {
        tvSubtitle    = findViewById(R.id.tv_profile_subtitle)
        etFirstName   = findViewById(R.id.et_first_name)
        etLastName    = findViewById(R.id.et_last_name)
        etPassword    = findViewById(R.id.et_password)
        btnTogglePass = findViewById(R.id.btn_toggle_password)
        btnSave       = findViewById(R.id.btn_save_profile)
        progressBar   = findViewById(R.id.profile_progress)
    }

    private fun prefillFromCache() {
        val dentist = TokenManager.getInstance(this).getDentist() ?: return
        tvSubtitle.text     = dentist.email
        etFirstName.setText(dentist.firstName)
        etLastName.setText(dentist.lastName)
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        if (passwordVisible) {
            etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePass.setImageResource(R.drawable.ic_eye_off)
        } else {
            etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePass.setImageResource(R.drawable.ic_eye)
        }
        // Keep cursor at end
        etPassword.setSelection(etPassword.text.length)
    }

    private fun saveProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName  = etLastName.text.toString().trim()
        val password  = etPassword.text.toString()

        if (firstName.isEmpty()) { etFirstName.error = "Required"; return }
        if (lastName.isEmpty())  { etLastName.error  = "Required"; return }
        if (password.isNotEmpty() && password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }

        val body = mutableMapOf<String, String>(
            "first_name" to firstName,
            "last_name"  to lastName
        )
        if (password.isNotEmpty()) body["password"] = password

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.updateProfile(body)
                if (r.isSuccessful) {
                    // Update cached dentist name
                    val tm = TokenManager.getInstance(this@ProfileActivity)
                    val dentist = tm.getDentist()
                    if (dentist != null) {
                        tm.saveDentist(dentist.copy(firstName = firstName, lastName = lastName))
                    }
                    // Clear password field after success
                    etPassword.setText("")
                    UiUtils.toast(this@ProfileActivity, "Profile updated successfully!")
                } else {
                    UiUtils.toast(this@ProfileActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@ProfileActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }
}
