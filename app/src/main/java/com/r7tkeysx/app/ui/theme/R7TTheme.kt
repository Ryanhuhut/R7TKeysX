package com.r7tkeysx.app.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.r7tkeysx.app.R

// Font dùng chung: Nunito (nét tròn, mềm; giấy phép mở SIL OFL). Muốn đổi font: thay file trong res/font rồi sửa tên ở đây.
val AppFont = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

// Bọc ngoài cùng của app và của bàn phím: mọi Text bên trong tự dùng AppFont
@Composable
fun R7TTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = AppFont)),
        content = content,
    )
}
