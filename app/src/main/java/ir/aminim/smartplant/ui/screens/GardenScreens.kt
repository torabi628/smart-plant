package ir.aminim.smartplant.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.data.MyPlant
import ir.aminim.smartplant.knowledge.PlantDb
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.util.Jalali
import ir.aminim.smartplant.util.fa

/* ═════════════════════════  باغچه من  ═════════════════════════ */
@Composable
fun GardenScreen(vm: MainViewModel, onBack: () -> Unit, onEdit: (MyPlant?) -> Unit, onPlant: (PlantInfo) -> Unit) {
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33 && vm.settings.reminders && vm.garden.isNotEmpty()) notif.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val sorted = vm.garden.sortedBy { minOf(it.waterInDays(), it.fertInDays()) }
    MeshBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TopBar("باغچه من", onBack, subtitle = "${vm.garden.size.fa()} گیاه • یادآور ${if (vm.settings.reminders) "روشن، ساعت ${vm.settings.reminderHour.fa()}" else "خاموش"}")
                if (vm.garden.isEmpty()) EmptyState(Icons.Rounded.Yard, "باغچه شما خالی است", "گیاهان خانه را اضافه کنید تا زمان آبیاری و کوددهی را یادآوری کنیم.")
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (vm.garden.isNotEmpty()) {
                        val due = vm.garden.count { it.waterInDays() <= 0 }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoTile(Icons.Rounded.WaterDrop, "آبیاری امروز", due.fa(), Palette.Sky, Modifier.weight(1f))
                            InfoTile(Icons.Rounded.Science, "کوددهی", vm.garden.count { it.fertInDays() <= 0 }.fa(), Palette.Mint, Modifier.weight(1f))
                            InfoTile(Icons.Rounded.Opacity, "کل آبیاری‌ها", vm.garden.sumOf { it.waterCount }.fa(), Palette.Teal, Modifier.weight(1f))
                        }
                    }
                    sorted.forEach { p -> GardenCard(vm, p, { onEdit(p) }, { PlantDb.byId(p.plantId)?.let(onPlant) }) }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp)) {
                GoldButton("افزودن گیاه", { onEdit(null) }, icon = Icons.Rounded.Add, brush = Palette.LeafBrush, contentColor = Color(0xFF06301A))
            }
        }
    }
}

