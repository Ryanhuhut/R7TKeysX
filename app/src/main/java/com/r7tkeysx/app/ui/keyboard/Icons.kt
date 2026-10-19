package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.r7tkeysx.app.model.ShiftState

// Quả địa cầu: vòng tròn + kinh tuyến (elip đứng) + 2 vĩ tuyến (đường ngang)
@Composable
fun GlobeIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        val r = w / 2f - stroke.width
        val c = Offset(w / 2f, w / 2f)
        drawCircle(color, radius = r, center = c, style = stroke)
        // kinh tuyến giữa: elip hẹp
        drawOval(color, topLeft = Offset(c.x - r * 0.42f, c.y - r), size = Size(r * 0.84f, r * 2f), style = stroke)
        // đường xích đạo + 2 vĩ tuyến
        drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), stroke.width, StrokeCap.Round)
        val dy = r * 0.5f
        val dx = kotlin.math.sqrt(r * r - dy * dy)
        drawLine(color, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y - dy), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(c.x - dx, c.y + dy), Offset(c.x + dx, c.y + dy), stroke.width, StrokeCap.Round)
    }
}

// Mũi tên nhỏ hướng sang phải ›
@Composable
fun ChevronIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.1f)
            lineTo(w * 0.78f, h * 0.5f)
            lineTo(w * 0.28f, h * 0.9f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Mũi tên Shift: rỗng khi OFF, đặc khi SINGLE, đặc + gạch chân khi CAPS_LOCK
@Composable
fun ShiftIcon(state: ShiftState, color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.5f, w * 0.06f)
            lineTo(w * 0.94f, w * 0.52f)
            lineTo(w * 0.7f, w * 0.52f)
            lineTo(w * 0.7f, w * 0.76f)
            lineTo(w * 0.3f, w * 0.76f)
            lineTo(w * 0.3f, w * 0.52f)
            lineTo(w * 0.06f, w * 0.52f)
            close()
        }
        if (state == ShiftState.OFF) {
            drawPath(path, color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
        } else {
            drawPath(path, color)
        }
        if (state == ShiftState.CAPS_LOCK) {
            drawRect(color, topLeft = Offset(w * 0.3f, w * 0.86f), size = Size(w * 0.4f, w * 0.08f))
        }
    }
}

// Phím xóa: hình thẻ nhọn bên trái có dấu X
@Composable
fun BackspaceIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.08f
        val path = Path().apply {
            moveTo(w * 0.05f, w * 0.5f)
            lineTo(w * 0.32f, w * 0.2f)
            lineTo(w * 0.95f, w * 0.2f)
            lineTo(w * 0.95f, w * 0.8f)
            lineTo(w * 0.32f, w * 0.8f)
            close()
        }
        drawPath(path, color, style = Stroke(width = sw, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.5f, w * 0.38f), Offset(w * 0.74f, w * 0.62f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.74f, w * 0.38f), Offset(w * 0.5f, w * 0.62f), sw, StrokeCap.Round)
    }
}

// Mặt cười đặc: mặt màu trắng, mắt và miệng màu nền phím
@Composable
fun EmojiIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, featureColor: Color = Color(0xFF3B3B3B)) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawCircle(color, radius = w / 2f - w * 0.04f, center = Offset(w / 2f, w / 2f))
        drawCircle(featureColor, radius = w * 0.06f, center = Offset(w * 0.36f, w * 0.40f))
        drawCircle(featureColor, radius = w * 0.06f, center = Offset(w * 0.64f, w * 0.40f))
        drawArc(
            featureColor, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.24f, w * 0.26f), size = Size(w * 0.52f, w * 0.5f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round),
        )
    }
}

