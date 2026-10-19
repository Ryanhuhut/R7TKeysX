package com.r7tkeysx.app.ui.keyboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.EmojiData
import com.r7tkeysx.app.data.EmojiStore
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.data.PinnedSorting
import com.r7tkeysx.app.data.RecentSorting
import com.r7tkeysx.app.data.SkinTone
import com.r7tkeysx.app.ui.theme.KeyboardPalette
import kotlinx.coroutines.launch

// Một ô trong lưới: một emoji, hoặc ô trống đệm cho đủ cột (để mỗi nhóm bắt đầu ở một cột mới)
private sealed interface EmojiEntry {
    class Item(val base: String, val section: String) : EmojiEntry
    class Pad(val id: Int) : EmojiEntry
    object HistorySlot : EmojiEntry // chỗ để dành cho emoji dùng gần đây khi chưa có gì
}

// Một nút chọn nhóm ở thanh dưới: biểu tượng + vị trí ô đầu của nhóm trong lưới
private class EmojiTab(val icon: String, val firstIndex: Int)

// Tiêu đề nhóm hiện phía trên lưới: nhóm bắt đầu từ ô thứ firstIndex
private class EmojiSection(val title: String, val firstIndex: Int)

// Lưới cuộn NGANG, mỗi cột 4 emoji; ô là hình vuông (rộng = cao) nên khoảng cách ngang bằng khoảng cách dọc
private const val EMOJI_ROWS = 4

