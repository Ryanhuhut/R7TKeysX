package com.r7tkeysx.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.IntState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.r7tkeysx.app.ui.theme.IosColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

// Nội dung phía sau mà kính sẽ "nhìn xuyên" (màn cài đặt đang cuộn). Không có (vd. trong bàn phím hệ thống) thì kính tự lùi về nền đặc.
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }

// Công tắc Liquid Glass ở trang Themes. Tắt = mọi nút, công tắc, thanh trượt vẽ phẳng (không kính, không hiệu ứng phồng) cho mượt nhất.
val LocalGlassEnabled = compositionLocalOf { true }

// Chiều cao (px) phần dưới màn bị bàn phím che; màn cài đặt chừa thêm chỗ để cuộn tới cuối.
// Là state chứ không phải giá trị: chỉ bước đo kích thước (layout) đọc nó, nên bàn phím đang trượt cũng không làm cả màn vẽ lại.
val LocalKeyboardInset = compositionLocalOf<IntState> { mutableIntStateOf(0) }

// Khoảng trống cuối trang = base + chiều cao bàn phím
@Composable
fun BottomInsetSpacer(base: Dp) {
    val inset = LocalKeyboardInset.current
    Spacer(
        Modifier.layout { _, constraints ->
            val height = base.roundToPx() + inset.intValue
            layout(constraints.maxWidth.coerceAtLeast(0), height) {}
        },
    )
}

val GlassSurface = Color.White.copy(alpha = 0.10f)

// Đánh dấu nội dung cuộn là "nguồn" để kính phía trên lấy làm nền (gồm cả nền đen, nên đặt NGOÀI vùng cuộn)
@Composable
fun Modifier.glassSource(background: Color): Modifier {
    val backdrop = LocalGlassBackdrop.current
    return (if (backdrop != null) layerBackdrop(backdrop as com.kyant.backdrop.backdrops.LayerBackdrop) else this).background(background)
}

@Composable
fun rememberGlassSource() = rememberLayerBackdrop()

// Kính lỏng: làm mờ + khúc xạ nội dung phía sau, viền sáng, bóng nhẹ. `interactive` thêm hiệu ứng nhấn như iOS.
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    surface: Color = GlassSurface,
    interactive: InteractiveHighlight? = null,
    blurRadius: Dp = 2.dp,
    lensHeight: Dp = 12.dp,
    lensAmount: Dp = 24.dp,
): Modifier {
    // tắt Liquid Glass của màn cài đặt thì vẽ nền đặc (nền kính vẫn có thể đang bật cho bàn phím)
    val backdrop = LocalGlassBackdrop.current?.takeIf { LocalGlassEnabled.current }
        ?: return this.clip(shape).background(surface.compositeOverDark())
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                lens(lensHeight.toPx(), lensAmount.toPx())
            },
            layerBlock = if (interactive != null) {
                {
                    val width = size.width
                    val height = size.height
                    val progress = interactive.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
                    val maxOffset = size.minDimension
                    val initialDerivative = 0.05f
                    val offset = interactive.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                    val maxDragScale = 4f.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
                    scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
                }
            } else null,
            onDrawSurface = { drawRect(surface) },
        )
        .then(interactive?.modifier ?: Modifier)
}

private fun Color.compositeOverDark(): Color =
    this.compositeOver(IosColors.SignInPill)

// Nút kính: chạm là phồng lên, vệt sáng đi theo ngón tay (không gợn sóng Material)
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    surface: Color = GlassSurface,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!LocalGlassEnabled.current) {
        // bản phẳng: nền đặc, chạm thì hơi mờ đi
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        Box(
            modifier
                .alpha(if (pressed) 0.6f else 1f)
                .clip(shape)
                .background(surface.compositeOverDark())
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
        return
    }
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(scope) }
    Box(
        modifier
            .liquidGlass(shape, surface, highlight)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .then(highlight.gestureModifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
