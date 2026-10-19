package com.r7tkeysx.app.ui.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.r7tkeysx.app.data.KeyFontWeight
import com.r7tkeysx.app.data.LandscapeMode
import com.r7tkeysx.app.data.LocalKeyboardSettings
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.LocalKeyboardPalette
import kotlinx.coroutines.withTimeoutOrNull

// Chiều cao phím ép từ ngoài (bản ngang dùng phím thấp hơn); null = theo cài đặt
val LocalKeyHeightOverride = compositionLocalOf<Dp?> { null }

// Hệ số cỡ chữ trên phím (bàn phím nổi nhỏ lại thì chữ cũng nhỏ theo, không bị cắt)
val LocalKeyFontScale = compositionLocalOf { 1f }

// Cờ cho nơi xử lý phím xóa: true = lần xóa này là xóa nguyên một từ (chỉ bật khi giữ rất lâu)
object HoldDelete {
    @Volatile var byWord = false
}

// Hiệu ứng nhấn dùng chung cho MỌI phím: co lại ngay (lò xo nhanh), giữ lâu thì nhỏ dần, nhả tay thì bật về có chút nảy.
// Chạm cực nhanh vẫn co tới mức PRESS_SCALE rồi mới bật lại nên lúc nào cũng thấy phím "lún".
// Đặt SAU modifier bắt chạm của phím để vùng nhận chạm giữ nguyên kích thước gốc.
@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(pressed) {
        if (pressed) {
            scale.animateTo(IosDimens.PRESS_SCALE, androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 1500f))
            scale.animateTo(
                IosDimens.PRESS_SCALE_HELD,
                androidx.compose.animation.core.tween(IosDimens.PRESS_HOLD_MS, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            )
        } else {
            if (scale.value > IosDimens.PRESS_SCALE + 0.01f) scale.animateTo(IosDimens.PRESS_SCALE, androidx.compose.animation.core.tween(40))
            scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

// Phím cơ bản: tự vẽ nền + bóng, tự bắt chạm (không dùng clickable nên không có gợn sóng).
// Nhấn xuống: phím sáng + rung nhẹ + (nếu có bubbleLabel và bật Character Preview) hiện bong bóng.
// Nhấc tay trong phím: gọi onTap.
@Composable
fun KeyBase(
    color: Color,
    pressedColor: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    bubbleLabel: String? = null,
    repeatOnHold: Boolean = false,
    flat: Boolean = false, // phím "phẳng": không bóng (dùng cho phím chỉ có icon / chữ, không nền)
    sound: KeySound = KeySound.TAP,
    variants: List<String> = emptyList(), // giữ lâu: hiện hàng chữ thay thế (đ, ă, â, á...), rê để chọn, thả để gõ
    onVariant: (String) -> Unit = {},
    content: @Composable BoxScope.(pressed: Boolean) -> Unit,
) {
    val currentSound by rememberUpdatedState(sound)
    val currentVariants by rememberUpdatedState(variants)
    val currentOnVariant by rememberUpdatedState(onVariant)
    var variantSel by remember { mutableStateOf(-1) } // -1 = không hiện hàng chữ thay thế
    val variantLeft = remember { floatArrayOf(Float.NaN) }
    val settings = LocalKeyboardSettings.current
    val palette = LocalKeyboardPalette.current
    var pressed by remember { mutableStateOf(false) }
    val currentOnTap by rememberUpdatedState(onTap) // luôn gọi bản mới nhất, tránh đọc giá trị cũ
    val repeatEnabled by rememberUpdatedState(repeatOnHold && settings.holdDeleteRepeat.value)
    val view = LocalView.current
    val corner = settings.keyCornerRadius.value.dp
    val shape = RoundedCornerShape(corner)
    // ghi khung phím vào sổ chung để chạm vào khe hở bên cạnh cũng gõ được phím này
    val registry = LocalKeyRegistry.current
    val token = remember { Any() }
    androidx.compose.runtime.DisposableEffect(registry) { onDispose { registry?.entries?.remove(token) } }

    Box(
        modifier
            .keyWave()
            .onGloballyPositioned { c ->
                registry?.entries?.set(token, KeyRegistry.Entry(c.boundsInRoot(), { pressed = it }, { view.keyTick(settings, currentSound); currentOnTap() }))
            }
            .height(LocalKeyHeightOverride.current ?: (IosDimens.KeyHeight * settings.keyHeightPercent.value / 100f))
            // vùng nhận chạm (nới rộng ra khe hở) đặt TRƯỚC lệnh cắt bo góc, nếu không phần nới bị cắt mất
            .keyPointerInput(LocalKeyGap.current) {
                awaitEachGesture {
                  try {
                    awaitFirstDown()
                    pressed = true
                    view.keyTick(settings, currentSound)
                    var released = false
                    val choices = currentVariants
                    val wait = when {
                        repeatEnabled -> IosDimens.REPEAT_DELAY_MS
                        choices.isNotEmpty() -> settings.longPressMs.value.toLong()
                        else -> Long.MAX_VALUE
                    }
                    val first = withTimeoutOrNull(wait) {
                        waitForKeyUp().also { released = true }
                    }
                    if (released) {
                        pressed = false
                        if (first != null) currentOnTap()
                        return@awaitEachGesture
                    }
                    if (choices.isNotEmpty()) {
                        // giữ đủ lâu: hiện hàng chữ thay thế, ô dưới ngón tay sáng lên; thả tay = gõ ô đang sáng
                        variantLeft[0] = Float.NaN
                        variantSel = 0
                        view.longPressTick(settings)
                        val cellW = VariantCellWidth.toPx()
                        val pad = VariantPadding.toPx()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            val left = variantLeft[0]
                            if (!left.isNaN()) {
                                val idx = ((change.position.x - left - pad) / cellW).toInt().coerceIn(0, choices.lastIndex)
                                if (idx != variantSel) { variantSel = idx; view.selectionTick(settings) }
                            }
                            change.consume()
                            if (!change.pressed) break
                        }
                        val chosen = choices.getOrNull(variantSel)
                        variantSel = -1
                        pressed = false
                        chosen?.let { currentOnVariant(it) }
                        return@awaitEachGesture
                    }
                    // giữ lâu (chỉ phím xóa): xóa từng ký tự; giữ rất lâu mới xóa từng từ. Dừng ngay khi nhấc tay
                    val startMs = System.currentTimeMillis() - IosDimens.REPEAT_DELAY_MS
                    while (true) {
                        HoldDelete.byWord = System.currentTimeMillis() - startMs >= IosDimens.DELETE_WORD_AFTER_MS
                        currentOnTap()
                        view.keyTick(settings, currentSound) // mỗi lần xóa (1 ký tự hoặc 1 từ) rung 1 cái
                        var done = false
                        val interval = if (HoldDelete.byWord) IosDimens.DELETE_WORD_INTERVAL_MS else IosDimens.REPEAT_INTERVAL_MS
                        withTimeoutOrNull(interval) {
                            waitForKeyUp().also { done = true }
                        }
                        // sự kiện nhấc tay có thể rơi vào lúc đang xóa (không ai nghe) nên kiểm tra lại ngón tay còn đè không
                        if (done || currentEvent.changes.none { it.pressed }) break
                    }
                    HoldDelete.byWord = false
                    pressed = false
                  } finally {
                    // cú chạm bị hệ thống huỷ giữa chừng (vuốt cạnh màn hình, cử chỉ chụp màn hình...) cũng phải trả phím về trạng thái thường
                    pressed = false; variantSel = -1; HoldDelete.byWord = false
                  }
                }
            }
            // co cả phím (bóng + nền + chữ) quanh tâm; đặt SAU vùng nhận chạm nên chạm vẫn ăn theo kích thước gốc
            .pressScale(pressed)
            .drawBehind {
                if (flat) return@drawBehind
                // bóng mờ 1dp dưới chân phím
                drawRoundRect(
                    palette.shadow,
                    topLeft = Offset(0f, 1.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(corner.toPx()),
                )
            }
            .clip(shape)
            .background(if (pressed) pressedColor else color),
        contentAlignment = Alignment.Center,
    ) {
        content(pressed)
        if (variantSel >= 0) VariantPopup(variants, variantSel, variantLeft)
        else if (pressed && bubbleLabel != null && settings.characterPreview.value) KeyBubble(bubbleLabel)
    }
}

// Có dùng bố cục ngang không: máy đang nằm ngang VÀ cài đặt Landscape Mode cho phép
@Composable
fun isLandscapeLayout(): Boolean {
    val settings = LocalKeyboardSettings.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return landscape && settings.landscapeMode.value == LandscapeMode.DYNAMIC
}

// Phím chữ: sáng lên khi nhấn, có bong bóng phóng to chữ.
// hint = chữ gợi ý nhỏ ở góc trên phải (số / ký hiệu). Cỡ chữ và độ đậm theo cài đặt Fonts.
@Composable
fun LetterKey(label: String, onTap: () -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    val palette = LocalKeyboardPalette.current
    val settings = LocalKeyboardSettings.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scale = (if (landscape) settings.fontSizeLandscape.value else settings.fontSizePortrait.value) / 100f * LocalKeyFontScale.current
    val weight = when (settings.fontWeight.value) {
        KeyFontWeight.REGULAR -> FontWeight.Normal
        KeyFontWeight.MEDIUM -> FontWeight.Medium
        KeyFontWeight.BOLD -> FontWeight.Bold
    }
    val onChar = LocalOnChar.current
    KeyBase(
        color = palette.letterKey,
        pressedColor = palette.letterKeyPressed,
        onTap = onTap,
        modifier = modifier.reportKeyCenter(label),
        bubbleLabel = label,
        variants = if (onChar != null) remember(label) { keyVariants(label) } else emptyList(),
        onVariant = { onChar?.invoke(it) },
    ) {
        Text(label, color = palette.keyText, fontSize = 26.sp * scale, fontWeight = weight)
        hint?.let {
            Text(
                it, color = palette.secondaryText, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

// Phím chức năng (123, emoji, backspace, shift...)
@Composable
fun FunctionKey(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    pressedColor: Color? = null,
    repeatOnHold: Boolean = false,
    flat: Boolean = false,
    sound: KeySound = if (repeatOnHold) KeySound.DELETE else KeySound.TAP, // phím giữ-để-lặp chính là phím xóa
    content: @Composable BoxScope.(pressed: Boolean) -> Unit,
) {
    val palette = LocalKeyboardPalette.current
    KeyBase(
        color = color ?: if (flat) Color.Transparent else palette.functionKey,
        pressedColor = pressedColor ?: palette.functionKeyPressed,
        onTap = onTap,
        modifier = modifier,
        repeatOnHold = repeatOnHold,
        flat = flat,
        sound = sound,
        content = content,
    )
}

// Bong bóng phóng to chữ, nằm ngay phía trên phím, kẹp lại không cho tràn khỏi màn hình
@Composable
private fun KeyBubble(label: String) {
    val palette = LocalKeyboardPalette.current
    val gapPx = with(LocalDensity.current) { IosDimens.BubbleGap.roundToPx() }
    val positionProvider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = (anchorBounds.center.x - popupContentSize.width / 2)
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val y = anchorBounds.top - popupContentSize.height - gapPx
                return IntOffset(x, y)
            }
        }
    }
    Popup(popupPositionProvider = positionProvider) {
        Box(
            Modifier
                .size(IosDimens.BubbleWidth, IosDimens.BubbleHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.bubble),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = palette.bubbleText, fontSize = 34.sp, fontWeight = FontWeight.Normal)
        }
    }
}
