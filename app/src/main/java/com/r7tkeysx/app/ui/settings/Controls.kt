package com.r7tkeysx.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.ui.glass.GlassSlider
import com.r7tkeysx.app.ui.keyboard.UpDownIcon
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import kotlin.math.roundToInt

private val ThumbWidth = 46.dp
private val ThumbHeight = 28.dp
private val TrackHeight = 8.dp
private val SideInset = IosDimens.CardPadding + 4.dp

// Dòng thanh trượt: tên + giá trị nhỏ bên cạnh, (mô tả), rồi thanh trượt xanh với núm trắng.
// Chạm hoặc kéo ngang đều chỉnh được; giá trị làm tròn về số nguyên trong [min, max].
@Composable
internal fun SliderRow(
    title: String,
    valueText: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
    onReset: (() -> Unit)? = null, // chạm vào con số thì về giá trị gốc
    ticks: Int = 0,
) {
    val currentReset by rememberUpdatedState(onReset)
    val currentEnabled by rememberUpdatedState(enabled)
    Column(Modifier.fillMaxWidth().padding(horizontal = SideInset, vertical = 14.dp).alpha(if (enabled) 1f else 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // tên dài thì tự xuống dòng, con số bên cạnh luôn nằm gọn một dòng
            Text(title, color = IosColors.PrimaryText, fontSize = 20.sp, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(6.dp))
            Text(
                valueText, maxLines = 1, softWrap = false, color = IosColors.SecondaryText, fontSize = 19.sp,
                // vùng chạm rộng hơn con số một chút
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures { if (currentEnabled) currentReset?.invoke() } }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
        description?.let {
            Text(it, color = IosColors.SecondaryText, fontSize = 16.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(10.dp))
        GlassSlider(value, min, max, onChange, enabled = enabled, ticks = ticks)
    }
}

// Dòng chọn dạng danh sách thả xuống: giá trị hiện tại + mũi tên ⌃⌄; chạm để mở menu.
@Composable
internal fun <T> DropdownRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
    description: ((T) -> String?)? = null, // mô tả dưới tên mỗi lựa chọn trong menu
) {
    var open by remember { mutableStateOf(false) }
    val currentEnabled by rememberUpdatedState(enabled)
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
                .pointerInput(Unit) { detectTapGestures { if (currentEnabled) open = true } }
                .padding(start = SideInset, end = 14.dp, top = 12.dp, bottom = 12.dp)
                .alpha(if (enabled) 1f else 0.45f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = IosColors.PrimaryText, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(label(selected), color = IosColors.SecondaryText, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(4.dp))
            UpDownIcon(IosColors.SecondaryText, size = 20.dp)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = IosColors.TypePopup) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column(Modifier.widthIn(max = 280.dp)) {
                                Text(
                                    label(option),
                                    color = if (option == selected) IosColors.Accent else IosColors.PrimaryText,
                                    fontSize = 18.sp,
                                )
                                description?.invoke(option)?.let {
                                    Text(it, color = IosColors.SecondaryText, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        },
                        onClick = { onSelect(option); open = false },
                    )
                }
            }
        }
    }
}

// Dòng bấm được kèm mũi tên › (vd. Clear History)
@Composable
internal fun ChevronRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    val currentClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .background(if (pressed) IosColors.RowPressed else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { currentClick() },
                )
            }
            .padding(start = SideInset, end = 18.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = IosColors.PrimaryText, fontSize = 20.sp)
            subtitle?.let { Text(it, color = IosColors.SecondaryText, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp)) }
        }
        com.r7tkeysx.app.ui.keyboard.ChevronIcon(IosColors.SecondaryText)
    }
}
