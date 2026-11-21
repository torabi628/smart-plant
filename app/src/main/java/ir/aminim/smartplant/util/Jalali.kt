package ir.aminim.smartplant.util

import java.util.Calendar

/** تبدیل تاریخ میلادی به شمسی و ابزارهای اعداد فارسی */
object Jalali {
    private val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    private val weekDays = arrayOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")

    data class JDate(val year: Int, val month: Int, val day: Int)

    fun fromGregorian(gy: Int, gm: Int, gd: Int): JDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return JDate(jy, jm, jd)
    }

    /** تبدیل شمسی به میلادی (سال، ماه، روز) */
    fun toGregorian(jy0: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        var jy = jy0 + 1595
        var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + jd + if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val sal = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > sal[gm]) { gd -= sal[gm]; gm++ }
        return Triple(gy, gm, gd)
    }

    /** میلی‌ثانیه پایان روز (۲۳:۵۹) برای تاریخ شمسی */
    fun millisOf(jy: Int, jm: Int, jd: Int, endOfDay: Boolean = true): Long {
        val (gy, gm, gd) = toGregorian(jy, jm, jd)
        return Calendar.getInstance().apply {
            clear(); set(gy, gm - 1, gd, if (endOfDay) 23 else 0, if (endOfDay) 59 else 0, 0)
        }.timeInMillis
    }

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun isLeap(jy: Int): Boolean {
        // آزمون کبیسه با تبدیل رفت‌وبرگشت
        val (gy, gm, gd) = toGregorian(jy, 12, 30)
        val back = fromGregorian(gy, gm, gd)
        return back.month == 12 && back.day == 30
    }

    fun monthName(m: Int) = monthNames[(m - 1).coerceIn(0, 11)]

    /** «آبان ۱۴۰۵» */
    fun monthYear(millis: Long): String { val j = of(millis); return "${monthNames[j.month - 1]} ${j.year}".fa() }

    /** «۲۲ آبان» */
    fun dayMonth(millis: Long): String { val j = of(millis); return "${j.day} ${monthNames[j.month - 1]}".fa() }

    fun of(millis: Long): JDate {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** ۱۴۰۵/۰۸/۲۲ */
    fun numeric(millis: Long): String {
        val j = of(millis)
        return "%04d/%02d/%02d".format(j.year, j.month, j.day).fa()
    }

    /** پنجشنبه ۲۲ آبان ۱۴۰۵ */
    fun long(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val j = of(millis)
        return "${weekDays[c.get(Calendar.DAY_OF_WEEK) - 1]} ${j.day} ${monthNames[j.month - 1]} ${j.year}".fa()
    }

    fun weekDay(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return weekDays[c.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun time(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)).fa()
    }
}

private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

/** تبدیل ارقام لاتین و عربی به ارقام فارسی */
fun String.fa(): String = buildString(length) {
    for (ch in this@fa) append(
        when (ch) {
            in '0'..'9' -> FA_DIGITS[ch - '0']
            in '٠'..'٩' -> FA_DIGITS[ch - '٠']
            else -> ch
        }
    )
}

fun Int.fa(): String = toString().fa()

/** ۹۹٬۰۰۰ */
fun Long.faGrouped(): String = "%,d".format(this).replace(',', '٬').fa()

/** مدت زمان به صورت ۰۲:۱۵ */
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return (if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)).fa()
}
