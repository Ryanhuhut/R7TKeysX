package com.r7tkeysx.app.engine

import java.text.Normalizer

// Bộ gõ Telex: nhận "từ đang soạn" + phím vừa bấm, trả về từ mới.
// Ví dụ: type("viê", 't') = "viêt";  type("viêt", 'j') = "việt".
// Không dính gì tới Android nên chạy và kiểm thử được trên máy tính.
object TelexEngine {
    // Dấu thanh (gắn sau chữ cái dưới dạng ký tự kết hợp Unicode)
    private const val ACUTE = '́'  // sắc  (s)
    private const val GRAVE = '̀'  // huyền (f)
    private const val HOOK = '̉'   // hỏi  (r)
    private const val TILDE = '̃'  // ngã  (x)
    private const val DOT = '̣'    // nặng (j)

    // Dấu mũ / trăng / râu
    private const val CIRC = '̂'   // â ê ô
    private const val BREVE = '̆'  // ă
    private const val HORN = '̛'   // ơ ư

    private val toneKeys = mapOf('s' to ACUTE, 'f' to GRAVE, 'r' to HOOK, 'x' to TILDE, 'j' to DOT)
    private val shapeMarks = setOf(CIRC, BREVE, HORN)
    private val toneMarks = setOf(ACUTE, GRAVE, HOOK, TILDE, DOT)

    // Một chữ cái = chữ gốc + dấu mũ/râu (nếu có) + dấu thanh (nếu có)
    private class Letter(var base: Char, var shape: Char? = null, var tone: Char? = null) {
        val lower get() = base.lowercaseChar()
        val isVowel get() = lower in "aeiouy"
    }

    // oldToneStyle = true: đặt dấu kiểu cũ (hòa, khỏe, thủy); false: kiểu mới (hoà, khoẻ, thuỷ)
    fun type(word: String, key: Char, oldToneStyle: Boolean = false): String {
        val w = parse(word)
        val k = key.lowercaseChar()
        // Từ đã bị "hoàn tác" (gõ đúp để bỏ dấu: cós + s = cos) thì không còn là vần tiếng Việt nữa:
        // các phím sau gõ thẳng chữ, không đặt dấu lại (cos + s = coss, + s = cosss... thay vì quay vòng cós/cos)
        if (isEscaped(w)) {
            w.addAll(parse(key.toString()))
            return render(w)
        }
        val handled = when {
            k in toneKeys -> applyTone(w, toneKeys.getValue(k), key, oldToneStyle)
            k == 'z' -> removeTone(w)
            k == 'w' -> applyW(w, key)
            k == 'a' || k == 'e' || k == 'o' -> applyCircumflex(w, k, key)
            k == 'd' -> applyStroke(w, key)
            else -> false
        }
        if (!handled) w.addAll(parse(key.toString())) // chữ có dấu sẵn (á, â, đ... từ phím giữ) cũng được tách dấu để gõ tiếp
        replaceTone(w, oldToneStyle)
        return render(w)
    }

    // ---- các luật ----

    // Sau nguyên âm đầu tiên chỉ có thể là phụ âm cuối c, ch, m, n, ng, nh, p, t. Gặp chữ khác (s, f, r, x, j, w, z, d...)
    // tức là người dùng đã hoàn tác dấu và đang gõ chữ thường. "aa", "ee" liền nhau (sau khi bỏ dấu mũ) và "dd" ở đầu cũng vậy.
    private fun isEscaped(w: List<Letter>): Boolean {
        if (w.size >= 2 && w[0].lower == 'd' && w[1].lower == 'd') return true
        val firstVowel = w.indexOfFirst { it.isVowel }
        if (firstVowel < 0) return false
        for (i in firstVowel + 1 until w.size) {
            val l = w[i]
            if (l.isVowel) {
                val p = w[i - 1]
                if (p.isVowel && p.lower == l.lower && l.lower in "ae" && p.shape == null && l.shape == null) return true
                continue
            }
            if (l.lower !in "chmngpt") return true
        }
        return false
    }

    private fun applyTone(w: MutableList<Letter>, tone: Char, key: Char, old: Boolean): Boolean {
        val run = toneRun(w)
        if (run.isEmpty()) return false
        val current = w.firstOrNull { it.tone != null }?.tone
        w.forEach { it.tone = null }
        if (current == tone) {      // bấm lại cùng dấu => bỏ dấu, in chữ phím đó
            w.add(Letter(key))
        } else {
            w[toneIndex(w, run, old)].tone = tone
        }
        return true
    }

    private fun removeTone(w: MutableList<Letter>): Boolean {
        if (w.none { it.tone != null }) return false
        w.forEach { it.tone = null }
        return true
    }

