package com.r7tkeysx.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.ui.glass.GlassButton
import com.r7tkeysx.app.ui.glass.BottomInsetSpacer
import com.r7tkeysx.app.ui.glass.glassSource
import com.r7tkeysx.app.ui.keyboard.BrushIcon
import com.r7tkeysx.app.ui.keyboard.ChevronIcon
import com.r7tkeysx.app.ui.keyboard.ClipboardIcon
import com.r7tkeysx.app.ui.keyboard.EmojiIcon
import com.r7tkeysx.app.ui.keyboard.GlobeIcon
import com.r7tkeysx.app.ui.keyboard.KeyboardIcon
import com.r7tkeysx.app.ui.keyboard.LockIcon
import com.r7tkeysx.app.ui.keyboard.MicIcon
import com.r7tkeysx.app.ui.keyboard.ResetIcon
import com.r7tkeysx.app.ui.keyboard.SlideIcon
import com.r7tkeysx.app.ui.keyboard.SpeakerIcon
import com.r7tkeysx.app.ui.keyboard.TapIcon
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.R7TTheme

// Màu ô icon: chuyển từ màu trên xuống màu dưới (lấy từ ảnh mẫu)
private enum class Tile(val top: Color, val bottom: Color) {
    Gray(Color(0xFFB9B9B9), Color(0xFF9A9A9A)),
    LightBlue(Color(0xFF92C9DD), Color(0xFF68B1DC)),
    Blue(Color(0xFF74A9EB), Color(0xFF4285ED)),
    Orange(Color(0xFFF3C067), Color(0xFFECA241)),
    Red(Color(0xFFED848B), Color(0xFFE4576A)),
}

// Một dòng trong danh sách: tên + giá trị phụ (nếu có) + icon
private class RowSpec(
    val page: SettingsPage,
    val value: String? = null,
    val tile: Tile,
    val icon: @Composable () -> Unit,
) {
    val title get() = page.title
}

