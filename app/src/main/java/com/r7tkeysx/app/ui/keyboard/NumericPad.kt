package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import com.r7tkeysx.app.data.LocalKeyboardSettings
import com.r7tkeysx.app.model.InputKind
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.KeyboardPalette
import com.r7tkeysx.app.ui.theme.LocalKeyboardPalette

// Các kiểu ô nhập dùng bàn phím số 3x4 thay cho QWERTY
val NumericKinds = setOf(InputKind.NUMBER, InputKind.PHONE, InputKind.DECIMAL, InputKind.NUMBER_PASSWORD)

private val DIGIT_ROWS = listOf("123", "456", "789")
private val DIGIT_LETTERS = mapOf(
    '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL",
    '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ",
)

// Bàn phím số: 3 hàng 1-9, hàng cuối [góc trái] 0 [xóa]. Phone có thêm trang "+*#" (pause, wait, *, #, +).
@Composable
fun NumericPad(
    kind: InputKind,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalKeyboardPalette.current
    var symbols by remember(kind) { mutableStateOf(false) } // chỉ Phone dùng: đang ở trang +*#
    val landscape = isLandscapeLayout()
    val heightScale = LocalKeyboardSettings.current.keyHeightPercent.value / 100f
    val keyModifier = Modifier.height((if (landscape) IosDimens.LandscapePadKeyHeight else IosDimens.PadKeyHeight) * heightScale)
    val gap = IosDimens.PadGap

    // chạm vào khe giữa các phím số vẫn trúng phím gần nhất
    androidx.compose.runtime.CompositionLocalProvider(LocalKeyGap provides androidx.compose.ui.unit.DpSize(gap, gap)) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = IosDimens.TraySidePadding, end = IosDimens.TraySidePadding,
                top = if (landscape) 10.dp else IosDimens.PadTopPadding,
                bottom = if (landscape) 10.dp else IosDimens.PadBottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (kind == InputKind.PHONE && symbols) {
            // trang 2 của Phone: hàng trên trống để giữ nguyên chiều cao bàn phím
            Spacer(keyModifier)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                WordKey("pause", palette, { onChar(",") }, keyModifier.weight(1f))
                Spacer(keyModifier.weight(1f))
                WordKey("wait", palette, { onChar(";") }, keyModifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                WordKey("*", palette, { onChar("*") }, keyModifier.weight(1f))
                Spacer(keyModifier.weight(1f))
                WordKey("#", palette, { onChar("#") }, keyModifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                FunctionKey(onTap = { symbols = false }, modifier = keyModifier.weight(1f), flat = true) {
                    Text("123", color = palette.keyText, fontSize = 26.sp)
                }
                WordKey("+", palette, { onChar("+") }, keyModifier.weight(1f))
                PadBackspace(palette, onBackspace, keyModifier.weight(1f))
            }
        } else {
            DIGIT_ROWS.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { c -> DigitKey(c, palette, onChar, keyModifier.weight(1f)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                if (kind == InputKind.PHONE) {
                    FunctionKey(onTap = { symbols = true }, modifier = keyModifier.weight(1f), flat = true) {
                        Text("+*#", color = palette.keyText, fontSize = 26.sp)
                    }
                } else {
                    // Number / Decimal / NumberPassword: dấu chấm không nền ở góc trái
                    FunctionKey(onTap = { onChar(".") }, modifier = keyModifier.weight(1f), flat = true) {
                        Text(".", color = palette.keyText, fontSize = 30.sp)
                    }
                }
                DigitKey('0', palette, onChar, keyModifier.weight(1f))
                PadBackspace(palette, onBackspace, keyModifier.weight(1f))
            }
        }
    }
}
}

// Phím số: chữ số to + chữ cái nhỏ bên dưới (như bàn phím gọi điện)
@Composable
private fun DigitKey(digit: Char, palette: KeyboardPalette, onChar: (String) -> Unit, modifier: Modifier) {
    KeyBase(
        color = palette.letterKey,
        pressedColor = palette.letterKeyPressed,
        onTap = { onChar(digit.toString()) },
        modifier = modifier,
    ) {
        // số và chữ cái xếp chồng thành một khối ở giữa phím (không ghim chữ cái sát đáy nữa: phím thấp là đè lên số).
        // Bỏ khoảng đệm thừa trên/dưới của font để khối gọn; phím thấp hơn 56dp thì thu cỡ chữ theo.
        val landscape = isLandscapeLayout()
        val letters = if (landscape) null else DIGIT_LETTERS[digit]
        val fit = (LocalKeyboardSettings.current.keyHeightPercent.value / 100f).coerceIn(0.75f, 1f)
        val tight = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digit.toString(), color = palette.keyText, fontSize = 28.sp * fit, lineHeight = 30.sp * fit, style = tight)
            if (letters != null) {
                Text(
                    letters, color = palette.keyText, fontSize = 10.sp * fit, lineHeight = 12.sp * fit, letterSpacing = 2.sp,
                    style = tight, modifier = Modifier.padding(top = 1.dp),
                )
            } else if (!landscape) {
                Spacer(Modifier.height(13.dp * fit)) // phím 1 và 0 không có chữ: giữ số cùng độ cao với các phím khác
            }
        }
    }
}

@Composable
private fun WordKey(label: String, palette: KeyboardPalette, onTap: () -> Unit, modifier: Modifier) {
    KeyBase(color = palette.letterKey, pressedColor = palette.letterKeyPressed, onTap = onTap, modifier = modifier) {
        Text(label, color = palette.keyText, fontSize = if (label.length > 1) 24.sp else 30.sp)
    }
}

@Composable
private fun PadBackspace(palette: KeyboardPalette, onBackspace: () -> Unit, modifier: Modifier) {
    FunctionKey(onTap = onBackspace, modifier = modifier, repeatOnHold = true, flat = true) { BackspaceIcon(palette.keyText) }
}

@Preview(showBackground = true, backgroundColor = 0xFF191A1C, widthDp = 393)
@Composable
private fun NumericPadPreview() = com.r7tkeysx.app.ui.theme.R7TTheme {
    NumericPad(InputKind.NUMBER, onChar = {}, onBackspace = {})
}
