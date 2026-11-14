package com.example.smilesync.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smilesync.R
import com.example.smilesync.activities.MainActivity
import com.example.smilesync.activities.StripeCheckoutActivity
import com.example.smilesync.api.RetrofitClient
import com.example.smilesync.utils.TokenManager
import com.example.smilesync.utils.UiUtils
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private var subscriptionData: Map<String, Any?>? = null
    private val REQUEST_SUBSCRIPTION = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateProfileInfo(view)
        setupListeners(view)
        fetchSubscriptionStatus()
    }

    override fun onResume() {
        super.onResume()
        view?.let { 
            populateProfileInfo(it)
            fetchSubscriptionStatus()
        }
    }

    private fun populateProfileInfo(view: View) {
        val dentist = TokenManager.getInstance(requireContext()).getDentist()
        val fullName = dentist?.fullName?.ifBlank { "Dr. User" } ?: "Dr. User"
        val initials = getInitials(fullName)

        view.findViewById<TextView>(R.id.tv_profile_name)?.text = fullName
        view.findViewById<TextView>(R.id.tv_profile_avatar_initials)?.text = initials
        view.findViewById<TextView>(R.id.tv_profile_avatar_large)?.text = initials
    }

    private fun getInitials(fullName: String): String {
        val parts = fullName.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            parts.size == 1 -> "${parts[0].take(2).uppercase()}"
            else -> "DR"
        }
    }

    private fun setupListeners(view: View) {
        // Profile avatar click listener for logout
        view.findViewById<MaterialCardView>(R.id.cv_profile_avatar_header)?.setOnClickListener {
            showLogoutMenu(it)
        }

        view.findViewById<Button>(R.id.btn_save_changes)?.setOnClickListener {
            val newPassword = view.findViewById<EditText>(R.id.et_new_password)?.text.toString()
            if (newPassword.isNotBlank()) {
                UiUtils.toast(requireContext(), "Password change API not implemented yet")
            } else {
                UiUtils.toast(requireContext(), "Enter a new password to change it")
            }
        }

        view.findViewById<Button>(R.id.btn_subscribe_now)?.setOnClickListener {
            openSubscriptionWebView()
        }

        view.findViewById<MaterialCardView>(R.id.card_subscription)?.setOnClickListener {
            openSubscriptionWebView()
        }

        view.findViewById<MaterialCardView>(R.id.card_payment_method)?.setOnClickListener {
            openSubscriptionWebView()
        }

        view.findViewById<Button>(R.id.btn_add_payment_method)?.setOnClickListener {
            openSubscriptionWebView()
        }

        view.findViewById<Button>(R.id.btn_cancel_subscription)?.setOnClickListener {
            handleCancelSubscription()
        }

        view.findViewById<MaterialCardView>(R.id.card_payment_history)?.setOnClickListener {
            openSubscriptionWebView()
        }
    }

    private fun showLogoutMenu(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    handleLogout()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun handleLogout() {
        UiUtils.confirm(
            requireContext(),
            "Logout",
            "Are you sure you want to log out?"
        ) {
            (activity as? MainActivity)?.doLogout()
        }
    }

    private fun fetchSubscriptionStatus() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getSubscriptionStatus()

                if (response.isSuccessful && response.body()?.result != null) {
                    subscriptionData = response.body()?.result
                    view?.let { updateSubscriptionUI(it) }
                } else {
                    // Show default subscription UI if endpoint not available
                    Log.d("SettingsFragment", "Subscription endpoint not available, showing default UI")
                    view?.let { showDefaultSubscriptionUI(it) }
                }
            } catch (e: Exception) {
                // Show default subscription UI on error
                Log.d("SettingsFragment", "Subscription API error, showing default UI")
                view?.let { showDefaultSubscriptionUI(it) }
            }
        }
    }

    private fun showDefaultSubscriptionUI(view: View) {
        // Show subscription section with default values when API is not available
        view.findViewById<MaterialCardView>(R.id.card_subscription)?.visibility = View.VISIBLE
        view.findViewById<MaterialCardView>(R.id.card_payment_method)?.visibility = View.VISIBLE
        view.findViewById<MaterialCardView>(R.id.card_cancel_subscription)?.visibility = View.GONE
        view.findViewById<MaterialCardView>(R.id.card_payment_history)?.visibility = View.GONE
        
        // Set default values
        view.findViewById<TextView>(R.id.tv_subscription_badge)?.visibility = View.GONE
        view.findViewById<TextView>(R.id.tv_subscription_price)?.text = "$29.00"
        view.findViewById<TextView>(R.id.tv_trial_expiration)?.text = "N/A"
        view.findViewById<TextView>(R.id.tv_access_status)?.apply {
            text = "Full Access (Read/Write)"
            setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        }
        view.findViewById<Button>(R.id.btn_subscribe_now)?.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.tv_payment_method_status)?.text = 
            "No payment method on file. Add a card for automated subscription renewals."
    }

    private fun hideSubscriptionSection(view: View) {
        // Hide subscription cards if backend endpoint is not implemented
        view.findViewById<MaterialCardView>(R.id.card_subscription)?.visibility = View.GONE
        view.findViewById<MaterialCardView>(R.id.card_payment_method)?.visibility = View.GONE
        view.findViewById<MaterialCardView>(R.id.card_cancel_subscription)?.visibility = View.GONE
        view.findViewById<MaterialCardView>(R.id.card_payment_history)?.visibility = View.GONE
    }

    private fun updateSubscriptionUI(view: View) {
        val data = subscriptionData ?: return

        val status = data["status"] as? String ?: "inactive"
        val planPrice = (data["plan_price"] as? Number)?.toDouble() ?: 29.0
        val trialEnd = data["trial_end"] as? String
        val isActive = data["is_active"] as? Boolean ?: false
        val hasPaymentMethod = data["has_payment_method"] as? Boolean ?: false
        val accessType = data["access_type"] as? String ?: "read_only"

        val badge = when (status) {
            "trialing" -> "Free Trial"
            "active" -> "Active"
            "canceled" -> "Canceled"
            "past_due" -> "Past Due"
            else -> "Inactive"
        }
        val badgeColor = when (status) {
            "trialing", "active" -> "#4CAF50"
            "canceled" -> "#FF9800"
            "past_due" -> "#F44336"
            else -> "#9E9E9E"
        }

        view.findViewById<TextView>(R.id.tv_subscription_badge)?.apply {
            text = badge
            setTextColor(android.graphics.Color.parseColor(badgeColor))
        }

        view.findViewById<TextView>(R.id.tv_subscription_price)?.text = "$${"%.2f".format(planPrice)}"

        view.findViewById<TextView>(R.id.tv_trial_expiration)?.text = 
            trialEnd?.let { formatDate(it) } ?: "N/A"

        val accessText = when (accessType) {
            "full" -> "Full Access (Read/Write)"
            "read_only" -> "Read Only (View Only)"
            else -> "No Access"
        }
        val accessColor = when (accessType) {
            "full" -> "#4CAF50"
            "read_only" -> "#FF9800"
            else -> "#F44336"
        }
        view.findViewById<TextView>(R.id.tv_access_status)?.apply {
            text = accessText
            setTextColor(android.graphics.Color.parseColor(accessColor))
        }

        val paymentMethodText = if (hasPaymentMethod) {
            "Payment method on file. Your subscription will auto-renew."
        } else {
            "No payment method on file. Add a card for automated subscription renewals."
        }
        view.findViewById<TextView>(R.id.tv_payment_method_status)?.text = paymentMethodText

        view.findViewById<Button>(R.id.btn_subscribe_now)?.visibility = 
            if (isActive) View.GONE else View.VISIBLE

        view.findViewById<MaterialCardView>(R.id.card_cancel_subscription)?.visibility =
            if (isActive && status != "canceled") View.VISIBLE else View.GONE
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun openSubscriptionWebView() {
        lifecycleScope.launch {
            try {
                val tokenResponse = RetrofitClient.api.generateWebViewToken(
                    purpose = "subscription", 
                    patientId = null
                )
                
                if (tokenResponse.isSuccessful && tokenResponse.body()?.result != null) {
                    val webToken = tokenResponse.body()?.result?.token
                    val baseUrl = "http://35.212.220.242"
                    val url = "$baseUrl/mobile/webview?token=$webToken"
                    
                    val intent = Intent(requireContext(), StripeCheckoutActivity::class.java)
                    intent.putExtra(StripeCheckoutActivity.EXTRA_CHECKOUT_URL, url)
                    startActivityForResult(intent, REQUEST_SUBSCRIPTION)
                } else {
                    val errorMsg = tokenResponse.body()?.error?.message ?: "Unknown error"
                    Log.e("SettingsFragment", "Token generation failed: $errorMsg")
                    UiUtils.toast(requireContext(), "Unable to open subscription page")
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error opening subscription", e)
                UiUtils.toast(requireContext(), "Error: ${e.message}")
            }
        }
    }

    private fun handleCancelSubscription() {
        UiUtils.confirm(
            requireContext(),
            "Cancel Subscription",
            "Are you sure you want to cancel your subscription? You will still have access until the end of your billing period."
        ) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.cancelSubscription()

                    if (response.isSuccessful && response.body()?.result != null) {
                        UiUtils.toast(requireContext(), "Subscription canceled successfully")
                        fetchSubscriptionStatus()
                    } else {
                        val errorMsg = response.body()?.error?.message ?: "Unknown error"
                        Log.e("SettingsFragment", "Cancel failed: $errorMsg")
                        UiUtils.toast(requireContext(), "Failed to cancel subscription")
                    }
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Error canceling subscription", e)
                    UiUtils.toast(requireContext(), "Error: ${e.message}")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SUBSCRIPTION) {
            fetchSubscriptionStatus()
        }
    }
}

