package com.ramflow.pro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AmbientBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = isDarkTheme()
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                if (dark) listOf(Color(0xFF0B1220), Color(0xFF101C33)) else listOf(Color(0xFFF7FBFF), Color(0xFFDCEBFF))
            )
        )
    ) {
        Box(Modifier.offset((-80).dp, (-60).dp).size(280.dp).blur(80.dp).background(Blue.copy(alpha = if (dark) 0.35f else 0.28f), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(70.dp, 60.dp).size(300.dp).blur(90.dp).background(Color(0xFF7FD6FF).copy(alpha = 0.3f), CircleShape))
        content()
    }
}

@Composable
fun Modifier.glass(shape: RoundedCornerShape = RoundedCornerShape(26.dp)): Modifier {
    val dark = isDarkTheme()
    return this
        .shadow(14.dp, shape, ambientColor = Color.White, spotColor = Blue.copy(alpha = 0.25f))
        .clip(shape)
        .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.68f))
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.25f else 0.95f), Color.White.copy(alpha = 0.15f))),
            shape
        )
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val base = modifier.glass()
    Column(if (onClick != null) base.clickable(onClick = onClick).padding(18.dp) else base.padding(18.dp), content = content)
}

@Composable
fun Ring(progress: Float, modifier: Modifier = Modifier.size(180.dp), stroke: Dp = 16.dp, content: @Composable BoxScope.() -> Unit) {
    val a by animateFloatAsState(progress.coerceIn(0f, 1f), if (LocalAnim.current) tween(600) else snap(), label = "ring")
    val track = if (isDarkTheme()) Color.White.copy(0.1f) else Blue.copy(0.12f)
    Box(modifier, Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2)) {
            drawArc(track, 0f, 360f, false, style = Stroke(stroke.toPx(), cap = StrokeCap.Round))
            drawArc(
                Brush.linearGradient(listOf(BlueLight, Blue)), -90f, 360f * a, false,
                style = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            )
        }
        content()
    }
}

@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier, max: Float = 100f) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val w = size.width; val h = size.height
        val step = w / (values.size - 1)
        val line = Path(); val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i * step; val y = h - (v / max).coerceIn(0f, 1f) * h
            if (i == 0) { line.moveTo(x, y); fill.moveTo(x, h); fill.lineTo(x, y) } else { line.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo((values.size - 1) * step, h); fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(Blue.copy(0.35f), Color.Transparent)))
        drawPath(line, Blue, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(0.65f), fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = true) {
    if (filled) Button(onClick, modifier, enabled, shape = RoundedCornerShape(50)) { Text(text, fontWeight = FontWeight.SemiBold) }
    else OutlinedButton(onClick, modifier, enabled, shape = RoundedCornerShape(50)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Text(
        text, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(color.copy(0.14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

fun fmtBytes(b: Long): String {
    if (b < 0) return "—"
    val gb = b / 1024.0 / 1024 / 1024
    return if (gb >= 1) String.format(Locale.US, "%.2f GB", gb) else String.format(Locale.US, "%.0f MB", b / 1024.0 / 1024)
}

fun fmtDuration(ms: Long, lang: String): String {
    val m = ms / 60000
    val h = if (lang == "tr") "sa" else "h"; val mn = if (lang == "tr") "dk" else "min"
    return if (m >= 60) "${m / 60} $h ${m % 60} $mn" else "$m $mn"
}

fun ago(ts: Long, lang: String): String {
    if (ts <= 0) return "—"
    val m = (System.currentTimeMillis() - ts) / 60000
    val tr = lang == "tr"
    return when {
        m < 1 -> if (tr) "az önce" else "just now"
        m < 60 -> if (tr) "$m dk önce" else "$m min ago"
        m < 1440 -> if (tr) "${m / 60} sa önce" else "${m / 60} h ago"
        else -> if (tr) "${m / 1440} g önce" else "${m / 1440} d ago"
    }
}
