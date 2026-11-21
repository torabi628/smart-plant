package ir.aminim.smartplant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.knowledge.Guides
import ir.aminim.smartplant.knowledge.Light
import ir.aminim.smartplant.knowledge.PlantCat
import ir.aminim.smartplant.knowledge.PlantDb
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.knowledge.ProblemDb
import ir.aminim.smartplant.knowledge.ProblemType
import ir.aminim.smartplant.knowledge.Tag
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.util.fa

fun catIcon(c: PlantCat): ImageVector = when (c) {
    PlantCat.FOLIAGE -> Icons.Rounded.Eco; PlantCat.FLOWER -> Icons.Rounded.LocalFlorist; PlantCat.SUCCULENT -> Icons.Rounded.Grass
    PlantCat.PALM -> Icons.Rounded.Park; PlantCat.HANGING -> Icons.Rounded.Spa
}

fun catBrush(c: PlantCat): Brush = when (c) {
    PlantCat.FOLIAGE -> Palette.LeafBrush; PlantCat.FLOWER -> Palette.PinkBrush; PlantCat.SUCCULENT -> Palette.MintBrush
    PlantCat.PALM -> Palette.TealBrush; PlantCat.HANGING -> Palette.VioletBrush
}

/* ═════════════════════════  دانشنامه گیاهان  ═════════════════════════ */
@Composable
fun LibraryScreen(vm: MainViewModel, onBack: () -> Unit, onPlant: (PlantInfo) -> Unit, onAiProfile: (String) -> Unit) {
    var q by rememberSaveableString()
    var cat by remember { mutableStateOf<PlantCat?>(null) }
    var tag by remember { mutableStateOf<Tag?>(null) }
    val list = PlantDb.all.filter { it.matches(q) && (cat == null || it.cat == cat) && (tag == null || tag in it.allTags) }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("دانشنامه گیاهان", onBack, subtitle = "${PlantDb.all.size.fa()} گیاه آپارتمانی رایج در ایران")
            Column(Modifier.padding(horizontal = 20.dp)) {
                AppTextField(q, { q = it }, "جستجو", placeholder = "نام فارسی، علمی یا محلی…", icon = Icons.Rounded.Search)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("همه", cat == null, { cat = null })
                    PlantCat.entries.forEach { c -> SelectChip(c.title, cat == c, { cat = if (cat == c) null else c }, icon = catIcon(c)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag.entries.forEach { t -> SelectChip(t.title, tag == t, { tag = if (tag == t) null else t }, color = Palette.Gold) }
                }
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { p -> PlantRow(p) { onPlant(p) } }
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.AutoAwesome, Palette.GoldBrush, size = 40.dp, tint = Palette.OnGold)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (list.isEmpty()) "«$q» در دانشنامه نیست" else "گیاه مورد نظرتان را پیدا نکردید؟", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                                Text("هوش مصنوعی شناسنامه کامل هر گیاهی را برایتان می‌سازد.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        GoldButton(if (q.isBlank()) "نام گیاه را در جستجو بنویسید" else "شناسنامه «$q» با هوش مصنوعی", { onAiProfile(q) }, Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.AutoAwesome, enabled = q.isNotBlank())
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSaveableString() = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

@Composable
fun PlantRow(p: PlantInfo, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(catIcon(p.cat), catBrush(p.cat), size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.fa, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                Text(p.sci, style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WbSunny, null, tint = Palette.Gold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(3.dp))
                    Text(Light.range(p.lightMin, p.lightMax), style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Rounded.Speed, null, tint = Palette.Teal, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(3.dp))
                    Text(p.levelTitle, style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
                }
            }
            Icon(if (p.petSafe) Icons.Rounded.Pets else Icons.Rounded.Warning, null, tint = if (p.petSafe) Palette.Mint else Palette.Coral.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
    }
}

/* ═════════════════════════  شناسنامه گیاه  ═════════════════════════ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantScreen(vm: MainViewModel, onBack: () -> Unit, onProblem: (String) -> Unit, onGarden: (PlantInfo) -> Unit, onAsk: (String) -> Unit) {
    val p = vm.plant ?: run { LaunchedEffect(Unit) { onBack() }; return }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(p.fa, onBack, subtitle = p.cat.title)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(catBrush(p.cat)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(catIcon(p.cat), null, tint = Color.White, modifier = Modifier.size(38.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(p.fa, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
                                Text(p.sci, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), fontStyle = FontStyle.Italic)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("نام‌های دیگر: ${p.aka}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Text("تیره: ${p.family} • خاستگاه: ${p.origin}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(p.intro, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile(Icons.Rounded.WbSunny, "نور", Light.range(p.lightMin, p.lightMax), Palette.Gold, Modifier.weight(1f))
                    InfoTile(Icons.Rounded.WaterDrop, "آبیاری", "هر ${p.waterSummer.fa()} تا ${p.waterWinter.fa()} روز", Palette.Sky, Modifier.weight(1f))
                    InfoTile(Icons.Rounded.Speed, "نگهداری", p.levelTitle, Palette.Teal, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile(Icons.Rounded.Thermostat, "دما", "${p.tempMin.fa()} تا ${p.tempMax.fa()}°", Palette.Orange, Modifier.weight(1f))
                    InfoTile(Icons.Rounded.Opacity, "رطوبت", listOf("", "کم", "متوسط", "زیاد")[p.humidity], Palette.Violet, Modifier.weight(1f))
                    InfoTile(if (p.petSafe) Icons.Rounded.Pets else Icons.Rounded.Warning, "حیوان و کودک", if (p.petSafe) "بی‌خطر" else "سمی", if (p.petSafe) Palette.Mint else Palette.Coral, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.allTags.forEach { Pill(it.title, color = Palette.Leaf) }
                    Pill("رشد ${p.growth}", color = Palette.TextMuted)
                    Pill(p.sizeTitle, color = Palette.TextMuted)
                }
                Spacer(Modifier.height(16.dp))
                ExpandCard("آبیاری", Icons.Rounded.WaterDrop, Palette.SkyBrush, initiallyOpen = true) {
                    Text(p.water, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                    Spacer(Modifier.height(6.dp))
                    Text("راهنما: فصل گرم حدود هر ${p.waterSummer.fa()} روز، فصل سرد حدود هر ${p.waterWinter.fa()} روز؛ همیشه اول خشکی خاک را بسنجید.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                ExpandCard("نور و جایگاه", Icons.Rounded.WbSunny, Palette.OrangeBrush) {
                    Text(Light.names[p.lightMin] + (if (p.lightMax != p.lightMin) " تا " + Light.names[p.lightMax] else ""), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                }
                Spacer(Modifier.height(10.dp))
                ExpandCard("رطوبت و دما", Icons.Rounded.Thermostat, Palette.VioletBrush) {
                    Text("رطوبت: ${p.humidityTitle}\nدمای مناسب: ${p.tempMin.fa()} تا ${p.tempMax.fa()} درجه سانتی‌گراد", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                }
                Spacer(Modifier.height(10.dp))
                ExpandCard("خاک و گلدان", Icons.Rounded.Landscape, Palette.SoilBrush) { Text(p.soil, style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
                Spacer(Modifier.height(10.dp))
                ExpandCard("کوددهی", Icons.Rounded.Science, Palette.MintBrush) { Text(p.fert, style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
                Spacer(Modifier.height(10.dp))
                ExpandCard("تکثیر", Icons.Rounded.ContentCut, Palette.TealBrush) { Text(p.propagate, style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
                Spacer(Modifier.height(10.dp))
                ExpandCard("ایمنی (کودک و حیوان خانگی)", if (p.petSafe) Icons.Rounded.Pets else Icons.Rounded.Warning, if (p.petSafe) Palette.MintBrush else Palette.CoralBrush, initiallyOpen = !p.petSafe) {
                    Text(p.toxicity, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                }
                Spacer(Modifier.height(10.dp))
                ExpandCard("راهنمای خرید", Icons.Rounded.ShoppingBag, Palette.GoldBrush, subtitle = "هنگام خرید به این‌ها دقت کنید") { BulletList(p.buy, numbered = true, dot = Palette.Gold) }
                if (p.tips.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    ExpandCard("نکته‌های حرفه‌ای", Icons.Rounded.TipsAndUpdates, Palette.PinkBrush) { BulletList(p.tips, dot = Palette.Pink) }
                }
                Spacer(Modifier.height(18.dp))
                SectionLabel("مشکلات رایج این گیاه")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.problems.mapNotNull { ProblemDb.byId(it) }.forEach { pr -> SelectChip(pr.name, false, { onProblem(pr.id) }, color = Palette.Coral, icon = Icons.Rounded.ReportProblem) }
                }
                Spacer(Modifier.height(20.dp))
                GoldButton("افزودن به باغچه من", { onGarden(p) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Yard, brush = Palette.LeafBrush, contentColor = Color(0xFF06301A))
                Spacer(Modifier.height(10.dp))
                GlassButton("پرسش از پزشک گیاه درباره ${p.fa}", { onAsk("درباره ${p.fa} (${p.sci}) سؤال دارم: ") }, Modifier.fillMaxWidth(), Icons.Rounded.Forum, Palette.Teal)
            }
        }
    }
}

/* ═════════════════════════  شناسنامه هوش مصنوعی  ═════════════════════════ */
@Composable
fun AiProfileScreen(vm: MainViewModel, onBack: () -> Unit) {
    val p = vm.profile
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(p?.title?.ifBlank { null } ?: "شناسنامه هوشمند", onBack, subtitle = p?.sci ?: "ساخته‌شده با هوش مصنوعی")
            when {
                vm.profileBusy -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Palette.Leaf)
                    Spacer(Modifier.height(14.dp))
                    Text("در حال تهیه شناسنامه گیاه…", color = Palette.TextMuted)
                }
                p == null -> EmptyState(Icons.Rounded.CloudOff, "اطلاعاتی دریافت نشد", "اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.")
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    p.sections.forEachIndexed { i, (t, body) ->
                        ExpandCard(t, Icons.Rounded.Eco, listOf(Palette.LeafBrush, Palette.SkyBrush, Palette.OrangeBrush, Palette.TealBrush, Palette.VioletBrush)[i % 5], initiallyOpen = i < 2) {
                            RichText(body)
                        }
                    }
                    if (p.toxicity.isNotBlank()) ExpandCard("ایمنی", Icons.Rounded.Pets, Palette.PinkBrush, initiallyOpen = true) { Text(p.toxicity, color = Palette.Text, style = MaterialTheme.typography.bodyMedium) }
                    if (p.buy.isNotEmpty()) ExpandCard("راهنمای خرید", Icons.Rounded.ShoppingBag, Palette.GoldBrush, initiallyOpen = true) { BulletList(p.buy, numbered = true, dot = Palette.Gold) }
                    Text("این شناسنامه با هوش مصنوعی تهیه شده و ممکن است نیاز به بررسی داشته باشد.", style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
                }
            }
        }
    }
}

