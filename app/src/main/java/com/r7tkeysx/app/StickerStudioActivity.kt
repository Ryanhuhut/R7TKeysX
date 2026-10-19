package com.r7tkeysx.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r7tkeysx.app.data.StickerStore
import com.r7tkeysx.app.ui.glass.GlassSlider
import com.r7tkeysx.app.ui.glass.GlassToggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Studio nhãn dán: chọn một ảnh trong máy -> kéo / chụm để căn trong khung (chọn hình dạng khung) -> thêm chữ -> lưu.
// Dùng bộ chọn ảnh của hệ thống nên app không cần xin quyền đọc ảnh. Chưa có xóa nền (cần mô hình AI, ngoài phạm vi hiện tại).
class StickerStudioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioScreen(onClose = { finish() }) }
    }
}

private val Blue = Color(0xFF0A84FF)
private val Panel = Color(0xFF1C1C1E)
private const val OUT_SIZE = 512 // cạnh rộng của nhãn dán xuất ra (px)
private const val TEXT_RATIO = 0.14f // cỡ chữ gốc = 14% chiều rộng khung

// Hình dạng khung cắt
private enum class StickerShape(val label: String, val aspect: Float) { // aspect = cao / rộng
    SQUARE("Vuông", 1f), RECT("Chữ nhật", 0.75f), STAR("Ngôi sao", 1f), HEART("Trái tim", 1f)
}

