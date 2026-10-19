package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.ClipItem
import com.r7tkeysx.app.data.ClipboardStore
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.KeyboardPalette

// Bảng Clipboard: tiêu đề + nút chọn / xóa / chỉ-ghim, bộ lọc Text / Images / Videos, lưới 2 cột các mục đã chép.
// Chạm = dán; giữ lâu = ghim / bỏ ghim; có hộp xác nhận trước khi xóa hết. (Hiện mới có chữ; ảnh / video chưa hỗ trợ.)
@Composable
fun ClipboardPanel(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    height: Dp,
    landscape: Boolean,
    onPaste: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackToLetters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { ClipboardStore.get(context) }
    val currentPaste by rememberUpdatedState(onPaste)
    val currentBack by rememberUpdatedState(onBackToLetters)

    var showText by remember { mutableStateOf(true) }
    var showImages by remember { mutableStateOf(true) }
    var showVideos by remember { mutableStateOf(false) }
    var pinnedOnly by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    val selected = remember { mutableSetOf<Long>().toMutableStateList() }
    var confirmClear by remember { mutableStateOf(false) }

    // dọn mục cũ / nhạy cảm mỗi lần mở bảng
    remember { store.cleanup(settings); 0 }

    val visible = store.items.filter { showText && (!pinnedOnly || it.pinned) }
    val pinned = visible.filter { it.pinned }
    val others = visible.filter { !it.pinned }
    val configured = if (landscape) settings.clipColumnsLandscape.value else settings.clipColumnsPortrait.value
    val columns = if (configured > 0) configured else if (landscape) 3 else 2

    Box(modifier.fillMaxWidth().height(height)) {
        Column(Modifier.fillMaxSize()) {
            // tiêu đề + 3 nút
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Clipboard", color = palette.keyText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                HeaderButton(
                    onTap = { selectMode = !selectMode; if (!selectMode) selected.clear() },
                ) { CheckCircleIcon(if (selectMode) IosColors.Accent else palette.keyText, filled = selectMode, tickColor = Color.White) }
                HeaderButton(
                    onTap = {
                        if (selectMode && selected.isNotEmpty()) { store.remove(selected.toSet()); selected.clear() }
                        else if (store.items.any { !it.pinned }) confirmClear = true
                    },
                ) { TrashIcon(palette.keyText, size = 24.dp) }
                HeaderButton(onTap = { pinnedOnly = !pinnedOnly }) {
                    BookmarkIcon(if (pinnedOnly) IosColors.Accent else palette.keyText, filled = pinnedOnly, slashed = !pinnedOnly)
                }
            }
            // bộ lọc loại nội dung
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Text", showText, palette) { showText = !showText }
                FilterChip("Images", showImages, palette) { showImages = !showImages }
                FilterChip("Videos", showVideos, palette) { showVideos = !showVideos }
            }
            // lưới mục
            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (pinnedOnly) "No pinned items" else "Nothing copied yet",
                        color = palette.secondaryText, fontSize = 16.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    if (pinned.isNotEmpty()) {
                        item(key = "h:pinned", span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Pinned", palette) }
                        items(pinned.size, key = { "c:${pinned[it].id}" }) { i -> ClipCell(pinned[i], palette, selectMode, pinned[i].id in selected, store, settings, selected, currentPaste, currentBack) }
                    }
                    if (others.isNotEmpty()) {
                        item(key = "h:other", span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Other", palette) }
                        items(others.size, key = { "c:${others[it].id}" }) { i -> ClipCell(others[i], palette, selectMode, others[i].id in selected, store, settings, selected, currentPaste, currentBack) }
                    }
                }
            }
            // hàng dưới: ABC | xóa lùi
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(64.dp)
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { view.keyTick(settings); currentBack() } },
                    contentAlignment = Alignment.CenterStart,
                ) { Text("ABC", color = palette.keyText, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
                Spacer(Modifier.weight(1f))
                FunctionKey(onTap = onBackspace, modifier = Modifier.width(60.dp), repeatOnHold = true, flat = true) { BackspaceIcon(palette.keyText) }
            }
        }
        if (confirmClear) {
            ConfirmDialog(
                message = "Are you sure you want to clear your full clipboard history? This will affect all items, except pinned ones.",
                palette = palette,
                onNo = { confirmClear = false },
                onYes = { store.clearUnpinned(); selected.clear(); confirmClear = false },
            )
        }
    }
}

