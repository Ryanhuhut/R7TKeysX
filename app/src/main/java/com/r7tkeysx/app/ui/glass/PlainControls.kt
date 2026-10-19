package com.r7tkeysx.app.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.r7tkeysx.app.ui.theme.IosColors
import kotlin.math.roundToInt

// Bản PHẲNG của công tắc và thanh trượt, dùng khi tắt Liquid Glass: không kính, không lò xo co giãn,
// chỉ một hoạt ảnh trượt núm đơn giản — nhẹ nhất cho máy, cuộn trang mượt nhất.

private val PlainTrackOff = Color(0xFF787880).copy(alpha = 0.36f)
private val PlainTick = Color(0xFF5A5A60)

@Composable
internal fun PlainToggle(selected: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentEnabled by rememberUpdatedState(enabled)
    val trackW = 68.dp
    val trackH = 32.dp
    val thumbW = 44.dp
    val pad = 2.dp
    val travelPx = with(LocalDensity.current) { (trackW - thumbW - pad * 2).toPx() }
    val padPx = with(LocalDensity.current) { pad.toPx() }
    val fraction by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 900f), label = "toggle")
    // cả vùng 48dp là chỗ chạm: chạm đâu cũng bật/tắt
    Box(
        modifier
            .size(trackW, 48.dp)
            .semantics { role = Role.Switch }
            .pointerInput(Unit) { detectTapGestures { if (currentEnabled) currentOnSelect(!currentSelected) } },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.size(trackW, trackH).clip(CircleShape).drawBehind { drawRect(lerp(PlainTrackOff, IosColors.StatusGreen, fraction)) })
        Box(
            Modifier
                .graphicsLayer { translationX = padPx + travelPx * fraction }
                .size(thumbW, 28.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
internal fun PlainSlider(
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ticks: Int = 0,
) {
    val currentOnChange by rememberUpdatedState(onChange)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentMin by rememberUpdatedState(min)
    val currentMax by rememberUpdatedState(max)
    BoxWithConstraints(modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
        val trackWidth = constraints.maxWidth
        val thumbPx = with(LocalDensity.current) { 46.dp.toPx() }
        val progress = if (max > min) ((value - min).toFloat() / (max - min)).coerceIn(0f, 1f) else 0f
        // x trên màn hình -> giá trị (tâm núm đi theo ngón tay)
        fun valueAt(x: Float): Int {
            val f = ((x - thumbPx / 2f) / (trackWidth - thumbPx)).coerceIn(0f, 1f)
            return (currentMin + (currentMax - currentMin) * f).roundToInt()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .drawBehind {
                    if (ticks > 1) {
                        val r = 1.8.dp.toPx()
                        val span = size.width - thumbPx
                        for (i in 0 until ticks) {
                            drawCircle(PlainTick, r, Offset(thumbPx / 2f + span * i / (ticks - 1), size.height / 2f + 17.dp.toPx()))
                        }
                    }
                }
                .pointerInput(trackWidth) { detectTapGestures { if (currentEnabled) currentOnChange(valueAt(it.x)) } }
                .pointerInput(trackWidth) {
                    detectHorizontalDragGestures(onDragStart = { if (currentEnabled) currentOnChange(valueAt(it.x)) }) { change, _ ->
                        if (currentEnabled) currentOnChange(valueAt(change.position.x))
                        change.consume()
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.clip(CircleShape).background(PlainTrackOff).height(8.dp).fillMaxWidth())
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(IosColors.Accent)
                    .height(8.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = ((constraints.maxWidth - thumbPx) * progress + thumbPx / 2f).roundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    },
            )
            Box(
                Modifier
                    .graphicsLayer { translationX = (trackWidth - thumbPx) * progress }
                    .size(46.dp, 28.dp)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
    }
}
