package com.example.smilesync.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.smilesync.R

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etResetEmail: EditText
    private lateinit var btnSendReset: AppCompatButton
    private lateinit var btnBack: LinearLayout
    private lateinit var tvBackToLogin: TextView
    private lateinit var tvResend: TextView
    private lateinit var layoutFormCard: LinearLayout
    private lateinit var layoutSuccess: LinearLayout
    private lateinit var tvSuccessEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etResetEmail    = findViewById(R.id.etResetEmail)
        btnSendReset    = findViewById(R.id.btnSendReset)
        btnBack         = findViewById(R.id.btn_back)
        tvBackToLogin   = findViewById(R.id.tvBackToLogin)
        tvResend        = findViewById(R.id.tv_resend)
        layoutSuccess   = findViewById(R.id.layout_success)
        tvSuccessEmail  = findViewById(R.id.tv_success_email)

        // The card that holds the email field + button is the parent of btnSendReset —
        // grab it by traversing: btnSendReset.parent gives us the bg_card_glass LinearLayout
        layoutFormCard  = btnSendReset.parent as LinearLayout
    }

    private fun setupClickListeners() {

        // Back arrow at top → finish this activity
        btnBack.setOnClickListener {
            finish()
        }

        // "Sign In" link at bottom → go back to login
        tvBackToLogin.setOnClickListener {
            navigateToLogin()
        }

        // Send reset link
        btnSendReset.setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            if (email.isEmpty()) {
                etResetEmail.error = "Email address is required"
                etResetEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.error = "Enter a valid email address"
                etResetEmail.requestFocus()
                return@setOnClickListener
            }

            // TODO: Connect to backend password-reset API
            showSuccessState(email)
        }

        // Resend link inside the success card
        tvResend.setOnClickListener {
            val email = etResetEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                Toast.makeText(this, "Reset link resent to $email", Toast.LENGTH_SHORT).show()
            } else {
                // Edge case: user cleared the field before resending
                showFormState()
            }
        }
    }

    /** Hides the form card and shows the success card */
    private fun showSuccessState(email: String) {
        tvSuccessEmail.text = "A reset link has been sent to\n$email"
        layoutFormCard.visibility = View.GONE
        layoutSuccess.visibility  = View.VISIBLE
    }

    /** Brings the form card back (used if resend tapped without an email) */
    private fun showFormState() {
        layoutFormCard.visibility = View.VISIBLE
        layoutSuccess.visibility  = View.GONE
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    /** Let the hardware back button also just go back to login */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        finish()
    }
}
