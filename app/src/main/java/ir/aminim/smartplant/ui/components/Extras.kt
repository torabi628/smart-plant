package ir.aminim.smartplant.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ir.aminim.smartplant.data.Images
import ir.aminim.smartplant.speech.Dictation
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.util.fa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/* ───────────── اشتراک‌گذاری و پیوندها ───────────── */
object ShareUtil {
    fun openUrl(c: Context, url: String) {
        try { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: ActivityNotFoundException) {}
    }

    fun shareText(c: Context, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        c.startActivity(Intent.createChooser(i, "اشتراک‌گذاری").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareApp(c: Context) = shareText(c,
        "«گیاه‌شناس هوشمند» از روی عکس، گیاه و بیماری‌اش را تشخیص می‌دهد و راه درمان را قدم‌به‌قدم می‌گوید؛ همراه با دانشنامه گیاهان، مشاور خرید، نورسنج و یادآور آبیاری.\nگروه پژوهشی مدیریت هوشمند امین\nhttps://cafebazaar.ir/app/ir.aminim.smartplant")
}

/* ───────────── تصویر از فایل ───────────── */
@Composable
fun PhotoThumb(path: String, modifier: Modifier = Modifier, maxSide: Int = 700, contentScale: ContentScale = ContentScale.Crop) {
    val bmp by produceState<Bitmap?>(null, path) { value = withContext(Dispatchers.IO) { Images.thumb(path, maxSide) } }
    Box(modifier.background(Palette.GlassStrong), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = contentScale) }
            ?: Icon(Icons.Rounded.Image, null, tint = Palette.TextFaint)
    }
}

/* ───────────── دکمه و فیلد ورودی صوتی ───────────── */
@Composable
fun rememberMicPermission(onGranted: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) onGranted() else Toast.makeText(ctx, "برای ورودی صوتی، اجازه میکروفون لازم است", Toast.LENGTH_LONG).show()
    }
    return {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) onGranted()
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
fun MicButton(listening: Boolean, level: Float, onClick: () -> Unit, size: Dp = 52.dp) {
    val pulse = rememberInfiniteTransition(label = "mic").animateFloat(1f, 1.18f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    val s = if (listening) pulse.value + level * 0.25f else 1f
    Box(
        Modifier.size(size).scale(s).clip(CircleShape)
            .background(if (listening) Palette.CoralBrush else Palette.LeafBrush).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic, "ورودی صوتی", tint = Color.White, modifier = Modifier.size(size * 0.5f)) }
}

/**
 * فیلد متنی با ورودی صوتی فارسی.
 * هنگام شنیدن، راهنمای بزرگ «بلند و شمرده صحبت کنید» نمایش داده می‌شود.
 */