// Nửa trên: màn cài đặt kiểu iOS tối, cuộn được. Tiêu đề lớn thu nhỏ thành tiêu đề giữa thanh trên khi cuộn.
@Composable
fun SettingsSection(
    layoutCount: Int,
    settings: KeyboardSettings,
    keyboardEnabled: Boolean,
    onEnableKeyboard: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
    // vị trí cuộn do màn ngoài giữ: mở một mục rồi quay lại thì trang vẫn ở đúng chỗ cũ, không nhảy về đầu
    scroll: androidx.compose.foundation.ScrollState = rememberScrollState(),
) {
    val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
    val collapse: () -> Float = { (scroll.value / collapseDistance).coerceIn(0f, 1f) }
    // 0 = tiêu đề lớn còn thấy, 1 = đã thu nhỏ. Là hàm: vị trí cuộn chỉ được đọc ở bước vẽ (graphicsLayer),
    // nếu đọc ngay khi dựng giao diện thì mỗi khung hình cuộn cả trang sẽ phải dựng lại

    val general = listOf(
        RowSpec(SettingsPage.Layouts, "$layoutCount", Tile.Gray) { GlobeIcon(Color(0xFFF5F5F7), size = 24.dp) },
        RowSpec(SettingsPage.Keyboard, null, Tile.LightBlue) { KeyboardIcon(Color.White, width = 26.dp) },
        RowSpec(SettingsPage.Themes, settings.theme.value.label, Tile.LightBlue) { BrushIcon(Color.White) },
        RowSpec(SettingsPage.Gestures, null, Tile.Blue) { TapIcon(Color.White) },
        RowSpec(SettingsPage.SlideToType, if (settings.slideToType.value) "On" else "Off", Tile.Blue) { SlideIcon(Color.White) },
        RowSpec(SettingsPage.Emojis, null, Tile.Orange) { EmojiIcon(Color.White, size = 24.dp, featureColor = Tile.Orange.bottom) },
        RowSpec(SettingsPage.Clipboard, null, Tile.Gray) { ClipboardIcon(Color.White, Tile.Gray.bottom) },
        RowSpec(SettingsPage.Sounds, null, Tile.Red) { SpeakerIcon(Color.White) },
        RowSpec(SettingsPage.VoiceInput, if (settings.showMicKey.value) "On" else "Off", Tile.Blue) { MicIcon(Color.White, size = 24.dp) },
        RowSpec(SettingsPage.Spelling, null, Tile.Gray) {
            Text("ABC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        },
    )
    val advanced = listOf(
        RowSpec(SettingsPage.Privacy, null, Tile.Gray) { LockIcon(Color.White) },
        RowSpec(SettingsPage.Reset, null, Tile.Gray) { ResetIcon(Color.White) },
    )
    val other = listOf(
        RowSpec(SettingsPage.Help, null, Tile.Gray) {
            Text("?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        },
        RowSpec(SettingsPage.About, null, Tile.Gray) {
            Text("i", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        },
    )

    Box(modifier) {
        Box(Modifier.fillMaxSize().glassSource(IosColors.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .padding(horizontal = IosDimens.ScreenPadding),
        ) {
            Spacer(Modifier.height(IosDimens.TopBarHeight)) // chừa chỗ cho thanh trên cùng đứng yên
            Text(
                "R7TKeysX",
                color = IosColors.PrimaryText,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = IosDimens.SectionGap).graphicsLayer { alpha = 1f - collapse() },
            )

            SectionLabel("Status")
            val onEnable by rememberUpdatedState(onEnableKeyboard)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(IosDimens.StatusCorner))
                    .background(if (keyboardEnabled) IosColors.StatusGreen else IosColors.Warning)
                    // chưa bật bàn phím thì chạm thẻ để mở màn hình bật (không dùng clickable => không gợn sóng)
                    .pointerInput(keyboardEnabled) { if (!keyboardEnabled) detectTapGestures { onEnable() } }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    if (keyboardEnabled) "R7TKeysX is correctly configured for use. Enjoy Typing!"
                    else "R7TKeysX is not enabled yet. Tap here to turn it on in Settings.",
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(IosDimens.SectionGap))
            SectionLabel("General")
            SettingsCard(general, onOpenPage)

            Spacer(Modifier.height(IosDimens.SectionGap))
            SectionLabel("Advanced")
            SettingsCard(advanced, onOpenPage)

            Spacer(Modifier.height(IosDimens.SectionGap))
            SectionLabel("Other")
            SettingsCard(other, onOpenPage)

            BottomInsetSpacer(120.dp) // chừa chỗ cho ô Try out nổi + bàn phím ở dưới
        }
        }

        TopBar(collapse, Modifier.align(Alignment.TopCenter))
    }
}

// Thanh trên cùng đứng yên: nút Sign in bên phải, tiêu đề nhỏ ở giữa hiện dần khi cuộn
@Composable
private fun TopBar(collapse: () -> Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to IosColors.Background, 0.8f to IosColors.Background, 1f to IosColors.TitleBarFade))
            .statusBarsPadding()
            .height(IosDimens.TopBarHeight + 12.dp)
            .padding(horizontal = IosDimens.ScreenPadding),
    ) {
        Text(
            "R7TKeysX",
            color = IosColors.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 8.dp).graphicsLayer { alpha = collapse() },
        )
        GlassButton(
            onClick = {},
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp),
        ) {
            Text(
                "Sign in",
                color = IosColors.PrimaryText,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = IosColors.SecondaryText,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = IosDimens.LabelIndent, bottom = IosDimens.LabelGap),
    )
}

// Thẻ chứa nhiều dòng, ngăn cách bằng đường kẻ mảnh bắt đầu từ mép ô icon
@Composable
private fun SettingsCard(rows: List<RowSpec>, onOpenPage: (SettingsPage) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(IosDimens.CardCorner))
            .background(IosColors.Card),
    ) {
        rows.forEachIndexed { i, row ->
            if (i > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = IosDimens.CardPadding)
                        .height(0.5.dp)
                        .background(IosColors.Divider),
                )
            }
            SettingsRow(row) { onOpenPage(row.page) }
        }
    }
}

// Một dòng: ô icon - tên - giá trị - mũi tên. Chạm vào thì nền dòng sáng lên rồi mở màn con.
@Composable
private fun SettingsRow(row: RowSpec, onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IosDimens.RowHeight)
            .background(if (pressed) IosColors.RowPressed else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { currentOnClick() },
                )
            }
            .padding(horizontal = IosDimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.verticalGradient(listOf(row.tile.top, row.tile.bottom))),
            contentAlignment = Alignment.Center,
        ) { row.icon() }
        Spacer(Modifier.width(14.dp))
        Text(row.title, color = IosColors.PrimaryText, fontSize = 19.sp, modifier = Modifier.weight(1f))
        row.value?.let {
            Text(it, color = IosColors.SecondaryText, fontSize = 19.sp)
            Spacer(Modifier.width(8.dp))
        } ?: Spacer(Modifier.width(8.dp))
        ChevronIcon(color = IosColors.SecondaryText)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 393, heightDp = 800)
@Composable
private fun SettingsSectionPreview() = R7TTheme {
    SettingsSection(
        layoutCount = 2, settings = KeyboardSettings.get(LocalContext.current), keyboardEnabled = true,
        onEnableKeyboard = {}, onOpenPage = {}, modifier = Modifier.fillMaxSize(),
    )
}
