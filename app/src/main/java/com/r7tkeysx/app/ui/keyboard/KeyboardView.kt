package com.r7tkeysx.app.ui.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.ui.glass.LocalGlassBackdrop
import com.r7tkeysx.app.data.LocalKeyboardSettings
import com.r7tkeysx.app.model.InputKind
import com.r7tkeysx.app.model.KeyboardLanguage
import com.r7tkeysx.app.model.LanguageCatalog
import com.r7tkeysx.app.model.KeyboardLayer
import com.r7tkeysx.app.model.ShiftState
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.voice.VoiceState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.r7tkeysx.app.ui.theme.Motion
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.r7tkeysx.app.ui.theme.KeyboardPalette
import com.r7tkeysx.app.ui.theme.LocalKeyboardPalette
import com.r7tkeysx.app.ui.theme.R7TTheme
import com.r7tkeysx.app.ui.theme.resolveKeyboardPalette

private const val ROW1 = "qwertyuiop"
private const val ROW2 = "asdfghjkl"
private const val ROW3 = "zxcvbnm"
private const val DIGITS = "1234567890"

// Tầng số và tầng ký hiệu (mỗi chuỗi = một hàng phím)
private const val NUM_ROW2 = "-/:;()$&@\""
private const val SYM_ROW1 = "[]{}#%^*+="
private const val SYM_ROW2 = "_\\|~<>€£¥•"
// Chữ gợi ý ký hiệu trên hàng 2 (a..l) và hàng 3 (z..m)
private const val HINT_ROW2 = "@#\$_&-+()"
private const val HINT_ROW3 = "*\"':;!?"
private const val PUNCT_ROW = ".,?!'" // hàng thứ 3 của cả hai tầng, 5 phím rộng hơn

// Giữ phím cách để rê con trỏ: bàn phím gọi start -> move(dx, dy) nhiều lần (px, theo ngón tay) -> end
class CursorControl(val start: () -> Unit, val move: (dx: Float, dy: Float) -> Unit, val end: () -> Unit)

val LocalCursorControl = compositionLocalOf<CursorControl?> { null }

// Nút return là dấu tích (ô nhập có nút Xong / Gửi / Tìm...) hay mũi tên xuống dòng
val LocalReturnIsAction = compositionLocalOf { false }

