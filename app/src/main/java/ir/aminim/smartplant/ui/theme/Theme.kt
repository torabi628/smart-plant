package ir.aminim.smartplant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import ir.aminim.smartplant.R

/** حالت ظاهری برنامه (قابل تغییر از منو و تنظیمات) */
object ThemeState {
    var dark by mutableStateOf(true)
}

/* ─────────────  پالت «جوهر و طلا»: برگرفته از هویت بصری گروه پژوهشی مدیریت هوشمند امین  ─────────────
 * رنگ‌های وابسته به حالت تیره/روشن getter هستند تا با تغییر حالت، کل رابط بازترسیم شود. */
object Palette {
    private val d get() = ThemeState.dark

    // ثابت
    val Ink = Color(0xFF041420)
    val OnGold = Color(0xFF0A1A2A)
    val GoldC = Color(0xFFEDB850)
    /** طلایی متن و آیکن؛ در حالت روشن تیره‌تر تا روی زمینه روشن خوانا باشد */
    val Gold: Color get() = if (d) GoldC else Color(0xFF9A6408)
    val GoldLight = Color(0xFFF8D88A)
    val GoldDeep = Color(0xFFD4952A)
    val TealDeep = Color(0xFF16626B)
    val Paper = Color(0xFFFFFDF8)
    val PaperInk = Color(0xFF1B2333)

    // وابسته به حالت
    val Bg get() = if (d) Color(0xFF061826) else Color(0xFFF1F5F6)
    val Surface get() = if (d) Color(0xFF0C2636) else Color(0xFFFFFFFF)
    val SurfaceHigh get() = if (d) Color(0xFF113246) else Color(0xFFE7EEF0)
    val Drawer get() = if (d) Color(0xFF071C2B) else Color(0xFFFAFCFC)
    val Teal get() = if (d) Color(0xFF45D3C7) else Color(0xFF0E8C84)
    val Sky get() = if (d) Color(0xFF7CB2FF) else Color(0xFF2F6FD6)
    val Violet get() = if (d) Color(0xFF9C8CFF) else Color(0xFF5B4BD6)
    val Mint get() = if (d) Color(0xFF5BE3A8) else Color(0xFF14936A)
    val Coral get() = if (d) Color(0xFFFF6B7A) else Color(0xFFD93A4F)
    val Orange get() = if (d) Color(0xFFFFA36B) else Color(0xFFD9692B)
    val Pink get() = if (d) Color(0xFFF07FC0) else Color(0xFFC2378A)
    /** سبز برگ: رنگ امضای «گیاه‌شناس هوشمند» */
    val Leaf get() = if (d) Color(0xFF7BE08F) else Color(0xFF1E8C3A)
    val LeafTint: Color get() = if (d) Color(0x2E7BE08F) else Color(0x221E8C3A)
    val Text get() = if (d) Color(0xFFF1F6F7) else Color(0xFF0E1B26)
    val TextMuted get() = if (d) Color(0xFFA3B6C0) else Color(0xFF52626E)
    val TextFaint get() = if (d) Color(0xFF6C8290) else Color(0xFF6A7A85)
    val Glass get() = if (d) Color(0x14FFFFFF) else Color(0x0D0A3040)
    val GlassStrong get() = if (d) Color(0x22FFFFFF) else Color(0x170A3040)
    val GlassBorder get() = if (d) Color(0x26FFFFFF) else Color(0x1F0A3040)
    val CardTop get() = if (d) Color(0x1FFFFFFF) else Color(0xFFFFFFFF)
    val CardBottom get() = if (d) Color(0x0AFFFFFF) else Color(0xF7FFFFFF)
    val CardEdgeTop get() = if (d) Color(0x40FFFFFF) else Color(0x1A0A3040)
    val CardEdgeBottom get() = if (d) Color(0x0DFFFFFF) else Color(0x0F0A3040)
    val FieldFocused get() = if (d) Color(0x14FFFFFF) else Color(0xFFFFFFFF)
    val FieldIdle get() = if (d) Color(0x0DFFFFFF) else Color(0xFFFFFFFF)
    val GoldTint: Color get() = if (d) Color(0x33EDB850) else Color(0x33D4952A)
    val BubbleUser get() = if (d) Color(0xFF16505A) else Color(0xFFD8F0EE)
    val BubbleBot get() = if (d) Color(0x1AFFFFFF) else Color(0xFFFFFFFF)
    val BlobA get() = if (d) Color(0x8016626B) else Color(0x2E16626B)
    val BlobB get() = if (d) Color(0x339C8CFF) else Color(0x1F9C8CFF)
    val BlobC: Color get() = if (d) Color(0x2EEDB850) else Color(0x29EDB850)

