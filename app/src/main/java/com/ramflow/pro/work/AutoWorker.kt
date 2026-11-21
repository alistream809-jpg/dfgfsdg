package com.ramflow.pro.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ramflow.pro.MainActivity
import com.ramflow.pro.R
import com.ramflow.pro.data.Prefs
import com.ramflow.pro.data.SystemMonitor
import java.util.concurrent.TimeUnit

/** Hafif, periyodik izleme. Uygulama oldurmez; yalnizca dikkat gerekirse bildirim gonderir. */
class AutoWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        val prefs = Prefs(c)
        if (!prefs.autoEnabled) return Result.success()
        val mon = SystemMonitor(c)
        val ram = mon.ram()
        val thermal = mon.thermalStatus()
        val tr = prefs.lang == "tr"
        val reasons = mutableListOf<String>()
        if (ram.pct >= prefs.alertPct) reasons += if (tr) "RAM kullanımı %${ram.pct.toInt()}" else "RAM usage ${ram.pct.toInt()}%"
        if (ram.low) reasons += if (tr) "Düşük bellek durumu" else "Low-memory condition"
        if (thermal >= 3) reasons += if (tr) "Cihaz ısınıyor (termal durum yüksek)" else "Device is hot (high thermal status)"
        if (reasons.isNotEmpty() && prefs.notifications) {
            notify(c, if (tr) "RAMFLOW PRO: dikkat gerekiyor" else "RAMFLOW PRO: attention needed", reasons.joinToString(" · "))
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(c: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ramflow_alerts", "RAMFLOW PRO", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, "ramflow_alerts")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(pi).setAutoCancel(true).build()
        NotificationManagerCompat.from(c).notify(1001, n)
    }
}

object AutoScheduler {
    private const val NAME = "ramflow_auto"
    fun start(c: Context, minutes: Int) {
        val req = PeriodicWorkRequestBuilder<AutoWorker>(minutes.toLong().coerceAtLeast(15), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
    fun stop(c: Context) { WorkManager.getInstance(c).cancelUniqueWork(NAME) }
}
