package com.r7tkeysx.app.voice

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Một file của model: tên lưu trên máy, đường dẫn tải, dung lượng (byte) để vẽ thanh tiến độ
class ModelFile(val name: String, val url: String, val bytes: Long)

// Cách nạp model vào thư viện sherpa-onnx
enum class VoiceKind { TRANSDUCER, WHISPER, QWEN }

// Hai chế độ nhận giọng nói
enum class VoiceMode(val label: String, val description: String) {
    ONE_LANGUAGE("One language", "Uses the Vietnamese or the English model to match the language of your keyboard."),
    MULTILINGUAL("Multilingual", "One bigger model that understands many languages by itself, whichever keyboard is active."),
}

// Chế độ "One language": tự theo ngôn ngữ bàn phím, hoặc ép dùng hẳn model Việt / Anh (chọn khi giữ nút mic)
enum class VoiceSmallPick { AUTO, VI, EN }

// Các model nhận diện giọng nói người dùng có thể tải về (không đóng sẵn trong file cài).
// Muốn đổi nguồn tải (vd. sang Google Drive của bạn) thì chỉ sửa các `url` ở đây; thêm model mới = thêm một mục
// (thư viện đã biết 3 kiểu: TRANSDUCER, WHISPER, QWEN; kiểu khác thì thêm nhánh ở VoiceInput.loadRecognizer).
// `small` = model nhỏ: tiến độ vẽ vòng tròn, không có nút huỷ; model to thì vẽ thanh dài kèm nút Cancel.
enum class VoiceModel(
    val label: String,
    val description: String,
    val folder: String,
    val files: List<ModelFile>,
    val kind: VoiceKind,
    val small: Boolean,
    val multilingual: Boolean,
) {
    VI(
        "Vietnamese", "Fast and light. Used when your keyboard is in Vietnamese.", "voice-vi",
        hf("csukuangfj2/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09").let { repo ->
            listOf(
                ModelFile("encoder.int8.onnx", "$repo/encoder.int8.onnx", 27_699_063),
                ModelFile("decoder.onnx", "$repo/decoder.onnx", 5_165_084),
                ModelFile("joiner.int8.onnx", "$repo/joiner.int8.onnx", 1_033_417),
                ModelFile("tokens.txt", "$repo/tokens.txt", 23_238),
            )
        },
        VoiceKind.TRANSDUCER, small = true, multilingual = false,
    ),
    EN(
        "English", "Adds punctuation and capital letters. Used when your keyboard is in English.", "voice-en",
        hf("csukuangfj/sherpa-onnx-zipformer-en-libriheavy-20230830-small-punct-case").let { repo ->
            listOf(
                ModelFile("encoder.int8.onnx", "$repo/encoder-epoch-88-avg-41.int8.onnx", 68_780_141),
                ModelFile("decoder.int8.onnx", "$repo/decoder-epoch-88-avg-41.int8.onnx", 670_318),
                ModelFile("joiner.int8.onnx", "$repo/joiner-epoch-88-avg-41.int8.onnx", 391_431),
                ModelFile("tokens.txt", "$repo/tokens.txt", 7_368),
            )
        },
        VoiceKind.TRANSDUCER, small = true, multilingual = false,
    ),
    WHISPER_SMALL(
        "Whisper Small", "Understands many languages. A good balance of speed and accuracy.", "voice-whisper-small",
        hf("csukuangfj/sherpa-onnx-whisper-small").let { repo ->
            listOf(
                ModelFile("small-encoder.int8.onnx", "$repo/small-encoder.int8.onnx", 112_442_483),
                ModelFile("small-decoder.int8.onnx", "$repo/small-decoder.int8.onnx", 262_226_114),
                ModelFile("small-tokens.txt", "$repo/small-tokens.txt", 816_730),
            )
        },
        VoiceKind.WHISPER, small = false, multilingual = true,
    ),
    WHISPER_MEDIUM(
        "Whisper Medium", "More accurate than Small, but slower and needs more memory.", "voice-whisper-medium",
        hf("csukuangfj/sherpa-onnx-whisper-medium").let { repo ->
            listOf(
                ModelFile("medium-encoder.int8.onnx", "$repo/medium-encoder.int8.onnx", 374_196_283),
                ModelFile("medium-decoder.int8.onnx", "$repo/medium-decoder.int8.onnx", 571_059_257),
                ModelFile("medium-tokens.txt", "$repo/medium-tokens.txt", 816_730),
            )
        },
        VoiceKind.WHISPER, small = false, multilingual = true,
    ),
    // Qwen3-ASR 0.6B: nhiều ngôn ngữ, tự đặt dấu câu. Nặng: ~1 GB tải, ~1,5 GB RAM khi chạy. (Giữ thư mục "voice-multi" của bản cũ.)
    QWEN(
        "Qwen", "Many languages with punctuation. The most capable here, and the heaviest.", "voice-multi",
        hf("csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25").let { repo ->
            listOf(
                ModelFile("encoder.int8.onnx", "$repo/encoder.int8.onnx", 182_491_662),
                ModelFile("conv_frontend.onnx", "$repo/conv_frontend.onnx", 44_148_281),
                ModelFile("decoder.int8.onnx", "$repo/decoder.int8.onnx", 755_914_231),
                ModelFile("tokenizer/vocab.json", "$repo/tokenizer/vocab.json", 2_776_833),
                ModelFile("tokenizer/merges.txt", "$repo/tokenizer/merges.txt", 1_671_853),
                ModelFile("tokenizer/tokenizer_config.json", "$repo/tokenizer/tokenizer_config.json", 12_487),
            )
        },
        VoiceKind.QWEN, small = false, multilingual = true,
    );

    val totalBytes get() = files.sumOf { it.bytes }
    val totalMb get() = (totalBytes / 1_000_000f).toInt()
    val sizeLabel get() = "$totalMb MB"

    companion object {
        val multilingualModels get() = entries.filter { it.multilingual }
        val perLanguageModels get() = entries.filter { !it.multilingual }
    }
}

