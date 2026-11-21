package com.ramflow.pro.ui

import androidx.activity.compose.BackHandler
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

fun launchExternal(ctx: Context, vm: MainViewModel, intent: Intent) {
    try {
        vm.markLeft()
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Unavailable", Toast.LENGTH_SHORT).show()
    }
}

fun appInfoIntent(pkg: String) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))

private val tabs = listOf("home", "perf", "apps", "cleanup", "settings")

@Composable
fun App(vm: MainViewModel) {
    var route by rememberSaveable { mutableStateOf("home") }
    BackHandler(enabled = route != "home") { route = "home" }
    Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
        AmbientBackground {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    when (route) {
                        "home" -> HomeScreen(vm) { route = it }
                        "perf" -> PerfScreen(vm) { route = it }
                        "apps" -> AppsScreen(vm)
                        "cleanup" -> CleanupScreen(vm)
                        "settings" -> SettingsScreen(vm) { route = it }
                        "boost" -> BoostScreen(vm) { route = it }
                        "auto" -> AutoScreen(vm) { route = it }
                        "history" -> HistoryScreen(vm) { route = it }
                    }
                }
                if (route in tabs) BottomBar(route) { route = it } else Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
fun BottomBar(route: String, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("home", Icons.Rounded.Home, tr("Home", "Ana Sayfa")),
        Triple("perf", Icons.Rounded.Speed, tr("Performance", "Performans")),
        Triple("apps", Icons.Rounded.Apps, tr("Apps", "Uygulamalar")),
        Triple("cleanup", Icons.Rounded.CleaningServices, tr("Cleanup", "Temizlik")),
        Triple("settings", Icons.Rounded.Settings, tr("Settings", "Ayarlar"))
    )
    Box(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp)).padding(vertical = 10.dp)) {
            items.forEach { (r, icon, label) ->
                val sel = r == route
                val tint = if (sel) Blue else MaterialTheme.colorScheme.onBackground.copy(0.55f)
                Column(
                    Modifier.weight(1f).clickable { onSelect(r) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                    Text(label, fontSize = 10.sp, color = tint, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}