// Icon bàn phím nhỏ (cho ô "Try out the keyboard"): khung bo góc + 2 hàng phím + thanh cách
@Composable
fun KeyboardIcon(color: Color, modifier: Modifier = Modifier, width: Dp = 32.dp) {
    Canvas(modifier.size(width, width * 0.7f)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.06f
        drawRoundRect(
            color, topLeft = Offset(sw / 2, sw / 2), size = Size(w - sw, h - sw),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f), style = Stroke(width = sw),
        )
        val dot = w * 0.065f
        for (row in 0..1) {
            for (i in 0 until 6) {
                val x = w * 0.17f + i * (w * 0.66f - dot) / 5f
                drawRect(color, topLeft = Offset(x, h * (0.27f + row * 0.22f)), size = Size(dot, dot))
            }
        }
        drawRect(color, topLeft = Offset(w * 0.27f, h * 0.7f), size = Size(w * 0.46f, dot))
    }
}

// Micro: thân viên thuốc + cung dưới + chân đế
@Composable
fun MicIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.08f
        drawRoundRect(
            color, topLeft = Offset(w * 0.37f, w * 0.06f), size = Size(w * 0.26f, w * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.13f), style = Stroke(width = sw),
        )
        drawArc(
            color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(w * 0.22f, w * 0.28f), size = Size(w * 0.56f, w * 0.46f),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(w * 0.5f, w * 0.74f), Offset(w * 0.5f, w * 0.92f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.36f, w * 0.92f), Offset(w * 0.64f, w * 0.92f), sw, StrokeCap.Round)
    }
}

// ---- Icon cho danh sách cài đặt (tất cả vẽ tay bằng Canvas, không dùng thư viện icon) ----

// Cọ sơn (Themes)
@Composable
fun BrushIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawLine(color, Offset(w * 0.80f, w * 0.14f), Offset(w * 0.50f, w * 0.46f), w * 0.16f, StrokeCap.Round)
        val tip = Path().apply {
            moveTo(w * 0.40f, w * 0.50f)
            lineTo(w * 0.54f, w * 0.64f)
            quadraticTo(w * 0.50f, w * 0.86f, w * 0.20f, w * 0.86f)
            quadraticTo(w * 0.26f, w * 0.64f, w * 0.40f, w * 0.50f)
            close()
        }
        drawPath(tip, color)
    }
}

// Chạm (Gestures): chấm ở giữa + hai vòng gợn lan ra
@Composable
fun TapIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val c = Offset(w / 2f, w / 2f)
        drawCircle(color, radius = w * 0.13f, center = c)
        val ring = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
        // mỗi vòng gồm 2 cung (trái-phải) chừa khoảng hở trên dưới, giống hiệu ứng sóng
        for (r in listOf(0.27f, 0.41f)) {
            val tl = Offset(c.x - w * r, c.y - w * r)
            val sz = Size(w * r * 2, w * r * 2)
            drawArc(color, -50f, 100f, false, tl, sz, style = ring)
            drawArc(color, 130f, 100f, false, tl, sz, style = ring)
        }
    }
}

// Đường lượn (Slide to Type): nét cong chữ S, có chấm ở điểm bắt đầu
@Composable
fun SlideIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.18f, w * 0.74f)
            cubicTo(w * 0.30f, w * 0.20f, w * 0.48f, w * 0.20f, w * 0.48f, w * 0.52f)
            cubicTo(w * 0.48f, w * 0.80f, w * 0.64f, w * 0.78f, w * 0.84f, w * 0.26f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.18f, w * 0.74f))
    }
}

// Bảng nhớ tạm (Clipboard)
@Composable
fun ClipboardIcon(color: Color, edgeColor: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawRoundRect(color, Offset(w * 0.22f, w * 0.16f), Size(w * 0.56f, w * 0.72f), androidx.compose.ui.geometry.CornerRadius(w * 0.1f))
        val clip = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
        drawRoundRect(edgeColor, Offset(w * 0.34f, w * 0.08f), Size(w * 0.32f, w * 0.18f), clip)
        drawRoundRect(color, Offset(w * 0.38f, w * 0.11f), Size(w * 0.24f, w * 0.12f), clip)
    }
}

