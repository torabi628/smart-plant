package ir.aminim.smartplant.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.engine.Advisor
import ir.aminim.smartplant.knowledge.Light
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.knowledge.Tag
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.util.fa
import kotlin.math.log10

/* ═════════════════════════  مشاور خرید  ═════════════════════════ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvisorScreen(vm: MainViewModel, onBack: () -> Unit, onPlant: (PlantInfo) -> Unit, onLight: () -> Unit) {
    val a = vm.advisor
    val picks = remember(a) { Advisor.recommend(a) }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("مشاور خرید گیاه", onBack, subtitle = "چه گیاهی برای خانه و شرایط من مناسب است؟")
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Question("۱. نور محل نگهداری", Icons.Rounded.WbSunny, Palette.Gold) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..4).forEach { l -> SelectChip(Light.names[l], a.light == l, { vm.advisor = a.copy(light = l) }, color = Palette.Gold) }
                    }
                    TextButton(onLight) { Icon(Icons.Rounded.Sensors, null, tint = Palette.Teal); Spacer(Modifier.width(6.dp)); Text("نمی‌دانم؛ با نورسنج اندازه بگیر", color = Palette.Teal) }
                }
                Question("۲. چقدر وقت مراقبت دارید؟", Icons.Rounded.Schedule, Palette.Sky) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("کم / سفر زیاد", "متوسط", "زیاد").forEachIndexed { i, t -> SelectChip(t, a.time == i + 1, { vm.advisor = a.copy(time = i + 1) }, color = Palette.Sky) }
                    }
                }
                Question("۳. تجربه نگهداری گیاه", Icons.Rounded.School, Palette.Violet) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("مبتدی", "متوسط", "حرفه‌ای").forEachIndexed { i, t -> SelectChip(t, a.skill == i + 1, { vm.advisor = a.copy(skill = i + 1) }, color = Palette.Violet) }
                    }
                }
                Question("۴. حیوان خانگی یا کودک خردسال دارید؟", Icons.Rounded.Pets, Palette.Pink) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("بله؛ فقط گیاه بی‌خطر", a.pets, { vm.advisor = a.copy(pets = true) }, color = Palette.Pink)
                        SelectChip("خیر", !a.pets, { vm.advisor = a.copy(pets = false) }, color = Palette.Pink)
                    }
                }
                Question("۵. اندازه دلخواه", Icons.Rounded.Straighten, Palette.Teal) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("مهم نیست", "کوچک و رومیزی", "متوسط", "بزرگ و ایستاده").forEachIndexed { i, t -> SelectChip(t, a.size == i, { vm.advisor = a.copy(size = i) }, color = Palette.Teal) }
                    }
                }
                Question("۶. هدف شما (چند انتخابی)", Icons.Rounded.Flag, Palette.Leaf) {
                    val goals = listOf(Tag.AIR, Tag.FLOWER, Tag.GIFT, Tag.BEDROOM, Tag.OFFICE, Tag.HANGING, Tag.BIG)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        goals.forEach { g -> val on = g in a.goals; SelectChip(g.title, on, { vm.advisor = a.copy(goals = if (on) a.goals - g else a.goals + g) }) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("پیشنهاد برای شما (${picks.size.fa()} گیاه)")
                Spacer(Modifier.height(8.dp))
                if (picks.isEmpty()) GlassCard(Modifier.fillMaxWidth()) {
                    Text("با این شرایط گیاه مناسبی در دانشنامه پیدا نشد. نور بیشتر یا شرایط آسان‌تر را امتحان کنید یا از مشاوره هوشمند بپرسید.", color = Palette.TextMuted)
                }
                picks.forEachIndexed { i, pk ->
                    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onPlant(pk.plant) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(catIcon(pk.plant.cat), catBrush(pk.plant.cat), size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (i == 0) { Icon(Icons.Rounded.EmojiEvents, null, tint = Palette.Gold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }
                                    Text(pk.plant.fa, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                                }
                                Text(pk.reasons.take(3).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = Palette.Leaf, maxLines = 2)
                                if (pk.warnings.isNotEmpty()) Text(pk.warnings.first(), style = MaterialTheme.typography.labelSmall, color = Palette.Orange, maxLines = 1)
                            }
                            Text("${pk.score.fa()}٪", style = MaterialTheme.typography.titleMedium, color = Palette.Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(6.dp))
                GoldButton("مشاوره هوشمند با هوش مصنوعی", { vm.askAdvisorAi() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome, loading = vm.advisorBusy)
                vm.advisorAi?.let {
                    Spacer(Modifier.height(12.dp))
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = Palette.Gold); Spacer(Modifier.width(8.dp))
                            Text("نظر کارشناس هوشمند", style = MaterialTheme.typography.titleSmall, color = Palette.Gold)
                        }
                        Spacer(Modifier.height(8.dp))
                        RichText(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun Question(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
    }
    Spacer(Modifier.height(8.dp))
    Column(content = content)
}

/* ═════════════════════════  نورسنج  ═════════════════════════ */
fun luxLevel(lux: Float): Int = when { lux < 1000 -> 1; lux < 5000 -> 2; lux < 15000 -> 3; else -> 4 }

