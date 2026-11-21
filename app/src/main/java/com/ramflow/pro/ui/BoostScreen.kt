package com.ramflow.pro.ui

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
fun StepRow(step: BoostStep) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (step.status) {
                StepStatus.DONE -> Icon(Icons.Rounded.CheckCircle, null, tint = Green)
                StepStatus.RUNNING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                StepStatus.USER_ACTION -> Icon(Icons.Rounded.Info, null, tint = Orange)
                StepStatus.BLOCKED -> Icon(Icons.Rounded.Block, null, tint = Red)
                StepStatus.PENDING -> Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = Color.Gray)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(step.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (step.detail.isNotEmpty()) Text(step.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.65f))
            val tag = when (step.status) {
                StepStatus.USER_ACTION -> tr("Needs your action", "Senin işlemin gerekli")
                StepStatus.BLOCKED -> tr("Not possible (Android restriction)", "Yapılamadı (Android kısıtı)")
                else -> null
            }
            if (tag != null) Text(tag, fontSize = 11.sp, color = if (step.status == StepStatus.BLOCKED) Red else Orange, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BoostScreen(vm: MainViewModel, nav: (String) -> Unit) {
    val rep = vm.boostReport
    val ram = vm.snapshot.ram
    val running = vm.boostSteps.firstOrNull { it.status == StepStatus.RUNNING }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar("BOOST") { nav("home") } }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Ring(if (rep != null) 1f else vm.boostProgress, Modifier.size(200.dp)) {
                        if (rep != null) Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.size(64.dp))
                        else Text("${(vm.boostProgress * 100).toInt()}%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        rep != null -> tr("OPTIMIZATION COMPLETE", "OPTİMİZASYON TAMAMLANDI")
                        vm.boostError != null -> vm.boostError!!
                        running != null -> running.title
                        else -> tr("Starting…", "Başlıyor…")
                    },
                    Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Metric(fmtBytes(ram.used), tr("Used (live)", "Kullanılan (canlı)"))
                    Metric(fmtBytes(ram.avail), tr("Available (live)", "Boş (canlı)"))
                }
            }
        }
        items(vm.boostSteps) { StepRow(it) }
        if (rep != null) {
            item {
                val diff = rep.after.avail - rep.before.avail
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle(Icons.Rounded.CheckCircle, tr("OPTIMIZATION COMPLETE", "OPTİMİZASYON TAMAMLANDI"))
                    StatRow(tr("RAM before (available)", "Önce RAM (boş)"), fmtBytes(rep.before.avail))
                    StatRow(tr("RAM after (available)", "Sonra RAM (boş)"), fmtBytes(rep.after.avail))
                    StatRow(tr("Available memory difference", "Boş bellek farkı"), (if (diff >= 0) "+" else "-") + fmtBytes(kotlin.math.abs(diff)))
                    rep.ramAfterManual?.let {
                        val d2 = it - rep.before.avail
                        StatRow(tr("After your manual actions", "Manuel işlemlerinden sonra"), "${fmtBytes(it)} (${if (d2 >= 0) "+" else "-"}${fmtBytes(kotlin.math.abs(d2))})")
                    }
                    StatRow(tr("Apps analyzed", "Analiz edilen uygulama"), "${rep.analyzed}")
                    StatRow(tr("Actions completed", "Tamamlanan işlem"), "${rep.completed}")
                    StatRow(tr("Actions needing user interaction", "Kullanıcı etkileşimi gereken"), "${rep.userActions}")
                    StatRow(tr("Operations blocked by Android", "Android'in engellediği işlem"), "${rep.blocked}")
                    StatRow(tr("Background-trim requests sent", "Gönderilen arka plan isteği"), "${rep.trimSent}")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("Background-trim requests were only sent to Android; the system decides what is really freed. Apps are not shown as 'stopped' unless you force-stop them yourself.",
                            "Arka plan istekleri yalnızca Android'e iletildi; gerçekte neyin kapanacağına sistem karar verir. Sen zorla durdurmadıkça hiçbir uygulama 'kapatıldı' olarak gösterilmez."),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                    )
                    Spacer(Modifier.height(10.dp))
                    if (rep.candidates.isNotEmpty()) PillButton(
                        tr("Manage ${rep.candidates.size} apps manually", "${rep.candidates.size} uygulamayı manuel yönet"),
                        { vm.selectCandidates(rep.candidates); vm.appFilter = 1; nav("apps") }, Modifier.fillMaxWidth()
                    )
                    PillButton(tr("Close", "Kapat"), { nav("home") }, Modifier.fillMaxWidth(), filled = false)
                }
            }
        }
    }
}
