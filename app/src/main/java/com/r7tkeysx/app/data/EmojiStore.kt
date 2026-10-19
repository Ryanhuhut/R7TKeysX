package com.r7tkeysx.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Emoji dùng gần đây (kèm số lần dùng) và emoji đã ghim. Chỉ có MỘT bản trong cả app, nhớ lại sau khi tắt máy.
class EmojiStore private constructor(private val prefs: SharedPreferences) {
    // mới nhất ở đầu danh sách
    var recents by mutableStateOf(loadRecents())
        private set
    // theo thứ tự ghim (cái ghim sau cùng nằm cuối)
    var pinned by mutableStateOf(loadPinned())
        private set

    class Recent(val emoji: String, val count: Int)

    fun use(emoji: String, maxHistory: Int) {
        val old = recents.firstOrNull { it.emoji == emoji }
        recents = (listOf(Recent(emoji, (old?.count ?: 0) + 1)) + recents.filterNot { it.emoji == emoji }).take(maxHistory.coerceAtLeast(1))
        prefs.edit().putString(KEY_RECENTS, recents.joinToString(" ") { "${it.emoji}:${it.count}" }).apply()
    }

    // Trả về true nếu emoji đang được ghim sau thao tác (false = vừa bỏ ghim hoặc đã đủ số lượng ghim)
    fun togglePin(emoji: String, maxPinned: Int): Boolean {
        if (emoji in pinned) {
            pinned = pinned - emoji
        } else {
            if (pinned.size >= maxPinned) return false
            pinned = pinned + emoji
        }
        prefs.edit().putString(KEY_PINNED, pinned.joinToString(" ")).apply()
        return emoji in pinned
    }

    fun trimHistory(max: Int) {
        if (recents.size <= max) return
        recents = recents.take(max)
        prefs.edit().putString(KEY_RECENTS, recents.joinToString(" ") { "${it.emoji}:${it.count}" }).apply()
    }

    fun clearHistory() {
        recents = emptyList()
        prefs.edit().remove(KEY_RECENTS).apply()
    }

    fun clearPinned() {
        pinned = emptyList()
        prefs.edit().remove(KEY_PINNED).apply()
    }

    private fun loadRecents(): List<Recent> =
        prefs.getString(KEY_RECENTS, null)?.split(" ")?.mapNotNull { token ->
            val i = token.lastIndexOf(':')
            if (i <= 0) null else Recent(token.substring(0, i), token.substring(i + 1).toIntOrNull() ?: 1)
        }.orEmpty()

    private fun loadPinned(): List<String> = prefs.getString(KEY_PINNED, null)?.split(" ")?.filter { it.isNotEmpty() }.orEmpty()

    companion object {
        private const val KEY_RECENTS = "recents"
        private const val KEY_PINNED = "pinned"

        @Volatile private var instance: EmojiStore? = null

        fun get(context: Context): EmojiStore = instance ?: synchronized(this) {
            instance ?: EmojiStore(
                context.applicationContext.getSharedPreferences("r7t_emoji", Context.MODE_PRIVATE),
            ).also { instance = it }
        }
    }
}
