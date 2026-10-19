package com.r7tkeysx.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.model.KeyboardController
import com.r7tkeysx.app.model.KeyboardLanguage
import com.r7tkeysx.app.model.LanguageCatalog
import com.r7tkeysx.app.ui.glass.GlassButton
import com.r7tkeysx.app.ui.glass.BottomInsetSpacer
import com.r7tkeysx.app.ui.glass.glassSource
import com.r7tkeysx.app.ui.keyboard.ChevronIcon
import com.r7tkeysx.app.ui.keyboard.CloseIcon
import com.r7tkeysx.app.ui.keyboard.CheckIcon
import com.r7tkeysx.app.ui.keyboard.TrashIcon
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TrashRed = Color(0xFFEB5545)
private val SwipeCommit = 84.dp          // vuốt sang trái quá chừng này là xóa
private const val SwipeFlingVelocity = 2400f // hoặc hất rất nhanh hơn chừng này (px/giây)
private val LanguageRowHeight = 90.dp
private val CardCorner = 30.dp

// Màn Layouts: danh sách bàn phím đang dùng (chạm = chọn, vuốt trái hoặc Edit = xóa),
// nút Add New Keyboard mở tấm phủ chọn ngôn ngữ, và phần kiểu đặt dấu tiếng Việt ở dưới.
@Composable
fun LayoutsScreen(
    settings: KeyboardSettings,
    controller: KeyboardController,
    onBack: () -> Unit,
    onSheetChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = controller.languageStore
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val currentSheetChange by rememberUpdatedState(onSheetChange)
    LaunchedEffect(adding) { currentSheetChange(adding) }
    DisposableEffect(Unit) { onDispose { currentSheetChange(false) } }
    BackHandler(enabled = adding) { adding = false }

    // chỉ còn một bàn phím thì thoát chế độ Edit (không còn gì để xóa)
    if (store.enabled.size <= 1 && editing) editing = false

    val scroll = rememberScrollState()
    Box(modifier) {
        Box(Modifier.fillMaxSize().glassSource(IosColors.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .padding(horizontal = IosDimens.ScreenPadding),
        ) {
            Spacer(Modifier.height(IosDimens.TopBarHeight + 12.dp))
            LargeTitle("Layouts")
            // các dòng có mặt lúc mở màn thì hiện sẵn; dòng thêm sau đó mới có hiệu ứng bung ra
            val initialIds = remember { store.enabled.map { it.id }.toSet() }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardCorner)).background(IosColors.Card).animateContentSize()) {
                store.enabled.forEachIndexed { i, lang ->
                    // key: mỗi dòng giữ trạng thái vuốt riêng, xóa một dòng không làm dòng sau bị "mở" theo
                    key(lang.id) {
                        val visibleState = remember { MutableTransitionState(lang.id in initialIds).apply { targetState = true } }
                        // xóa: thu dòng lại trước, xong hiệu ứng mới thật sự bỏ khỏi danh sách
                        val removeNow = visibleState.isIdle && !visibleState.currentState && !visibleState.targetState
                        LaunchedEffect(removeNow) { if (removeNow) store.remove(lang.id) }
                        AnimatedVisibility(
                            visibleState = visibleState,
                            enter = expandVertically(tween(Motion.ROW_MS, easing = Motion.Decelerate)) + fadeIn(tween(Motion.ROW_MS)),
                            exit = shrinkVertically(tween(Motion.ROW_MS, easing = Motion.Decelerate)) + fadeOut(tween(Motion.ROW_MS / 2)),
                        ) {
                            Column {
                                if (i > 0) Divider()
                                LanguageRow(
                                    lang = lang,
                                    active = lang.id == store.selectedId,
                                    editing = editing,
                                    canDelete = store.enabled.size > 1,
                                    onSelect = { store.select(lang.id) },
                                    onDelete = { visibleState.targetState = false },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            AddButton { adding = true }
            Spacer(Modifier.height(IosDimens.SectionGap + 8.dp))
            Group("Vietnamese Tone Marks", "Where the tone mark goes in vowel pairs such as oa, oe and uy.") {
                CheckRow("New style", !settings.oldToneStyle.value, { settings.oldToneStyle.set(false) }, subtitle = "hoà · khoẻ · thuỷ")
                Divider()
                CheckRow("Old style", settings.oldToneStyle.value, { settings.oldToneStyle.set(true) }, subtitle = "hòa · khỏe · thủy")
            }
            BottomInsetSpacer(130.dp) // chừa chỗ cho ô Try out nổi + bàn phím ở dưới
        }
        }
        DetailTopBar("Layouts", onBack, Modifier.align(Alignment.TopCenter), titleAlpha = rememberTitleAlpha(scroll), trailing = {
            if (store.enabled.size > 1) {
                GlassButton(
                    onClick = { editing = !editing },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp).height(50.dp),
                ) {
                    Text(if (editing) "Done" else "Edit", color = IosColors.PrimaryText, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 22.dp))
                }
            }
        })
        AnimatedVisibility(
            visible = adding,
            enter = slideInVertically(tween(Motion.SHEET_MS, easing = Motion.Decelerate)) { it },
            exit = slideOutVertically(tween(Motion.SHEET_MS * 3 / 4, easing = Motion.Decelerate)) { it },
        ) {
            AddKeyboardSheet(
                added = store.enabled,
                onPick = { store.add(it); adding = false },
                onClose = { adding = false },
            )
        }
    }
}

// Một dòng ngôn ngữ. Vuốt sang trái lộ nút xóa đỏ; ở chế độ Edit có nút trừ đỏ bên trái.
@Composable
private fun LanguageRow(
    lang: KeyboardLanguage,
    active: Boolean,
    editing: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val currentSelect by rememberUpdatedState(onSelect)
    val currentDelete by rememberUpdatedState(onDelete)
    val commitPx = with(LocalDensity.current) { SwipeCommit.toPx() }
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) } // 0 = đứng yên, âm = đang vuốt sang trái
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var committed by remember { mutableStateOf(false) }

    // thả tay không đủ xa: trượt về chỗ cũ, đi nhanh rồi chậm dần
    fun settleBack() {
        scope.launch { animate(offsetX, 0f, animationSpec = tween(Motion.SETTLE_MS, easing = Motion.Decelerate)) { v, _ -> offsetX = v } }
    }

    // vuốt đủ xa (hoặc hất mạnh) sang trái: dòng trượt hẳn ra ngoài rồi xóa luôn, không cần bấm thêm gì
    fun commitDelete() {
        committed = true
        scope.launch {
            animate(offsetX, -rowWidthPx, animationSpec = tween(Motion.SWIPE_OUT_MS, easing = Motion.Decelerate)) { v, _ -> offsetX = v }
            currentDelete()
        }
    }

    Box(Modifier.fillMaxWidth().height(LanguageRowHeight).onSizeChanged { rowWidthPx = it.width.toFloat() }) {
        if (canDelete) {
            // vùng đỏ lộ ra rộng dần theo ngón tay; biểu tượng thùng rác nằm ở mép trái của vùng đỏ
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val w = (-offsetX).roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, constraints.maxHeight))
                        layout(w, placeable.height) { placeable.place(0, 0) }
                    }
                    .background(TrashRed),
                contentAlignment = Alignment.CenterStart,
            ) { TrashIcon(Color.White, modifier = Modifier.padding(start = 34.dp)) }
        }
        Row(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxSize()
                .background(IosColors.Card)
                .draggable(
                    state = rememberDraggableState { dx -> if (!committed) offsetX = (offsetX + dx).coerceIn(-rowWidthPx, 0f) },
                    orientation = Orientation.Horizontal,
                    enabled = canDelete && !editing && !committed,
                    onDragStopped = { velocity -> if (offsetX <= -commitPx || velocity < -SwipeFlingVelocity) commitDelete() else settleBack() },
                )
                .pointerInput(Unit) {
                    detectTapGestures { if (offsetX == 0f) currentSelect() }
                }
                .padding(start = if (editing && canDelete) 14.dp else IosDimens.CardPadding + 4.dp, end = IosDimens.CardPadding + 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing && canDelete) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(TrashRed)
                        .pointerInput(Unit) { detectTapGestures { currentDelete() } },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.width(12.dp).height(2.5.dp).background(Color.White)) }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(lang.name, color = IosColors.PrimaryText, fontSize = 21.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(lang.subtitle, color = IosColors.SecondaryText, fontSize = 17.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            if (active) {
                CheckIcon(IosColors.Accent)
                Spacer(Modifier.width(12.dp))
            }
            ChevronIcon(IosColors.SecondaryText)
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val currentClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(CircleShape)
            .background(if (pressed) IosColors.RowPressed else IosColors.Card)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { currentClick() },
                )
            }
            .padding(start = 28.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text("Add New Keyboard", color = IosColors.Accent, fontSize = 21.sp) }
}

