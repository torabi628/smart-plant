package ir.aminim.smartplant.engine

import ir.aminim.smartplant.knowledge.PlantDb
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.knowledge.Problem
import ir.aminim.smartplant.knowledge.ProblemDb
import ir.aminim.smartplant.knowledge.Tag

/** موتور عیب‌یابی آفلاین بر پایه علائم */
object SymptomEngine {
    data class Match(val problem: Problem, val score: Int, val matched: List<String>)

    fun rank(selected: Set<String>, plant: PlantInfo? = null): List<Match> {
        if (selected.isEmpty()) return emptyList()
        return ProblemDb.all.mapNotNull { p ->
            val hit = selected.filter { (p.weights[it] ?: 0) > 0 }
            if (hit.isEmpty()) return@mapNotNull null
            val raw = hit.sumOf { p.weights[it] ?: 0 }.toDouble()
            val total = p.weights.values.sum().toDouble()
            val fit = raw / (3.0 * selected.size)
            val coverage = raw / total
            var s = 100 * (0.55 * fit + 0.45 * coverage)
            // مشکلات رایج همان گیاه کمی وزن بیشتری می‌گیرند
            if (plant != null && p.id in plant.problems) s *= 1.15
            if (p.id == "aging" && selected.size > 1) s *= 0.4
            Match(p, s.toInt().coerceIn(1, 97), hit)
        }.sortedByDescending { it.score }.take(5)
    }

    /** استخراج علائم از متن آزاد کاربر (تایپ یا گفتار) */
    fun symptomsFromText(text: String): Set<String> {
        val t = " " + text.replace('ي', 'ی').replace('ك', 'ک').replace("‌", " ") + " "
        val out = mutableSetOf<String>()
        ProblemDb.symptoms.forEach { s -> if (s.keywords.any { k -> t.contains(k.replace("‌", " ")) }) out += s.id }
        // پالایش: «مگس سفید» نباید «مگس قارچ» حساب شود
        if ("whiteflies" in out && !t.contains("خاک")) out -= "gnats"
        if ("yellow_new" in out) out -= "yellow_old"
        if ("white_powder" in out) out -= "cotton"
        return out
    }
}

/** مشاور خرید: پیشنهاد گیاه مناسب بر اساس شرایط کاربر */
object Advisor {
    data class Answers(
        val light: Int = 2,
        /** وقت مراقبت ۱ کم، ۲ متوسط، ۳ زیاد */
        val time: Int = 2,
        val pets: Boolean = false,
        /** ۱ میز، ۲ متوسط، ۳ کف و بزرگ، ۰ مهم نیست */
        val size: Int = 0,
        val goals: Set<Tag> = emptySet(),
        /** تجربه ۱ مبتدی، ۲ متوسط، ۳ حرفه‌ای */
        val skill: Int = 1,
    )

    data class Pick(val plant: PlantInfo, val score: Int, val reasons: List<String>, val warnings: List<String>)

    fun recommend(a: Answers): List<Pick> = PlantDb.all.mapNotNull { p ->
        if (a.pets && !p.petSafe) return@mapNotNull null
        var s = 50.0
        val why = mutableListOf<String>()
        val warn = mutableListOf<String>()
        when {
            a.light in p.lightMin..p.lightMax -> { s += 30; why += "با نور محل شما (${ir.aminim.smartplant.knowledge.Light.short[a.light]}) سازگار است" }
            a.light == p.lightMin - 1 -> { s -= 10; warn += "کمی نور بیشتری لازم دارد" }
            a.light == p.lightMax + 1 -> { s -= 8; warn += "آفتاب مستقیم ممکن است برگ‌ها را بسوزاند؛ پرده توری بکشید" }
            else -> s -= 60
        }
        val effort = p.level + (if (p.humidity == 3) 1 else 0) + (if (p.waterSummer <= 4) 1 else 0)
        val capacity = a.time + a.skill
        if (effort <= capacity) { s += 10; if (p.level == 1) why += "نگهداری آسان" } else { s -= (effort - capacity) * 12; warn += "مراقبت بیشتری از وقت/تجربه شما می‌خواهد" }
        if (a.time == 1 && Tag.TRAVEL in p.tags) { s += 12; why += "بی‌آبی و سفر را تحمل می‌کند" }
        if (a.pets) why += "برای حیوانات خانگی بی‌خطر است"
        if (a.size != 0) { if (a.size == p.size) { s += 10; why += "اندازه مناسب فضای شما" } else s -= 6 * kotlin.math.abs(a.size - p.size) }
        a.goals.forEach { g -> if (g in p.allTags) { s += 12; why += g.title } }
        if (p.humidity == 3) warn += "رطوبت بالا می‌خواهد"
        if (!p.petSafe) warn += "سمی است؛ دور از دسترس کودک و حیوان"
        Pick(p, s.toInt().coerceIn(0, 100), why.distinct(), warn.distinct())
    }.filter { it.score >= 45 }.sortedByDescending { it.score }.take(10)

    /** پیشنهاد گیاه بر اساس سطح نور اندازه‌گیری‌شده */
    fun forLight(level: Int): List<PlantInfo> = PlantDb.all.filter { level in it.lightMin..it.lightMax }.sortedBy { it.level }
}
