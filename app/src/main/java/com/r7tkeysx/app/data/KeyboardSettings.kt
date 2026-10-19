package com.r7tkeysx.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) { SYSTEM("Follow System"), DARK("Dark"), LIGHT("Light") }

enum class KeyFontWeight(val label: String) { REGULAR("Regular"), MEDIUM("Medium"), BOLD("Bold") }

// Ưu tiên khi chữ gợi ý (số / ký hiệu nhỏ trên phím) trùng với phím khác
enum class HintPriority(val label: String) { SMART("Smart priority"), HINT_FIRST("Hint first"), KEY_FIRST("Key first") }

// Màu da của emoji: nhãn, màu chấm hiện trong cài đặt, ký tự điều chỉnh màu da (rỗng = vàng mặc định)
enum class SkinTone(val label: String, val color: Long, val modifier: String) {
    DEFAULT("Default", 0xFFFFC83D, ""),
    LIGHT("Light", 0xFFF7D6C0, "\uD83C\uDFFB"),
    MEDIUM_LIGHT("Medium-Light", 0xFFDDA67F, "\uD83C\uDFFC"),
    MEDIUM("Medium", 0xFFB9855B, "\uD83C\uDFFD"),
    MEDIUM_DARK("Medium-Dark", 0xFF8A5A3A, "\uD83C\uDFFE"),
    DARK("Dark", 0xFF4B3228, "\uD83C\uDFFF"),
}

enum class PinnedSorting(val label: String) { NEW_FIRST("Manual, new items first"), NEW_LAST("Manual, new items last"), ALPHABETICAL("Alphabetical") }

enum class RecentSorting(val label: String) { NEWEST_FIRST("Automatic, newest first"), MOST_USED_FIRST("Automatic, most used first") }

// Đồng bộ clipboard hệ thống <-> clipboard riêng của bàn phím, theo từng chiều
enum class ClipSync(val label: String, val description: String) {
    NONE("No events", "No events are synced in this direction. In this mode, sync excludes text content."),
    CLEAR_ONLY("Only clear events", "Only events clearing clip data are synced in this direction. In this mode, sync excludes text content."),
    SET_ONLY("Only set events", "Only events setting clip data are synced in this direction. In this mode, sync includes text content."),
    ALL("All events", "Both events setting and clearing clip data are synced in this direction. In this mode, sync includes text content.");

    val includesSet get() = this == SET_ONLY || this == ALL
    val includesClear get() = this == CLEAR_ONLY || this == ALL
}

enum class EmojiTrigger(val label: String) { LEADING_COLON("Leading colon"), ALWAYS("Always") }

enum class LandscapeMode(val label: String) { DYNAMIC("Show Dynamically"), PORTRAIT_ONLY("Portrait Only") }

enum class HapticStrength(val label: String) { LIGHT("Light"), MEDIUM("Medium"), STRONG("Strong") }

// Một cài đặt bật/tắt: giá trị là "state" (đổi là giao diện tự cập nhật) và tự lưu vào máy
class BoolSetting(private val prefs: SharedPreferences, private val key: String, private val default: Boolean) {
    var value by mutableStateOf(prefs.getBoolean(key, default))
        private set

    fun set(v: Boolean) {
        value = v
        prefs.edit().putBoolean(key, v).apply()
    }

    fun reset() = set(default)
}

// Một cài đặt dạng số nguyên trong khoảng [min, max] (thanh trượt)
class IntSetting(
    private val prefs: SharedPreferences,
    private val key: String,
    private val default: Int,
    val min: Int,
    val max: Int,
) {
    var value by mutableStateOf(prefs.getInt(key, default).coerceIn(min, max))
        private set

    fun set(v: Int) {
        value = v.coerceIn(min, max)
        prefs.edit().putInt(key, value).apply()
    }

    fun reset() = set(default)
}

// Một cài đặt chọn 1 trong nhiều (enum)
class ChoiceSetting<E : Enum<E>>(
    private val prefs: SharedPreferences,
    private val key: String,
    private val entries: List<E>,
    private val default: E,
) {
    var value by mutableStateOf(entries.firstOrNull { it.name == prefs.getString(key, null) } ?: default)
        private set

    fun set(v: E) {
        value = v
        prefs.edit().putString(key, v.name).apply()
    }

    fun reset() = set(default)
}

// Toàn bộ tùy chỉnh của bàn phím. Chỉ có MỘT bản trong cả app (app cài đặt và bàn phím hệ thống
// chạy cùng tiến trình), nên chỉnh ở màn cài đặt là bàn phím đổi ngay.
class KeyboardSettings private constructor(private val prefs: SharedPreferences) {
    private val resetters = mutableListOf<() -> Unit>()

