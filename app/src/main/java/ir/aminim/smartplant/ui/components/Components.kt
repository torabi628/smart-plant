package ir.aminim.smartplant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.RecordVoiceOver
import ir.aminim.smartplant.ui.theme.Palette
import ir.aminim.smartplant.util.fa

/* ───────────── پس‌زمینه مش‌گرادیان متحرک ───────────── */
@Composable
fun MeshBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "mesh")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    Box(modifier.fillMaxSize().background(Palette.Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            fun blob(c: Offset, r: Float, color: Color) =
                drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = r), r, c)
            blob(Offset(w * (0.85f - 0.15f * p), h * (0.08f + 0.05f * p)), w * 0.85f, Palette.BlobA)
            blob(Offset(w * (0.05f + 0.2f * p), h * (0.45f - 0.08f * p)), w * 0.75f, Palette.BlobB)
            blob(Offset(w * (0.7f + 0.1f * p), h * (0.95f - 0.06f * p)), w * 0.7f, Palette.BlobC)
        }
        content()
    }
}

/* ───────────── کارت شیشه‌ای ───────────── */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 600f), label = "s")
    Column(
        modifier
            .scale(scale)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Palette.CardTop, Palette.CardBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Palette.CardEdgeTop, Palette.CardEdgeBottom)), shape)
            .then(if (onClick != null) Modifier.clickable(source, null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/* ───────────── دکمه طلایی اصلی ───────────── */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    brush: Brush = Palette.GoldBrush,
    contentColor: Color = Palette.OnGold,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 700f), label = "b")
    Row(
        modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (enabled) 18.dp else 0.dp, RoundedCornerShape(20.dp), ambientColor = Palette.Gold, spotColor = Palette.Gold)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(source, null, enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = contentColor, strokeWidth = 2.5.dp)
        } else {
            icon?.let { Icon(it, null, tint = contentColor, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)) }
            Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
        }
    }
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = Palette.Text) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = tint, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = tint, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = Palette.Text) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.48f)) }
}

/* ───────────── نوار بالای صفحه ───────────── */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onBack?.let { CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, it); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.Text)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/* ───────────── نشان آیکن گرادیانی ───────────── */
@Composable
fun IconBadge(icon: ImageVector, brush: Brush, size: Dp = 48.dp, glow: Color? = null, tint: Color = Color.White) {
    Box(
        Modifier
            .then(if (glow != null) Modifier.shadow(14.dp, RoundedCornerShape(size * 0.32f), ambientColor = glow, spotColor = glow) else Modifier)
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Palette.GoldBrush))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = Palette.Text, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** متن با گرادیان طلایی */
@Composable
fun GradientText(text: String, style: TextStyle, brush: Brush = Palette.TitleBrush, modifier: Modifier = Modifier, align: TextAlign? = null) {
    Text(text, modifier = modifier, style = style.copy(brush = brush), textAlign = align)
}

/* ───────────── فیلد ورودی ───────────── */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    icon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Palette.TextFaint) },
        leadingIcon = if (icon != null) { { Icon(icon, null, tint = Palette.Gold) } } else null,
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(18.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Gold,
            unfocusedBorderColor = Palette.GlassBorder,
            focusedContainerColor = Palette.FieldFocused,
            unfocusedContainerColor = Palette.FieldIdle,
            focusedLabelColor = Palette.Gold,
            unfocusedLabelColor = Palette.TextMuted,
            cursorColor = Palette.Gold,
        ),
    )
}

/* ───────────── ورودی چیپ (حاضرین، دستور جلسه، …) ───────────── */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipInput(items: List<String>, onChange: (List<String>) -> Unit, label: String, placeholder: String, icon: ImageVector) {
    var text by remember { mutableStateOf("") }
    fun commit() {
        val parts = text.split('،', ',', '\n').map { it.trim() }.filter { it.isNotEmpty() && it !in items }
        if (parts.isNotEmpty()) onChange(items + parts)
        text = ""
    }
    Column {
        AppTextField(
            value = text,
            onValueChange = { v ->
                text = v
                if (v.endsWith("،") || v.endsWith(",")) commit()
            },
            label = label,
            placeholder = placeholder,
            icon = icon,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailing = {
                Box(
                    Modifier.padding(end = 6.dp).size(34.dp).clip(CircleShape).background(Palette.GoldBrush).clickable { commit() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, null, tint = Palette.OnGold, modifier = Modifier.size(20.dp)) }
            },
        )
        if (items.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(Palette.GlassStrong)
                            .border(1.dp, Palette.GlassBorder, RoundedCornerShape(14.dp))
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item, style = MaterialTheme.typography.labelMedium, color = Palette.Text)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Close, "حذف", tint = Palette.TextMuted,
                            modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onChange(items - item) },
                        )
                    }
                }
            }
        }
    }
}

/* ───────────── نشانگر مراحل ───────────── */
@Composable
fun StepIndicator(current: Int, labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { i, l ->
            val active = i <= current
            val w by animateFloatAsState(if (i == current) 1f else 0f, label = "w")
            Column(Modifier.weight(1f + w * 0.6f)) {
                Box(
                    Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (active) Palette.GoldBrush else Brush.linearGradient(listOf(Palette.GlassStrong, Palette.GlassStrong)))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${(i + 1).fa()}. $l",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == current) Palette.Gold else if (active) Palette.TextMuted else Palette.TextFaint,
                    fontWeight = if (i == current) FontWeight.Bold else FontWeight.Medium,
                )
            }
            if (i < labels.lastIndex) Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun Pill(text: String, icon: ImageVector? = null, color: Color = Palette.TextMuted, bg: Color = Palette.Glass) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).border(1.dp, Palette.GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun BottomBarContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Bg.copy(alpha = 0.92f), Palette.Bg)))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 14.dp),
        content = content,
    )
}



/** راهنمای بزرگ صحبت کردن؛ در همه بخش‌هایی که صدا از کاربر گرفته می‌شود */
const val SPEAK_HINT = "بزرگ، شمرده شمرده و بلند صحبت کنید تا هوش مصنوعی بتواند تشخیص دهد"

@Composable
fun SpeakHint(modifier: Modifier = Modifier, color: Color = Palette.Gold, bg: Color = Palette.GoldTint) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(androidx.compose.material.icons.Icons.Rounded.RecordVoiceOver, null, tint = color, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            SPEAK_HINT, color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 32.sp),
        )
    }
}
