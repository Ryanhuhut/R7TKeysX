package com.r7tkeysx.app.ui.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.View
import com.r7tkeysx.app.R
import com.r7tkeysx.app.data.HapticStrength
import com.r7tkeysx.app.data.KeyboardSettings

// Ba loại tiếng phím, lấy từ res/raw (tap.ogg, backspace.ogg, enter.ogg)
enum class KeySound { TAP, DELETE, RETURN }

// Bộ phát tiếng phím: nạp sẵn 3 file ngắn vào SoundPool (phát gần như tức thì, không trễ như MediaPlayer)
private object KeySoundPlayer {
    private var pool: SoundPool? = null
    private val ids = IntArray(KeySound.entries.size)

    fun play(context: Context, sound: KeySound) {
        ensure(context).play(ids[sound.ordinal], VOLUME, VOLUME, 1, 0, 1f)
    }

    // Nạp file là việc chạy ngầm, nên nạp trước khi bàn phím hiện để tiếng phím đầu tiên không bị mất
    fun ensure(context: Context): SoundPool = pool ?: SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // phát theo âm lượng hệ thống: máy để rung / im lặng thì tiếng phím cũng tắt theo
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_LOW_LATENCY) // đường phát độ trễ thấp, tiếng ra gần cùng lúc với chữ
                    .build(),
            )
            .build()
            .also { p ->
                val app = context.applicationContext
                ids[KeySound.TAP.ordinal] = p.load(app, R.raw.tap, 1)
                ids[KeySound.DELETE.ordinal] = p.load(app, R.raw.backspace, 1)
                ids[KeySound.RETURN.ordinal] = p.load(app, R.raw.enter, 1)
                pool = p
            }

    private const val VOLUME = 0.7f // 0..1, nhân với âm lượng hệ thống
}

fun preloadKeySounds(context: Context) {
    KeySoundPlayer.ensure(context)
}

// Rung + âm thanh khi chạm phím. Gom về một chỗ; độ mạnh và bật/tắt lấy từ cài đặt.
fun View.keyTick(settings: KeyboardSettings, sound: KeySound = KeySound.TAP) {
    if (settings.keySound.value) KeySoundPlayer.play(context, sound)
    if (!settings.haptic.value) return
    performHapticFeedback(
        when (settings.hapticStrength.value) {
            HapticStrength.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
            HapticStrength.MEDIUM -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticStrength.STRONG -> HapticFeedbackConstants.LONG_PRESS
        },
    )
}

// Đổi mục khi trượt trong popup
fun View.selectionTick(settings: KeyboardSettings) {
    if (settings.haptic.value) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

// Popup vừa mở
fun View.longPressTick(settings: KeyboardSettings) {
    if (settings.haptic.value) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
