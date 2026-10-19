package com.r7tkeysx.app

import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.InputMethodService.Insets
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.data.LanguageStore
import com.r7tkeysx.app.engine.TelexEngine
import com.r7tkeysx.app.model.KeyboardController
import com.r7tkeysx.app.model.KeyboardLayer
import com.r7tkeysx.app.ui.keyboard.KeyboardView
import com.r7tkeysx.app.ui.keyboard.preloadKeySounds
import com.r7tkeysx.app.ui.theme.R7TTheme
import com.r7tkeysx.app.voice.VoiceInput

// Bàn phím hệ thống thật: hiện ở mọi app. Giao diện dùng lại KeyboardView; chữ gõ ra gửi qua InputConnection
// ("đường dây" từ bàn phím tới ô nhập của app đang mở).
class R7TInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    // Compose cần "vòng đời" và "kho lưu state" gắn vào cửa sổ; dịch vụ bàn phím không có sẵn nên tự cấp
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var controller: KeyboardController
    private lateinit var settings: KeyboardSettings
    private lateinit var voice: VoiceInput

    // Từ tiếng Việt đang soạn (gạch chân trong ô nhập, chưa chốt); rỗng nếu không soạn gì
    private var composing by mutableStateOf("")
    private var lastCorrection: Pair<String, String>? = null // (chữ đã gõ, chữ đã sửa) để Backspace ngay sau đó trả về chữ gốc
    private var rejectedCorrection: String? = null // chữ vừa được trả lại: bấm cách tiếp thì không sửa nữa
    // Chữ vừa sao chép, đưa lên thanh gợi ý để dán một chạm (chỉ khi bật Clipboard Suggestion)
    private var clipText by mutableStateOf<String?>(null)
    // Lịch sử clipboard: ghi lại mỗi lần người dùng chép chữ (bàn phím được phép đọc clipboard khi đang là bàn phím hiện hành)
    private val clipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        com.r7tkeysx.app.data.ClipboardStore.captureSystemClip(this, settings)
    }
    // Bàn phím nổi: khay nằm ở đâu trong cửa sổ bàn phím (null = đang gắn đáy)
    private var floatingBounds: android.graphics.Rect? = null
    private var plainMode = false
    private var currentWord by mutableStateOf("") // từ đang gõ dở ngay trước con trỏ (để gợi ý chữ)
    private var glideWord by mutableStateOf<String?>(null) // từ vừa vuốt ra (để đổi sang từ đoán khác)
    private var glideAlts by mutableStateOf<List<String>>(emptyList())
    private var currentEmoji by mutableStateOf<String?>(null) // emoji hợp với mấy chữ vừa gõ (ô gợi ý bên phải)
    private var returnIsAction by mutableStateOf(false) // ô có nút Xong / Gửi / Tìm: nút return hiện dấu tích
    private var showCount by mutableIntStateOf(0) // tăng mỗi lần bàn phím hiện ra (để bàn phím nổi chạy hiệu ứng trồi lên) // ô mật khẩu / email / url: gõ thẳng, không Telex

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        settings = KeyboardSettings.get(this)
        voice = VoiceInput.get(this)
        com.r7tkeysx.app.engine.WordSuggester.preload(this)
        preloadKeySounds(this)
        getSystemService(android.content.ClipboardManager::class.java)?.addPrimaryClipChangedListener(clipListener)
        controller = KeyboardController(settings, LanguageStore.get(this)) {
            commitComposing() // đổi ngôn ngữ giữa chừng thì chốt từ đang soạn trước
        }
    }

    override fun onCreateInputView(): View {
        val decor = window.window!!.decorView
        decor.setViewTreeLifecycleOwner(this)
        decor.setViewTreeSavedStateRegistryOwner(this)
        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // Blur Radius: làm mờ nền phía sau cửa sổ bàn phím (Android 12 trở lên, tùy máy có hỗ trợ không)
                // (tắt Liquid Glass ở trang Themes là tắt luôn hiệu ứng này)
                // Bàn phím nổi: cửa sổ phủ cả màn nên làm mờ nền sẽ nhòe luôn cả app phía sau => tắt mờ nền khi đang nổi
                val blur = settings.blurEnabled.value && settings.keyboardGlass.value && !settings.floating.value
                LaunchedEffect(blur, settings.blurRadius.value) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val radius = if (blur) settings.blurRadius.value * resources.displayMetrics.density else 0f
                        window.window?.setBackgroundBlurRadius(radius.toInt())
                    }
                }
                R7TTheme {
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
                    onBackspace = ::onBackspace,
                    onReturn = ::onReturn,
                    onSpace = ::onSpace,
                    onShiftTap = controller::onShiftTap,
                    suggestions = voice.statusText?.let { listOf("", it, "") }
                        ?: wordSuggestions().takeIf { it.isNotEmpty() }
                        ?: listOf(
                            if (composing.isEmpty()) clipText?.let { "Paste: " + it.replace('\n', ' ').take(24) }.orEmpty() else "",
                            composing,
                            "",
                        ),
                    returnIsAction = returnIsAction,
                    cursorControl = cursorControl,
                    onGlide = ::onGlide,
                    onSticker = ::onSticker,
                    onOpenStickerStudio = ::openStickerStudio,
                    onSuggestionTap = ::onSuggestionTap,
                    onDismiss = { requestHideSelf(0) },
                    voiceState = voice.state,
                    voiceLevel = voice.level,
                    onMic = ::onMic,
                    appearKey = showCount,
                    onFloatingBounds = { rect ->
                        floatingBounds = rect?.let { android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
                        window.window?.decorView?.requestLayout() // tính lại vùng nhận chạm
                    },
                )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (!restarting) showCount++
        window.window?.let { RefreshRate.requestMax(this, it) } // cửa sổ bàn phím cũng xin tần số quét cao nhất
        composing = ""
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        plainMode = cls != InputType.TYPE_CLASS_TEXT || variation in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
        )
        // gợi ý dán = mục chép gần nhất, còn trong thời hạn "Limit clipboard suggestions to"
        com.r7tkeysx.app.data.ClipboardStore.captureSystemClip(this, settings)
        clipText = if (settings.clipboardSuggestion.value) {
            com.r7tkeysx.app.data.ClipboardStore.get(this).latestWithin(settings.clipSuggestionSeconds.value)?.text
        } else null
        controller.resetShift()
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        returnIsAction = info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 &&
            action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        refreshCurrentWord()
        // ô nhập số / điện thoại thì mở thẳng tầng số
        val startLayer = if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) KeyboardLayer.NUMBERS else KeyboardLayer.LETTERS
        // "Hide history on next text field" tắt: sang ô nhập khác vẫn giữ bảng Clipboard đang mở
        if (!(controller.layer == KeyboardLayer.CLIPBOARD && !settings.clipHideOnNextField.value)) controller.showLayer(startLayer)
        updateAutoCaps()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        voice.cancel() // bàn phím ẩn: thôi nghe, không gõ chữ vào ô khác
        commitComposing()
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // người dùng chạm sang chỗ khác trong ô nhập => bỏ từ đang soạn (đã nằm sẵn trong ô)
        if (composing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            composing = ""
            currentInputConnection?.finishComposingText()
        }
        refreshCurrentWord()
    }

    // Bàn phím nổi: cửa sổ bàn phím phủ cả màn nhưng app bên dưới vẫn dùng trọn màn hình
    // (không bị đẩy lên), và chỉ vùng khay nổi mới nhận chạm; chạm chỗ khác đi thẳng xuống app.
    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        if (!settings.floating.value) return
        val decor = window.window?.decorView ?: return
        outInsets.contentTopInsets = decor.height
        outInsets.visibleTopInsets = decor.height
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        outInsets.touchableRegion.setEmpty()
        floatingBounds?.let { outInsets.touchableRegion.set(it) }
    }

    override fun onEvaluateFullscreenMode() = false // không phủ kín màn hình khi xoay ngang

    override fun onDestroy() {
        getSystemService(android.content.ClipboardManager::class.java)?.removePrimaryClipChangedListener(clipListener)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    // ---- xử lý phím ----

    private fun onChar(text: String) {
        lastCorrection = null
        glideWord = null
        val ic = currentInputConnection ?: return
        val isVietnamese = controller.language.label == "VI" && !plainMode
        if (isVietnamese && text.length == 1 && text[0].isLetter()) {
            composing = TelexEngine.type(composing, text[0], settings.oldToneStyle.value)
            ic.setComposingText(composing, 1)
        } else {
            commitComposing()
            ic.commitText(text, 1)
        }
        controller.onLetterTyped()
    }

    private fun onSpace() {
        lastCorrection = null
        if (settings.returnToLetters.value) controller.showLayer(KeyboardLayer.LETTERS)
        commitComposing()
        val ic = currentInputConnection ?: return
        // ". Shortcut": bấm cách lần 2 liền sau "chữ + cách" thì thành ". "
        if (settings.periodShortcut.value) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                ic.endBatchEdit()
                updateAutoCaps()
                return
            }
        }
        // Sửa chính tả (chỉ tiếng Anh, ô nhập thường)
        if (settings.autoCorrect.value && !plainMode && controller.language.id != "vi") {
            val before = ic.getTextBeforeCursor(MAX_RESUME_WORD, 0)?.toString().orEmpty()
            val word = before.takeLastWhile { it.isLetter() }
            val charBefore = before.getOrNull(before.length - word.length - 1)
            val fixed = if (word.isEmpty() || word == rejectedCorrection || charBefore == '\'' || charBefore?.isDigit() == true) null
            else com.r7tkeysx.app.engine.WordSuggester.forLanguage(controller.language.id)?.correct(word)
            if (fixed != null) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(word.length, 0)
                ic.commitText("$fixed ", 1)
                ic.endBatchEdit()
                lastCorrection = word to fixed
                updateAutoCaps()
                return
            }
        }
        ic.commitText(" ", 1)
        updateAutoCaps()
    }

    private fun onReturn() {
        lastCorrection = null
        commitComposing()
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = info?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action) // ô tìm kiếm, gửi, đi tới...
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        updateAutoCaps()
    }

    private fun onBackspace() {
        val ic = currentInputConnection ?: return
        // vừa tự sửa chữ: Backspace trả lại chữ gốc người dùng đã gõ
        lastCorrection?.let { (orig, fixed) ->
            lastCorrection = null
            if (ic.getTextBeforeCursor(fixed.length + 1, 0)?.toString() == "$fixed ") {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(fixed.length + 1, 0)
                ic.commitText(orig, 1)
                ic.endBatchEdit()
                rejectedCorrection = orig
                return
            }
        }
        if (com.r7tkeysx.app.ui.keyboard.HoldDelete.byWord && ic.getSelectedText(0).isNullOrEmpty()) {
            // giữ rất lâu: xóa nguyên một từ sát trái con trỏ + dấu cách đứng ngay sau từ đó
            commitComposing()
            val before = ic.getTextBeforeCursor(MAX_RESUME_WORD * 4, 0)?.toString().orEmpty()
            val body = before.trimEnd()
            val word = body.takeLastWhile { !it.isWhitespace() }
            val n = (before.length - body.length) + word.length
            if (n > 0) ic.deleteSurroundingText(n, 0)
            updateAutoCaps()
            return
        }
        if (composing.isNotEmpty()) {
            composing = composing.dropLast(1)
            if (composing.isEmpty()) ic.commitText("", 1) else ic.setComposingText(composing, 1)
            return
        }
        val isVietnamese = controller.language.label == "VI" && !plainMode
        if (!isVietnamese || ic.getSelectedText(0)?.isNotEmpty() == true) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            return
        }
        // Xóa 1 ký tự, rồi "mở lại" từ nằm sát trước con trỏ để gõ tiếp dấu được
        // (co + cách, xóa cách, bấm s => "có"). Gói trong một lượt sửa để ô nhập chỉ báo cập nhật một lần.
        ic.beginBatchEdit()
        ic.deleteSurroundingText(1, 0)
        val before = ic.getTextBeforeCursor(MAX_RESUME_WORD, 0)?.toString().orEmpty()
        val word = before.takeLastWhile { it.isLetter() }
        if (word.isNotEmpty()) {
            ic.deleteSurroundingText(word.length, 0)
            ic.setComposingText(word, 1)
            composing = word
        }
        ic.endBatchEdit()
    }

    private fun openStickerStudio() {
        startActivity(Intent(this, StickerStudioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // Gửi nhãn dán: ô nhập nhận được ảnh thì chèn thẳng; không thì chép vào bộ nhớ tạm để người dùng dán
    private fun onSticker(file: java.io.File) {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.stickers", file)
        val accepts = info != null && androidx.core.view.inputmethod.EditorInfoCompat.getContentMimeTypes(info)
            .any { android.content.ClipDescription.compareMimeTypes("image/png", it) }
        if (accepts) {
            val content = androidx.core.view.inputmethod.InputContentInfoCompat(
                uri, android.content.ClipDescription("sticker", arrayOf("image/png")), null,
            )
            val sent = androidx.core.view.inputmethod.InputConnectionCompat.commitContent(
                ic, info!!, content, androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null,
            )
            if (sent) return
        }
        getSystemService(android.content.ClipboardManager::class.java)
            ?.setPrimaryClip(android.content.ClipData.newUri(contentResolver, "sticker", uri))
        android.widget.Toast.makeText(this, "Đã chép nhãn dán. Giữ ô nhập rồi chọn Dán.", android.widget.Toast.LENGTH_LONG).show()
    }

    // Mic: chưa cho phép ghi âm thì mở app để xin quyền (bàn phím không tự hỏi quyền được)
    private fun onMic() {
        if (!voice.hasMicPermission()) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // app đang chạy thì nhận intent mới qua onNewIntent
                    .putExtra(MainActivity.EXTRA_REQUEST_MIC, true),
            )
            return
        }
        if (!voice.isModelInstalled()) {
            // chưa tải model giọng nói: mở app ở trang Voice Input để chọn ngôn ngữ và tải
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // app đang chạy thì nhận intent mới qua onNewIntent
                    .putExtra(MainActivity.EXTRA_OPEN_VOICE, true),
            )
            return
        }
        commitComposing()
        voice.toggle(::commitVoiceText)
    }

    // Gõ ra một đoạn nói: thêm dấu cách nếu dính chữ trước, viết hoa nếu đang ở đầu câu
    private fun commitVoiceText(text: String) {
        val ic = currentInputConnection ?: return
        commitComposing()
        val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
        val info = currentInputEditorInfo
        val capsMode = info != null && ic.getCursorCapsMode(info.inputType) != 0
        var out = if (capsMode) text.replaceFirstChar { it.uppercase() } else text
        if (before.isNotEmpty() && !before.last().isWhitespace()) out = " $out"
        ic.commitText(out, 1)
        updateAutoCaps()
    }

    private fun commitComposing() {
        if (composing.isEmpty()) return
        composing = ""
        currentInputConnection?.finishComposingText()
    }

    // ---- gợi ý chữ ----

    private fun refreshCurrentWord() {
        currentEmoji = if (plainMode) null else currentInputConnection?.let { ic ->
            if (ic.getSelectedText(0)?.isNotEmpty() == true) null
            else com.r7tkeysx.app.engine.EmojiSuggester.suggest(ic.getTextBeforeCursor(60, 0)?.toString().orEmpty())
        }
        currentWord = when {
            plainMode -> ""
            composing.isNotEmpty() -> composing
            else -> {
                val ic = currentInputConnection
                if (ic == null || ic.getSelectedText(0)?.isNotEmpty() == true) "" else
                    ic.getTextBeforeCursor(MAX_RESUME_WORD, 0)?.toString().orEmpty().takeLastWhile { it.isLetter() || it == '\'' }.trimStart('\'')
            }
        }
    }

    // [«đang gõ», gợi ý 1 (tốt nhất), gợi ý 2]; rỗng nếu không có gì để gợi ý
    // ô phải nhường cho emoji nếu mấy chữ vừa gõ có emoji hợp (vd. "hoa héo" -> 🥀)
    private fun wordSuggestions(): List<String> {
        val word = currentWord
        // vừa vuốt ra một từ: hiện các từ đoán khác để chạm đổi
        if (glideWord != null && word == glideWord && glideAlts.size > 1) {
            val others = glideAlts.filter { it != word }
            return listOf(others.getOrElse(0) { "" }, word, others.getOrElse(1) { "" })
        }
        val emoji = currentEmoji
        if (word.isEmpty()) return if (emoji != null) listOf(clipText?.let { "Paste: " + it.replace('\n', ' ').take(24) }.orEmpty(), "", emoji) else emptyList()
        val found = com.r7tkeysx.app.engine.WordSuggester.forLanguage(controller.language.id)?.suggest(word, 2).orEmpty()
        return listOf("\u00AB$word\u00BB", found.getOrElse(0) { "" }, emoji ?: found.getOrElse(1) { "" })
    }

    // Chạm emoji gợi ý: thêm emoji ngay sau chữ (giữ nguyên chữ đã gõ), cách một dấu cách
    private fun insertEmoji(emoji: String) {
        val ic = currentInputConnection ?: return
        commitComposing()
        val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
        ic.commitText((if (before.isEmpty() || before.last().isWhitespace()) "" else " ") + emoji + " ", 1)
    }

    // Chạm gợi ý: thay từ đang gõ bằng từ được chọn + dấu cách (ô trái «...» là giữ nguyên chữ đã gõ)
    // Vuốt để gõ: chèn từ tốt nhất (cách từ trước một dấu cách), viết hoa theo Shift
    private fun onGlide(words: List<String>) {
        val ic = currentInputConnection ?: return
        commitComposing()
        val cased = words.map { w ->
            when (controller.shiftState) {
                com.r7tkeysx.app.model.ShiftState.CAPS_LOCK -> w.uppercase()
                com.r7tkeysx.app.model.ShiftState.SINGLE -> w.replaceFirstChar { it.uppercase() }
                else -> w
            }
        }
        val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
        ic.commitText((if (before.isEmpty() || before.last().isWhitespace()) "" else " ") + cased[0], 1)
        glideWord = cased[0]
        glideAlts = cased
        controller.onLetterTyped()
        refreshCurrentWord()
    }

    private fun applySuggestion(index: Int): Boolean {
        val gw = glideWord
        if (gw != null && currentWord == gw && glideAlts.size > 1 && voice.statusText == null) {
            val chosen = wordSuggestions().getOrNull(index).orEmpty()
            if (chosen.isNotEmpty() && chosen != gw) {
                val ic = currentInputConnection ?: return true
                ic.beginBatchEdit(); ic.deleteSurroundingText(gw.length, 0); ic.commitText(chosen, 1); ic.endBatchEdit()
                glideWord = chosen
                refreshCurrentWord()
            }
            return true
        }
        val emoji = currentEmoji
        if (index == 2 && emoji != null && voice.statusText == null) { insertEmoji(emoji); return true }
        val word = currentWord
        if (word.isEmpty() || voice.statusText != null) return false
        val ic = currentInputConnection ?: return true
        val chosen = if (index == 0) word else wordSuggestions().getOrNull(index).orEmpty()
        if (chosen.isEmpty()) return true
        ic.beginBatchEdit()
        if (composing.isNotEmpty()) {
            ic.setComposingText(chosen, 1)
            composing = ""
            ic.finishComposingText()
        } else {
            ic.deleteSurroundingText(word.length, 0)
            ic.commitText(chosen, 1)
        }
        ic.commitText(" ", 1)
        ic.endBatchEdit()
        updateAutoCaps()
        return true
    }

    // ---- giữ phím cách để rê con trỏ ----
    private var trackX = 0f
    private var trackY = 0f
    private val cursorControl = com.r7tkeysx.app.ui.keyboard.CursorControl(
        start = { commitComposing(); trackX = 0f; trackY = 0f },
        move = { dx, dy ->
            val density = resources.displayMetrics.density
            // đi đều theo ngón tay (không gia tốc, không trôi): rê bao xa thì con trỏ đi bấy nhiêu chữ
            trackX += dx
            trackY += dy
            val stepX = density * TRACK_STEP_X_DP
            val stepY = density * TRACK_STEP_Y_DP
            while (trackX >= stepX) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT); trackX -= stepX }
            while (trackX <= -stepX) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT); trackX += stepX }
            while (trackY >= stepY) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN); trackY -= stepY }
            while (trackY <= -stepY) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP); trackY += stepY }
        },
        end = { refreshCurrentWord(); updateAutoCaps() },
    )

    private fun onSuggestionTap(index: Int) {
        if (applySuggestion(index)) return
        val text = clipText ?: return
        if (index != 0 || composing.isNotEmpty()) return
        currentInputConnection?.commitText(text, 1)
        clipText = null
    }

    private fun readClipboard(): String? {
        val cm = getSystemService(ClipboardManager::class.java) ?: return null
        val text = runCatching { cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() }.getOrNull()
        return text?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_CLIP }
    }

    // Đầu câu thì tự bật chữ hoa cho chữ kế tiếp
    private fun updateAutoCaps() {
        val info = currentInputEditorInfo ?: return
        if (info.inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return
        val caps = currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0
        if (caps != 0) controller.autoCapitalize()
    }

    private companion object {
        const val MAX_RESUME_WORD = 24 // từ dài hơn thế thì không mở lại
        const val MAX_CLIP = 500 // đoạn sao chép dài hơn thế thì không gợi ý
        const val TRACK_STEP_X_DP = 9f // rê ngang chừng này (dp) = con trỏ dời 1 chữ
        const val TRACK_STEP_Y_DP = 26f // rê dọc chừng này = lên / xuống 1 dòng
    }
}