@Composable
private fun StudioScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var shape by remember { mutableStateOf(StickerShape.SQUARE) }
    var cornerPct by remember { mutableIntStateOf(14) } // bo góc hình vuông / chữ nhật: % cạnh ngắn (50 = tròn / viên thuốc)
    // ảnh: phóng thêm (1 = vừa khít khung) và độ lệch (px trong khung xem)
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // chữ
    var textMode by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var textPos by remember { mutableStateOf(Offset(0.5f, 0.82f)) } // tâm chữ theo tỉ lệ khung (0..1)
    var textScale by remember { mutableFloatStateOf(1f) }
    var textRot by remember { mutableFloatStateOf(0f) } // độ
    var textColor by remember { mutableStateOf(Color.White) }
    var outlineColor by remember { mutableStateOf(Color.Black) }
    var outlineOn by remember { mutableStateOf(false) } // mặc định chữ không viền
    var outlinePct by remember { mutableIntStateOf(14) } // độ dày viền chữ: % cỡ chữ
    var saving by remember { mutableStateOf(false) }
    var frameW by remember { mutableFloatStateOf(1f) } // kích thước khung xem lần dựng gần nhất (px), cần lúc lưu
    var frameH by remember { mutableFloatStateOf(1f) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            if (bitmap == null) onClose() // chưa có ảnh nào mà bỏ chọn thì thoát luôn
        } else scope.launch {
            val loaded = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
            if (loaded != null) { bitmap = loaded; zoom = 1f; offset = Offset.Zero }
        }
    }
    LaunchedEffect(Unit) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().imePadding()) {
        // thanh trên: trái Hủy, giữa tiêu đề, phải Thêm (hoặc Xong khi đang sửa chữ)
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !textMode, modifier = Modifier.align(Alignment.CenterStart),
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)),
            ) {
                Text("Hủy", color = Blue, fontSize = 17.sp, modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClose() } })
            }
            Text("Studio nhãn dán", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            androidx.compose.animation.Crossfade(
                targetState = textMode, modifier = Modifier.align(Alignment.CenterEnd),
                animationSpec = androidx.compose.animation.core.tween(220), label = "topAction",
            ) { inText ->
                if (inText) {
                    Text(
                        "Xong", color = Blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pointerInput(Unit) { detectTapGestures { textMode = false } },
                    )
                } else {
                    Text(
                        if (saving) "Đang lưu…" else "Thêm", color = if (bitmap != null && !saving) Blue else Color(0xFF636366),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pointerInput(bitmap, saving) {
                            detectTapGestures {
                                val bmp = bitmap
                                if (bmp != null && !saving) {
                                    saving = true
                                    val spec = StickerSpec(
                                        shape, cornerPct, caption, textPos, textScale, textRot, textColor, outlineColor, if (outlineOn) outlinePct else 0,
                                        zoom, offset, frameW, frameH,
                                    )
                                    scope.launch {
                                        withContext(Dispatchers.IO) { StickerStore.save(context, renderSticker(bmp, spec)) }
                                        onClose()
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
        androidx.compose.animation.Crossfade(
            targetState = textMode, modifier = Modifier.fillMaxWidth(),
            animationSpec = androidx.compose.animation.core.tween(220), label = "subtitle",
        ) { inText ->
            Text(
                if (inText) "Kéo để dời chữ · hai ngón để phóng to và xoay" else "Tạo nhãn dán của bạn",
                color = Color(0xFF8E8E93), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }

        // khung cắt
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val aspect by androidx.compose.animation.core.animateFloatAsState(
                shape.aspect, androidx.compose.animation.core.spring(0.85f, 300f), label = "frameAspect",
            )
            val fw = minOf(maxWidth, maxHeight / aspect)
            val fh = fw * aspect
            val wPx = with(density) { fw.toPx() }
            val hPx = with(density) { fh.toPx() }
            frameW = wPx; frameH = hPx
            val clipShape = remember(shape, cornerPct, wPx, hPx) {
                GenericShape { size, _ -> addPath(shapePath(shape, size.width, size.height, cornerPct).asComposePath()) }
            }
            val measurer = rememberTextMeasurer()
            Box(
                Modifier
                    .size(fw, fh)
                    .clip(clipShape)
                    .background(Color(0xFF2C2C2E))
                    .border(1.dp, Color(0x33FFFFFF), clipShape)
                    // một bộ cử chỉ duy nhất: đang sửa chữ thì kéo / chụm / xoay là của CHỮ, ngược lại là của ẢNH
                    .pointerInput(bitmap, textMode) {
                        detectTransformGestures { centroid, pan, gestureZoom, rotation ->
                            val bmp = bitmap ?: return@detectTransformGestures
                            if (textMode) {
                                textPos = Offset((textPos.x + pan.x / wPx).coerceIn(0f, 1f), (textPos.y + pan.y / hPx).coerceIn(0f, 1f))
                                textScale = (textScale * gestureZoom).coerceIn(0.3f, 5f)
                                textRot += rotation
                            } else {
                                // phóng to / thu nhỏ quanh điểm giữa hai ngón (không phải quanh tâm ảnh) để vùng đang xem đứng yên dưới ngón tay
                                val newZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                                val z = newZoom / zoom
                                val c = Offset(centroid.x - wPx / 2f, centroid.y - hPx / 2f)
                                offset = clampOffset(c * (1f - z) + offset * z + pan, bmp, newZoom, wPx, hPx)
                                zoom = newZoom
                            }
                        }
                    },
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    val img = remember(bmp) { bmp.asImageBitmap() }
                    Canvas(Modifier.fillMaxSize()) {
                        val base = max(size.width / bmp.width, size.height / bmp.height) // phủ kín khung
                        val off = clampOffset(offset, bmp, zoom, size.width, size.height)
                        withTransform({
                            translate(size.width / 2 + off.x, size.height / 2 + off.y)
                            scale(base * zoom, base * zoom, pivot = Offset.Zero)
                        }) {
                            drawImage(img, topLeft = Offset(-bmp.width / 2f, -bmp.height / 2f))
                        }
                    }
                }
                if (caption.isNotEmpty()) {
                    // vẽ bằng đúng hàm dùng khi lưu ảnh nên xem trước và nhãn dán cuối cùng giống hệt nhau
                    Canvas(Modifier.fillMaxSize()) {
                        drawIntoCanvas { c ->
                            drawCaption(
                                c.nativeCanvas, size.width, size.height, caption, textPos, textScale, textRot,
                                textColor, outlineColor, if (outlineOn) outlinePct else 0,
                            )
                        }
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedContent(
            targetState = textMode,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(280, delayMillis = 60)) +
                    androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 6 }) togetherWith
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140))
            },
            label = "controlsPanel",
        ) { inText ->
        if (inText) {
            TextControls(
                caption = caption, onCaption = { caption = it.take(24) },
                textColor = textColor, onTextColor = { textColor = it },
                outlineOn = outlineOn, onOutlineOn = { outlineOn = it },
                outlineColor = outlineColor, onOutlineColor = { outlineColor = it },
                outlinePct = outlinePct, onOutlinePct = { outlinePct = it },
            )
        } else {
          Column {
            // chọn hình dạng khung
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StickerShape.entries.forEach { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (s == shape) Blue.copy(alpha = 0.28f) else Panel)
                            .pointerInput(s) { detectTapGestures { shape = s; zoom = 1f; offset = Offset.Zero } },
                        contentAlignment = Alignment.Center,
                    ) { Text(s.label, color = if (s == shape) Color.White else Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                }
            }
            // thanh kéo độ bo góc (kính lỏng): chỉ có nghĩa với khung vuông / chữ nhật; hiện / ẩn có hiệu ứng trượt
            androidx.compose.animation.AnimatedVisibility(
                visible = shape == StickerShape.SQUARE || shape == StickerShape.RECT,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(280)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Bo góc", color = Color(0xFF8E8E93), fontSize = 15.sp)
                    Box(Modifier.weight(1f).padding(horizontal = 14.dp)) { GlassSlider(cornerPct, 0, 50, { cornerPct = it }) }
                    Text("$cornerPct%", color = Color.White, fontSize = 15.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioButton("Văn bản", Modifier.weight(1f)) { textMode = true }
                StudioButton("Chọn ảnh khác", Modifier.weight(1f)) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
          }
        }
        }
        Spacer(Modifier.height(6.dp))
    }
}

// Phần chỉnh chữ: ô nhập + một thẻ gọn (Màu chữ / Viền bật tắt + màu viền / Độ dày). Chạm chấm màu để mở bánh xe màu.
@Composable
private fun TextControls(
    caption: String, onCaption: (String) -> Unit,
    textColor: Color, onTextColor: (Color) -> Unit,
    outlineOn: Boolean, onOutlineOn: (Boolean) -> Unit,
    outlineColor: Color, onOutlineColor: (Color) -> Unit,
    outlinePct: Int, onOutlinePct: (Int) -> Unit,
) {
    var picking by remember { mutableIntStateOf(0) } // 0: không mở, 1: đang chọn màu chữ, 2: đang chọn màu viền
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicTextField(
                value = caption, onValueChange = onCaption, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(Blue),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (caption.isEmpty()) Text("Nhập chữ cho nhãn dán", color = Color(0xFF636366), fontSize = 17.sp)
                    inner()
                },
            )
        }
        // thẻ chỉnh chữ <-> bảng chọn màu: trượt + mờ chuyển qua lại
        androidx.compose.animation.AnimatedContent(
            targetState = picking,
            transitionSpec = {
                val forward = targetState != 0
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260, delayMillis = 50)) +
                    androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { if (forward) it / 5 else -it / 8 }) togetherWith
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(130))
            },
            label = "colorPanel",
        ) { mode ->
            if (mode == 0) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel)) {
                    SettingRow("Màu chữ") { ColorDot(textColor) { focus.clearFocus(); picking = 1 } }
                    RowDivider()
                    SettingRow("Viền") {
                        // chấm màu viền bung ra khi bật viền, thu lại khi tắt
                        androidx.compose.animation.AnimatedVisibility(
                            visible = outlineOn,
                            enter = androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(0.7f, 500f), initialScale = 0.3f) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(140), targetScale = 0.3f) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)),
                        ) {
                            Row { ColorDot(outlineColor) { focus.clearFocus(); picking = 2 }; Spacer(Modifier.width(14.dp)) }
                        }
                        GlassToggle(outlineOn, onOutlineOn)
                    }
                    // thanh độ dày: trượt xuống khi bật viền, cuộn lên khi tắt
                    androidx.compose.animation.AnimatedVisibility(
                        visible = outlineOn,
                        enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240, delayMillis = 60)),
                        exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)),
                    ) {
                        Column {
                            RowDivider()
                            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Độ dày", color = Color.White, fontSize = 16.sp, modifier = Modifier.width(64.dp))
                                Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { GlassSlider(outlinePct, 1, 30, onOutlinePct) }
                                Text("$outlinePct", color = Color(0xFF8E8E93), fontSize = 15.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            } else {
                ColorPickerPanel(
                    title = if (mode == 1) "Màu chữ" else "Màu viền",
                    color = if (mode == 1) textColor else outlineColor,
                    onColor = { if (mode == 1) onTextColor(it) else onOutlineColor(it) },
                    onDone = { picking = 0 },
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(Color(0x33FFFFFF)))
}

@Composable
private fun ColorDot(color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color(0x66FFFFFF), CircleShape)
            .pointerInput(onClick) { detectTapGestures { onClick() } },
    )
}

