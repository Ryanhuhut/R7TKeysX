package com.r7tkeysx.app.ui.settings

// Các màn con của phần cài đặt (mỗi dòng trong danh sách mở một màn)
enum class SettingsPage(val title: String) {
    Layouts("Layouts"),
    Keyboard("Keyboard"),
    Themes("Themes"),
    Gestures("Gestures"),
    SlideToType("Slide to Type"),
    Emojis("Emojis"),
    Clipboard("Clipboard"),
    Sounds("Sounds & Haptic Feedback"),
    VoiceInput("Voice Input"),
    Spelling("Spelling & Typing"),
    Privacy("Privacy & Permissions"),
    Reset("Reset Settings"),
    Help("Help & Feedback"),
    About("About R7TKeysX"),
}