    private fun bool(key: String, default: Boolean) = BoolSetting(prefs, key, default).also { resetters += it::reset }
    private fun int(key: String, default: Int, min: Int, max: Int) =
        IntSetting(prefs, key, default, min, max).also { resetters += it::reset }
    private fun <E : Enum<E>> choice(key: String, entries: List<E>, default: E) =
        ChoiceSetting(prefs, key, entries, default).also { resetters += it::reset }

    // Themes
    val theme = choice("theme", ThemeMode.entries, ThemeMode.SYSTEM)

    // Keyboard > Fonts
    val fontWeight = choice("font_weight", KeyFontWeight.entries, KeyFontWeight.REGULAR)
    val fontSizePortrait = int("font_size_portrait", 100, 70, 150) // % cỡ chữ trên phím
    val fontSizeLandscape = int("font_size_landscape", 100, 70, 150)

    // Keyboard > Layout
    val glassEffects = bool("glass_effects", true) // kính lỏng của MÀN CÀI ĐẶT (nút, công tắc, thanh trượt, ô Try out)
    val keyboardGlass = bool("keyboard_glass", true) // kính lỏng / làm mờ của BÀN PHÍM
    val blurEnabled = bool("blur_enabled", false)
    val blurRadius = int("blur_radius", 12, 0, 100)
    val adaptiveCorners = bool("adaptive_corners", true)
    val glassBorder = bool("glass_border", true)
    val numberHints = bool("number_hints", false)
    val numberHintPriority = choice("number_hint_priority", HintPriority.entries, HintPriority.SMART)
    val symbolHints = bool("symbol_hints", false)
    val symbolHintPriority = choice("symbol_hint_priority", HintPriority.entries, HintPriority.SMART)
    val punctuationKeys = bool("punctuation_keys", false) // phím , và . cạnh phím cách
    val landscapeMode = choice("landscape_mode", LandscapeMode.entries, LandscapeMode.DYNAMIC)
    val verticalSpacing = int("vertical_spacing", 72, 50, 150) // % khoảng cách dọc giữa các hàng
    val horizontalSpacing = int("horizontal_spacing", 69, 50, 150)
    val keyHeightPercent = int("key_height_percent", 101, 60, 140) // % chiều cao phím
    val keyWidthPercent = int("key_width_percent", 100, 70, 110)   // % chiều rộng phím (nhỏ hơn 100 thì khe hở rộng ra)
    val bottomSpacing = int("bottom_spacing", 14, 0, 160)               // khoảng đệm (dp) dưới hàng quả địa cầu / mic, tính từ đáy khay
    val keyCornerRadius = int("key_corner_radius", 8, 0, 22)       // bo góc phím (dp)

    // Bàn phím nổi (giữ quả địa cầu > nút trái) và chỉnh cỡ bàn phím thường (nút phải)
    val floating = bool("floating", false)
    val floatWidth = int("float_width", 72, 45, 100)  // % bề ngang màn hình
    val floatX = int("float_x", -1, -1, 4000)          // vị trí góc trên-trái (dp); -1 = chưa đặt, tự căn giữa
    val floatY = int("float_y", -1, -1, 4000)
    // vị trí riêng khi máy nằm ngang (màn ngang khác kích thước hẳn nên không dùng chung vị trí với màn dọc)
    val floatXLand = int("float_x_land", -1, -1, 4000)
    val floatYLand = int("float_y_land", -1, -1, 4000)
    val dockWidth = int("dock_width", 100, 55, 100)   // % bề ngang khi đang gắn đáy (thu hẹp để gõ một tay)
    val dockShift = int("dock_shift", 50, 0, 100)     // vị trí ngang khi đã thu hẹp: 0 = sát trái, 100 = sát phải

    // Emoji
    val emojiSkinTone = choice("emoji_skin_tone", SkinTone.entries, SkinTone.DEFAULT)
    val emojiHistory = bool("emoji_history", true)
    val emojiMaxHistory = int("emoji_max_history", 60, 10, 120)
    val emojiMaxPinned = int("emoji_max_pinned", 8, 0, 12)
    val emojiPinnedSorting = choice("emoji_pinned_sorting", PinnedSorting.entries, PinnedSorting.NEW_FIRST)
    val emojiRecentSorting = choice("emoji_recent_sorting", RecentSorting.entries, RecentSorting.NEWEST_FIRST)
    val emojiDynamicSearch = bool("emoji_dynamic_search", true)
    val emojiTrigger = choice("emoji_trigger", EmojiTrigger.entries, EmojiTrigger.LEADING_COLON)
    val emojiUpdateFrequent = bool("emoji_update_frequent", true)
    val emojiShowName = bool("emoji_show_name", true)
    val emojiMinQuery = int("emoji_min_query", 3, 1, 7)
    val emojiMaxSuggestions = int("emoji_max_suggestions", 5, 1, 10)

