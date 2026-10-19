package com.r7tkeysx.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

// Thời lượng + kiểu chuyển động dùng chung. Muốn đổi cảm giác của MỌI hiệu ứng: sửa ở đây.
object Motion {
    // Đi nhanh lúc đầu rồi chậm dần, đuôi dài và mềm (ease-out quint); không cần nhanh, chỉ cần mượt
    val Decelerate = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    const val PAGE_MS = 520        // chuyển trang
    const val KEYBOARD_MS = 420    // bàn phím trượt lên / xuống
    const val SHEET_MS = 460       // tấm phủ Add New Keyboard
    const val SETTLE_MS = 420      // dòng vuốt bị thả ra không đủ xa -> trượt về chỗ cũ
    const val SWIPE_OUT_MS = 380   // vuốt xóa: dòng trượt hẳn ra ngoài
    const val ROW_MS = 380         // dòng bung ra / thu lại
    const val BAR_MS = 380         // ô Try out hiện / ẩn
    const val DIALOG_MS = 320
}
