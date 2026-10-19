package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.r7tkeysx.app.ui.theme.IosColors
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.r7tkeysx.app.model.KeyboardLanguage
import com.r7tkeysx.app.data.LocalKeyboardSettings
import com.r7tkeysx.app.ui.theme.LocalKeyboardPalette
import com.r7tkeysx.app.ui.theme.IosDimens
import kotlinx.coroutines.withTimeoutOrNull

private const val NONE = -1
private const val BTN_FLOAT = 1000  // nút trái ở đáy popup: bàn phím nổi
private const val BTN_ADJUST = 1001 // nút phải: chỉnh cỡ / vị trí bàn phím

// Quả địa cầu. Chạm nhanh: ngôn ngữ kế tiếp. Giữ ~300ms: hiện popup, trượt lên/xuống để chọn, thả tay để chốt.
// Cả hai cách dùng MỘT khối cử chỉ duy nhất (awaitEachGesture), không chồng 2 bộ bắt cử chỉ.
@Composable
fun GlobeKey(
    languages: List<KeyboardLanguage>,
    selectedIndex: Int,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    extras: List<String> = emptyList(), // mục thêm ở cuối popup (vd. Emoji, Clipboard), không phải ngôn ngữ
    onExtra: (Int) -> Unit = {},
    // hàng 2 nút ở đáy popup (bàn phím nổi | chỉnh cỡ): mặc định lấy từ KeyboardView qua LocalKeyboardModeActions
    floatingActive: Boolean = LocalKeyboardModeActions.current?.floating ?: false,
    onFloat: (() -> Unit)? = LocalKeyboardModeActions.current?.onFloat,
    onAdjust: (() -> Unit)? = LocalKeyboardModeActions.current?.onAdjust,
) {
    val currentOnFloat by rememberUpdatedState(onFloat)
    val currentOnAdjust by rememberUpdatedState(onAdjust)
    val hasButtons = onFloat != null && onAdjust != null
    val currentHasButtons by rememberUpdatedState(hasButtons)
    val currentOnExtra by rememberUpdatedState(onExtra)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentCount by rememberUpdatedState(languages.size + extras.size)
    val currentLanguageCount by rememberUpdatedState(languages.size)
    val view = LocalView.current
    val density = LocalDensity.current
    val settings = LocalKeyboardSettings.current
    val palette = LocalKeyboardPalette.current

    var down by remember { mutableStateOf(false) } // ngón tay đang đè (cho hiệu ứng co giãn)
    var popupVisible by remember { mutableStateOf(false) }
    var hovered by remember { mutableIntStateOf(NONE) } // mục đang sáng; NONE = ngón tay đang ở ngoài popup

    // kích thước popup đổi ra pixel một lần (không dùng số pixel cứng)
    val itemH = with(density) { IosDimens.PopupItemHeight.toPx() }
    val padV = with(density) { IosDimens.PopupPadding.toPx() }
    val popupW = with(density) { IosDimens.PopupWidth.toPx() }
    val gap = with(density) { IosDimens.BubbleGap.toPx() }
    val slack = with(density) { IosDimens.PopupSlack.toPx() }
    val btnGap = with(density) { IosDimens.PopupButtonGap.toPx() }
    val btnH = with(density) { IosDimens.PopupButtonHeight.toPx() }

    Box(
        modifier
            .size(IosDimens.GlobeTouchSize)
            .pointerInput(Unit) {
                awaitEachGesture {
                  try {
                    awaitFirstDown()
                    down = true
                    var released = false
                    // tắt "Hold Globe for Language Menu" thì không có giữ-để-chọn, chỉ còn chạm nhanh
                    val up = withTimeoutOrNull(if (settings.globeLongPress.value) settings.longPressMs.value.toLong() else Long.MAX_VALUE) {
                        waitForUpOrCancellation().also { released = true }
                    }
                    if (released) { // nhấc tay sớm => chạm nhanh
                        down = false
                        if (up != null) currentOnNext()
                        return@awaitEachGesture
                    }

                    // giữ đủ lâu => mở popup
                    val count = currentCount
                    hovered = currentSelected
                    popupVisible = true
                    view.longPressTick(settings)
                    val buttons = currentHasButtons
                    val popupH = count * itemH + padV * 2 + if (buttons) btnGap + btnH else 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val p = change.position // toạ độ so với góc trên-trái của nút
                        val itemsTop = -gap - popupH + padV
                        val insideX = p.x >= -slack && p.x <= popupW + slack
                        val rel = (p.y - itemsTop) / itemH
                        val btnTop = itemsTop + count * itemH + btnGap
                        val now = when {
                            insideX && rel >= 0f && rel < count -> rel.toInt()
                            // hàng nút ở đáy: nửa trái / nửa phải (thêm chút dư phía dưới cho dễ trúng)
                            buttons && insideX && p.y >= btnTop - btnGap && p.y < btnTop + btnH + padV ->
                                if (p.x < popupW / 2f) BTN_FLOAT else BTN_ADJUST
                            else -> NONE
                        }
                        if (now != hovered) {
                            hovered = now
                            if (now != NONE) view.selectionTick(settings)
                        }
                        change.consume()
                        if (!change.pressed) break
                    }
                    val chosen = hovered
                    down = false
                    popupVisible = false
                    hovered = NONE
                    when {
                        chosen == BTN_FLOAT -> currentOnFloat?.invoke()
                        chosen == BTN_ADJUST -> currentOnAdjust?.invoke()
                        chosen == NONE -> Unit
                        chosen < currentLanguageCount -> currentOnSelect(chosen)
                        else -> currentOnExtra(chosen - currentLanguageCount)
                    }
                  } finally {
                    down = false; popupVisible = false; hovered = NONE
                  }
                }
            }
            .pressScale(down),
        contentAlignment = Alignment.Center,
    ) {
        GlobeIcon(palette.keyText, size = 32.dp)
        LanguagePopup(popupVisible, languages.map { it.name } + extras, hovered, hasButtons, floatingActive)
    }
}

