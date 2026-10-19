package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import android.os.SystemClock
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.r7tkeysx.app.engine.GlideTyper
import kotlin.math.abs

// Tâm các phím chữ (toạ độ gốc của cửa sổ), do LetterKey tự ghi vào khi được đặt lên màn
val LocalKeyCenters = compositionLocalOf<MutableMap<Char, Offset>?> { null }

// Phím chữ ghi lại tâm của mình (chỉ chữ a-z)
fun Modifier.reportKeyCenter(label: String): Modifier = composed {
    val centers = LocalKeyCenters.current
    if (centers == null || label.length != 1 || label[0].lowercaseChar() !in 'a'..'z') return@composed this
    val c = label[0].lowercaseChar()
    onGloballyPositioned { coords ->
        val p = coords.positionInRoot()
        centers[c] = Offset(p.x + coords.size.width / 2f, p.y + coords.size.height / 2f)
    }
}

// Vuốt để gõ: đặt lên vùng các hàng phím chữ. Chạm rồi kéo nhanh qua các phím (trước khi giữ lâu) thì thành vuốt:
// vệt trắng mờ chạy theo ngón tay (đầu đậm, đuôi mảnh rồi tan dần), các phím bên dưới không gõ ra chữ.
// Trong lúc vuốt, onLive báo từ đoán tới đâu (để hiện lên thanh gợi ý); thả tay thì onWord nhận các từ đoán được.
// Chỉ bắt đầu khi ngón tay chạm trúng một phím chữ, nên phím cách / xóa / shift vẫn hoạt động bình thường.
fun Modifier.glideTyping(
    enabled: Boolean,
    longPressMs: Long,
    onLive: (String?) -> Unit,
    onWord: (List<String>) -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this
    val centers = LocalKeyCenters.current ?: return@composed this
    val currentOnWord by rememberUpdatedState(onWord)
    val currentOnLive by rememberUpdatedState(onLive)
    // các điểm của vệt + thời điểm (ms) ngón tay đi qua, để phần cũ tan dần
    val trail = remember { mutableStateListOf<TrailPoint>() }
    var now by remember { mutableLongStateOf(0L) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // còn vệt trên màn thì cập nhật đồng hồ mỗi khung hình để đuôi tan mượt (kể cả khi ngón tay đứng yên / đã thả)
    LaunchedEffect(trail.isNotEmpty()) {
        while (trail.isNotEmpty()) {
            withFrameMillis { }
            now = SystemClock.uptimeMillis()
            trail.removeAll { now - it.time > TRAIL_LIFE_MS }
        }
    }
    this
        .onGloballyPositioned { coords = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val origin = coords?.positionInRoot() ?: return@awaitEachGesture
                val keySize = run {
                    // bề ngang một phím ~ khoảng cách giữa 2 phím liền nhau trên cùng hàng
                    val q = centers['q']; val w = centers['w']
                    if (q != null && w != null) abs(w.x - q.x) else 40.dp.toPx()
                }
                val start = down.position + origin
                val nearest = centers.values.minOfOrNull { (it - start).getDistance() } ?: return@awaitEachGesture
                if (nearest > keySize * 0.75f) return@awaitEachGesture // không chạm trúng phím chữ
                val pts = arrayListOf(down.position)
                var gliding = false
                var lastLive = 0L
                fun guess(): List<String> = GlideTyper.english()?.recognize(
                    pts.map { GlideTyper.Point(it.x + origin.x, it.y + origin.y) },
                    centers.mapValues { GlideTyper.Point(it.value.x, it.value.y) },
                    keySize,
                ).orEmpty()
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    pts += change.position
                    if (!gliding) {
                        if (!change.pressed) break
                        val moved = (change.position - down.position).getDistance()
                        if (change.uptimeMillis - down.uptimeMillis >= longPressMs) break // giữ lâu: để phím mở hàng chữ có dấu
                        if (moved > keySize * 0.8f) {
                            gliding = true
                            pts.forEach { trail += TrailPoint(it, SystemClock.uptimeMillis()) }
                        } else continue
                    }
                    change.consume() // phím bên dưới coi như bị huỷ, không gõ chữ
                    trail += TrailPoint(change.position, SystemClock.uptimeMillis())
                    now = SystemClock.uptimeMillis()
                    // đoán từ tới đâu báo tới đó (không quá ~10 lần/giây cho nhẹ máy)
                    if (change.pressed && change.uptimeMillis - lastLive > LIVE_EVERY_MS) {
                        lastLive = change.uptimeMillis
                        currentOnLive(guess().firstOrNull())
                    }
                    if (!change.pressed) break
                }
                if (gliding) {
                    val words = guess()
                    currentOnLive(null)
                    if (words.isNotEmpty()) currentOnWord(words)
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (trail.size < 2) return@drawWithContent
            // vẽ từng đoạn: càng mới càng dày và rõ, càng cũ càng mảnh và trong (đuôi sao chổi)
            val maxW = 9.dp.toPx()
            for (i in 1 until trail.size) {
                val p0 = trail[i - 1]
                val p1 = trail[i]
                val life = 1f - ((now - p1.time).toFloat() / TRAIL_LIFE_MS).coerceIn(0f, 1f)
                if (life <= 0f) continue
                drawLine(
                    TrailColor.copy(alpha = 0.55f * life),
                    p0.pos, p1.pos,
                    strokeWidth = maxW * (0.15f + 0.85f * life),
                    cap = StrokeCap.Round,
                )
            }
        }
}

private class TrailPoint(val pos: Offset, val time: Long)

private val TrailColor = androidx.compose.ui.graphics.Color(0xFFE5E5EA) // trắng xám, như vệt vuốt của iPhone
private const val TRAIL_LIFE_MS = 320L // phần vệt cũ hơn chừng này thì tan hẳn
private const val LIVE_EVERY_MS = 90L