// Nửa dưới: bàn phím. Không giữ state gì, mọi thứ do cha truyền xuống; tùy chỉnh đọc từ `settings`.
@Composable
fun KeyboardView(
    settings: KeyboardSettings,
    shiftState: ShiftState,
    layer: KeyboardLayer,
    onLayerChange: (KeyboardLayer) -> Unit,
    languages: List<KeyboardLanguage>,
    languageIndex: Int,
    onNextLanguage: () -> Unit,
    onSelectLanguage: (Int) -> Unit,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onReturn: () -> Unit,
    onSpace: () -> Unit,
    onShiftTap: () -> Unit,
    modifier: Modifier = Modifier,
    kind: InputKind = InputKind.TEXT, // kiểu ô nhập: quyết định bố cục (chữ / số / email / url...)
    suggestions: List<String> = emptyList(), // tối đa 3 ô: trái, giữa, phải
    onSuggestionTap: (Int) -> Unit = {},
    onDismiss: (() -> Unit)? = null, // có thì thanh gợi ý hiện nút ˅ và vuốt xuống để ẩn bàn phím
    voiceState: VoiceState = VoiceState.IDLE, // nhập bằng giọng nói: nút mic sáng xanh khi đang nghe
    voiceLevel: Float = 0f,
    onMic: () -> Unit = {},
    onFloatingBounds: ((Rect?) -> Unit)? = null, // bàn phím nổi: báo vị trí khay (bàn phím hệ thống cần để chỉ nhận chạm trong khay)
    appearKey: Int = 0, // bàn phím nổi: tăng số này mỗi lần bàn phím hiện ra để chạy hiệu ứng trồi lên
    // ép kiểu hiển thị (app thử có 2 chỗ đặt bàn phím: gắn đáy / nổi); null = theo cài đặt
    floatingOverride: Boolean? = null,
    returnIsAction: Boolean = false, // true: nút return hiện dấu tích thay vì mũi tên xuống dòng
    cursorControl: CursorControl? = null, // có thì giữ phím cách để rê con trỏ
    onGlide: ((List<String>) -> Unit)? = null, // vuốt để gõ (chỉ tiếng Anh): nhận các từ đoán được, từ tốt nhất đứng đầu
    onSticker: (java.io.File) -> Unit = {}, // chạm một nhãn dán trong thư viện
    onOpenStickerStudio: () -> Unit = {}, // nút "+" trong thư viện nhãn dán
) {
    var stickerMode by remember { mutableStateOf(false) } // bảng emoji đang hiện thư viện nhãn dán
    LaunchedEffect(layer) { if (layer != KeyboardLayer.EMOJI) stickerMode = false }
    val keyCenters = remember { mutableMapOf<Char, androidx.compose.ui.geometry.Offset>() }
    var glideLive by remember { mutableStateOf<String?>(null) } // đang vuốt: từ đoán tới đâu hiện ở ô trái thanh gợi ý
    // đang rê con trỏ bằng phím cách: các phím mờ nhoè đi (như iPhone)
    var trackpad by remember { mutableStateOf(false) }
    val trackBlur by androidx.compose.animation.core.animateDpAsState(if (trackpad) 14.dp else 0.dp, tween(if (trackpad) 260 else 200), label = "trackBlur")
    val trackDim by animateFloatAsState(if (trackpad) 0.55f else 1f, tween(240), label = "trackDim")
    val wrappedCursor = cursorControl?.let { c ->
        CursorControl(start = { trackpad = true; c.start() }, move = c.move, end = { trackpad = false; c.end() })
    }
    val palette = resolveKeyboardPalette(settings.theme.value)
    val floating = floatingOverride ?: settings.floating.value
    var adjusting by remember { mutableStateOf(false) } // đang ở chế độ chỉnh cỡ bàn phím thường
    val showHandles = remember { mutableStateOf(false) } // bàn phím nổi: đang hiện 4 dấu góc
    // chiều cao một phím: bản ngang thấp hơn; cả hai nhân với cài đặt Key height (%).
    // Bàn phím nổi nhỏ hơn thì phím cũng thấp đi một chút cho cân.
    val floatScale = if (floating) 0.55f + 0.45f * settings.floatWidth.value / 100f else 1f
    // bàn phím NỔI trên màn ngang: giữ dáng bàn phím dọc (quả địa cầu / mic nằm dưới hàng phím, không ra hai bên) nhưng thấp lại cho vừa màn ngang thấp
    val compact = floating && isLandscapeLayoutFor(settings)
    val compactScale = if (compact) 0.72f else 1f
    val keyHeight = (if (isLandscapeLayoutFor(settings) && !floating) IosDimens.LandscapeKeyHeight else IosDimens.KeyHeight) *
        (settings.keyHeightPercent.value / 100f) * floatScale * compactScale
    val bottomRowHeight = 58.dp * compactScale
    val modeActions = KeyboardModeActions(
        floating = floating,
        onFloat = {
            adjusting = false
            if (floating) KeyboardMorph.toDocked() else KeyboardMorph.toFloating()
            settings.floating.set(!floating)
            showHandles.value = !floating // vừa chuyển sang nổi: hiện dấu góc để biết kéo được
        },
        onAdjust = { if (floating) showHandles.value = true else adjusting = true },
    )
    CompositionLocalProvider(
        LocalKeyboardSettings provides settings,
        LocalKeyboardPalette provides palette,
        LocalKeyHeightOverride provides keyHeight,
        LocalKeyboardModeActions provides modeActions,
        LocalCursorControl provides wrappedCursor,
        LocalOnChar provides onChar,
        LocalKeyCenters provides keyCenters,
        LocalReturnIsAction provides returnIsAction,
        // chữ trên phím co theo bề ngang bàn phím (nổi, hoặc thu hẹp để gõ một tay)
        LocalKeyFontScale provides 0.35f + 0.65f * (if (floating) settings.floatWidth.value else settings.dockWidth.value) / 100f,
    ) {
        val trayContent: @Composable (footer: (@Composable () -> Unit)?) -> Unit = { footer ->
        val landscape = isLandscapeLayout() && !floating // bàn phím nổi luôn dùng bố cục dọc (xem `compact`)
        // Adaptive Bottom Corners: góc dưới bo theo góc màn hình điện thoại; bàn phím nổi bo đều 4 góc
        val bottomCorner = 0.dp // khay tràn kín xuống đáy màn (không bo góc dưới, kẻo lộ nền ở hai góc khi nền sáng); các phím vẫn "bay" bên trong
        val trayShape = if (floating) RoundedCornerShape(IosDimens.FloatCorner) else RoundedCornerShape(
            topStart = IosDimens.TrayCorner, topEnd = IosDimens.TrayCorner,
            bottomStart = bottomCorner, bottomEnd = bottomCorner,
        )
        // Enable Blur Effect: khay hơi trong suốt để lộ nền (độ mờ phía sau do cửa sổ bàn phím hệ thống lo)
        val glassBackdrop = LocalGlassBackdrop.current
        // tắt Liquid Glass là tắt luôn hiệu ứng mờ: khay đặc, không vẽ nền phía sau
        val blur = settings.blurEnabled.value && settings.keyboardGlass.value
        val glassTray = blur && glassBackdrop != null
        val trayColor = if (blur) palette.tray.copy(alpha = 0.82f) else palette.tray
        Column(
            modifier
                .fillMaxWidth()
                .clip(trayShape)
                .then(
                    if (glassTray) {
                        // nhìn xuyên và làm mờ màn cài đặt phía sau; Blur Radius là độ mờ (dp)
                        Modifier.drawBackdrop(
                            backdrop = glassBackdrop!!,
                            shape = { trayShape },
                            effects = { blur(settings.blurRadius.value.dp.toPx()) },
                            highlight = null,
                            shadow = null,
                            onDrawSurface = { drawRect(trayColor) },
                        )
                    } else Modifier.background(trayColor),
                )
                // viền sáng rất mảnh ở mép trên, mờ dần xuống dưới (công tắc Glass border)
                .then(
                    if (settings.glassBorder.value) {
                        Modifier.border(0.5.dp, Brush.verticalGradient(listOf(palette.hairline, Color.Transparent)), trayShape)
                    } else Modifier,
                )
                .then(if (kind in NumericKinds || floating) Modifier else Modifier.navigationBarsPadding())
                // Bottom spacing: nâng hàng quả địa cầu / mic lên khỏi đáy điện thoại cho dễ bấm
                .padding(bottom = if (floating) 0.dp else settings.bottomSpacing.value.dp),
        ) {
            // đổi sang / khỏi bảng Clipboard: bảng mới phồng nhẹ + mờ hiện ra, bảng cũ mờ đi (cao bằng nhau nên không giật khung)
            androidx.compose.animation.AnimatedContent(
                targetState = layer == KeyboardLayer.CLIPBOARD,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(tween(240, delayMillis = 50)) +
                        androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(1f, 420f), initialScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f))) togetherWith
                        (androidx.compose.animation.fadeOut(tween(130)) +
                            androidx.compose.animation.scaleOut(tween(180), targetScale = 1f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f))) using
                        androidx.compose.animation.SizeTransform(clip = false)
                },
                label = "clipboardSwap",
            ) { isClip ->
            Column {
            if (kind in NumericKinds) {
                NumericPad(kind, onChar, onBackspace)
                return@Column
            }
            val suggestionHeight = if (landscape) IosDimens.LandscapeSuggestionHeight else IosDimens.SuggestionBarHeight * compactScale
            if (isClip) {
                // bảng Clipboard chiếm trọn khay (thanh gợi ý + 4 hàng phím + hàng quả địa cầu) nên chiều cao bàn phím không đổi
                val rowGap = (if (landscape) IosDimens.LandscapeKeyGapV else IosDimens.KeyGapV) * (settings.verticalSpacing.value / 100f)
                ClipboardPanel(
                    settings = settings,
                    palette = palette,
                    height = suggestionHeight + keyHeight * 4 + rowGap * 3 + bottomRowHeight,
                    landscape = landscape,
                    onPaste = onChar,
                    onBackspace = onBackspace,
                    onBackToLetters = { onLayerChange(KeyboardLayer.LETTERS) },
                )
                return@Column
            }
            if (layer == KeyboardLayer.EMOJI) {
                EmojiSearchBar(palette, suggestionHeight, stickerActive = stickerMode, onStickerClick = { stickerMode = !stickerMode })
            } else if (settings.suggestionBar.value && kind != InputKind.PASSWORD) {
                SuggestionBar(
                    glideLive?.let { listOf(it, "", "") } ?: suggestions, palette, onSuggestionTap, onDismiss,
                    height = suggestionHeight,
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }
            val keysArea: @Composable (Modifier) -> Unit = { areaModifier ->
                BoxWithConstraints(
                    areaModifier
                        .then(if (trackBlur > 0.dp) Modifier.blur(trackBlur, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier)
                        .graphicsLayer { alpha = trackDim },
                ) {
                    // Khe hở mong muốn (Horizontal key spacing) -> độ rộng phím tự nhiên; rồi nhân Key width (%).
                    // Phím hẹp lại thì khe hở tự rộng ra để hàng vẫn đầy; phím không bao giờ rộng tới mức khe < 2dp.
                    val baseGap = IosDimens.KeyGapH * (settings.horizontalSpacing.value / 100f)
                    val naturalKeyW = (maxWidth - baseGap * 9) / 10
                    // Một độ rộng phím chữ duy nhất cho cả các hàng => các hàng rộng bằng nhau
                    val keyW = minOf(naturalKeyW * (settings.keyWidthPercent.value / 100f), (maxWidth - 2.dp * 9) / 10)
                    val gap = (maxWidth - keyW * 10) / 9
                    val wideW = keyW * IosDimens.WIDE_KEY_FACTOR // Shift, Backspace, 123, emoji
                    // hàng 3: 2 phím rộng ôm 7 phím chữ, chừa khe hở rộng hơn ở hai đầu như bàn phím iOS
                    val edgeGap = (maxWidth - keyW * 7 - gap * 6 - wideW * 2) / 2
                    val totalW = maxWidth

                    val gapV = (if (landscape) IosDimens.LandscapeKeyGapV else IosDimens.KeyGapV) * (settings.verticalSpacing.value / 100f)
                    val keyRegistry = remember { KeyRegistry() }
                    CompositionLocalProvider(LocalKeyGap provides androidx.compose.ui.unit.DpSize(gap, gapV), LocalKeyRegistry provides keyRegistry) {
                    Box(Modifier.gapTapCatcher(keyRegistry, maxOf(gap, gapV, edgeGap))) {
                    // đổi tầng chữ <-> số <-> ký hiệu <-> emoji: mờ chéo nhanh thay vì đổi cái rụp
                    AnimatedContent(
                        targetState = layer,
                        transitionSpec = {
                            // bảng emoji / chữ / số đổi chỗ: bảng mới phồng từ 92% + mờ hiện (góc dưới trái, chỗ quả địa cầu), bảng cũ mờ nhanh
                            (fadeIn(tween(230, delayMillis = 40)) + androidx.compose.animation.scaleIn(
                                androidx.compose.animation.core.spring(1f, 460f), initialScale = 0.92f, // 1f = không nảy quá, khỏi tràn khung
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f),
                            )) togetherWith fadeOut(tween(110)) using androidx.compose.animation.SizeTransform(clip = false)
                        },
                        label = "layer",
                    ) { shownLayer ->
                        if (shownLayer == KeyboardLayer.EMOJI) {
                            // bảng emoji cao bằng đúng 4 hàng phím để bàn phím không đổi chiều cao khi chuyển
                            EmojiPanel(
                                settings = settings,
                                palette = palette,
                                height = keyHeight * 4 + gapV * 3,
                                onEmoji = onChar,
                                onBackspace = onBackspace,
                                onBackToLetters = { onLayerChange(KeyboardLayer.LETTERS) },
                                stickerMode = stickerMode,
                                onStickerModeChange = { stickerMode = it },
                                onSticker = onSticker,
                                onOpenStickerStudio = onOpenStickerStudio,
                            )
                        } else {
                            val glideOn = onGlide != null && shownLayer == KeyboardLayer.LETTERS && settings.slideToType.value &&
                                languages.getOrNull(languageIndex)?.label == "EN" && kind == InputKind.TEXT
                            Column(
                                Modifier.glideTyping(glideOn, settings.longPressMs.value.toLong(), onLive = { glideLive = it }) { onGlide?.invoke(it) },
                                verticalArrangement = Arrangement.spacedBy(gapV),
                            ) {
                                if (shownLayer == KeyboardLayer.LETTERS) {
                                    LetterRows(shiftState, palette, settings, keyW, wideW, edgeGap, gap, onChar, onBackspace, onShiftTap)
                                } else {
                                    SymbolRows(shownLayer, palette, keyW, wideW, edgeGap, totalW, gap, onChar, onBackspace, onLayerChange)
                                }
                                BottomKeyRow(
                                    shownLayer, kind, landscape, palette, settings, keyW, wideW, gap, languages[languageIndex.coerceIn(0, languages.lastIndex)].label, onChar,
                                    onToggleLayer = {
                                        onLayerChange(if (shownLayer == KeyboardLayer.LETTERS) KeyboardLayer.NUMBERS else KeyboardLayer.LETTERS)
                                    },
                                    onEmoji = { onLayerChange(KeyboardLayer.EMOJI) },
                                    onSpace = onSpace, onReturn = onReturn,
                                )
                            }
                        }
                    }
                    }
                    }
                }
            }
            if (landscape) {
                // Bản ngang: quả địa cầu ở ngoài trái, mic ngoài phải, thẳng hàng với hàng phím dưới cùng
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    keysArea(Modifier.fillMaxWidth().padding(horizontal = IosDimens.LandscapeSideWidth))
                    Box(
                        Modifier.align(Alignment.BottomStart).width(IosDimens.LandscapeSideWidth)
                            .height(keyHeight).wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                        contentAlignment = Alignment.Center,
                    ) { GlobeKey(languages, languageIndex, onNextLanguage, onSelectLanguage, extras = GlobeExtras, onExtra = { onLayerChange(globeExtraLayer(it)) }) }
                    if (settings.showMicKey.value) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).width(IosDimens.LandscapeSideWidth)
                                .height(keyHeight).wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                            contentAlignment = Alignment.Center,
                        ) {
                            MicKey(voiceState, voiceLevel, onMic)
                        }
                    }
                }
            } else {
                keysArea(Modifier.fillMaxWidth().padding(horizontal = IosDimens.TraySidePadding))
                BottomRow(settings, languages, languageIndex, onNextLanguage, onSelectLanguage, onOpenLayer = onLayerChange, voiceState, voiceLevel, onMic, rowHeight = bottomRowHeight)
            }
            }
            }
            footer?.invoke()
        }
        }
        if (floating) {
            FloatingKeyboard(settings, showHandles, onDismiss, onFloatingBounds, appearKey) { footer -> trayContent(footer) }
        } else {
            DockedKeyboard(settings, adjusting, onAdjustDone = { adjusting = false }) { trayContent(null) }
        }
    }
}

