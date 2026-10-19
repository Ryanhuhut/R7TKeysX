package com.r7tkeysx.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.r7tkeysx.app.model.DefaultLanguageIds
import com.r7tkeysx.app.model.KeyboardLanguage
import com.r7tkeysx.app.model.LanguageCatalog

// Danh sách ngôn ngữ đang bật + ngôn ngữ đang dùng. Chỉ có MỘT bản trong cả app (như KeyboardSettings),
// nên thêm / xóa ở màn Layouts là quả địa cầu trên bàn phím đổi theo ngay.
class LanguageStore private constructor(private val prefs: SharedPreferences) {
    var enabled by mutableStateOf(loadEnabled())
        private set
    var selectedId by mutableStateOf(loadSelected())
        private set

    val index: Int get() = enabled.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    fun select(id: String) {
        if (enabled.none { it.id == id } || id == selectedId) return
        selectedId = id
        save()
    }

    fun add(lang: KeyboardLanguage) {
        if (enabled.any { it.id == lang.id }) return
        enabled = enabled + lang
        save()
    }

    // Luôn giữ lại ít nhất một bàn phím
    fun remove(id: String) {
        if (enabled.size <= 1) return
        enabled = enabled.filterNot { it.id == id }
        if (selectedId == id) selectedId = enabled.first().id
        save()
    }

    fun reset() {
        enabled = defaults()
        selectedId = enabled.first().id
        save()
    }

    private fun save() {
        prefs.edit().putString(KEY_LANGUAGES, enabled.joinToString(",") { it.id }).putString(KEY_SELECTED, selectedId).apply()
    }

    private fun defaults() = DefaultLanguageIds.mapNotNull { id -> LanguageCatalog.firstOrNull { it.id == id } }

    private fun loadEnabled(): List<KeyboardLanguage> {
        val ids = prefs.getString(KEY_LANGUAGES, null)?.split(",").orEmpty()
        return ids.mapNotNull { id -> LanguageCatalog.firstOrNull { it.id == id } }.ifEmpty { defaults() }
    }

    // Bản cũ chỉ lưu số thứ tự (language_index) trong danh sách mặc định; vẫn đọc được để không mất lựa chọn
    private fun loadSelected(): String {
        val list = loadEnabled()
        val saved = prefs.getString(KEY_SELECTED, null)
        if (saved != null && list.any { it.id == saved }) return saved
        return list.getOrNull(prefs.getInt("language_index", 0)) ?.id ?: list.first().id
    }

    companion object {
        private const val KEY_LANGUAGES = "languages"
        private const val KEY_SELECTED = "language_id"

        @Volatile private var instance: LanguageStore? = null

        fun get(context: Context): LanguageStore = instance ?: synchronized(this) {
            instance ?: LanguageStore(
                context.applicationContext.getSharedPreferences("r7t_prefs", Context.MODE_PRIVATE),
            ).also { instance = it }
        }
    }
}
