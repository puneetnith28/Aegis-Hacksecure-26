package com.obsidian.aegis.services

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.TrafficStats
import android.os.PowerManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.AvailabilityCallback
import android.location.GnssStatus
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.AudioManager.AudioRecordingCallback
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.obsidian.aegis.BuildConfig
import com.obsidian.aegis.R
import com.obsidian.aegis.databinding.IndicatorsLayoutBinding
import com.obsidian.aegis.db.AccessLogsDatabase
import com.obsidian.aegis.helpers.setViewTint
import com.obsidian.aegis.helpers.updateOpacity
import com.obsidian.aegis.helpers.updateSize
import com.obsidian.aegis.models.AccessLog
import com.obsidian.aegis.models.IndicatorType
import com.obsidian.aegis.repository.AccessLogsRepo
import com.obsidian.aegis.repository.SharedPrefManager
import com.obsidian.aegis.ui.home.HomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

class IndicatorService : AccessibilityService() {
    private lateinit var binding: IndicatorsLayoutBinding
    private var cameraManager: CameraManager? = null
    private var cameraCallback: AvailabilityCallback? = null
    private var locationManager: LocationManager? = null
    private var locationCallback: GnssStatus.Callback? = null
    private var audioManager: AudioManager? = null
    private var micCallback: AudioRecordingCallback? = null
    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var windowManager: WindowManager
    private lateinit var accessLogsRepo: AccessLogsRepo
    private lateinit var usageStatsHelper: com.obsidian.aegis.helpers.UsageStatsHelper
    private val notification_channel_id = "PRIVACY_INDICATORS_NOTIFICATION"
    private val suspicious_channel_id = "SUSPICIOUS_ACTIVITIES_NOTIFICATION"
    private var notifManager: NotificationManagerCompat? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private val notificationID = 256
    private var isCameraOn = false
    private var isMicOn = false
    private var isLocationOn = false
    private var currentAppId = BuildConfig.APPLICATION_ID

    private var activeCameraAppId: String? = null
    private var activeMicAppId: String? = null
    private var activeLocationAppId: String? = null

    private val micStartTimes = mutableMapOf<String, Long>()
    private val locationStartTimes = mutableMapOf<String, Long>()
    private val cameraStartTimes = mutableMapOf<String, Long>()
    private val lastSuspiciousLogTime = mutableMapOf<String, Long>()
    private val appInfoHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val sensorAccessTimes = mutableMapOf<String, MutableList<Long>>()
    private val appStartTxBytes = mutableMapOf<String, Long>()
    private val sensorMonitorHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isMonitoringNetwork = false
    
    private val sensorMonitorRunnable = object : Runnable {
        override fun run() {
            if (isCameraOn) checkPeriodicSuspicious("Camera", activeCameraAppId ?: currentAppId)
            if (isMicOn) checkPeriodicSuspicious("Microphone", activeMicAppId ?: currentAppId)
            if (isLocationOn) checkPeriodicSuspicious("Location", activeLocationAppId ?: currentAppId)
            
            if (isMonitoringNetwork) {
                sensorMonitorHandler.postDelayed(this, 3000)
            }
        }
    }

    private fun captureRealForegroundApp(): String {
        return usageStatsHelper.getCurrentForegroundApp()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: currentAppId
    }

    override fun onCreate() {
        super.onCreate()
        fetchData()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        createOverlay()
        setUpInnerViews()
        startCallBacks()
    }

    private fun fetchData() {
        sharedPrefManager = SharedPrefManager.getInstance(applicationContext)
        accessLogsRepo = AccessLogsRepo(AccessLogsDatabase(this))
        usageStatsHelper = com.obsidian.aegis.helpers.UsageStatsHelper(this)
    }

    private fun startCallBacks() {
        if (cameraManager == null) cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraManager!!.registerAvailabilityCallback(getCameraCallback(), null)

        if (audioManager == null) audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager!!.registerAudioRecordingCallback(getMicCallback(), null)

        registerLocationCallback()
    }

