package com.ramflow.pro.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import com.ramflow.pro.MainViewModel
import com.ramflow.pro.data.*

@Composable
fun thermalName(l: Int): String = when (l) {
    0 -> tr("Normal", "Normal"); 1 -> tr("Light", "Hafif"); 2 -> tr("Moderate", "Orta")
    3 -> tr("Severe", "Ciddi"); 4 -> tr("Critical", "Kritik"); 5 -> tr("Emergency", "Acil")
    6 -> tr("Shutdown", "Kapanma"); else -> tr("Not supported", "Desteklenmiyor")
}

@Composable
fun batteryStatusName(s: String): String = when (s) {
    "charging" -> tr("Charging", "Şarj oluyor"); "discharging" -> tr("Discharging", "Şarj olmuyor (pil kullanılıyor)")
    "full" -> tr("Full", "Dolu"); "not_charging" -> tr("Not charging", "Şarj edilmiyor"); else -> tr("Unknown", "Bilinmiyor")
}

@Composable
fun pluggedName(s: String): String = when (s) {
    "ac" -> tr("AC", "Duvar adaptörü"); "usb" -> "USB"; "wireless" -> tr("Wireless", "Kablosuz"); else -> tr("Not plugged", "Takılı değil")
}

private val LazyPad = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)

@Composable
fun MiniCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, big: String, small: String) {
    GlassCard(modifier) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
        Text(big, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(small, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
    }
}

@Composable
fun HomeScreen(vm: MainViewModel, nav: (String) -> Unit) {
    val s = vm.snapshot; val ram = s.ram
    LazyColumn(Modifier.fillMaxSize(), contentPadding = LazyPad, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("RAMFLOW PRO", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(s.model, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Memory, tr("Memory (RAM)", "Bellek (RAM)"))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Ring(ram.pct / 100f, Modifier.size(190.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${ram.pct.toInt()}%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                            Text(tr("in use", "kullanımda"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Metric(fmtBytes(ram.total), tr("Total", "Toplam"))
                    Metric(fmtBytes(ram.used), tr("Used", "Kullanılan"))
                    Metric(fmtBytes(ram.avail), tr("Available", "Boş"))
                }
                Spacer(Modifier.height(12.dp))
                LineChart(vm.ramSamples, Modifier.fillMaxWidth().height(80.dp))
                Text(tr("Live · last 60 s", "Canlı · son 60 sn"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.boost(); nav("boost") },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, null); Spacer(Modifier.width(8.dp))
                    Text("BOOST", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniCard(Modifier.weight(1f), Icons.Rounded.Speed, "CPU", "${s.cpu.cores} ${tr("cores", "çekirdek")}",
                    s.cpu.curMhz?.let { "$it MHz" } ?: s.cpu.abi)
                MiniCard(Modifier.weight(1f), Icons.Rounded.BatteryFull, tr("Battery", "Pil"), "${s.battery.pct}%",
                    s.battery.tempC?.let { String.format(Locale.US, "%.1f °C", it) } ?: batteryStatusName(s.battery.status))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniCard(Modifier.weight(1f), Icons.Rounded.Thermostat, tr("Thermal", "Isı"), thermalName(s.thermal), tr("system status", "sistem durumu"))
                MiniCard(Modifier.weight(1f), Icons.Rounded.Storage, tr("Storage", "Depolama"), fmtBytes(s.storage.free), tr("free", "boş"))
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Info, tr("System", "Sistem"))
                StatRow(tr("Android", "Android"), s.androidVersion)
                StatRow(tr("Device", "Cihaz"), s.model)
                StatRow(tr("Storage", "Depolama"), "${fmtBytes(s.storage.free)} / ${fmtBytes(s.storage.total)}")
                StatRow(tr("Thermal status", "Termal durum"), thermalName(s.thermal))
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { nav("auto") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Autorenew, null, tint = Blue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Auto Optimization", "Otomatik Optimizasyon"), fontWeight = FontWeight.Bold)
                        Text(
                            if (vm.autoEnabled && vm.autoRunning) tr("AUTO ON · monitoring", "AUTO AÇIK · izleniyor")
                            else if (vm.autoEnabled) tr("AUTO ON · stopped", "AUTO AÇIK · durdu") else tr("AUTO OFF", "AUTO KAPALI"),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun PerfScreen(vm: MainViewModel, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val s = vm.snapshot; val ram = s.ram; val c = s.cpu; val b = s.battery
    LazyColumn(Modifier.fillMaxSize(), contentPadding = LazyPad, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar(tr("Performance Center", "Performans Merkezi")) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Memory, tr("RAM Optimizer", "RAM Optimize Edici"))
                StatRow(tr("Total", "Toplam"), fmtBytes(ram.total))
                StatRow(tr("Used", "Kullanılan"), "${fmtBytes(ram.used)} (${ram.pct.toInt()}%)")
                StatRow(tr("Available", "Boş"), fmtBytes(ram.avail))
                StatRow(tr("Low-memory threshold", "Düşük bellek eşiği"), fmtBytes(ram.threshold))
                StatRow(tr("Memory pressure", "Bellek baskısı"), if (ram.low) tr("LOW MEMORY", "DÜŞÜK BELLEK") else tr("Normal", "Normal"))
                LineChart(vm.ramSamples, Modifier.fillMaxWidth().height(70.dp).padding(vertical = 6.dp))
                PillButton("BOOST", { vm.boost(); nav("boost") }, Modifier.fillMaxWidth())
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Speed, tr("CPU Monitor", "CPU İzleyici"))
                StatRow(tr("Cores", "Çekirdek"), "${c.cores}")
                StatRow(tr("Architecture", "Mimari"), c.abi)
                StatRow(tr("Hardware", "Donanım"), c.hardware)
                StatRow(tr("Current frequency", "Anlık frekans"), c.curMhz?.let { "$it MHz" } ?: tr("Not accessible", "Erişilemiyor"))
                StatRow(tr("Max frequency", "Maks. frekans"), c.maxMhz?.let { "$it MHz" } ?: tr("Not accessible", "Erişilemiyor"))
                StatRow(tr("RAMFLOW CPU load", "RAMFLOW CPU yükü"), "${c.appLoadPct.toInt()}%")
                LineChart(vm.cpuSamples, Modifier.fillMaxWidth().height(60.dp).padding(vertical = 6.dp))
                Text(
                    tr("Android 8+ blocks system-wide CPU usage for apps, so only this app's own load is measured.",
                        "Android 8+ sistem genelindeki CPU kullanımını uygulamalara kapatır; yalnızca bu uygulamanın kendi yükü ölçülür."),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.55f)
                )
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Thermostat, tr("Thermal Monitor", "Isı İzleyici"))
                StatRow(tr("System thermal status", "Sistem termal durumu"), thermalName(s.thermal))
                StatRow(tr("Battery temperature", "Pil sıcaklığı"), b.tempC?.let { String.format(Locale.US, "%.1f °C", it) } ?: tr("Not accessible", "Erişilemiyor"))
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.BatteryFull, tr("Battery Monitor", "Pil İzleyici"))
                StatRow(tr("Level", "Seviye"), "${b.pct}%")
                StatRow(tr("Status", "Durum"), batteryStatusName(b.status))
                StatRow(tr("Power source", "Güç kaynağı"), pluggedName(b.plugged))
                StatRow(tr("Voltage", "Voltaj"), "${b.voltageMv} mV")
                Spacer(Modifier.height(6.dp))
                PillButton(tr("Battery usage settings", "Pil kullanım ayarları"), {
                    launchExternal(ctx, vm, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                }, Modifier.fillMaxWidth(), filled = false)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { nav("cleanup") }) {
                SectionTitle(Icons.Rounded.Storage, tr("Storage Analyzer", "Depolama Analizi"))
                LinearProgressIndicator(progress = { s.storage.pct / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(8.dp))
                StatRow(tr("Used", "Kullanılan"), fmtBytes(s.storage.used))
                StatRow(tr("Free", "Boş"), fmtBytes(s.storage.free))
            }
        }
        item { NavRow(Icons.Rounded.Apps, tr("Background Manager", "Arka Plan Yöneticisi")) { vm.appFilter = 1; nav("apps") } }
        item { NavRow(Icons.Rounded.Apps, tr("App Manager", "Uygulama Yöneticisi")) { vm.appFilter = 0; nav("apps") } }
        item { NavRow(Icons.Rounded.History, tr("Performance History", "Performans Geçmişi")) { nav("history") } }
        item { NavRow(Icons.Rounded.Tune, tr("Optimization Settings", "Optimizasyon Ayarları")) { nav("settings") } }
    }
}

@Composable
fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue); Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}

@Composable
fun HistoryScreen(vm: MainViewModel, nav: (String) -> Unit) {
    val lang = LocalLang.current
    val fmt = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = LazyPad, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar(tr("Performance History", "Performans Geçmişi")) { nav("perf") } }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(Icons.Rounded.Memory, tr("RAM usage · this session", "RAM kullanımı · bu oturum"))
                LineChart(vm.ramSamples, Modifier.fillMaxWidth().height(100.dp))
                SectionTitle(Icons.Rounded.Speed, tr("RAMFLOW CPU load", "RAMFLOW CPU yükü"))
                LineChart(vm.cpuSamples, Modifier.fillMaxWidth().height(70.dp))
            }
        }
        item { Text(tr("Boost history", "Boost geçmişi"), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (vm.history.isEmpty()) item {
            Text(tr("No saved boosts yet.", "Henüz kayıtlı boost yok."), color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
        }
        items(vm.history) { h ->
            val d = h.availAfter - h.availBefore
            GlassCard(Modifier.fillMaxWidth()) {
                Text(fmt.format(Date(h.ts)), fontWeight = FontWeight.Bold)
                StatRow(tr("Available before", "Önce boş"), fmtBytes(h.availBefore))
                StatRow(tr("Available after", "Sonra boş"), fmtBytes(h.availAfter))
                StatRow(tr("Difference", "Fark"), (if (d >= 0) "+" else "-") + fmtBytes(kotlin.math.abs(d)))
                StatRow(tr("Apps analyzed / actions / need you", "Analiz / işlem / senden beklenen"), "${h.analyzed} / ${h.completed} / ${h.userActions}")
            }
        }
        if (vm.history.isNotEmpty()) item {
            PillButton(tr("Clear history", "Geçmişi temizle"), { vm.clearHistory() }, Modifier.fillMaxWidth(), filled = false)
        }
    }
}
