package ir.aminim.smartplant.data

import org.json.JSONArray
import org.json.JSONObject

private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).trim().takeIf { s -> s.isNotBlank() && s != "null" } }
private fun List<String>.json() = JSONArray().also { a -> forEach { a.put(it) } }
private fun JSONObject.s(k: String) = optString(k).trim().let { if (it == "null") "" else it }

/** گزینه جایگزین در شناسایی گیاه */
data class Alt(val name: String, val sci: String, val confidence: Int)

/** یک مشکل تشخیص‌داده‌شده */
data class Found(
    val name: String,
    val type: String,
    val probability: Int,
    val cause: String,
    val signs: List<String>,
    val immediate: List<String>,
    val organic: List<String>,
    val chemical: List<String>,
    val recovery: String,
    val problemId: String = "",
) {
    fun toJson(): JSONObject = JSONObject().put("name", name).put("type", type).put("probability", probability).put("cause", cause)
        .put("signs", signs.json()).put("immediate", immediate.json()).put("organic", organic.json()).put("chemical", chemical.json())
        .put("recovery", recovery).put("problem_id", problemId)

    companion object {
        fun from(o: JSONObject): Found {
            val t = o.optJSONObject("treatment")
            return Found(
                name = o.s("name"), type = o.s("type"), probability = o.optInt("probability", 0).coerceIn(0, 100), cause = o.s("cause"),
                signs = (o.optJSONArray("signs") ?: o.optJSONArray("symptoms")).strings(),
                immediate = (o.optJSONArray("immediate") ?: t?.optJSONArray("immediate")).strings(),
                organic = (o.optJSONArray("organic") ?: t?.optJSONArray("organic")).strings(),
                chemical = (o.optJSONArray("chemical") ?: t?.optJSONArray("chemical")).strings(),
                recovery = o.s("recovery").ifBlank { o.s("recovery_time") }, problemId = o.s("problem_id"),
            )
        }
    }
}

/** نتیجه کامل یک تشخیص */
data class Diagnosis(
    val id: String,
    val time: Long,
    val images: List<String>,
    val note: String,
    val engine: String,
    val offline: Boolean,
    val isPlant: Boolean,
    val plantFa: String,
    val plantSci: String,
    val family: String,
    val confidence: Int,
    val about: String,
    val alternatives: List<Alt>,
    /** healthy | mild | moderate | severe */
    val status: String,
    val healthScore: Int,
    val healthSummary: String,
    val problems: List<Found>,
    val care: List<Pair<String, String>>,
    val prevention: List<String>,
    val toxicity: String,
    val urgency: String,
    val questions: List<String>,
    val tips: List<String>,
    val plantId: String = "",
) {
    val statusTitle: String get() = when (status) {
        "healthy" -> "سالم"; "mild" -> "مشکل خفیف"; "moderate" -> "نیازمند رسیدگی"; "severe" -> "وضعیت جدی"; else -> "نامشخص"
    }
    val title: String get() = plantFa.ifBlank { if (isPlant) "گیاه ناشناخته" else "تصویر نامعتبر" }

    fun toJson(): JSONObject = JSONObject().put("id", id).put("time", time).put("images", images.json()).put("note", note)
        .put("engine", engine).put("offline", offline).put("is_plant", isPlant)
        .put("plant", JSONObject().put("name_fa", plantFa).put("name_sci", plantSci).put("family", family).put("confidence", confidence).put("about", about)
            .put("alternatives", JSONArray().also { a -> alternatives.forEach { a.put(JSONObject().put("name_fa", it.name).put("name_sci", it.sci).put("confidence", it.confidence)) } }))
        .put("health", JSONObject().put("status", status).put("score", healthScore).put("summary", healthSummary))
        .put("problems", JSONArray().also { a -> problems.forEach { a.put(it.toJson()) } })
        .put("care_list", JSONArray().also { a -> care.forEach { (k, v) -> a.put(JSONObject().put("k", k).put("v", v)) } })
        .put("prevention", prevention.json()).put("toxicity", toxicity).put("urgency", urgency)
        .put("questions", questions.json()).put("tips", tips.json()).put("plant_id", plantId)

    companion object {
        val CARE_LABELS = linkedMapOf(
            "light" to "نور", "water" to "آبیاری", "humidity" to "رطوبت", "temperature" to "دما", "soil" to "خاک",
            "fertilizer" to "کوددهی", "repot" to "تعویض گلدان", "pruning" to "هرس", "propagation" to "تکثیر",
        )

        /** ساخت از JSON (چه ذخیره‌شده، چه پاسخ خام هوش مصنوعی) */
        fun from(o: JSONObject, id: String = o.optString("id"), time: Long = o.optLong("time"), images: List<String> = o.optJSONArray("images").strings(),
                 note: String = o.optString("note"), engine: String = o.optString("engine"), offline: Boolean = o.optBoolean("offline")): Diagnosis {
            val p = o.optJSONObject("plant") ?: JSONObject()
            val h = o.optJSONObject("health") ?: JSONObject()
            val care = mutableListOf<Pair<String, String>>()
            o.optJSONArray("care_list")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { care += it.s("k") to it.s("v") } }
            o.optJSONObject("care")?.let { c -> CARE_LABELS.forEach { (k, label) -> c.s(k).takeIf { it.isNotBlank() }?.let { care += label to it } } }
            val alts = mutableListOf<Alt>()
            p.optJSONArray("alternatives")?.let { a ->
                for (i in 0 until a.length()) a.optJSONObject(i)?.let { alts += Alt(it.s("name_fa"), it.s("name_sci"), it.optInt("confidence", 0)) }
            }
            val probs = mutableListOf<Found>()
            o.optJSONArray("problems")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { probs += Found.from(it) } }
            val status = h.s("status").lowercase().let { if (it in listOf("healthy", "mild", "moderate", "severe")) it else if (probs.isEmpty()) "healthy" else "moderate" }
            return Diagnosis(
                id = id, time = time, images = images, note = note, engine = engine, offline = offline,
                isPlant = o.optBoolean("is_plant", true),
                plantFa = p.s("name_fa"), plantSci = p.s("name_sci"), family = p.s("family"),
                confidence = p.optInt("confidence", 0).coerceIn(0, 100), about = p.s("about").ifBlank { p.s("description") },
                alternatives = alts.filter { it.name.isNotBlank() },
                status = status, healthScore = h.optInt("score", if (status == "healthy") 90 else 60).coerceIn(0, 100), healthSummary = h.s("summary"),
                problems = probs.filter { it.name.isNotBlank() }.sortedByDescending { it.probability },
                care = care, prevention = o.optJSONArray("prevention").strings(), toxicity = o.s("toxicity"), urgency = o.s("urgency"),
                questions = o.optJSONArray("questions").strings(), tips = o.optJSONArray("tips").strings(), plantId = o.s("plant_id"),
            )
        }
    }
}

