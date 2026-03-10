package com.obsidian.aegis.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import com.obsidian.aegis.BuildConfig
import com.obsidian.aegis.databinding.ActivityHomeBinding
import com.obsidian.aegis.databinding.ContentServiceEnabledBinding
import com.obsidian.aegis.helpers.goToActivity
import com.obsidian.aegis.helpers.openBrowser
import com.obsidian.aegis.helpers.openSharingScreen
import com.obsidian.aegis.helpers.setViewTint
import com.obsidian.aegis.services.IndicatorService
import com.obsidian.aegis.repository.SharedPrefManager
import com.obsidian.aegis.ui.logs.AccessLogsActivity
import com.obsidian.aegis.ui.logs.SuspiciousLogsActivity


class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    lateinit var serviceEnabledBinding: ContentServiceEnabledBinding

    lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serviceEnabledBinding = binding.contentServiceEnabled

        val viewModelProviderFactory = HomeViewModelProviderFactory(application, SharedPrefManager.getInstance(applicationContext))
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(HomeViewModel::class.java)

        setUpObservers()
        setUpListeners()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
            }
        }
    }

    private fun setUpObservers() {
        viewModel.cameraIndicatorStatus.observe(this, {
            serviceEnabledBinding.switchCamera.isChecked = it
            binding.indicatorsLayout.ivCam.visibility = if(it==true) View.VISIBLE else View.GONE
        })

        viewModel.microphoneIndicatorStatus.observe(this, {
            serviceEnabledBinding.switchMic.isChecked = it
            binding.indicatorsLayout.ivMic.visibility = if(it==true) View.VISIBLE else View.GONE
        })

        viewModel.locationIndicatorStatus.observe(this, {
            serviceEnabledBinding.switchLocation.isChecked = it
            binding.indicatorsLayout.ivLoc.visibility = if(it==true) View.VISIBLE else View.GONE
        })

        viewModel.suspiciousDetectionStatus.observe(this, {
            serviceEnabledBinding.switchSuspicious.isChecked = it
        })

        viewModel.screenOffMonitoringStatus.observe(this, {
            serviceEnabledBinding.switchScreenOff.isChecked = it
        })

        viewModel.showAppInfoOnIndicatorStatus.observe(this, {
            serviceEnabledBinding.switchAppInfo.isChecked = it
        })

        viewModel.autoRevokeEnabledStatus.observe(this, {
            serviceEnabledBinding.switchAutoRevoke.isChecked = it
        })

        viewModel.privacyHealthScore.observe(this, { score ->
            serviceEnabledBinding.tvHealthScoreValue.text = score.toString()
            serviceEnabledBinding.cpHealthScore.progress = score
            when {
                score >= 85 -> {
                    serviceEnabledBinding.tvHealthScoreValue.setTextColor(Color.GREEN)
                    serviceEnabledBinding.tvHealthScoreDesc.text = "Overall privacy is Excellent"
                }
                score >= 70 -> {
                    serviceEnabledBinding.tvHealthScoreValue.setTextColor(Color.YELLOW)
                    serviceEnabledBinding.tvHealthScoreDesc.text = "Overall privacy is Fair"
                }
                else -> {
                    serviceEnabledBinding.tvHealthScoreValue.setTextColor(Color.RED)
                    serviceEnabledBinding.tvHealthScoreDesc.text = "Overall privacy is Poor"
                }
            }
        })

        viewModel.vibrationAlertStatus.observe(this, {
            serviceEnabledBinding.switchVibration.isChecked = it
        })

        viewModel.notificationAlertStatus.observe(this, {
            serviceEnabledBinding.switchNotification.isChecked = it
        })

        viewModel.indicatorBackgroundColor.observe(this, {
            binding.indicatorsLayout.llBackground.setBackgroundColor(Color.parseColor(it))
        })

        viewModel.indicatorForegroundColor.observe(this, {
            binding.indicatorsLayout.ivCam.setViewTint(viewModel.sharedPrefManager.cameraColor)
            binding.indicatorsLayout.ivMic.setViewTint(viewModel.sharedPrefManager.micColor)
            binding.indicatorsLayout.ivLoc.setViewTint(viewModel.sharedPrefManager.locationColor)
        })
    }

    private fun setUpListeners() {
        binding.mainSwitch.setOnCheckedChangeListener { button, isEnabled ->
            if(isEnabled){
               if(isAccessibilityServiceEnabled(applicationContext)) 
                   serviceEnabled()
                else
                    openAccessibilitySettingsPage(isEnabled)
            }else {
                if(!isAccessibilityServiceEnabled(applicationContext))
                    serviceDisabled()
                else
                    openAccessibilitySettingsPage(isEnabled)
            }
        }

        serviceEnabledBinding.switchCamera.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setCameraIndicatorStatus(isEnabled)
        }

        serviceEnabledBinding.switchMic.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setMicrophoneIndicatorStatus(isEnabled)
        }

        serviceEnabledBinding.switchLocation.setOnCheckedChangeListener { button, isEnabled ->
            if (isEnabled) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    button.isChecked = false
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        101
                    )
                } else {
                    viewModel.setLocationIndicatorStatus(true)
                }
            } else {
                viewModel.setLocationIndicatorStatus(false)
            }
        }

        serviceEnabledBinding.switchVibration.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setVibrationAlertStatus(isEnabled)
        }

        serviceEnabledBinding.switchNotification.setOnCheckedChangeListener { button, isEnabled ->
            if (isEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    button.isChecked = false
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        102
                    )
                } else {
                    viewModel.setNotificationAlertStatus(true)
                }
            } else {
                viewModel.setNotificationAlertStatus(false)
            }
        }

        serviceEnabledBinding.settingsText.setOnClickListener {
            openCustomizationScreen()
        }

        serviceEnabledBinding.switchSettings.setOnClickListener {
            openCustomizationScreen()
        }

        serviceEnabledBinding.logsText.setOnClickListener {
            openAccessLogsScreen()
        }

        serviceEnabledBinding.whitelistText.setOnClickListener {
            val intent = Intent(this, com.obsidian.aegis.ui.whitelist.WhitelistActivity::class.java)
            startActivity(intent)
        }

        serviceEnabledBinding.switchLogs.setOnClickListener {
            openAccessLogsScreen()
        }

        serviceEnabledBinding.switchSuspicious.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setSuspiciousDetectionStatus(isEnabled)
        }

        serviceEnabledBinding.switchScreenOff.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setScreenOffMonitoringStatus(isEnabled)
        }

        serviceEnabledBinding.switchAppInfo.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setShowAppInfoOnIndicatorStatus(isEnabled)
        }

        serviceEnabledBinding.switchAutoRevoke.setOnCheckedChangeListener { button, isEnabled ->
            viewModel.setAutoRevokeEnabledStatus(isEnabled)
        }

        serviceEnabledBinding.suspiciousText.setOnClickListener {
            openSuspiciousLogsScreen()
        }

        serviceEnabledBinding.switchSuspiciousLogs.setOnClickListener {
            openSuspiciousLogsScreen()
        }

        serviceEnabledBinding.riskText.setOnClickListener {
            openRiskAnalysisScreen()
        }

        serviceEnabledBinding.sensorText.setOnClickListener {
            openSensorAnalysisScreen()
        }


