package ir.aminim.smartplant.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import ir.aminim.smartplant.BuildConfig
import ir.aminim.smartplant.settings.AiMode
import ir.aminim.smartplant.settings.AppSettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** سرویس‌دهنده هوش مصنوعی ابری (سازگار با OpenAI) */
data class AiProvider(val id: String, val title: String, val subtitle: String, val baseUrl: String, val model: String, val signupUrl: String = "")

/** یک پیام گفتگو؛ تصاویر به صورت base64 (JPEG) */
data class Msg(val role: String, val text: String, val images: List<String> = emptyList())

class AiException(msg: String) : Exception(msg)

/**
 * اتصال به هوش مصنوعی ابری با زنجیره جایگزین (همان ساختار برنامه‌های گروه امین):
 * داخلی:   آوال‌ای (۳ تشخیص در روز) ← Pollinations ← سرویس رایگان
 * خارجی:   Pollinations ← سرویس رایگان
 * اختیاری: کلید شخصی ← Pollinations ← سرویس رایگان
 * آفلاین:  هیچ (موتور علائم و دانشنامه داخلی)
 */
class AiClient(private val context: Context) {
    private val s = AppSettings(context)

    companion object {
        val presets = listOf(
            AiProvider("avalai", "آوال‌ای (AvalAI)", "ایرانی • پرداخت ریالی • GPT، Claude و Gemini", "https://api.avalai.ir/v1", "gpt-4.1", "https://avalai.ir"),
            AiProvider("gapgpt", "گپ‌جی‌پی‌تی (GapGPT)", "ایرانی • پرداخت ریالی", "https://api.gapgpt.app/v1", "gpt-4o", "https://gapgpt.app"),
            AiProvider("pollinations", "Pollinations", "کلید در enter.pollinations.ai", "https://gen.pollinations.ai/v1", "openai", "https://enter.pollinations.ai"),
            AiProvider("openrouter", "OpenRouter", "ده‌ها مدل", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", "https://openrouter.ai/keys"),
            AiProvider("openai", "OpenAI", "سرویس اصلی ChatGPT", "https://api.openai.com/v1", "gpt-4.1-mini", "https://platform.openai.com/api-keys"),
            AiProvider("custom", "سرویس دلخواه", "هر سرویس سازگار با OpenAI (مدل باید تصویر بپذیرد)", "", ""),
        )
        const val FREE_URL = "https://text.pollinations.ai/openai"
        const val INTERNAL_TITLE = "هوش مصنوعی قوی داخلی"
        const val EXTERNAL_TITLE = "هوش مصنوعی قوی خارجی"
        const val FREE_TITLE = "سرویس رایگان عمومی"
        val internalReady: Boolean get() = BuildConfig.AVAL_URL.startsWith("http") && BuildConfig.AVAL_KEY.isNotBlank()
        val externalReady: Boolean get() = BuildConfig.AI_DIRECT_URL.startsWith("http") && BuildConfig.AI_DIRECT_KEY.isNotBlank()
        @Volatile var lastWorking: String = ""
    }

    private data class Route(val id: String, val title: String, val url: String, val key: String, val model: String)

    private fun routes(allowInternal: Boolean): List<Route> {
        val mode = s.aiMode
        if (mode == AiMode.OFFLINE) return emptyList()
        val r = mutableListOf<Route>()
        if (mode == AiMode.PERSONAL && s.cloudUrl.startsWith("http") && s.cloudKey.isNotBlank())
            r += Route("user", "کلید شخصی شما", "${s.cloudUrl}/chat/completions", s.cloudKey, s.cloudModel.ifBlank { "gpt-4o-mini" })
        if (mode == AiMode.INTERNAL && allowInternal && internalReady && s.internalLeft > 0)
            r += Route("internal", INTERNAL_TITLE, "${BuildConfig.AVAL_URL.trimEnd('/')}/chat/completions", BuildConfig.AVAL_KEY, BuildConfig.AVAL_MODEL)
        val external = mutableListOf<Route>()
        if (externalReady) {
            val url = "${BuildConfig.AI_DIRECT_URL.trimEnd('/')}/chat/completions"
            val main = BuildConfig.AI_DIRECT_MODEL.ifBlank { "openai-large" }
            external += Route("direct", EXTERNAL_TITLE, url, BuildConfig.AI_DIRECT_KEY, main)
            if (main != "openai") external += Route("direct2", EXTERNAL_TITLE, url, BuildConfig.AI_DIRECT_KEY, "openai")
        }
        if (s.freeAi) external += Route("free", FREE_TITLE, FREE_URL, "", "openai")
        r += external.sortedBy { if (it.id == lastWorking) 0 else 1 }
        return r
    }

    /** آخرین پاسخ از کدام موتور بود */
    @Volatile var lastTitle: String = ""; private set
    @Volatile var lastWasInternal: Boolean = false; private set

    fun online(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)

    /** آیا اکنون هوش مصنوعی ابری قابل استفاده است؟ */
    fun available(allowInternal: Boolean = true): Boolean = routes(allowInternal).isNotEmpty() && online()

    /** عنوان موتوری که اول امتحان می‌شود */
    fun activeTitle(allowInternal: Boolean = true): String = routes(allowInternal).firstOrNull()?.title ?: "موتور آفلاین"

    private fun open(route: Route, timeoutMs: Int): HttpURLConnection = (URL(route.url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; connectTimeout = 15000; readTimeout = timeoutMs
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("User-Agent", "SmartPlant/${BuildConfig.VERSION_NAME} (Android)")
        if (route.key.isNotBlank()) setRequestProperty("Authorization", "Bearer ${route.key}")
    }

    private fun content(m: Msg): Any =
        if (m.images.isEmpty()) m.text
        else JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", m.text))
            m.images.forEach { b64 ->
                put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64").put("detail", "high")))
            }
        }

    private fun call(route: Route, msgs0: List<Msg>, json: Boolean, maxTokens: Int, temperature: Double): String {
        // سرویس رایگان دستور سیستمی جداگانه را خوب رعایت نمی‌کند؛ با پیام اول کاربر ادغام شود
        val msgs = if (route.id == "free") {
            val sys = msgs0.filter { it.role == "system" }.joinToString("\n") { it.text }
            val rest = msgs0.filter { it.role != "system" }.toMutableList()
            val i = rest.indexOfFirst { it.role == "user" }
            if (sys.isNotBlank() && i >= 0) rest[i] = rest[i].copy(text = sys + "\n\n" + rest[i].text)
            rest
        } else msgs0
        val body = JSONObject().put("model", route.model)
            .put("messages", JSONArray().also { a -> msgs.forEach { m -> a.put(JSONObject().put("role", m.role).put("content", content(m))) } })
        if (route.id.startsWith("direct")) body.put("max_tokens", maxOf(maxTokens, 2500))
        else if (route.id != "free") body.put("temperature", temperature).put("max_tokens", maxTokens)
        if (json && route.id != "free") body.put("response_format", JSONObject().put("type", "json_object"))
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val c = open(route, 150_000)
        c.outputStream.use { it.write(bytes) }
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
        if (code == 400 && json && route.id != "free") {
            body.remove("response_format")
            val c2 = open(route, 150_000)
            c2.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (c2.responseCode in 200..299) return parse(c2.inputStream.bufferedReader().readText())
        }
        if (code == 429) throw AiException("${route.title}: سهمیه یا ترافیک سرویس پر است")
        if (code !in 200..299) throw AiException("${route.title}: خطای $code")
        return parse(txt)
    }

    private fun parse(txt: String): String {
        val t = txt.trim()
        if (t.startsWith("{")) {
            val o = JSONObject(t)
            o.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.let { if (it.isNotBlank()) return it }
            if (o.has("error")) throw AiException("پاسخ نامعتبر سرویس")
        }
        if (t.isBlank()) throw AiException("پاسخ خالی")
        return t
    }

    /** جایگزین نهایی متنی (بدون تصویر) */
    private fun freeGet(msgs: List<Msg>, json: Boolean): String {
        val system = msgs.filter { it.role == "system" }.joinToString("\n") { it.text }
        val prompt = msgs.filter { it.role != "system" }.joinToString("\n\n") { it.text }
        val url = "https://text.pollinations.ai/" + URLEncoder.encode(prompt.take(3000), "UTF-8").replace("+", "%20") +
            "?model=openai&system=" + URLEncoder.encode(system.take(2500), "UTF-8").replace("+", "%20") + if (json) "&json=true" else ""
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 12000; readTimeout = 90_000; setRequestProperty("User-Agent", "SmartPlant/1.0") }
        if (c.responseCode !in 200..299) throw AiException("$FREE_TITLE: خطای ${c.responseCode}")
        return c.inputStream.bufferedReader().readText().trim().also { if (it.isBlank()) throw AiException("پاسخ خالی") }
    }

