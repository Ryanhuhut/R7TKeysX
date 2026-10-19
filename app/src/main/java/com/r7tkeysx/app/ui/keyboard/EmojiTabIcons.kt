package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Icon nét mảnh cho thanh chọn nhóm emoji (thay cho emoji màu). `key` là biểu tượng của nhóm trong EmojiData.
// Mọi icon vẽ trong ô vuông 1x1 rồi nhân kích thước thật, nét dày = 8% cạnh.
@Composable
fun EmojiCategoryIcon(key: String, color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val st = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (key) {
            "🕘" -> clock(color, w, st)
            "😀" -> smiley(color, w, st)
            "🐻" -> paw(color, w, st)
            "🍔" -> apple(color, w, st)
            "⚽" -> ball(color, w, st)
            "🚗" -> car(color, w, st)
            "💡" -> bulb(color, w, st)
            "❤️" -> heart(color, w, st)
            "🏁" -> flag(color, w, st)
        }
    }
}

private fun DrawScope.clock(c: Color, w: Float, st: Stroke) {
    drawCircle(c, radius = w * 0.42f, center = Offset(w / 2, w / 2), style = st)
    drawLine(c, Offset(w / 2, w / 2), Offset(w / 2, w * 0.24f), st.width, StrokeCap.Round)
    drawLine(c, Offset(w / 2, w / 2), Offset(w * 0.68f, w * 0.58f), st.width, StrokeCap.Round)
}

private fun DrawScope.smiley(c: Color, w: Float, st: Stroke) {
    drawCircle(c, radius = w * 0.42f, center = Offset(w / 2, w / 2), style = st)
    drawCircle(c, radius = w * 0.045f, center = Offset(w * 0.37f, w * 0.42f))
    drawCircle(c, radius = w * 0.045f, center = Offset(w * 0.63f, w * 0.42f))
    drawArc(
        c, startAngle = 25f, sweepAngle = 130f, useCenter = false,
        topLeft = Offset(w * 0.3f, w * 0.3f), size = Size(w * 0.4f, w * 0.4f), style = st,
    )
}

// Dấu chân thú: đệm lớn + 4 ngón
private fun DrawScope.paw(c: Color, w: Float, st: Stroke) {
    val pad = Path().apply {
        moveTo(w * 0.5f, w * 0.5f)
        cubicTo(w * 0.34f, w * 0.5f, w * 0.24f, w * 0.64f, w * 0.27f, w * 0.76f)
        cubicTo(w * 0.3f, w * 0.88f, w * 0.42f, w * 0.86f, w * 0.5f, w * 0.83f)
        cubicTo(w * 0.58f, w * 0.86f, w * 0.7f, w * 0.88f, w * 0.73f, w * 0.76f)
        cubicTo(w * 0.76f, w * 0.64f, w * 0.66f, w * 0.5f, w * 0.5f, w * 0.5f)
        close()
    }
    drawPath(pad, c, style = st)
    listOf(0.17f to 0.42f, 0.38f to 0.22f, 0.62f to 0.22f, 0.83f to 0.42f).forEach { (x, y) ->
        drawOval(c, Offset(w * (x - 0.075f), w * (y - 0.1f)), Size(w * 0.15f, w * 0.2f), style = st)
    }
}

private fun DrawScope.apple(c: Color, w: Float, st: Stroke) {
    val body = Path().apply {
        moveTo(w * 0.5f, w * 0.32f)
        cubicTo(w * 0.34f, w * 0.18f, w * 0.1f, w * 0.34f, w * 0.14f, w * 0.6f)
        cubicTo(w * 0.17f, w * 0.82f, w * 0.34f, w * 0.94f, w * 0.5f, w * 0.86f)
        cubicTo(w * 0.66f, w * 0.94f, w * 0.83f, w * 0.82f, w * 0.86f, w * 0.6f)
        cubicTo(w * 0.9f, w * 0.34f, w * 0.66f, w * 0.18f, w * 0.5f, w * 0.32f)
        close()
    }
    drawPath(body, c, style = st)
    drawLine(c, Offset(w * 0.5f, w * 0.3f), Offset(w * 0.54f, w * 0.12f), st.width, StrokeCap.Round)
    val leaf = Path().apply {
        moveTo(w * 0.57f, w * 0.2f)
        cubicTo(w * 0.66f, w * 0.1f, w * 0.78f, w * 0.12f, w * 0.8f, w * 0.14f)
        cubicTo(w * 0.78f, w * 0.24f, w * 0.66f, w * 0.26f, w * 0.57f, w * 0.2f)
    }
    drawPath(leaf, c, style = st)
}

// Bóng đá: vòng tròn + ngũ giác giữa + 5 nan nối ra viền
private fun DrawScope.ball(c: Color, w: Float, st: Stroke) {
    val ctr = Offset(w / 2, w / 2)
    val r = w * 0.42f
    drawCircle(c, radius = r, center = ctr, style = st)
    val pent = Path()
    val pts = (0 until 5).map { i ->
        val a = Math.toRadians(-90.0 + i * 72.0)
        Offset(ctr.x + (w * 0.15f * Math.cos(a)).toFloat(), ctr.y + (w * 0.15f * Math.sin(a)).toFloat())
    }
    pts.forEachIndexed { i, p -> if (i == 0) pent.moveTo(p.x, p.y) else pent.lineTo(p.x, p.y) }
    pent.close()
    drawPath(pent, c, style = st)
    pts.forEachIndexed { i, p ->
        val a = Math.toRadians(-90.0 + i * 72.0)
        drawLine(c, p, Offset(ctr.x + (r * Math.cos(a)).toFloat(), ctr.y + (r * Math.sin(a)).toFloat()), st.width, StrokeCap.Round)
    }
}

