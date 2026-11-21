package com.ramflow.pro.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.ramflow.pro.MainViewModel
import com.ramflow.pro.data.*

private val Pad = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)

@Composable
fun UsageBanner(vm: MainViewModel) {
    val ctx = LocalContext.current
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Rounded.Lock, tr("Usage access needed", "Kullanım erişimi gerekli"))
        Text(
            tr("To show last-used time, foreground time, app size and cache, grant 'Usage access' to RAMFLOW PRO. Data never leaves your device.",
                "Son kullanım, ön plan süresi, uygulama boyutu ve önbellek bilgisi için RAMFLOW PRO'ya 'Kullanım erişimi' ver. Veriler cihazdan çıkmaz."),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        PillButton(tr("Open settings", "Ayarları aç"), {
            launchExternal(ctx, vm, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }, Modifier.fillMaxWidth())
    }
}

@Composable
fun AppsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val lang = LocalLang.current
    LaunchedEffect(Unit) { if (vm.apps.isEmpty()) vm.refreshApps() }
    val now = System.currentTimeMillis()
    val list = when (vm.appFilter) {
        1 -> vm.apps.filter { !it.isSystem && it.lastUsed > 0 && now - it.lastUsed < 24 * 3600_000L }.sortedByDescending { it.lastUsed }
        2 -> vm.apps.sortedByDescending { it.totalBytes }
        else -> vm.apps
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = Pad, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TopBar(tr("Apps", "Uygulamalar")) }
                IconButton(onClick = { vm.refreshApps() }) { Icon(Icons.Rounded.Refresh, null) }
            }
        }
        if (!vm.usageAccess) item { UsageBanner(vm) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(vm.appFilter == 0, { vm.appFilter = 0 }, label = { Text(tr("All", "Tümü")) })
                FilterChip(vm.appFilter == 1, { vm.appFilter = 1 }, label = { Text(tr("Background (24h)", "Arka plan (24sa)")) })
                FilterChip(vm.appFilter == 2, { vm.appFilter = 2 }, label = { Text(tr("Largest", "En büyük")) })
            }
        }
        if (vm.appsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (vm.manageQueue.isNotEmpty()) item {
            val next = vm.manageQueue.first()
            val label = vm.apps.firstOrNull { it.pkg == next }?.label ?: next
            GlassCard(Modifier.fillMaxWidth()) {
                Text(tr("Manage queue: ${vm.manageQueue.size} left", "Yönetim sırası: ${vm.manageQueue.size} kaldı"), fontWeight = FontWeight.Bold)
                Text(tr("Next: $label. Tap 'Force stop' on the App Info screen, then come back.", "Sıradaki: $label. Uygulama Bilgisi'nde 'Zorla durdur'a bas, sonra geri dön."), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(tr("OPEN APP INFO", "UYGULAMA BİLGİSİNİ AÇ"), {
                        val p = vm.manageQueue.removeAt(0); launchExternal(ctx, vm, appInfoIntent(p))
                    }, Modifier.weight(1f))
                    PillButton(tr("CANCEL", "İPTAL"), { vm.manageQueue.clear() }, filled = false)
                }
            }
        }
        if (vm.selected.isNotEmpty()) item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(tr("${vm.selected.size} selected", "${vm.selected.size} seçili"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("MANAGE", { vm.startManage() }, Modifier.weight(1f))
                    PillButton(tr("CLEAR", "TEMİZLE"), { vm.selected.clear() }, filled = false)
                }
            }
        }
        items(list, key = { it.pkg }) { a ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (a.icon != null) Image(a.icon, null, Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)))
                    else Box(Modifier.size(44.dp).background(Blue.copy(0.2f), RoundedCornerShape(11.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.pkg, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    }
                    Checkbox(a.pkg in vm.selected, { vm.toggleSelect(a.pkg) })
                }
                Spacer(Modifier.height(6.dp))
                val dim = MaterialTheme.colorScheme.onBackground.copy(0.65f)
                if (vm.usageAccess) {
                    Text(tr("Last used: ${ago(a.lastUsed, lang)} · Foreground (7d): ${fmtDuration(a.foregroundMs, lang)}",
                        "Son kullanım: ${ago(a.lastUsed, lang)} · Ön plan (7g): ${fmtDuration(a.foregroundMs, lang)}"), fontSize = 12.sp, color = dim)
                    Text(tr("App ${fmtBytes(a.appBytes)} · Data ${fmtBytes(a.dataBytes)} · Cache ${fmtBytes(a.cacheBytes)}",
                        "Uygulama ${fmtBytes(a.appBytes)} · Veri ${fmtBytes(a.dataBytes)} · Önbellek ${fmtBytes(a.cacheBytes)}"), fontSize = 12.sp, color = dim)
                }
                Text(tr("RAM / battery per app: not accessible to third-party apps (Android restriction)",
                    "Uygulama başı RAM / pil: üçüncü taraf uygulamalara kapalı (Android kısıtı)"), fontSize = 11.sp, color = dim)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (a.pkg in vm.trimRequested) StatusChip(tr("Trim requested · unverified", "İstek gönderildi · doğrulanamaz"), Orange)
                    else StatusChip(tr("Identified only", "Yalnızca tespit"), Color.Gray)
                    if (a.isSystem) StatusChip(tr("System", "Sistem"), Blue)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("OPEN", {
                        val i = ctx.packageManager.getLaunchIntentForPackage(a.pkg)
                        if (i != null) launchExternal(ctx, vm, i)
                    }, Modifier.weight(1f), filled = false)
                    PillButton("APP INFO", { launchExternal(ctx, vm, appInfoIntent(a.pkg)) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CleanupScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s = vm.snapshot.storage
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshOwnCache(); if (vm.apps.isEmpty()) vm.refreshApps() }
    val totalCache = vm.apps.filter { it.cacheBytes > 0 }.sumOf { it.cacheBytes }
    val topLarge = vm.apps.sortedByDescending { it.totalBytes }.take(8)
    val topCache = vm.apps.filter { it.cacheBytes > 0 }.sortedByDescending { it.cacheBytes }.take(8)

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(tr("Clear RAMFLOW cache?", "RAMFLOW önbelleği silinsin mi?")) },
        text = { Text(tr("Only RAMFLOW PRO's own temporary files (${fmtBytes(vm.ownCache)}) will be deleted. No personal files are touched.",
            "Yalnızca RAMFLOW PRO'nun kendi geçici dosyaları (${fmtBytes(vm.ownCache)}) silinecek. Kişisel dosyalara dokunulmaz.")) },
        confirmButton = { TextButton({ vm.clearOwnCache(); confirm = false }) { Text(tr("Delete", "Sil")) } },
        dismissButton = { TextButton({ confirm = false }) { Text(tr("Cancel", "Vazgeç")) } }
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = Pad, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar(tr("Smart Cleanup", "Akıllı Temizlik")) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Storage, tr("Storage analysis", "Depolama analizi"))
                LinearProgressIndicator(progress = { s.pct / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)))
                Spacer(Modifier.height(8.dp))
                StatRow(tr("Total", "Toplam"), fmtBytes(s.total))
                StatRow(tr("Used", "Kullanılan"), "${fmtBytes(s.used)} (${s.pct.toInt()}%)")
                StatRow(tr("Free", "Boş"), fmtBytes(s.free))
                Spacer(Modifier.height(6.dp))
                PillButton(tr("Open storage settings", "Depolama ayarlarını aç"), {
                    launchExternal(ctx, vm, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                }, Modifier.fillMaxWidth())
            }
        }
        if (!vm.usageAccess) item { UsageBanner(vm) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.CleaningServices, tr("Cache & temporary files", "Önbellek ve geçici dosyalar"))
                StatRow(tr("Cache of all apps (info)", "Tüm uygulama önbelleği (bilgi)"), if (vm.usageAccess) fmtBytes(totalCache) else "—")
                StatRow(tr("RAMFLOW own temp files", "RAMFLOW kendi geçici dosyaları"), fmtBytes(vm.ownCache))
                Text(
                    tr("Android does not let apps delete other apps' cache. Use App Info → Storage → Clear cache. Nothing is deleted without your confirmation.",
                        "Android, uygulamaların başka uygulamaların önbelleğini silmesine izin vermez. Uygulama Bilgisi → Depolama → Önbelleği temizle yolunu kullan. Onayın olmadan hiçbir şey silinmez."),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                )
                Spacer(Modifier.height(8.dp))
                PillButton(tr("Clear RAMFLOW temp files", "RAMFLOW geçici dosyalarını sil"), { confirm = true },
                    Modifier.fillMaxWidth(), enabled = vm.ownCache > 0, filled = false)
            }
        }
        if (vm.usageAccess) {
            item { Text(tr("Largest apps", "En büyük uygulamalar"), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            items(topLarge, key = { "L" + it.pkg }) { a -> CleanRow(vm, a, fmtBytes(a.totalBytes)) }
            item { Text(tr("Apps with most cache", "En çok önbellek tutanlar"), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            items(topCache, key = { "C" + it.pkg }) { a -> CleanRow(vm, a, fmtBytes(a.cacheBytes)) }
        }
    }
}

@Composable
private fun CleanRow(vm: MainViewModel, a: AppEntry, value: String) {
    val ctx = LocalContext.current
    GlassCard(Modifier.fillMaxWidth(), onClick = { launchExternal(ctx, vm, appInfoIntent(a.pkg)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (a.icon != null) Image(a.icon, null, Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(10.dp))
            Text(a.label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(value, fontWeight = FontWeight.Bold, color = Blue)
        }
    }
}