// Loa phát tiếng (Sounds & Haptic Feedback)
@Composable
fun SpeakerIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val body = Path().apply {
            moveTo(w * 0.12f, w * 0.40f)
            lineTo(w * 0.28f, w * 0.40f)
            lineTo(w * 0.48f, w * 0.22f)
            lineTo(w * 0.48f, w * 0.78f)
            lineTo(w * 0.28f, w * 0.60f)
            lineTo(w * 0.12f, w * 0.60f)
            close()
        }
        drawPath(body, color)
        val sw = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        drawArc(color, -45f, 90f, false, Offset(w * 0.30f, w * 0.30f), Size(w * 0.40f, w * 0.40f), style = sw)
        drawArc(color, -45f, 90f, false, Offset(w * 0.20f, w * 0.20f), Size(w * 0.60f, w * 0.60f), style = sw)
    }
}

// Ổ khóa (Privacy & Permissions)
@Composable
fun LockIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawRoundRect(color, Offset(w * 0.22f, w * 0.44f), Size(w * 0.56f, w * 0.44f), androidx.compose.ui.geometry.CornerRadius(w * 0.1f))
        drawArc(
            color, 180f, 180f, false, Offset(w * 0.32f, w * 0.14f), Size(w * 0.36f, w * 0.56f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
    }
}

// Mũi tên xoay vòng (Reset Settings)
@Composable
fun ResetIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawArc(
            color, -30f, 290f, false, Offset(w * 0.2f, w * 0.2f), Size(w * 0.6f, w * 0.6f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
        val head = Path().apply {
            moveTo(w * 0.62f, w * 0.14f)
            lineTo(w * 0.88f, w * 0.20f)
            lineTo(w * 0.76f, w * 0.44f)
            close()
        }
        drawPath(head, color)
    }
}

// Mũi tên quay lại ‹
@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.66f, w * 0.1f)
            lineTo(w * 0.28f, w * 0.5f)
            lineTo(w * 0.66f, w * 0.9f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Dấu tick ✓ (mục đang chọn)
@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.12f, w * 0.54f)
            lineTo(w * 0.40f, w * 0.80f)
            lineTo(w * 0.88f, w * 0.22f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Mũi tên xuống ˅ (ẩn bàn phím)
@Composable
fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.12f, w * 0.34f)
            lineTo(w * 0.50f, w * 0.72f)
            lineTo(w * 0.88f, w * 0.34f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Mũi tên return ↵ (dùng trên phím return của bản ngang)
@Composable
fun ReturnIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.88f, w * 0.2f)
            lineTo(w * 0.88f, w * 0.58f)
            lineTo(w * 0.14f, w * 0.58f)
            moveTo(w * 0.36f, w * 0.36f)
            lineTo(w * 0.14f, w * 0.58f)
            lineTo(w * 0.36f, w * 0.8f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Thùng rác (nút xóa khi vuốt dòng sang trái)
@Composable
fun TrashIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.09f
        drawLine(color, Offset(w * 0.16f, w * 0.22f), Offset(w * 0.84f, w * 0.22f), sw, StrokeCap.Round) // nắp
        drawLine(color, Offset(w * 0.38f, w * 0.1f), Offset(w * 0.62f, w * 0.1f), sw, StrokeCap.Round)   // quai
        val body = Path().apply {
            moveTo(w * 0.24f, w * 0.3f)
            lineTo(w * 0.28f, w * 0.88f)
            lineTo(w * 0.72f, w * 0.88f)
            lineTo(w * 0.76f, w * 0.3f)
            close()
        }
        drawPath(body, color)
        drawLine(Color.Black.copy(alpha = 0.35f), Offset(w * 0.42f, w * 0.42f), Offset(w * 0.42f, w * 0.76f), sw * 0.8f, StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.35f), Offset(w * 0.58f, w * 0.42f), Offset(w * 0.58f, w * 0.76f), sw * 0.8f, StrokeCap.Round)
    }
}

// Dấu X (đóng tấm phủ)
@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.1f
        drawLine(color, Offset(w * 0.15f, w * 0.15f), Offset(w * 0.85f, w * 0.85f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.85f, w * 0.15f), Offset(w * 0.15f, w * 0.85f), sw, StrokeCap.Round)
    }
}

