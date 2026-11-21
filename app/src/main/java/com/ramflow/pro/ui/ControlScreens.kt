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

private val Pad2 = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)

fun requestNotif(ctx: Context, launch: (String) -> Unit) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) launch(Manifest.permission.POST_NOTIFICATIONS)
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (sub != null) Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
        }
        Switch(checked, onChange)
    }
}

@Composable
fun AutoScreen(vm: MainViewModel, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ram = vm.snapshot.ram
    val attention = ram.pct >= vm.alertPct || ram.low || vm.snapshot.thermal >= 3
    LazyColumn(Modifier.fillMaxSize(), contentPadding = Pad2, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar(tr("Auto Optimization", "Otomatik Optimizasyon")) { nav("home") } }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Autorenew, tr("Auto mode", "Otomatik mod"))
                Text(
                    if (!vm.autoEnabled) tr("AUTO OFF", "AUTO KAPALI")
                    else if (vm.autoRunning) tr("AUTO ON · monitoring every ${vm.intervalMin} min", "AUTO AÇIK · her ${vm.intervalMin} dk izleniyor")
                    else tr("AUTO ON · monitoring stopped", "AUTO AÇIK · izleme durdu"),
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("AUTO ON", { vm.setAuto(true); requestNotif(ctx) { perm.launch(it) } }, Modifier.weight(1f), filled = vm.autoEnabled)
                    PillButton("AUTO OFF", { vm.setAuto(false) }, Modifier.weight(1f), filled = !vm.autoEnabled)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("START", { vm.startAuto() }, Modifier.weight(1f), enabled = vm.autoEnabled && !vm.autoRunning)
                    PillButton("STOP", { vm.stopAuto() }, Modifier.weight(1f), enabled = vm.autoRunning, filled = false)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(tr("Check interval", "Kontrol aralığı"), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { m ->
                        FilterChip(vm.intervalMin == m, { vm.setInterval(m) }, label = { Text("$m ${tr("min", "dk")}") })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(tr("RAM alert threshold", "RAM uyarı eşiği"), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(80, 85, 90).forEach { p ->
                        FilterChip(vm.alertPct == p, { vm.setAlertPct(p) }, label = { Text("%$p") })
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Speed, tr("Right now", "Şu an"))
                StatRow("RAM", "${ram.pct.toInt()}% · ${fmtBytes(ram.avail)} ${tr("free", "boş")}")
                StatRow(tr("Thermal", "Isı"), thermalName(vm.snapshot.thermal))
                StatRow(tr("Attention needed", "Dikkat gerekli mi"), if (attention) tr("YES", "EVET") else tr("No", "Hayır"))
            }
        }
        item {
            Text(
                tr("Auto mode uses a light WorkManager check (minimum 15 min). It never kills apps and runs no permanent service; it only notifies you when attention is needed.",
                    "Otomatik mod hafif bir WorkManager kontrolü kullanır (en az 15 dk). Uygulama kapatmaz, kalıcı servis çalıştırmaz; yalnızca dikkat gerektiğinde bildirim gönderir."),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
            )
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = Pad2, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar(tr("Settings", "Ayarlar")) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Tune, tr("Appearance", "Görünüm"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(vm.themeMode == 0, { vm.setTheme(0) }, label = { Text(tr("System", "Sistem")) })
                    FilterChip(vm.themeMode == 1, { vm.setTheme(1) }, label = { Text(tr("Light Mode", "Açık Mod")) })
                    FilterChip(vm.themeMode == 2, { vm.setTheme(2) }, label = { Text(tr("Dark Mode", "Koyu Mod")) })
                }
                Spacer(Modifier.height(8.dp))
                Text(tr("Language", "Dil"), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(vm.lang == "tr", { vm.setLang("tr") }, label = { Text("Türkçe") })
                    FilterChip(vm.lang == "en", { vm.setLang("en") }, label = { Text("English") })
                }
                SwitchRow(tr("Animations", "Animasyonlar"), null, vm.animations) { vm.setAnimations(it) }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Bolt, tr("Optimization", "Optimizasyon"))
                SwitchRow(tr("Request background trim in Boost", "Boost'ta arka plan temizliği iste"),
                    tr("Uses Android's killBackgroundProcesses; result is up to the system.", "Android'in killBackgroundProcesses API'sini kullanır; sonucu sistem belirler."),
                    vm.trimEnabled) { vm.setTrim(it) }
                SwitchRow(tr("Notifications", "Bildirimler"), tr("Alerts from auto mode", "Otomatik mod uyarıları"), vm.notifications) {
                    vm.setNotifications(it); if (it) requestNotif(ctx) { p -> perm.launch(p) }
                }
                PillButton(tr("Auto Optimization settings", "Otomatik Optimizasyon ayarları"), { nav("auto") }, Modifier.fillMaxWidth(), filled = false)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Lock, tr("Privacy", "Gizlilik"))
                SwitchRow(tr("Save boost history on device", "Boost geçmişini cihazda sakla"), null, vm.saveHistory) { vm.setSaveHistory(it) }
                Text(
                    tr("RAMFLOW PRO has no internet permission. All data stays on your device. Permissions: Usage access (optional, granted by you), notifications, package list and background-process request.",
                        "RAMFLOW PRO'nun internet izni yoktur. Tüm veriler cihazında kalır. İzinler: Kullanım erişimi (isteğe bağlı, sen verirsin), bildirim, paket listesi ve arka plan süreç isteği."),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                )
                Spacer(Modifier.height(8.dp))
                PillButton(tr("Clear history", "Geçmişi sil"), { vm.clearHistory() }, Modifier.fillMaxWidth(), filled = false)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Info, tr("About RAMFLOW PRO", "RAMFLOW PRO Hakkında"))
                StatRow(tr("Version", "Sürüm"), "1.0.0")
                StatRow(tr("Package", "Paket"), "com.ramflow.pro")
                Text(
                    tr("A transparent RAM & performance monitor. No root, no fake cleaning: operations Android does not allow are shown as 'needs your action' or 'blocked'.",
                        "Şeffaf bir RAM ve performans izleyici. Root yok, sahte temizlik yok: Android'in izin vermediği işlemler 'senin işlemin gerekli' veya 'engellendi' olarak gösterilir."),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                )
            }
        }
    }
}
