package com.example.smilesync.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.smilesync.R
import com.example.smilesync.utils.TokenManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashDuration = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNext()
        }, splashDuration)
    }

    private fun navigateToNext() {
        val intent = if (TokenManager.getInstance(this).isLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LandingActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
}
