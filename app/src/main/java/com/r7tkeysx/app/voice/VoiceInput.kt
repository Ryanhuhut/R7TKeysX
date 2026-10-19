package com.r7tkeysx.app.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineQwen3AsrModelConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.r7tkeysx.app.data.LanguageStore
import com.r7tkeysx.app.data.KeyboardSettings
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.sqrt

// Trạng thái nhập bằng giọng nói, để bàn phím vẽ nút mic và dòng chữ trên thanh gợi ý
enum class VoiceState { IDLE, LISTENING, RECOGNIZING, ERROR }

// Nhập bằng giọng nói, chạy hoàn toàn trên máy (offline) bằng model nhận diện nhẹ cho MỘT ngôn ngữ (tiếng Việt hoặc tiếng Anh)
// qua thư viện sherpa-onnx. Model không đóng sẵn trong file cài mà tải về lúc người dùng chọn (xem VoiceModels.kt).
//
// Cách chạy: bấm mic -> vừa ghi âm vừa nạp model. Mỗi lần người nói ngừng một nhịp, đoạn vừa nói được
// nhận diện ngay và chữ hiện ra, trong khi máy vẫn tiếp tục nghe. Im lặng lâu hoặc bấm mic lần nữa là dừng.
// Chỉ có MỘT bản trong cả app (app cài đặt và bàn phím hệ thống dùng chung model đã nạp).
class VoiceInput private constructor(private val appContext: Context) {
    var state by mutableStateOf(VoiceState.IDLE)
        private set
    var level by mutableFloatStateOf(0f) // độ to giọng nói 0..1, để nút mic "đập" theo
        private set
    var message by mutableStateOf<String?>(null) // lỗi để hiện trên thanh gợi ý
        private set

