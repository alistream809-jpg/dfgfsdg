package com.ramflow.pro.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.delay

/**
 * Gercek Boost motoru. Yalnizca Android'in izin verdigi islemleri yapar:
 *  - bellek istatistigi okuma (ActivityManager.MemoryInfo)
 *  - ActivityManager.killBackgroundProcesses (yalnizca ARKA PLAN surecleri, sistem istegi; sonucu uygulama basina dogrulanamaz)
 *  - zorla durdurma icin kullaniciyi Uygulama Bilgisi ekranina yonlendirme
 */
class BoostEngine(private val ctx: Context, private val monitor: SystemMonitor, private val repo: AppRepository) {

    private fun protectedPackages(): Set<String> {
        val pm = ctx.packageManager
        val set = mutableSetOf(ctx.packageName, "com.android.systemui", "android")
        try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .forEach { set += it.activityInfo.packageName }
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.let { ComponentName.unflattenFromString(it)?.packageName?.let { p -> set += p } }
        } catch (e: Exception) { }
        return set
    }

    suspend fun run(
        apps: List<AppEntry>, trim: Boolean, tr: Boolean,
        update: (List<BoostStep>, Float) -> Unit
    ): BoostReport {
        fun L(en: String, trk: String) = if (tr) trk else en
        val titles = listOf(
            L("Collect memory statistics", "Bellek istatistikleri toplanıyor"),
            L("Identify accessible apps", "Erişilebilir uygulamalar tespit ediliyor"),
            L("Detect background activity", "Arka plan etkinliği tespit ediliyor"),
            L("Check per-app memory", "Uygulama başı bellek kontrolü"),
            L("Request background trim", "Arka plan temizliği isteniyor"),
            L("Re-measure memory", "Bellek yeniden ölçülüyor"),
            L("Apps for manual management", "Manuel yönetilecek uygulamalar")
        )
        val steps = titles.map { BoostStep(it, StepStatus.PENDING, "") }.toMutableList()
        fun setStep(i: Int, s: StepStatus, d: String) {
            steps[i] = steps[i].copy(status = s, detail = d)
            val done = if (s == StepStatus.RUNNING) i else i + 1
            update(steps.toList(), done / titles.size.toFloat())
        }

        setStep(0, StepStatus.RUNNING, ""); delay(400)
        val before = monitor.ram()
        setStep(0, StepStatus.DONE, L("${fmt(before.avail)} available of ${fmt(before.total)}", "${fmt(before.total)} içinde ${fmt(before.avail)} boş"))

        setStep(1, StepStatus.RUNNING, ""); delay(300)
        val access = repo.hasUsageAccess()
        setStep(
            1, if (access) StepStatus.DONE else StepStatus.USER_ACTION,
            L("${apps.size} apps analyzed", "${apps.size} uygulama analiz edildi") +
                if (access) "" else L(" · usage access not granted (limited)", " · kullanım erişimi verilmedi (sınırlı)")
        )

        setStep(2, StepStatus.RUNNING, ""); delay(300)
        val now = System.currentTimeMillis()
        val prot = protectedPackages()
        val cands = apps.filter { a ->
            !a.isSystem && a.pkg !in prot &&
                (!access || (a.lastUsed > 0 && now - a.lastUsed > 10 * 60_000L && now - a.lastUsed < 3 * 24 * 3600_000L))
        }
        setStep(2, StepStatus.DONE, L("${cands.size} user apps likely idle in background", "${cands.size} kullanıcı uygulaması arka planda boşta olabilir"))

        setStep(3, StepStatus.RUNNING, ""); delay(200)
        setStep(3, StepStatus.BLOCKED, L(
            "Android hides other apps' memory from third-party apps (not readable without root)",
            "Android diğer uygulamaların bellek bilgisini üçüncü taraf uygulamalardan gizler (root olmadan okunamaz)"
        ))

        var sent = 0
        setStep(4, StepStatus.RUNNING, "")
        if (trim && cands.isNotEmpty()) {
            val am = ctx.getSystemService(ActivityManager::class.java)
            for (c in cands) {
                try { am.killBackgroundProcesses(c.pkg); sent++ } catch (e: Exception) { }
                delay(15)
            }
            setStep(4, StepStatus.DONE, L(
                "$sent requests sent. Android decides what is really freed; per-app result cannot be verified",
                "$sent istek gönderildi. Gerçekte neyin kapandığına Android karar verir; uygulama bazında doğrulanamaz"
            ))
        } else {
            setStep(4, StepStatus.BLOCKED, if (!trim) L("Disabled in settings", "Ayarlarda kapalı") else L("Nothing to trim", "Temizlenecek uygulama yok"))
        }

        setStep(5, StepStatus.RUNNING, ""); delay(2000)
        val after = monitor.ram()
        val diff = after.avail - before.avail
        setStep(5, StepStatus.DONE, L("Available memory change: ${if (diff >= 0) "+" else "-"}${fmt(kotlin.math.abs(diff))}",
            "Boş bellek değişimi: ${if (diff >= 0) "+" else "-"}${fmt(kotlin.math.abs(diff))}"))

        setStep(6, StepStatus.RUNNING, ""); delay(200)
        val userActions = cands.size + if (access) 0 else 1
        setStep(6, if (cands.isNotEmpty()) StepStatus.USER_ACTION else StepStatus.DONE, L(
            "${cands.size} apps can be force-stopped by you from App Info",
            "${cands.size} uygulamayı Uygulama Bilgisi'nden sen zorla durdurabilirsin"
        ))

        val completed = steps.count { it.status == StepStatus.DONE }
        val blocked = steps.count { it.status == StepStatus.BLOCKED }
        return BoostReport(before, after, apps.size, completed, userActions, blocked, sent, cands.map { it.pkg })
    }

    private fun fmt(b: Long): String {
        val gb = b / 1024.0 / 1024 / 1024
        return if (gb >= 1) String.format(java.util.Locale.US, "%.2f GB", gb) else "${b / 1024 / 1024} MB"
    }
}
