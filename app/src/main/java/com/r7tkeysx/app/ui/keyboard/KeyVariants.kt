package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.LocalKeyboardPalette

// Giữ một phím chữ / ký hiệu thì hiện hàng chữ "họ hàng" của nó (giữ d -> đ, giữ a -> ă â á à...).
// Chỉ chữ tiếng Việt; phím không có chữ có dấu (s, c, n...) thì giữ không hiện gì.
private val VARIANTS = mapOf(
    "a" to "ă â á à ả ã ạ",
    "e" to "ê é è ẻ ẽ ẹ",
    "i" to "í ì ỉ ĩ ị",
    "o" to "ô ơ ó ò ỏ õ ọ",
    "u" to "ư ú ù ủ ũ ụ",
    "y" to "ý ỳ ỷ ỹ ỵ",
    "d" to "đ",
    // ký hiệu
    "-" to "– — •",
    "/" to "\\",
    "$" to "₫ € £ ¥ ¢",
    "&" to "§",
    "." to "…",
    "?" to "¿",
    "!" to "¡",
    "'" to "‘ ’ `",
    "\"" to "“ ” « »",
    "%" to "‰",
    "=" to "≠ ≈",
    "*" to "×",
    "+" to "±",
    "<" to "≤",
    ">" to "≥",
)

// Danh sách chữ thay thế cho nhãn phím; chữ HOA thì trả về bản HOA (bỏ chữ không có bản hoa một ký tự như ß)
fun keyVariants(label: String): List<String> {
    // phím số: chính số đó, số nhỏ phía trên (²), số nhỏ phía dưới (₂), rồi các phân số
    DIGIT_VARIANTS[label]?.let { return listOf(label) + it.split(' ') }
    return keyVariantsNoDigit(label)
}

private val DIGIT_VARIANTS = mapOf(
    "1" to "¹ ₁ ½ ⅓ ¼ ⅕ ⅙ ⅛",
    "2" to "² ₂ ⅔ ⅖",
    "3" to "³ ₃ ¾ ⅗ ⅜",
    "4" to "⁴ ₄ ⅘",
    "5" to "⁵ ₅ ⅚ ⅝",
    "6" to "⁶ ₆",
    "7" to "⁷ ₇ ⅞",
    "8" to "⁸ ₈",
    "9" to "⁹ ₉",
    "0" to "⁰ ₀ ° ∅",
)

private fun keyVariantsNoDigit(label: String): List<String> {
    val list = VARIANTS[label.lowercase()]?.split(' ') ?: return emptyList()
    if (label.length == 1 && label[0].isUpperCase()) {
        return list.map { it.uppercase() }.filter { it.length == 1 }
    }
    return list
}

// Bàn phím truyền xuống: gõ một chữ (để phím chữ gõ được chữ thay thế mà không cần thêm tham số qua nhiều tầng)
val LocalOnChar = compositionLocalOf<((String) -> Unit)?> { null }

val VariantCellWidth = 40.dp
val VariantCellHeight = 52.dp
val VariantPadding = 5.dp

// Hàng chữ thay thế, đặt ngay trên phím, căn giữa theo phím nhưng không tràn khỏi màn.
// `leftOut[0]` nhận lại mép trái của hàng (px, so với mép trái phím) để bên bắt chạm biết ngón tay đang ở ô nào.
@Composable
fun VariantPopup(variants: List<String>, selected: Int, leftOut: FloatArray) {
    val palette = LocalKeyboardPalette.current
    val gapPx = with(LocalDensity.current) { IosDimens.BubbleGap.roundToPx() }
    val provider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = (anchorBounds.center.x - popupContentSize.width / 2)
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                leftOut[0] = (x - anchorBounds.left).toFloat()
                return IntOffset(x, anchorBounds.top - popupContentSize.height - gapPx)
            }
        }
    }
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(0.8f, 700f)) }
    val shape = RoundedCornerShape(12.dp)
    Popup(popupPositionProvider = provider) {
        Row(
            Modifier
                .graphicsLayer {
                    val t = appear.value
                    scaleX = 0.85f + 0.15f * t
                    scaleY = 0.85f + 0.15f * t
                    alpha = t.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .shadow(10.dp, shape)
                .clip(shape)
                .background(palette.bubble)
                .padding(VariantPadding),
        ) {
            variants.forEachIndexed { i, v ->
                val on = i == selected
                Box(
                    Modifier
                        .size(VariantCellWidth, VariantCellHeight)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) IosColors.Accent else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(v, color = if (on) Color.White else palette.bubbleText, fontSize = 26.sp)
                }
            }
        }
    }
}
