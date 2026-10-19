package com.r7tkeysx.app.ui.settings

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.HapticStrength
import com.r7tkeysx.app.data.ClipSync
import com.r7tkeysx.app.data.EmojiTrigger
import com.r7tkeysx.app.data.HintPriority
import com.r7tkeysx.app.data.PinnedSorting
import com.r7tkeysx.app.data.RecentSorting
import com.r7tkeysx.app.data.SkinTone
import com.r7tkeysx.app.data.IntSetting
import com.r7tkeysx.app.data.KeyFontWeight
import com.r7tkeysx.app.data.LandscapeMode
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.data.ThemeMode
import com.r7tkeysx.app.model.KeyboardController
import com.r7tkeysx.app.ui.glass.GlassButton
import com.r7tkeysx.app.ui.glass.GlassToggle
import com.r7tkeysx.app.ui.glass.BottomInsetSpacer
import com.r7tkeysx.app.ui.glass.glassSource
import com.r7tkeysx.app.ui.keyboard.BackIcon
import com.r7tkeysx.app.ui.keyboard.CheckIcon
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens

// Màn con của một dòng cài đặt. Mọi công tắc ghi thẳng vào KeyboardSettings nên bàn phím bên dưới đổi ngay.
@Composable
fun SettingsDetail(
    page: SettingsPage,
    settings: KeyboardSettings,
    controller: KeyboardController,
    onOpenKeyboardSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSheetChange: (Boolean) -> Unit = {}, // màn có tấm phủ toàn màn hình (Add New Keyboard) báo ra để ẩn ô Try out
) {
    if (page == SettingsPage.Layouts) {
        LayoutsScreen(settings, controller, onBack, onSheetChange, modifier)
        return
    }
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
            Spacer(Modifier.height(IosDimens.TopBarHeight + 12.dp)) // chừa chỗ cho thanh trên cùng
            LargeTitle(page.title)
            when (page) {
                SettingsPage.Layouts -> Unit // vẽ riêng ở LayoutsScreen
                SettingsPage.Keyboard -> KeyboardPage(settings)
                SettingsPage.Themes -> ThemesPage(settings)
                SettingsPage.Gestures -> GesturesPage(settings)
                SettingsPage.SlideToType -> SlideToTypePage(settings)
                SettingsPage.Emojis -> EmojisPage(settings)
                SettingsPage.Clipboard -> ClipboardPage(settings)
                SettingsPage.Sounds -> SoundsPage(settings)
                SettingsPage.VoiceInput -> VoiceInputPage(settings)
                SettingsPage.Spelling -> SpellingPage(settings)
                SettingsPage.Privacy -> PrivacyPage(settings)
                SettingsPage.Help -> HelpPage(onOpenKeyboardSettings)
                SettingsPage.About -> AboutPage()
                SettingsPage.Reset -> Unit // xử lý bằng hộp thoại xác nhận ở MainActivity
            }
            BottomInsetSpacer(130.dp) // chừa chỗ cho ô Try out nổi + bàn phím ở dưới
        }
        }
        DetailTopBar(page.title, onBack, Modifier.align(Alignment.TopCenter), titleAlpha = rememberTitleAlpha(scroll))
    }
}

// ---------------- các trang ----------------

