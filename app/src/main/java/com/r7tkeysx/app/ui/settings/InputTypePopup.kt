package com.r7tkeysx.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.model.InputKind
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.Motion

// Popup chọn kiểu ô nhập, nằm ngay phía trên ô "Try out the keyboard".
// Chạm ra ngoài popup thì đóng (lớp phủ trong suốt phủ cả màn hình bắt cú chạm đó).
@Composable
fun InputTypePopup(
    visible: Boolean,
    onSelect: (InputKind) -> Unit,
    onDismiss: () -> Unit,
    bottomOffset: Dp = 0.dp, // chừa chỗ cho ô nhập nằm dưới popup
    modifier: Modifier = Modifier,
) {
    val currentSelect by rememberUpdatedState(onSelect)
    val currentDismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier
            .fillMaxSize()
            // chỉ bắt cú chạm ra ngoài khi popup đang mở (đang đóng dở thì không chặn chạm xuống nền)
            .then(if (visible) Modifier.pointerInput(Unit) { detectTapGestures { currentDismiss() } } else Modifier),
    ) {
        val shape = RoundedCornerShape(IosDimens.TypePopupCorner)
        // bung ra từ góc dưới trái (chỗ icon bàn phím) với lò xo nhẹ, đóng lại thì thu về chỗ đó
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomStart),
            enter = fadeIn(tween(120)) + scaleIn(spring(0.78f, 520f), initialScale = 0.6f, transformOrigin = TransformOrigin(0.1f, 1f)),
            exit = fadeOut(tween(160)) + scaleOut(tween(Motion.DIALOG_MS, easing = Motion.Decelerate), targetScale = 0.7f, transformOrigin = TransformOrigin(0.1f, 1f)),
        ) {
        Column(
            Modifier
                .padding(start = 9.dp, bottom = bottomOffset)
                .width(IosDimens.TypePopupWidth)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(IosColors.TypePopup)
                // chạm trúng popup thì không tính là chạm ra ngoài
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            InputKind.entries.forEachIndexed { i, kind ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(IosColors.Divider))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(IosDimens.TypePopupItemHeight)
                        .pointerInput(kind) { detectTapGestures { currentSelect(kind) } }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(kind.title, color = IosColors.PrimaryText, fontSize = 20.sp)
                }
            }
        }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 393, heightDp = 600)
@Composable
private fun InputTypePopupPreview() {
    InputTypePopup(visible = true, onSelect = {}, onDismiss = {})
}
