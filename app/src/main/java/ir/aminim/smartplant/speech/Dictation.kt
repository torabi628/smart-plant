package ir.aminim.smartplant.speech

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import org.vosk.Recognizer
import kotlin.math.log10
import kotlin.math.sqrt

/** دیکته صوتی فارسی: متن نهایی + متن در حال شنیدن + سطح صدا برای موج متحرک */
abstract class Dictation {
    var listening by mutableStateOf(false); protected set
    var partial by mutableStateOf(""); protected set
    var finalText by mutableStateOf(""); protected set
    var level by mutableFloatStateOf(0f); protected set
    var error by mutableStateOf<String?>(null); protected set

    val text: String get() = listOf(finalText, partial).filter { it.isNotBlank() }.joinToString(" ").trim()

    abstract fun start()
    abstract fun stop()
    abstract fun release()

    fun clear() { finalText = ""; partial = ""; error = null }

    protected fun append(t: String) {
        val s = t.trim()
        if (s.isNotEmpty()) finalText = (finalText + " " + s).trim()
    }
}

/** موتور آنلاین گفتار اندروید (Google) با شروع خودکار پس از هر مکث */
class GoogleDictation(private val context: Context) : Dictation() {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var want = false

    fun available() = SpeechRecognizer.isRecognitionAvailable(context)

    private val intent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    override fun start() {
        if (!available()) {
            error = "سرویس گفتار گوگل روی این گوشی فعال نیست؛ از تنظیمات، موتور «آفلاین» را انتخاب کنید یا برنامه Google را نصب کنید."
            return
        }
        error = null; want = true; listening = true
        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(listener) }
        listen()
    }

    override fun stop() {
        want = false
        if (partial.isNotBlank()) { append(partial); partial = "" }
        handler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.stopListening(); recognizer?.cancel() }
        listening = false; level = 0f
    }

    override fun release() {
        want = false; handler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.destroy() }; recognizer = null; listening = false
    }

    private fun listen() {
        if (!want) return
        try { recognizer?.startListening(intent) } catch (t: Throwable) { later(400) }
    }

    private fun later(ms: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ if (want) listen() }, ms)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(code: Int) {
            when (code) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> later(150)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> { recognizer?.cancel(); later(500) }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { error = "برای دستور صوتی، اجازه میکروفون لازم است."; stop() }
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> {
                    error = "اتصال سرویس گفتار برقرار نشد؛ اینترنت را بررسی کنید یا از تنظیمات موتور آفلاین را فعال کنید."
                    later(2500)
                }
                else -> later(400)
            }
        }
        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { append(it) }
            partial = ""; error = null
            later(60)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { partial = it }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

/** موتور آفلاین گفتار (Vosk) */
class VoskDictation(private val context: Context) : Dictation() {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var thread: Thread? = null

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!VoskModels.ready(context)) { error = "مدل گفتار آفلاین نصب نیست؛ از «مرکز هوش مصنوعی» آن را دانلود کنید."; return }
        if (running) return
        error = null; running = true; listening = true
        thread = Thread {
            try {
                val rec = Recognizer(VoskModels.load(context), 16000f)
                val minBuf = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val ar = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 8192) * 2)
                ar.startRecording()
                val buf = ShortArray(4000)
                while (running) {
                    val n = ar.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    var sum = 0.0
                    for (i in 0 until n) sum += buf[i] * buf[i].toDouble()
                    val lv = ((20 * log10(sqrt(sum / n) + 1) - 30) / 45).toFloat().coerceIn(0f, 1f)
                    if (rec.acceptWaveForm(buf, n)) {
                        val t = JSONObject(rec.result).optString("text")
                        main.post { level = lv; partial = ""; append(t) }
                    } else {
                        val p = JSONObject(rec.partialResult).optString("partial")
                        main.post { level = lv; partial = p }
                    }
                }
                ar.stop(); ar.release()
                val t = JSONObject(rec.finalResult).optString("text")
                main.post { partial = ""; append(t) }
                rec.close()
            } catch (t: Throwable) {
                main.post { error = "خطای موتور آفلاین: ${t.message}"; listening = false }
            }
        }.also { it.start() }
    }

    override fun stop() {
        running = false; thread?.join(1500); listening = false; level = 0f
    }

    override fun release() { stop() }
}
