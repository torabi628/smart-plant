package ir.aminim.smartplant.ai

import ir.aminim.smartplant.data.AiProfile
import ir.aminim.smartplant.data.Diagnosis
import ir.aminim.smartplant.data.Found
import ir.aminim.smartplant.engine.SymptomEngine
import ir.aminim.smartplant.knowledge.Light
import ir.aminim.smartplant.knowledge.PlantDb
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.knowledge.ProblemDb
import ir.aminim.smartplant.knowledge.ProblemType
import org.json.JSONObject

/** دستورها و تبدیل‌های هوش مصنوعی گیاه‌شناس */
object PlantAi {

    private const val ROLE = """تو «گیاه‌شناس هوشمند» هستی؛ متخصص ارشد گیاه‌شناسی، گیاه‌پزشکی (بیماری‌ها، آفات و تغذیه) و باغبانی گیاهان آپارتمانی، باغچه‌ای و باغی.
مخاطب تو کاربر ایرانی است؛ شرایط آپارتمان‌های ایران (هوای خشک، آب شهری سخت و کلردار، بخاری و کولر، تابستان گرم) را در نظر بگیر.
فارسی روان، دقیق و قابل فهم بنویس. هرگز حدس را به جای قطعیت جا نزن؛ میزان اطمینان را واقع‌بینانه بیان کن."""

    val DIAGNOSE_SYSTEM = """$ROLE

وظیفه: تصویر(ها) و توضیح کاربر را با دقت بسیار بررسی کن و «فقط» یک شیء JSON معتبر با این ساختار برگردان (کلیدها انگلیسی، مقادیر فارسی):
{
 "is_plant": true,
 "plant": {"name_fa": "نام رایج فارسی در ایران", "name_sci": "نام علمی (و رقم اگر مشخص است)", "family": "تیره", "confidence": 0-100, "about": "۲ تا ۳ جمله معرفی", "alternatives": [{"name_fa": "", "name_sci": "", "confidence": 0-100}]},
 "health": {"status": "healthy | mild | moderate | severe", "score": 0-100, "summary": "جمع‌بندی وضعیت سلامت در ۲ تا ۳ جمله"},
 "problems": [{"name": "نام مشکل", "type": "care | pest | disease | nutrient", "probability": 0-100, "cause": "علت و توضیح سازوکار", "signs": ["نشانه‌هایی که در تصویر/توضیح دیدی"], "immediate": ["اقدام فوری گام‌به‌گام"], "organic": ["درمان خانگی/ارگانیک با مقدار و غلظت دقیق"], "chemical": ["سم یا داروی شیمیایی با نام ماده مؤثره و نام تجاری رایج در ایران، دوز طبق برچسب و نکته ایمنی"], "recovery": "زمان تقریبی بهبود"}],
 "care": {"light": "", "water": "", "humidity": "", "temperature": "", "soil": "", "fertilizer": "", "repot": "", "pruning": "", "propagation": ""},
 "prevention": ["پیشگیری"],
 "toxicity": "سمی بودن برای انسان، کودک، گربه و سگ",
 "urgency": "چقدر فوری است و اگر درمان نشود چه می‌شود",
 "questions": ["حداکثر ۳ پرسش برای قطعی کردن تشخیص، اگر لازم است"],
 "tips": ["نکته‌های حرفه‌ای اختصاصی همین گیاه"]
}

قواعد مهم:
- اگر در تصویر گیاهی نیست یا تصویر قابل تشخیص نیست: is_plant=false و دلیل را در health.summary بنویس و بخواه عکس بهتری بگیرد.
- گیاه را تا حد گونه (و اگر ممکن است رقم) شناسایی کن. اگر مطمئن نیستی confidence را پایین بگذار و ۱ تا ۳ گزینه جایگزین بده.
- همه نشانه‌ها را بررسی کن: رنگ و لکه‌های برگ، لبه و نوک برگ، پشت برگ، ساقه، آفات قابل مشاهده، خاک، گلدان، زهکش و محیط.
- مشکلات را به ترتیب احتمال مرتب کن؛ مشکلات مراقبتی (آبیاری، نور، رطوبت، دما) را هم در نظر بگیر که شایع‌ترین علت‌اند.
- اگر گیاه سالم است، problems خالی و status=healthy باشد و فقط نکات مراقبت بده.
- درمان‌ها عملی، دقیق و مرحله‌به‌مرحله باشند (مقدار، غلظت، تعداد و فاصله نوبت‌ها). اول راهکار ایمن/ارگانیک، سپس شیمیایی فقط اگر لازم است؛ برای مشکلات مراقبتی chemical خالی باشد.
- مراقبت (care) مخصوص همین گونه و شرایط آپارتمان ایرانی باشد.
- فقط JSON؛ بدون متن اضافه."""

