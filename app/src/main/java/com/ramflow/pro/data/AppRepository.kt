package com.ramflow.pro.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.os.storage.StorageManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val ctx: Context) {

    fun hasUsageAccess(): Boolean = try {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) ==
            AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val pm = ctx.packageManager
        val access = hasUsageAccess()
        val now = System.currentTimeMillis()
        val usage: Map<String, UsageStats> = if (access) try {
            ctx.getSystemService(UsageStatsManager::class.java)
                .queryAndAggregateUsageStats(now - 7L * 24 * 3600_000, now)
        } catch (e: Exception) { emptyMap() } else emptyMap()
        val ssm = ctx.getSystemService(StorageStatsManager::class.java)
        val me = Process.myUserHandle()

        pm.getInstalledApplications(0)
            .filter { it.packageName != ctx.packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { ai ->
                var app = -1L; var data = -1L; var cache = -1L
                if (access) try {
                    val s = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName, me)
                    app = s.appBytes; cache = s.cacheBytes; data = (s.dataBytes - s.cacheBytes).coerceAtLeast(0)
                } catch (e: Exception) { }
                val u = usage[ai.packageName]
                val sys = (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                val icon = try { pm.getApplicationIcon(ai.packageName).toBitmap(88, 88).asImageBitmap() } catch (e: Exception) { null }
                AppEntry(
                    ai.packageName, ai.loadLabel(pm).toString(), icon, sys,
                    u?.lastTimeUsed ?: 0L, u?.totalTimeInForeground ?: 0L, app, cache, data
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