    val GoldBrush = Brush.linearGradient(listOf(GoldLight, GoldC, GoldDeep))
    /** گرادیان متن تیترها (در حالت روشن، طلایی سوخته و پررنگ) */
    val TitleBrush: Brush get() = if (d) GoldBrush else Brush.linearGradient(listOf(Color(0xFFC07F10), Color(0xFF8A5600), Color(0xFF6E4300)))
    val TealBrush = Brush.linearGradient(listOf(Color(0xFF6FE6DA), Color(0xFF16929A)))
    val InkBrush = Brush.linearGradient(listOf(Color(0xFF1F7A82), Color(0xFF0B2A45)))
    val SkyBrush = Brush.linearGradient(listOf(Color(0xFF9CC5FF), Color(0xFF4F86E8)))
    val VioletBrush = Brush.linearGradient(listOf(Color(0xFFB9AEFF), Color(0xFF6A5AF0)))
    val MintBrush = Brush.linearGradient(listOf(Color(0xFF8AF0CC), Color(0xFF2FB487)))
    val CoralBrush = Brush.linearGradient(listOf(Color(0xFFFF9AA5), Color(0xFFE5485A)))
    val OrangeBrush = Brush.linearGradient(listOf(Color(0xFFFFC39A), Color(0xFFF07A3A)))
    val PinkBrush = Brush.linearGradient(listOf(Color(0xFFFFA8D9), Color(0xFFD0479A)))
    val HeroBrush = Brush.linearGradient(listOf(Color(0xFF1F8A5E), Color(0xFF136065), Color(0xFF1B3F78)))
    val LeafBrush = Brush.linearGradient(listOf(Color(0xFFA6F0A8), Color(0xFF2FA05A)))
    val SoilBrush = Brush.linearGradient(listOf(Color(0xFFE0B48A), Color(0xFF9A5B2E)))

    /** رنگ‌های سری نمودارها */
    val Series get() = listOf(Gold, Teal, Violet, Sky, Coral, Mint, Orange, Pink, Color(0xFF5ED6E0), Color(0xFFB7C46B))
}

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

private fun s(size: Int, w: FontWeight, lh: Int = (size * 1.6).toInt()) =
    TextStyle(fontFamily = Vazirmatn, fontWeight = w, fontSize = size.sp, lineHeight = lh.sp)

val AppTypography = Typography(
    displayLarge = s(40, FontWeight.Black, 52),
    displayMedium = s(32, FontWeight.ExtraBold, 44),
    displaySmall = s(28, FontWeight.ExtraBold, 40),
    headlineLarge = s(26, FontWeight.ExtraBold, 38),
    headlineMedium = s(22, FontWeight.Bold, 34),
    headlineSmall = s(19, FontWeight.Bold, 30),
    titleLarge = s(18, FontWeight.Bold, 28),
    titleMedium = s(16, FontWeight.Bold, 26),
    titleSmall = s(14, FontWeight.Medium, 22),
    bodyLarge = s(16, FontWeight.Normal, 28),
    bodyMedium = s(14, FontWeight.Normal, 24),
    bodySmall = s(12, FontWeight.Normal, 20),
    labelLarge = s(15, FontWeight.Bold, 22),
    labelMedium = s(13, FontWeight.Medium, 18),
    labelSmall = s(11, FontWeight.Medium, 16),
)

@Composable
fun SmartPlantTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val scheme = if (ThemeState.dark) darkColorScheme(
            primary = Palette.GoldC, onPrimary = Palette.OnGold, secondary = Palette.Teal, onSecondary = Palette.OnGold,
            tertiary = Palette.Violet, background = Palette.Bg, onBackground = Palette.Text, surface = Palette.Surface,
            onSurface = Palette.Text, surfaceVariant = Palette.SurfaceHigh, onSurfaceVariant = Palette.TextMuted,
            outline = Palette.GlassBorder, error = Palette.Coral, surfaceContainerLow = Palette.Surface,
            surfaceContainer = Palette.Surface, surfaceContainerHigh = Palette.SurfaceHigh, surfaceContainerHighest = Palette.SurfaceHigh,
        ) else lightColorScheme(
            primary = Palette.GoldDeep, onPrimary = Palette.OnGold, secondary = Palette.Teal, onSecondary = Color.White,
            tertiary = Palette.Violet, background = Palette.Bg, onBackground = Palette.Text, surface = Color.White,
            onSurface = Palette.Text, surfaceVariant = Palette.SurfaceHigh, onSurfaceVariant = Palette.TextMuted,
            outline = Palette.GlassBorder, error = Palette.Coral, surfaceContainerLow = Color.White,
            surfaceContainer = Color.White, surfaceContainerHigh = Palette.SurfaceHigh, surfaceContainerHighest = Palette.SurfaceHigh,
        )
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