// Bảng chọn màu: bánh xe màu (góc = sắc, xa tâm = đậm) + thanh độ sáng
@Composable
private fun ColorPickerPanel(title: String, color: Color, onColor: (Color) -> Unit, onDone: () -> Unit) {
    val init = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(init[0]) }
    var sat by remember { mutableFloatStateOf(init[1]) }
    var value by remember { mutableIntStateOf((init[2] * 100).toInt()) } // độ sáng 0..100
    fun emit() = onColor(Color.hsv(hue, sat.coerceIn(0f, 1f), value / 100f))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart))
            Text("Xong", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterEnd).pointerInput(Unit) { detectTapGestures { onDone() } })
        }
        ColorWheel(hue, sat, value / 100f, Modifier.size(180.dp)) { h, sa -> hue = h; sat = sa; emit() }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Độ sáng", color = Color(0xFF8E8E93), fontSize = 14.sp, modifier = Modifier.width(64.dp))
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { GlassSlider(value, 0, 100, { value = it; emit() }) }
            Box(Modifier.size(26.dp).clip(CircleShape).background(Color.hsv(hue, sat.coerceIn(0f, 1f), value / 100f)).border(1.dp, Color(0x66FFFFFF), CircleShape))
        }
    }
}

@Composable
private fun ColorWheel(hue: Float, sat: Float, value: Float, modifier: Modifier, onPick: (hue: Float, sat: Float) -> Unit) {
    val currentPick by androidx.compose.runtime.rememberUpdatedState(onPick)
    Canvas(
        modifier.pointerInput(Unit) {
            fun pick(p: Offset) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val d = p - center
                val h = ((Math.toDegrees(kotlin.math.atan2(d.y, d.x).toDouble()) + 360.0) % 360.0).toFloat()
                currentPick(h, (d.getDistance() / (size.width / 2f)).coerceIn(0f, 1f))
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                pick(down.position); down.consume()
                while (true) {
                    val change = awaitPointerEvent().changes.first()
                    if (!change.pressed) break
                    pick(change.position); change.consume()
                }
            }
        },
    ) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(androidx.compose.ui.graphics.Brush.sweepGradient(List(7) { Color.hsv((it * 60f) % 360f, 1f, 1f) }, c), r, c)
        drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White, Color.Transparent), c, r), r, c)
        drawCircle(Color.Black.copy(alpha = 1f - value), r, c)
        // vòng đánh dấu màu đang chọn
        val a = Math.toRadians(hue.toDouble())
        val pos = Offset(c.x + (cos(a) * sat * r).toFloat(), c.y + (sin(a) * sat * r).toFloat())
        drawCircle(Color.hsv(hue, sat.coerceIn(0f, 1f), value), 11.dp.toPx(), pos)
        drawCircle(Color.White, 11.dp.toPx(), pos, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun StudioButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .pointerInput(onClick) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
}