@Composable
private fun GardenCard(vm: MainViewModel, p: MyPlant, onEdit: () -> Unit, onInfo: () -> Unit) {
    val w = p.waterInDays()
    val f = p.fertInDays()
    val wc = when { w <= 0 -> Palette.Coral; w <= 1 -> Palette.Orange; else -> Palette.Sky }
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(74.dp).clip(RoundedCornerShape(20.dp))) {
                if (p.photo.isNotBlank()) PhotoThumb(p.photo, Modifier.fillMaxSize(), 300)
                else Box(Modifier.fillMaxSize().background(Palette.LeafBrush), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Eco, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name.ifBlank { p.species.ifBlank { "گیاه من" } }, style = MaterialTheme.typography.titleMedium, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(p.species, p.location).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = wc, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp))
                    Text(when { w < 0 -> "${(-w).fa()} روز عقب افتاده"; w == 0 -> "امروز آبیاری"; w == Int.MAX_VALUE -> "بدون برنامه"; else -> "${w.fa()} روز دیگر" },
                        style = MaterialTheme.typography.labelMedium, color = wc, fontWeight = FontWeight.Bold)
                }
                if (p.fertEvery > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Science, null, tint = Palette.Mint, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp))
                    Text(if (f <= 0) "نوبت کوددهی" else "کود: ${f.fa()} روز دیگر", style = MaterialTheme.typography.labelSmall, color = Palette.Mint)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(Palette.SkyBrush).clickable { vm.water(p) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.WaterDrop, "آبیاری کردم", tint = Color.White)
                }
                if (p.fertEvery > 0 && f <= 0) Box(Modifier.size(36.dp).clip(CircleShape).background(Palette.MintBrush).clickable { vm.fertilize(p) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Science, "کود دادم", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                if (p.plantId.isNotBlank()) Icon(Icons.Rounded.Info, "راهنما", tint = Palette.TextMuted, modifier = Modifier.size(20.dp).clickable(onClick = onInfo))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("آخرین آبیاری: ${Jalali.dayMonth(p.lastWater)}", style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GardenEditScreen(vm: MainViewModel, onBack: () -> Unit) {
    val orig = remember { vm.editing ?: vm.newGardenPlant() }
    val isNew = vm.garden.none { it.id == orig.id }
    var name by remember { mutableStateOf(orig.name) }
    var species by remember { mutableStateOf(orig.species) }
    var plantId by remember { mutableStateOf(orig.plantId) }
    var location by remember { mutableStateOf(orig.location) }
    var photo by remember { mutableStateOf(orig.photo) }
    var waterEvery by remember { mutableFloatStateOf(orig.waterEvery.toFloat()) }
    var fertEvery by remember { mutableFloatStateOf(orig.fertEvery.toFloat()) }
    var notes by remember { mutableStateOf(orig.notes) }
    var confirm by remember { mutableStateOf(false) }
    val (camera, gallery) = rememberPhotoPickers { uri -> vm.setGardenPhoto(uri) { photo = it } }
    MeshBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (isNew) "افزودن گیاه" else "ویرایش گیاه", onBack) {
                if (!isNew) CircleIconButton(Icons.Rounded.DeleteOutline, { confirm = true }, size = 40.dp, tint = Palette.Coral)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(110.dp).clip(RoundedCornerShape(26.dp)).border(1.dp, Palette.GlassBorder, RoundedCornerShape(26.dp))) {
                        if (photo.isNotBlank()) PhotoThumb(photo, Modifier.fillMaxSize(), 400)
                        else Box(Modifier.fillMaxSize().background(Palette.LeafTint), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Eco, null, tint = Palette.Leaf, modifier = Modifier.size(44.dp)) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("دوربین", camera, icon = Icons.Rounded.CameraAlt, tint = Palette.Leaf)
                        GlassButton("گالری", gallery, icon = Icons.Rounded.PhotoLibrary, tint = Palette.Leaf)
                    }
                }
                Spacer(Modifier.height(16.dp))
                AppTextField(name, { name = it }, "نام دلخواه", placeholder = "مثلاً پتوس پذیرایی", icon = Icons.Rounded.Badge)
                Spacer(Modifier.height(12.dp))
                AppTextField(species, { species = it; if (PlantDb.byId(plantId)?.fa != it) plantId = "" }, "نوع گیاه", placeholder = "از دانشنامه انتخاب کنید یا بنویسید", icon = Icons.Rounded.LocalFlorist)
                if (species.isNotBlank() && plantId.isBlank()) {
                    val sug = PlantDb.all.filter { it.matches(species) }.take(8)
                    if (sug.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sug.forEach { p -> SelectChip(p.fa, false, {
                                species = p.fa; plantId = p.id
                                if (name.isBlank()) name = p.fa
                                val warm = Jalali.of(System.currentTimeMillis()).month in 1..6
                                waterEvery = (if (warm) p.waterSummer else p.waterWinter).toFloat()
                                fertEvery = if (warm) 21f else 0f
                            }, icon = Icons.Rounded.Eco) }
                        }
                    }
                }
                plantId.takeIf { it.isNotBlank() }?.let { PlantDb.byId(it) }?.let { info ->
                    Spacer(Modifier.height(8.dp))
                    Text("پیشنهاد دانشنامه: فصل گرم هر ${info.waterSummer.fa()} روز، فصل سرد هر ${info.waterWinter.fa()} روز. ${info.water}", style = MaterialTheme.typography.bodySmall, color = Palette.Leaf)
                }
                Spacer(Modifier.height(12.dp))
                AppTextField(location, { location = it }, "محل نگهداری", placeholder = "مثلاً کنار پنجره شرقی پذیرایی", icon = Icons.Rounded.Place)
                Spacer(Modifier.height(18.dp))
                Text("آبیاری هر ${waterEvery.toInt().fa()} روز", style = MaterialTheme.typography.titleSmall, color = Palette.Sky)
                Slider(waterEvery, { waterEvery = it }, valueRange = 1f..40f, steps = 38, colors = SliderDefaults.colors(thumbColor = Palette.Sky, activeTrackColor = Palette.Sky))
                Text(if (fertEvery < 1f) "کوددهی: خاموش (پاییز و زمستان)" else "کوددهی هر ${fertEvery.toInt().fa()} روز", style = MaterialTheme.typography.titleSmall, color = Palette.Mint)
                Slider(fertEvery, { fertEvery = it }, valueRange = 0f..60f, steps = 59, colors = SliderDefaults.colors(thumbColor = Palette.Mint, activeTrackColor = Palette.Mint))
                Spacer(Modifier.height(8.dp))
                AppTextField(notes, { notes = it }, "یادداشت", placeholder = "مثلاً تاریخ تعویض گلدان، کود مصرفی…", singleLine = false, minLines = 3, icon = Icons.Rounded.EditNote)
                Spacer(Modifier.height(10.dp))
                Text("نکته: یادآور فقط زمان را یادآوری می‌کند؛ همیشه پیش از آبیاری خشکی خاک را با انگشت بسنجید.", style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
            }
            BottomBarContainer {
                GoldButton("ذخیره", {
                    vm.upsert(orig.copy(name = name.trim().ifBlank { species.ifBlank { "گیاه من" } }, species = species.trim(), plantId = plantId, location = location.trim(),
                        photo = photo, waterEvery = waterEvery.toInt(), fertEvery = fertEvery.toInt(), notes = notes.trim()))
                    vm.editing = null; onBack()
                }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check, brush = Palette.LeafBrush, contentColor = Color(0xFF06301A))
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("حذف از باغچه؟") },
        text = { Text("«${orig.name}» از باغچه شما حذف می‌شود.") },
        confirmButton = { TextButton({ vm.deletePlant(orig); vm.editing = null; confirm = false; onBack() }) { Text("حذف", color = Palette.Coral) } },
        dismissButton = { TextButton({ confirm = false }) { Text("انصراف") } },
        containerColor = Palette.Surface,
    )
}