private fun hf(repo: String) = "https://huggingface.co/$repo/resolve/main"

// Tình trạng model
sealed interface ModelState {
    data object NotInstalled : ModelState
    data class Downloading(val progress: Float) : ModelState // 0..1
    data object Installed : ModelState
    data class Failed(val reason: String) : ModelState
}

// Loại mạng đang dùng: Wi-Fi (tải thoải mái), di động (phải hỏi người dùng), hoặc không có mạng
enum class NetworkKind { WIFI, CELLULAR, NONE }

// Quản lý việc tải / xóa model. Chỉ có MỘT bản trong cả app (app cài đặt và bàn phím dùng chung), sống cùng tiến trình.
class VoiceModelManager private constructor(private val appContext: Context) {
    // state theo từng model; là state của Compose nên màn cài đặt tự vẽ lại khi tiến độ đổi
    private val states = mutableStateMapOf<VoiceModel, ModelState>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = HashMap<VoiceModel, Job>()
    private val prefs = appContext.getSharedPreferences("r7t_voice_models", Context.MODE_PRIVATE)
    private var callbackRegistered = false

    fun dir(model: VoiceModel) = File(appContext.filesDir, model.folder)

    fun isInstalled(model: VoiceModel): Boolean =
        File(dir(model), OK_MARKER).isFile && model.files.all { File(dir(model), it.name).isFile }

    fun state(model: VoiceModel): ModelState =
        states[model] ?: if (isInstalled(model)) ModelState.Installed else ModelState.NotInstalled

