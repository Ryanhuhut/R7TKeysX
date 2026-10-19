package com.r7tkeysx.app.engine

import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

// Vuốt để gõ (tiếng Anh): ngón tay vẽ một đường qua các phím, tìm từ có "đường lý tưởng" (nối tâm các phím của từ)
// giống đường đó nhất. Cách làm theo ý tưởng SHARK2: so vị trí sau khi chia cả hai đường thành cùng số điểm,
// cộng thêm phạt nếu đường ngón tay không đi gần một chữ nào đó, và ưu tiên từ thông dụng.
class GlideTyper(words: List<String>) {
    // chỉ giữ từ a-z (bỏ từ có dấu nháy) kèm thứ hạng thông dụng
    private val entries: List<Pair<String, Int>> = words.mapIndexedNotNull { i, w ->
        val lw = w.lowercase()
        if (lw.length >= 2 && lw.all { it in 'a'..'z' }) lw to i else null
    }
    private val byFirstLast: Map<String, List<Pair<String, Int>>> = entries.groupBy { "" + it.first.first() + it.first.last() }

    class Point(val x: Float, val y: Float)

    // points: đường ngón tay; centers: tâm từng phím chữ (cùng hệ toạ độ); keySize: bề ngang một phím (px)
    fun recognize(points: List<Point>, centers: Map<Char, Point>, keySize: Float, max: Int = 3): List<String> {
        if (points.size < 2 || centers.isEmpty()) return emptyList()
        val starts = nearestKeys(points.first(), centers, keySize)
        val ends = nearestKeys(points.last(), centers, keySize)
        val user = resample(points, SAMPLES)
        val scored = ArrayList<Pair<String, Float>>()
        for (s in starts) for (e in ends) {
            for ((word, rank) in byFirstLast["" + s + e].orEmpty()) {
                val ideal = idealPath(word, centers) ?: continue
                val loc = locationDistance(user, resample(ideal, SAMPLES)) / keySize
                if (loc > 1.6f) continue
                val miss = missPenalty(word, points, centers, keySize)
                if (miss > 2f) continue
                val freq = ln(rank + 2f) / ln(entries.size + 2f) // 0 = rất thông dụng, 1 = hiếm
                scored += word to (loc + miss * 0.8f + freq * 0.45f)
            }
        }
        return scored.sortedBy { it.second }.map { it.first }.distinct().take(max)
    }

    // các phím gần điểm đầu / cuối (phím gần nhất + phím sát bên nếu ngón tay rơi giữa hai phím)
    private fun nearestKeys(p: Point, centers: Map<Char, Point>, keySize: Float): List<Char> {
        val sorted = centers.entries.sortedBy { hypot(it.value.x - p.x, it.value.y - p.y) }
        val best = hypot(sorted[0].value.x - p.x, sorted[0].value.y - p.y)
        return sorted.take(3).filter { hypot(it.value.x - p.x, it.value.y - p.y) <= max(best * 1.6f, keySize * 0.6f) }.map { it.key }
    }

    // đường lý tưởng: tâm các phím của từ, bỏ chữ lặp liền nhau ("hello" -> h e l o)
    private fun idealPath(word: String, centers: Map<Char, Point>): List<Point>? {
        val out = ArrayList<Point>(word.length)
        var last = ' '
        for (c in word) {
            if (c == last) continue
            out += centers[c] ?: return null
            last = c
        }
        return if (out.size == 1) listOf(out[0], out[0]) else out
    }

    // mỗi chữ của từ phải có ít nhất một điểm của đường ngón tay đi ngang gần phím đó
    private fun missPenalty(word: String, points: List<Point>, centers: Map<Char, Point>, keySize: Float): Float {
        var total = 0f
        for (c in word.toSet()) {
            val k = centers[c] ?: return 99f
            var d = Float.MAX_VALUE
            for (p in points) d = min(d, hypot(p.x - k.x, p.y - k.y))
            total += max(0f, d / keySize - 0.55f)
        }
        return total
    }

    private fun locationDistance(a: List<Point>, b: List<Point>): Float {
        var sum = 0f
        for (i in a.indices) sum += hypot(a[i].x - b[i].x, a[i].y - b[i].y)
        return sum / a.size
    }

    // chia một đường thành n điểm cách đều nhau
    private fun resample(path: List<Point>, n: Int): List<Point> {
        var length = 0f
        for (i in 1 until path.size) length += hypot(path[i].x - path[i - 1].x, path[i].y - path[i - 1].y)
        if (length < 1e-3f) return List(n) { path[0] }
        val step = length / (n - 1)
        val out = ArrayList<Point>(n)
        out += path[0]
        var acc = 0f
        var prev = path[0]
        var i = 1
        while (i < path.size && out.size < n) {
            val cur = path[i]
            val d = hypot(cur.x - prev.x, cur.y - prev.y)
            if (acc + d >= step && d > 0f) {
                val t = (step - acc) / d
                val q = Point(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y))
                out += q
                prev = q
                acc = 0f
            } else {
                acc += d
                prev = cur
                i++
            }
        }
        while (out.size < n) out += path.last()
        return out
    }

    companion object {
        private const val SAMPLES = 40

        @Volatile private var english: GlideTyper? = null

        // dùng chung danh sách từ tiếng Anh của bộ gợi ý chữ (nạp sẵn ở WordSuggester.preload)
        fun english(): GlideTyper? = english ?: WordSuggester.englishWords()?.let { GlideTyper(it) }?.also { english = it }
    }
}