// Đường viền hình dạng (px) dùng chung cho khung xem và ảnh xuất, nên hai bên luôn khớp nhau
private fun shapePath(shape: StickerShape, w: Float, h: Float, cornerPct: Int): AndroidPath = AndroidPath().apply {
    when (shape) {
        StickerShape.SQUARE, StickerShape.RECT -> {
            val r = min(w, h) * cornerPct / 100f // % cạnh ngắn, như RoundedCornerShape(percent)
            addRoundRect(RectF(0f, 0f, w, h), r, r, AndroidPath.Direction.CW)
        }
        StickerShape.STAR -> {
            val outer = min(w, h) / 2f * 0.98f
            val inner = outer * 0.46f
            for (i in 0 until 10) {
                val a = Math.toRadians(-90.0 + i * 36.0)
                val radius = if (i % 2 == 0) outer else inner
                val x = w / 2f + (radius * cos(a)).toFloat()
                val y = h / 2f + (radius * sin(a)).toFloat() + outer * 0.04f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        StickerShape.HEART -> {
            moveTo(w * 0.5f, h * 0.92f)
            cubicTo(w * 0.06f, h * 0.62f, w * 0.0f, h * 0.3f, w * 0.2f, h * 0.15f)
            cubicTo(w * 0.35f, h * 0.04f, w * 0.46f, h * 0.14f, w * 0.5f, h * 0.27f)
            cubicTo(w * 0.54f, h * 0.14f, w * 0.65f, h * 0.04f, w * 0.8f, h * 0.15f)
            cubicTo(w * 1.0f, h * 0.3f, w * 0.94f, h * 0.62f, w * 0.5f, h * 0.92f)
            close()
        }
    }
}

// Giữ ảnh luôn phủ kín khung (không để lộ mép trống khi kéo)
private fun clampOffset(o: Offset, bmp: Bitmap, zoom: Float, w: Float, h: Float): Offset {
    val base = max(w / bmp.width, h / bmp.height)
    val maxX = ((bmp.width * base * zoom - w) / 2f).coerceAtLeast(0f)
    val maxY = ((bmp.height * base * zoom - h) / 2f).coerceAtLeast(0f)
    return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
}

// Đọc ảnh đã thu nhỏ (cạnh dài tối đa ~2048px cho đỡ tốn bộ nhớ) và xoay đúng chiều theo EXIF
private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2048) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: return@runCatching null
    val degrees = resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    if (degrees == 0f) decoded
    else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
}.getOrNull()