@Composable
private fun KeyboardPage(settings: KeyboardSettings) {
    Group("Fonts", "Select the font for the keyboard") {
        KeyFontWeight.entries.forEachIndexed { i, w ->
            if (i > 0) Divider()
            CheckRow(w.label, settings.fontWeight.value == w, { settings.fontWeight.set(w) })
        }
        Divider()
        PercentSlider("Font size in portrait", settings.fontSizePortrait)
        Divider()
        PercentSlider("Font size in landscape", settings.fontSizeLandscape)
    }
    Group("Layout") {
        ToggleRow(
            "Liquid Glass", settings.keyboardGlass.value, settings.keyboardGlass::set,
            subtitle = "See-through, blurred keyboard. Only affects the keyboard; the settings screen has its own switch in Themes.",
        )
        Divider()
        ToggleRow(
            "Enable Blur Effect", settings.blurEnabled.value && settings.keyboardGlass.value, settings.blurEnabled::set,
            enabled = settings.keyboardGlass.value,
            subtitle = "Show a static blur effect on the keyboard. Dynamic blur makes the keyboard very slow, it will not be implemented for now.",
        )
        Divider()
        SliderRow(
            "Blur Radius", "${settings.blurRadius.value}", settings.blurRadius.value, settings.blurRadius.min, settings.blurRadius.max,
            settings.blurRadius::set, onReset = settings.blurRadius::reset, description = "Adjust the strength of the live blur behind the keyboard.", enabled = settings.blurEnabled.value && settings.keyboardGlass.value,
        )
        Divider()
        ToggleRow("Glass border", settings.glassBorder.value, settings.glassBorder::set)
        Divider()
        ToggleRow("Numeric Row", settings.numberRow.value, settings.numberRow::set, subtitle = "Show a row of numbers above the characters view")
        Divider()
        ToggleRow("Number hints", settings.numberHints.value, settings.numberHints::set)
        Divider()
        DropdownRow("Number hint priority", HintPriority.entries, settings.numberHintPriority.value, { it.label }, settings.numberHintPriority::set, enabled = settings.numberHints.value)
        Divider()
        ToggleRow("Symbol Hints", settings.symbolHints.value, settings.symbolHints::set, subtitle = "Show symbol hints in the characters view")
        Divider()
        DropdownRow("Symbol hint priority", HintPriority.entries, settings.symbolHintPriority.value, { it.label }, settings.symbolHintPriority::set, enabled = settings.symbolHints.value)
        Divider()
        ToggleRow("Punctuations in the characters view", settings.punctuationKeys.value, settings.punctuationKeys::set, subtitle = "Show \".\" and \",\" in the characters row.")
        Divider()
        DropdownRow("Landscape Mode", LandscapeMode.entries, settings.landscapeMode.value, { it.label }, settings.landscapeMode::set)
        Divider()
        PercentSlider("Vertical key spacing", settings.verticalSpacing)
        Divider()
        PercentSlider("Horizontal key spacing", settings.horizontalSpacing)
        Divider()
        PercentSlider("Key height", settings.keyHeightPercent)
        Divider()
        PercentSlider("Key width", settings.keyWidthPercent)
        Divider()
        SliderRow(
            "Key corner radius", "${settings.keyCornerRadius.value}", settings.keyCornerRadius.value,
            settings.keyCornerRadius.min, settings.keyCornerRadius.max, settings.keyCornerRadius::set,
            onReset = settings.keyCornerRadius::reset,
        )
        Divider()
        SliderRow(
            "Bottom spacing", "${settings.bottomSpacing.value} dp", settings.bottomSpacing.value,
            settings.bottomSpacing.min, settings.bottomSpacing.max, settings.bottomSpacing::set,
            description = "Space between the globe key and the bottom of the phone.", onReset = settings.bottomSpacing::reset,
        )
    }
    Group("Key behavior") {
        ToggleRow("Key press popups", settings.characterPreview.value, settings.characterPreview::set)
        Divider()
        ToggleRow("Merge hint popups", settings.mergeHintPopups.value, settings.mergeHintPopups::set)
        Divider()
        SliderRow("Long press delay", "${settings.longPressMs.value} ms", settings.longPressMs.value, settings.longPressMs.min, settings.longPressMs.max, settings.longPressMs::set, onReset = settings.longPressMs::reset)
        Divider()
        ToggleRow("Return to letters after space", settings.returnToLetters.value, settings.returnToLetters::set)
        Divider()
        ToggleRow("Wave animation", settings.keyWave.value, settings.keyWave::set, subtitle = "Keys ripple in like a wave when you switch between letters, numbers and symbols (portrait only).")
        Divider()
        DropdownRow(
            "Caps Lock activation", listOf(true, false), settings.capsLockDoubleTap.value,
            { if (it) "Double-tap Shift" else "Off" }, settings.capsLockDoubleTap::set,
        )
    }
    Group("Utility key and indicators") {
        ToggleRow("Utility key", settings.showEmojiKey.value, settings.showEmojiKey::set)
        Divider()
        ToggleRow("Utility key in keys row", settings.utilityKeyInRow.value, settings.utilityKeyInRow::set)
    }
    Group(footer = "The Suggestion Bar shows the word you are typing.") {
        ToggleRow("Suggestion Bar", settings.suggestionBar.value, settings.suggestionBar::set)
    }
}

