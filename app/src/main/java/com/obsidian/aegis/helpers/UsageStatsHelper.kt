package com.obsidian.aegis.helpers

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Gets the total time the given app spent in the foreground over the last 24 hours.
     * This acts as a proxy for battery baseline estimation.
     */
    fun getAppUsageTimeLast24Hours(packageName: String): Long {
        if (!hasUsageStatsPermission()) return 0L

        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        val stats = usageStatsManager.queryAndAggregateUsageStats(
            startTime,
            endTime
        )

        val appStats = stats[packageName] ?: return 0L
        
        // Calculate background time.
        // On Android Q (API 29) and above, totalTimeForegroundServiceUsed is available.
        // It's a good proxy for background battery usage.
        var bgTime = 0L
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            bgTime += appStats.totalTimeForegroundServiceUsed
        }
        
        // As a supplementary or fallback, we could consider totalTimeVisible - totalTimeInForeground,
        // but totalTimeForegroundServiceUsed is the most direct metric for active background work.
        // If they want background battery usage proxy:
        return bgTime
    }

    /**
     * Finds the app immediately in the foreground using UsageEvents.
     * highly accurate for detecting exactly who launched a sensor.
     */
    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) return null
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - (1000 * 60 * 5), now) // Look back 5 minutes
        var latestPackage: String? = null
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }
}