// Bản không-composable-state của isLandscapeLayout, dùng được ngay ở chỗ khai báo CompositionLocal
@Composable
private fun isLandscapeLayoutFor(settings: KeyboardSettings): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        settings.landscapeMode.value == com.r7tkeysx.app.data.LandscapeMode.DYNAMIC

// Các mục thêm ở cuối popup giữ quả địa cầu: Emoji, Clipboard
private val GlobeExtras = listOf("Emoji", "Clipboard")
private fun globeExtraLayer(index: Int) = if (index == 0) KeyboardLayer.EMOJI else KeyboardLayer.CLIPBOARD

// Các hàng chữ (kèm hàng số nếu bật Number Row)
@Composable
private fun LetterRows(
    shiftState: ShiftState,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    keyW: Dp,
    wideW: Dp,
    edgeGap: Dp,
    gap: Dp,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onShiftTap: () -> Unit,
) {
    val upper = shiftState != ShiftState.OFF
    fun shown(c: Char) = if (upper) c.uppercase() else c.toString()
    // chữ gợi ý nhỏ ở góc phím: hàng 1 là số (hoặc ký hiệu nếu tắt Number hints), hàng 2-3 là ký hiệu
    fun hint1(i: Int): String? = when {
        settings.numberHints.value && !settings.numberRow.value -> DIGITS[i].toString()
        settings.symbolHints.value -> SYM_ROW1[i].toString()
        else -> null
    }
    fun hint2(i: Int): String? = if (settings.symbolHints.value) HINT_ROW2[i].toString() else null
    fun hint3(i: Int): String? = if (settings.symbolHints.value) HINT_ROW3[i].toString() else null

    if (settings.numberRow.value) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            DIGITS.forEach { c -> LetterKey(c.toString(), { onChar(c.toString()) }, Modifier.width(keyW)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        ROW1.forEachIndexed { i, c -> LetterKey(shown(c), { onChar(shown(c)) }, Modifier.width(keyW), hint1(i)) }
    }
    Row(
        Modifier.padding(horizontal = (keyW + gap) / 2),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        ROW2.forEachIndexed { i, c -> LetterKey(shown(c), { onChar(shown(c)) }, Modifier.width(keyW), hint2(i)) }
    }
    Row {
        FunctionKey(
            onTap = onShiftTap,
            modifier = Modifier.width(wideW),
            color = if (shiftState == ShiftState.OFF) palette.functionKey else palette.shiftActive,
            pressedColor = if (shiftState == ShiftState.OFF) palette.functionKeyPressed else palette.shiftActivePressed,
        ) {
            ShiftIcon(shiftState, if (shiftState == ShiftState.OFF) palette.keyText else palette.shiftActiveIcon)
        }
        Spacer(Modifier.width(edgeGap))
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            ROW3.forEachIndexed { i, c -> LetterKey(shown(c), { onChar(shown(c)) }, Modifier.width(keyW), hint3(i)) }
        }
        Spacer(Modifier.width(edgeGap))
        BackspaceKey(palette, onBackspace, Modifier.width(wideW))
    }
}

