package com.r7tkeysx.app.model

// Ba chế độ chữ hoa: tắt / một chữ / khóa chữ hoa
enum class ShiftState { OFF, SINGLE, CAPS_LOCK }

// Tầng phím đang hiện: chữ / số / ký hiệu
enum class KeyboardLayer { LETTERS, NUMBERS, SYMBOLS, EMOJI, CLIPBOARD }

// Một ngôn ngữ bàn phím: mã, tên đầy đủ, nhãn ngắn hiện trên phím cách, dòng mô tả trong danh sách Layouts
data class KeyboardLanguage(val id: String, val name: String, val label: String, val subtitle: String)

// Danh sách ngôn ngữ có thể thêm (chỉ các ngôn ngữ chữ Latinh hay dùng; bố cục phím hiện vẫn là QWERTY cho tất cả,
// riêng tiếng Việt có bộ gõ Telex)
val LanguageCatalog = listOf(
    KeyboardLanguage("vi", "Tiếng Việt (Telex)", "VI", "QWERTY / Telex / Vietnamese tones"),
    KeyboardLanguage("en-US", "English (US)", "EN", "QWERTY / Western / Dollar ($)"),
    KeyboardLanguage("en-GB", "English (UK)", "EN", "QWERTY / Western / Pound (£)"),
    KeyboardLanguage("fr", "Français", "FR", "QWERTY / Western / Euro (€)"),
    KeyboardLanguage("de", "Deutsch", "DE", "QWERTY / Western / Euro (€)"),
    KeyboardLanguage("es", "Español", "ES", "QWERTY / Western / Euro (€)"),
    KeyboardLanguage("it", "Italiano", "IT", "QWERTY / Western / Euro (€)"),
    KeyboardLanguage("pt-BR", "Português (Brasil)", "PT", "QWERTY / Western / Real (R$)"),
    KeyboardLanguage("id", "Bahasa Indonesia", "ID", "QWERTY / Western / Rupiah (Rp)"),
    KeyboardLanguage("tr", "Türkçe", "TR", "QWERTY / Western / Lira (₺)"),
)

val DefaultLanguageIds = listOf("vi", "en-US")

// Kiểu ô nhập mà bàn phím phải phục vụ (tên hiện trong popup chọn kiểu)
enum class InputKind(val title: String) {
    TEXT("Text"),
    NUMBER("Number"),
    PHONE("Phone"),
    PASSWORD("Password"),
    URI("Uri"),
    EMAIL("Email"),
    DECIMAL("Decimal"),
    NUMBER_PASSWORD("NumberPassword"),
}
