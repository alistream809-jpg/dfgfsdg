package com.ramflow.pro.data

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ramflow", Context.MODE_PRIVATE)

    var theme: Int get() = sp.getInt("theme", 0); set(v) = sp.edit().putInt("theme", v).apply()
    var lang: String get() = sp.getString("lang", "tr") ?: "tr"; set(v) = sp.edit().putString("lang", v).apply()
    var animations: Boolean get() = sp.getBoolean("anim", true); set(v) = sp.edit().putBoolean("anim", v).apply()
    var notifications: Boolean get() = sp.getBoolean("notif", true); set(v) = sp.edit().putBoolean("notif", v).apply()
    var autoEnabled: Boolean get() = sp.getBoolean("auto", false); set(v) = sp.edit().putBoolean("auto", v).apply()
    var autoRunning: Boolean get() = sp.getBoolean("autorun", false); set(v) = sp.edit().putBoolean("autorun", v).apply()
    var intervalMin: Int get() = sp.getInt("interval", 15); set(v) = sp.edit().putInt("interval", v).apply()
    var alertPct: Int get() = sp.getInt("alertPct", 85); set(v) = sp.edit().putInt("alertPct", v).apply()
    var trim: Boolean get() = sp.getBoolean("trim", true); set(v) = sp.edit().putBoolean("trim", v).apply()
    var saveHistory: Boolean get() = sp.getBoolean("saveHistory", true); set(v) = sp.edit().putBoolean("saveHistory", v).apply()

    fun history(): List<HistoryEntry> = (sp.getString("history", "") ?: "").lines().mapNotNull { line ->
        val p = line.split(",")
        if (p.size < 6) null else try {
            HistoryEntry(p[0].toLong(), p[1].toLong(), p[2].toLong(), p[3].toInt(), p[4].toInt(), p[5].toInt())
        } catch (e: Exception) { null }
    }

    fun addHistory(e: HistoryEntry) {
        val all = (listOf(e) + history()).take(30)
        sp.edit().putString("history", all.joinToString("\n") {
            "${it.ts},${it.availBefore},${it.availAfter},${it.analyzed},${it.completed},${it.userActions}"
        }).apply()
    }

    fun clearHistory() = sp.edit().remove("history").apply()
}
