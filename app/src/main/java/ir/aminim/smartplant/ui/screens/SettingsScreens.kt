package ir.aminim.smartplant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.ai.AiClient
import ir.aminim.smartplant.reminders.Reminders
import ir.aminim.smartplant.settings.AiMode
import ir.aminim.smartplant.settings.AppSettings
import ir.aminim.smartplant.settings.SpeechEngine
import ir.aminim.smartplant.speech.VoskModels
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.ui.theme.ThemeState
import ir.aminim.smartplant.util.fa

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onTheme: (String) -> Unit, onAbout: () -> Unit) {
    val ctx = LocalContext.current
    val s = vm.settings
    var tick by remember { mutableIntStateOf(0) } // بازترسیم پس از تغییر تنظیمات
    var mode by remember { mutableStateOf(s.aiMode) }
    var speech by remember { mutableStateOf(s.speechEngine) }
    var name by remember { mutableStateOf(s.userName) }
    var city by remember { mutableStateOf(s.city) }
    var provider by remember { mutableStateOf(s.cloudProvider) }
    var url by remember { mutableStateOf(s.cloudUrl) }
    var apiKey by remember { mutableStateOf(s.cloudKey) }
    var model by remember { mutableStateOf(s.cloudModel) }
    var free by remember { mutableStateOf(s.freeAi) }
    var reminders by remember { mutableStateOf(s.reminders) }
    var hour by remember { mutableFloatStateOf(s.reminderHour.toFloat()) }
    var confirmClear by remember { mutableStateOf(false) }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("تنظیمات", onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                SectionLabel("موتور هوش مصنوعی")
                Spacer(Modifier.height(8.dp))
                AiMode.entries.forEachIndexed { i, m ->
                    val sel = mode == m
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (sel) Palette.LeafTint else Palette.Glass)
                            .border(1.dp, if (sel) Palette.Leaf else Palette.GlassBorder, RoundedCornerShape(18.dp))
                            .clickable { mode = m; s.aiMode = m }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(if (sel) Palette.Leaf else Palette.GlassStrong), contentAlignment = Alignment.Center) {
                            Text((i + 1).fa(), color = if (sel) Color(0xFF06301A) else Palette.TextMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                            Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                            if (m == AiMode.INTERNAL) {
                                Text(
                                    if (!AiClient.internalReady) "کلید داخلی در این نسخه تنظیم نشده؛ خودکار از گزینه ۲ استفاده می‌شود"
                                    else "امروز: ${s.internalLeft.fa()} از ${AppSettings.INTERNAL_DAILY.fa()} تشخیص باقی‌مانده",
                                    style = MaterialTheme.typography.labelSmall, color = Palette.Gold,
                                )
                            }
                        }
                        RadioButton(sel, { mode = m; s.aiMode = m }, colors = RadioButtonDefaults.colors(selectedColor = Palette.Leaf))
                    }
                }
                if (mode == AiMode.PERSONAL) {
                    Spacer(Modifier.height(10.dp))
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("سرویس‌دهنده", style = MaterialTheme.typography.labelLarge, color = Palette.Text)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiClient.presets.forEach { p ->
                                SelectChip(p.title, provider == p.id, {
                                    provider = p.id; s.cloudProvider = p.id
                                    if (p.baseUrl.isNotBlank()) { url = p.baseUrl; s.cloudUrl = p.baseUrl; model = p.model; s.cloudModel = p.model }
                                })
                            }
                        }
                        AiClient.presets.firstOrNull { it.id == provider }?.let { p ->
                            Text(p.subtitle, style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted, modifier = Modifier.padding(top = 6.dp))
                            if (p.signupUrl.isNotBlank()) TextButton({ ShareUtil.openUrl(ctx, p.signupUrl) }) { Text("دریافت کلید از ${p.title}", color = Palette.Teal) }
                        }
                        AppTextField(url, { url = it; s.cloudUrl = it }, "نشانی API", placeholder = "https://…/v1", icon = Icons.Rounded.Link)
                        Spacer(Modifier.height(10.dp))
                        AppTextField(apiKey, { apiKey = it; s.cloudKey = it }, "کلید API", icon = Icons.Rounded.Key)
                        Spacer(Modifier.height(10.dp))
                        AppTextField(model, { model = it; s.cloudModel = it }, "مدل (باید تصویر بپذیرد)", placeholder = "gpt-4.1", icon = Icons.Rounded.Memory)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("سرویس رایگان عمومی به‌عنوان پشتیبان", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                        Text("اگر بقیه مسیرها پاسخ ندادند", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                    }
                    Switch(free, { free = it; s.freeAi = it }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Leaf))
                }
                Spacer(Modifier.height(8.dp))
                GlassButton(if (vm.testing) "در حال آزمون…" else "آزمون اتصال هوش مصنوعی", { if (!vm.testing) vm.testAi() }, Modifier.fillMaxWidth(), Icons.Rounded.NetworkCheck, Palette.Teal)
                vm.testResult.forEach { (t, ok) ->
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (ok) Palette.Leaf else Palette.Coral, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("$t: ${if (ok) "آماده" else "در دسترس نیست"}", style = MaterialTheme.typography.bodySmall, color = Palette.Text)
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("ورودی صوتی")
                Spacer(Modifier.height(8.dp))
                SpeechEngine.entries.forEach { e ->
                    Row(Modifier.fillMaxWidth().clickable { speech = e; s.speechEngine = e }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(speech == e, { speech = e; s.speechEngine = e }, colors = RadioButtonDefaults.colors(selectedColor = Palette.Leaf))
                        Column {
                            Text(e.title, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                            Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("مدل‌های گفتار آفلاین فارسی (برای زمان قطعی اینترنت)", style = MaterialTheme.typography.labelLarge, color = Palette.Text)
                Spacer(Modifier.height(6.dp))
                key(tick) {
                    VoskModels.catalog.forEach { m ->
                        val installed = VoskModels.installed(ctx, m.id)
                        val prog = vm.asrProgress[m.id]
                        GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = if (installed) Palette.Leaf else Palette.TextMuted)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${m.title} • ${m.sizeMb.fa()} مگابایت", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                                    Text(m.desc, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                                }
                                when {
                                    prog != null -> Text("${(prog * 100).toInt().fa()}٪", color = Palette.Gold, style = MaterialTheme.typography.labelLarge)
                                    installed -> TextButton({ VoskModels.delete(ctx, m.id); tick++ }) { Text("حذف", color = Palette.Coral) }
                                    else -> TextButton({ vm.installAsr(m) }) { Text("دانلود", color = Palette.Leaf) }
                                }
                            }
                            if (prog != null) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator({ prog }, Modifier.fillMaxWidth(), color = Palette.Leaf, trackColor = Palette.GlassStrong) }
                        }
                    }
                }
                LaunchedEffect(vm.asrProgress.size) { tick++ }

                Spacer(Modifier.height(22.dp))
                SectionLabel("یادآور مراقبت")
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("اعلان روزانه آبیاری و کوددهی", style = MaterialTheme.typography.titleSmall, color = Palette.Text, modifier = Modifier.weight(1f))
                    Switch(reminders, { reminders = it; s.reminders = it; Reminders.schedule(ctx) }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Leaf))
                }
                if (reminders) {
                    Text("ساعت اعلان: ${hour.toInt().fa()}:۰۰", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
                    Slider(hour, { hour = it }, onValueChangeFinished = { s.reminderHour = hour.toInt(); Reminders.schedule(ctx) }, valueRange = 6f..22f, steps = 15,
                        colors = SliderDefaults.colors(thumbColor = Palette.Leaf, activeTrackColor = Palette.Leaf))
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("ظاهر و مشخصات")
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark" to "تیره", "light" to "روشن", "system" to "مطابق سیستم").forEach { (k, t) ->
                        SelectChip(t, s.themeMode == k, { onTheme(k); tick++ }, color = Palette.Gold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(name, { name = it; s.userName = it }, "نام شما", icon = Icons.Rounded.Person)
                Spacer(Modifier.height(10.dp))
                AppTextField(city, { city = it; s.city = it }, "شهر", icon = Icons.Rounded.LocationCity)

                Spacer(Modifier.height(22.dp))
                GlassButton("پاک کردن تاریخچه تشخیص‌ها", { confirmClear = true }, Modifier.fillMaxWidth(), Icons.Rounded.DeleteSweep, Palette.Coral)
                Spacer(Modifier.height(10.dp))
                GlassButton("درباره ما", onAbout, Modifier.fillMaxWidth(), Icons.Rounded.Info, Palette.Teal)
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("پاک کردن تاریخچه؟") },
        text = { Text("همه تشخیص‌ها و عکس‌هایشان حذف می‌شوند. باغچه شما دست نمی‌خورد.") },
        confirmButton = { TextButton({ vm.clearHistory(); confirmClear = false }) { Text("پاک کن", color = Palette.Coral) } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("انصراف") } },
        containerColor = Palette.Surface,
    )
}