@Composable
private fun PercentSlider(title: String, setting: IntSetting) {
    SliderRow(title, "${setting.value}%", setting.value, setting.min, setting.max, setting::set, onReset = setting::reset)
}

@Composable
private fun ThemesPage(settings: KeyboardSettings) {
    Group(footer = "Glass buttons, switches, sliders and the Try out bar in this settings screen. The keyboard has its own Liquid Glass switch in Keyboard > Layout.") {
        ToggleRow("Liquid Glass", settings.glassEffects.value, settings.glassEffects::set)
    }
    Group("Appearance", "Applies to the keyboard. This settings screen always stays dark.") {
        ThemeMode.entries.forEachIndexed { i, mode ->
            if (i > 0) Divider()
            CheckRow(mode.label, settings.theme.value == mode, { settings.theme.set(mode) })
        }
    }
}

@Composable
private fun GesturesPage(settings: KeyboardSettings) {
    Group(footer = "Hold Delete keeps deleting until you let go. Without the language menu, the globe key only switches to the next language.") {
        ToggleRow("Hold Delete to Repeat", settings.holdDeleteRepeat.value, settings.holdDeleteRepeat::set)
        Divider()
        ToggleRow("Hold Globe for Language Menu", settings.globeLongPress.value, settings.globeLongPress::set)
    }
    Group("Long-Press Delay", "How long to hold the globe key before the language menu opens.") {
        SliderRow("Long press delay", "${settings.longPressMs.value} ms", settings.longPressMs.value, settings.longPressMs.min, settings.longPressMs.max, settings.longPressMs::set, onReset = settings.longPressMs::reset)
    }
}

@Composable
private fun SlideToTypePage(settings: KeyboardSettings) {
    Group(footer = "Glide your finger across the letters to type a whole word, then lift. Works with the English keyboard; Vietnamese (Telex) uses normal typing. Other guesses appear in the suggestion bar.") {
        ToggleRow("Slide to Type", settings.slideToType.value, settings.slideToType::set)
    }
}

@Composable
private fun EmojisPage(settings: KeyboardSettings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.r7tkeysx.app.data.EmojiStore.get(context) }
    SkinToneCard(settings)
    Group("Emoji History") {
        ToggleRow("Enable History", settings.emojiHistory.value, settings.emojiHistory::set, subtitle = "Show recently used emojis in the emoji keyboard")
        Divider()
        SliderRow(
            "Maximum History Size", "${settings.emojiMaxHistory.value}", settings.emojiMaxHistory.value,
            settings.emojiMaxHistory.min, settings.emojiMaxHistory.max,
            { settings.emojiMaxHistory.set(it); store.trimHistory(it) },
            description = "Set the maximum number of recently used emojis",
            onReset = { settings.emojiMaxHistory.reset(); store.trimHistory(settings.emojiMaxHistory.value) }, ticks = 7,
        )
        Divider()
        SliderRow(
            "Maximum Pinned Emojis", "${settings.emojiMaxPinned.value}", settings.emojiMaxPinned.value,
            settings.emojiMaxPinned.min, settings.emojiMaxPinned.max, settings.emojiMaxPinned::set,
            description = "Set the maximum number of pinned emojis", onReset = settings.emojiMaxPinned::reset, ticks = 13,
        )
        Divider()
        DropdownRow("Pinned emoji sorting", PinnedSorting.entries, settings.emojiPinnedSorting.value, { it.label }, settings.emojiPinnedSorting::set)
        Divider()
        DropdownRow("Recent emoji sorting", RecentSorting.entries, settings.emojiRecentSorting.value, { it.label }, settings.emojiRecentSorting::set)
        Divider()
        ChevronRow("Clear History", "Remove all recently used emojis") { store.clearHistory() }
        Divider()
        ChevronRow("Clear Pinned", "Remove all pinned emojis") { store.clearPinned() }
    }
    Group("Emoji suggestions", "Type a colon and a word (like :smile) in the keyboard to find emojis by name.") {
        ToggleRow(
            "Enable Emoji Dynamic Search", settings.emojiDynamicSearch.value, settings.emojiDynamicSearch::set,
            subtitle = "Show emoji suggestions as you type at the bottom of the keyboard",
        )
        Divider()
        DropdownRow("Trigger type", EmojiTrigger.entries, settings.emojiTrigger.value, { it.label }, settings.emojiTrigger::set, enabled = settings.emojiDynamicSearch.value)
        Divider()
        ToggleRow(
            "Update Frequently Used Emojis", settings.emojiUpdateFrequent.value, settings.emojiUpdateFrequent::set,
            subtitle = "Add emoji to the frequently used emojis when selected from suggestions",
        )
        Divider()
        ToggleRow(
            "Show emoji name", settings.emojiShowName.value, settings.emojiShowName::set,
            subtitle = "Emoji suggestions display their name alongside the emoji",
        )
        Divider()
        SliderRow(
            "Minimum Query Length", "${settings.emojiMinQuery.value}", settings.emojiMinQuery.value,
            settings.emojiMinQuery.min, settings.emojiMinQuery.max, settings.emojiMinQuery::set,
            description = "Set the minimum number of characters needed to suggest emojis", onReset = settings.emojiMinQuery::reset, ticks = 7,
        )
        Divider()
        SliderRow(
            "Maximum Suggestions Count", "${settings.emojiMaxSuggestions.value}", settings.emojiMaxSuggestions.value,
            settings.emojiMaxSuggestions.min, settings.emojiMaxSuggestions.max, settings.emojiMaxSuggestions::set,
            description = "Set the maximum number of suggested emojis", onReset = settings.emojiMaxSuggestions::reset, ticks = 10,
        )
    }
}

