package ir.aminim.smartplant.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.data.Diagnosis
import ir.aminim.smartplant.data.Found
import ir.aminim.smartplant.knowledge.PlantDb
import ir.aminim.smartplant.knowledge.ProblemDb
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.util.Jalali
import ir.aminim.smartplant.util.fa
import kotlinx.coroutines.delay

/* ═════════════════════════  گرفتن عکس و توضیح  ═════════════════════════ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(vm: MainViewModel, onBack: () -> Unit, onAnalyze: () -> Unit, onSymptoms: () -> Unit) {
    val (camera, gallery) = rememberPhotoPickers { vm.addImage(it) }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("تشخیص هوشمند", onBack, subtitle = "عکس + توضیح = دقیق‌ترین تشخیص")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                SectionLabel("عکس‌های گیاه (تا ${MainViewModel.MAX_SHOTS.fa()} عکس)")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    vm.shots.forEach { p ->
                        Box(Modifier.size(118.dp).clip(RoundedCornerShape(20.dp))) {
                            PhotoThumb(p, Modifier.fillMaxSize(), 400)
                            Box(
                                Modifier.align(Alignment.TopStart).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable { vm.removeImage(p) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Close, "حذف", tint = Color.White, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    if (vm.importing) Box(Modifier.size(118.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Glass), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Palette.Leaf) }
                    if (vm.shots.size < MainViewModel.MAX_SHOTS) {
                        AddShotTile(Icons.Rounded.AddAPhoto, "دوربین", camera)
                        AddShotTile(Icons.Rounded.PhotoLibrary, "گالری", gallery)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ExpandCard("راهنمای عکس خوب برای تشخیص دقیق", Icons.Rounded.CameraEnhance, Palette.SkyBrush, subtitle = "۴ عکس از زاویه‌های مختلف بهترین نتیجه را می‌دهد") {
                    BulletList(
                        listOf(
                            "در نور روز (نه زیر لامپ زرد و نه آفتاب تند) عکس بگیرید.",
                            "یک عکس نزدیک و واضح از برگ یا بخش آسیب‌دیده.",
                            "یک عکس از کل گیاه همراه با گلدان.",
                            "اگر آفت مشکوک است، از پشت برگ و محل اتصال برگ به ساقه.",
                            "در صورت امکان از سطح خاک یا ریشه (از زهکش).",
                        ), numbered = true,
                    )
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("چه اتفاقی افتاده؟ (اختیاری ولی بسیار مفید)")
                Spacer(Modifier.height(8.dp))
                VoiceTextField(
                    vm.note, { vm.note = it }, "توضیح با صدا یا متن",
                    "مثلاً: از دو هفته پیش برگ‌های پایینی زرد شده، هفته‌ای دو بار آب می‌دهم، کنار پنجره است…",
                    vm::newDictation,
                )
                Spacer(Modifier.height(10.dp))
                val quick = listOf("برگ‌ها زرد شده", "نوک برگ‌ها قهوه‌ای شده", "لکه روی برگ", "پژمرده و افتاده", "برگ می‌ریزد", "حشره یا تار دیدم", "خاک همیشه خیس است", "تازه خریدم", "کنار بخاری/کولر است", "رشد نمی‌کند")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { q ->
                        val on = vm.note.contains(q)
                        SelectChip(q, on, { vm.note = if (on) vm.note.replace(q, "").replace("،،", "،").trim('،', ' ') else listOf(vm.note.trim(), q).filter { it.isNotBlank() }.joinToString("، ") })
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("نام گیاه را می‌دانید؟ (اختیاری)")
                Spacer(Modifier.height(8.dp))
                AppTextField(vm.plantHint, { vm.plantHint = it }, "نام گیاه", placeholder = "مثلاً پتوس", icon = Icons.Rounded.LocalFlorist)
                if (vm.plantHint.length >= 2) {
                    val sug = PlantDb.all.filter { it.matches(vm.plantHint) && it.fa != vm.plantHint }.take(6)
                    if (sug.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sug.forEach { p -> SelectChip(p.fa, false, { vm.plantHint = p.fa }, icon = Icons.Rounded.Eco) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlassCard(Modifier.fillMaxWidth(), onClick = onSymptoms) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.WifiOff, Palette.OrangeBrush, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("اینترنت ندارید؟", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                            Text("با انتخاب علائم، آفلاین عیب‌یابی کنید", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                        }
                        Icon(Icons.Rounded.ChevronLeft, null, tint = Palette.TextMuted)
                    }
                }
            }
            BottomBarContainer {
                Text("موتور: ${vm.aiEngineTitle}", style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted, modifier = Modifier.padding(bottom = 6.dp))
                GoldButton("تشخیص بده", { vm.diagnose(); onAnalyze() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome,
                    enabled = (vm.shots.isNotEmpty() || vm.note.isNotBlank()) && !vm.importing)
            }
        }
    }
}

@Composable
private fun AddShotTile(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Column(
        Modifier.size(118.dp).clip(RoundedCornerShape(20.dp)).background(Palette.LeafTint)
            .border(1.5.dp, Palette.Leaf.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Palette.Leaf, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Palette.Leaf)
    }
}

/* ═════════════════════════  نتیجه تشخیص  ═════════════════════════ */
@Composable
fun ResultScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onChat: (Diagnosis) -> Unit,
    onGarden: (Diagnosis) -> Unit,
    onPlant: (String) -> Unit,
    onProblem: (String) -> Unit,
    onSymptoms: () -> Unit,
) {
    val d = vm.current
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (vm.busy) "در حال تشخیص…" else "نتیجه تشخیص", onBack, subtitle = d?.let { "${Jalali.long(it.time)} • ${it.engine}" }) {
                if (d != null && !vm.busy) {
                    val ctx = LocalContext.current
                    CircleIconButton(Icons.Rounded.Share, { ShareUtil.shareText(ctx, shareText(d)) }, size = 40.dp)
                }
            }
            when {
                vm.busy -> AnalyzingView(vm)
                vm.busyError != null -> ErrorView(vm.busyError!!, onRetry = { vm.diagnose() }, onSymptoms = onSymptoms)
                d != null -> ResultContent(vm, d, onChat, onGarden, onPlant, onProblem)
                else -> EmptyState(Icons.Rounded.Eco, "تشخیصی انتخاب نشده", "از صفحه اصلی یک تشخیص جدید شروع کنید.")
            }
        }
    }
}