@Composable
fun VoiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    newDictation: () -> Dictation,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
) {
    val dict = remember { newDictation() }
    var base by remember { mutableStateOf("") }
    DisposableEffect(Unit) { onDispose { dict.release() } }
    LaunchedEffect(dict.text, dict.listening) {
        if (dict.listening || dict.text.isNotBlank()) {
            val t = dict.text
            if (t.isNotBlank()) onValueChange(listOf(base, t).filter { it.isNotBlank() }.joinToString(" "))
        }
    }
    val start = rememberMicPermission { base = value; dict.clear(); dict.start() }
    Column(modifier) {
        AppTextField(
            value, onValueChange, label, placeholder = placeholder, singleLine = false, minLines = minLines,
            trailing = {
                Box(Modifier.padding(end = 6.dp)) {
                    MicButton(dict.listening, dict.level, { if (dict.listening) dict.stop() else start() }, 44.dp)
                }
            },
        )
        AnimatedVisibility(dict.listening) {
            Column {
                Spacer(Modifier.height(10.dp))
                SpeakHint()
                Spacer(Modifier.height(8.dp))
                VoiceWave(dict.level)
            }
        }
        dict.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Palette.Coral, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun VoiceWave(level: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "w").animateFloat(0f, 6.28f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "t")
    val color = Palette.Leaf
    Canvas(modifier.fillMaxWidth().height(40.dp)) {
        val n = 32
        val w = size.width / n
        for (i in 0 until n) {
            val amp = (0.15f + level * 0.85f) * (0.5f + 0.5f * kotlin.math.sin(t.value + i * 0.55f))
            val h = size.height * amp.coerceIn(0.08f, 1f)
            drawLine(color, Offset(i * w + w / 2, (size.height - h) / 2), Offset(i * w + w / 2, (size.height + h) / 2), strokeWidth = w * 0.5f, cap = StrokeCap.Round)
        }
    }
}

/* ───────────── متن با قالب‌بندی ساده (Markdown سبک) ───────────── */
@Composable
fun RichText(text: String, color: Color = Palette.Text, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.isBlank() -> Spacer(Modifier.height(4.dp))
                trimmed.startsWith("#") -> Text(inline(trimmed.trimStart('#').trim()), style = MaterialTheme.typography.titleMedium, color = Palette.Gold)
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> Row {
                    Box(Modifier.padding(top = 10.dp).size(6.dp).clip(CircleShape).background(Palette.Leaf))
                    Spacer(Modifier.width(10.dp))
                    Text(inline(trimmed.drop(2)), style = MaterialTheme.typography.bodyMedium, color = color)
                }
                Regex("^[0-9۰-۹]+[.)-] ").containsMatchIn(trimmed) -> {
                    val num = trimmed.substringBefore(' ').trimEnd('.', ')', '-')
                    Row {
                        Text(num.fa() + ".", style = MaterialTheme.typography.bodyMedium, color = Palette.Gold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(inline(trimmed.substringAfter(' ')), style = MaterialTheme.typography.bodyMedium, color = color)
                    }
                }
                else -> Text(inline(trimmed), style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }
}

private fun inline(s: String) = buildAnnotatedString {
    val parts = s.split("**")
    parts.forEachIndexed { i, p -> if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p) }
}

/* ───────────── فهرست گلوله‌ای / شماره‌دار ───────────── */
@Composable
fun BulletList(items: List<String>, numbered: Boolean = false, dot: Color = Palette.Leaf, color: Color = Palette.Text) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.Top) {
                if (numbered) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(dot.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Text((i + 1).fa(), color = dot, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                } else {
                    Box(Modifier.padding(top = 9.dp).size(7.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(10.dp))
                }
                Text(s, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
            }
        }
    }
}

/* ───────────── کارت بازشونده ───────────── */
@Composable
fun ExpandCard(
    title: String,
    icon: ImageVector,
    brush: Brush,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyOpen: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "r")
    GlassCard(modifier.fillMaxWidth().animateContentSize(), padding = 16.dp) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, brush, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 2) }
            }
            trailing?.invoke()
            Icon(Icons.Rounded.ExpandMore, null, tint = Palette.TextMuted, modifier = Modifier.rotate(rot))
        }
        if (open) {
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/* ───────────── حلقه امتیاز سلامت ───────────── */
@Composable
fun ScoreRing(score: Int, color: Color, size: Dp = 92.dp, label: String = "سلامت") {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) { anim.animateTo(score / 100f, tween(1100, easing = FastOutSlowInEasing)) }
    val track = Palette.GlassStrong
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = this.size.minDimension * 0.1f
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, Offset(sw / 2, sw / 2), arc, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * anim.value, false, Offset(sw / 2, sw / 2), arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text((anim.value * 100).toInt().fa(), style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
        }
    }
}

/** نوار درصد افقی */
@Composable
fun PercentBar(value: Int, color: Color, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(value / 100f, tween(900), label = "pb")
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.GlassStrong)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

/** رنگ وضعیت سلامت */
fun statusColor(status: String): Color = when (status) {
    "healthy" -> Palette.Leaf; "mild" -> Palette.Gold; "moderate" -> Palette.Orange; "severe" -> Palette.Coral; else -> Palette.TextMuted
}

/** نشان سطح (مثلاً نور یا سختی) با نقطه‌ها */
@Composable
fun LevelDots(level: Int, max: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { i -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (i < level) color else Palette.GlassStrong)) }
    }
}

/** کاشی کوچک اطلاعات (آیکن، عنوان، مقدار) */
@Composable
fun InfoTile(icon: ImageVector, title: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Palette.Glass).border(1.dp, Palette.GlassBorder, RoundedCornerShape(18.dp)).padding(12.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
        Text(value, style = MaterialTheme.typography.labelLarge, color = Palette.Text, maxLines = 2)
    }
}

/** پیام خالی */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, Palette.LeafBrush, size = 72.dp, glow = Palette.Leaf)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = TextAlign.Center)
    }
}

/** چیپ انتخابی */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = Palette.Leaf, icon: ImageVector? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else Palette.Glass)
            .border(1.dp, if (selected) color else Palette.GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) { Icon(Icons.Rounded.Check, null, tint = color, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        else icon?.let { Icon(it, null, tint = Palette.TextMuted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) color else Palette.Text, fontSize = 13.sp)
    }
}