    // Dòng chữ ngắn cho thanh gợi ý (ô giữa); null = không có gì để báo
    val statusText: String?
        get() = when (state) {
            VoiceState.IDLE -> null
            VoiceState.LISTENING -> "Listening\u2026"
            VoiceState.RECOGNIZING -> "Writing\u2026"
            VoiceState.ERROR -> message
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadLock = Mutex()
    private var recognizer: OfflineRecognizer? = null
    private var recognizerModel: VoiceModel? = null // model của recognizer đang nạp (đổi model thì nạp lại)
    private var session: Job? = null
    private var releaseJob: Job? = null
    @Volatile private var stopRequested = false
    @Volatile private var discard = false

    private val models = VoiceModelManager.get(appContext)

    // Model đang dùng để nhận giọng: chế độ "One language" thì theo ngôn ngữ bàn phím đang bật (Việt -> model Việt, còn lại -> model Anh);
    // chế độ "Multilingual" thì dùng model đa ngôn ngữ người dùng đã chọn
    val activeModel: VoiceModel
        get() {
            val s = KeyboardSettings.get(appContext)
            return when (s.voiceMode.value) {
                VoiceMode.MULTILINGUAL -> s.voiceMultiModel.value
                VoiceMode.ONE_LANGUAGE -> when (s.voiceSmallPick.value) {
                    VoiceSmallPick.VI -> VoiceModel.VI
                    VoiceSmallPick.EN -> VoiceModel.EN
                    VoiceSmallPick.AUTO -> if (LanguageStore.get(appContext).selectedId == "vi") VoiceModel.VI else VoiceModel.EN
                }
            }
        }

    fun isModelInstalled(): Boolean = models.isInstalled(activeModel)

    fun hasMicPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // Bấm mic: đang nghe thì dừng, đang rảnh thì bắt đầu. onText được gọi (trên luồng giao diện) cho từng đoạn nhận ra.
    fun toggle(onText: (String) -> Unit) {
        when (state) {
            VoiceState.LISTENING -> stopRequested = true
            VoiceState.RECOGNIZING -> Unit // đang xử lý đoạn cuối, đợi chút
            VoiceState.IDLE, VoiceState.ERROR -> start(onText)
        }
    }

    // Bàn phím bị ẩn / chuyển sang ô nhập khác: dừng nghe và bỏ chữ chưa kịp gõ ra (tránh gõ nhầm vào ô khác)
    fun cancel() {
        if (state == VoiceState.IDLE) return
        discard = true
        stopRequested = true
    }

    private fun start(onText: (String) -> Unit) {
        if (!hasMicPermission()) return fail("Mic not allowed")
        if (!isModelInstalled()) return fail("Download a voice model first")
        releaseJob?.cancel()
        stopRequested = false
        discard = false
        message = null
        state = VoiceState.LISTENING
        val active = activeModel // model theo lúc bắt đầu nói (đổi ngôn ngữ bàn phím giữa chừng không làm lệch)
        session = scope.launch {
            val model = async { loadRecognizer(active) } // nạp model song song với ghi âm, người dùng nói được ngay
            val segments = Channel<FloatArray>(Channel.UNLIMITED)
            // "Người viết chữ": lấy từng đoạn âm thanh ra nhận diện, theo đúng thứ tự
            var wrote = false
            val writer = launch {
                val rec = model.await() ?: return@launch
                for (samples in segments) {
                    val raw = runCatching { decode(rec, samples) }.getOrDefault("")
                    // Model đôi khi đoán nhầm đoạn ngắn / không rõ thành tiếng Trung. Các bàn phím ở đây đều là chữ Latinh,
                    // nên kết quả có chữ Hán / Nhật / Hàn coi như nghe không ra, bỏ đi.
                    val text = if (active.kind == VoiceKind.QWEN && hasCjk(raw)) "" else tidy(raw, active)
                    saveDebugAudio(samples, raw)
                    if (text.isNotBlank() && !discard) {
                        wrote = true
                        withContext(Dispatchers.Main) { onText(text) }
                    }
                }
            }
            runCatching { record(segments) }
            segments.close()
            if (!discard) state = VoiceState.RECOGNIZING
            writer.join()
            when {
                model.await() == null -> fail("Model failed")
                !wrote && !discard -> fail("Didn't catch that")
                else -> state = VoiceState.IDLE
            }
            level = 0f
            scheduleRelease()
        }
    }

    private fun fail(text: String) {
        message = text
        state = VoiceState.ERROR
        scope.launch {
            delay(ERROR_SHOW_MS)
            if (state == VoiceState.ERROR) { state = VoiceState.IDLE; message = null }
        }
    }

    // Ghi âm 16 kHz rồi gom cả câu mới đưa đi nhận diện (đoạn quá ngắn dễ bị model đoán sai ngôn ngữ).
    // Đo độ to để biết lúc nào đang nói; nói dài thì cắt ở chỗ ngừng hơi lâu để chữ hiện dần.
    // Trả về true nếu có nghe thấy tiếng nói.
    @SuppressLint("MissingPermission") // đã kiểm tra quyền ở start()
    private suspend fun record(segments: Channel<FloatArray>): Boolean = withContext(Dispatchers.IO) {
        val frame = SAMPLE_RATE / 10 // khung 100 ms
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val audio = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, frame * 2 * 4),
        )
        if (audio.state != AudioRecord.STATE_INITIALIZED) { audio.release(); return@withContext false }
        val pcm = ShortArray(frame)
        val current = ArrayList<FloatArray>() // các khung của câu đang nói (kể cả chỗ ngừng ngắn)
        val preRoll = ArrayDeque<FloatArray>() // vài khung ngay trước khi bắt đầu nói (để không mất âm đầu)
        var noise = -1f
        var inSpeech = false
        var silentFrames = 0 // số khung im lặng liên tiếp gần nhất
        var speechFrames = 0 // số khung có tiếng trong câu đang gom
        var idleFrames = 0
        var heard = false

        suspend fun flush() {
            // bỏ phần im lặng ở cuối (giữ lại 0,3 s), bỏ hẳn nếu chỉ là tiếng động ngắn
            val keep = (current.size - (silentFrames - TAIL_KEEP_FRAMES).coerceAtLeast(0)).coerceAtLeast(0)
            if (speechFrames >= MIN_SPEECH_FRAMES && keep > 0) segments.send(join(current.subList(0, keep)))
            current.clear()
            speechFrames = 0
        }