    /**
     * گفتگو با مدل (متن و تصویر)؛ زنجیره سرویس‌ها به ترتیب امتحان می‌شود.
     * @param allowInternal فقط برای تشخیص اصلی true است تا سهمیه روزانه هدر نرود.
     */
    fun chat(msgs: List<Msg>, json: Boolean = true, maxTokens: Int = 3500, temperature: Double = 0.3, allowInternal: Boolean = false): String {
        if (!online()) throw AiException("اینترنت در دسترس نیست")
        var last: Throwable? = null
        lastWasInternal = false
        val hasImages = msgs.any { it.images.isNotEmpty() }
        for (r in routes(allowInternal)) {
            try {
                val out = call(r, msgs, json, maxTokens, temperature)
                if (r.id == "internal") { s.consumeInternal(); lastWasInternal = true } else lastWorking = if (r.id == "direct2") "direct" else r.id
                lastTitle = r.title
                return out
            } catch (t: Throwable) { last = t }
            if (r.id == "free" && !hasImages) runCatching { return freeGet(msgs, json).also { lastWorking = "free"; lastTitle = FREE_TITLE } }.onFailure { last = it }
        }
        throw AiException(last?.message ?: "هیچ سرویس هوش مصنوعی در دسترس نیست")
    }

    /** آزمون همه مسیرها: (عنوان، نتیجه) */
    fun diagnose(): List<Pair<String, Boolean>> = routes(true).filter { it.id != "direct2" }.map { r ->
        r.title to runCatching { call(r, listOf(Msg("user", "فقط بنویس: آماده")), false, 16, 0.0).isNotBlank() }.getOrDefault(false)
    }
}

/** استخراج JSON از پاسخ مدل (حتی اگر داخل ``` یا همراه متن باشد) */
fun extractJson(s: String): JSONObject {
    val a = s.indexOf('{'); val b = s.lastIndexOf('}')
    if (a < 0 || b <= a) throw AiException("پاسخ هوش مصنوعی قابل خواندن نبود")
    return JSONObject(s.substring(a, b + 1))
}
