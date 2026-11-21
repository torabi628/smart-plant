package ir.aminim.smartplant.settings

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** موتور تبدیل گفتار */
enum class SpeechEngine(val title: String, val subtitle: String) {
    GOOGLE("آنلاین (Google)", "متن هم‌زمان با دقت بالا؛ نیازمند اینترنت"),
    OFFLINE("آفلاین (Vosk)", "بدون اینترنت؛ پس از دانلود مدل گفتار فارسی"),
}

/** موتور هوش مصنوعی تشخیص (به ترتیب نمایش؛ اولی پیش‌فرض است) */
enum class AiMode(val title: String, val subtitle: String) {
    INTERNAL("هوش مصنوعی قوی داخلی", "آوال‌ای • دقیق‌ترین تشخیص • ۳ تشخیص در روز؛ بعد از آن خودکار به گزینه بعد می‌رود"),
    EXTERNAL("هوش مصنوعی قوی خارجی", "Pollinations • بدون محدودیت"),
    OFFLINE("هوش مصنوعی آفلاین (ضعیف)", "بدون اینترنت؛ تشخیص بر پایه علائم و دانشنامه داخلی"),
    PERSONAL("هوش مصنوعی اختیاری (کلید شخصی)", "سرویس و کلید دلخواه خودتان");

    companion object {
        fun of(v: String): AiMode = entries.firstOrNull { it.name == v } ?: INTERNAL
    }
}

/** تنظیمات ماندگار برنامه */
class AppSettings(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun str(k: String, d: String = "") = p.getString(k, d) ?: d
    private fun put(k: String, v: String) = p.edit().putString(k, v).apply()
    private fun bool(k: String, d: Boolean) = p.getBoolean(k, d)
    private fun putBool(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()

    var userName: String get() = str("user_name"); set(v) = put("user_name", v.trim())
    var city: String get() = str("city"); set(v) = put("city", v.trim())
    var themeMode: String get() = str("theme_mode", "dark"); set(v) = put("theme_mode", v)
    var onboarded: Boolean get() = bool("onboarded", false); set(v) = putBool("onboarded", v)

    var speechEngine: SpeechEngine
        get() = SpeechEngine.entries.firstOrNull { it.name == str("speech_engine") } ?: SpeechEngine.GOOGLE
        set(v) = put("speech_engine", v.name)
    var asrModelId: String get() = str("asr_model", "small"); set(v) = put("asr_model", v)

    // ───── هوش مصنوعی
    var aiMode: AiMode get() = AiMode.of(str("ai_mode")); set(v) = put("ai_mode", v.name)
    var freeAi: Boolean get() = bool("free_ai", true); set(v) = putBool("free_ai", v)
    var cloudProvider: String get() = str("cloud_provider", "avalai"); set(v) = put("cloud_provider", v)
    var cloudUrl: String get() = str("cloud_url", "https://api.avalai.ir/v1"); set(v) = put("cloud_url", v.trim().trimEnd('/'))
    var cloudKey: String get() = str("cloud_key"); set(v) = put("cloud_key", v.trim())
    var cloudModel: String get() = str("cloud_model", "gpt-4.1"); set(v) = put("cloud_model", v.trim())

    /* ───── سهمیه روزانه هوش مصنوعی قوی داخلی ───── */
    private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    val internalUsed: Int get() = if (str("internal_day") == today()) p.getInt("internal_used", 0) else 0
    val internalLeft: Int get() = (INTERNAL_DAILY - internalUsed).coerceAtLeast(0)
    fun consumeInternal() { p.edit().putString("internal_day", today()).putInt("internal_used", internalUsed + 1).apply() }

    // ───── یادآور
    var reminders: Boolean get() = bool("reminders", true); set(v) = putBool("reminders", v)
    var reminderHour: Int get() = p.getInt("reminder_hour", 9); set(v) = p.edit().putInt("reminder_hour", v).apply()

    companion object {
        /** سقف روزانه تشخیص با هوش مصنوعی قوی داخلی */
        const val INTERNAL_DAILY = 3
    }
}