// Thẻ chọn màu da emoji: chấm màu + mũi tên ⌃⌄, chạm mở danh sách
@Composable
private fun SkinToneCard(settings: KeyboardSettings) {
    var open by remember { mutableStateOf(false) }
    val tone = settings.emojiSkinTone.value
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(IosColors.Card)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .pointerInput(Unit) { detectTapGestures { open = true } }
                .padding(start = IosDimens.CardPadding + 4.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Emoji Skin Tone", color = IosColors.PrimaryText, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(tone.color)))
            Spacer(Modifier.width(8.dp))
            com.r7tkeysx.app.ui.keyboard.UpDownIcon(IosColors.SecondaryText, size = 22.dp)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = IosColors.TypePopup) {
                SkinTone.entries.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(option.color)))
                                Spacer(Modifier.width(12.dp))
                                Text(option.label, color = if (option == tone) IosColors.Accent else IosColors.PrimaryText, fontSize = 18.sp)
                            }
                        },
                        onClick = { settings.emojiSkinTone.set(option); open = false },
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(IosDimens.SectionGap))
}

@Composable
private fun ClipboardPage(settings: KeyboardSettings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.r7tkeysx.app.data.ClipboardStore.get(context) }
    Group {
        ToggleRow("Use internal clipboard", settings.clipInternal.value, settings.clipInternal::set, subtitle = "Use the internal clipboard instead of the system clipboard.")
        Divider()
        DropdownRow("Sync from system clipboard", ClipSync.entries, settings.clipSyncFrom.value, { it.label }, settings.clipSyncFrom::set, description = { it.description })
        Divider()
        DropdownRow("Sync to system clipboard", ClipSync.entries, settings.clipSyncTo.value, { it.label }, settings.clipSyncTo::set, description = { it.description })
    }
    Group("Clipboard suggestions") {
        ToggleRow("Clipboard content suggestions", settings.clipboardSuggestion.value, settings.clipboardSuggestion::set, subtitle = "Suggest previously copied clipboard content")
        Divider()
        SliderRow(
            "Limit clipboard suggestions to", "", settings.clipSuggestionSeconds.value, settings.clipSuggestionSeconds.min, settings.clipSuggestionSeconds.max,
            settings.clipSuggestionSeconds::set, description = "Items copied within the last ${settings.clipSuggestionSeconds.value} s",
            enabled = settings.clipboardSuggestion.value, onReset = settings.clipSuggestionSeconds::reset,
        )
    }
    Group("Clipboard history") {
        ToggleRow("Enable clipboard history", settings.clipHistory.value, settings.clipHistory::set, subtitle = "Retain clipboard items for quick access")
        Divider()
        SliderRow(
            "Clipboard columns in portrait", if (settings.clipColumnsPortrait.value == 0) "Auto" else "${settings.clipColumnsPortrait.value}",
            settings.clipColumnsPortrait.value, settings.clipColumnsPortrait.min, settings.clipColumnsPortrait.max, settings.clipColumnsPortrait::set,
            onReset = settings.clipColumnsPortrait::reset, ticks = 6,
        )
        Divider()
        SliderRow(
            "Clipboard columns in landscape", if (settings.clipColumnsLandscape.value == 0) "Auto" else "${settings.clipColumnsLandscape.value}",
            settings.clipColumnsLandscape.value, settings.clipColumnsLandscape.min, settings.clipColumnsLandscape.max, settings.clipColumnsLandscape::set,
            onReset = settings.clipColumnsLandscape::reset, ticks = 7,
        )
        Divider()
        ToggleRow("Clean up old items", settings.clipCleanOld.value, settings.clipCleanOld::set)
        Divider()
        SliderRow(
            "Auto clean old items after", "${settings.clipCleanOldMinutes.value} m", settings.clipCleanOldMinutes.value,
            settings.clipCleanOldMinutes.min, settings.clipCleanOldMinutes.max, settings.clipCleanOldMinutes::set,
            enabled = settings.clipCleanOld.value, onReset = settings.clipCleanOldMinutes::reset,
        )
        Divider()
        ToggleRow("Auto clean sensitive items", settings.clipCleanSensitive.value, settings.clipCleanSensitive::set)
        Divider()
        SliderRow(
            "Auto clean sensitive items after", "${settings.clipCleanSensitiveSeconds.value} s", settings.clipCleanSensitiveSeconds.value,
            settings.clipCleanSensitiveSeconds.min, settings.clipCleanSensitiveSeconds.max, settings.clipCleanSensitiveSeconds::set,
            enabled = settings.clipCleanSensitive.value, onReset = settings.clipCleanSensitiveSeconds::reset,
        )
        Divider()
        ToggleRow("Limit clipboard history", settings.clipLimitHistory.value, settings.clipLimitHistory::set, subtitle = "Limit the number of items in your clipboard history.")
        Divider()
        SliderRow(
            "Maximum clipboard history size", "${settings.clipMaxHistory.value}", settings.clipMaxHistory.value,
            settings.clipMaxHistory.min, settings.clipMaxHistory.max, { settings.clipMaxHistory.set(it); store.cleanup(settings) },
            description = "Set the maximum number of items in your clipboard history.", enabled = settings.clipLimitHistory.value,
            onReset = { settings.clipMaxHistory.reset(); store.cleanup(settings) }, ticks = 10,
        )
    }
    Group("Other") {
        ToggleRow("Hide history on paste", settings.clipHideOnPaste.value, settings.clipHideOnPaste::set)
        Divider()
        ToggleRow("Hide history on next text field", settings.clipHideOnNextField.value, settings.clipHideOnNextField::set)
        Divider()
        ToggleRow(
            "Clear primary clip affects history", settings.clipClearPrimaryAffects.value, settings.clipClearPrimaryAffects::set,
            subtitle = "Clearing the primary clip also removes it from the history, if not pinned",
        )
    }
}

