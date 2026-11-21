package com.ramflow.pro.data

import androidx.compose.ui.graphics.ImageBitmap

data class RamInfo(val total: Long, val avail: Long, val threshold: Long, val low: Boolean) {
    val used: Long get() = total - avail
    val pct: Float get() = if (total > 0) used * 100f / total else 0f
}

data class BatteryInfo(val pct: Int, val tempC: Float?, val status: String, val plugged: String, val voltageMv: Int)

data class CpuInfo(
    val cores: Int, val abi: String, val hardware: String,
    val curMhz: Int?, val maxMhz: Int?, val appLoadPct: Float
)

data class StorageInfo(val total: Long, val free: Long) {
    val used: Long get() = total - free
    val pct: Float get() = if (total > 0) used * 100f / total else 0f
}

data class SystemSnapshot(
    val ram: RamInfo, val battery: BatteryInfo, val cpu: CpuInfo, val storage: StorageInfo,
    val thermal: Int, val androidVersion: String, val model: String
)

data class AppEntry(
    val pkg: String, val label: String, val icon: ImageBitmap?, val isSystem: Boolean,
    val lastUsed: Long, val foregroundMs: Long,
    val appBytes: Long, val cacheBytes: Long, val dataBytes: Long
) {
    val totalBytes: Long get() = maxOf(appBytes, 0) + maxOf(dataBytes, 0) + maxOf(cacheBytes, 0)
}

enum class StepStatus { PENDING, RUNNING, DONE, USER_ACTION, BLOCKED }

data class BoostStep(val title: String, val status: StepStatus, val detail: String)

data class BoostReport(
    val before: RamInfo, val after: RamInfo,
    val analyzed: Int, val completed: Int, val userActions: Int, val blocked: Int,
    val trimSent: Int, val candidates: List<String>,
    val ramAfterManual: Long? = null
)

data class HistoryEntry(
    val ts: Long, val availBefore: Long, val availAfter: Long,
    val analyzed: Int, val completed: Int, val userActions: Int
)