//        serviceEnabledBinding.shareText.setOnClickListener {
//            this.openSharingScreen(HomeViewModel.SHARING_TEXT)
//        }

        serviceEnabledBinding.issueText.setOnClickListener {
            this.openBrowser(HomeViewModel.GITHUB_REPO)
        }

        binding.contentCredits.imgGithub.setOnClickListener {
            this.openBrowser(HomeViewModel.GITHUB_PROFILE)
        }

        binding.contentCredits.imgLinkedIn.setOnClickListener {
            this.openBrowser(HomeViewModel.LINKEDIN)
        }

        binding.contentCredits.tvHackSecure.setOnClickListener {
            this.openBrowser(HomeViewModel.HACKSECURE)
        }
    }

    private fun openAccessibilitySettingsPage(isServiceDisabled:Boolean) {
        if (isServiceDisabled){
            Toast.makeText(this, "Turn On Aegis", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this, "Turn Off Aegis", Toast.LENGTH_LONG).show()
        }
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openCustomizationScreen() {
        val customizationFragment = CustomizationFragment()
        supportFragmentManager.beginTransaction().apply {
            replace(binding.homeFragmentContainer.id, customizationFragment)
            addToBackStack("CUSTOMIZATION_SCREEN")
            commit()
        }
    }

    private fun openAccessLogsScreen() {
        this.goToActivity(AccessLogsActivity::class.java)
    }

    private fun openSuspiciousLogsScreen() {
        this.goToActivity(SuspiciousLogsActivity::class.java)
    }

    private fun openRiskAnalysisScreen() {
        this.goToActivity(com.obsidian.aegis.ui.risk.RiskActivity::class.java)
    }

    private fun openSensorAnalysisScreen() {
        this.goToActivity(com.obsidian.aegis.ui.risk.SensorAnalysisActivity::class.java)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                serviceEnabledBinding.switchLocation.isChecked = true
                viewModel.setLocationIndicatorStatus(true)
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
                serviceEnabledBinding.switchLocation.isChecked = false
            }
        } else if (requestCode == 102) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                serviceEnabledBinding.switchNotification.isChecked = true
                viewModel.setNotificationAlertStatus(true)
            } else {
                Toast.makeText(this, "Notification permission is required for alerts", Toast.LENGTH_LONG).show()
                serviceEnabledBinding.switchNotification.isChecked = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityServiceEnabled(applicationContext)) serviceEnabled() else serviceDisabled()
    }
    private fun isAccessibilityServiceEnabled(mContext: Context): Boolean {
        if(BuildConfig.IN_APP_TESTING_TOGGLE) return true
        val APPLICATION_ID = BuildConfig.APPLICATION_ID
        val ACCESSIBILITY_SERVICE = IndicatorService::class.java.canonicalName
        var accessibilityEnabled = 0
        val service = "$APPLICATION_ID/$ACCESSIBILITY_SERVICE"
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: SettingNotFoundException) {

        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun serviceEnabled() {
        binding.mainSwitch.isChecked = true
        binding.mainSwitch.text = "Enabled"
        binding.contentServiceDisabled.root.visibility = View.GONE
        serviceEnabledBinding.root.visibility = View.VISIBLE
        binding.contentCredits.root.visibility = View.VISIBLE
        binding.indicatorsLayout.root.visibility = View.VISIBLE
    }

    private fun serviceDisabled() {
        binding.mainSwitch.isChecked = false
        binding.mainSwitch.text = "Disabled"
        binding.contentServiceDisabled.root.visibility = View.VISIBLE
        serviceEnabledBinding.root.visibility = View.GONE
        binding.contentCredits.root.visibility = View.GONE
        binding.indicatorsLayout.root.visibility = View.GONE
    }
}