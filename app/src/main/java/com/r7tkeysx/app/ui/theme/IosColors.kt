package com.r7tkeysx.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Bảng màu: muốn đổi màu thì sửa ở đây, mọi nơi tự đổi theo
object IosColors {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val SecondaryText = Color(0xFF8E8E93)
    val StatusGreen = Color(0xFF30D158)
    val PrimaryText = Color(0xFFFFFFFF)
    val SignInPill = Color(0xFF191919)
    val SignInBorder = Color(0xFF2C2C2E)

    // Bàn phím
    val KeyboardTray = Color(0xFF191A1C)
    val LetterKey = Color(0xFF3B3B3B)
    val LetterKeyPressed = Color(0xFF636366)
    val FunctionKey = Color(0xFF3B3B3B)
    val ReturnBlue = Color(0xFF2A61D6)
    val KeyShadow = Color(0x99000000)
    val Bubble = Color(0xFF636366)
    val ReturnBluePressed = Color(0xFF3F78EA)
    val Hairline = Color(0x33FFFFFF)
    val Divider = Color(0xFF38383A)
    val RowPressed = Color(0xFF2C2C2E)
    val Accent = Color(0xFF0A84FF)
    val SwitchOff = Color(0xFF39393D)
    val Danger = Color(0xFFFF453A)
    val Warning = Color(0xFFFF9F0A)
    val TitleBarFade = Color(0x00000000)
    val TryOutPill = Color(0xFF272727)
    val TypePopup = Color(0xFF2C2C2E)
}

// Kích thước dùng chung (đơn vị dp: co giãn theo độ phân giải màn hình)
object IosDimens {
    val ScreenPadding = 20.dp
    val CardCorner = 22.dp
    val CardPadding = 16.dp
    val TopBarHeight = 64.dp
    val StatusCorner = 34.dp // thẻ Status bo tròn hẳn như viên thuốc
    val LabelIndent = 20.dp
    val RowHeight = 66.dp
    val SectionGap = 28.dp
    val LabelGap = 8.dp

    // Bàn phím
    val KeyHeight = 47.dp
    val KeyCorner = 8.dp // giá trị mặc định của Key corner radius
    val KeyGapH = 7.dp
    val KeyGapV = 11.dp
    val TrayCorner = 28.dp
    val TrayBottomCorner = 40.dp // Adaptive Bottom Corners: xấp xỉ góc bo của màn hình điện thoại
    val TraySidePadding = 8.dp
    val SuggestionBarHeight = 52.dp // thanh gợi ý chữ
    val BubbleWidth = 54.dp
    val BubbleHeight = 62.dp
    val BubbleGap = 6.dp

    // Phím chức năng (Shift, xóa, 123, emoji) rộng bằng chừng này lần phím chữ; return rộng hơn
    const val WIDE_KEY_FACTOR = 1.39f
    const val RETURN_KEY_FACTOR = 2.9f

    // Quả địa cầu + popup chọn ngôn ngữ
    val GlobeTouchSize = 56.dp
    val PopupWidth = 248.dp
    val PopupItemHeight = 60.dp
    val PopupPadding = 10.dp
    val PopupSlack = 16.dp // ngón tay lệch ra ngoài popup chừng này vẫn tính là trong
    val PopupButtonHeight = 52.dp // 2 nút ở đáy popup (bàn phím nổi | chỉnh cỡ)
    val PopupButtonGap = 8.dp

    // Bàn phím nổi + chế độ chỉnh cỡ
    val FloatCorner = 26.dp
    val FloatHandleHeight = 30.dp     // dải dưới đáy bàn phím nổi: thanh kéo + nút đóng
    val ResizeBracket = 16.dp         // độ dài mỗi cánh thẳng của dấu góc xanh (ngoài phần cung tròn)
    val ResizeBracketStroke = 10.dp
    val AdjustButton = 44.dp          // 3 nút tròn ở giữa khi chỉnh cỡ
    val DockZoneHeight = 96.dp        // vùng xanh ở đáy màn: thả bàn phím nổi vào đây là về bàn phím thường

    // Bản ngang: phím thấp hơn, quả địa cầu và mic nằm ngoài hai bên
    val LandscapeKeyHeight = 36.dp
    val LandscapeKeyGapV = 10.dp
    val LandscapeSuggestionHeight = 40.dp
    val LandscapeSideWidth = 96.dp
    val LandscapePadKeyHeight = 40.dp

    // Bàn phím số (Number, Phone...)
    val PadKeyHeight = 56.dp
    val PadGap = 6.dp
    val PadTopPadding = 25.dp
    val PadBottomPadding = 24.dp

    // Popup chọn kiểu nhập (Text, Number, ...)
    val TypePopupWidth = 284.dp
    val TypePopupItemHeight = 56.dp
    val TypePopupCorner = 28.dp

    // Giữ quả địa cầu lâu chừng này thì mở popup (mili-giây)
    const val LONG_PRESS_MS = 300L

    // Giữ phím xóa: chờ bao lâu thì bắt đầu lặp, rồi lặp mỗi bao lâu (mili-giây)
    const val REPEAT_DELAY_MS = 400L
    const val REPEAT_INTERVAL_MS = 70L

    // Giữ xóa quá chừng này (tính từ lúc chạm) thì chuyển sang xóa từng từ,
    // mỗi từ cách nhau lâu hơn để cảm giác rung từng cái một (mili-giây)
    const val DELETE_WORD_AFTER_MS = 3000L
    const val DELETE_WORD_INTERVAL_MS = 220L

    // Nhấn phím: co lại (1 = nguyên cỡ). Chạm nhanh chỉ co tới PRESS_SCALE rồi bật lại;
    // giữ lâu thì tiếp tục nhỏ dần tới PRESS_SCALE_HELD trong PRESS_HOLD_MS (mili-giây)
    const val PRESS_SCALE = 0.93f
    const val PRESS_SCALE_HELD = 0.80f
    const val PRESS_HOLD_MS = 900

    // Chạm Shift lần 2 trong khoảng này thì khóa chữ hoa (mili-giây)
    const val DOUBLE_TAP_MS = 300L
}