private fun DrawScope.car(c: Color, w: Float, st: Stroke) {
    val body = Path().apply {
        moveTo(w * 0.08f, w * 0.64f)
        lineTo(w * 0.08f, w * 0.52f)
        lineTo(w * 0.2f, w * 0.48f)
        lineTo(w * 0.3f, w * 0.3f)
        lineTo(w * 0.66f, w * 0.3f)
        lineTo(w * 0.78f, w * 0.48f)
        lineTo(w * 0.92f, w * 0.52f)
        lineTo(w * 0.92f, w * 0.64f)
        close()
    }
    drawPath(body, c, style = st)
    drawLine(c, Offset(w * 0.2f, w * 0.48f), Offset(w * 0.78f, w * 0.48f), st.width, StrokeCap.Round)
    drawCircle(c, radius = w * 0.09f, center = Offset(w * 0.27f, w * 0.68f), style = st)
    drawCircle(c, radius = w * 0.09f, center = Offset(w * 0.73f, w * 0.68f), style = st)
}

private fun DrawScope.bulb(c: Color, w: Float, st: Stroke) {
    val glass = Path().apply {
        moveTo(w * 0.38f, w * 0.64f)
        cubicTo(w * 0.38f, w * 0.52f, w * 0.24f, w * 0.48f, w * 0.24f, w * 0.34f)
        cubicTo(w * 0.24f, w * 0.18f, w * 0.36f, w * 0.1f, w * 0.5f, w * 0.1f)
        cubicTo(w * 0.64f, w * 0.1f, w * 0.76f, w * 0.18f, w * 0.76f, w * 0.34f)
        cubicTo(w * 0.76f, w * 0.48f, w * 0.62f, w * 0.52f, w * 0.62f, w * 0.64f)
        close()
    }
    drawPath(glass, c, style = st)
    drawLine(c, Offset(w * 0.4f, w * 0.76f), Offset(w * 0.6f, w * 0.76f), st.width, StrokeCap.Round)
    drawLine(c, Offset(w * 0.44f, w * 0.88f), Offset(w * 0.56f, w * 0.88f), st.width, StrokeCap.Round)
}

private fun DrawScope.heart(c: Color, w: Float, st: Stroke) {
    val p = Path().apply {
        moveTo(w * 0.5f, w * 0.86f)
        cubicTo(w * 0.1f, w * 0.58f, w * 0.06f, w * 0.34f, w * 0.2f, w * 0.22f)
        cubicTo(w * 0.34f, w * 0.1f, w * 0.46f, w * 0.18f, w * 0.5f, w * 0.3f)
        cubicTo(w * 0.54f, w * 0.18f, w * 0.66f, w * 0.1f, w * 0.8f, w * 0.22f)
        cubicTo(w * 0.94f, w * 0.34f, w * 0.9f, w * 0.58f, w * 0.5f, w * 0.86f)
        close()
    }
    drawPath(p, c, style = st)
}

private fun DrawScope.flag(c: Color, w: Float, st: Stroke) {
    drawLine(c, Offset(w * 0.22f, w * 0.1f), Offset(w * 0.22f, w * 0.9f), st.width, StrokeCap.Round)
    val f = Path().apply {
        moveTo(w * 0.22f, w * 0.16f)
        cubicTo(w * 0.4f, w * 0.06f, w * 0.56f, w * 0.26f, w * 0.82f, w * 0.14f)
        lineTo(w * 0.82f, w * 0.5f)
        cubicTo(w * 0.56f, w * 0.62f, w * 0.4f, w * 0.42f, w * 0.22f, w * 0.52f)
    }
    drawPath(f, c, style = st)
}

// Nút nhãn dán: ô vuông bo góc, góc dưới phải bị "lột" cong lên + một chấm cười
@Composable
fun StickerIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val st = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            moveTo(w * 0.3f, w * 0.1f)
            lineTo(w * 0.7f, w * 0.1f)
            cubicTo(w * 0.84f, w * 0.1f, w * 0.9f, w * 0.16f, w * 0.9f, w * 0.3f)
            lineTo(w * 0.9f, w * 0.52f)
            lineTo(w * 0.52f, w * 0.9f)
            lineTo(w * 0.3f, w * 0.9f)
            cubicTo(w * 0.16f, w * 0.9f, w * 0.1f, w * 0.84f, w * 0.1f, w * 0.7f)
            lineTo(w * 0.1f, w * 0.3f)
            cubicTo(w * 0.1f, w * 0.16f, w * 0.16f, w * 0.1f, w * 0.3f, w * 0.1f)
            close()
        }
        drawPath(body, color, style = st)
        val curl = Path().apply {
            moveTo(w * 0.9f, w * 0.52f)
            cubicTo(w * 0.62f, w * 0.52f, w * 0.52f, w * 0.62f, w * 0.52f, w * 0.9f)
        }
        drawPath(curl, color, style = st)
        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.32f, w * 0.38f))
        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.56f, w * 0.38f))
        drawArc(color, 20f, 140f, false, Offset(w * 0.3f, w * 0.34f), Size(w * 0.28f, w * 0.26f), style = st)
    }
}

@Composable
fun PlusIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.1f
        drawLine(color, Offset(w * 0.2f, w / 2), Offset(w * 0.8f, w / 2), sw, StrokeCap.Round)
        drawLine(color, Offset(w / 2, w * 0.2f), Offset(w / 2, w * 0.8f), sw, StrokeCap.Round)
    }
}