@Composable
private fun SoundsPage(settings: KeyboardSettings) {
    Group(footer = "Letters, delete and return each have their own sound. Silent when the phone is on vibrate or silent mode.") {
        ToggleRow("Key Click Sound", settings.keySound.value, settings.keySound::set)
        Divider()
        ToggleRow("Haptic Feedback", settings.haptic.value, settings.haptic::set)
    }
    Group("Haptic Strength") {
        HapticStrength.entries.forEachIndexed { i, strength ->
            if (i > 0) Divider()
            CheckRow(strength.label, settings.hapticStrength.value == strength, { settings.hapticStrength.set(strength) }, enabled = settings.haptic.value)
        }
    }
}

@Composable
private fun SpellingPage(settings: KeyboardSettings) {
    Group(footer = "Capitalizes the first letter of every sentence.") {
        ToggleRow("Auto-Capitalization", settings.autoCapitalization.value, settings.autoCapitalization::set)
    }
    Group(footer = "Double-tap Shift to turn on Caps Lock.") {
        ToggleRow("Enable Caps Lock", settings.capsLockDoubleTap.value, settings.capsLockDoubleTap::set)
    }
    Group(footer = "Shows a magnified letter above the key you press.") {
        ToggleRow("Character Preview", settings.characterPreview.value, settings.characterPreview::set)
    }
    Group(footer = "Fixes a misspelled word when you press space. Press Backspace right after to undo the fix.") {
        ToggleRow("Auto-Correction", settings.autoCorrect.value, settings.autoCorrect::set)
    }
    Group(footer = "Double-tapping the space bar types a period followed by a space.") {
        ToggleRow("\".\" Shortcut", settings.periodShortcut.value, settings.periodShortcut::set)
    }
}

