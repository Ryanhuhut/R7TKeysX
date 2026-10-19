package com.r7tkeysx.app.ui.keyboard

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.r7tkeysx.app.ui.theme.IosDimens

private val GlassTop = Color(0xF2262628)
private val GlassBottom = Color(0xF5121214)

// Hình bo góc mà bán kính đổi theo tiến độ mở: lúc đầu là viên tròn (giọt nhỏ), mở xong là chữ nhật bo góc thường
private class MorphShape(val t: () -> Float, val finalRadius: Dp) : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val full = minOf(size.width, size.height) / 2f
        val r = lerp(full, with(density) { finalRadius.toPx() }.coerceAtMost(full), t().coerceIn(0f, 1f))
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
    }
}

// Popup kiểu kính tối nở ra từ chỗ bấm: một giọt nhỏ phồng lên thành menu (bo góc từ tròn sang chữ nhật, hơi vọt rồi lắng),
// chữ bên trong từ mờ nhòe rõ dần; đóng thì co ngược về đúng nút. visible = false thì chạy hiệu ứng đóng rồi mới gỡ popup.
// origin: điểm gốc của hiệu ứng trong popup (0..1), thường là góc gần nút bấm.
@Composable
fun MorphPopup(
    visible: Boolean,
    positionProvider: PopupPositionProvider,
    origin: TransformOrigin,
    width: Dp = IosDimens.PopupWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = remember { Animatable(0f) }
    var present by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            present = true
            t.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 380f))
        } else if (present) {
            t.animateTo(0f, tween(230, easing = FastOutSlowInEasing))
            present = false
        }
    }
    if (!present) return
    val density = LocalDensity.current
    val shape = remember { MorphShape({ t.value }, 32.dp) }
    Popup(popupPositionProvider = positionProvider) {
        Column(
            Modifier
                .graphicsLayer {
                    val p = t.value
                    val s = lerp(0.12f, 1f, p.coerceAtLeast(0f))
                    scaleX = s; scaleY = s
                    transformOrigin = origin
                    alpha = (p * 4f).coerceIn(0f, 1f)
                    this.shape = shape
                    clip = true
                    shadowElevation = 24.dp.toPx() * p.coerceIn(0f, 1f)
                    ambientShadowColor = Color.Black; spotShadowColor = Color.Black
                }
                .width(width)
                .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
                .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.07f), 0.45f to Color.Transparent))
                .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))), shape)
                .padding(IosDimens.PopupPadding),
        ) {
            // nội dung: mờ nhòe -> rõ nét, hiện dần khi giọt đã to ra một nửa
            Column(
                Modifier.graphicsLayer {
                    val c = ((t.value - 0.35f) / 0.55f).coerceIn(0f, 1f)
                    alpha = c
                    if (Build.VERSION.SDK_INT >= 31 && c < 1f) {
                        val b = (1f - c) * 18.dp.toPx()
                        if (b > 0.5f) renderEffect = BlurEffect(b, b, TileMode.Decal)
                    }
                },
                content = content,
            )
        }
    }
}