// Popup nằm ngay phía trên quả địa cầu, mép trái thẳng hàng với nút. Kiểu kính tối: nền gần đen trong mờ,
// viền sáng mảnh ở mép trên, hàng đang chọn thành viên thuốc sáng. Đáy có 2 nút: bàn phím nổi | chỉnh cỡ.
// Hiện ra bằng lò xo nhẹ, phóng từ góc dưới trái (chỗ quả địa cầu).
@Composable
private fun LanguagePopup(visible: Boolean, names: List<String>, hovered: Int, buttons: Boolean, floatingActive: Boolean) {
    val palette = LocalKeyboardPalette.current
    val gapPx = with(LocalDensity.current) { IosDimens.BubbleGap.roundToPx() }
    val provider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ) = IntOffset(
                anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                anchorBounds.top - popupContentSize.height - gapPx,
            )
        }
    }
    MorphPopup(visible = visible, positionProvider = provider, origin = TransformOrigin(0.1f, 1f)) {
            names.forEachIndexed { i, name ->
                val on = i == hovered
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(IosDimens.PopupItemHeight)
                        .clip(CircleShape)
                        .then(if (on) Modifier.background(PillOn).border(0.6.dp, Color.White.copy(alpha = 0.14f), CircleShape) else Modifier)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(name, color = palette.popupText, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
            if (buttons) {
                Spacer(Modifier.height(IosDimens.PopupButtonGap))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PopupButton(hovered == BTN_FLOAT, Modifier.weight(1f)) {
                        FloatKeyboardIcon(if (floatingActive) IosColors.Accent else Color.White, size = 26.dp)
                    }
                    PopupButton(hovered == BTN_ADJUST, Modifier.weight(1f)) { ResizeIcon(Color.White, size = 24.dp) }
                }
            }
    }
}

@Composable
private fun PopupButton(on: Boolean, modifier: Modifier, icon: @Composable () -> Unit) {
    Box(
        modifier
            .height(IosDimens.PopupButtonHeight)
            .clip(CircleShape)
            .background(if (on) PillOn else ButtonOff)
            .border(0.6.dp, Color.White.copy(alpha = if (on) 0.18f else 0.07f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

private val GlassTop = Color(0xF2262628)
private val GlassBottom = Color(0xF5121214)
private val PillOn = Color.White.copy(alpha = 0.17f)
private val ButtonOff = Color.White.copy(alpha = 0.07f)