@Composable
private fun PrivacyPage(settings: KeyboardSettings) {
    Group("What R7TKeysX can access", "The Internet is used only to download the voice model you choose (from Hugging Face). Nothing you type or say is ever sent anywhere.") {
        InfoRow("Network access", "Voice model download only")
        Divider()
        InfoRow("Typed text sent anywhere", "Never")
        Divider()
        InfoRow("Clipboard", if (settings.clipboardSuggestion.value) "Read when you open the keyboard" else "Not used")
    }
}

@Composable
private fun HelpPage(onOpenKeyboardSettings: () -> Unit) {
    val context = LocalContext.current
    Group("Set up") {
        ButtonRow("Open Keyboard Settings", onOpenKeyboardSettings)
        Divider()
        ButtonRow("Choose Keyboard…") {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }
    Group("Telex cheat sheet", "Type the letter after the vowel. For example: v-i-e-e-t-j gives việt.") {
        TextBlock("aa → â    ee → ê    oo → ô\naw → ă    ow → ơ    uw → ư\ndd → đ")
        Divider()
        TextBlock("s → sắc    f → huyền    r → hỏi\nx → ngã    j → nặng    z → bỏ dấu")
    }
}

@Composable
private fun AboutPage() {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?" }
    Group {
        InfoRow("Version", version)
        Divider()
        InfoRow("Package", context.packageName)
        Divider()
        InfoRow("Font", "Nunito (SIL OFL)")
    }
}

// ---------------- hộp thoại xác nhận ----------------

@Composable
fun ResetConfirmDialog(onCancel: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { onCancel() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(IosColors.Card)
                .border(1.dp, IosColors.Hairline, RoundedCornerShape(26.dp))
                .pointerInput(Unit) { detectTapGestures { } } // chạm vào hộp thì không đóng
                .padding(vertical = 22.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Reset all settings?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Every keyboard option goes back to its default. Your typed text is not affected.",
                color = IosColors.SecondaryText, fontSize = 15.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row {
                DialogButton("Cancel", IosColors.Accent, Modifier.weight(1f), onCancel)
                Spacer(Modifier.width(12.dp))
                DialogButton("Reset", IosColors.Danger, Modifier.weight(1f), onConfirm)
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(IosColors.RowPressed)
            .pointerInput(Unit) { detectTapGestures { currentOnClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------- linh kiện dùng chung ----------------

// Thanh trên cùng: nút quay lại tròn bên trái, tiêu đề ở giữa
@Composable
internal fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable BoxScope.() -> Unit = {}, // nút bên phải (vd. Edit)
    titleAlpha: () -> Float = { 1f }, // tiêu đề nhỏ ở giữa hiện dần khi tiêu đề lớn đã cuộn khuất (đọc ở bước vẽ)
) {
    val currentOnBack by rememberUpdatedState(onBack)
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to IosColors.Background, 0.8f to IosColors.Background, 1f to IosColors.TitleBarFade))
            .statusBarsPadding()
            .height(IosDimens.TopBarHeight + 12.dp)
            .padding(horizontal = IosDimens.ScreenPadding),
    ) {
        GlassButton(
            onClick = { currentOnBack() },
            modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp).size(50.dp),
        ) { BackIcon(IosColors.PrimaryText) }
        trailing()
        Text(
            title,
            color = IosColors.PrimaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 25.dp, start = 62.dp, end = 62.dp).graphicsLayer { alpha = titleAlpha() },
        )
    }
}

// Một nhóm: nhãn nhỏ phía trên (nếu có), thẻ chứa các dòng, chú thích phía dưới (nếu có)
@Composable
internal fun Group(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        header?.let {
            Text(
                it, color = IosColors.SecondaryText, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = IosDimens.LabelIndent, bottom = IosDimens.LabelGap),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(IosDimens.CardCorner))
                .background(IosColors.Card),
            content = content,
        )
        footer?.let {
            Text(
                it, color = IosColors.SecondaryText, fontSize = 14.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(start = IosDimens.LabelIndent, end = IosDimens.LabelIndent, top = 8.dp),
            )
        }
        Spacer(Modifier.height(IosDimens.SectionGap))
    }
}