// Tầng số / ký hiệu: 2 hàng 10 phím + hàng [đổi tầng] 5 dấu câu [xóa]
@Composable
private fun SymbolRows(
    layer: KeyboardLayer,
    palette: KeyboardPalette,
    keyW: Dp,
    wideW: Dp,
    edgeGap: Dp,
    totalW: Dp,
    gap: Dp,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onLayerChange: (KeyboardLayer) -> Unit,
) {
    val numbers = layer == KeyboardLayer.NUMBERS
    val row1 = if (numbers) DIGITS else SYM_ROW1
    val row2 = if (numbers) NUM_ROW2 else SYM_ROW2
    // 5 dấu câu chia đều phần còn lại của hàng giữa 2 phím rộng
    val punctW = (totalW - wideW * 2 - edgeGap * 2 - gap * (PUNCT_ROW.length - 1)) / PUNCT_ROW.length

    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        row1.forEach { c -> LetterKey(c.toString(), { onChar(c.toString()) }, Modifier.width(keyW)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        row2.forEach { c -> LetterKey(c.toString(), { onChar(c.toString()) }, Modifier.width(keyW)) }
    }
    Row {
        FunctionKey(
            onTap = { onLayerChange(if (numbers) KeyboardLayer.SYMBOLS else KeyboardLayer.NUMBERS) },
            modifier = Modifier.width(wideW),
        ) {
            FitKeyLabel(if (numbers) "#+=" else "123", palette.keyText)
        }
        Spacer(Modifier.width(edgeGap))
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PUNCT_ROW.forEach { c -> LetterKey(c.toString(), { onChar(c.toString()) }, Modifier.width(punctW)) }
        }
        Spacer(Modifier.width(edgeGap))
        BackspaceKey(palette, onBackspace, Modifier.width(wideW))
    }
}

