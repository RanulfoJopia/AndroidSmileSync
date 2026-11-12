package com.example.smilesync.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smilesync.R
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.fragments.ConsultationFragment
import com.example.smilesync.fragments.DashboardFragment
import com.example.smilesync.fragments.PatientsFragment
import com.example.smilesync.fragments.SettingsFragment
import com.example.smilesync.fragments.ThreeDFragment
import com.example.smilesync.utils.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auth guard - redirect to login if no token
        if (!TokenManager.getInstance(this).isLoggedIn()) {
            goToLogin(); return
        }

        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            bottomNav.selectedItemId = R.id.nav_dashboard
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard    -> DashboardFragment()
                R.id.nav_patient      -> PatientsFragment()
                R.id.nav_consultation -> ConsultationFragment()
                R.id.nav_3d           -> ThreeDFragment()
                R.id.nav_settings     -> SettingsFragment()
                else                  -> DashboardFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /** Called by SettingsFragment when user taps Logout */
    fun doLogout() {
        TokenManager.getInstance(this).logout()
        RetrofitClient.resetClient()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}