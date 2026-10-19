package com.r7tkeysx.app.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

// Một mục trong lịch sử clipboard (hiện mới lưu chữ; ảnh / video chưa hỗ trợ)
class ClipItem(val id: Long, val text: String, val pinned: Boolean, val time: Long, val sensitive: Boolean)

// Lịch sử clipboard riêng của bàn phím. Chỉ có MỘT bản trong cả app, nhớ lại sau khi tắt máy.
// Các quy tắc (giới hạn số mục, dọn mục cũ, dọn mục nhạy cảm) đọc từ KeyboardSettings.
class ClipboardStore private constructor(private val prefs: SharedPreferences) {
    // mới nhất ở đầu danh sách
    var items by mutableStateOf(load())
        private set

    // Thêm mục mới (hoặc đưa mục trùng nội dung lên đầu), rồi áp dụng giới hạn / dọn dẹp
    fun add(text: String, sensitive: Boolean, settings: KeyboardSettings, now: Long = System.currentTimeMillis()) {
        if (text.isBlank()) return
        val old = items.firstOrNull { it.text == text }
        val item = ClipItem(old?.id ?: now, text, old?.pinned ?: false, now, sensitive || (old?.sensitive ?: false))
        items = listOf(item) + items.filterNot { it.text == text }
        cleanup(settings, now)
    }

    fun togglePin(id: Long) {
        items = items.map { if (it.id == id) ClipItem(it.id, it.text, !it.pinned, it.time, it.sensitive) else it }
        save()
    }

    fun remove(ids: Set<Long>) {
        items = items.filterNot { it.id in ids }
        save()
    }

    // Xóa hết, trừ mục đã ghim
    fun clearUnpinned() {
        items = items.filter { it.pinned }
        save()
    }

    // Mục mới nhất, nếu được chép trong vòng `seconds` giây (dùng cho gợi ý trên thanh gợi ý)
    fun latestWithin(seconds: Int, now: Long = System.currentTimeMillis()): ClipItem? =
        items.firstOrNull()?.takeIf { now - it.time <= seconds * 1000L }

    // Dọn theo cài đặt; mục đã ghim thì không bao giờ bị dọn
    fun cleanup(settings: KeyboardSettings, now: Long = System.currentTimeMillis()) {
        var list = items
        if (settings.clipCleanOld.value) {
            val limit = settings.clipCleanOldMinutes.value * 60_000L
            list = list.filter { it.pinned || now - it.time <= limit }
        }
        if (settings.clipCleanSensitive.value) {
            val limit = settings.clipCleanSensitiveSeconds.value * 1000L
            list = list.filter { it.pinned || !it.sensitive || now - it.time <= limit }
        }
        if (settings.clipLimitHistory.value) {
            val max = settings.clipMaxHistory.value
            var kept = 0
            list = list.filter { if (it.pinned) true else (++kept <= max) }
        }
        if (list.size != items.size) items = list
        save()
    }

    private fun save() {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().put("id", it.id).put("t", it.text).put("p", it.pinned).put("ts", it.time).put("s", it.sensitive))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun load(): List<ClipItem> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        (0 until array.length()).map {
            val o = array.getJSONObject(it)
            ClipItem(o.getLong("id"), o.getString("t"), o.optBoolean("p"), o.optLong("ts"), o.optBoolean("s"))
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY = "items"

        @Volatile private var instance: ClipboardStore? = null

        fun get(context: Context): ClipboardStore = instance ?: synchronized(this) {
            instance ?: ClipboardStore(
                context.applicationContext.getSharedPreferences("r7t_clipboard", Context.MODE_PRIVATE),
            ).also { instance = it }
        }

        // Đọc clipboard hệ thống và đưa vào lịch sử (nếu bật lịch sử và cho phép đồng bộ chiều "from system")
        fun captureSystemClip(context: Context, settings: KeyboardSettings) {
            if (!settings.clipHistory.value) return
            val manager = context.getSystemService(ClipboardManager::class.java) ?: return
            val clip: ClipData? = runCatching { manager.primaryClip }.getOrNull()
            val store = get(context)
            if (clip == null || clip.itemCount == 0) {
                // clipboard hệ thống bị xóa: nếu cho phép thì bỏ mục mới nhất chưa ghim khỏi lịch sử
                if (settings.clipSyncFrom.value.includesClear && settings.clipClearPrimaryAffects.value) {
                    store.items.firstOrNull { !it.pinned }?.let { store.remove(setOf(it.id)) }
                }
                return
            }
            if (!settings.clipSyncFrom.value.includesSet) return
            val text = runCatching { clip.getItemAt(0).coerceToText(context)?.toString() }.getOrNull()?.trim().orEmpty()
            if (text.isEmpty() || text.length > MAX_CLIP_LENGTH) return
            val sensitive = clip.description?.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true
            // đã là mục mới nhất rồi thì thôi (tránh làm mới thời gian mỗi lần mở bàn phím)
            if (store.items.firstOrNull()?.text == text) return
            store.add(text, sensitive, settings)
        }

        // Đặt chữ vào clipboard hệ thống (khi cho phép đồng bộ chiều "to system")
        fun setSystemClip(context: Context, text: String) {
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("R7TKeysX", text))
        }

        private const val MAX_CLIP_LENGTH = 5000
    }
}