    fun diagnoseUser(note: String, imageCount: Int, plantHint: String): String = buildString {
        if (imageCount > 0) append("$imageCount تصویر از گیاه پیوست شده است (برگ نزدیک، کل گیاه و/یا خاک). ")
        else append("تصویری در دسترس نیست؛ فقط بر اساس توضیح کاربر تشخیص بده و confidence را متناسب تنظیم کن. ")
        if (plantHint.isNotBlank()) append("\nکاربر می‌گوید نام گیاه احتمالاً «$plantHint» است. ")
        append("\nتوضیح کاربر: ")
        append(note.ifBlank { "توضیحی نداده است؛ از روی تصویر کامل بررسی کن." })
    }

    val CHAT_SYSTEM = """$ROLE

تو «پزشک گیاه» در یک گفتگو هستی. به پرسش‌ها کوتاه، دقیق و کاربردی پاسخ بده.
- از تیترهای کوتاه با ** و فهرست‌های گلوله‌ای (- ) استفاده کن.
- اگر عکس فرستاده شد، اول بگو چه می‌بینی، بعد تشخیص و راهکار.
- اگر اطلاعات کافی نیست، یک یا دو پرسش دقیق بپرس.
- درباره سموم شیمیایی همیشه نکته ایمنی و دوز طبق برچسب را بگو.
- خارج از موضوع گیاه، باغبانی و کشاورزی خانگی پاسخ نده و مؤدبانه به موضوع برگرد."""

    fun chatContext(d: Diagnosis?): String {
        if (d == null) return ""
        return buildString {
            append("زمینه گفتگو: کاربر پیش‌تر تشخیص زیر را دریافت کرده است.\n")
            append("گیاه: ${d.plantFa} (${d.plantSci})\nوضعیت: ${d.statusTitle} — ${d.healthSummary}\n")
            if (d.problems.isNotEmpty()) append("مشکلات: " + d.problems.joinToString("، ") { "${it.name} (${it.probability}٪)" } + "\n")
            if (d.note.isNotBlank()) append("توضیح اولیه کاربر: ${d.note}\n")
        }
    }

    val PROFILE_SYSTEM = """$ROLE

برای گیاهی که کاربر نام می‌برد یک شناسنامه نگهداری کامل و دقیق بنویس و فقط این JSON را برگردان:
{"title": "نام رایج فارسی", "sci": "نام علمی", "sections": [{"title": "معرفی", "text": ""}, {"title": "نور", "text": ""}, {"title": "آبیاری", "text": ""}, {"title": "رطوبت و دما", "text": ""}, {"title": "خاک و گلدان", "text": ""}, {"title": "کوددهی", "text": ""}, {"title": "تکثیر", "text": ""}, {"title": "هرس و نکات گل‌دهی", "text": ""}, {"title": "مشکلات رایج و درمان", "text": ""}, {"title": "مناسب چه کسی و چه جایی است", "text": ""}], "buy": ["هنگام خرید به چه دقت کنیم"], "toxicity": "سمی بودن برای انسان و حیوانات"}
اگر نام گیاه نامعتبر یا ناشناخته است، title را "نامشخص" بگذار و در sections توضیح بده."""

