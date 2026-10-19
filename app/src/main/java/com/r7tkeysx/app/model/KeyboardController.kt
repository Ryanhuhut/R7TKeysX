package com.r7tkeysx.app.model

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.data.LanguageStore
import com.r7tkeysx.app.ui.theme.IosDimens

// Giữ state dùng chung của bàn phím (Shift + ngôn ngữ), cả app demo lẫn bàn phím hệ thống đều dùng.
class KeyboardController(
    private val settings: KeyboardSettings,
    val languageStore: LanguageStore,
    private val onLanguageChanged: () -> Unit = {},
) {
    var shiftState by mutableStateOf(ShiftState.OFF)
        private set

    var layer by mutableStateOf(KeyboardLayer.LETTERS)
        private set

    // Ngôn ngữ nằm trong LanguageStore (dùng chung với màn Layouts)
    val languages get() = languageStore.enabled
    val languageIndex get() = languageStore.index
    val language get() = languages[languageIndex]

    private var lastShiftTap = 0L // thời điểm chạm Shift gần nhất

    // Chạm Shift: tự đo thời gian giữa 2 lần chạm (không dùng onDoubleTap vì nó làm chậm lần chạm đầu)
    fun onShiftTap() {
        val now = SystemClock.uptimeMillis()
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.SINGLE
            ShiftState.SINGLE ->
                if (settings.capsLockDoubleTap.value && now - lastShiftTap <= IosDimens.DOUBLE_TAP_MS) ShiftState.CAPS_LOCK
                else ShiftState.OFF
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftTap = now
    }

    // Gõ xong một chữ: SINGLE về OFF, CAPS_LOCK giữ nguyên
    fun onLetterTyped() {
        if (shiftState == ShiftState.SINGLE) shiftState = ShiftState.OFF
    }

    // Đầu câu / đầu ô nhập thì tự bật chữ hoa cho chữ kế tiếp (không phá CAPS_LOCK)
    fun autoCapitalize() {
        if (settings.autoCapitalization.value && shiftState == ShiftState.OFF) shiftState = ShiftState.SINGLE
    }

    fun resetShift() {
        shiftState = ShiftState.OFF
    }

    fun showLayer(newLayer: KeyboardLayer) {
        layer = newLayer
    }

    fun selectLanguage(index: Int) {
        val target = languages.getOrNull(index) ?: return
        if (target.id == languageStore.selectedId) return
        languageStore.select(target.id)
        onLanguageChanged()
    }

    fun nextLanguage() = selectLanguage((languageIndex + 1) % languages.size)
}
