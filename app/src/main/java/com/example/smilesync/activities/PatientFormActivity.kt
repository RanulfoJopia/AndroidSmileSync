package com.example.smilesync.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.UiUtils
import kotlinx.coroutines.launch

class PatientFormActivity : AppCompatActivity() {

    private var patientId: Int = -1          // -1 = add mode
    private val isEdit get() = patientId != -1

    private lateinit var tvTitle        : TextView
    private lateinit var etFirstName    : EditText
    private lateinit var etMiddleName   : EditText
    private lateinit var etLastName     : EditText
    private lateinit var etNameSuffix   : EditText
    private lateinit var etBirthdate    : EditText
    private lateinit var etAddress      : EditText
    private lateinit var rgSex          : RadioGroup
    private lateinit var rbMale         : RadioButton
    private lateinit var rbFemale       : RadioButton
    private lateinit var btnSave        : Button
    private lateinit var progressBar    : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_form)

        patientId = intent.getIntExtra("patient_id", -1)

        tvTitle       = findViewById(R.id.tv_patient_form_title)
        etFirstName   = findViewById(R.id.et_first_name)
        etMiddleName  = findViewById(R.id.et_middle_name)
        etLastName    = findViewById(R.id.et_last_name)
        etNameSuffix  = findViewById(R.id.et_name_suffix)
        etBirthdate   = findViewById(R.id.et_birthdate)
        etAddress     = findViewById(R.id.et_address)
        rgSex         = findViewById(R.id.rg_sex)
        rbMale        = findViewById(R.id.rb_male)
        rbFemale      = findViewById(R.id.rb_female)
        btnSave       = findViewById(R.id.btn_save_patient)
        progressBar   = findViewById(R.id.patient_form_progress)

        tvTitle.text = if (isEdit) "Edit Patient" else "Add Patient"

        findViewById<View>(R.id.btn_back_form).setOnClickListener { finish() }

        if (isEdit) loadPatient()

        btnSave.setOnClickListener { savePatient() }
    }

    private fun loadPatient() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.api.getPatient(patientId)
                if (r.isSuccessful) {
                    val p = r.body()?.result ?: return@launch
                    etFirstName.setText(p.firstName)
                    etMiddleName.setText(p.middleName ?: "")
                    etLastName.setText(p.lastName)
                    etNameSuffix.setText(p.nameSuffix ?: "")
                    etBirthdate.setText(p.birthdate?.take(10) ?: "")
                    etAddress.setText(p.address ?: "")
                    when (p.sex) {
                        "M" -> rbMale.isChecked = true
                        "F" -> rbFemale.isChecked = true
                    }
                }
            } catch (e: Exception) {
                UiUtils.toast(this@PatientFormActivity, "Failed to load patient")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun savePatient() {
        val firstName = etFirstName.text.toString().trim()
        val lastName  = etLastName.text.toString().trim()
        val birthdate = etBirthdate.text.toString().trim()
        val sex       = when (rgSex.checkedRadioButtonId) {
            R.id.rb_male   -> "M"
            R.id.rb_female -> "F"
            else           -> ""
        }

        if (firstName.isEmpty()) { etFirstName.error = "Required"; return }
        if (lastName.isEmpty())  { etLastName.error  = "Required"; return }
        if (birthdate.isEmpty()) { etBirthdate.error = "Required (YYYY-MM-DD)"; return }
        if (sex.isEmpty())       { UiUtils.toast(this, "Please select sex"); return }

        val body = mapOf(
            "first_name"  to firstName,
            "middle_name" to etMiddleName.text.toString().trim().ifBlank { null },
            "last_name"   to lastName,
            "name_suffix" to etNameSuffix.text.toString().trim().ifBlank { null },
            "birthdate"   to birthdate,
            "sex"         to sex,
            "address"     to etAddress.text.toString().trim().ifBlank { null }
        )

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val r = if (isEdit)
                    RetrofitClient.api.updatePatient(patientId, body)
                else
                    RetrofitClient.api.createPatient(body)

                if (r.isSuccessful) {
                    UiUtils.toast(this@PatientFormActivity,
                        if (isEdit) "Patient updated!" else "Patient added!")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    UiUtils.toast(this@PatientFormActivity, UiUtils.errorMessage(r))
                }
            } catch (e: Exception) {
                UiUtils.toast(this@PatientFormActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }
}