// Mọi thứ cần để vẽ ra nhãn dán cuối cùng
private class StickerSpec(
    val shape: StickerShape, val cornerPct: Int,
    val caption: String, val textPos: Offset, val textScale: Float, val textRot: Float,
    val textColor: Color, val outlineColor: Color, val outlinePct: Int,
    val zoom: Float, val offset: Offset, val frameW: Float, val frameH: Float,
)

// Vẽ nhãn dán (nền trong suốt ngoài hình): cùng phép biến đổi như khung xem, chỉ nhân tỉ lệ k = ảnh xuất / khung xem
private fun renderSticker(src: Bitmap, spec: StickerSpec): Bitmap {
    val outW = OUT_SIZE
    val outH = (OUT_SIZE * spec.frameH / spec.frameW).toInt().coerceAtLeast(1)
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    val k = outW / spec.frameW
    canvas.clipPath(shapePath(spec.shape, outW.toFloat(), outH.toFloat(), spec.cornerPct))

    val off = clampOffset(spec.offset, src, spec.zoom, spec.frameW, spec.frameH)
    val base = max(spec.frameW / src.width, spec.frameH / src.height)
    val m = Matrix().apply {
        postTranslate(-src.width / 2f, -src.height / 2f)
        postScale(base * spec.zoom * k, base * spec.zoom * k)
        postTranslate(outW / 2f + off.x * k, outH / 2f + off.y * k)
    }
    canvas.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

    if (spec.caption.isNotEmpty()) {
        drawCaption(
            canvas, outW.toFloat(), outH.toFloat(), spec.caption, spec.textPos, spec.textScale, spec.textRot,
            spec.textColor, spec.outlineColor, spec.outlinePct,
        )
    }
    return out
}

// Vẽ chữ lên nhãn dán (viền trước, chữ sau) quanh tâm textPos, có phóng và xoay. Dùng chung cho khung xem và ảnh xuất.
private fun drawCaption(
    canvas: AndroidCanvas, w: Float, h: Float, caption: String, textPos: Offset, textScale: Float, textRot: Float,
    textColor: Color, outlineColor: Color, outlinePct: Int,
) {
    val fontPx = w * TEXT_RATIO
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb(); textSize = fontPx; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    val fm = fill.fontMetrics
    canvas.save()
    canvas.translate(textPos.x * w, textPos.y * h)
    canvas.rotate(textRot)
    canvas.scale(textScale, textScale)
    val y = -(fm.ascent + fm.descent) / 2f // tâm chữ đúng chỗ người dùng đặt
    if (outlinePct > 0) {
        val edge = Paint(fill).apply {
            color = outlineColor.toArgb(); style = Paint.Style.STROKE
            strokeWidth = fontPx * outlinePct / 100f * 2f; strokeJoin = Paint.Join.ROUND
        }
        canvas.drawText(caption, 0f, y, edge)
    }
    canvas.drawText(caption, 0f, y, fill)
    canvas.restore()
}
