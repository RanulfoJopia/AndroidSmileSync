package com.example.smilesync.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.UiUtils
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PatientFormActivity : AppCompatActivity() {

    private var patientId: Int = -1
    private val isEdit get() = patientId != -1

    private lateinit var tvTitle: TextView
    private lateinit var etFirstName: EditText
    private lateinit var etMiddleName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etNameSuffix: EditText
    private lateinit var etBirthdate: TextView
    private lateinit var cvBirthdate: MaterialCardView
    private lateinit var etAddress: EditText
    private lateinit var rgSex: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var cvSex: MaterialCardView
    private lateinit var tvSex: TextView
    private lateinit var cvProvince: MaterialCardView
    private lateinit var tvProvince: TextView
    private lateinit var cvCity: MaterialCardView
    private lateinit var tvCity: TextView
    private lateinit var cvBarangay: MaterialCardView
    private lateinit var tvBarangay: TextView
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private var selectedDate: String = ""
    private var selectedSex: String = ""
    private var selectedProvince: String = ""
    private var selectedProvinceCode: String = ""
    private var selectedCity: String = ""
    private var selectedCityCode: String = ""
    private var selectedBarangay: String = ""

    // Cache for address data
    private var provinces: List<AddressItem> = emptyList()
    private var cities: List<AddressItem> = emptyList()
    private var barangays: List<AddressItem> = emptyList()

    data class AddressItem(val code: String, val name: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_form)

        patientId = intent.getIntExtra("patient_id", -1)

        tvTitle = findViewById(R.id.tv_patient_form_title)
        etFirstName = findViewById(R.id.et_first_name)
        etMiddleName = findViewById(R.id.et_middle_name)
        etLastName = findViewById(R.id.et_last_name)
        etNameSuffix = findViewById(R.id.et_name_suffix)
        etBirthdate = findViewById(R.id.et_birthdate)
        cvBirthdate = findViewById(R.id.cv_birthdate)
        etAddress = findViewById(R.id.et_address)
        rgSex = findViewById(R.id.rg_sex)
        rbMale = findViewById(R.id.rb_male)
        rbFemale = findViewById(R.id.rb_female)
        cvSex = findViewById(R.id.cv_sex)
        tvSex = findViewById(R.id.tv_sex)
        cvProvince = findViewById(R.id.cv_province)
        tvProvince = findViewById(R.id.tv_province)
        cvCity = findViewById(R.id.cv_city)
        tvCity = findViewById(R.id.tv_city)
        cvBarangay = findViewById(R.id.cv_barangay)
        tvBarangay = findViewById(R.id.tv_barangay)
        btnSave = findViewById(R.id.btn_save_patient)
        progressBar = findViewById(R.id.patient_form_progress)

        tvTitle.text = if (isEdit) "Edit Patient" else "Add Patient"

        findViewById<View>(R.id.btn_back_form).setOnClickListener { finish() }

        // Setup click listeners
        cvBirthdate.setOnClickListener { showDatePicker() }
        cvSex.setOnClickListener { showSexDialog() }
        cvProvince.setOnClickListener { 
            if (provinces.isEmpty()) loadProvinces()
            else showProvinceDialog()
        }
        cvCity.setOnClickListener { showCityDialog() }
        cvBarangay.setOnClickListener { showBarangayDialog() }

        // Load provinces on start
        loadProvinces()

        if (isEdit) loadPatient()

        btnSave.setOnClickListener { savePatient() }
    }

    private fun loadProvinces() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                
                provinces = withContext(Dispatchers.IO) {
                    // Using PSGC API - Philippines Standard Geographic Code
                    val url = "https://psgc.gitlab.io/api/provinces/"
                    val json = URL(url).readText()
                    val jsonArray = JSONArray(json)
                    
                    val list = mutableListOf<AddressItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val code = obj.getString("code")
                        val name = obj.getString("name")
                        list.add(AddressItem(code, name))
                    }
                    list.sortedBy { it.name }
                }
                
                progressBar.visibility = View.GONE
                
                if (provinces.isEmpty()) {
                    UiUtils.toast(this@PatientFormActivity, "Failed to load provinces")
                }
                
            } catch (e: Exception) {
                Log.e("PatientForm", "Error loading provinces", e)
                progressBar.visibility = View.GONE
                UiUtils.toast(this@PatientFormActivity, "Error loading provinces: ${e.message}")
            }
        }
    }

    private fun loadCities(provinceCode: String) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                
                cities = withContext(Dispatchers.IO) {
                    // Load cities/municipalities for the selected province
                    val url = "https://psgc.gitlab.io/api/provinces/$provinceCode/cities-municipalities/"
                    val json = URL(url).readText()
                    val jsonArray = JSONArray(json)
                    
                    val list = mutableListOf<AddressItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val code = obj.getString("code")
                        val name = obj.getString("name")
                        list.add(AddressItem(code, name))
                    }
                    list.sortedBy { it.name }
                }
                
                progressBar.visibility = View.GONE
                showCityDialog()
                
            } catch (e: Exception) {
                Log.e("PatientForm", "Error loading cities", e)
                progressBar.visibility = View.GONE
                UiUtils.toast(this@PatientFormActivity, "Error loading cities: ${e.message}")
            }
        }
    }

    private fun loadBarangays(cityCode: String) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                
                barangays = withContext(Dispatchers.IO) {
                    // Load barangays for the selected city
                    val url = "https://psgc.gitlab.io/api/cities-municipalities/$cityCode/barangays/"
                    val json = URL(url).readText()
                    val jsonArray = JSONArray(json)
                    
                    val list = mutableListOf<AddressItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val code = obj.getString("code")
                        val name = obj.getString("name")
                        list.add(AddressItem(code, name))
                    }
                    list.sortedBy { it.name }
                }
                
                progressBar.visibility = View.GONE
                showBarangayDialog()
                
            } catch (e: Exception) {
                Log.e("PatientForm", "Error loading barangays", e)
                progressBar.visibility = View.GONE
                UiUtils.toast(this@PatientFormActivity, "Error loading barangays: ${e.message}")
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                calendar.time = sdf.parse(selectedDate) ?: Date()
            } catch (e: Exception) {
                // Use current date
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                etBirthdate.text = selectedDate
                etBirthdate.setTextColor(getColor(android.R.color.black))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showSexDialog() {
        val options = arrayOf("Male", "Female")
        AlertDialog.Builder(this)
            .setTitle("Select Sex")
            .setItems(options) { dialog, which ->
                selectedSex = if (which == 0) "M" else "F"
                tvSex.text = options[which]
                tvSex.setTextColor(getColor(android.R.color.black))
                
                if (which == 0) rbMale.isChecked = true else rbFemale.isChecked = true
                
                dialog.dismiss()
            }
            .show()
    }

    private fun showProvinceDialog() {
        if (provinces.isEmpty()) {
            UiUtils.toast(this, "Loading provinces...")
            return
        }

        val provinceNames = provinces.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select Province")
            .setItems(provinceNames) { dialog, which ->
                val selected = provinces[which]
                selectedProvince = selected.name
                selectedProvinceCode = selected.code
                tvProvince.text = selectedProvince
                tvProvince.setTextColor(getColor(android.R.color.black))
                
                // Reset city and barangay
                selectedCity = ""
                selectedCityCode = ""
                selectedBarangay = ""
                cities = emptyList()
                barangays = emptyList()
                tvCity.text = "Select city"
                tvCity.setTextColor(getColor(android.R.color.darker_gray))
                tvBarangay.text = "Select barangay"
                tvBarangay.setTextColor(getColor(android.R.color.darker_gray))
                
                // Load cities for this province
                loadCities(selectedProvinceCode)
                
                dialog.dismiss()
            }
            .show()
    }

    private fun showCityDialog() {
        if (selectedProvinceCode.isEmpty()) {
            UiUtils.toast(this, "Please select province first")
            return
        }

        if (cities.isEmpty()) {
            UiUtils.toast(this, "Loading cities...")
            loadCities(selectedProvinceCode)
            return
        }

        val cityNames = cities.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select City/Municipality")
            .setItems(cityNames) { dialog, which ->
                val selected = cities[which]
                selectedCity = selected.name
                selectedCityCode = selected.code
                tvCity.text = selectedCity
                tvCity.setTextColor(getColor(android.R.color.black))
                
                // Reset barangay
                selectedBarangay = ""
                barangays = emptyList()
                tvBarangay.text = "Select barangay"
                tvBarangay.setTextColor(getColor(android.R.color.darker_gray))
                
                // Load barangays for this city
                loadBarangays(selectedCityCode)
                
                dialog.dismiss()
            }
            .show()
    }

    private fun showBarangayDialog() {
        if (selectedCityCode.isEmpty()) {
            UiUtils.toast(this, "Please select city first")
            return
        }

        if (barangays.isEmpty()) {
            UiUtils.toast(this, "Loading barangays...")
            loadBarangays(selectedCityCode)
            return
        }

        val barangayNames = barangays.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select Barangay")
            .setItems(barangayNames) { dialog, which ->
                selectedBarangay = barangayNames[which]
                tvBarangay.text = selectedBarangay
                tvBarangay.setTextColor(getColor(android.R.color.black))
                dialog.dismiss()
            }
            .show()
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
                    
                    val birthdate = p.birthdate?.take(10) ?: ""
                    if (birthdate.isNotEmpty()) {
                        selectedDate = birthdate
                        etBirthdate.text = birthdate
                        etBirthdate.setTextColor(getColor(android.R.color.black))
                    }
                    
                    selectedSex = p.sex ?: ""
                    when (p.sex) {
                        "M" -> {
                            rbMale.isChecked = true
                            tvSex.text = "Male"
                            tvSex.setTextColor(getColor(android.R.color.black))
                        }
                        "F" -> {
                            rbFemale.isChecked = true
                            tvSex.text = "Female"
                            tvSex.setTextColor(getColor(android.R.color.black))
                        }
                    }
                    
                    val address = p.address ?: ""
                    if (address.isNotEmpty()) {
                        val parts = address.split(",").map { it.trim() }
                        if (parts.size >= 3) {
                            selectedBarangay = parts[0]
                            selectedCity = parts[1]
                            selectedProvince = parts[2]
                            tvProvince.text = selectedProvince
                            tvProvince.setTextColor(getColor(android.R.color.black))
                            tvCity.text = selectedCity
                            tvCity.setTextColor(getColor(android.R.color.black))
                            tvBarangay.text = selectedBarangay
                            tvBarangay.setTextColor(getColor(android.R.color.black))
                        }
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
        val lastName = etLastName.text.toString().trim()

        if (firstName.isEmpty()) {
            etFirstName.error = "Required"
            return
        }
        if (lastName.isEmpty()) {
            etLastName.error = "Required"
            return
        }
        if (selectedDate.isEmpty()) {
            UiUtils.toast(this, "Please select birthdate")
            return
        }
        if (selectedSex.isEmpty()) {
            UiUtils.toast(this, "Please select sex")
            return
        }

        val addressParts = mutableListOf<String>()
        if (selectedBarangay.isNotEmpty()) addressParts.add(selectedBarangay)
        if (selectedCity.isNotEmpty()) addressParts.add(selectedCity)
        if (selectedProvince.isNotEmpty()) addressParts.add(selectedProvince)
        val address = addressParts.joinToString(", ")

        val body = mapOf(
            "first_name" to firstName,
            "middle_name" to etMiddleName.text.toString().trim().ifBlank { null },
            "last_name" to lastName,
            "name_suffix" to etNameSuffix.text.toString().trim().ifBlank { null },
            "birthdate" to selectedDate,
            "sex" to selectedSex,
            "address" to address.ifBlank { null }
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
                    UiUtils.toast(
                        this@PatientFormActivity,
                        if (isEdit) "Patient updated!" else "Patient added!"
                    )
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