    fun networkKind(): NetworkKind {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return NetworkKind.NONE
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return NetworkKind.NONE) ?: return NetworkKind.NONE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return NetworkKind.NONE
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) NetworkKind.WIFI
        else NetworkKind.CELLULAR
    }

    // Model người dùng đã chọn tải nhưng chưa có Wi-Fi: tự tải khi có Wi-Fi
    var pending: VoiceModel?
        get() = prefs.getString(KEY_PENDING, null)?.let { n -> VoiceModel.entries.firstOrNull { it.name == n } }
        private set(v) { prefs.edit().apply { if (v == null) remove(KEY_PENDING) else putString(KEY_PENDING, v.name) }.apply() }

    // Người dùng chọn tải một model. Wi-Fi -> tải ngay. Di động -> trả về CELLULAR để giao diện hỏi lại rồi gọi download().
    // Không có mạng -> nhớ lại, có Wi-Fi sẽ tự tải. Trả về loại mạng hiện tại.
    fun request(model: VoiceModel): NetworkKind {
        val kind = networkKind()
        when (kind) {
            NetworkKind.WIFI -> download(model)
            NetworkKind.CELLULAR -> Unit // chờ người dùng đồng ý
            NetworkKind.NONE -> { pending = model; states[model] = ModelState.Failed("No connection. It will download when Wi-Fi is available."); watchForWifi() }
        }
        return kind
    }

    // Người dùng từ chối tải bằng 4G: vẫn nhớ để tự tải khi có Wi-Fi
    fun postponeToWifi(model: VoiceModel) {
        pending = model
        watchForWifi()
    }

    // Gọi khi mở app: có model đang chờ mà đã có Wi-Fi thì tải luôn
    fun resumePending() {
        val model = pending ?: return
        if (isInstalled(model)) { pending = null; return }
        if (networkKind() == NetworkKind.WIFI) download(model) else watchForWifi()
    }

    private fun watchForWifi() {
        if (callbackRegistered) return
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return
        callbackRegistered = true
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    if (pending != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) resumePending()
                }
            })
        }.onFailure { callbackRegistered = false }
    }

    // Tải các file còn thiếu (tải tiếp được file dở), ghi file ".ok" khi đủ. Xong thì dọn model đa ngôn ngữ cũ.
    fun download(model: VoiceModel) {
        if (jobs[model]?.isActive == true || isInstalled(model)) return
        pending = null
        states[model] = ModelState.Downloading(0f)
        jobs[model] = scope.launch {
            runCatching {
                val dir = dir(model).apply { mkdirs() }
                File(dir, OK_MARKER).delete()
                val total = model.totalBytes.toFloat()
                var done = 0L
                var lastShown = 0f
                for (file in model.files) {
                    val target = File(dir, file.name)
                    target.parentFile?.mkdirs() // file nằm trong thư mục con (vd. tokenizer/)
                    if (target.isFile && target.length() == file.bytes) { done += file.bytes; continue }
                    downloadFile(file, File(dir, file.name + ".part"), target) { got ->
                        val p = ((done + got) / total).coerceIn(0f, 1f)
                        // chỉ báo giao diện khi tiến độ nhích >= 0,3% (khỏi vẽ lại hàng nghìn lần cho một model lớn)
                        if (p - lastShown >= 0.003f) { lastShown = p; states[model] = ModelState.Downloading(p) }
                    }
                    done += file.bytes
                }
                File(dir, OK_MARKER).writeText("ok")
            }.onSuccess {
                states[model] = ModelState.Installed
                File(appContext.filesDir, LEGACY_FOLDER).deleteRecursively() // model đa ngôn ngữ ~1 GB của bản cũ, dọn cho nhẹ máy
            }.onFailure {
                // huỷ giữa chừng (nút Cancel) không phải là lỗi
                if (it !is kotlinx.coroutines.CancellationException) states[model] = ModelState.Failed(it.message?.take(80) ?: "Download failed")
            }
        }
    }

    // Huỷ tải (nút Cancel): dừng hẳn và xóa phần đã tải dở cho đỡ chiếm dung lượng
    fun cancel(model: VoiceModel) {
        val job = jobs[model]
        if (job?.isActive == true) {
            // chỉ dọn nếu giữa chừng người dùng chưa bấm tải lại (tải lại tạo job mới, dọn nhầm sẽ xóa luôn file đang tải của job mới)
            job.invokeOnCompletion { if (jobs[model] === job && !isInstalled(model)) dir(model).deleteRecursively() }
            job.cancel()
        }
        states.remove(model)
    }

    fun delete(model: VoiceModel) {
        cancel(model)
        dir(model).deleteRecursively()
        states[model] = ModelState.NotInstalled
    }

    // Tải một file; có file dở (.part) thì xin tải tiếp từ chỗ đã có (Range) để mất mạng giữa chừng khỏi tải lại từ đầu
    private suspend fun downloadFile(file: ModelFile, part: File, target: File, onProgress: (Long) -> Unit) {
        var existing = if (part.isFile) part.length() else 0L
        if (existing >= file.bytes) { part.delete(); existing = 0L }
        val conn = (URL(file.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 30_000; instanceFollowRedirects = true
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) error("Server error $code")
            val append = code == HttpURLConnection.HTTP_PARTIAL && existing > 0
            var got = if (append) existing else 0L
            conn.inputStream.use { input ->
                java.io.FileOutputStream(part, append).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        got += n
                        onProgress(got)
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    }
                }
            }
            if (part.length() != file.bytes) { part.delete(); error("Incomplete download, try again") }
            target.delete()
            if (!part.renameTo(target)) error("Could not save the file")
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val OK_MARKER = ".ok"
        private const val KEY_PENDING = "pending"
        const val LEGACY_FOLDER = "qwen3-asr"

        @Volatile private var instance: VoiceModelManager? = null

        fun get(context: Context): VoiceModelManager = instance ?: synchronized(this) {
            instance ?: VoiceModelManager(context.applicationContext).also { instance = it }
        }
    }
}