        try {
            audio.startRecording()
            while (!stopRequested) {
                val n = audio.read(pcm, 0, frame)
                if (n <= 0) continue
                val f = FloatArray(n) { pcm[it] / 32768f }
                var sum = 0f
                for (x in f) sum += x * x
                val rms = sqrt(sum / n)
                level = (level * 0.5f + (rms / LOUD_RMS).coerceIn(0f, 1f) * 0.5f)
                // nền ồn: bám nhanh khi nhỏ đi, tăng rất chậm khi to lên (để giọng nói không bị tính là ồn)
                noise = if (noise < 0f) rms else if (rms < noise) noise * 0.5f + rms * 0.5f else noise * 0.995f + rms * 0.005f
                val speech = rms > max(MIN_SPEECH_RMS, noise * 3f)

                if (!inSpeech) {
                    preRoll.addLast(f)
                    if (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()
                    if (speech) {
                        inSpeech = true
                        heard = true
                        silentFrames = 0
                        current.addAll(preRoll)
                        preRoll.clear()
                    } else if (++idleFrames >= (if (heard) END_SILENCE_FRAMES else NO_SPEECH_FRAMES)) {
                        break // im lặng lâu: tự dừng
                    }
                    continue
                }
                current += f
                if (speech) { speechFrames++; silentFrames = 0 } else silentFrames++
                when {
                    // im hẳn: chốt câu cuối và dừng nghe
                    silentFrames >= END_SILENCE_FRAMES -> { flush(); inSpeech = false; break }
                    // nói đã lâu và vừa ngừng một nhịp: gửi đoạn này đi nhận diện, nghe tiếp
                    (silentFrames >= PAUSE_FRAMES && current.size >= MIN_SPLIT_FRAMES) || current.size >= MAX_SEGMENT_FRAMES -> {
                        flush(); inSpeech = false; idleFrames = silentFrames
                    }
                }
            }
        } finally {
            runCatching { audio.stop() }
            audio.release()
        }
        if (inSpeech && current.isNotEmpty()) flush()
        heard
    }

    // Làm sạch kết quả theo model: model tiếng Việt viết HOA hết nên đưa về chữ thường (bàn phím tự viết hoa đầu câu);
    // model tiếng Anh đôi khi thừa một dấu " ở cuối
    private fun tidy(raw: String, model: VoiceModel): String {
        var t = raw.trim()
        if (model == VoiceModel.VI) t = t.lowercase(java.util.Locale("vi"))
        if (t.endsWith("\"") && t.count { it == '"' } % 2 == 1) t = t.dropLast(1).trimEnd()
        return t
    }

    private fun hasCjk(text: String) = text.any {
        val b = Character.UnicodeBlock.of(it)
        b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || b == Character.UnicodeBlock.HIRAGANA ||
            b == Character.UnicodeBlock.KATAKANA || b == Character.UnicodeBlock.HANGUL_SYLLABLES ||
            b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
    }