    // Clipboard
    val clipInternal = bool("clip_internal", true)
    val clipSyncFrom = choice("clip_sync_from", ClipSync.entries, ClipSync.ALL)
    val clipSyncTo = choice("clip_sync_to", ClipSync.entries, ClipSync.NONE)
    val clipSuggestionSeconds = int("clip_suggestion_seconds", 60, 5, 300)
    val clipHistory = bool("clip_history", true)
    val clipColumnsPortrait = int("clip_cols_portrait", 0, 0, 5)  // 0 = tự động
    val clipColumnsLandscape = int("clip_cols_landscape", 0, 0, 6)
    val clipCleanOld = bool("clip_clean_old", false)
    val clipCleanOldMinutes = int("clip_clean_old_minutes", 20, 1, 120)
    val clipCleanSensitive = bool("clip_clean_sensitive", false)
    val clipCleanSensitiveSeconds = int("clip_clean_sensitive_seconds", 20, 5, 120)
    val clipLimitHistory = bool("clip_limit_history", true)
    val clipMaxHistory = int("clip_max_history", 20, 5, 100)
    val clipHideOnPaste = bool("clip_hide_on_paste", false)
    val clipHideOnNextField = bool("clip_hide_on_next_field", true)
    val clipClearPrimaryAffects = bool("clip_clear_primary_affects", true)

    // Keyboard > Key behavior
    val mergeHintPopups = bool("merge_hint_popups", false)
    val returnToLetters = bool("return_to_letters", true) // bấm cách ở tầng số / ký hiệu thì về tầng chữ
    val utilityKeyInRow = bool("utility_key_in_row", false)

    // Keyboard
    val numberRow = bool("number_row", false)
    val suggestionBar = bool("suggestion_bar", true)

    // Layouts (tiếng Việt)
    val oldToneStyle = bool("old_tone_style", false) // true: hòa, khỏe, thủy — false: hoà, khoẻ, thuỷ

    // Slide to Type: vuốt qua các phím để gõ cả từ (chỉ khi bàn phím đang ở tiếng Anh)
    val slideToType = bool("slide_to_type", true)

    // Gestures
    val holdDeleteRepeat = bool("hold_delete_repeat", true)
    val globeLongPress = bool("globe_long_press", true)
    val longPressMs = int("long_press_ms", 300, 150, 600) // giữ phím lâu chừng này (ms) thì mở popup

    // Sounds & Haptic Feedback
    val haptic = bool("haptic", true)
    val hapticStrength = choice("haptic_strength", HapticStrength.entries, HapticStrength.MEDIUM)
    val keySound = bool("key_sound", false)

    // Emojis / Voice Input / Clipboard
    val showEmojiKey = bool("show_emoji_key", true)
    val showMicKey = bool("show_mic_key", true)
    // Nhận giọng nói: "One language" = model Việt / Anh theo ngôn ngữ bàn phím; "Multilingual" = một model đa ngôn ngữ (chọn ở voiceMultiModel)
    val voiceMode = choice("voice_mode", com.r7tkeysx.app.voice.VoiceMode.entries, com.r7tkeysx.app.voice.VoiceMode.ONE_LANGUAGE)
    val voiceSmallPick = choice("voice_small_pick", com.r7tkeysx.app.voice.VoiceSmallPick.entries, com.r7tkeysx.app.voice.VoiceSmallPick.AUTO)
    val voiceMultiModel = choice("voice_multi_model", com.r7tkeysx.app.voice.VoiceModel.multilingualModels, com.r7tkeysx.app.voice.VoiceModel.QWEN)
    val clipboardSuggestion = bool("clipboard_suggestion", true)

    // Spelling & Typing
    val autoCapitalization = bool("auto_capitalization", true)
    val capsLockDoubleTap = bool("caps_lock_double_tap", true)
    val characterPreview = bool("character_preview", false) // bong bóng phóng to chữ khi nhấn (mặc định tắt: phím chỉ sáng lên)
    val periodShortcut = bool("period_shortcut", true)
    val autoCorrect = bool("auto_correct", true) // bấm cách thì tự sửa từ gõ sai thành từ gần giống (tiếng Anh)

    fun resetAll() = resetters.forEach { it() }

    companion object {
        @Volatile private var instance: KeyboardSettings? = null

        fun get(context: Context): KeyboardSettings = instance ?: synchronized(this) {
            instance ?: KeyboardSettings(
                context.applicationContext.getSharedPreferences("r7t_settings", Context.MODE_PRIVATE),
            ).also { instance = it }
        }
    }
}

// Để các phím tự lấy cài đặt mà không phải truyền qua từng tầng
val LocalKeyboardSettings = compositionLocalOf<KeyboardSettings> { error("KeyboardSettings chưa được cung cấp") }
