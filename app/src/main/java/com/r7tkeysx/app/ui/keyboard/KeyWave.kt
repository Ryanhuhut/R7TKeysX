package com.r7tkeysx.app.ui.keyboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.hypot

// Hiệu ứng "làn sóng" khi bàn phím đổi tầng (chữ <-> số <-> ký hiệu): mỗi phím hiện ra lần lượt, lan từ phím 123 / ABC ở góc dưới trái
// ra khắp bàn phím. Cái nào xa hơn thì hiện muộn hơn.
// origin: gốc của làn sóng (toạ độ gốc cửa sổ), do bàn phím báo lên.
class KeyWave(val origin: State<Offset?>)

// null = không có hiệu ứng (phím hiện ngay): lần dựng đầu tiên, bản ngang, công tắc tắt, tầng emoji...
val LocalKeyWave = compositionLocalOf<KeyWave?> { null }

private const val WAVE_PX_PER_MS = 3.4f // tốc độ lan: ~330ms từ góc này sang góc kia của bàn phím

// Gắn vào mỗi phím. Không có làn sóng thì không làm gì (alpha 1, kích thước nguyên).
@Composable
fun Modifier.keyWave(): Modifier {
    val wave = LocalKeyWave.current
    val progress = remember { Animatable(if (wave == null) 1f else 0f) }
    var center by remember { mutableStateOf<Offset?>(null) }
    val origin = wave?.origin?.value
    LaunchedEffect(wave != null, center != null, origin != null) {
        if (wave == null) return@LaunchedEffect
        val c = center ?: return@LaunchedEffect
        val o = origin ?: return@LaunchedEffect
        delay((hypot(c.x - o.x, c.y - o.y) / WAVE_PX_PER_MS).toLong())
        // phím bật lên bằng lò xo (hơi vọt), đồng thời rõ dần
        progress.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 520f))
    }
    return this
        .onGloballyPositioned { if (wave != null && center == null) center = it.boundsInRoot().center }
        .graphicsLayer {
            val p = progress.value
            if (p >= 1f) return@graphicsLayer
            // không dùng lớp ngoài màn hình để bóng dưới chân phím không bị cắt
            compositingStrategy = CompositingStrategy.ModulateAlpha
            alpha = (p * 2.2f).coerceIn(0f, 1f)
            val s = 0.55f + 0.45f * p
            scaleX = s; scaleY = s
            translationY = (1f - p) * 14.dp.toPx()
            transformOrigin = TransformOrigin.Center
        }
}