/** گیاه در «باغچه من» */
data class MyPlant(
    val id: String,
    val name: String,
    val species: String,
    val plantId: String,
    val location: String,
    val photo: String,
    val waterEvery: Int,
    val lastWater: Long,
    val fertEvery: Int,
    val lastFert: Long,
    val created: Long,
    val notes: String,
    val waterCount: Int = 0,
) {
    /** چند روز تا آبیاری بعدی (منفی یعنی عقب افتاده) */
    fun waterInDays(now: Long = System.currentTimeMillis()): Int =
        if (waterEvery <= 0) Int.MAX_VALUE else (waterEvery - ((now - lastWater) / 86_400_000L).toInt())
    fun fertInDays(now: Long = System.currentTimeMillis()): Int =
        if (fertEvery <= 0) Int.MAX_VALUE else (fertEvery - ((now - lastFert) / 86_400_000L).toInt())

    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("species", species).put("plant_id", plantId)
        .put("location", location).put("photo", photo).put("water_every", waterEvery).put("last_water", lastWater)
        .put("fert_every", fertEvery).put("last_fert", lastFert).put("created", created).put("notes", notes).put("water_count", waterCount)

    companion object {
        fun from(o: JSONObject) = MyPlant(
            o.optString("id"), o.optString("name"), o.optString("species"), o.optString("plant_id"), o.optString("location"),
            o.optString("photo"), o.optInt("water_every", 7), o.optLong("last_water"), o.optInt("fert_every", 0), o.optLong("last_fert"),
            o.optLong("created"), o.optString("notes"), o.optInt("water_count"),
        )
    }
}

/** پیام گفتگو با پزشک گیاه */
data class ChatMsg(val role: String, val text: String, val image: String? = null, val error: Boolean = false)

/** نمایه گیاهی که هوش مصنوعی برای گیاهان خارج از دانشنامه می‌سازد */
data class AiProfile(val title: String, val sci: String, val sections: List<Pair<String, String>>, val buy: List<String>, val toxicity: String)
