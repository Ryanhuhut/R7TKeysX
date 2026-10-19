package com.r7tkeysx.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.r7tkeysx.app.ui.glass.LocalGlassEnabled
import com.r7tkeysx.app.ui.keyboard.preloadKeySounds
import com.r7tkeysx.app.voice.VoiceInput
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.data.LanguageStore
import com.r7tkeysx.app.engine.TelexEngine
import com.r7tkeysx.app.model.InputKind
import com.r7tkeysx.app.model.KeyboardController
import com.r7tkeysx.app.ui.glass.LocalGlassBackdrop
import com.r7tkeysx.app.ui.glass.LocalKeyboardInset
import com.r7tkeysx.app.ui.glass.rememberGlassSource
import com.r7tkeysx.app.ui.keyboard.KeyboardView
import com.r7tkeysx.app.ui.settings.InputTypePopup
import com.r7tkeysx.app.ui.settings.ResetConfirmDialog
import com.r7tkeysx.app.ui.settings.SettingsDetail
import com.r7tkeysx.app.ui.settings.SettingsPage
import com.r7tkeysx.app.ui.settings.SettingsSection
import com.r7tkeysx.app.ui.settings.TryOutBar
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.Motion
import com.r7tkeysx.app.ui.theme.R7TTheme


// Yêu cầu điều hướng từ bên ngoài giao diện (vd. bàn phím hệ thống bảo mở trang giọng nói)
object PendingNav {
    var openVoice by mutableStateOf(false)
}

class MainActivity : ComponentActivity() {
    // true khi R7TKeysX đã được bật trong Cài đặt > Bàn phím; cập nhật mỗi lần quay lại app
    private var keyboardEnabled by mutableStateOf(false)

