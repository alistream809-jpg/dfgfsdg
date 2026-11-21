package com.ramflow.pro

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ramflow.pro.data.*
import com.ramflow.pro.work.AutoScheduler
import kotlinx.coroutines.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val monitor = SystemMonitor(app)
    val repo = AppRepository(app)
    private val engine = BoostEngine(app, monitor, repo)
    private val ctx: Application get() = getApplication()

    var snapshot by mutableStateOf(monitor.snapshot())
    val ramSamples = mutableStateListOf<Float>()
    val cpuSamples = mutableStateListOf<Float>()

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
    var appsLoading by mutableStateOf(false)
    var usageAccess by mutableStateOf(repo.hasUsageAccess())
    var appFilter by mutableIntStateOf(0)
    val selected = mutableStateListOf<String>()
    val manageQueue = mutableStateListOf<String>()
    val trimRequested = mutableStateListOf<String>()

    var boostRunning by mutableStateOf(false)
    var boostSteps by mutableStateOf<List<BoostStep>>(emptyList())
    var boostProgress by mutableFloatStateOf(0f)
    var boostReport by mutableStateOf<BoostReport?>(null)
    var boostError by mutableStateOf<String?>(null)
    var history by mutableStateOf(prefs.history())

    var themeMode by mutableIntStateOf(prefs.theme)
    var lang by mutableStateOf(prefs.lang)
    var animations by mutableStateOf(prefs.animations)
    var notifications by mutableStateOf(prefs.notifications)
    var autoEnabled by mutableStateOf(prefs.autoEnabled)
    var autoRunning by mutableStateOf(prefs.autoRunning)
    var intervalMin by mutableIntStateOf(prefs.intervalMin)
    var alertPct by mutableIntStateOf(prefs.alertPct)
    var trimEnabled by mutableStateOf(prefs.trim)
    var saveHistory by mutableStateOf(prefs.saveHistory)
    var ownCache by mutableLongStateOf(0L)

    private var left = false
    fun markLeft() { left = true }

    suspend fun sampleLoop() {
        while (currentCoroutineContext().isActive) {
            val s = withContext(Dispatchers.Default) { monitor.snapshot() }
            snapshot = s
            push(ramSamples, s.ram.pct); push(cpuSamples, s.cpu.appLoadPct)
            delay(1000)
        }
    }

    private fun push(l: SnapshotStateList<Float>, v: Float) { l.add(v); while (l.size > 60) l.removeAt(0) }

    /** Ekran her one geldiginde: izin durumu + ayarlardan donus sonrasi bellek yeniden olcumu. */
    fun onResume() {
        val had = usageAccess
        usageAccess = repo.hasUsageAccess()
        if (left) {
            left = false
            val r = boostReport
            if (r != null) boostReport = r.copy(ramAfterManual = monitor.ram().avail)
            if (usageAccess != had) refreshApps()
        }
    }

    fun refreshApps() {
        if (appsLoading) return
        appsLoading = true
        viewModelScope.launch {
            try { apps = repo.loadApps() } catch (e: Exception) { } finally { appsLoading = false }
        }
    }

    fun boost() {
        if (boostRunning) return
        boostRunning = true; boostReport = null; boostSteps = emptyList(); boostProgress = 0f; boostError = null
        viewModelScope.launch {
            try {
                if (apps.isEmpty()) apps = repo.loadApps()
                val rep = engine.run(apps, trimEnabled, lang == "tr") { s, p -> boostSteps = s; boostProgress = p }
                boostReport = rep
                trimRequested.clear(); if (rep.trimSent > 0) trimRequested.addAll(rep.candidates)
                if (saveHistory) {
                    prefs.addHistory(HistoryEntry(System.currentTimeMillis(), rep.before.avail, rep.after.avail, rep.analyzed, rep.completed, rep.userActions))
                    history = prefs.history()
                }
            } catch (e: Exception) { boostError = e.message ?: "error" } finally { boostRunning = false }
        }
    }

    fun toggleSelect(pkg: String) { if (pkg in selected) selected.remove(pkg) else selected.add(pkg) }
    fun selectCandidates(list: List<String>) { selected.clear(); selected.addAll(list) }
    fun startManage() { manageQueue.clear(); manageQueue.addAll(selected); selected.clear() }

    // ---- ayarlar ----
    fun setTheme(v: Int) { themeMode = v; prefs.theme = v }
    fun setLang(v: String) { lang = v; prefs.lang = v }
    fun setAnimations(v: Boolean) { animations = v; prefs.animations = v }
    fun setNotifications(v: Boolean) { notifications = v; prefs.notifications = v }
    fun setTrim(v: Boolean) { trimEnabled = v; prefs.trim = v }
    fun setSaveHistory(v: Boolean) { saveHistory = v; prefs.saveHistory = v }
    fun setAlertPct(v: Int) { alertPct = v; prefs.alertPct = v }
    fun clearHistory() { prefs.clearHistory(); history = emptyList() }

    fun setAuto(on: Boolean) { autoEnabled = on; prefs.autoEnabled = on; if (on) startAuto() else stopAuto() }
    fun startAuto() {
        if (!autoEnabled) return
        AutoScheduler.start(ctx, intervalMin); autoRunning = true; prefs.autoRunning = true
    }
    fun stopAuto() { AutoScheduler.stop(ctx); autoRunning = false; prefs.autoRunning = false }
    fun setInterval(m: Int) { intervalMin = m; prefs.intervalMin = m; if (autoRunning) AutoScheduler.start(ctx, m) }

    // ---- gecici dosyalar (yalnizca RAMFLOW'un kendi onbellegi) ----
    private fun dirSize(f: java.io.File) = f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    fun refreshOwnCache() {
        ownCache = dirSize(ctx.cacheDir) + (ctx.externalCacheDir?.let { dirSize(it) } ?: 0L)
    }
    fun clearOwnCache() {
        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        ctx.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        refreshOwnCache()
    }
}
