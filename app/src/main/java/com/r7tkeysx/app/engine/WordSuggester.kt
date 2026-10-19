package com.r7tkeysx.app.engine

import android.content.Context
import java.text.Normalizer

// Gợi ý chữ: tìm trong danh sách từ thông dụng (xếp từ hay dùng nhất xuống) những từ bắt đầu bằng phần đang gõ.
// Tiếng Việt gõ không dấu vẫn ra từ có dấu: "tie" -> tiếng, tiền, tiếp; đã gõ dấu thì chỉ lấy từ đúng dấu đó.
class WordSuggester(internal val words: List<String>) {
    private val keys = words.map { stripMarks(it.lowercase()) }
    // chỉ mục theo chữ cái đầu (không dấu) để mỗi lần gõ không phải dò cả danh sách
    private val byFirst: Map<Char, IntArray> = keys.indices.groupBy { keys[it].firstOrNull() ?: ' ' }
        .mapValues { (_, v) -> v.toIntArray() }

    fun suggest(typed: String, max: Int = 2): List<String> {
        if (typed.isEmpty()) return emptyList()
        val t = typed.lowercase()
        val plain = stripMarks(t)
        val out = ArrayList<String>(max)
        for (i in byFirst[plain[0]] ?: return emptyList()) {
            val w = words[i]
            if (w.length < t.length || !keys[i].startsWith(plain)) continue
            if (!marksCompatible(t, w.lowercase()) || w.equals(typed, ignoreCase = true)) continue
            out += matchCase(typed, w)
            if (out.size >= max) break
        }
        return out
    }

    private val wordSet: Set<String> by lazy { words.mapTo(HashSet()) { it.lowercase() } }

    // Sửa chính tả: từ gõ không có trong danh sách mà chỉ lệch đúng 1 chữ so với một từ thông dụng
    // (sai / thiếu / thừa 1 chữ, hoặc đảo 2 chữ liền nhau, chữ đầu giữ nguyên) thì trả về từ đúng. Không cần sửa thì trả null.
    fun correct(typed: String): String? {
        if (typed.length < 3 || !typed.all { it.isLetter() }) return null
        if (typed.length > 1 && typed.all { it.isUpperCase() }) return null // viết tắt: giữ nguyên
        val t = typed.lowercase()
        if (t in wordSet) return null
        for (i in byFirst[t[0]] ?: return null) {
            val k = keys[i]
            if (kotlin.math.abs(k.length - t.length) <= 1 && oneEditAway(t, k)) return matchCase(typed, words[i])
        }
        return null
    }

    companion object {
        // a và b khác nhau đúng 1 thao tác: đổi / thêm / bớt 1 chữ, hoặc đảo 2 chữ liền nhau
        private fun oneEditAway(a: String, b: String): Boolean {
            if (a == b) return false
            if (a.length == b.length) {
                val diff = a.indices.filter { a[it] != b[it] }
                return diff.size == 1 || (diff.size == 2 && diff[1] == diff[0] + 1 && a[diff[0]] == b[diff[1]] && a[diff[1]] == b[diff[0]])
            }
            val (long, short) = if (a.length > b.length) a to b else b to a
            var i = 0
            while (i < short.length && long[i] == short[i]) i++
            return long.substring(i + 1) == short.substring(i)
        }

        // Bỏ dấu tiếng Việt: "tiếng" -> "tieng", "đ" -> "d"
        fun stripMarks(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace('đ', 'd').replace('Đ', 'D')

        // Chữ đã gõ có dấu thì từ gợi ý phải có đúng chữ đó ở cùng chỗ; chữ gõ không dấu thì khớp mọi dấu
        private fun marksCompatible(typed: String, word: String): Boolean {
            for (i in typed.indices) {
                val c = typed[i]
                if (c == word[i]) continue
                if (stripMarks(c.toString()) == c.toString() && c != 'đ' && stripMarks(word[i].toString()) == c.toString()) continue
                return false
            }
            return true
        }

        // Viết hoa theo kiểu người dùng đang gõ: "Tie" -> "Tiếng", "TIE" -> "TIẾNG"
        private fun matchCase(typed: String, word: String): String = when {
            typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase()
            typed[0].isUpperCase() -> word.replaceFirstChar { it.uppercase() }
            else -> word
        }

        @Volatile private var english: WordSuggester? = null
        @Volatile private var vietnamese: WordSuggester? = null

        // Nạp 2 danh sách từ (assets/dict) ở luồng nền; chưa nạp xong thì chưa có gợi ý
        fun preload(context: Context) {
            if (english != null && vietnamese != null) return
            val app = context.applicationContext
            Thread {
                fun read(name: String) = app.assets.open("dict/$name").bufferedReader().readLines().filter { it.isNotBlank() }
                if (vietnamese == null) vietnamese = WordSuggester(read("vi.txt"))
                if (english == null) english = WordSuggester(read("en.txt"))
                GlideTyper.english() // dựng sẵn bộ vuốt để gõ, lần vuốt đầu không bị khựng
            }.start()
        }

        // danh sách từ tiếng Anh (cho vuốt để gõ); null nếu chưa nạp xong
        fun englishWords(): List<String>? = english?.words

        // Gợi ý theo ngôn ngữ bàn phím: "vi" dùng danh sách tiếng Việt, còn lại dùng tiếng Anh (Mỹ)
        fun forLanguage(languageId: String): WordSuggester? = if (languageId == "vi") vietnamese else english
    }
}