    // Xin quyền ghi âm (cho nhập bằng giọng nói). Bàn phím hệ thống không tự hỏi được nên mở app này để hỏi.
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleMicRequest(intent)
        setContent {
            R7TTheme {
                R7TKeysXApp(
                    keyboardEnabled = keyboardEnabled,
                    onEnableKeyboard = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleMicRequest(intent)
    }

    private fun handleMicRequest(intent: Intent?) {
        // bàn phím báo "chưa có model giọng nói": mở thẳng trang Voice Input để chọn ngôn ngữ và tải
        if (intent?.getBooleanExtra(EXTRA_OPEN_VOICE, false) == true) {
            intent.removeExtra(EXTRA_OPEN_VOICE)
            PendingNav.openVoice = true
        }
        if (intent?.getBooleanExtra(EXTRA_REQUEST_MIC, false) == true && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            intent.removeExtra(EXTRA_REQUEST_MIC)
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    companion object {
        const val EXTRA_REQUEST_MIC = "request_mic"
        const val EXTRA_OPEN_VOICE = "open_voice"
    }

    override fun onResume() {
        super.onResume()
        RefreshRate.requestMax(this, window) // chạy ở tần số quét cao nhất của máy
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboardEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
    }
}

// Màn demo: nửa trên cài đặt (có các màn con), nửa dưới bàn phím thử ngay trong app
// (bàn phím hệ thống thật nằm ở R7TInputMethodService; hai bên dùng chung cài đặt).
@Composable
fun R7TKeysXApp(keyboardEnabled: Boolean, onEnableKeyboard: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { KeyboardSettings.get(context) }
    val controller = remember {
        KeyboardController(settings, LanguageStore.get(context))
    }
    val voice = remember { VoiceInput.get(context) }
    LaunchedEffect(Unit) { preloadKeySounds(context) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // Lịch sử clipboard trong app demo: ghi lại khi người dùng chép chữ lúc app đang mở
    DisposableEffect(Unit) {
        val manager = context.getSystemService(android.content.ClipboardManager::class.java)
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            com.r7tkeysx.app.data.ClipboardStore.captureSystemClip(context, settings)
        }
        manager?.addPrimaryClipChangedListener(listener)
        com.r7tkeysx.app.data.ClipboardStore.captureSystemClip(context, settings)
        onDispose { manager?.removePrimaryClipChangedListener(listener) }
    }
    var typed by rememberSaveable { mutableStateOf("") } // rememberSaveable: xoay máy không mất chữ
    var cursor by rememberSaveable { mutableIntStateOf(0) } // con trỏ đứng trước ký tự thứ mấy
    // phần chữ trước con trỏ; mọi thao tác gõ / xóa chỉ sửa phần này rồi ghép lại với phần sau
    fun before() = typed.substring(0, cursor.coerceIn(0, typed.length))
    fun edit(change: (String) -> String) {
        val b = before()
        val after = typed.substring(b.length)
        val nb = change(b)
        typed = nb + after
        cursor = nb.length
    }
    var page by remember { mutableStateOf<SettingsPage?>(null) }
    LaunchedEffect(PendingNav.openVoice) {
        if (PendingNav.openVoice) { page = SettingsPage.VoiceInput; PendingNav.openVoice = false }
    }
    // vị trí cuộn của màn cài đặt chính, giữ ở đây để không mất khi mở rồi đóng một mục con
    val mainScroll = androidx.compose.foundation.rememberScrollState()
    var confirmReset by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) } // tấm phủ Add New Keyboard đang mở: ẩn ô Try out
    var keyboardVisible by rememberSaveable { mutableStateOf(false) } // mặc định ẩn để màn cài đặt dùng hết màn hình
    LaunchedEffect(keyboardVisible) { if (!keyboardVisible) voice.cancel() } // ẩn bàn phím thì thôi nghe

    var typePickerVisible by remember { mutableStateOf(false) } // popup chọn kiểu nhập (Text, Number...)
    var inputKind by rememberSaveable { mutableStateOf(InputKind.TEXT) }
    var barHeightPx by remember { mutableStateOf(0) } // chiều cao ô nổi, để đặt popup ngay phía trên
    val density = LocalDensity.current

    // Back: đóng popup, rồi ẩn bàn phím, rồi mới thoát màn con
    BackHandler(enabled = typePickerVisible || keyboardVisible || page != null) {
        if (typePickerVisible) typePickerVisible = false
        else if (keyboardVisible) keyboardVisible = false
        else page = null
    }

    // Đầu ô nhập / đầu câu thì tự bật chữ hoa
    fun refreshAutoCaps() {
        val b = before()
        if (b.isEmpty() || b.endsWith(". ") || b.endsWith("\n")) controller.autoCapitalize()
    }
    LaunchedEffect(Unit) { refreshAutoCaps() }

    // Gõ một ký tự: tiếng Việt thì chạy Telex trên từ đang gõ (các chữ cái ở cuối ô)
    fun onChar(c: String) {
        edit { b ->
            if (controller.language.label == "VI" && c.length == 1 && c[0].isLetter()) {
                val word = b.takeLastWhile { it.isLetter() }
                b.dropLast(word.length) + TelexEngine.type(word, c[0], settings.oldToneStyle.value)
            } else {
                b + c
            }
        }
        controller.onLetterTyped()
    }

    fun onSpace() {
        if (settings.returnToLetters.value) controller.showLayer(com.r7tkeysx.app.model.KeyboardLayer.LETTERS)
        // ". Shortcut": bấm cách lần 2 liền sau "chữ + cách" thì thành ". "
        edit { b ->
            if (settings.periodShortcut.value && b.length >= 2 && b.last() == ' ' && b[b.length - 2].isLetterOrDigit()) return@edit b.dropLast(1) + ". "
            // Sửa chính tả (tiếng Anh): từ vừa gõ sai thì thay bằng từ đúng gần nhất
            if (settings.autoCorrect.value && controller.language.id != "vi") {
                val word = b.takeLastWhile { it.isLetter() }
                val before = b.getOrNull(b.length - word.length - 1)
                val fixed = if (word.isEmpty() || before == '\'' || before?.isDigit() == true) null
                else com.r7tkeysx.app.engine.WordSuggester.forLanguage(controller.language.id)?.correct(word)
                if (fixed != null) return@edit b.dropLast(word.length) + "$fixed "
            }
            "$b "
        }
        refreshAutoCaps()
    }

    // Kính lỏng: màn cài đặt đang cuộn là "nền" (backdrop); ô Try out, bàn phím và các nút kính nhìn xuyên qua nó
    val backdrop = rememberGlassSource()
    val keyboardHeightState = remember { mutableIntStateOf(0) } // chiều cao bàn phím (px); đưa xuống dạng state để chỉ bước đo kích thước đọc nó, không bắt cả app vẽ lại mỗi khung hình
    var overlayHeightPx by remember { mutableIntStateOf(0) }   // ô Try out + bàn phím, để đặt popup ngay phía trên
    // ---- gợi ý chữ: từ đang gõ dở ngay trước con trỏ ----
    LaunchedEffect(Unit) { com.r7tkeysx.app.engine.WordSuggester.preload(context) }
    val currentWord = before().takeLastWhile { it.isLetter() || it == '\'' }.trimStart('\'')
    // vuốt để gõ: từ vừa vuốt ra + các từ đoán khác (chạm để đổi)
    var glideWord by remember { mutableStateOf<String?>(null) }
    var glideAlts by remember { mutableStateOf<List<String>>(emptyList()) }
    val glideShown = glideWord != null && currentWord == glideWord && glideAlts.size > 1
    // emoji hợp với mấy chữ vừa gõ (vd. "hoa héo" -> 🥀) chiếm ô gợi ý bên phải
    val currentEmoji = com.r7tkeysx.app.engine.EmojiSuggester.suggest(before())
    val wordSuggestions = if (glideShown) {
        val others = glideAlts.filter { it != glideWord }
        listOf(others.getOrElse(0) { "" }, glideWord!!, others.getOrElse(1) { "" })
    } else if (currentWord.isEmpty()) {
        if (currentEmoji != null) listOf("", "", currentEmoji) else emptyList()
    } else {
        val found = com.r7tkeysx.app.engine.WordSuggester.forLanguage(controller.language.id)?.suggest(currentWord, 2).orEmpty()
        listOf("\u00AB$currentWord\u00BB", found.getOrElse(0) { "" }, currentEmoji ?: found.getOrElse(1) { "" })
    }

    // ---- giữ phím cách để rê con trỏ trong ô Try out ----
    var tracking by remember { mutableStateOf(false) }
    var trackOffset by remember { mutableFloatStateOf(0f) } // quãng rê còn dư (px), chưa đủ một chữ
    val stepPx = with(density) { 9.dp.toPx() }
    val lineStepPx = with(density) { 26.dp.toPx() } // rê dọc chừng này = lên / xuống 1 dòng
    val tryOutLayout = remember { arrayOfNulls<androidx.compose.ui.text.TextLayoutResult>(1) } // bố cục chữ của ô Try out
    var trackY by remember { mutableFloatStateOf(0f) }
    // dời con trỏ lên (-1) / xuống (+1) một dòng, giữ gần đúng vị trí ngang cũ
    fun moveLine(dir: Int) {
        val l = tryOutLayout[0]?.takeIf { it.layoutInput.text.length == typed.length } ?: return
        val line = l.getLineForOffset(cursor.coerceIn(0, typed.length))
        val target = line + dir
        if (target < 0 || target >= l.lineCount) return
        val x = l.getHorizontalPosition(cursor.coerceIn(0, typed.length), true)
        val y = (l.getLineTop(target) + l.getLineBottom(target)) / 2f
        cursor = l.getOffsetForPosition(androidx.compose.ui.geometry.Offset(x, y)).coerceIn(0, typed.length)
    }
    val cursorControl = remember(stepPx) {
        com.r7tkeysx.app.ui.keyboard.CursorControl(
            start = { tracking = true; trackOffset = 0f; trackY = 0f },
            move = { dx, dy ->
                trackY += dy
                while (trackY >= lineStepPx) { moveLine(1); trackY -= lineStepPx }
                while (trackY <= -lineStepPx) { moveLine(-1); trackY += lineStepPx }
                var acc = trackOffset + dx // đi đều theo ngón tay, không gia tốc
                while (acc >= stepPx && cursor < typed.length) { cursor = nextGrapheme(typed, cursor); acc -= stepPx }
                while (acc <= -stepPx && cursor > 0) { cursor = prevGrapheme(typed, cursor); acc += stepPx }
                trackOffset = acc.coerceIn(-stepPx, stepPx) // tới đầu / cuối chữ thì con trỏ chỉ nhích thêm một chút
            },
            end = { tracking = false; trackOffset = 0f; refreshAutoCaps() },
        )
    }
    val floating = settings.floating.value
    // bàn phím trong app (dùng ở 2 chỗ: gắn dưới ô Try out, hoặc nổi trên cả màn)
    val keyboard: @Composable (Boolean) -> Unit = { floatingSlot ->
                KeyboardView(
                    settings = settings,
                    shiftState = controller.shiftState,
                    layer = controller.layer,
                    onLayerChange = controller::showLayer,
                    languages = controller.languages,
                    languageIndex = controller.languageIndex,
                    onNextLanguage = controller::nextLanguage,
                    onSelectLanguage = controller::selectLanguage,
                    onChar = ::onChar,
                    onBackspace = { edit { if (com.r7tkeysx.app.ui.keyboard.HoldDelete.byWord) dropLastWord(it) else dropLastGrapheme(it) } },
                    onReturn = { edit { "$it\n" }; refreshAutoCaps() },
                    onSpace = ::onSpace,
                    onShiftTap = controller::onShiftTap,
                    kind = inputKind,
                    onDismiss = { keyboardVisible = false },
                    suggestions = voice.statusText?.let { listOf("", it, "") } ?: wordSuggestions,
                    onSuggestionTap = { i ->
                        if (glideShown) {
                            val chosen = wordSuggestions.getOrNull(i).orEmpty()
                            val gw = glideWord!!
                            if (chosen.isNotEmpty() && chosen != gw) { edit { b -> b.dropLast(gw.length) + chosen }; glideWord = chosen }
                            return@KeyboardView
                        }
                        if (i == 2 && currentEmoji != null && voice.statusText == null) {
                            // emoji: thêm ngay sau chữ, giữ nguyên chữ đã gõ
                            edit { b -> b + (if (b.isEmpty() || b.last().isWhitespace()) "" else " ") + currentEmoji + " " }
                            return@KeyboardView
                        }
                        val word = currentWord
                        val chosen = if (i == 0) word else wordSuggestions.getOrNull(i).orEmpty()
                        if (word.isNotEmpty() && chosen.isNotEmpty() && voice.statusText == null) {
                            edit { b -> b.dropLast(word.length) + chosen + " " }
                            refreshAutoCaps()
                        }
                    },
                    cursorControl = cursorControl,
                    onOpenStickerStudio = { context.startActivity(android.content.Intent(context, StickerStudioActivity::class.java)) },
                    onSticker = { android.widget.Toast.makeText(context, "Nhãn dán chỉ gửi được qua bàn phím hệ thống", android.widget.Toast.LENGTH_SHORT).show() },
                    onGlide = { words ->
                        val cased = words.map { w ->
                            when (controller.shiftState) {
                                com.r7tkeysx.app.model.ShiftState.CAPS_LOCK -> w.uppercase()
                                com.r7tkeysx.app.model.ShiftState.SINGLE -> w.replaceFirstChar { it.uppercase() }
                                else -> w
                            }
                        }
                        edit { b -> b + (if (b.isEmpty() || b.last().isWhitespace()) "" else " ") + cased[0] }
                        glideWord = cased[0]
                        glideAlts = cased
                        controller.onLetterTyped()
                    },
                    floatingOverride = floatingSlot,
                    voiceState = voice.state,
                    voiceLevel = voice.level,
                    onMic = {
                        if (!voice.hasMicPermission()) askMic.launch(Manifest.permission.RECORD_AUDIO)
                        else voice.toggle { text ->
                            // dính chữ trước thì thêm cách; đầu ô / đầu câu thì viết hoa
                            edit { b ->
                                val startOfSentence = b.isBlank() || b.trimEnd().lastOrNull() in listOf('.', '!', '?')
                                val t = if (startOfSentence) text.replaceFirstChar { it.uppercase() } else text
                                b + if (b.isEmpty() || b.last().isWhitespace()) t else " $t"
                            }
                            refreshAutoCaps()
                        }
                    },
                )
    }
    CompositionLocalProvider(
        // nền cho kính: cần khi màn cài đặt HOẶC bàn phím đang dùng kính (mỗi bên tự xét công tắc của mình)
        LocalGlassBackdrop provides if (settings.glassEffects.value || settings.keyboardGlass.value) backdrop else null,
        LocalGlassEnabled provides settings.glassEffects.value,
        LocalKeyboardInset provides keyboardHeightState,
    ) {
        Box(Modifier.fillMaxSize().background(IosColors.Background)) {
            // Chuyển trang kiểu iOS: trang con trượt vào từ phải, trang cũ lùi nhẹ sang trái; quay lại thì ngược lại
            AnimatedContent(
                targetState = page,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val offsetSpec = tween<IntOffset>(Motion.PAGE_MS, easing = Motion.Decelerate)
                    val fadeSpec = tween<Float>(Motion.PAGE_MS, easing = Motion.Decelerate)
                    when {
                        initialState == null && targetState != null ->
                            (slideInHorizontally(offsetSpec) { it } togetherWith
                                (slideOutHorizontally(offsetSpec) { -it / 4 } + fadeOut(fadeSpec, targetAlpha = 0.4f)))
                                .apply { targetContentZIndex = 1f }
                        targetState == null ->
                            ((slideInHorizontally(offsetSpec) { -it / 4 } + fadeIn(fadeSpec, initialAlpha = 0.4f)) togetherWith
                                slideOutHorizontally(offsetSpec) { it })
                                .apply { targetContentZIndex = -1f }
                        else -> fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
                    }
                },
                label = "page",
            ) { shownPage ->
                if (shownPage == null) {
                    SettingsSection(
                        layoutCount = controller.languages.size,
                        settings = settings,
                        keyboardEnabled = keyboardEnabled,
                        onEnableKeyboard = onEnableKeyboard,
                        onOpenPage = { if (it == SettingsPage.Reset) confirmReset = true else page = it },
                        modifier = Modifier.fillMaxSize(),
                        scroll = mainScroll,
                    )
                } else {
                    SettingsDetail(
                        page = shownPage,
                        settings = settings,
                        controller = controller,
                        onOpenKeyboardSettings = onEnableKeyboard,
                        onBack = { page = null },
                        onSheetChange = { sheetOpen = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            // Ô Try out + bàn phím nằm chồng lên phía dưới màn cài đặt (không đẩy nội dung lên), ô nhập đi theo mép trên bàn phím
            AnimatedVisibility(
                visible = !sheetOpen,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(Motion.BAR_MS)) + slideInVertically(tween(Motion.BAR_MS, easing = Motion.Decelerate)) { it / 3 },
                exit = fadeOut(tween(Motion.BAR_MS / 2)) + slideOutVertically(tween(Motion.BAR_MS, easing = Motion.Decelerate)) { it / 3 },
            ) {
              Column(Modifier.onSizeChanged { overlayHeightPx = it.height }) {
                TryOutBar(
                    text = typed,
                    onClear = { typed = ""; cursor = 0; refreshAutoCaps() },
                    cursor = cursor,
                    onTextLayout = { tryOutLayout[0] = it },
                    tracking = tracking,
                    // chạm ô: hiện / ẩn bàn phím với kiểu đang chọn; chỉ chạm icon bàn phím mới mở popup chọn kiểu
                    onToggleKeyboard = { keyboardVisible = !keyboardVisible },
                    onPickType = { keyboardVisible = false; typePickerVisible = !typePickerVisible },
                    modifier = Modifier
                        // bàn phím ẩn thì ô nổi nằm trên thanh điều hướng của máy
                        .then(if (keyboardVisible) Modifier else Modifier.navigationBarsPadding())
                        .padding(horizontal = IosDimens.ScreenPadding, vertical = 16.dp),
                )
                Box(Modifier.onSizeChanged { keyboardHeightState.intValue = it.height }) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = keyboardVisible && !floating,
                        // vừa trượt vừa co/giãn chiều cao: ô nhập đi theo cùng nhịp, không để lộ khoảng đen rồi giật
                        // vừa đổi qua lại với bàn phím nổi: không trượt, để hiệu ứng "bóp / nở" giữa hai bàn phím lo
                        enter = if (com.r7tkeysx.app.ui.keyboard.KeyboardMorph.pending) androidx.compose.animation.EnterTransition.None
                        else slideInVertically(tween(Motion.KEYBOARD_MS, easing = Motion.Decelerate)) { it } + expandVertically(tween(Motion.KEYBOARD_MS, easing = Motion.Decelerate), clip = false),
                        exit = if (floating) androidx.compose.animation.ExitTransition.None
                        else slideOutVertically(tween(Motion.KEYBOARD_MS, easing = Motion.Decelerate)) { it } + shrinkVertically(tween(Motion.KEYBOARD_MS, easing = Motion.Decelerate), clip = false),
                    ) { keyboard(false) }
                }
              }
            }
            // Bàn phím nổi: nằm trên cả màn (không chiếm chỗ dưới ô Try out), tự đặt vị trí bên trong
            AnimatedVisibility(
                visible = keyboardVisible && floating && !sheetOpen,
                modifier = Modifier.fillMaxSize(),
                enter = androidx.compose.animation.EnterTransition.None, // bàn phím nổi tự trồi từ dưới lên
                exit = if (floating) fadeOut(tween(Motion.BAR_MS)) else androidx.compose.animation.ExitTransition.None,
            ) { keyboard(true) }
            // luôn dựng popup để có hiệu ứng đóng; bên trong tự ẩn khi visible = false
            run {
                // overlayHeightPx gồm ô Try out với khoảng đệm 16dp trên/dưới; bỏ đệm trên rồi chừa 6dp hở cho popup
                InputTypePopup(
                    visible = typePickerVisible && !sheetOpen,
                    onSelect = { inputKind = it; typePickerVisible = false; keyboardVisible = true },
                    onDismiss = { typePickerVisible = false },
                    bottomOffset = with(density) { overlayHeightPx.toDp() } - 10.dp,
                )
            }
            AnimatedVisibility(
                visible = confirmReset,
                enter = fadeIn(tween(Motion.DIALOG_MS)) + scaleIn(tween(Motion.DIALOG_MS, easing = Motion.Decelerate), initialScale = 0.9f),
                exit = fadeOut(tween(Motion.DIALOG_MS / 2)) + scaleOut(tween(Motion.DIALOG_MS / 2, easing = Motion.Decelerate), targetScale = 0.95f),
            ) {
                ResetConfirmDialog(
                    onCancel = { confirmReset = false },
                    onConfirm = {
                        settings.resetAll()
                        controller.languageStore.reset()
                        controller.resetShift()
                        confirmReset = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// Vị trí "ký tự nhìn thấy" kế tiếp / trước đó (emoji nhiều mã vẫn tính là một)
private fun nextGrapheme(text: String, at: Int): Int {
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(text)
    val n = it.following(at.coerceIn(0, text.length - 1).coerceAtLeast(0))
    return if (n == java.text.BreakIterator.DONE) text.length else n
}

private fun prevGrapheme(text: String, at: Int): Int {
    if (at <= 0) return 0
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(text)
    val p = it.preceding(at.coerceAtMost(text.length))
    return if (p == java.text.BreakIterator.DONE) 0 else p
}

// Xóa nguyên một "ký tự nhìn thấy" ở cuối (emoji gồm nhiều ký tự mã hóa, xóa lẻ từng cái sẽ ra ô vuông hỏng)
private fun dropLastGrapheme(text: String): String {
    if (text.isEmpty()) return text
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(text)
    val start = it.preceding(text.length)
    return text.substring(0, if (start == java.text.BreakIterator.DONE) 0 else start)
}

// Xóa nguyên một từ ở cuối + các dấu cách sát ngay sau từ đó (không lan sang từ trước)
private fun dropLastWord(text: String): String {
    val body = text.trimEnd()
    return body.dropLastWhile { !it.isWhitespace() }
}
