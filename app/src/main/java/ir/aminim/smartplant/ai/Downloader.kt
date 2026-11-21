package ir.aminim.smartplant.ai

import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** دانلودکننده عمومی با ادامه دانلود و دنبال کردن تغییر مسیر */
object Downloader {
    fun fetch(url: String, part: File, cancelled: () -> Boolean, progress: (Float, Long, Long) -> Unit) {
        var existing = if (part.exists()) part.length() else 0L
        var conn = open(url, existing)
        var redirects = 0
        while (conn.responseCode in 300..399 && redirects < 8) {
            val loc = conn.getHeaderField("Location"); conn.disconnect()
            conn = open(URL(URL(url), loc).toString(), existing); redirects++
        }
        val code = conn.responseCode
        if (code == 416) { conn.disconnect(); progress(1f, existing, existing); return }
        if (code !in 200..299) throw IllegalStateException("دانلود ناموفق بود (کد $code). منبع دیگری را امتحان کنید.")
        if (code == 200) existing = 0L
        val len = conn.contentLengthLong
        val total = if (len > 0) existing + len else -1L
        RandomAccessFile(part, "rw").use { raf ->
            raf.setLength(existing); raf.seek(existing)
            conn.inputStream.use { input ->
                val buf = ByteArray(256 * 1024); var read = existing; var n: Int; var last = 0L
                while (input.read(buf).also { n = it } > 0) {
                    if (cancelled()) throw InterruptedException("لغو شد")
                    raf.write(buf, 0, n); read += n
                    val now = System.currentTimeMillis()
                    if (now - last > 250) { last = now; progress(if (total > 0) read.toFloat() / total else 0f, read, total) }
                }
                progress(1f, read, total)
            }
        }
    }

    private fun open(url: String, from: Long): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 20000; readTimeout = 60000; instanceFollowRedirects = false
        setRequestProperty("User-Agent", "SmartPlant/1.0 (Android)")
        if (from > 0) setRequestProperty("Range", "bytes=$from-")
    }

    fun unzip(zipFile: File, target: File) {
        target.deleteRecursively(); target.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val f = File(target, e.name)
                if (!f.canonicalPath.startsWith(target.canonicalPath)) throw SecurityException("zip")
                if (e.isDirectory) f.mkdirs() else { f.parentFile?.mkdirs(); f.outputStream().use { zip.copyTo(it) } }
                e = zip.nextEntry
            }
        }
    }
}