@Composable
fun LightMeterScreen(vm: MainViewModel, onBack: () -> Unit, onPlant: (PlantInfo) -> Unit, onUse: (Int) -> Unit) {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var lux by remember { mutableFloatStateOf(0f) }
    var peak by remember { mutableFloatStateOf(0f) }
    DisposableEffect(sensor) {
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { lux = lux * 0.7f + e.values[0] * 0.3f; if (lux > peak) peak = lux }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sensor?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sm.unregisterListener(l) }
    }
    val level = luxLevel(lux)
    val colors = listOf(Palette.TextMuted, Palette.Violet, Palette.Teal, Palette.Leaf, Palette.Gold)
    val anim by animateFloatAsState(((log10(lux.coerceAtLeast(1f)) / 5f).coerceIn(0f, 1f)), tween(400), label = "lux")
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("نورسنج گیاه", onBack, subtitle = "حسگر نور گوشی شما")
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (sensor == null) {
                    EmptyState(Icons.Rounded.SensorsOff, "این گوشی حسگر نور ندارد", "از جدول راهنمای نور در بخش آموزش استفاده کنید: فاصله تا پنجره و جهت آن را در نظر بگیرید.")
                } else {
                    Spacer(Modifier.height(10.dp))
                    val c = colors[level]
                    val track = Palette.GlassStrong
                    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val sw = 22f
                            val arc = Size(size.width - sw, size.height - sw)
                            drawArc(track, 135f, 270f, false, Offset(sw / 2, sw / 2), arc, style = Stroke(sw, cap = StrokeCap.Round))
                            drawArc(Brush.sweepGradient(listOf(Color(0xFF6A5AF0), Color(0xFF16929A), Color(0xFF2FA05A), Color(0xFFEDB850), Color(0xFF6A5AF0))), 135f, 270f * anim, false, Offset(sw / 2, sw / 2), arc, style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.WbSunny, null, tint = c, modifier = Modifier.size(34.dp))
                            Text(lux.toInt().toString().fa(), style = MaterialTheme.typography.displayMedium, color = c, fontWeight = FontWeight.Black)
                            Text("لوکس", style = MaterialTheme.typography.labelLarge, color = Palette.TextMuted)
                        }
                    }
                    Text(Light.names[level], style = MaterialTheme.typography.headlineSmall, color = c, fontWeight = FontWeight.Bold)
                    Text("بیشترین مقدار ثبت‌شده: ${peak.toInt().toString().fa()} لوکس", style = MaterialTheme.typography.labelMedium, color = Palette.TextFaint)
                    Spacer(Modifier.height(14.dp))
                    GlassCard(Modifier.fillMaxWidth()) {
                        SubTitle("روش درست اندازه‌گیری", Icons.Rounded.Info, Palette.Sky)
                        BulletList(
                            listOf(
                                "گوشی را دقیقاً جای گیاه بگذارید و صفحه را رو به پنجره (منبع نور) بگیرید.",
                                "در روشن‌ترین ساعت روز (حدود ۱۰ صبح تا ۲ بعدازظهر) اندازه بگیرید.",
                                "حسگر معمولاً بالای صفحه کنار دوربین جلوست؛ آن را با دست نپوشانید.",
                                "عدد تقریبی است؛ برای مقایسه نقاط مختلف خانه عالی است.",
                            ), dot = Palette.Sky,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GoldButton("از این نور در مشاور خرید استفاده کن", { onUse(level) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Storefront)
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("گیاهان مناسب این نقطه")
                    Spacer(Modifier.height(8.dp))
                    Advisor.forLight(level).take(10).forEach { p -> PlantRow(p) { onPlant(p) }; Spacer(Modifier.height(10.dp)) }
                }
                Spacer(Modifier.height(10.dp))
                Text("راهنما: کم‌نور کمتر از ۱۰۰۰ • متوسط ۱۰۰۰ تا ۵۰۰۰ • روشن غیرمستقیم ۵۰۰۰ تا ۱۵۰۰۰ • آفتاب مستقیم بیش از ۱۵۰۰۰ لوکس".fa(),
                    style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint, textAlign = TextAlign.Center)
            }
        }
    }
}
