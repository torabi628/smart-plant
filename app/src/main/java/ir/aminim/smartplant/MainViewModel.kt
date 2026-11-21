package ir.aminim.smartplant

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.aminim.smartplant.ai.AiClient
import ir.aminim.smartplant.ai.Msg
import ir.aminim.smartplant.ai.PlantAi
import ir.aminim.smartplant.data.AiProfile
import ir.aminim.smartplant.data.ChatMsg
import ir.aminim.smartplant.data.Diagnosis
import ir.aminim.smartplant.data.Images
import ir.aminim.smartplant.data.MyPlant
import ir.aminim.smartplant.data.Storage
import ir.aminim.smartplant.engine.Advisor
import ir.aminim.smartplant.engine.SymptomEngine
import ir.aminim.smartplant.knowledge.Light
import ir.aminim.smartplant.knowledge.PlantDb
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.reminders.Reminders
import ir.aminim.smartplant.settings.AppSettings
import ir.aminim.smartplant.settings.SpeechEngine
import ir.aminim.smartplant.speech.Dictation
import ir.aminim.smartplant.speech.GoogleDictation
import ir.aminim.smartplant.speech.VoskDictation
import ir.aminim.smartplant.speech.VoskModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    val settings = AppSettings(app)
    private val storage = Storage(app)
    val ai = AiClient(app)

    var toast by mutableStateOf<String?>(null)

    val diagnoses = mutableStateListOf<Diagnosis>()
    val garden = mutableStateListOf<MyPlant>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val d = storage.loadDiagnoses().sortedByDescending { it.time }
            val g = storage.loadGarden()
            withContext(Dispatchers.Main) { diagnoses.addAll(d); garden.addAll(g) }
        }
        Reminders.schedule(app)
    }

    /* ═══════════════  گرفتن تصویر و توضیح  ═══════════════ */
    val shots = mutableStateListOf<String>()
    var note by mutableStateOf("")
    var plantHint by mutableStateOf("")
    var importing by mutableStateOf(false)

    fun newCapture() { shots.clear(); note = ""; plantHint = "" }

    fun addImage(uri: Uri) {
        if (shots.size >= MAX_SHOTS) { toast = "حداکثر $MAX_SHOTS عکس"; return }
        importing = true
        viewModelScope.launch {
            val p = withContext(Dispatchers.IO) { Images.import(ctx, uri) }
            importing = false
            if (p != null) shots += p else toast = "خواندن عکس ممکن نشد"
        }
    }

    fun removeImage(p: String) { shots.remove(p) }

    /* ═══════════════  تشخیص  ═══════════════ */
    var busy by mutableStateOf(false)
    var busyStage by mutableStateOf("")
    var busyError by mutableStateOf<String?>(null)
    var current by mutableStateOf<Diagnosis?>(null)

    val aiEngineTitle: String get() = ai.activeTitle(true)

    fun diagnose() {
        if (shots.isEmpty() && note.isBlank()) { toast = "یک عکس بگیرید یا مشکل گیاه را توضیح دهید"; return }
        busy = true; busyError = null; current = null
        val images = shots.toList(); val text = note.trim(); val hint = plantHint.trim()
        val id = System.currentTimeMillis().toString(); val time = System.currentTimeMillis()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (ai.available(true)) {
                    runCatching {
                        withContext(Dispatchers.Main) { busyStage = "در حال آماده‌سازی تصاویر…" }
                        val b64 = images.mapNotNull { Images.base64(it) }
                        withContext(Dispatchers.Main) { busyStage = "تحلیل با ${ai.activeTitle(true)}…" }
                        val raw = ai.chat(
                            listOf(Msg("system", PlantAi.DIAGNOSE_SYSTEM), Msg("user", PlantAi.diagnoseUser(text, b64.size, hint), b64)),
                            json = true, maxTokens = 3500, temperature = 0.2, allowInternal = true,
                        )
                        PlantAi.parseDiagnosis(raw, id, time, images, text, ai.lastTitle)
                    }.getOrElse { e -> offlineOrNull(id, time, images, text, hint) ?: return@withContext Result.failure<Diagnosis>(e) }.let { Result.success(it) }
                } else {
                    offlineOrNull(id, time, images, text, hint)?.let { Result.success(it) }
                        ?: Result.failure(IllegalStateException(
                            if (!ai.online()) "اینترنت در دسترس نیست. تحلیل عکس به اینترنت نیاز دارد؛ علائم را با کلمات توضیح دهید یا از «عیب‌یابی با علائم» استفاده کنید."
                            else "موتور هوش مصنوعی آفلاین انتخاب شده است و عکس را نمی‌تواند تحلیل کند؛ علائم را توضیح دهید یا از «عیب‌یابی با علائم» استفاده کنید."))
                }
            }
            busy = false
            result.onSuccess { d -> current = d; saveDiagnosis(d) }
                .onFailure { busyError = it.message ?: "خطای ناشناخته" }
        }
    }

    private fun offlineOrNull(id: String, time: Long, images: List<String>, text: String, hint: String): Diagnosis? {
        val sy = SymptomEngine.symptomsFromText("$text $hint")
        if (sy.isEmpty()) return null
        val plant = PlantDb.findIn("$hint $text")
        return PlantAi.offline(id, time, images, text, sy, plant)
    }

    /** عیب‌یابی آفلاین با انتخاب علائم */
    val symptomSel = mutableStateListOf<String>()
    var symptomPlant by mutableStateOf<PlantInfo?>(null)

    fun runSymptoms() {
        if (symptomSel.isEmpty()) { toast = "دست‌کم یک علامت انتخاب کنید"; return }
        val now = System.currentTimeMillis()
        val d = PlantAi.offline(now.toString(), now, emptyList(), "علائم انتخاب‌شده", symptomSel.toSet(), symptomPlant)
        busyError = null; busy = false
        current = d; saveDiagnosis(d)
    }

    private fun saveDiagnosis(d: Diagnosis) {
        diagnoses.removeAll { it.id == d.id }
        diagnoses.add(0, d)
        val snapshot = diagnoses.toList()
        viewModelScope.launch(Dispatchers.IO) { storage.saveDiagnoses(snapshot) }
    }

    fun open(d: Diagnosis) { current = d; busy = false; busyError = null }

    fun deleteDiagnosis(d: Diagnosis) {
        diagnoses.removeAll { it.id == d.id }
        val snapshot = diagnoses.toList()
        val stillUsed = garden.map { it.photo }.toSet()
        viewModelScope.launch(Dispatchers.IO) {
            storage.saveDiagnoses(snapshot)
            d.images.filter { it !in stillUsed }.forEach { runCatching { File(it).delete() } }
        }
    }

    /** پاسخ به پرسش‌های تکمیلی: تشخیص دوباره با اطلاعات بیشتر */
    fun refine(d: Diagnosis, answer: String) {
        shots.clear(); shots.addAll(d.images)
        note = (d.note + "\n" + answer).trim(); plantHint = d.plantFa
        diagnose()
    }

    /* ═══════════════  گفتگو با پزشک گیاه  ═══════════════ */
    val chat = mutableStateListOf<ChatMsg>()
    var chatBusy by mutableStateOf(false)
    var chatAbout by mutableStateOf<Diagnosis?>(null)
    var chatImage by mutableStateOf<String?>(null)
    var chatDraft by mutableStateOf("")

    fun startChat(about: Diagnosis?) {
        if (chatAbout?.id != about?.id || about == null) chat.clear()
        chatAbout = about; chatImage = null
    }

    fun attachChatImage(uri: Uri) {
        viewModelScope.launch { withContext(Dispatchers.IO) { Images.import(ctx, uri) }?.let { chatImage = it } ?: run { toast = "خواندن عکس ممکن نشد" } }
    }

    fun send(text: String) {
        val t = text.trim()
        val img = chatImage
        if (t.isBlank() && img == null) return
        chat += ChatMsg("user", t.ifBlank { "این عکس را بررسی کن." }, img)
        chatImage = null; chatBusy = true
        val history = chat.toList()
        val about = chatAbout
        viewModelScope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    val msgs = mutableListOf(Msg("system", PlantAi.CHAT_SYSTEM + "\n\n" + PlantAi.chatContext(about)))
                    // فقط تصویر پیام آخر ارسال می‌شود تا حجم کم بماند
                    history.takeLast(12).forEachIndexed { i, m ->
                        val imgs = if (i == history.takeLast(12).lastIndex && m.image != null) listOfNotNull(Images.base64(m.image)) else emptyList()
                        if (!m.error) msgs += Msg(if (m.role == "user") "user" else "assistant", m.text, imgs)
                    }
                    ai.chat(msgs, json = false, maxTokens = 1800, temperature = 0.4)
                }
            }
            chatBusy = false
            out.onSuccess { chat += ChatMsg("assistant", it.trim()) }
                .onFailure { chat += ChatMsg("assistant", "پاسخ دریافت نشد: ${it.message}\nاتصال اینترنت یا موتور هوش مصنوعی را در تنظیمات بررسی کنید.", error = true) }
        }
    }

    /* ═══════════════  دانشنامه  ═══════════════ */
    var plant by mutableStateOf<PlantInfo?>(null)
    var problemId by mutableStateOf("")
    var guideId by mutableStateOf("")
    var profile by mutableStateOf<AiProfile?>(null)
    var profileBusy by mutableStateOf(false)

    fun askProfile(name: String) {
        if (name.isBlank()) return
        profile = null; profileBusy = true
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { PlantAi.parseProfile(ai.chat(listOf(Msg("system", PlantAi.PROFILE_SYSTEM), Msg("user", "گیاه: $name")), json = true, maxTokens = 3000)) }
            }
            profileBusy = false
            r.onSuccess { profile = it }.onFailure { toast = "دریافت اطلاعات ممکن نشد: ${it.message}" }
        }
    }

    /* ═══════════════  مشاور خرید  ═══════════════ */
    var advisor by mutableStateOf(Advisor.Answers())
    var advisorAi by mutableStateOf<String?>(null)
    var advisorBusy by mutableStateOf(false)

    fun askAdvisorAi() {
        val a = advisor
        val summary = buildString {
            append("- نور محل نگهداری: ${Light.names[a.light]}\n")
            append("- وقت مراقبت: ${listOf("", "کم (سفر زیاد/مشغله)", "متوسط", "زیاد")[a.time]}\n")
            append("- تجربه: ${listOf("", "مبتدی", "متوسط", "حرفه‌ای")[a.skill]}\n")
            append("- حیوان خانگی یا کودک خردسال: ${if (a.pets) "دارد (فقط گیاهان غیرسمی)" else "ندارد"}\n")
            if (a.size > 0) append("- اندازه دلخواه: ${listOf("", "کوچک و رومیزی", "متوسط", "بزرگ و ایستاده")[a.size]}\n")
            if (a.goals.isNotEmpty()) append("- هدف: ${a.goals.joinToString("، ") { it.title }}\n")
            if (settings.city.isNotBlank()) append("- شهر: ${settings.city}\n")
        }
        advisorBusy = true; advisorAi = null
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { ai.chat(listOf(Msg("system", PlantAi.CHAT_SYSTEM), Msg("user", PlantAi.advisorPrompt(summary))), json = false, maxTokens = 1800, temperature = 0.5) }
            }
            advisorBusy = false
            r.onSuccess { advisorAi = it.trim() }.onFailure { toast = "مشاوره هوشمند در دسترس نیست: ${it.message}" }
        }
    }

    /* ═══════════════  باغچه من  ═══════════════ */
    var editing by mutableStateOf<MyPlant?>(null)

    private fun persistGarden() {
        val snapshot = garden.toList()
        viewModelScope.launch(Dispatchers.IO) { storage.saveGarden(snapshot) }
    }

    fun newGardenPlant(from: PlantInfo? = null, d: Diagnosis? = null): MyPlant {
        val now = System.currentTimeMillis()
        val info = from ?: d?.plantId?.let { PlantDb.byId(it) }
        val month = ir.aminim.smartplant.util.Jalali.of(now).month
        val warm = month in 1..6
        return MyPlant(
            id = now.toString(), name = d?.plantFa?.takeIf { it.isNotBlank() } ?: info?.fa ?: "", species = info?.fa ?: d?.plantFa.orEmpty(),
            plantId = info?.id.orEmpty(), location = "", photo = d?.images?.firstOrNull().orEmpty(),
            waterEvery = info?.let { if (warm) it.waterSummer else it.waterWinter } ?: 7, lastWater = now,
            fertEvery = if (info != null && warm) 21 else 0, lastFert = now, created = now, notes = "",
        )
    }

    fun upsert(p: MyPlant) {
        val i = garden.indexOfFirst { it.id == p.id }
        if (i >= 0) garden[i] = p else garden.add(0, p)
        persistGarden()
    }

    fun deletePlant(p: MyPlant) { garden.removeAll { it.id == p.id }; persistGarden() }

    fun water(p: MyPlant) { upsert(p.copy(lastWater = System.currentTimeMillis(), waterCount = p.waterCount + 1)); toast = "آبیاری «${p.name}» ثبت شد 💧" }
    fun fertilize(p: MyPlant) { upsert(p.copy(lastFert = System.currentTimeMillis())); toast = "کوددهی «${p.name}» ثبت شد" }

    fun setGardenPhoto(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch { withContext(Dispatchers.IO) { Images.import(ctx, uri) }?.let(onDone) }
    }

    val dueCount: Int get() = garden.count { it.waterInDays() <= 0 || it.fertInDays() <= 0 }

    /* ═══════════════  گفتار  ═══════════════ */
    fun newDictation(): Dictation =
        if (settings.speechEngine == SpeechEngine.OFFLINE && VoskModels.ready(ctx)) VoskDictation(ctx) else GoogleDictation(ctx)

    val asrProgress = mutableStateMapOf<String, Float>()
    fun installAsr(m: VoskModels.AsrModel) {
        if (asrProgress.containsKey(m.id)) return
        asrProgress[m.id] = 0f
        viewModelScope.launch {
            val r = runCatching { VoskModels.install(ctx, m) { p -> viewModelScope.launch(Dispatchers.Main) { asrProgress[m.id] = p } } }
            asrProgress.remove(m.id)
            r.onSuccess { settings.speechEngine = SpeechEngine.OFFLINE; toast = "مدل گفتار «${m.title}» نصب شد" }
                .onFailure { toast = "دانلود ناموفق: ${it.message}" }
        }
    }

    /* ═══════════════  آزمون اتصال  ═══════════════ */
    var testing by mutableStateOf(false)
    var testResult by mutableStateOf<List<Pair<String, Boolean>>>(emptyList())
    fun testAi() {
        testing = true; testResult = emptyList()
        viewModelScope.launch {
            testResult = withContext(Dispatchers.IO) { runCatching { ai.diagnose() }.getOrDefault(emptyList()) }
            testing = false
            if (testResult.isEmpty()) toast = "هیچ مسیر ابری فعال نیست یا اینترنت قطع است"
        }
    }

    fun clearHistory() {
        val used = garden.map { it.photo }.toSet()
        val all = diagnoses.toList()
        diagnoses.clear()
        viewModelScope.launch(Dispatchers.IO) {
            storage.saveDiagnoses(emptyList())
            all.flatMap { it.images }.filter { it !in used }.forEach { runCatching { File(it).delete() } }
        }
    }

    /* ═══════════════  داده نمایشی برای تصاویر فروشگاه  ═══════════════ */
    fun seedDemo() {
        if (diagnoses.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val pothos = PlantDb.byId("pothos")!!
        val d1 = PlantAi.offline((now - 3_600_000).toString(), now - 3_600_000, emptyList(), "برگ‌های پایینی پتوسم زرد شده و خاک همیشه خیسه، چند تا مگس ریز هم دور گلدونه",
            setOf("yellow_old", "soil_wet", "gnats", "droop"), pothos).copy(engine = "هوش مصنوعی قوی داخلی", offline = false, confidence = 94,
            alternatives = listOf(ir.aminim.smartplant.data.Alt("فیلودندرون قلبی", "Philodendron hederaceum", 4)))
        val d2 = PlantAi.offline((now - 86_400_000).toString(), now - 86_400_000, emptyList(), "پشت برگ‌ها تار ریز دیدم و برگ‌ها خال‌خالی شده",
            setOf("web", "stipple", "heater"), PlantDb.byId("areca")).copy(engine = "هوش مصنوعی قوی خارجی", offline = false, confidence = 88)
        diagnoses.addAll(listOf(d1, d2))
        garden.addAll(listOf(
            newGardenPlant(pothos).copy(id = "g1", name = "پتوس پذیرایی", location = "کنار پنجره شرقی", lastWater = now - 8 * 86_400_000L, waterCount = 14),
            newGardenPlant(PlantDb.byId("snake")).copy(id = "g2", name = "سانسوریا اتاق خواب", location = "اتاق خواب", lastWater = now - 5 * 86_400_000L, waterCount = 6),
            newGardenPlant(PlantDb.byId("orchid")).copy(id = "g3", name = "ارکیده سفید", location = "میز ناهارخوری", lastWater = now - 2 * 86_400_000L, waterCount = 9),
        ))
        current = d1
    }

    companion object { const val MAX_SHOTS = 4 }
}
