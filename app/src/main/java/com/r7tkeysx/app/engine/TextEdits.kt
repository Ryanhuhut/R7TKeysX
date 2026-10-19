package com.r7tkeysx.app.engine

// Các phép sửa chữ thuần (không dính Android) dùng chung cho bàn phím hệ thống và bàn phím thử trong app; kiểm thử được trên máy tính.
object TextEdits {
    // Giữ phím xóa đủ lâu: xóa nguyên MỘT từ sát trái con trỏ + các dấu cách đứng ngay sau từ đó (không lan sang từ trước).
    // Trả về số ký tự cần xóa tính từ con trỏ ngược về trái. Ví dụ "xin chào " -> 5 (xóa "chào ").
    fun wordDeleteCount(before: String): Int {
        val body = before.trimEnd()
        return before.length - body.dropLastWhile { !it.isWhitespace() }.length
    }
}