@Composable
internal fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = IosDimens.CardPadding).height(0.5.dp).background(IosColors.Divider))
}

@Composable
internal fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(start = IosDimens.CardPadding + 4.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp).alpha(if (enabled) 1f else 0.45f)) {
            Text(title, color = IosColors.PrimaryText, fontSize = 18.sp)
            subtitle?.let {
                Text(it, color = IosColors.SecondaryText, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        GlassToggle(selected = checked, onSelect = onChange, enabled = enabled, modifier = Modifier.alpha(if (enabled) 1f else 0.45f))
    }
}

// Dòng chọn 1 trong nhiều: mục đang chọn có dấu tick xanh
@Composable
internal fun CheckRow(title: String, selected: Boolean, onClick: () -> Unit, subtitle: String? = null, enabled: Boolean = true) {
    val currentOnClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(if (pressed) IosColors.RowPressed else Color.Transparent)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { currentOnClick() },
                    )
                }
            }
            .padding(start = IosDimens.CardPadding + 4.dp, end = IosDimens.CardPadding + 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = IosColors.PrimaryText, fontSize = 18.sp, modifier = Modifier.weight(1f).alpha(if (enabled) 1f else 0.45f))
        subtitle?.let {
            Text(it, color = IosColors.SecondaryText, fontSize = 16.sp, modifier = Modifier.padding(end = 12.dp).alpha(if (enabled) 1f else 0.45f))
        }
        Box(Modifier.size(22.dp)) {
            // dấu tick bật lên có hiệu ứng phóng nhẹ
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160)) +
                    androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(0.6f, 600f), initialScale = 0.4f),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)),
            ) { CheckIcon(IosColors.Accent.copy(alpha = if (enabled) 1f else 0.45f)) }
        }
    }
}

@Composable
internal fun InfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = IosDimens.CardPadding + 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = IosColors.PrimaryText, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, color = IosColors.SecondaryText, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ButtonRow(title: String, onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(if (pressed) IosColors.RowPressed else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { currentOnClick() },
                )
            }
            .padding(horizontal = IosDimens.CardPadding + 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = IosColors.Accent, fontSize = 18.sp)
    }
}

@Composable
private fun TextBlock(text: String) {
    Text(
        text, color = IosColors.PrimaryText, fontSize = 17.sp, lineHeight = 26.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = IosDimens.CardPadding + 4.dp, vertical = 14.dp),
    )
}

// Tiêu đề lớn đầu trang (cuộn theo nội dung)
@Composable
internal fun LargeTitle(title: String) {
    Text(
        title, color = IosColors.PrimaryText, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 22.dp),
    )
}

// Cuộn qua ~80dp thì tiêu đề nhỏ hiện hẳn. Trả về hàm để vị trí cuộn chỉ bị đọc ở bước vẽ, không bắt cả trang dựng lại mỗi khung hình.
@Composable
internal fun rememberTitleAlpha(scroll: androidx.compose.foundation.ScrollState): () -> Float {
    val threshold = with(androidx.compose.ui.platform.LocalDensity.current) { 80.dp.toPx() }
    return remember(scroll, threshold) { { (scroll.value / threshold).coerceIn(0f, 1f) } }
}
