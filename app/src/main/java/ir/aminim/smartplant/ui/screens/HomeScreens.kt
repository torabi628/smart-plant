package ir.aminim.smartplant.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import ir.aminim.smartplant.BuildConfig
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.R
import ir.aminim.smartplant.Routes
import ir.aminim.smartplant.data.Diagnosis
import ir.aminim.smartplant.data.Images
import ir.aminim.smartplant.knowledge.Guides
import ir.aminim.smartplant.settings.AiMode
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.ui.theme.ThemeState
import ir.aminim.smartplant.util.Jalali
import ir.aminim.smartplant.util.fa
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** دوربین و گالری؛ خروجی: (گرفتن عکس، انتخاب از گالری) */
@Composable
fun rememberPhotoPickers(onPicked: (Uri) -> Unit): Pair<() -> Unit, () -> Unit> {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pending?.let(onPicked) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    val takePhoto: () -> Unit = {
        runCatching {
            val f = Images.newCameraFile(ctx)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
            pending = uri
            camera.launch(uri)
        }.onFailure { android.widget.Toast.makeText(ctx, "دوربین در دسترس نیست؛ از گالری انتخاب کنید", android.widget.Toast.LENGTH_LONG).show() }
    }
    val pick: () -> Unit = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    return takePhoto to pick
}

/* ═════════════════════════  اسپلش  ═════════════════════════ */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val scale = remember { Animatable(0.55f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(450))
        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f))
        delay(900)
        onDone()
    }
    val glow = rememberInfiniteTransition(label = "g").animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "ga")
    MeshBackground {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(scale.value).alpha(alpha.value)) {
                Box(Modifier.size(220.dp).drawBehind { drawCircle(Brush.radialGradient(listOf(Palette.Leaf.copy(alpha = glow.value * 0.5f), Color.Transparent))) })
                AppLogo(size = 136.dp)
            }
            Spacer(Modifier.height(26.dp))
            GradientText("گیاه‌شناس هوشمند", MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(alpha.value))
            Spacer(Modifier.height(8.dp))
            Text("عکس بگیر، تشخیص بده، درمان کن", style = MaterialTheme.typography.titleSmall, color = Palette.TextMuted, modifier = Modifier.alpha(alpha.value))
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).alpha(alpha.value), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(if (ThemeState.dark) R.drawable.logo_amin_gold else R.drawable.logo_amin_navy), null, Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("گروه پژوهشی مدیریت هوشمند امین", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted)
        }
    }
}

/** لوگوی برنامه */
@Composable
fun AppLogo(size: Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(Color(0xFF1A7A5E), Color(0xFF0F4A52), Color(0xFF0A1A3A))))
            .border(1.dp, Palette.Gold.copy(alpha = 0.35f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().scale(1.45f)) }
}

