package ir.aminim.smartplant.speech

import android.content.Context
import ir.aminim.smartplant.ai.Downloader
import ir.aminim.smartplant.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File

/** مدل‌های گفتار آفلاین فارسی (Vosk) برای زمان قطعی اینترنت؛ همان سه مدل «صورتجلسه هوشمند» */
object VoskModels {
    data class AsrModel(val id: String, val title: String, val desc: String, val url: String, val sizeMb: Int)

    val catalog = listOf(
        AsrModel("small", "فارسی سبک", "سریع و کم‌حجم؛ مناسب همه گوشی‌ها", "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.42.zip", 53),
        AsrModel("fa05", "فارسی دقیق", "دقت بالاتر روی گفتار روزمره؛ گوشی با رم ۴ گیگ به بالا", "https://alphacephei.com/vosk/models/vosk-model-fa-0.5.zip", 1000),
        AsrModel("fa042", "فارسی حرفه‌ای", "بالاترین دقت؛ گوشی قوی (رم ۶ گیگ به بالا) و فضای کافی", "https://alphacephei.com/vosk/models/vosk-model-fa-0.42.zip", 1600),
    )

    private fun dir(c: Context, id: String) = File(c.filesDir, if (id == "small") "vosk/asr" else "vosk/asr_$id")
    private fun ok(f: File) = File(f, "am").exists() || File(f, "conf").exists()

    fun dirOf(c: Context, id: String): File? {
        val root = dir(c, id)
        if (!root.exists()) return null
        if (ok(root)) return root
        return root.listFiles()?.firstOrNull { it.isDirectory && ok(it) }
    }

    fun installed(c: Context, id: String) = dirOf(c, id) != null
    fun activeId(c: Context): String = AppSettings(c).asrModelId.takeIf { installed(c, it) } ?: catalog.firstOrNull { installed(c, it.id) }?.id ?: "small"
    fun asrDir(c: Context): File? = dirOf(c, activeId(c))
    fun ready(c: Context) = asrDir(c) != null

    private var asr: Model? = null
    private var path: String? = null

    @Synchronized
    fun load(c: Context): Model {
        val p = asrDir(c)!!.absolutePath
        if (asr == null || path != p) { runCatching { asr?.close() }; asr = Model(p); path = p }
        return asr!!
    }

    @Synchronized
    fun setActive(c: Context, id: String) { AppSettings(c).asrModelId = id; runCatching { asr?.close() }; asr = null; path = null }

    suspend fun install(c: Context, m: AsrModel, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val zip = File(c.cacheDir, "vosk_${m.id}.zip.part")
        Downloader.fetch(m.url, zip, { false }) { p, _, _ -> progress(p * 0.93f) }
        Downloader.unzip(zip, dir(c, m.id))
        zip.delete()
        if (!installed(c, m.id)) throw IllegalStateException("فایل مدل نامعتبر است")
        setActive(c, m.id)
        progress(1f)
    }

    fun delete(c: Context, id: String) {
        if (activeId(c) == id) setActive(c, "")
        dir(c, id).deleteRecursively()
    }
}