// Tấm phủ chọn ngôn ngữ để thêm: "Suggested" (tiếng Việt) rồi "Other Keyboards"
@Composable
private fun AddKeyboardSheet(added: List<KeyboardLanguage>, onPick: (KeyboardLanguage) -> Unit, onClose: () -> Unit) {
    val currentClose by rememberUpdatedState(onClose)
    val currentPick by rememberUpdatedState(onPick)
    val available = LanguageCatalog.filter { lang -> added.none { it.id == lang.id } }
    val suggested = available.filter { it.id == "vi" }
    val others = available - suggested.toSet()
    val sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) { // chặn chạm xuyên xuống dưới
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 10.dp)
                .clip(sheetShape)
                .background(IosColors.Card),
        ) {
            Box(Modifier.fillMaxWidth().height(IosDimens.TopBarHeight + 12.dp).padding(horizontal = IosDimens.ScreenPadding)) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(IosColors.SignInPill)
                        .border(1.dp, IosColors.SignInBorder, CircleShape)
                        .pointerInput(Unit) { detectTapGestures { currentClose() } },
                    contentAlignment = Alignment.Center,
                ) { CloseIcon(IosColors.PrimaryText) }
                Text(
                    "Add New Keyboard", color = IosColors.PrimaryText, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = IosDimens.ScreenPadding - 4.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                if (suggested.isNotEmpty()) {
                    SheetGroup("Suggested Keyboards", suggested) { currentPick(it) }
                }
                if (others.isNotEmpty()) {
                    SheetGroup("Other Keyboards", others) { currentPick(it) }
                }
                if (available.isEmpty()) {
                    Text("All available keyboards have been added.", color = IosColors.SecondaryText, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp))
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SheetGroup(header: String, items: List<KeyboardLanguage>, onPick: (KeyboardLanguage) -> Unit) {
    Text(
        header, color = IosColors.SecondaryText, fontSize = 19.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardCorner)).background(IosColors.TypePopup)) {
        items.forEachIndexed { i, lang ->
            if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 20.dp).height(0.5.dp).background(IosColors.Divider))
            var pressed by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(if (pressed) IosColors.Divider else Color.Transparent)
                    .pointerInput(lang.id) {
                        detectTapGestures(
                            onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                            onTap = { onPick(lang) },
                        )
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(lang.name, color = IosColors.PrimaryText, fontSize = 21.sp, maxLines = 1) }
        }
    }
    Spacer(Modifier.height(30.dp))
}
