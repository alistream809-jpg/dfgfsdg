package com.ramflow.pro.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import java.io.File

/** Tum degerler dogrudan Android sistem API'lerinden okunur. */
class SystemMonitor(private val ctx: Context) {
    private val am = ctx.getSystemService(ActivityManager::class.java)
    private val pw = ctx.getSystemService(PowerManager::class.java)
    private val cores = Runtime.getRuntime().availableProcessors()
    private var lastCpu = Process.getElapsedCpuTime()
    private var lastWall = SystemClock.elapsedRealtime()

    fun ram(): RamInfo {
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return RamInfo(mi.totalMem, mi.availMem, mi.threshold, mi.lowMemory)
    }

    fun battery(): BatteryInfo {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plug = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val t = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val v = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = when (st) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        val plugged = when {
            (plug and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> "ac"
            (plug and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> "usb"
            (plug and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> "wireless"
            else -> "none"
        }
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        return BatteryInfo(pct, if (t == Int.MIN_VALUE) null else t / 10f, status, plugged, v)
    }

    /** PowerManager.THERMAL_STATUS_* (0..6), desteklenmiyorsa -1. */
    fun thermalStatus(): Int = try { pw.currentThermalStatus } catch (e: Exception) { -1 }

    fun storage(): StorageInfo {
        val s = StatFs(Environment.getDataDirectory().path)
        return StorageInfo(s.totalBytes, s.availableBytes)
    }

    private fun readLong(path: String): Long = try { File(path).readText().trim().toLong() } catch (e: Exception) { 0L }

    /** Android 8+ /proc/stat'i engelledigi icin genel CPU yuku okunamaz; uygulamanin kendi CPU yuku olculur. */
    fun cpu(): CpuInfo {
        val cpuNow = Process.getElapsedCpuTime()
        val wallNow = SystemClock.elapsedRealtime()
        val dw = (wallNow - lastWall).coerceAtLeast(1)
        val load = ((cpuNow - lastCpu) * 100f / (dw * cores)).coerceIn(0f, 100f)
        lastCpu = cpuNow; lastWall = wallNow
        var cur = 0L; var max = 0L
        for (i in 0 until cores) {
            cur = maxOf(cur, readLong("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"))
            max = maxOf(max, readLong("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"))
        }
        return CpuInfo(
            cores, Build.SUPPORTED_ABIS.firstOrNull() ?: "?", Build.HARDWARE,
            if (cur > 0) (cur / 1000).toInt() else null,
            if (max > 0) (max / 1000).toInt() else null, load
        )
    }

    fun snapshot() = SystemSnapshot(
        ram(), battery(), cpu(), storage(), thermalStatus(),
        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    )
}
