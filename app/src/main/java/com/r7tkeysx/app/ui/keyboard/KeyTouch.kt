package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TouchBoundsExpansion
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// Khe hở giữa các phím (ngang, dọc). Mỗi phím nhận chạm rộng thêm NỬA khe ra mỗi phía,
// nên chạm vào khe hở vẫn trúng phím gần nhất (nhìn thì vẫn có khe, bấm thì không có chỗ chết).
val LocalKeyGap = compositionLocalOf { DpSize(0.dp, 0.dp) }

// Giống pointerInput(Unit) nhưng vùng nhận chạm nới ra `expand` (mỗi phía) so với hình phím
fun Modifier.keyPointerInput(expand: DpSize, block: suspend PointerInputScope.() -> Unit): Modifier =
    this then KeyTouchElement(expand, block)

private class KeyTouchElement(val expand: DpSize, val block: suspend PointerInputScope.() -> Unit) : ModifierNodeElement<KeyTouchNode>() {
    override fun create() = KeyTouchNode(expand, block)
    override fun update(node: KeyTouchNode) { node.expand = expand } // như pointerInput(Unit): không khởi động lại cử chỉ
    override fun equals(other: Any?) = other is KeyTouchElement && other.expand == expand
    override fun hashCode() = expand.hashCode()
    override fun InspectorInfo.inspectableProperties() { name = "keyPointerInput" }
}

private class KeyTouchNode(var expand: DpSize, block: suspend PointerInputScope.() -> Unit) : DelegatingNode(), PointerInputModifierNode {
    private val input = delegate(SuspendingPointerInputModifierNode(PointerInputEventHandler { block() }))

    override val touchBoundsExpansion: TouchBoundsExpansion
        get() = with(requireDensity()) {
            val x = (expand.width / 2).roundToPx()
            val y = (expand.height / 2).roundToPx()
            TouchBoundsExpansion(x, y, x, y)
        }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) =
        input.onPointerEvent(pointerEvent, pass, bounds)

    override fun onCancelPointerInput() = input.onCancelPointerInput()
}

// Như waitForUpOrCancellation nhưng KHÔNG huỷ khi ngón tay nằm ngoài hình phím (chạm ở khe hở vẫn tính),
// chỉ huỷ khi cử chỉ bị nơi khác "giành" (vd. vuốt để gõ)
suspend fun AwaitPointerEventScope.waitForKeyUp(): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.all { it.changedToUp() }) return event.changes[0]
        if (event.changes.any { it.isConsumed }) return null
        val final = awaitPointerEvent(PointerEventPass.Final)
        if (final.changes.any { it.isConsumed }) return null
    }
}

// ---- Chạm vào khe hở: chuyển cho phím gần nhất ----

// Mỗi phím ghi lại khung của mình (toạ độ gốc cửa sổ) + cách "nhấn" / "gõ" nó
class KeyRegistry {
    class Entry(val rect: androidx.compose.ui.geometry.Rect, val press: (Boolean) -> Unit, val tap: () -> Unit)
    val entries = mutableMapOf<Any, Entry>()
}

val LocalKeyRegistry = compositionLocalOf<KeyRegistry?> { null }

// Đặt lên vùng chứa các phím: cú chạm nào không trúng phím nào (rơi vào khe) thì tính cho phím gần nhất.
// Chạy SAU các phím (lượt Main đi từ phím lên vùng chứa), nên chạm trúng phím thì phím tự xử lý như thường.
fun Modifier.gapTapCatcher(registry: KeyRegistry, maxDistance: androidx.compose.ui.unit.Dp): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val gap = LocalKeyGap.current
    val halfX = with(LocalDensity.current) { (gap.width / 2).toPx() }
    val halfY = with(LocalDensity.current) { (gap.height / 2).toPx() }
    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(registry) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val p = down.position + origin
                val list = registry.entries.values.toList()
                // Vùng nhận chạm của phím đã nới nửa khe mỗi phía: chạm trong vùng đó phím tự lo rồi,
                // nếu bộ này gõ thêm lần nữa thì 1 lần chạm ra 2 chữ
                if (list.any { it.rect.inflateXY(halfX, halfY).contains(p) }) return@awaitEachGesture
                val limit = maxDistance.toPx()
                val nearest = list.minByOrNull { distanceToRect(p, it.rect) } ?: return@awaitEachGesture
                if (distanceToRect(p, nearest.rect) > limit) return@awaitEachGesture
                nearest.press(true)
                val up = waitForKeyUp()
                nearest.press(false)
                if (up != null) nearest.tap()
            }
        }
}

private fun androidx.compose.ui.geometry.Rect.inflateXY(dx: Float, dy: Float) =
    androidx.compose.ui.geometry.Rect(left - dx, top - dy, right + dx, bottom + dy)

private fun distanceToRect(p: androidx.compose.ui.geometry.Offset, r: androidx.compose.ui.geometry.Rect): Float {
    val dx = maxOf(r.left - p.x, 0f, p.x - r.right)
    val dy = maxOf(r.top - p.y, 0f, p.y - r.bottom)
    return kotlin.math.hypot(dx, dy)
}
