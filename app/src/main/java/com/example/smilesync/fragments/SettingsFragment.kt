package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smilesync.R
import com.example.smilesync.activities.MainActivity
import com.example.smilesync.activities.ProfileActivity
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateDentistInfo(view)

        // Edit Profile → ProfileActivity
        view.findViewById<LinearLayout>(R.id.item_edit_profile)?.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        // Coming soon stubs
        val stubs = listOf(
            R.id.item_notifications to "Notifications",
            R.id.item_privacy       to "Privacy & Security",
            R.id.item_help          to "Help & Support",
            R.id.item_about         to "About SmileSync"
        )
        stubs.forEach { (id, label) ->
            view.findViewById<LinearLayout>(id)?.setOnClickListener {
                UiUtils.toast(requireContext(), "$label — coming soon")
            }
        }

        // Logout
        view.findViewById<LinearLayout>(R.id.item_logout)?.setOnClickListener {
            UiUtils.confirm(requireContext(), "Logout", "Are you sure you want to log out?") {
                (activity as? MainActivity)?.doLogout()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh name after returning from ProfileActivity
        view?.let { populateDentistInfo(it) }
    }

    private fun populateDentistInfo(view: View) {
        val dentist = TokenManager.getInstance(requireContext()).getDentist()
        view.findViewById<TextView>(R.id.tv_settings_doctor_name)?.text =
            dentist?.fullName?.ifBlank { "Dr. —" } ?: "Dr. —"
        view.findViewById<TextView>(R.id.tv_settings_doctor_email)?.text =
            dentist?.email ?: "—"
    }
}