/* ═════════════════════════  معرفی اولیه  ═════════════════════════ */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(vm: MainViewModel, onDone: () -> Unit) {
    val pages = listOf(
        Triple(Icons.Rounded.CameraAlt, "عکس بگیر، تشخیص بده", "از برگ آسیب‌دیده و کل گیاه عکس بگیرید؛ هوش مصنوعی گونه گیاه، بیماری، آفت یا مشکل مراقبتی را تشخیص می‌دهد."),
        Triple(Icons.Rounded.HealthAndSafety, "درمان قدم‌به‌قدم", "اقدام فوری، درمان خانگی و ارگانیک، و در صورت نیاز سم مناسب با دوز و نکات ایمنی؛ همراه با پیشگیری."),
        Triple(Icons.Rounded.AutoStories, "دانشنامه و مشاور خرید", "بیش از ۴۵ گیاه آپارتمانی با شناسنامه کامل، مشاور خرید، نورسنج، یادآور آبیاری و پزشک گیاه برای پرسش‌های شما."),
    )
    val pager = rememberPagerState { pages.size + 1 }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(vm.settings.userName) }
    var city by remember { mutableStateOf(vm.settings.city) }
    fun finish() { vm.settings.userName = name; vm.settings.city = city; vm.settings.onboarded = true; onDone() }
    MeshBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                TextButton({ finish() }) { Text("رد شدن", color = Palette.TextMuted) }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (i < pages.size) {
                        val (icon, t, d) = pages[i]
                        IconBadge(icon, listOf(Palette.LeafBrush, Palette.TealBrush, Palette.GoldBrush)[i], size = 120.dp, glow = Palette.Leaf, tint = if (i == 2) Palette.OnGold else Color.White)
                        Spacer(Modifier.height(32.dp))
                        GradientText(t, MaterialTheme.typography.headlineMedium, align = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Text(d, style = MaterialTheme.typography.bodyLarge, color = Palette.TextMuted, textAlign = TextAlign.Center)
                    } else {
                        AppLogo(96.dp)
                        Spacer(Modifier.height(20.dp))
                        GradientText("خوش آمدید", MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(20.dp))
                        AppTextField(name, { name = it }, "نام شما (اختیاری)", icon = Icons.Rounded.Person)
                        Spacer(Modifier.height(12.dp))
                        AppTextField(city, { city = it }, "شهر (برای توصیه‌های اقلیمی)", placeholder = "مثلاً تهران", icon = Icons.Rounded.LocationCity)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    repeat(pages.size + 1) { i ->
                        val w by animateDpAsState(if (pager.currentPage == i) 26.dp else 8.dp, label = "d")
                        Box(Modifier.padding(end = 6.dp).height(8.dp).width(w).clip(CircleShape).background(if (pager.currentPage == i) Palette.Leaf else Palette.GlassStrong))
                    }
                }
                GoldButton(if (pager.currentPage == pages.size) "شروع" else "بعدی", {
                    if (pager.currentPage == pages.size) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }, brush = Palette.LeafBrush, contentColor = Color(0xFF06301A))
            }
        }
    }
}