    fun parseProfile(raw: String): AiProfile {
        val o = extractJson(raw)
        val secs = mutableListOf<Pair<String, String>>()
        o.optJSONArray("sections")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { s -> val t = s.optString("text").trim(); if (t.isNotBlank()) secs += s.optString("title") to t } }
        val buy = o.optJSONArray("buy")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } } ?: emptyList()
        return AiProfile(o.optString("title"), o.optString("sci"), secs, buy, o.optString("toxicity"))
    }

    fun advisorPrompt(summary: String): String = """کاربر می‌خواهد گیاه آپارتمانی بخرد. شرایط او:
$summary
۵ گیاه مناسب که در گلفروشی‌های ایران پیدا می‌شوند پیشنهاد بده. برای هر کدام: چرا مناسب است، یک نکته مراقبت کلیدی و یک نکته هنگام خرید. در پایان یک جمع‌بندی یک‌خطی. پاسخ کوتاه و فهرست‌وار باشد."""

    /* ───────────── تشخیص آفلاین ───────────── */

    /** ساخت تشخیص از موتور علائم (بدون اینترنت) */
    fun offline(id: String, time: Long, images: List<String>, note: String, symptoms: Set<String>, plant: PlantInfo?): Diagnosis {
        val matches = SymptomEngine.rank(symptoms, plant)
        val found = matches.map { m ->
            val p = m.problem
            Found(
                name = p.name, type = when (p.type) { ProblemType.PEST -> "pest"; ProblemType.DISEASE -> "disease"; ProblemType.NUTRIENT -> "nutrient"; else -> "care" },
                probability = m.score, cause = p.summary + (if (p.causes.isNotEmpty()) "\nعلت‌های رایج: " + p.causes.joinToString("، ") else ""),
                signs = m.matched.mapNotNull { ProblemDb.symptom(it)?.title }, immediate = p.immediate, organic = p.organic, chemical = p.chemical,
                recovery = p.recovery, problemId = p.id,
            )
        }
        val top = matches.firstOrNull()
        val sev = top?.problem?.severity ?: 0
        val status = when { matches.isEmpty() -> "healthy"; sev >= 3 && top!!.score > 40 -> "severe"; sev == 2 -> "moderate"; else -> "mild" }
        val care = mutableListOf<Pair<String, String>>()
        plant?.let {
            care += "نور" to Light.range(it.lightMin, it.lightMax)
            care += "آبیاری" to "${it.water}\n(تقریباً هر ${it.waterSummer} روز در فصل گرم و هر ${it.waterWinter} روز در فصل سرد)"
            care += "رطوبت" to it.humidityTitle
            care += "دما" to "${it.tempMin} تا ${it.tempMax} درجه"
            care += "خاک" to it.soil
            care += "کوددهی" to it.fert
            care += "تکثیر" to it.propagate
        }
        return Diagnosis(
            id = id, time = time, images = images, note = note, engine = "موتور آفلاین علائم", offline = true, isPlant = true,
            plantFa = plant?.fa ?: "", plantSci = plant?.sci ?: "", family = plant?.family ?: "",
            confidence = if (plant != null) 70 else 0, about = plant?.intro ?: "",
            alternatives = emptyList(), status = status,
            healthScore = when (status) { "healthy" -> 85; "mild" -> 70; "moderate" -> 50; else -> 30 },
            healthSummary = if (matches.isEmpty()) "علامت شناخته‌شده‌ای در توضیح شما پیدا نشد. از بخش «عیب‌یابی با علائم» علائم را دقیق انتخاب کنید یا با اتصال اینترنت عکس را تحلیل کنید."
            else "بر اساس ${symptoms.size} علامت، محتمل‌ترین مشکل «${top!!.problem.name}» است. این تشخیص آفلاین و تقریبی است؛ برای دقت بیشتر با اینترنت عکس را تحلیل کنید.",
            problems = found,
            care = care,
            prevention = matches.flatMap { it.problem.prevent }.distinct().take(6),
            toxicity = plant?.toxicity ?: "",
            urgency = when (status) { "severe" -> "فوری: هرچه زودتر اقدام کنید؛ تأخیر ممکن است به از دست رفتن گیاه منجر شود."; "moderate" -> "طی چند روز آینده رسیدگی کنید."; else -> "فوریتی ندارد." },
            questions = emptyList(), tips = plant?.tips ?: emptyList(), plantId = plant?.id ?: "",
        )
    }

    /** تلاش برای یافتن گیاه دانشنامه بر اساس نام شناسایی‌شده */
    fun matchPlant(fa: String, sci: String): PlantInfo? {
        val n = PlantDb.norm(fa)
        val genus = sci.split(' ').firstOrNull()?.lowercase().orEmpty()
        return PlantDb.all.firstOrNull { PlantDb.norm(it.fa) == n }
            ?: PlantDb.all.firstOrNull { p -> n.length >= 3 && (PlantDb.norm(p.fa).contains(n) || n.contains(PlantDb.norm(p.fa)) || p.aka.split('،').any { a -> PlantDb.norm(a).let { it.length >= 3 && (it == n) } }) }
            ?: PlantDb.all.firstOrNull { genus.length > 3 && it.sci.lowercase().startsWith(genus) && sci.lowercase().split(' ').getOrNull(1)?.let { sp -> it.sci.lowercase().contains(sp) } == true }
    }

    fun parseDiagnosis(raw: String, id: String, time: Long, images: List<String>, note: String, engine: String): Diagnosis {
        val o: JSONObject = extractJson(raw)
        val d = Diagnosis.from(o, id = id, time = time, images = images, note = note, engine = engine, offline = false)
        val local = matchPlant(d.plantFa, d.plantSci)
        return if (local != null) d.copy(plantId = local.id) else d
    }
}
