package com.obsidian.aegis.ui.onboarding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.obsidian.aegis.ui.home.HomeActivity

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isOnboardingCompleted()) {
            startHomeActivity()
            finish()
            return
        }

        setContent {
            OnboardingScreen(
                onFinished = {
                    setOnboardingCompleted()
                    startHomeActivity()
                    finish()
                }
            )
        }
    }

    private fun isOnboardingCompleted(): Boolean {
        val sharedPref = getSharedPreferences("aegis_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("onboarding_completed", false)
    }

    private fun setOnboardingCompleted() {
        val sharedPref = getSharedPreferences("aegis_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("onboarding_completed", true)
            apply()
        }
    }

    private fun startHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
    }
}