@Composable
private fun BackspaceKey(palette: KeyboardPalette, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    FunctionKey(onTap = onBackspace, modifier = modifier, repeatOnHold = true) { BackspaceIcon(palette.keyText) }
}

// Hàng dưới: [123 / ABC] [emoji] [phím cách] [return]
@Composable
private fun BottomKeyRow(
    layer: KeyboardLayer,
    kind: InputKind,
    landscape: Boolean,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    keyW: Dp,
    wideW: Dp,
    gap: Dp,
    languageLabel: String,
    onChar: (String) -> Unit,
    onToggleLayer: () -> Unit,
    onEmoji: () -> Unit,
    onSpace: () -> Unit,
    onReturn: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        FunctionKey(onTap = onToggleLayer, modifier = Modifier.width(wideW)) {
            FitKeyLabel(if (layer == KeyboardLayer.LETTERS) "123" else "ABC", palette.keyText)
        }
        if (settings.showEmojiKey.value) {
            FunctionKey(onTap = onEmoji, modifier = Modifier.width(wideW)) {
                EmojiIcon(palette.keyText, featureColor = palette.functionKey)
            }
        }
        when (kind) {
            // Email: phím cách ngắn lại, thêm @ và .
            InputKind.EMAIL -> {
                SpaceKey(languageLabel, palette, onSpace, Modifier.weight(1f))
                LetterKey("@", { onChar("@") }, Modifier.width(wideW))
                LetterKey(".", { onChar(".") }, Modifier.width(wideW))
            }
            // Uri: không cần phím cách, thay bằng / . .com
            InputKind.URI -> {
                LetterKey("/", { onChar("/") }, Modifier.width(wideW))
                LetterKey(".", { onChar(".") }, Modifier.width(wideW))
                FunctionKey(onTap = { onChar(".com") }, modifier = Modifier.weight(1f)) {
                    Text(".com", color = palette.keyText, fontSize = 20.sp * LocalKeyFontScale.current, maxLines = 1, softWrap = false)
                }
            }
            else -> {
                val punct = settings.punctuationKeys.value
                if (punct) LetterKey(",", { onChar(",") }, Modifier.width(keyW))
                SpaceKey(languageLabel, palette, onSpace, Modifier.weight(1f))
                if (punct) LetterKey(".", { onChar(".") }, Modifier.width(keyW))
            }
        }
        FunctionKey(
            onTap = onReturn,
            sound = KeySound.RETURN,
            modifier = Modifier.width(keyW * IosDimens.RETURN_KEY_FACTOR),
            color = palette.returnKey,
            pressedColor = palette.returnKeyPressed,
        ) {
            // ô có nút hành động (Xong / Gửi / Tìm / Đi) -> dấu tích; ô nhiều dòng -> mũi tên xuống dòng ↵
            if (LocalReturnIsAction.current || kind == InputKind.EMAIL || kind == InputKind.URI) {
                CheckIcon(Color.White, size = 26.dp * LocalKeyFontScale.current)
            } else {
                ReturnIcon(Color.White, size = 28.dp * LocalKeyFontScale.current)
            }
        }
    }
}