// Ô tìm emoji ở trên cùng (thay thanh gợi ý khi đang ở bảng emoji) + nút tròn nhỏ mở thư viện nhãn dán.
// Ô tìm hiện mới là giao diện; tra tên emoji làm ở bước sau.
@Composable
fun EmojiSearchBar(
    palette: KeyboardPalette,
    height: Dp,
    stickerActive: Boolean,
    onStickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentClick by rememberUpdatedState(onStickerClick)
    Row(modifier.fillMaxWidth().height(height).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(palette.letterKey.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchIcon(palette.secondaryText)
            Spacer(Modifier.width(8.dp))
            Text("Search Emoji", color = palette.secondaryText, fontSize = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(if (stickerActive) palette.letterKey else palette.letterKey.copy(alpha = 0.55f))
                .pointerInput(Unit) { detectTapGestures { currentClick() } },
            contentAlignment = Alignment.Center,
        ) { StickerIcon(if (stickerActive) palette.keyText else palette.secondaryText, size = 20.dp) }
    }
}

// Bảng emoji: lưới cuộn ngang 4 hàng (vuốt qua lại như iOS), tiêu đề nhóm ở trên, thanh nhóm + ABC + xóa ở dưới.
// Đầu lưới là Pinned / Frequently Used (nếu có). Chạm = chèn emoji; giữ lâu = ghim / bỏ ghim.
@Composable
fun EmojiPanel(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    height: Dp,
    onEmoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackToLetters: () -> Unit,
    stickerMode: Boolean,
    onStickerModeChange: (Boolean) -> Unit,
    onSticker: (java.io.File) -> Unit,
    onOpenStickerStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { EmojiStore.get(context) }
    val tone = settings.emojiSkinTone.value.modifier
    val currentOnEmoji by rememberUpdatedState(onEmoji)
    val currentBack by rememberUpdatedState(onBackToLetters)

    // Dựng danh sách ô + vị trí từng nhóm. Dựng lại khi lịch sử / ghim / cách sắp xếp đổi.
    val historyOn = settings.emojiHistory.value
    val pinnedSorting = settings.emojiPinnedSorting.value
    val recentSorting = settings.emojiRecentSorting.value
    // Mỗi ô có key cố định nên khi mục mới chèn lên đầu, lưới giữ nguyên chỗ đang xem
    val (entries, tabs, sections) = remember(store.recents, store.pinned, historyOn, pinnedSorting, recentSorting) {
        val list = mutableListOf<EmojiEntry>()
        val tabList = mutableListOf<EmojiTab>()
        val sectionList = mutableListOf<EmojiSection>()
        var padId = 0
        // đệm cho đủ bội của 4 ô để nhóm sau bắt đầu ở cột mới
        fun padToColumn() { while (list.size % EMOJI_ROWS != 0) list += EmojiEntry.Pad(padId++) }
        // chừa nguyên một cột trống để tách dải lịch sử khỏi các nhóm emoji phía sau
        fun gapColumn() { repeat(EMOJI_ROWS) { list += EmojiEntry.Pad(padId++) } }
        val pins = when (pinnedSorting) {
            PinnedSorting.NEW_FIRST -> store.pinned.reversed()
            PinnedSorting.NEW_LAST -> store.pinned
            PinnedSorting.ALPHABETICAL -> store.pinned.sorted()
        }
        val recents = if (!historyOn) emptyList() else when (recentSorting) {
            RecentSorting.NEWEST_FIRST -> store.recents.map { it.emoji }
            RecentSorting.MOST_USED_FIRST -> store.recents.sortedByDescending { it.count }.map { it.emoji }
        }
        tabList += EmojiTab("🕘", list.size) // luôn có ô đầu cho lịch sử; chưa có gì thì chừa một khoảng trống
        if (pins.isEmpty() && recents.isEmpty()) {
            sectionList += EmojiSection("Frequently Used", list.size)
            list += EmojiEntry.HistorySlot
            padToColumn()
            gapColumn()
        }
        // ghim đứng trước rồi tới dùng gần đây, liền một dải (không đệm trống ở giữa); emoji đã ghim không lặp lại
        if (pins.isNotEmpty() || recents.isNotEmpty()) {
            sectionList += EmojiSection("Frequently Used", list.size)
            pins.forEach { list += EmojiEntry.Item(it, "pin") }
            recents.filter { it !in pins }.forEach { list += EmojiEntry.Item(it, "recent") }
            padToColumn()
            gapColumn()
        }
        EmojiData.categories.forEach { category ->
            tabList += EmojiTab(category.icon, list.size)
            sectionList += EmojiSection(category.title, list.size)
            category.emojis.forEach { list += EmojiEntry.Item(it, category.title) }
            padToColumn()
        }
        Triple(list, tabList, sectionList)
    }

    // Một bộ đo chữ dùng chung có bộ nhớ đệm: ô emoji chỉ vẽ lại kết quả đã đo, không dựng thành phần Text riêng cho từng ô
    val textMeasurer = rememberTextMeasurer(cacheSize = 1024)
    val emojiStyle = remember { TextStyle(fontSize = 27.sp) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // nhóm / tiêu đề đang hiện = cái có ô đầu gần nhất phía trước cột đang ở mép trái lưới
    val activeTab by remember(tabs) {
        derivedStateOf { tabs.indexOfLast { it.firstIndex <= gridState.firstVisibleItemIndex }.coerceAtLeast(0) }
    }
    val activeTitle by remember(sections) {
        derivedStateOf { sections.lastOrNull { it.firstIndex <= gridState.firstVisibleItemIndex }?.title.orEmpty() }
    }

    // chọn nhóm khi đang ở thư viện nhãn dán: thoát ra rồi mới cuộn tới nhóm (lưới phải dựng lại trước)
    var pendingScroll by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(stickerMode, pendingScroll) {
        val target = pendingScroll
        if (!stickerMode && target != null) { gridState.scrollToItem(target); pendingScroll = null }
    }

    Column(modifier.fillMaxWidth().height(height)) {
        if (stickerMode) {
            StickerLibrary(
                settings = settings, palette = palette,
                onPick = onSticker, onCreate = onOpenStickerStudio,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            // tiêu đề nhóm: đổi nhóm thì mờ chuyển sang tên mới
            Box(Modifier.fillMaxWidth().height(24.dp).padding(start = 14.dp), contentAlignment = Alignment.CenterStart) {
                Crossfade(targetState = activeTitle, animationSpec = tween(160), label = "emojiTitle") { title ->
                    Text(
                        title.uppercase(), color = palette.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp, maxLines = 1,
                    )
                }
            }
            LazyHorizontalGrid(
                state = gridState,
                rows = GridCells.Fixed(EMOJI_ROWS),
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(
                    count = entries.size,
                    key = { i -> when (val e = entries[i]) { is EmojiEntry.Item -> "${e.section}:${e.base}"; is EmojiEntry.Pad -> "pad:${e.id}"; EmojiEntry.HistorySlot -> "history" } },
                    span = { i -> if (entries[i] === EmojiEntry.HistorySlot) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                    contentType = { i -> if (entries[i] is EmojiEntry.Item) 1 else 0 },
                ) { i ->
                    when (val e = entries[i]) {
                        is EmojiEntry.Pad -> Spacer(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
                        EmojiEntry.HistorySlot -> Box(Modifier.fillMaxHeight().aspectRatio(3f, matchHeightConstraintsFirst = true).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text("Emojis you use will appear here", color = palette.secondaryText.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        is EmojiEntry.Item -> EmojiCell(
                            base = e.base,
                            display = EmojiData.withTone(e.base, tone),
                            isPinned = e.base in store.pinned || EmojiData.withTone(e.base, tone) in store.pinned,
                            palette = palette,
                            settings = settings,
                            measurer = textMeasurer,
                            style = emojiStyle,
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true),
                            onCommit = { inserted ->
                                currentOnEmoji(inserted)
                                if (settings.emojiHistory.value) store.use(inserted, settings.emojiMaxHistory.value)
                            },
                            onTogglePin = { toPin -> store.togglePin(toPin, settings.emojiMaxPinned.value) },
                        )
                    }
                }
            }
        }
        // thanh dưới: ABC | nhóm | xóa
        Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            var abcDown by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .width(48.dp)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { abcDown = true; tryAwaitRelease(); abcDown = false },
                            onTap = { view.keyTick(settings); currentBack() },
                        )
                    }
                    .pressScale(abcDown),
                contentAlignment = Alignment.Center,
            ) { Text("ABC", color = palette.keyText, fontSize = 16.sp, maxLines = 1, softWrap = false) }
            // mỗi nhóm chia đều phần còn lại của thanh (không bao giờ tràn, dù có thêm nhóm dùng gần đây)
            Row(Modifier.weight(1f).padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == activeTab && !stickerMode
                    val bg by animateColorAsState(if (selected) palette.letterKey else androidx.compose.ui.graphics.Color.Transparent, label = "tab")
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .pointerInput(tab) {
                                detectTapGestures {
                                    if (stickerMode) { pendingScroll = tab.firstIndex; onStickerModeChange(false) }
                                    else scope.launch { gridState.animateScrollToItem(tab.firstIndex) }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { EmojiCategoryIcon(tab.icon, if (selected) palette.keyText else palette.secondaryText, size = 22.dp) }
                }
            }
            FunctionKey(onTap = onBackspace, modifier = Modifier.width(44.dp), repeatOnHold = true, flat = true) { BackspaceIcon(palette.keyText) }
        }
    }
}

// Một ô emoji. Chạm = chèn. Giữ lâu:
//  - emoji có màu da (bàn tay, người...): hiện hàng màu da phía trên, vuốt trái / phải để chọn, thả tay để chèn (có thêm nút ghim 📌)
//  - emoji khác: ghim / bỏ ghim
// Cố ý làm nhẹ (một bộ bắt cử chỉ duy nhất, không theo dõi vị trí, không hoạt ảnh màu) vì cuộn lưới có hàng chục ô cùng lúc.
@Composable
private fun EmojiCell(
    base: String,
    display: String,
    isPinned: Boolean,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    measurer: TextMeasurer,
    style: TextStyle,
    modifier: Modifier,
    onCommit: (String) -> Unit,
    onTogglePin: (String) -> Unit,
) {
    val currentCommit by rememberUpdatedState(onCommit)
    val currentPin by rememberUpdatedState(onTogglePin)
    val currentDisplay by rememberUpdatedState(display)
    val view = LocalView.current
    val density = LocalDensity.current
    var pressed by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var hovered by remember { mutableIntStateOf(-1) }
    val geometry = remember { TonePopupGeometry() }
    val tones = SkinTone.entries

    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed || pickerOpen) palette.letterKey else androidx.compose.ui.graphics.Color.Transparent)
            .pointerInput(base) {
                val itemWpx = TONE_ITEM_W.toPx()
                val padPx = TONE_PAD.toPx()
                val optionCount = tones.size + 1 // 6 màu da + nút ghim
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    val held = awaitLongPressOrCancellation(down.id)
                    if (held == null) {
                        // nhấc tay sớm = chạm (không bị cuộn lưới giành mất); còn đang đè mà trượt đi = cuộn
                        pressed = false
                        val last = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (last != null && !last.pressed && !last.isConsumed) {
                            view.keyTick(settings)
                            currentCommit(currentDisplay)
                        }
                        return@awaitEachGesture
                    }
                    pressed = false
                    view.longPressTick(settings)
                    held.consume()
                    if (!EmojiData.isSkinnable(base)) {
                        currentPin(base)
                        while (true) { // chờ nhấc tay
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                        }
                        return@awaitEachGesture
                    }
                    pickerOpen = true
                    hovered = tones.indexOf(settings.emojiSkinTone.value)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        val rel = geometry.anchorLeft + change.position.x - geometry.left - padPx
                        val index = if (rel >= 0f) (rel / itemWpx).toInt().takeIf { it in 0 until optionCount } ?: -1 else -1
                        if (index != hovered) {
                            hovered = index
                            if (index >= 0) view.selectionTick(settings)
                        }
                        if (!change.pressed) {
                            when {
                                hovered in tones.indices -> currentCommit(EmojiData.withTone(base, tones[hovered].modifier))
                                hovered == tones.size -> currentPin(EmojiData.withTone(base, settings.emojiSkinTone.value.modifier))
                            }
                            break
                        }
                    }
                    pickerOpen = false
                    hovered = -1
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val layout = measurer.measure(display, style)
                onDrawBehind {
                    drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2f, (size.height - layout.size.height) / 2f))
                }
            },
        )
        if (isPinned) Text("📌", fontSize = 9.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp))
        if (pickerOpen) TonePopup(base, hovered, isPinned, palette, geometry)
    }
}

