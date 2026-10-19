package com.r7tkeysx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.r7tkeysx.app.data.ThemeMode

// Bộ màu của bàn phím: có bản tối và bản sáng. Muốn đổi màu bàn phím thì sửa ở đây.
class KeyboardPalette(
    val tray: Color,
    val letterKey: Color,
    val letterKeyPressed: Color,
    val functionKey: Color,
    val functionKeyPressed: Color,
    val keyText: Color,
    val secondaryText: Color,
    val returnKey: Color,
    val returnKeyPressed: Color,
    val bubble: Color,
    val bubbleText: Color,
    val shadow: Color,
    val hairline: Color,
    val shiftActive: Color,
    val shiftActivePressed: Color,
    val shiftActiveIcon: Color,
    val popup: Color,
    val popupText: Color,
    val highlight: Color, // mục đang chọn trong popup
)

val DarkKeyboardPalette = KeyboardPalette(
    tray = Color(0xFF191A1C),
    letterKey = Color(0xFF3B3B3B),
    letterKeyPressed = Color(0xFF636366),
    functionKey = Color(0xFF3B3B3B),
    functionKeyPressed = Color(0xFF636366),
    keyText = Color.White,
    secondaryText = Color(0xFF8E8E93),
    returnKey = Color(0xFF2A61D6),
    returnKeyPressed = Color(0xFF3F78EA),
    bubble = Color(0xFF636366),
    bubbleText = Color.White,
    shadow = Color(0x99000000),
    hairline = Color(0x33FFFFFF),
    shiftActive = Color.White,
    shiftActivePressed = Color(0xFFD1D1D6),
    shiftActiveIcon = Color.Black,
    popup = Color(0xFF3B3B3B),
    popupText = Color.White,
    highlight = Color(0xFF2A61D6),
)

val LightKeyboardPalette = KeyboardPalette(
    tray = Color(0xFFD2D5DB),
    letterKey = Color.White,
    letterKeyPressed = Color(0xFFD9DCE1),
    functionKey = Color(0xFFADB3BD),
    functionKeyPressed = Color.White,
    keyText = Color.Black,
    secondaryText = Color(0xFF6C6C70),
    returnKey = Color(0xFF007AFF),
    returnKeyPressed = Color(0xFF3395FF),
    bubble = Color.White,
    bubbleText = Color.Black,
    shadow = Color(0x66000000),
    hairline = Color(0x33000000),
    shiftActive = Color.White,
    shiftActivePressed = Color(0xFFE5E5EA),
    shiftActiveIcon = Color.Black,
    popup = Color(0xFFF2F3F5),
    popupText = Color.Black,
    highlight = Color(0xFF007AFF),
)

val LocalKeyboardPalette = staticCompositionLocalOf { DarkKeyboardPalette }

// Chọn bộ màu theo cài đặt Themes (Follow System thì theo chế độ sáng/tối của máy)
@Composable
fun resolveKeyboardPalette(mode: ThemeMode): KeyboardPalette = when (mode) {
    ThemeMode.DARK -> DarkKeyboardPalette
    ThemeMode.LIGHT -> LightKeyboardPalette
    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkKeyboardPalette else LightKeyboardPalette
}