@Composable
private fun AnalyzingView(vm: MainViewModel) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "y")
    val facts = listOf(
        "بیش از ۸۰ درصد مرگ گیاهان آپارتمانی به خاطر آبیاری بیش از حد است.",
        "گلدان بدون سوراخ زهکش، دشمن شماره یک ریشه است.",
        "کنه تارعنکبوتی در هوای گرم و خشک کنار بخاری انفجاری تکثیر می‌شود.",
        "زردی برگ جوان با رگبرگ سبز، معمولاً کمبود آهن است.",
        "گیاه تازه خریداری‌شده را دو هفته جدا از بقیه نگه دارید.",
        "آب شهری را ۲۴ ساعت در ظرف باز بگذارید تا کلرش کم شود.",
    )
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(3500); i = (i + 1) % facts.size } }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val leaf = Palette.Leaf
        Box(
            Modifier.size(240.dp).clip(RoundedCornerShape(32.dp)).border(2.dp, Palette.Leaf.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .drawWithContent {
                    drawContent()
                    val yy = size.height * y
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, leaf.copy(alpha = 0.35f), Color.Transparent), startY = yy - 60f, endY = yy + 60f))
                    drawLine(leaf, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 4f)
                },
        ) {
            vm.shots.firstOrNull()?.let { PhotoThumb(it, Modifier.fillMaxSize(), 700) }
                ?: Box(Modifier.fillMaxSize().background(Palette.HeroBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Spa, null, tint = Color.White, modifier = Modifier.size(90.dp))
                }
        }
        Spacer(Modifier.height(28.dp))
        GradientText("در حال بررسی گیاه شما", MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(vm.busyStage.ifBlank { "در حال آماده‌سازی…" }, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth(0.7f).clip(RoundedCornerShape(4.dp)), color = Palette.Leaf, trackColor = Palette.GlassStrong)
        Spacer(Modifier.height(28.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, null, tint = Palette.Gold)
                Spacer(Modifier.width(10.dp))
                Text("می‌دانستید؟ ${facts[i]}", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("تحلیل دقیق ممکن است تا یک دقیقه طول بکشد.", style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
    }
}

@Composable
private fun ErrorView(msg: String, onRetry: () -> Unit, onSymptoms: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        IconBadge(Icons.Rounded.CloudOff, Palette.CoralBrush, size = 80.dp, glow = Palette.Coral)
        Spacer(Modifier.height(18.dp))
        Text("تشخیص انجام نشد", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
        Spacer(Modifier.height(8.dp))
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        GoldButton("تلاش دوباره", onRetry, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
        Spacer(Modifier.height(10.dp))
        GlassButton("عیب‌یابی آفلاین با علائم", onSymptoms, Modifier.fillMaxWidth(), Icons.Rounded.Checklist, Palette.Orange)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultContent(
    vm: MainViewModel, d: Diagnosis,
    onChat: (Diagnosis) -> Unit, onGarden: (Diagnosis) -> Unit, onPlant: (String) -> Unit, onProblem: (String) -> Unit,
) {
    var answer by remember(d.id) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        // تصاویر
        if (d.images.isNotEmpty()) {
            val pager = rememberPagerState { d.images.size }
            Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(26.dp))) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { i -> PhotoThumb(d.images[i], Modifier.fillMaxSize(), 1000) }
                if (d.images.size > 1) Row(Modifier.align(Alignment.BottomCenter).padding(10.dp)) {
                    repeat(d.images.size) { i -> Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 10.dp else 7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.5f))) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // شناسنامه گیاه
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (d.isPlant) Icons.Rounded.LocalFlorist else Icons.Rounded.HideImage, if (d.isPlant) Palette.LeafBrush else Palette.CoralBrush, size = 52.dp, glow = Palette.Leaf)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    GradientText(d.title, MaterialTheme.typography.headlineSmall)
                    if (d.plantSci.isNotBlank()) Text(d.plantSci, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, fontWeight = FontWeight.Medium)
                    if (d.family.isNotBlank()) Text("تیره: ${d.family}", style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
                }
            }
            if (d.confidence > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("اطمینان شناسایی", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted, modifier = Modifier.width(110.dp))
                    PercentBar(d.confidence, Palette.Leaf, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("${d.confidence.fa()}٪", style = MaterialTheme.typography.labelLarge, color = Palette.Leaf)
                }
            }
            if (d.about.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(d.about, style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
            if (d.alternatives.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("گزینه‌های دیگر: " + d.alternatives.joinToString("، ") { "${it.name}${if (it.confidence > 0) " (${it.confidence.fa()}٪)" else ""}" },
                    style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
            if (d.plantId.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                GlassButton("شناسنامه کامل در دانشنامه", { onPlant(d.plantId) }, Modifier.fillMaxWidth(), Icons.Rounded.AutoStories, Palette.Leaf)
            }
        }
        Spacer(Modifier.height(14.dp))

        // سلامت
        val sc = statusColor(d.status)
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(d.healthScore, sc)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(sc.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(d.statusTitle, style = MaterialTheme.typography.labelLarge, color = sc)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(d.healthSummary, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                }
            }
            if (d.urgency.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(sc.copy(alpha = 0.12f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = sc, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(d.urgency, style = MaterialTheme.typography.bodySmall, color = Palette.Text)
                }
            }
        }

        // مشکلات و درمان
        if (d.problems.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("مشکلات تشخیص‌داده‌شده و درمان")
            Spacer(Modifier.height(8.dp))
            d.problems.forEachIndexed { i, p ->
                ProblemCard(p, i == 0, onProblem)
                Spacer(Modifier.height(10.dp))
            }
        }

        // مراقبت
        if (d.care.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ExpandCard("برنامه مراقبت اختصاصی", Icons.Rounded.Spa, Palette.TealBrush, subtitle = "نور، آب، خاک، کود و …", initiallyOpen = d.problems.isEmpty()) {
                d.care.forEach { (k, v) ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Icon(careIcon(k), null, tint = Palette.Teal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(k, style = MaterialTheme.typography.labelLarge, color = Palette.Teal)
                            Text(v, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                        }
                    }
                }
            }
        }
        if (d.prevention.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ExpandCard("پیشگیری", Icons.Rounded.Shield, Palette.MintBrush) { BulletList(d.prevention, dot = Palette.Mint) }
        }
        if (d.toxicity.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Pets, Palette.PinkBrush, size = 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ایمنی برای کودک و حیوان خانگی", style = MaterialTheme.typography.labelLarge, color = Palette.Pink)
                        Text(d.toxicity, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                    }
                }
            }
        }
        if (d.tips.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ExpandCard("نکته‌های حرفه‌ای", Icons.Rounded.TipsAndUpdates, Palette.GoldBrush) { BulletList(d.tips, dot = Palette.Gold) }
        }

        // پرسش‌های تکمیلی
        if (d.questions.isNotEmpty() && !d.offline) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Quiz, null, tint = Palette.Sky)
                    Spacer(Modifier.width(8.dp))
                    Text("برای تشخیص دقیق‌تر پاسخ دهید", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                }
                Spacer(Modifier.height(8.dp))
                BulletList(d.questions, numbered = true, dot = Palette.Sky)
                Spacer(Modifier.height(10.dp))
                VoiceTextField(answer, { answer = it }, "پاسخ شما", "پاسخ پرسش‌ها را بنویسید یا بگویید…", vm::newDictation, minLines = 2)
                Spacer(Modifier.height(10.dp))
                GoldButton("تشخیص دوباره با این اطلاعات", { vm.refine(d, answer) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh, enabled = answer.isNotBlank())
            }
        }

        Spacer(Modifier.height(18.dp))
        GoldButton("گفتگو با پزشک گیاه درباره این تشخیص", { onChat(d) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Forum, brush = Palette.TealBrush, contentColor = Color.White)
        Spacer(Modifier.height(10.dp))
        if (d.isPlant) GlassButton("افزودن به باغچه من و یادآور آبیاری", { onGarden(d) }, Modifier.fillMaxWidth(), Icons.Rounded.Yard, Palette.Leaf)
        Spacer(Modifier.height(14.dp))
        Text(
            if (d.offline) "این نتیجه با موتور آفلاین و بر اساس علائم به دست آمده و تقریبی است."
            else "تشخیص هوش مصنوعی ممکن است خطا داشته باشد. در مصرف سموم حتماً دستور برچسب را رعایت کنید و در موارد جدی با کارشناس گیاه‌پزشکی مشورت کنید.",
            style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun careIcon(label: String) = when (label) {
    "نور" -> Icons.Rounded.WbSunny; "آبیاری" -> Icons.Rounded.WaterDrop; "رطوبت" -> Icons.Rounded.Opacity; "دما" -> Icons.Rounded.Thermostat
    "خاک" -> Icons.Rounded.Landscape; "کوددهی" -> Icons.Rounded.Science; "تعویض گلدان" -> Icons.Rounded.Inventory2; "هرس" -> Icons.Rounded.ContentCut
    else -> Icons.Rounded.Eco
}

private fun typeTitle(t: String) = when (t) { "pest" -> "آفت"; "disease" -> "بیماری"; "nutrient" -> "تغذیه"; else -> "مراقبت و محیط" }

@Composable
private fun ProblemCard(p: Found, open: Boolean, onProblem: (String) -> Unit) {
    val (icon, brush, color) = when (p.type) {
        "pest" -> Triple(Icons.Rounded.BugReport, Palette.CoralBrush, Palette.Coral)
        "disease" -> Triple(Icons.Rounded.Coronavirus, Palette.VioletBrush, Palette.Violet)
        "nutrient" -> Triple(Icons.Rounded.Science, Palette.SkyBrush, Palette.Sky)
        else -> Triple(Icons.Rounded.WaterDrop, Palette.OrangeBrush, Palette.Orange)
    }
    ExpandCard(p.name, icon, brush, subtitle = typeTitle(p.type), initiallyOpen = open, trailing = {
        if (p.probability > 0) Text("${p.probability.fa()}٪", style = MaterialTheme.typography.titleMedium, color = color, modifier = Modifier.padding(end = 6.dp))
    }) {
        if (p.probability > 0) { PercentBar(p.probability, color); Spacer(Modifier.height(12.dp)) }
        if (p.cause.isNotBlank()) { Text(p.cause, style = MaterialTheme.typography.bodyMedium, color = Palette.Text); Spacer(Modifier.height(10.dp)) }
        if (p.signs.isNotEmpty()) { SubTitle("نشانه‌ها", Icons.Rounded.Visibility, Palette.TextMuted); BulletList(p.signs, dot = Palette.TextMuted); Spacer(Modifier.height(10.dp)) }
        if (p.immediate.isNotEmpty()) { SubTitle("اقدام فوری", Icons.Rounded.Bolt, Palette.Coral); BulletList(p.immediate, numbered = true, dot = Palette.Coral); Spacer(Modifier.height(10.dp)) }
        if (p.organic.isNotEmpty()) { SubTitle("درمان خانگی و ارگانیک", Icons.Rounded.Eco, Palette.Leaf); BulletList(p.organic, dot = Palette.Leaf); Spacer(Modifier.height(10.dp)) }
        if (p.chemical.isNotEmpty()) {
            SubTitle("درمان شیمیایی (در صورت نیاز)", Icons.Rounded.Science, Palette.Violet)
            BulletList(p.chemical, dot = Palette.Violet)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.Coral.copy(alpha = 0.12f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, null, tint = Palette.Coral, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("با دستکش و ماسک، در فضای باز و دور از کودک و حیوان؛ دوز طبق برچسب.", style = MaterialTheme.typography.labelSmall, color = Palette.Text)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (p.recovery.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timelapse, null, tint = Palette.Teal, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("زمان بهبود: ${p.recovery}", style = MaterialTheme.typography.labelMedium, color = Palette.Teal)
            }
        }
        val local = p.problemId.ifBlank { ProblemDb.all.firstOrNull { it.name == p.name }?.id.orEmpty() }
        if (local.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            TextButton({ onProblem(local) }) { Text("اطلاعات کامل این مشکل در دانشنامه", color = Palette.Gold) }
        }
    }
}

@Composable
fun SubTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

fun shareText(d: Diagnosis): String = buildString {
    append("🌿 نتیجه تشخیص «گیاه‌شناس هوشمند»\n\n")
    append("گیاه: ${d.title}"); if (d.plantSci.isNotBlank()) append(" (${d.plantSci})"); append("\n")
    append("وضعیت: ${d.statusTitle} — ${d.healthSummary}\n")
    d.problems.forEach { p ->
        append("\n▪️ ${p.name}${if (p.probability > 0) " (${p.probability}٪)" else ""}\n")
        if (p.immediate.isNotEmpty()) append("اقدام فوری:\n" + p.immediate.joinToString("\n") { "- $it" } + "\n")
        if (p.organic.isNotEmpty()) append("درمان ارگانیک:\n" + p.organic.joinToString("\n") { "- $it" } + "\n")
        if (p.chemical.isNotEmpty()) append("درمان شیمیایی:\n" + p.chemical.joinToString("\n") { "- $it" } + "\n")
    }
    if (d.care.isNotEmpty()) { append("\nمراقبت:\n"); d.care.forEach { (k, v) -> append("- $k: $v\n") } }
    append("\nگروه پژوهشی مدیریت هوشمند امین")
}

/* ═════════════════════════  عیب‌یابی آفلاین با علائم  ═════════════════════════ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomScreen(vm: MainViewModel, onBack: () -> Unit, onResult: () -> Unit) {
    var plantQuery by remember { mutableStateOf(vm.symptomPlant?.fa ?: "") }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("عیب‌یابی با علائم", onBack, subtitle = "کاملاً آفلاین • علائمی را که می‌بینید انتخاب کنید")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                SectionLabel("گیاه شما (اختیاری)")
                Spacer(Modifier.height(8.dp))
                AppTextField(plantQuery, { plantQuery = it; if (vm.symptomPlant?.fa != it) vm.symptomPlant = null }, "نام گیاه", placeholder = "مثلاً سانسوریا", icon = Icons.Rounded.LocalFlorist)
                if (plantQuery.isNotBlank() && vm.symptomPlant == null) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlantDb.all.filter { it.matches(plantQuery) }.take(8).forEach { p -> SelectChip(p.fa, false, { vm.symptomPlant = p; plantQuery = p.fa }, icon = Icons.Rounded.Eco) }
                    }
                }
                ProblemDb.groups.forEach { g ->
                    Spacer(Modifier.height(16.dp))
                    SectionLabel(g)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProblemDb.symptoms.filter { it.group == g }.forEach { s ->
                            val on = s.id in vm.symptomSel
                            SelectChip(s.title, on, { if (on) vm.symptomSel.remove(s.id) else vm.symptomSel.add(s.id) }, color = Palette.Orange)
                        }
                    }
                }
            }
            BottomBarContainer {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (vm.symptomSel.isNotEmpty()) {
                        GlassButton("پاک کردن", { vm.symptomSel.clear() }, icon = Icons.Rounded.ClearAll)
                        Spacer(Modifier.width(10.dp))
                    }
                    GoldButton("تحلیل ${if (vm.symptomSel.isEmpty()) "" else vm.symptomSel.size.fa() + " علامت"}", { vm.runSymptoms(); if (vm.symptomSel.isNotEmpty()) onResult() },
                        Modifier.weight(1f), icon = Icons.Rounded.Biotech, enabled = vm.symptomSel.isNotEmpty())
                }
            }
        }
    }
}

/* ═════════════════════════  تاریخچه  ═════════════════════════ */
@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (Diagnosis) -> Unit) {
    var confirm by remember { mutableStateOf<Diagnosis?>(null) }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("تاریخچه تشخیص‌ها", onBack, subtitle = "${vm.diagnoses.size.fa()} تشخیص")
            if (vm.diagnoses.isEmpty()) EmptyState(Icons.Rounded.History, "هنوز تشخیصی ثبت نشده", "اولین عکس گیاه‌تان را بگیرید.")
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                vm.diagnoses.forEach { d ->
                    GlassCard(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { onOpen(d) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))) {
                                d.images.firstOrNull()?.let { PhotoThumb(it, Modifier.fillMaxSize(), 300) }
                                    ?: Box(Modifier.fillMaxSize().background(Palette.LeafBrush), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Eco, null, tint = Color.White) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.title, style = MaterialTheme.typography.titleSmall, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(d.problems.firstOrNull()?.name ?: d.statusTitle, style = MaterialTheme.typography.bodySmall, color = statusColor(d.status), maxLines = 1)
                                Text("${Jalali.long(d.time)} • ${if (d.offline) "آفلاین" else "هوش مصنوعی"}", style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
                            }
                            IconButton({ confirm = d }) { Icon(Icons.Rounded.DeleteOutline, "حذف", tint = Palette.TextMuted) }
                        }
                    }
                }
            }
        }
    }
    confirm?.let { d ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("حذف تشخیص؟") },
            text = { Text("«${d.title}» و عکس‌هایش حذف می‌شود.") },
            confirmButton = { TextButton({ vm.deleteDiagnosis(d); confirm = null }) { Text("حذف", color = Palette.Coral) } },
            dismissButton = { TextButton({ confirm = null }) { Text("انصراف") } },
            containerColor = Palette.Surface,
        )
    }
}