// Thanh gợi ý 3 cột (ô nào chưa có chữ thì để trống). Chạm một ô có chữ thì báo ra ngoài.
// Nếu có onDismiss: ô bên phải (khi trống) hiện nút ˅, và vuốt xuống trên thanh này cũng ẩn bàn phím.
@Composable
private fun SuggestionBar(
    suggestions: List<String>,
    palette: KeyboardPalette,
    onTap: (Int) -> Unit,
    onDismiss: (() -> Unit)?,
    height: Dp = IosDimens.SuggestionBarHeight,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentDismiss by rememberUpdatedState(onDismiss)
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 4.dp)
            .pointerInput(Unit) {
                var dragged = 0f
                val threshold = 36.dp.toPx()
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { _, dy -> dragged += dy },
                    onDragEnd = { if (dragged > threshold) currentDismiss?.invoke() },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val text = suggestions.getOrNull(i).orEmpty()
            val isDismissCell = i == 2 && text.isEmpty() && onDismiss != null
            var pressed by remember { mutableStateOf(false) }
            val pressAlpha by animateFloatAsState(if (pressed) 0.14f else 0f, tween(if (pressed) 60 else 220), label = "sugPress")
            Box(
                Modifier
                    .weight(1f)
                    .height(height - 8.dp)
                    // chạm: ô sáng lên thành viên bo tròn như iPhone
                    .background(palette.keyText.copy(alpha = pressAlpha), RoundedCornerShape(10.dp))
                    .pointerInput(text, isDismissCell) {
                        if (text.isNotEmpty() || isDismissCell) {
                            detectTapGestures(
                                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                                onTap = { if (isDismissCell) currentDismiss?.invoke() else currentOnTap(i) },
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // emoji / nút ˅ thì mờ chéo cả ô; chữ thì "biến hình" từng ký tự (chữ giống nhau đứng yên, chữ khác mờ sang chữ mới)
                val isEmoji = text.isNotEmpty() && text.none { it.isLetterOrDigit() } && !text.startsWith("Paste")
                AnimatedContent(
                    targetState = when {
                        isDismissCell -> DISMISS_CELL
                        isEmoji -> text
                        text.isEmpty() -> ""
                        else -> WORD_CELL
                    },
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) using androidx.compose.animation.SizeTransform(clip = false) },
                    contentAlignment = Alignment.Center,
                    label = "suggestion$i",
                ) { kindKey ->
                    when (kindKey) {
                        DISMISS_CELL -> ChevronDownIcon(palette.secondaryText)
                        WORD_CELL -> MorphText(
                            // chữ gợi ý mờ nhẹ, ô giữa (gợi ý tốt nhất) rõ hơn một chút
                            text.ifEmpty { " " }, color = palette.keyText.copy(alpha = if (i == 1) 0.82f else 0.62f), fontSize = 16.sp,
                            fontWeight = if (i == 1) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        "" -> Spacer(Modifier.fillMaxWidth())
                        else -> Text(kindKey, fontSize = 24.sp, maxLines = 1)
                    }
                }
            }
            if (i < 2) Box(Modifier.width(0.5.dp).height(22.dp).background(palette.hairline))
        }
    }
}

private const val DISMISS_CELL = "\u0000dismiss"
private const val WORD_CELL = "\u0000word"

// Nhãn chữ trên phím chức năng (123, ABC, #+=): tự co cỡ chữ cho vừa phím khi bàn phím bị thu nhỏ, không bao giờ bị cắt
@Composable
private fun FitKeyLabel(text: String, color: Color) {
    androidx.compose.foundation.text.BasicText(
        text,
        style = androidx.compose.ui.text.TextStyle(color = color, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        maxLines = 1,
        softWrap = false,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 18.sp * LocalKeyFontScale.current.coerceAtLeast(0.6f), stepSize = 0.5.sp),
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

// Phím cách: nhãn ngôn ngữ nhỏ ở góc.
// Chạm: dấu cách. Giữ ~0,3 s: thành "bàn di chuột" như iPhone — bàn phím mờ đi, rê ngón tay để dời con trỏ
// qua từng chữ (trái / phải, lên / xuống dòng). Một khối cử chỉ duy nhất lo cả chạm lẫn giữ + rê.
@Composable
private fun SpaceKey(languageLabel: String, palette: KeyboardPalette, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val settings = LocalKeyboardSettings.current
    val view = LocalView.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentCursor by rememberUpdatedState(LocalCursorControl.current)
    var pressed by remember { mutableStateOf(false) }
    val corner = settings.keyCornerRadius.value.dp
    val shape = RoundedCornerShape(corner)
    val registry = LocalKeyRegistry.current
    val token = remember { Any() }
    androidx.compose.runtime.DisposableEffect(registry) { onDispose { registry?.entries?.remove(token) } }
    Box(
        modifier
            .onGloballyPositioned { c ->
                registry?.entries?.set(token, KeyRegistry.Entry(c.boundsInRoot(), { pressed = it }, { view.keyTick(settings); currentOnTap() }))
            }
            .height(LocalKeyHeightOverride.current ?: (IosDimens.KeyHeight * settings.keyHeightPercent.value / 100f))
            // vùng nhận chạm (nới rộng ra khe hở) đặt TRƯỚC lệnh cắt bo góc, nếu không phần nới bị cắt mất
            .keyPointerInput(LocalKeyGap.current) {
                var trackStarted = false
                awaitEachGesture {
                  try {
                    val down = awaitFirstDown()
                    pressed = true
                    view.keyTick(settings)
                    var released = false
                    val up = withTimeoutOrNull(settings.longPressMs.value.toLong()) {
                        waitForKeyUp().also { released = true }
                    }
                    if (released) {
                        pressed = false
                        if (up != null) currentOnTap()
                        return@awaitEachGesture
                    }
                    val cursor = currentCursor
                    if (cursor == null) {
                        waitForKeyUp()
                        pressed = false
                        return@awaitEachGesture
                    }
                    cursor.start()
                    trackStarted = true
                    view.longPressTick(settings)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val d = change.positionChange()
                        if (d != androidx.compose.ui.geometry.Offset.Zero) cursor.move(d.x, d.y)
                        change.consume()
                    }
                    cursor.end()
                    trackStarted = false
                    pressed = false
                  } finally {
                    pressed = false
                    if (trackStarted) { trackStarted = false; currentCursor?.end() } // bị huỷ giữa lúc rê con trỏ: nhả bàn phím khỏi chế độ mờ
                  }
                }
            }
            .pressScale(pressed)
            .drawBehind {
                drawRoundRect(palette.shadow, topLeft = androidx.compose.ui.geometry.Offset(0f, 1.dp.toPx()), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx()))
            }
            .clip(shape)
            .background(if (pressed) palette.letterKeyPressed else palette.letterKey),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            languageLabel,
            color = palette.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp),
        )
    }
}