    //This feature is EXPERIMENTAL
    private fun registerLocationCallback() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(locationManager==null) locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager!!.registerGnssStatusCallback(getLocationCallback())
            val locationListener = LocationListener {  }
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 8.4f,locationListener)
            locationManager!!.removeUpdates(locationListener)
        }else{
            sharedPrefManager.isLocationEnabled = false
        }
    }

    private fun getCameraCallback(): AvailabilityCallback {
        cameraCallback = object : AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                super.onCameraAvailable(cameraId)
                if (isCameraOn) {
                    isCameraOn = false
                    val app = activeCameraAppId ?: currentAppId
                    val startTime = cameraStartTimes[app] ?: 0L
                    if (startTime > 0) {
                        makeLog(IndicatorType.CAMERA, startTime, System.currentTimeMillis(), app)
                        cameraStartTimes[app] = 0L
                    }
                    activeCameraAppId = null
                    hideCam()
                    dismissNotification()
                    stopSensorMonitoring()
                }
            }

            override fun onCameraUnavailable(cameraId: String) {
                super.onCameraUnavailable(cameraId)
                if (!isCameraOn) {
                    isCameraOn = true
                    activeCameraAppId = captureRealForegroundApp()
                    val appId = activeCameraAppId!!
                    cameraStartTimes[appId] = System.currentTimeMillis()
                    showCam()
                    triggerVibration()
                    showNotification()
                    
                    recordTrafficStart(appId)
                    checkFrequentAccessSuspicious("Camera", appId)
                    startSensorMonitoring()
                    
                    showAppInfoOnIndicator()
                    
                    appInfoHandler.postDelayed({
                        if (isCameraOn) {
                            val updatedApp = captureRealForegroundApp()
                            if (activeCameraAppId != updatedApp) {
                                activeCameraAppId = updatedApp
                                showAppInfoOnIndicator()
                            }
                        }
                    }, 1000)
                }
            }
        }
        return cameraCallback as AvailabilityCallback
    }

    private fun getMicCallback(): AudioRecordingCallback {
        micCallback = object : AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                if (configs.size > 0) {
                    if (!isMicOn) {
                        isMicOn = true
                        
                        var micApp = captureRealForegroundApp()
                        activeMicAppId = micApp
                        val appId = activeMicAppId!!
                        
                        showMic()
                        triggerVibration()
                        showNotification()
                        micStartTimes[appId] = System.currentTimeMillis()
                        
                        recordTrafficStart(appId)
                        checkFrequentAccessSuspicious("Microphone", appId)
                        startSensorMonitoring()
                        
                        showAppInfoOnIndicator()
                        
                        appInfoHandler.postDelayed({
                            if (isMicOn) {
                                val updatedApp = captureRealForegroundApp()
                                if (activeMicAppId != updatedApp) {
                                    activeMicAppId = updatedApp
                                    showAppInfoOnIndicator()
                                }
                            }
                        }, 1000)
                    }
                } else {
                    if (isMicOn) {
                        isMicOn = false
                        hideMic()
                        dismissNotification()
                        val app = activeMicAppId ?: currentAppId
                        val startTime = micStartTimes[app] ?: 0L
                        if (startTime > 0) {
                            makeLog(IndicatorType.MICROPHONE, startTime, System.currentTimeMillis(), app)
                            micStartTimes[app] = 0L
                        }
                        activeMicAppId = null
                        stopSensorMonitoring()
                    }
                }
            }
        }
        return micCallback as AudioRecordingCallback
    }

    private fun getLocationCallback(): GnssStatus.Callback {
        locationCallback = object : GnssStatus.Callback() {
            override fun onStarted() {
                super.onStarted()
                if (!isLocationOn) {
                    isLocationOn = true
                    activeLocationAppId = captureRealForegroundApp()
                    val appId = activeLocationAppId!!
                    locationStartTimes[appId] = System.currentTimeMillis()
                    showLocation()
                    triggerVibration()
                    showNotification()
                    
                    recordTrafficStart(appId)
                    checkFrequentAccessSuspicious("Location", appId)
                    startSensorMonitoring()
                    
                    showAppInfoOnIndicator()
                    
                    appInfoHandler.postDelayed({
                        if (isLocationOn) {
                            val updatedApp = captureRealForegroundApp()
                            if (activeLocationAppId != updatedApp) {
                                activeLocationAppId = updatedApp
                                showAppInfoOnIndicator()
                            }
                        }
                    }, 1000)
                }
            }

            override fun onStopped() {
                super.onStopped()
                if (isLocationOn) {
                    isLocationOn = false
                    val app = activeLocationAppId ?: currentAppId
                    val startTime = locationStartTimes[app] ?: 0L
                    if (startTime > 0) {
                        makeLog(IndicatorType.LOCATION, startTime, System.currentTimeMillis(), app)
                        locationStartTimes[app] = 0L
                    }
                    activeLocationAppId = null
                    hideLocation()
                    dismissNotification()
                    stopSensorMonitoring()
                }
            }
        }
        return locationCallback as GnssStatus.Callback
    }

    private fun startSensorMonitoring() {
        if (!isMonitoringNetwork) {
            isMonitoringNetwork = true
            sensorMonitorHandler.post(sensorMonitorRunnable)
        }
    }

    private fun stopSensorMonitoring() {
        if (!isCameraOn && !isMicOn && !isLocationOn) {
            isMonitoringNetwork = false
            sensorMonitorHandler.removeCallbacks(sensorMonitorRunnable)
        }
    }

    private fun recordTrafficStart(appId: String) {
        val uid = getUidForPackage(appId)
        if (uid != -1) {
            appStartTxBytes[appId] = TrafficStats.getUidTxBytes(uid)
        }
    }

    private fun checkPeriodicSuspicious(sensor: String, appId: String) {
        if (!sharedPrefManager.isSuspiciousDetectionEnabled) return
        if (sharedPrefManager.isAppWhitelisted(appId)) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOff = !powerManager.isInteractive
        
        if (isScreenOff && sharedPrefManager.isScreenOffMonitoringEnabled()) {
            saveSuspiciousActivity(appId, "$sensor accessed while screen was off", "Critical")
        } else if (isAppInBackground(appId)) {
            saveSuspiciousActivity(appId, "$sensor accessed while app is in background", "High")
        }
        
        val uid = getUidForPackage(appId)
        if (uid != -1) {
            val startTx = appStartTxBytes[appId] ?: 0L
            val currentTx = TrafficStats.getUidTxBytes(uid)
            if (startTx > 0 && currentTx > startTx + 1024) { 
                saveSuspiciousActivity(appId, "Sending $sensor data to remote network", "Critical")
                appStartTxBytes[appId] = currentTx
            }
        }
    }

    private fun checkFrequentAccessSuspicious(sensor: String, appId: String) {
        if (!sharedPrefManager.isSuspiciousDetectionEnabled) return
        if (sharedPrefManager.isAppWhitelisted(appId)) return
        
        val now = System.currentTimeMillis()
        val shortTimeWindow = now - 60000 // 1 minute window
        val times = sensorAccessTimes.getOrPut(appId) { mutableListOf() }
        times.add(now)
        times.removeAll { it < shortTimeWindow }

        if (times.size >= 3) {
            saveSuspiciousActivity(appId, "Accessed $sensor multiple times in short period", "High")
            times.clear()
        }
    }

    private fun getUidForPackage(packageName: String): Int {
        return try {
            packageManager.getApplicationInfo(packageName, 0).uid
        } catch (e: Exception) {
            -1
        }
    }

    private fun isAppInBackground(packageName: String): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = am.runningAppProcesses ?: return false
        for (processInfo in runningProcesses) {
            if (processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && 
                processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                for (pkg in processInfo.pkgList) {
                    if (pkg == packageName) return true
                }
            }
        }
        return false
    }

    private fun showSuspiciousAlertNotification(appName: String, reason: String) {
        if (!sharedPrefManager.isNotificationEnabled) return
        createSuspiciousNotificationChannel()
        val alertBuilder = NotificationCompat.Builder(applicationContext, suspicious_channel_id)
            .setSmallIcon(R.drawable.camera_indicator2)
            .setContentTitle("⚠️ Suspicious Activity Detected!")
            .setContentText("The app $appName is suspicious: $reason. Check it!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("The app $appName is suspicious: $reason. Check it!"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(Color.RED)
            
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        NotificationManagerCompat.from(applicationContext).notify(notificationId, alertBuilder.build())
    }

    private fun showAppInfoOnIndicator() {
        if (!sharedPrefManager.isShowAppInfoOnIndicator()) return
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(currentAppId, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val appIcon = pm.getApplicationIcon(appInfo)

            binding.tvAppName.text = appName
            binding.ivAppIcon.setImageDrawable(appIcon)
            binding.tvAppName.visibility = View.VISIBLE
            binding.ivAppIcon.visibility = View.VISIBLE

            appInfoHandler.removeCallbacksAndMessages(null)
            appInfoHandler.postDelayed({
                binding.tvAppName.visibility = View.GONE
                binding.ivAppIcon.visibility = View.GONE
            }, 3000)
        } catch (e: Exception) {
            // Silently fail — don't disrupt indicator functionality
        }
    }

    private fun saveSuspiciousActivity(appId: String, description: String, riskLevel: String) {
        val now = System.currentTimeMillis()
        if (sharedPrefManager.isAppWhitelisted(appId)) return
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOff = !powerManager.isInteractive
        val monitoringEnabled = sharedPrefManager.isScreenOffMonitoringEnabled()
        
        var finalRiskLevel = riskLevel
        var finalDescription = description
        
        if (isScreenOff && monitoringEnabled && !description.contains("screen was off")) {
            finalRiskLevel = "Critical"
            finalDescription = "[Screen-Off] $description"
        }

        val key = "${appId}_$description"
        val lastTime = lastSuspiciousLogTime[key] ?: 0L
        if (now - lastTime < 300000) return 

        val appName = getAppName(appId)
        val activity = com.obsidian.aegis.models.SuspiciousActivity(
            time = now,
            appId = appId,
            appName = appName,
            description = finalDescription,
            riskLevel = finalRiskLevel,
            isScreenOff = isScreenOff
        )
        GlobalScope.launch(Dispatchers.IO) {
            accessLogsRepo.saveSuspiciousActivity(activity)
            lastSuspiciousLogTime[key] = now
        }
        
        showSuspiciousAlertNotification(appName, finalDescription)

        if (sharedPrefManager.isAutoRevokeEnabled()) {
            triggerAutoRevokeSequence(appId)
        }
    }

    private fun triggerAutoRevokeSequence(appId: String) {
        GlobalScope.launch(Dispatchers.Main) {
            var ringtone: Ringtone? = null
            try {
                var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                ringtone?.play()
            } catch (e: Exception) {
            }

            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                vibrator.vibrate(pattern, -1)
            }

            delay(5000)

            ringtone?.stop()
            vibrator.cancel()

            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$appId")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                Toast.makeText(applicationContext, "Aegis: Please revoke permissions for this suspicious app.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
            }
        }
    }

    private fun triggerVibration() {
        if (sharedPrefManager.isVibrationEnabled) {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                v.vibrate(500)
            }
        }
    }

    private fun setUpInnerViews() {
        setViewColors()
        showInitialAnimation(true)
    }

    private fun showInitialAnimation(isEnabled: Boolean) {
        val delay = if (isEnabled) 1000 else 0
        binding.ivLoc.postDelayed({
            binding.ivLoc.visibility = View.GONE
            binding.ivCam.visibility = View.GONE
            binding.ivMic.visibility = View.GONE
        }, delay.toLong())
    }

    private fun createOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        layoutParams = WindowManager.LayoutParams()
        layoutParams.apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = layoutGravity
        }
        binding = IndicatorsLayoutBinding.inflate(LayoutInflater.from(this))
        windowManager.addView(binding.root, layoutParams)
    }

    private fun updateLayoutGravity() {
        layoutParams.gravity = layoutGravity
        windowManager.updateViewLayout(binding.root, layoutParams)
    }

    private val layoutGravity: Int
        get() = sharedPrefManager.indicatorPosition.layoutGravity

    private fun makeLog(indicatorType: IndicatorType, startTime: Long, stopTime: Long, appId: String) {
        if (isLogEligible(appId)) {
            val durationMs = stopTime - startTime
            val log = AccessLog(startTime, durationMs, appId, getAppName(appId), indicatorType)
            GlobalScope.launch(Dispatchers.IO) {
                accessLogsRepo.save(log)
            }
        }
    }

    private fun getAppName(packageName: String): String {
        val packageManager = applicationContext.packageManager
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)) as String
        } catch (exp: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun isLogEligible(currentAppId: String): Boolean {
        return currentAppId != BuildConfig.APPLICATION_ID
    }

    private fun showMic() {
        if (sharedPrefManager.isMicIndicatorEnabled) {
            updateIndicatorProperties()
            binding.ivMic.visibility = View.VISIBLE
        }
    }

    private fun hideMic() {
        binding.ivMic.visibility = View.GONE
    }

    private fun showCam() {
        if (sharedPrefManager.isCameraIndicatorEnabled) {
            updateIndicatorProperties()
            binding.ivCam.visibility = View.VISIBLE
        }
    }

    private fun hideCam() {
        binding.ivCam.visibility = View.GONE
    }

    private fun showLocation() {
        if (sharedPrefManager.isLocationEnabled) {
            updateIndicatorProperties()
            binding.ivLoc.visibility = View.VISIBLE
        }
    }

    private fun hideLocation() {
        binding.ivLoc.visibility = View.GONE
    }

    private fun updateIndicatorProperties() {
        updateLayoutGravity()
        updateIndicatorsSize()
        updateIndicatorsOpacity()
        setViewColors()
    }

    private fun setViewColors() {
        val indicatorBackground = sharedPrefManager.indicatorBackgroundColor
        binding.ivCam.setViewTint(sharedPrefManager.cameraColor)
        binding.ivMic.setViewTint(sharedPrefManager.micColor)
        binding.ivLoc.setViewTint(sharedPrefManager.locationColor)
        binding.llBackground.setBackgroundColor(Color.parseColor(indicatorBackground))
    }

    private fun updateIndicatorsOpacity() {
        binding.root.updateOpacity(sharedPrefManager.indicatorOpacity.opacity)
    }

    private fun updateIndicatorsSize() {
        val size = sharedPrefManager.indicatorSize.size
        binding.cvIndicators.radius = (size / 2).toFloat()
        binding.ivCam.updateSize(size)
        binding.ivMic.updateSize(size)
        binding.ivLoc.updateSize(size)
    }

    fun upScaleView(view: View) {
        val fade_in = ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        fade_in.duration = 350
        fade_in.fillAfter = true
        view.startAnimation(fade_in)
    }

    fun downScaleView(view: View) {
        val fade_in = ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        fade_in.duration = 350
        fade_in.fillAfter = true
        view.startAnimation(fade_in)
    }

    private fun setupNotification() {
        createNotificationChannel()
        notificationBuilder = NotificationCompat.Builder(applicationContext, notification_channel_id)
                .setSmallIcon(R.drawable.camera_indicator2)
                .setContentTitle(notificationTitle)
                .setContentText(notificationDescription)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        notifManager = NotificationManagerCompat.from(applicationContext)
    }

    private val notificationTitle: String
        get() {
            val active = mutableListOf<String>()
            if (isCameraOn) active.add("Camera")
            if (isMicOn) active.add("Mic")
            if (isLocationOn) active.add("Location")
            
            return when (active.size) {
                0 -> "Aegis"
                1 -> "Your ${active[0]} is ON"
                2 -> "Your ${active[0]} and ${active[1]} is ON"
                else -> "Your Camera, Mic and Location are ON"
            }
        }
    private val notificationDescription: String
        get() {
            val active = mutableListOf<String>()
            if (isCameraOn) active.add("Camera")
            if (isMicOn) active.add("Microphone")
            if (isLocationOn) active.add("Location")

            val appsText = "A third-party app is using your "
            return when (active.size) {
                0 -> "Monitoring your privacy"
                1 -> "$appsText${active[0]}"
                2 -> "$appsText${active[0]} and ${active[1]}"
                else -> "$appsText${active[0]}, ${active[1]} and ${active[2]}"
            }
        }

    private fun showNotification() {
        if (sharedPrefManager.isNotificationEnabled) {
            setupNotification()
            if (notifManager != null) notifManager!!.notify(notificationID, notificationBuilder!!.build())
        }
    }

    private fun dismissNotification() {
        if (isCameraOn || isMicOn || isLocationOn) {
            showNotification()
        } else {
            if (notifManager != null) notifManager!!.cancel(notificationID)
        }
    }

    private val pendingIntent: PendingIntent
        get() {
            val intent = Intent(applicationContext, HomeActivity::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getActivity(applicationContext, 1, intent, flags)
        }

    private fun createNotificationChannel() {
        val notificationChannel = "Notifications for Aegis"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(notification_channel_id, notificationChannel, importance)
            val description = getString(R.string.notification_alert_summary)
            channel.description = description
            channel.lightColor = Color.RED
            val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSuspiciousNotificationChannel() {
        val notificationChannelName = "Suspicious Alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(suspicious_channel_id, notificationChannelName, importance)
            val description = "Critical alerts for suspicious app behaviors"
            channel.description = description
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {}
    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        if (accessibilityEvent.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && accessibilityEvent.packageName != null) {
            currentAppId = try {
                val componentName = ComponentName(accessibilityEvent.packageName.toString(), accessibilityEvent.className.toString())
                componentName.packageName
            } catch (exp: Exception) {
                BuildConfig.APPLICATION_ID
            }
        }
    }

    private fun unRegisterCameraCallBack() {
        if (cameraManager != null
                && cameraCallback != null) {
            cameraManager!!.unregisterAvailabilityCallback(cameraCallback!!)
        }
    }

    private fun unRegisterMicCallback() {
        if (audioManager != null
                && micCallback != null) {
            audioManager!!.unregisterAudioRecordingCallback(micCallback!!)
        }
    }

    private fun unRegisterLocationCallback() {
        if (locationManager != null
                && locationCallback != null) {
            locationManager!!.unregisterGnssStatusCallback(locationCallback!!)
        }
    }

    override fun onDestroy() {
        unRegisterCameraCallBack()
        unRegisterMicCallback()
        unRegisterLocationCallback()
        super.onDestroy()
    }
}