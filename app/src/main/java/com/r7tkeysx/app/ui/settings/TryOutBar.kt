package com.r7tkeysx.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.ui.glass.liquidGlass
import com.r7tkeysx.app.ui.keyboard.KeyboardIcon
import com.r7tkeysx.app.ui.theme.IosColors
import kotlinx.coroutines.delay

private const val CURSOR_BLINK_MS = 530L

// Ô nhập nổi dạng viên thuốc: hiện chữ đã gõ + con trỏ nhấp nháy + nút ✕ xóa nhanh
@Composable
fun TryOutBar(
    text: String,
    onClear: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onPickType: () -> Unit, // chạm icon bàn phím: mở popup chọn kiểu nhập
    modifier: Modifier = Modifier,
    cursor: Int = text.length, // con trỏ đứng trước ký tự thứ mấy
    onTextLayout: (TextLayoutResult) -> Unit = {}, // báo bố cục chữ (vị trí từng dòng) để rê con trỏ lên / xuống dòng
    tracking: Boolean = false, // đang giữ phím cách để rê con trỏ: con trỏ đứng yên, không nhấp nháy
) {
    val currentPickType by androidx.compose.runtime.rememberUpdatedState(onPickType)
    val currentToggle by androidx.compose.runtime.rememberUpdatedState(onToggleKeyboard)
    var cursorOn by remember { mutableStateOf(true) }
    LaunchedEffect(text, cursor) { // vừa gõ / dời con trỏ thì hiện liền rồi mới nhấp nháy
        cursorOn = true
        while (true) {
            delay(CURSOR_BLINK_MS)
            cursorOn = !cursorOn
        }
    }
    val scroll = rememberScrollState()
    // chữ dài / xuống dòng thì cuộn xuống cuối để luôn thấy chỗ đang gõ
    LaunchedEffect(text) { scroll.scrollTo(scroll.maxValue) }

    val pill = RoundedCornerShape(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .liquidGlass(pill, surface = IosColors.TryOutPill.copy(alpha = 0.55f), blurRadius = 4.dp, lensHeight = 10.dp, lensAmount = 14.dp)
            .border(1.dp, IosColors.Hairline, pill)
            // chạm vào ô (ngoài nút ✕) thì hiện / ẩn bàn phím
            .pointerInput(Unit) { detectTapGestures { currentToggle() } }
            .padding(start = 12.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // vùng chạm của icon rộng hơn hình icon một chút
        Box(Modifier.pointerInput(Unit) { detectTapGestures { currentPickType() } }.padding(6.dp)) {
            KeyboardIcon(IosColors.SecondaryText)
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f).heightIn(max = 96.dp).verticalScroll(scroll)) {
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            // đang rê con trỏ lên / xuống: cuộn ô cho dòng có con trỏ luôn nhìn thấy
            LaunchedEffect(cursor, tracking) {
                val l = layout ?: return@LaunchedEffect
                if (!tracking || text.isEmpty()) return@LaunchedEffect
                val r = l.getCursorRect(cursor.coerceIn(0, l.layoutInput.text.length))
                val viewH = scroll.viewportSize
                when {
                    r.top < scroll.value -> scroll.animateScrollTo(r.top.toInt())
                    r.bottom > scroll.value + viewH -> scroll.animateScrollTo((r.bottom - viewH).toInt())
                }
            }
            val density = LocalDensity.current
            // bố cục chữ có thể còn là của lần vẽ trước (chữ ngắn hơn): luôn kẹp con trỏ trong độ dài của chính bố cục đó
            fun TextLayoutResult.caret(i: Int) = getCursorRect(if (text.isEmpty()) 0 else i.coerceIn(0, layoutInput.text.length))
            Text(
                text.ifEmpty { "Try out the keyboard" },
                color = if (text.isEmpty()) IosColors.SecondaryText else Color.White,
                fontSize = 20.sp,
                onTextLayout = { layout = it; if (text.isNotEmpty()) onTextLayout(it) },
                modifier = Modifier
                    .padding(start = 3.dp) // chừa chỗ cho con trỏ ở đầu dòng
                    .drawWithContent {
                        drawContent()
                        val l = layout ?: return@drawWithContent
                        val rect = l.caret(cursor)
                        // đang rê bằng phím cách: con trỏ đứng yên không nhấp nháy, nhảy ngay tới chỗ mới
                        if (tracking || cursorOn) {
                            drawLine(IosColors.ReturnBlue, Offset(rect.left, rect.top), Offset(rect.left, rect.bottom), with(density) { 2.dp.toPx() })
                        }
                    },
            )
        }
        Spacer(Modifier.width(8.dp))
        // Nút ✕: chỉ hiện khi đã có chữ
        if (text.isNotEmpty()) {
            val currentOnClear by androidx.compose.runtime.rememberUpdatedState(onClear)
            Canvas(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(IosColors.SecondaryText)
                    .pointerInput(Unit) { detectTapGestures { currentOnClear() } },
            ) {
                val s = size.width
                val sw = s * 0.09f
                val a = s * 0.34f
                val b = s * 0.66f
                drawLine(IosColors.TryOutPill, Offset(a, a), Offset(b, b), sw, StrokeCap.Round)
                drawLine(IosColors.TryOutPill, Offset(b, a), Offset(a, b), sw, StrokeCap.Round)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 393)
@Composable
private fun TryOutBarPreview() {
    Box(Modifier.padding(20.dp)) { TryOutBar(text = "hello world", onClear = {}, onToggleKeyboard = {}, onPickType = {}) }
}