// Hàng đáy: quả địa cầu bên trái, mic bên phải (chỉ icon, không có nền phím)
@Composable
private fun BottomRow(
    settings: KeyboardSettings,
    languages: List<KeyboardLanguage>,
    languageIndex: Int,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    onOpenLayer: (KeyboardLayer) -> Unit,
    voiceState: VoiceState,
    voiceLevel: Float,
    onMic: () -> Unit,
    rowHeight: Dp = 58.dp,
) {
    Row(
        Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlobeKey(languages, languageIndex, onNext, onSelect, extras = GlobeExtras, onExtra = { onOpenLayer(globeExtraLayer(it)) })
        // khoảng giữa quả địa cầu và mic: sóng âm thanh nhảy theo giọng nói khi đang nghe
        Box(Modifier.weight(1f).height(40.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = voiceState == VoiceState.LISTENING || voiceState == VoiceState.RECOGNIZING,
                enter = fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(320), initialScale = 0.6f),
                exit = fadeOut(tween(200)) + androidx.compose.animation.scaleOut(tween(200), targetScale = 0.6f),
            ) {
                VoiceWave(voiceLevel, listening = voiceState == VoiceState.LISTENING, Modifier.fillMaxWidth().height(40.dp))
            }
        }
        if (settings.showMicKey.value) {
            MicKey(voiceState, voiceLevel, onMic)
        }
    }
}