@Composable
private fun HeaderButton(onTap: () -> Unit, content: @Composable () -> Unit) {
    val currentTap by rememberUpdatedState(onTap)
    Box(
        Modifier
            .size(46.dp)
            .pointerInput(Unit) { detectTapGestures { currentTap() } },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, palette: KeyboardPalette, onToggle: () -> Unit) {
    val currentToggle by rememberUpdatedState(onToggle)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) IosColors.Accent else palette.letterKey)
            .pointerInput(Unit) { detectTapGestures { currentToggle() } }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionTitle(title: String, palette: KeyboardPalette) {
    Text(
        title.uppercase(), color = palette.keyText, fontSize = 15.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 0.dp),
    )
}

// Một mục clipboard: khung bo góc + viền mảnh, chữ tối đa 4 dòng
@Composable
private fun ClipCell(
    item: ClipItem,
    palette: KeyboardPalette,
    selectMode: Boolean,
    isSelected: Boolean,
    store: ClipboardStore,
    settings: KeyboardSettings,
    selection: MutableList<Long>,
    onPaste: (String) -> Unit,
    onBackToLetters: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentItem by rememberUpdatedState(item)
    val currentSelectMode by rememberUpdatedState(selectMode)
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (pressed) palette.letterKey else Color.Transparent)
            .border(1.dp, if (isSelected) IosColors.Accent else palette.keyText.copy(alpha = 0.35f), shape)
            .pointerInput(item.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    val held = awaitLongPressOrCancellation(down.id)
                    if (held == null) { // chạm (nhấc tay sớm) hoặc đang cuộn
                        pressed = false
                        val last = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (last != null && !last.pressed && !last.isConsumed) {
                            view.keyTick(settings)
                            val it = currentItem
                            if (currentSelectMode) {
                                if (it.id in selection) selection.remove(it.id) else selection.add(it.id)
                            } else {
                                onPaste(it.text)
                                if (settings.clipSyncTo.value.includesSet) ClipboardStore.setSystemClip(context, it.text)
                                if (settings.clipHideOnPaste.value) onBackToLetters()
                            }
                        }
                        return@awaitEachGesture
                    }
                    pressed = false
                    view.longPressTick(settings)
                    store.togglePin(currentItem.id)
                    held.consume()
                    while (true) { // chờ nhấc tay để cú nhấc không bị tính là chạm
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) break
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(
            item.text, color = palette.keyText, fontSize = 17.sp, lineHeight = 21.sp,
            maxLines = 4, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(end = if (item.pinned || selectMode) 18.dp else 0.dp),
        )
        if (selectMode) {
            CheckCircleIcon(if (isSelected) IosColors.Accent else palette.secondaryText, size = 18.dp, filled = isSelected, tickColor = Color.White, modifier = Modifier.align(Alignment.TopEnd))
        } else if (item.pinned) {
            BookmarkIcon(palette.secondaryText, size = 16.dp, filled = true, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

// Hộp xác nhận kiểu iOS (No / Yes) nổi giữa bảng, nền tối mờ phía sau
@Composable
private fun ConfirmDialog(message: String, palette: KeyboardPalette, onNo: () -> Unit, onYes: () -> Unit) {
    val currentNo by rememberUpdatedState(onNo)
    val currentYes by rememberUpdatedState(onYes)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { currentNo() } },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(18.dp)
        Column(
            Modifier
                .padding(horizontal = 36.dp)
                .clip(shape)
                .background(palette.popup)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Text(
                message, color = palette.keyText, fontSize = 17.sp, lineHeight = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            )
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.hairline))
            Row(Modifier.fillMaxWidth().height(50.dp)) {
                Box(
                    Modifier.weight(1f).fillMaxSize().pointerInput(Unit) { detectTapGestures { currentNo() } },
                    contentAlignment = Alignment.Center,
                ) { Text("No", color = IosColors.Accent, fontSize = 19.sp) }
                Box(Modifier.width(0.5.dp).fillMaxSize().background(palette.hairline))
                Box(
                    Modifier.weight(1f).fillMaxSize().pointerInput(Unit) { detectTapGestures { currentYes() } },
                    contentAlignment = Alignment.Center,
                ) { Text("Yes", color = IosColors.Danger, fontSize = 19.sp) }
            }
        }
    }
}
