package com.r7tkeysx.app.ui.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.data.StickerStore
import com.r7tkeysx.app.ui.theme.KeyboardPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Thư viện nhãn dán của người dùng: ô "+" (mở Studio) + các nhãn đã tạo. Chạm nhãn = gửi, giữ lâu = xóa.
@Composable
fun StickerLibrary(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    onPick: (File) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentPick by rememberUpdatedState(onPick)
    val currentCreate by rememberUpdatedState(onCreate)
    val files = remember(StickerStore.revision) { StickerStore.list(context) } // đọc revision => Studio lưu xong là tự cập nhật

    if (files.isEmpty()) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Choose photos to build your sticker library.", color = palette.secondaryText, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            AddCell(palette, Modifier.size(52.dp)) { view.keyTick(settings); currentCreate() }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "add") { AddCell(palette, Modifier.aspectRatio(1f).fillMaxWidth()) { view.keyTick(settings); currentCreate() } }
        items(files, key = { it.name }) { file ->
            val bmp by produceState<ImageBitmap?>(null, file) {
                value = withContext(Dispatchers.IO) { StickerStore.thumbnail(file, 192)?.asImageBitmap() }
            }
            Box(
                Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(file) {
                        detectTapGestures(
                            onTap = { view.keyTick(settings); currentPick(file) },
                            onLongPress = { view.longPressTick(settings); StickerStore.delete(file) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                bmp?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
            }
        }
    }
}

@Composable
private fun AddCell(palette: KeyboardPalette, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(palette.letterKey.copy(alpha = 0.6f))
            .pointerInput(onClick) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) { PlusIcon(palette.keyText, size = 22.dp) }
}