/* ═════════════════════════  بیماری‌ها و آفات  ═════════════════════════ */
fun typeBrush(t: ProblemType) = when (t) { ProblemType.PEST -> Palette.CoralBrush; ProblemType.DISEASE -> Palette.VioletBrush; ProblemType.NUTRIENT -> Palette.SkyBrush; else -> Palette.OrangeBrush }
fun typeIcon(t: ProblemType) = when (t) { ProblemType.PEST -> Icons.Rounded.BugReport; ProblemType.DISEASE -> Icons.Rounded.Coronavirus; ProblemType.NUTRIENT -> Icons.Rounded.Science; else -> Icons.Rounded.WaterDrop }

@Composable
fun ProblemsScreen(onBack: () -> Unit, onProblem: (String) -> Unit) {
    var type by remember { mutableStateOf<ProblemType?>(null) }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("بیماری‌ها و آفات", onBack, subtitle = "شناخت، درمان و پیشگیری")
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("همه", type == null, { type = null })
                ProblemType.entries.forEach { t -> SelectChip(t.title, type == t, { type = if (type == t) null else t }, icon = typeIcon(t)) }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ProblemDb.all.filter { type == null || it.type == type }, key = { it.id }) { p ->
                    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onProblem(p.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(typeIcon(p.type), typeBrush(p.type), size = 46.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                                Text(p.summary, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            LevelDots(p.severity, 3, Palette.Coral)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProblemScreen(vm: MainViewModel, onBack: () -> Unit, onPlant: (PlantInfo) -> Unit) {
    val p = ProblemDb.byId(vm.problemId) ?: run { LaunchedEffect(Unit) { onBack() }; return }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(p.name, onBack, subtitle = p.type.title)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(typeIcon(p.type), typeBrush(p.type), size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleLarge, color = Palette.Text)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("شدت: ", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted)
                                LevelDots(p.severity, 3, Palette.Coral)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(p.summary, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                }
                ExpandCard("نشانه‌ها", Icons.Rounded.Visibility, Palette.SkyBrush, initiallyOpen = true) { BulletList(p.signs, dot = Palette.Sky) }
                ExpandCard("علت‌ها", Icons.Rounded.Psychology, Palette.OrangeBrush) { BulletList(p.causes, dot = Palette.Orange) }
                ExpandCard("اقدام فوری", Icons.Rounded.Bolt, Palette.CoralBrush, initiallyOpen = true) { BulletList(p.immediate, numbered = true, dot = Palette.Coral) }
                if (p.organic.isNotEmpty()) ExpandCard("درمان خانگی و ارگانیک", Icons.Rounded.Eco, Palette.LeafBrush, initiallyOpen = true) { BulletList(p.organic, dot = Palette.Leaf) }
                if (p.chemical.isNotEmpty()) ExpandCard("درمان شیمیایی", Icons.Rounded.Science, Palette.VioletBrush) { BulletList(p.chemical, dot = Palette.Violet) }
                ExpandCard("پیشگیری", Icons.Rounded.Shield, Palette.MintBrush) { BulletList(p.prevent, dot = Palette.Mint) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timelapse, null, tint = Palette.Teal, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("زمان بهبود: ${p.recovery}", style = MaterialTheme.typography.labelLarge, color = Palette.Teal)
                }
                val plants = PlantDb.all.filter { p.id in it.problems }
                if (plants.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    SectionLabel("گیاهان مستعد")
                    plants.forEach { pl -> PlantRow(pl) { onPlant(pl) } }
                }
            }
        }
    }
}

/* ═════════════════════════  آموزش‌ها  ═════════════════════════ */
fun guideIcon(k: String): ImageVector = when (k) {
    "water" -> Icons.Rounded.WaterDrop; "light" -> Icons.Rounded.WbSunny; "pot" -> Icons.Rounded.Inventory2; "soil" -> Icons.Rounded.Landscape
    "fert" -> Icons.Rounded.Science; "cut" -> Icons.Rounded.ContentCut; "bug" -> Icons.Rounded.BugReport; "humidity" -> Icons.Rounded.Opacity
    "travel" -> Icons.Rounded.Luggage; "shop" -> Icons.Rounded.ShoppingBag; "pet" -> Icons.Rounded.Pets; "winter" -> Icons.Rounded.AcUnit
    else -> Icons.Rounded.School
}

@Composable
fun GuidesScreen(onBack: () -> Unit, onGuide: (String) -> Unit) {
    val brushes = listOf(Palette.SkyBrush, Palette.OrangeBrush, Palette.SoilBrush, Palette.LeafBrush, Palette.MintBrush, Palette.TealBrush, Palette.CoralBrush, Palette.VioletBrush, Palette.GoldBrush, Palette.PinkBrush)
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("آموزش نگهداری", onBack, subtitle = "${Guides.all.size.fa()} آموزش کاربردی")
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Guides.all.size) { i ->
                    val g = Guides.all[i]
                    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onGuide(g.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(guideIcon(g.icon), brushes[i % brushes.size], size = 48.dp, tint = if (brushes[i % brushes.size] == Palette.GoldBrush) Palette.OnGold else Color.White)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.title, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
                                Text(g.summary, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideScreen(vm: MainViewModel, onBack: () -> Unit) {
    val g = Guides.byId(vm.guideId) ?: run { LaunchedEffect(Unit) { onBack() }; return }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(g.title, onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(guideIcon(g.icon), Palette.LeafBrush, size = 56.dp, glow = Palette.Leaf)
                    Spacer(Modifier.width(14.dp))
                    Text(g.summary, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.weight(1f))
                }
                g.sections.forEach { (title, items) ->
                    Spacer(Modifier.height(16.dp))
                    SectionLabel(title)
                    Spacer(Modifier.height(8.dp))
                    GlassCard(Modifier.fillMaxWidth()) { BulletList(items) }
                }
            }
        }
    }
}