// Sóng âm thanh: một dãy vạch tròn đầu, cao ở giữa thấp hai bên, nhảy theo độ to giọng nói.
// Đang nhận diện (đã thôi nghe): vạch thấp, gợn nhẹ như đang "suy nghĩ".
@Composable
private fun VoiceWave(level: Float, listening: Boolean, modifier: Modifier) {
    // độ to thật thường nhỏ, lấy căn bậc hai cho sóng nhìn sống động hơn
    val target = if (listening) kotlin.math.sqrt(level.coerceIn(0f, 1f)) else 0.12f
    val loud by animateFloatAsState(target, tween(110), label = "waveLevel")
    var time by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) androidx.compose.runtime.withFrameNanos { time = (it - start) / 1_000_000_000f }
    }
    androidx.compose.foundation.Canvas(modifier) {
        val bars = 19
        val barW = 3.5.dp.toPx()
        val step = size.width / bars
        val mid = (bars - 1) / 2f
        for (i in 0 until bars) {
            val envelope = 1f - 0.65f * ((i - mid) / mid).let { it * it } // giữa cao, hai đầu thấp
            val wobble = 0.55f + 0.45f * kotlin.math.sin(time * 7.3f + i * 0.9f) * kotlin.math.sin(time * 3.1f + i * 0.41f)
            val h = ((0.14f + 0.86f * loud * envelope * wobble) * size.height).coerceIn(barW, size.height)
            val cx = step * (i + 0.5f)
            drawLine(
                IosColors.Accent.copy(alpha = 0.55f + 0.45f * envelope),
                androidx.compose.ui.geometry.Offset(cx, (size.height - h) / 2f),
                androidx.compose.ui.geometry.Offset(cx, (size.height + h) / 2f),
                barW, androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

// Nút mic: chạm để nói / dừng. Đang nghe: icon xanh, vòng tròn phía sau phồng theo độ to giọng nói.
// Đang nhận diện: vòng tròn mờ đứng yên.
@Composable
private fun MicKey(state: VoiceState, level: Float, onMic: () -> Unit) {
    val palette = LocalKeyboardPalette.current
    val settings = LocalKeyboardSettings.current
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val models = remember { com.r7tkeysx.app.voice.VoiceModelManager.get(context) }
    val currentOnMic by rememberUpdatedState(onMic)
    val listening = state == VoiceState.LISTENING
    val busy = state == VoiceState.RECOGNIZING
    val pulse by animateFloatAsState(if (listening) 0.75f + 0.35f * level else if (busy) 0.8f else 0f, tween(90), label = "mic")
    var down by remember { mutableStateOf(false) }
    var popupVisible by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(-1) }
    // các lựa chọn khi giữ mic: Auto (theo ngôn ngữ bàn phím) + mọi model đã tải; chốt lại mỗi lần mở popup
    var choices by remember { mutableStateOf(emptyList<MicChoice>()) }
    val currentChoices by rememberUpdatedState(choices)

    val itemH = with(density) { IosDimens.PopupItemHeight.toPx() }
    val padV = with(density) { IosDimens.PopupPadding.toPx() }
    val popupW = with(density) { MicPopupWidth.toPx() }
    val gap = with(density) { IosDimens.BubbleGap.toPx() }
    val slack = with(density) { IosDimens.PopupSlack.toPx() }
    val boxW = with(density) { IosDimens.GlobeTouchSize.toPx() }

    fun buildChoices(): List<MicChoice> {
        val list = ArrayList<MicChoice>()
        val smallInstalled = com.r7tkeysx.app.voice.VoiceModel.perLanguageModels.filter { models.isInstalled(it) }
        if (smallInstalled.isNotEmpty()) list += MicChoice("Auto", "Matches your keyboard", null)
        smallInstalled.forEach { list += MicChoice(it.label, it.sizeLabel, it) }
        com.r7tkeysx.app.voice.VoiceModel.multilingualModels.filter { models.isInstalled(it) }.forEach { list += MicChoice(it.label, it.sizeLabel, it) }
        return list
    }
    fun currentIndex(list: List<MicChoice>): Int {
        val multi = settings.voiceMode.value == com.r7tkeysx.app.voice.VoiceMode.MULTILINGUAL
        return list.indexOfFirst { c ->
            when {
                multi -> c.model == settings.voiceMultiModel.value
                c.model == null -> settings.voiceSmallPick.value == com.r7tkeysx.app.voice.VoiceSmallPick.AUTO
                else -> (c.model == com.r7tkeysx.app.voice.VoiceModel.VI && settings.voiceSmallPick.value == com.r7tkeysx.app.voice.VoiceSmallPick.VI) ||
                    (c.model == com.r7tkeysx.app.voice.VoiceModel.EN && settings.voiceSmallPick.value == com.r7tkeysx.app.voice.VoiceSmallPick.EN)
            }
        }
    }
    fun apply(c: MicChoice) {
        val m = c.model
        when {
            m == null -> { settings.voiceMode.set(com.r7tkeysx.app.voice.VoiceMode.ONE_LANGUAGE); settings.voiceSmallPick.set(com.r7tkeysx.app.voice.VoiceSmallPick.AUTO) }
            m.multilingual -> { settings.voiceMode.set(com.r7tkeysx.app.voice.VoiceMode.MULTILINGUAL); settings.voiceMultiModel.set(m) }
            else -> {
                settings.voiceMode.set(com.r7tkeysx.app.voice.VoiceMode.ONE_LANGUAGE)
                settings.voiceSmallPick.set(if (m == com.r7tkeysx.app.voice.VoiceModel.VI) com.r7tkeysx.app.voice.VoiceSmallPick.VI else com.r7tkeysx.app.voice.VoiceSmallPick.EN)
            }
        }
    }

    val provider = remember(gap) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect, windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize,
            ) = androidx.compose.ui.unit.IntOffset(
                (anchorBounds.right - popupContentSize.width).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                anchorBounds.top - popupContentSize.height - gap.toInt(),
            )
        }
    }

    Box(
        Modifier
            .size(IosDimens.GlobeTouchSize)
            // MỘT khối cử chỉ: chạm nhanh = bật / tắt mic; giữ = hiện danh sách model đã tải, trượt để chọn, thả tay để chốt
            .pointerInput(Unit) {
                awaitEachGesture {
                  try {
                    awaitFirstDown()
                    down = true
                    var released = false
                    val list = buildChoices()
                    val wait = if (list.size >= 2 && state != VoiceState.LISTENING) settings.longPressMs.value.toLong() else Long.MAX_VALUE
                    val up = withTimeoutOrNull(wait) { waitForUpOrCancellation().also { released = true } }
                    if (released) {
                        down = false
                        if (up != null) { view.keyTick(settings); currentOnMic() }
                        return@awaitEachGesture
                    }
                    choices = list
                    hovered = currentIndex(list)
                    popupVisible = true
                    view.longPressTick(settings)
                    val popupH = list.size * itemH + padV * 2
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val p = change.position // toạ độ so với góc trên-trái của nút
                        val itemsTop = -gap - popupH + padV
                        val insideX = p.x >= boxW - popupW - slack && p.x <= boxW + slack
                        val rel = (p.y - itemsTop) / itemH
                        val now = if (insideX && rel >= 0f && rel < list.size) rel.toInt() else -1
                        if (now != hovered) { hovered = now; if (now >= 0) view.selectionTick(settings) }
                        change.consume()
                        if (!change.pressed) break
                    }
                    val chosen = hovered
                    down = false
                    popupVisible = false
                    hovered = -1
                    currentChoices.getOrNull(chosen)?.let { apply(it) }
                  } finally {
                    down = false; popupVisible = false; hovered = -1
                  }
                }
            }
            .pressScale(down)
            .drawBehind {
                if (pulse > 0f) drawCircle(IosColors.Accent.copy(alpha = if (listening) 0.28f else 0.16f), radius = size.minDimension / 2f * pulse)
            },
        contentAlignment = Alignment.Center,
    ) {
        MicIcon(if (listening || busy) IosColors.Accent else palette.keyText, size = 30.dp)
        MorphPopup(visible = popupVisible, positionProvider = provider, origin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 1f), width = MicPopupWidth) {
            choices.forEachIndexed { i, c ->
                val on = i == hovered
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(IosDimens.PopupItemHeight)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .then(if (on) Modifier.background(Color.White.copy(alpha = 0.17f)).border(0.6.dp, Color.White.copy(alpha = 0.14f), androidx.compose.foundation.shape.CircleShape) else Modifier)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(c.title, color = palette.popupText, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(c.subtitle, color = palette.popupText.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

private class MicChoice(val title: String, val subtitle: String, val model: com.r7tkeysx.app.voice.VoiceModel?)
private val MicPopupWidth = 232.dp

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 393)
@Composable
private fun KeyboardViewPreview() = R7TTheme {
    KeyboardView(
        settings = KeyboardSettings.get(LocalContext.current),
        shiftState = ShiftState.OFF, layer = KeyboardLayer.LETTERS, onLayerChange = {},
        languages = LanguageCatalog.take(2), languageIndex = 1, onNextLanguage = {}, onSelectLanguage = {},
        onChar = {}, onBackspace = {}, onReturn = {}, onSpace = {}, onShiftTap = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 393)
@Composable
private fun KeyboardViewNumbersPreview() = R7TTheme {
    KeyboardView(
        settings = KeyboardSettings.get(LocalContext.current),
        shiftState = ShiftState.OFF, layer = KeyboardLayer.NUMBERS, onLayerChange = {},
        languages = LanguageCatalog.take(2), languageIndex = 1, onNextLanguage = {}, onSelectLanguage = {},
        onChar = {}, onBackspace = {}, onReturn = {}, onSpace = {}, onShiftTap = {},
    )
}
