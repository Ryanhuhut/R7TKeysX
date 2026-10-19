package com.r7tkeysx.app.ui.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

// Chữ "biến hình" như thanh gợi ý iPhone: đổi từ này sang từ khác thì chữ nào giống ở cùng vị trí đứng yên,
// chữ nào khác thì mờ dần sang chữ mới ngay tại chỗ, chữ thêm vào thì hiện dần; bề ngang cả từ co giãn êm.
@Composable
fun MorphText(text: String, color: Color, fontSize: TextUnit, fontWeight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
    val glyphs = remember(text) { graphemes(text) }
    Row(modifier.animateContentSize(spring(dampingRatio = 1f, stiffness = 900f))) {
        glyphs.forEachIndexed { i, g ->
            key(i) {
                // ô chữ mới xuất hiện: hiện dần
                val appear = remember { Animatable(0f) }
                LaunchedEffect(Unit) { appear.animateTo(1f, tween(200)) }
                AnimatedContent(
                    targetState = g,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) using SizeTransform(clip = false) },
                    modifier = Modifier.graphicsLayer { alpha = appear.value },
                    label = "glyph",
                ) { ch ->
                    Text(ch, color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

private fun graphemes(s: String): List<String> {
    if (s.isEmpty()) return emptyList()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(s)
    val out = ArrayList<String>(s.length)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        out += s.substring(start, end)
        start = end
        end = it.next()
    }
    return out
}