// Mũi tên lên-xuống ⌃⌄ (dòng chọn dạng danh sách thả xuống)
@Composable
fun UpDownIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val st = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val up = Path().apply { moveTo(w * 0.3f, w * 0.4f); lineTo(w * 0.5f, w * 0.2f); lineTo(w * 0.7f, w * 0.4f) }
        val down = Path().apply { moveTo(w * 0.3f, w * 0.6f); lineTo(w * 0.5f, w * 0.8f); lineTo(w * 0.7f, w * 0.6f) }
        drawPath(up, color, style = st)
        drawPath(down, color, style = st)
    }
}

// Kính lúp (ô tìm emoji)
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.12f
        val r = w * 0.32f
        drawCircle(color, radius = r, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(width = sw))
        drawLine(color, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.9f, w * 0.9f), sw, StrokeCap.Round)
    }
}

// Vòng tròn có dấu tick (chế độ chọn nhiều mục); filled = đã bật (nền đặc)
@Composable
fun CheckCircleIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 26.dp, filled: Boolean = false, tickColor: Color = Color.Black) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.075f
        val c = Offset(w / 2f, w / 2f)
        if (filled) drawCircle(color, radius = w / 2f - sw / 2f, center = c) else drawCircle(color, radius = w / 2f - sw, center = c, style = Stroke(width = sw))
        val tick = Path().apply {
            moveTo(w * 0.3f, w * 0.52f)
            lineTo(w * 0.45f, w * 0.66f)
            lineTo(w * 0.71f, w * 0.36f)
        }
        drawPath(tick, if (filled) tickColor else color, style = Stroke(width = sw * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Dấu trang; filled = đặc, slashed = bị gạch chéo (không lọc)
@Composable
fun BookmarkIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 26.dp, filled: Boolean = false, slashed: Boolean = false) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.075f
        val path = Path().apply {
            moveTo(w * 0.27f, w * 0.14f)
            lineTo(w * 0.73f, w * 0.14f)
            lineTo(w * 0.73f, w * 0.88f)
            lineTo(w * 0.5f, w * 0.68f)
            lineTo(w * 0.27f, w * 0.88f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = sw, join = StrokeJoin.Round))
        if (slashed) drawLine(color, Offset(w * 0.14f, w * 0.1f), Offset(w * 0.86f, w * 0.92f), sw, StrokeCap.Round)
    }
}

// Bàn phím nổi: khung chữ nhật nhỏ, tam giác trỏ lên trên và xuống dưới
@Composable
fun FloatKeyboardIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.08f
        drawRoundRect(
            color, topLeft = Offset(w * 0.18f, w * 0.34f), size = Size(w * 0.64f, w * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f), style = Stroke(width = sw),
        )
        val up = Path().apply { moveTo(w * 0.5f, w * 0.06f); lineTo(w * 0.62f, w * 0.22f); lineTo(w * 0.38f, w * 0.22f); close() }
        val down = Path().apply { moveTo(w * 0.5f, w * 0.94f); lineTo(w * 0.62f, w * 0.78f); lineTo(w * 0.38f, w * 0.78f); close() }
        drawPath(up, color)
        drawPath(down, color)
    }
}

// Chỉnh cỡ: hai mũi tên chéo ↖ ↘
@Composable
fun ResizeIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.085f
        val a = w * 0.14f
        val b = w * 0.86f
        val head = w * 0.26f
        drawLine(color, Offset(a, a), Offset(b, b), sw, StrokeCap.Round)
        drawLine(color, Offset(a, a), Offset(a + head, a), sw, StrokeCap.Round)
        drawLine(color, Offset(a, a), Offset(a, a + head), sw, StrokeCap.Round)
        drawLine(color, Offset(b, b), Offset(b - head, b), sw, StrokeCap.Round)
        drawLine(color, Offset(b, b), Offset(b, b - head), sw, StrokeCap.Round)
    }
}