/* ═════════════════════════  منوی کناری  ═════════════════════════ */
@Composable
fun AppDrawer(vm: MainViewModel, current: String?, onNavigate: (String) -> Unit, onTheme: (String) -> Unit, onDiagnose: () -> Unit) {
    val ctx = LocalContext.current
    ModalDrawerSheet(drawerContainerColor = Palette.Drawer, drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp), modifier = Modifier.width(310.dp)) {
        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().background(Palette.HeroBrush).statusBarsPadding().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(54.dp); Spacer(Modifier.width(12.dp))
                    Column {
                        Text("گیاه‌شناس هوشمند", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(vm.settings.userName.ifBlank { "نسخه ${BuildConfig.VERSION_NAME.fa()}" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.padding(horizontal = 16.dp)) { GoldButton("تشخیص جدید", onDiagnose, Modifier.fillMaxWidth(), icon = Icons.Rounded.CameraAlt) }
            Spacer(Modifier.height(10.dp))
            val items = listOf(
                Triple(Routes.HOME, Icons.Rounded.Home, "خانه"),
                Triple(Routes.HISTORY, Icons.Rounded.History, "تاریخچه تشخیص‌ها"),
                Triple(Routes.GARDEN, Icons.Rounded.Yard, "باغچه من"),
                Triple(Routes.CHAT, Icons.Rounded.Forum, "پزشک گیاه"),
                Triple(Routes.SYMPTOMS, Icons.Rounded.Checklist, "عیب‌یابی با علائم"),
                Triple(Routes.LIBRARY, Icons.Rounded.LocalFlorist, "دانشنامه گیاهان"),
                Triple(Routes.PROBLEMS, Icons.Rounded.BugReport, "بیماری‌ها و آفات"),
                Triple(Routes.ADVISOR, Icons.Rounded.Storefront, "مشاور خرید"),
                Triple(Routes.LIGHT, Icons.Rounded.WbSunny, "نورسنج"),
                Triple(Routes.GUIDES, Icons.Rounded.School, "آموزش نگهداری"),
                Triple(Routes.SETTINGS, Icons.Rounded.Tune, "تنظیمات و هوش مصنوعی"),
                Triple(Routes.ABOUT, Icons.Rounded.Info, "درباره ما"),
            )
            items.forEach { (r, icon, label) ->
                val sel = current == r
                val badge = when (r) {
                    Routes.HISTORY -> vm.diagnoses.size.takeIf { it > 0 }?.fa()
                    Routes.GARDEN -> vm.dueCount.takeIf { it > 0 }?.let { "${it.fa()} نوبت" }
                    else -> null
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Palette.LeafTint else Color.Transparent).clickable { onNavigate(r) }.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = if (sel) Palette.Leaf else Palette.TextMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall, color = if (sel) Palette.Leaf else Palette.Text, modifier = Modifier.weight(1f), fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                    badge?.let { Pill(it, color = Palette.Teal) }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Palette.GlassBorder, modifier = Modifier.padding(horizontal = 20.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ThemeState.dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, tint = Palette.Gold)
                Spacer(Modifier.width(14.dp))
                Text(if (ThemeState.dark) "حالت تیره" else "حالت روشن", style = MaterialTheme.typography.titleSmall, color = Palette.Text, modifier = Modifier.weight(1f))
                Switch(ThemeState.dark, { onTheme(if (it) "dark" else "light") }, colors = SwitchDefaults.colors(checkedThumbColor = Palette.OnGold, checkedTrackColor = Palette.Gold))
            }
            Row(Modifier.fillMaxWidth().clickable { ShareUtil.openUrl(ctx, "bazaar://details?id=ir.aminim.smartplant") }.padding(horizontal = 26.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.StarRate, null, tint = Palette.Gold); Spacer(Modifier.width(14.dp))
                Text("امتیاز در کافه‌بازار", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
            }
            Row(Modifier.fillMaxWidth().clickable { ShareUtil.shareApp(ctx) }.padding(horizontal = 26.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Share, null, tint = Palette.Teal); Spacer(Modifier.width(14.dp))
                Text("معرفی به دوستان", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(if (ThemeState.dark) R.drawable.logo_amin_gold else R.drawable.logo_amin_navy), null, Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text("گروه پژوهشی مدیریت هوشمند امین", style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
            }
        }
    }
}

/* ═════════════════════════  خانه  ═════════════════════════ */
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onMenu: () -> Unit,
    onTheme: (String) -> Unit,
    onCapture: () -> Unit,
    onDescribe: () -> Unit,
    onOpen: (Diagnosis) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val (camera, gallery) = rememberPhotoPickers { uri -> vm.newCapture(); vm.addImage(uri); onCapture() }
    val month = remember { Jalali.of(System.currentTimeMillis()).month }
    val (seasonTitle, seasonTips) = remember(month) { Guides.season(month) }
    MeshBackground {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.Rounded.Menu, onMenu, size = 42.dp)
                Spacer(Modifier.width(10.dp))
                AppLogo(40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("گیاه‌شناس هوشمند", style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                    Text(Jalali.long(System.currentTimeMillis()), style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
                }
                val rot by animateFloatAsState(if (ThemeState.dark) 0f else 180f, tween(500), label = "r")
                CircleIconButton(if (ThemeState.dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, { onTheme(if (ThemeState.dark) "light" else "dark") },
                    size = 42.dp, tint = Palette.Gold, modifier = Modifier.rotate(rot))
            }
            Spacer(Modifier.height(20.dp))
            GradientText(
                (if (vm.settings.userName.isNotBlank()) "${vm.settings.userName} عزیز، " else "") + "گیاهت چشه؟",
                MaterialTheme.typography.headlineMedium,
            )
            Text("عکس بگیرید یا بگویید چه شده؛ تشخیص و درمان را به ما بسپارید.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
            Spacer(Modifier.height(16.dp))

            // کارت اصلی تشخیص
            HeroDiagnoseCard(vm, camera, gallery, onDescribe)

            if (vm.dueCount > 0) {
                Spacer(Modifier.height(14.dp))
                GlassCard(Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.GARDEN) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.WaterDrop, Palette.SkyBrush, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${vm.dueCount.fa()} گیاه امروز منتظر رسیدگی است", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                            Text(vm.garden.filter { it.waterInDays() <= 0 || it.fertInDays() <= 0 }.joinToString("، ") { it.name }, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Rounded.ChevronLeft, null, tint = Palette.TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("ابزارها")
            Spacer(Modifier.height(8.dp))
            val tools = listOf(
                Tool(Icons.Rounded.Forum, "پزشک گیاه", "پرسش و پاسخ با عکس", Palette.TealBrush, Routes.CHAT),
                Tool(Icons.Rounded.Checklist, "عیب‌یابی با علائم", "بدون اینترنت", Palette.OrangeBrush, Routes.SYMPTOMS),
                Tool(Icons.Rounded.LocalFlorist, "دانشنامه گیاهان", "شناسنامه کامل نگهداری", Palette.LeafBrush, Routes.LIBRARY),
                Tool(Icons.Rounded.Storefront, "مشاور خرید", "کدام گیاه برای من؟", Palette.GoldBrush, Routes.ADVISOR),
                Tool(Icons.Rounded.WbSunny, "نورسنج", "نور خانه را بسنج", Palette.OrangeBrush, Routes.LIGHT),
                Tool(Icons.Rounded.Yard, "باغچه من", "یادآور آبیاری و کود", Palette.SkyBrush, Routes.GARDEN),
                Tool(Icons.Rounded.BugReport, "بیماری‌ها و آفات", "شناخت و درمان", Palette.CoralBrush, Routes.PROBLEMS),
                Tool(Icons.Rounded.School, "آموزش نگهداری", "آبیاری، خاک، تکثیر…", Palette.VioletBrush, Routes.GUIDES),
            )
            tools.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t -> ToolCard(t, Modifier.weight(1f)) { onNavigate(t.route) } }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.TipsAndUpdates, Palette.GoldBrush, size = 40.dp, tint = Palette.OnGold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("نکته‌های فصل", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted)
                        Text(seasonTitle, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                    }
                }
                Spacer(Modifier.height(12.dp))
                BulletList(seasonTips, dot = Palette.Gold)
            }

            if (vm.diagnoses.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("تشخیص‌های اخیر", trailing = { TextButton({ onNavigate(Routes.HISTORY) }) { Text("همه", color = Palette.Gold) } })
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    vm.diagnoses.take(8).forEach { d -> RecentCard(d) { onOpen(d) } }
                }
            }
        }
    }
}

@Composable
private fun HeroDiagnoseCard(vm: MainViewModel, camera: () -> Unit, gallery: () -> Unit, onDescribe: () -> Unit) {
    val glow = rememberInfiniteTransition(label = "h").animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "hg")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Palette.HeroBrush)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.07f + glow.value * 0.05f), radius = size.width * 0.45f, center = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.1f))
                drawCircle(Color(0x33EDB850), radius = size.width * 0.3f, center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height))
            }
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("تشخیص هوشمند", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                Text("نام گیاه، بیماری و درمان", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Spa, null, tint = Palette.GoldLight, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        GoldButton("عکس بگیر و تشخیص بده", camera, Modifier.fillMaxWidth(), icon = Icons.Rounded.CameraAlt)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroSmall(Icons.Rounded.PhotoLibrary, "از گالری", Modifier.weight(1f), gallery)
            HeroSmall(Icons.Rounded.Mic, "بگو یا بنویس", Modifier.weight(1f), onDescribe)
        }
        Spacer(Modifier.height(12.dp))
        val mode = vm.settings.aiMode
        val engine = when {
            mode == AiMode.INTERNAL && vm.settings.internalLeft > 0 -> "موتور: هوش مصنوعی قوی داخلی • ${vm.settings.internalLeft.fa()} تشخیص امروز باقی‌مانده"
            mode == AiMode.INTERNAL -> "سهمیه امروز داخلی تمام شد؛ ادامه با هوش مصنوعی قوی خارجی"
            else -> "موتور: ${mode.title}"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Palette.GoldLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(engine, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun HeroSmall(icon: ImageVector, text: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.14f)).border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

private data class Tool(val icon: ImageVector, val title: String, val sub: String, val brush: Brush, val route: String)

@Composable
private fun ToolCard(t: Tool, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.height(118.dp), padding = 14.dp, onClick = onClick) {
        IconBadge(t.icon, t.brush, size = 42.dp, tint = if (t.brush == Palette.GoldBrush) Palette.OnGold else Color.White)
        Spacer(Modifier.weight(1f))
        Text(t.title, style = MaterialTheme.typography.titleSmall, color = Palette.Text, fontWeight = FontWeight.Bold)
        Text(t.sub, style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RecentCard(d: Diagnosis, onClick: () -> Unit) {
    GlassCard(Modifier.width(170.dp), padding = 0.dp, onClick = onClick) {
        Box(Modifier.fillMaxWidth().height(110.dp)) {
            d.images.firstOrNull()?.let { PhotoThumb(it, Modifier.fillMaxSize(), 400) }
                ?: Box(Modifier.fillMaxSize().background(Palette.LeafBrush), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Eco, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(statusColor(d.status)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(d.statusTitle, color = Color(0xFF0A1A2A), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(d.title, style = MaterialTheme.typography.titleSmall, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(d.problems.firstOrNull()?.name ?: "بدون مشکل جدی", style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Jalali.dayMonth(d.time), style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
        }
    }
}

/* ═════════════════════════  درباره ما  ═════════════════════════ */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("درباره ما", onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppLogo(110.dp)
                Spacer(Modifier.height(14.dp))
                GradientText("گیاه‌شناس هوشمند", MaterialTheme.typography.headlineLarge)
                Text("نسخه ${BuildConfig.VERSION_NAME.fa()} • رایگان", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted)
                Spacer(Modifier.height(20.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "گیاه‌شناس هوشمند دستیار هوش مصنوعی شما برای شناسایی گیاه، تشخیص بیماری‌ها، آفات و مشکلات مراقبتی، و ارائه درمان قدم‌به‌قدم است. " +
                            "کافی است از گیاه عکس بگیرید یا با صدا و متن توضیح دهید. دانشنامه گیاهان آپارتمانی، مشاور خرید، نورسنج، یادآور آبیاری و " +
                            "پزشک گیاه نیز کمک می‌کنند گیاهان‌تان سال‌ها سالم و شاداب بمانند.",
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Text, textAlign = TextAlign.Justify,
                    )
                }
                Spacer(Modifier.height(14.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(if (ThemeState.dark) R.drawable.logo_amin_gold else R.drawable.logo_amin_navy), null, Modifier.size(64.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("گروه پژوهشی مدیریت هوشمند امین", style = MaterialTheme.typography.titleSmall, color = Palette.Text, fontWeight = FontWeight.Bold)
                            Text("پژوهش و توسعه راهکارهای هوش مصنوعی", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("امتیاز در بازار", { ShareUtil.openUrl(ctx, "bazaar://details?id=ir.aminim.smartplant") }, Modifier.weight(1f), Icons.Rounded.StarRate, Palette.Gold)
                    GlassButton("وب‌سایت", { ShareUtil.openUrl(ctx, "https://aminim.ir") }, Modifier.weight(1f), Icons.Rounded.Language, Palette.Teal)
                }
                Spacer(Modifier.height(10.dp))
                GlassButton("معرفی برنامه به دیگران", { ShareUtil.shareApp(ctx) }, Modifier.fillMaxWidth(), Icons.Rounded.Share)
                Spacer(Modifier.height(18.dp))
                Text(
                    "حریم خصوصی: عکس‌ها و تاریخچه فقط روی گوشی شما ذخیره می‌شوند. هنگام تشخیص ابری، فقط عکس‌ها و توضیح همان تشخیص برای تحلیل به سرویس هوش مصنوعی ارسال می‌شود.\n" +
                        "تشخیص هوش مصنوعی جایگزین نظر کارشناس گیاه‌پزشکی نیست؛ در مصرف سموم دستور برچسب را رعایت کنید.",
                    style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint, textAlign = TextAlign.Center,
                )
            }
        }
    }
}