// Vị trí cửa sổ popup (px) để biết ngón tay đang ở ô màu nào
private class TonePopupGeometry {
    var left = 0f        // mép trái popup (px, theo cửa sổ)
    var anchorLeft = 0f  // mép trái ô emoji (px, theo cửa sổ)
}

private val TONE_ITEM_W = 42.dp
private val TONE_PAD = 6.dp

// Hàng chọn màu da nổi phía trên ô emoji; ô đang dưới ngón tay sáng lên
@Composable
private fun TonePopup(base: String, hovered: Int, pinned: Boolean, palette: KeyboardPalette, geometry: TonePopupGeometry) {
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val provider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                geometry.left = x.toFloat()
                geometry.anchorLeft = anchorBounds.left.toFloat()
                return IntOffset(x, anchorBounds.top - popupContentSize.height - gapPx)
            }
        }
    }
    val tones = SkinTone.entries
    Popup(popupPositionProvider = provider) {
        Row(
            Modifier
                .shadow(10.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(palette.popup)
                .padding(TONE_PAD),
        ) {
            tones.forEachIndexed { i, tone ->
                ToneOption(EmojiData.withTone(base, tone.modifier), selected = i == hovered, palette)
            }
            ToneOption(if (pinned) "📍" else "📌", selected = hovered == tones.size, palette)
        }
    }
}

@Composable
private fun ToneOption(label: String, selected: Boolean, palette: KeyboardPalette) {
    Box(
        Modifier
            .size(TONE_ITEM_W, 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.highlight else androidx.compose.ui.graphics.Color.Transparent),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 27.sp) }
}