    // w: a->ă, o->ơ, u->ư, "uo"->"ươ"; không có nguyên âm nào thì w = ư
    private fun applyW(w: MutableList<Letter>, key: Char): Boolean {
        val upper = key.isUpperCase()
        for (i in 0 until w.size - 1) {
            if (w[i].lower == 'u' && w[i + 1].lower == 'o' && !(i > 0 && w[i - 1].lower == 'q')) {
                // đã là "ươ" rồi mà bấm w nữa (gõ kiểu uwow): bỏ qua, giữ nguyên "ươ"
                w[i].shape = HORN; w[i + 1].shape = HORN
                return true
            }
        }
        val target = w.indexOfLast { it.lower == 'a' || it.lower == 'o' || it.lower == 'u' }
        if (target < 0) {
            if (w.any { it.isVowel }) return false
            w.add(Letter(if (upper) 'U' else 'u', HORN))
            return true
        }
        val mark = if (w[target].lower == 'a') BREVE else HORN
        if (w[target].shape == mark) {
            w[target].shape = null; w.add(Letter(key))
        } else {
            w[target].shape = mark
        }
        return true
    }

    // aa->â, ee->ê, oo->ô; bấm lần 3 thì hoàn tác
    private fun applyCircumflex(w: MutableList<Letter>, k: Char, key: Char): Boolean {
        // "ư" + o => "ươ" (gõ b-u-w-o-i-r ra "bưởi"; "ưo" không phải vần tiếng Việt)
        val last = w.lastOrNull()
        if (k == 'o' && last != null && last.lower == 'u' && last.shape == HORN) {
            w.add(Letter(key, HORN))
            return true
        }
        val target = w.indexOfLast { it.lower == k }
        if (target < 0) return false
        if (w[target].shape == CIRC) {
            w[target].shape = null; w.add(Letter(key))
        } else {
            w[target].shape = CIRC
        }
        return true
    }

    // dd -> đ; bấm lần 3 thì hoàn tác
    private fun applyStroke(w: MutableList<Letter>, key: Char): Boolean {
        val last = w.lastOrNull() ?: return false
        return when (last.lower) {
            'd' -> { last.base = if (last.base.isUpperCase()) 'Đ' else 'đ'; true }
            'đ' -> { last.base = if (last.base.isUpperCase()) 'D' else 'd'; w.add(Letter(key)); true }
            else -> false
        }
    }

    // ---- đặt dấu thanh đúng chỗ ----

    // Dấu thanh có thể đã đặt rồi mà sau đó từ dài thêm (hoà -> hoàn): đặt lại cho đúng vị trí
    private fun replaceTone(w: MutableList<Letter>, old: Boolean) {
        val tone = w.firstOrNull { it.tone != null }?.tone ?: return
        w.forEach { it.tone = null }
        val run = toneRun(w)
        if (run.isNotEmpty()) w[toneIndex(w, run, old)].tone = tone
    }

    // Cụm nguyên âm mang dấu (bỏ "u" trong "qu" và "i" trong "gi" vì chúng là phụ âm)
    private fun toneRun(w: List<Letter>): List<Int> {
        var start = -1
        for (i in w.indices) {
            if (!w[i].isVowel) continue
            if (i > 0 && w[i].lower == 'u' && w[i - 1].lower == 'q') continue
            if (i > 0 && w[i].lower == 'i' && w[i - 1].lower == 'g' && i + 1 < w.size && w[i + 1].isVowel) continue
            start = i
            break
        }
        if (start < 0) return emptyList()
        val run = mutableListOf<Int>()
        var i = start
        while (i < w.size && w[i].isVowel) { run.add(i); i++ }
        return run
    }

    private fun toneIndex(w: List<Letter>, run: List<Int>, old: Boolean): Int {
        val shaped = run.filter { w[it].shape != null }
        if (shaped.isNotEmpty()) return shaped.last()   // ê, ô, ơ... được ưu tiên
        if (run.size == 1) return run[0]
        if (run.size == 2) {
            val hasFinalConsonant = run[1] + 1 < w.size
            val pair = "" + w[run[0]].lower + w[run[1]].lower
            val modernPair = !old && pair in setOf("oa", "oe", "uy") // kiểu mới: dấu rơi vào chữ thứ hai
            return if (hasFinalConsonant || modernPair) run[1] else run[0]
        }
        return run[1]
    }

    // ---- đổi qua lại giữa chuỗi và danh sách chữ cái ----

    private fun parse(word: String): MutableList<Letter> {
        val out = mutableListOf<Letter>()
        for (ch in Normalizer.normalize(word, Normalizer.Form.NFD)) {
            when {
                ch in shapeMarks && out.isNotEmpty() -> out.last().shape = ch
                ch in toneMarks && out.isNotEmpty() -> out.last().tone = ch
                else -> out.add(Letter(ch))
            }
        }
        return out
    }

    private fun render(w: List<Letter>): String {
        val sb = StringBuilder()
        for (l in w) {
            sb.append(l.base)
            l.shape?.let { sb.append(it) }
            l.tone?.let { sb.append(it) }
        }
        return Normalizer.normalize(sb, Normalizer.Form.NFC)
    }
}