    // Chỉ ở bản debug: lưu 8 đoạn âm thanh gần nhất (files/voice_debug/*.wav) + chữ nhận ra vào logcat (thẻ VoiceInput),
    // để kéo về máy tính xem micro thu được gì khi nhận diện sai
    private fun saveDebugAudio(samples: FloatArray, text: String) {
        if (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        runCatching {
            var peak = 0f
            for (x in samples) peak = max(peak, kotlin.math.abs(x))
            val dir = File(appContext.filesDir, "voice_debug").apply { mkdirs() }
            val name = "seg_${System.currentTimeMillis()}.wav"
            Log.i("VoiceInput", "$name ${samples.size * 1000 / SAMPLE_RATE} ms peak=$peak -> $text")
            val pcm = java.nio.ByteBuffer.allocate(44 + samples.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            pcm.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples.size * 2)
            for (x in samples) pcm.putShort((x.coerceIn(-1f, 1f) * 32767).toInt().toShort())
            File(dir, name).writeBytes(pcm.array())
            dir.listFiles()?.sortedByDescending { it.name }?.drop(8)?.forEach { it.delete() }
        }
    }

    private fun join(frames: List<FloatArray>): FloatArray {
        val out = FloatArray(frames.sumOf { it.size })
        var i = 0
        for (fr in frames) { fr.copyInto(out, i); i += fr.size }
        return out
    }

    private fun decode(rec: OfflineRecognizer, samples: FloatArray): String {
        val stream = rec.createStream()
        try {
            // thêm chút im lặng ở cuối để model "nghe" trọn chữ cuối
            stream.acceptWaveform(samples + FloatArray(SAMPLE_RATE / 4), SAMPLE_RATE)
            rec.decode(stream)
            return rec.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    // Chỉ để thử (bản debug): nhận diện thẳng một đoạn âm thanh 16 kHz, không qua micro
    suspend fun transcribeForTest(samples: FloatArray): String? =
        withContext(Dispatchers.Default) { loadRecognizer(activeModel)?.let { decode(it, samples) } }

    // Nạp model đang dùng (vài giây); đổi model thì bỏ bản cũ nạp bản mới. Trả về null nếu lỗi.
    private suspend fun loadRecognizer(wanted: VoiceModel): OfflineRecognizer? = loadLock.withLock {
        if (recognizer != null && recognizerModel != wanted) { recognizer?.release(); recognizer = null }
        recognizer ?: runCatching {
            val dir = models.dir(wanted).absolutePath
            val f = wanted.files.map { "$dir/${it.name}" }
            val model = when (wanted.kind) {
                // zipformer transducer một ngôn ngữ: files = [encoder, decoder, joiner, tokens]
                VoiceKind.TRANSDUCER -> OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(encoder = f[0], decoder = f[1], joiner = f[2]),
                    tokens = f[3], numThreads = THREADS, provider = "cpu",
                )
                // Whisper đa ngôn ngữ: files = [encoder, decoder, tokens]; language rỗng = tự nhận ra ngôn ngữ đang nói
                VoiceKind.WHISPER -> OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(encoder = f[0], decoder = f[1], language = "", task = "transcribe"),
                    tokens = f[2], numThreads = THREADS, provider = "cpu",
                )
                // Qwen3-ASR: đa ngôn ngữ, tự nhận ra ngôn ngữ
                VoiceKind.QWEN -> OfflineModelConfig(
                    qwen3Asr = OfflineQwen3AsrModelConfig(
                        convFrontend = "$dir/conv_frontend.onnx",
                        encoder = "$dir/encoder.int8.onnx",
                        decoder = "$dir/decoder.int8.onnx",
                        tokenizer = "$dir/tokenizer",
                        maxTotalLen = 1024,
                        maxNewTokens = 384,
                    ),
                    numThreads = THREADS, provider = "cpu",
                )
            }
            OfflineRecognizer(config = OfflineRecognizerConfig(featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80), modelConfig = model))
        }.getOrNull().also { recognizer = it; recognizerModel = wanted }
    }

    // Không nói nữa một lúc thì trả lại bộ nhớ (model vẫn chiếm vài trăm MB RAM)
    private fun scheduleRelease() {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(RELEASE_AFTER_MS)
            loadLock.withLock {
                if (state == VoiceState.IDLE || state == VoiceState.ERROR) {
                    recognizer?.release()
                    recognizer = null
                }
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val THREADS = 4 // số nhân CPU dùng để nhận diện
        private const val MIN_SPEECH_RMS = 0.012f // nhỏ hơn mức này coi như im lặng
        private const val LOUD_RMS = 0.12f // mức "to" cho hiệu ứng nút mic
        private const val PRE_ROLL_FRAMES = 3 // giữ 0,3 s trước tiếng nói
        private const val PAUSE_FRAMES = 12 // ngừng 1,2 s ...
        private const val MIN_SPLIT_FRAMES = 60 // ... khi đã nói được 6 s thì cắt đoạn để chữ hiện dần
        private const val END_SILENCE_FRAMES = 20 // im 2 s sau khi đã nói = tự dừng
        private const val TAIL_KEEP_FRAMES = 3 // giữ 0,3 s im lặng cuối đoạn
        private const val MIN_SPEECH_FRAMES = 3 // đoạn có tiếng nói ngắn hơn 0,3 s coi là tiếng động, bỏ qua
        private const val NO_SPEECH_FRAMES = 60 // chưa nói gì trong 6 s = tự dừng
        private const val MAX_SEGMENT_FRAMES = 250 // một đoạn dài nhất 25 s
        private const val ERROR_SHOW_MS = 3000L
        private const val RELEASE_AFTER_MS = 90_000L

        @Volatile private var instance: VoiceInput? = null

        fun get(context: Context): VoiceInput = instance ?: synchronized(this) {
            instance ?: VoiceInput(context.applicationContext).also { instance = it }
        }
    }
}
