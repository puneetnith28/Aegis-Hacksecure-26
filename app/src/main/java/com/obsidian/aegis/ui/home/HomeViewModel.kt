package com.obsidian.aegis.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.obsidian.aegis.models.IndicatorOpacity
import com.obsidian.aegis.models.IndicatorPosition
import com.obsidian.aegis.models.IndicatorSize
import com.obsidian.aegis.repository.SharedPrefManager
import androidx.lifecycle.viewModelScope
import com.obsidian.aegis.db.AccessLogsDatabase
import com.obsidian.aegis.repository.AccessLogsRepo
import kotlinx.coroutines.launch

class HomeViewModel(application: Application,
                    val sharedPrefManager: SharedPrefManager) : AndroidViewModel(application) {

    private val repo = AccessLogsRepo(AccessLogsDatabase(application))

    val privacyHealthScore = MutableLiveData(100)
    
    init {
        calculatePrivacyHealthScore()
    }

    private fun calculatePrivacyHealthScore() {
        viewModelScope.launch {
            val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000 // Last 24 hours
            val activities = repo.fetchRecentSuspiciousActivities(since)
            var score = 100
            for (activity in activities) {
                when (activity.riskLevel) {
                    "Critical" -> score -= 15
                    "Tracking" -> score -= 15
                    "High" -> score -= 10
                    "Medium" -> score -= 5
                    else -> score -= 2
                }
            }
            if (score < 0) score = 0
            privacyHealthScore.value = score
        }
    }

    val cameraIndicatorStatus = MutableLiveData(sharedPrefManager.isCameraIndicatorEnabled)

    val microphoneIndicatorStatus = MutableLiveData(sharedPrefManager.isMicIndicatorEnabled)

    val locationIndicatorStatus = MutableLiveData(sharedPrefManager.isLocationEnabled)

    val vibrationAlertStatus = MutableLiveData(sharedPrefManager.isVibrationEnabled)

    val notificationAlertStatus = MutableLiveData(sharedPrefManager.isNotificationEnabled)

    val suspiciousDetectionStatus = MutableLiveData(sharedPrefManager.isSuspiciousDetectionEnabled)

    val screenOffMonitoringStatus = MutableLiveData(sharedPrefManager.isScreenOffMonitoringEnabled)

    val showAppInfoOnIndicatorStatus = MutableLiveData(sharedPrefManager.isShowAppInfoOnIndicator)

    val autoRevokeEnabledStatus = MutableLiveData(sharedPrefManager.isAutoRevokeEnabled)

    val indicatorForegroundColor = MutableLiveData(sharedPrefManager.indicatorColor)

    val indicatorBackgroundColor = MutableLiveData(sharedPrefManager.indicatorBackgroundColor)

    val indicatorPosition = MutableLiveData(sharedPrefManager.indicatorPosition)

    val indicatorSize = MutableLiveData(sharedPrefManager.indicatorSize)

    val indicatorOpacity = MutableLiveData(sharedPrefManager.indicatorOpacity)

    fun setCameraIndicatorStatus(isEnabled: Boolean) {
        sharedPrefManager.isCameraIndicatorEnabled = isEnabled
        cameraIndicatorStatus.value = isEnabled
    }

    fun setMicrophoneIndicatorStatus(isEnabled: Boolean) {
        sharedPrefManager.isMicIndicatorEnabled = isEnabled
        microphoneIndicatorStatus.value = isEnabled
    }

    fun setLocationIndicatorStatus(isEnabled: Boolean) {
        sharedPrefManager.isLocationEnabled = isEnabled
        locationIndicatorStatus.value = isEnabled
    }

    fun setVibrationAlertStatus(isEnabled: Boolean) {
        sharedPrefManager.isVibrationEnabled = isEnabled
        vibrationAlertStatus.value = isEnabled
    }

    fun setNotificationAlertStatus(isEnabled: Boolean) {
        sharedPrefManager.isNotificationEnabled = isEnabled
        notificationAlertStatus.value = isEnabled
    }

    fun setSuspiciousDetectionStatus(isEnabled: Boolean) {
        sharedPrefManager.setSuspiciousDetectionEnabled(isEnabled)
        suspiciousDetectionStatus.value = isEnabled
    }

    fun setScreenOffMonitoringStatus(isEnabled: Boolean) {
        sharedPrefManager.setScreenOffMonitoringEnabled(isEnabled)
        screenOffMonitoringStatus.value = isEnabled
    }

    fun setShowAppInfoOnIndicatorStatus(isEnabled: Boolean) {
        sharedPrefManager.setShowAppInfoOnIndicator(isEnabled)
        showAppInfoOnIndicatorStatus.value = isEnabled
    }

    fun setAutoRevokeEnabledStatus(isEnabled: Boolean) {
        sharedPrefManager.setAutoRevokeEnabled(isEnabled)
        autoRevokeEnabledStatus.value = isEnabled
    }

    fun setIndicatorForegroundColor(color: String) {
        sharedPrefManager.indicatorColor = color
        indicatorForegroundColor.value = color
    }

    fun setIndicatorBackgroundColor(color: String) {
        sharedPrefManager.indicatorBackgroundColor = color
        indicatorBackgroundColor.value = color
    }

    fun setIndicatorPosition(position: IndicatorPosition) {
        sharedPrefManager.indicatorPosition = position
        indicatorPosition.value = position
    }

    fun setIndicatorSize(size: IndicatorSize) {
        sharedPrefManager.indicatorSize = size
        indicatorSize.value = size
    }

    fun setIndicatorOpacity(opacity: IndicatorOpacity) {
        sharedPrefManager.indicatorOpacity = opacity
        indicatorOpacity.value = opacity
    }

    companion object {
        const val GITHUB_REPO = "https://github.com/puneetnith28/Obsidian"
        const val GITHUB_PROFILE = "https://github.com/puneetnith28"
        const val LINKEDIN = "https://www.linkedin.com/in/puneet-yadav-541166325"
        const val HACKSECURE = "https://github.com/puneetnith28/Obsidian"
        const val SHARING_TEXT = "Checkout this app which provides you the Privacy Features of iOS 14 and Android 12 on your device. https://play.google.com/store/apps/details?id=com.obsidian.aegis"
    }

}