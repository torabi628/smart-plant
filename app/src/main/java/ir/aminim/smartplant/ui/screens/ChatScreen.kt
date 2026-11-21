package ir.aminim.smartplant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ir.aminim.smartplant.MainViewModel
import ir.aminim.smartplant.data.ChatMsg
import ir.aminim.smartplant.ui.components.*
import ir.aminim.smartplant.ui.theme.Palette

/* ═════════════════════════  پزشک گیاه (گفتگو)  ═════════════════════════ */
@Composable
fun ChatScreen(vm: MainViewModel, onBack: () -> Unit) {
    var input by remember { mutableStateOf(vm.chatDraft.also { vm.chatDraft = "" }) }
    val list = rememberLazyListState()
    val (camera, gallery) = rememberPhotoPickers { vm.attachChatImage(it) }
    val dict = remember { vm.newDictation() }
    var base by remember { mutableStateOf("") }
    DisposableEffect(Unit) { onDispose { dict.release() } }
    LaunchedEffect(dict.text) { if (dict.listening && dict.text.isNotBlank()) input = listOf(base, dict.text).filter { it.isNotBlank() }.joinToString(" ") }
    val startMic = rememberMicPermission { base = input; dict.clear(); dict.start() }
    LaunchedEffect(vm.chat.size, vm.chatBusy) { if (vm.chat.isNotEmpty()) list.animateScrollToItem(vm.chat.size + (if (vm.chatBusy) 1 else 0)) }

    fun send() { if (dict.listening) dict.stop(); vm.send(input); input = "" }

    MeshBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("پزشک گیاه", onBack, subtitle = vm.chatAbout?.let { "درباره: ${it.title}" } ?: "هر پرسشی درباره گیاهان دارید بپرسید") {
                if (vm.chat.isNotEmpty()) CircleIconButton(Icons.Rounded.DeleteSweep, { vm.chat.clear() }, size = 40.dp)
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    if (vm.chat.isEmpty()) ChatWelcome(vm) { input = it }
                }
                itemsIndexed(vm.chat) { _, m -> Bubble(m) }
                if (vm.chatBusy) item { TypingBubble() }
            }
            Column(Modifier.fillMaxWidth().background(Palette.Bg.copy(alpha = 0.95f)).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                AnimatedVisibility(dict.listening) { Column { SpeakHint(); Spacer(Modifier.height(6.dp)); VoiceWave(dict.level); Spacer(Modifier.height(6.dp)) } }
                vm.chatImage?.let { img ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) { PhotoThumb(img, Modifier.fillMaxSize(), 300) }
                        Spacer(Modifier.width(8.dp))
                        Text("عکس پیوست شد", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted, modifier = Modifier.weight(1f))
                        IconButton({ vm.chatImage = null }) { Icon(Icons.Rounded.Close, null, tint = Palette.TextMuted) }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        CircleIconButton(Icons.Rounded.AddPhotoAlternate, { menu = true }, size = 46.dp, tint = Palette.Leaf)
                        DropdownMenu(menu, { menu = false }, containerColor = Palette.Surface) {
                            DropdownMenuItem({ Text("دوربین") }, { menu = false; camera() }, leadingIcon = { Icon(Icons.Rounded.CameraAlt, null) })
                            DropdownMenuItem({ Text("گالری") }, { menu = false; gallery() }, leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) })
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        input, { input = it }, Modifier.weight(1f),
                        placeholder = { Text("پرسش خود را بنویسید یا بگویید…", color = Palette.TextFaint) },
                        maxLines = 5, shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.Text),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.Leaf, unfocusedBorderColor = Palette.GlassBorder, cursorColor = Palette.Leaf,
                            focusedContainerColor = Palette.FieldFocused, unfocusedContainerColor = Palette.FieldIdle),
                    )
                    Spacer(Modifier.width(8.dp))
                    if (input.isBlank() && vm.chatImage == null) MicButton(dict.listening, dict.level, { if (dict.listening) dict.stop() else startMic() }, 50.dp)
                    else Box(Modifier.size(50.dp).clip(CircleShape).background(Palette.LeafBrush).clickable(enabled = !vm.chatBusy) { send() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.Send, "ارسال", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatWelcome(vm: MainViewModel, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(Icons.Rounded.MedicalServices, Palette.TealBrush, size = 76.dp, glow = Palette.Teal)
        Spacer(Modifier.height(14.dp))
        GradientText("سلام! من پزشک گیاه هستم", MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("عکس بفرستید، با صدا بپرسید یا بنویسید. درباره بیماری، آفت، نگهداری، خرید و تکثیر گیاهان کمک‌تان می‌کنم.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        val sug = if (vm.chatAbout != null) listOf("دقیقاً چه سمی بخرم و چطور مصرف کنم؟", "آیا به گیاهان دیگرم سرایت می‌کند؟", "چطور بفهمم درمان جواب داده؟", "گلدانش را عوض کنم؟")
        else listOf("پتوس می‌خواهم بخرم؛ چه شرایطی لازم دارد؟", "برای اتاق کم‌نور چه گیاهی خوب است؟", "چطور از سانسوریا قلمه بگیرم؟", "چرا نوک برگ‌های نخلم قهوه‌ای می‌شود؟", "برای سفر دو هفته‌ای با گیاهانم چه کنم؟", "گیاه بی‌خطر برای گربه پیشنهاد بده")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sug.forEach { s -> SelectChip(s, false, { onPick(s) }, color = Palette.Teal, icon = Icons.Rounded.ChatBubbleOutline) }
        }
    }
}

@Composable
private fun Bubble(m: ChatMsg) {
    val user = m.role == "user"
    val clip = LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.Start else Arrangement.End) {
        Column(
            Modifier.fillMaxWidth(if (user) 0.85f else 0.95f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (user) 6.dp else 20.dp, bottomEnd = if (user) 20.dp else 6.dp))
                .background(if (user) Palette.BubbleUser else Palette.BubbleBot)
                .border(1.dp, if (m.error) Palette.Coral.copy(alpha = 0.5f) else Palette.GlassBorder, RoundedCornerShape(20.dp))
                .padding(12.dp),
        ) {
            m.image?.let { PhotoThumb(it, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)), 700); Spacer(Modifier.height(8.dp)) }
            if (user) Text(m.text, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Spa, null, tint = Palette.Leaf, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text("پزشک گیاه", style = MaterialTheme.typography.labelMedium, color = Palette.Leaf, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ContentCopy, "کپی", tint = Palette.TextFaint, modifier = Modifier.size(16.dp).clickable { clip.setText(AnnotatedString(m.text)) })
                }
                Spacer(Modifier.height(6.dp))
                RichText(m.text, color = if (m.error) Palette.Coral else Palette.Text)
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val t = rememberInfiniteTransition(label = "t")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Palette.BubbleBot).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse), label = "a$i")
                Box(Modifier.padding(horizontal = 3.dp).size(9.dp).alpha(a).clip(CircleShape).background(Palette.Leaf))
            }
            Spacer(Modifier.width(10.dp))
            Text("در حال بررسی…", style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted)
        }
    }
}
