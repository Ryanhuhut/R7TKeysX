package com.r7tkeysx.app.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.r7tkeysx.app.ui.theme.Motion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.LocalKeyboardPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Hai nút ở đáy popup quả địa cầu gọi vào đây (để GlobeKey không phải nhận thêm tham số qua nhiều tầng)
class KeyboardModeActions(val floating: Boolean, val onFloat: () -> Unit, val onAdjust: () -> Unit)

val LocalKeyboardModeActions = compositionLocalOf<KeyboardModeActions?> { null }

private val Blue = IosColors.Accent

// Chuyển qua lại bàn phím thường <-> bàn phím nổi: nhớ khung (toạ độ trong cửa sổ) của bàn phím đang hiện,
// để bàn phím kia "bóp" lại từ đúng khung đó (hoặc "nở" ra), thay vì hiện ra từ chỗ khác.
internal object KeyboardMorph {
    var dockRect: Rect? = null  // khung bàn phím thường, lần đo gần nhất
    var floatRect: Rect? = null // khung khay nổi, lần đo gần nhất
    private var from: Rect? = null

    fun toFloating() { from = dockRect }
    fun toDocked() { from = floatRect }
    val pending get() = from != null
    fun take(): Rect? = from.also { from = null }
}

// Khung của một phần tử theo toạ độ MÀN HÌNH (không phải toạ độ cửa sổ). Bàn phím hệ thống đổi giữa 2 cửa sổ khác nhau
// (gắn đáy: cửa sổ thấp; nổi: cửa sổ phủ cả màn) nên chỉ toạ độ màn hình mới so sánh được giữa hai chế độ.
internal fun androidx.compose.ui.layout.LayoutCoordinates.boundsOnScreen(view: android.view.View): Rect {
    val onScreen = IntArray(2); val inWindow = IntArray(2)
    view.getLocationOnScreen(onScreen); view.getLocationInWindow(inWindow)
    return boundsInWindow().translate((onScreen[0] - inWindow[0]).toFloat(), (onScreen[1] - inWindow[1]).toFloat())
}

// Bàn phím mới xuất hiện ở khung `fromScreen` (toạ độ màn hình) rồi co / giãn mượt về khung thật của nó (lò xo êm, không nảy).
// ready = false: chưa biết vị trí thật (đang đo) thì ẩn tạm.
private fun Modifier.morphFrom(fromScreen: Rect?, ready: Boolean = true): Modifier = composed {
    if (fromScreen == null) return@composed this
    val view = LocalView.current
    var self by remember { mutableStateOf<Rect?>(null) }
    var from by remember { mutableStateOf(fromScreen) } // đổi sang toạ độ cửa sổ hiện tại lúc đo xong
    val progress = remember { Animatable(0f) }
    LaunchedEffect(self != null) {
        if (self != null) progress.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 320f))
    }
    this
        .onGloballyPositioned {
            if (ready && self == null) {
                self = it.boundsInWindow()
                val onScreen = IntArray(2); val inWindow = IntArray(2)
                view.getLocationOnScreen(onScreen); view.getLocationInWindow(inWindow)
                from = fromScreen.translate(-(onScreen[0] - inWindow[0]).toFloat(), -(onScreen[1] - inWindow[1]).toFloat())
            }
        }
        .graphicsLayer {
            val s = self
            if (s == null || s.width <= 0f || s.height <= 0f) { alpha = 0f; return@graphicsLayer }
            val t = progress.value
            if (t >= 1f) return@graphicsLayer
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = androidx.compose.ui.util.lerp(from.width / s.width, 1f, t)
            scaleY = androidx.compose.ui.util.lerp(from.height / s.height, 1f, t)
            translationX = androidx.compose.ui.util.lerp(from.left - s.left, 0f, t)
            translationY = androidx.compose.ui.util.lerp(from.top - s.top, 0f, t)
        }
}
private const val HANDLES_HIDE_MS = 4000L

// ---------------------------------------------------------------- bàn phím nổi

// Bàn phím nổi: một khay nhỏ lơ lửng, đặt được ở bất kỳ đâu trên màn.
// - Thanh ngang ở đáy: giữ và kéo để di chuyển. Thả tay -> hiện 4 dấu góc xanh, kéo dấu góc để đổi cỡ.
// - Kéo xuống sát đáy màn: hiện vùng xanh; thả vào đó -> về bàn phím thường.
// - Nút mũi tên cạnh thanh kéo: ẩn bàn phím (trượt xuống + mờ dần).
// content nhận `footer` (dải thanh kéo) để đặt ở cuối khay.
@Composable
fun FloatingKeyboard(
    settings: KeyboardSettings,
    showHandles: MutableState<Boolean>,
    onDismiss: (() -> Unit)?,
    onBounds: ((Rect?) -> Unit)?,
    appearKey: Int = 0, // đổi số này (mỗi lần bàn phím hiện ra) để chạy lại hiệu ứng trồi lên
    content: @Composable (footer: @Composable () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnBounds by rememberUpdatedState(onBounds)
    // bàn phím ẩn đi (chuyển về gắn đáy) thì báo "không còn vùng nổi"
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { currentOnBounds?.invoke(null) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val margin = with(density) { FrameMargin.toPx() }
        val dockZone = with(density) { IosDimens.DockZoneHeight.toPx() }
        // màn ngang thấp: không có vùng xanh thả về bàn phím thường (đã có nút trong menu quả địa cầu), kẻo chiếm hết chỗ và cản việc di chuyển
        val dockEnabled = screenW <= screenH

        var widthPct by remember { mutableFloatStateOf(settings.floatWidth.value.toFloat()) }
        // chiều rộng khay tính theo "bề ngang kiểu màn dọc": màn ngang thì lấy theo bề cao màn (x1,25 ~ bề ngang một điện thoại dọc)
        // chứ không theo bề ngang màn rộng, kẻo bàn phím nổi thành một dải dẹt quá khổ
        val refW = if (screenW > screenH) screenH * 1.25f else screenW
        val kbW = refW * widthPct / 100f
        var kbH by remember { mutableIntStateOf(0) }
        // vị trí góc trên-trái của khay (px); NaN = chưa đặt, chờ đo xong chiều cao rồi đặt giữa, gần đáy
        // màn dọc và màn ngang nhớ vị trí riêng; xoay máy thì nạp lại vị trí của chiều đó (không bị văng ra mép)
        val land = screenW > screenH
        val posXSetting = if (land) settings.floatXLand else settings.floatX
        val posYSetting = if (land) settings.floatYLand else settings.floatY
        var x by remember(land) { mutableFloatStateOf(if (posXSetting.value >= 0) with(density) { posXSetting.value.dp.toPx() } else Float.NaN) }
        var y by remember(land) { mutableFloatStateOf(if (posYSetting.value >= 0) with(density) { posYSetting.value.dp.toPx() } else Float.NaN) }
        var dragging by remember { mutableStateOf(false) }
        var anchorBottom by remember { mutableFloatStateOf(Float.NaN) } // đang kéo góc trên: mép dưới cần giữ nguyên (px)
        var overDock by remember { mutableStateOf(false) }
        val exit = remember { Animatable(0f) } // 0 = đang hiện, 1 = đã trượt xuống + mờ hẳn
        val enter = remember { Animatable(1f) } // 1 = còn nằm dưới + trong suốt, 0 = đã trồi lên hẳn
        val morph = remember { KeyboardMorph.take() } // vừa chuyển từ bàn phím thường: bóp lại từ khung cũ thay vì trồi lên
        val firstShow = remember { booleanArrayOf(true) }
        LaunchedEffect(appearKey) {
            // lần hiện đầu tiên ngay sau khi chuyển chế độ: để hiệu ứng bóp lo, không trồi lên
            if (morph != null && firstShow[0]) { firstShow[0] = false; enter.snapTo(0f); return@LaunchedEffect }
            firstShow[0] = false
            enter.snapTo(1f)
            enter.animateTo(0f, tween(Motion.KEYBOARD_MS + 60, easing = Motion.Decelerate))
        }
        val handlesAlpha by animateFloatAsState(if (showHandles.value) 1f else 0f, tween(if (showHandles.value) 260 else 180), label = "handles")

        fun clampPos() {
            if (x.isNaN() || y.isNaN() || kbH == 0) return
            x = x.coerceIn(margin - kbW * 0.5f, screenW - margin - kbW * 0.5f) // cho thò ra ngoài tối đa nửa khay
            y = y.coerceIn(margin, (screenH - kbH - margin).coerceAtLeast(margin))
        }
        fun save() {
            with(density) {
                posXSetting.set(x.coerceAtLeast(0f).toDp().value.roundToInt())
                posYSetting.set(y.coerceAtLeast(0f).toDp().value.roundToInt())
            }
            settings.floatWidth.set(widthPct.roundToInt())
        }
        LaunchedEffect(kbH, screenW, screenH) {
            if (kbH == 0) return@LaunchedEffect
            if (x.isNaN()) x = (screenW - kbW) / 2f
            if (y.isNaN()) y = screenH - kbH - with(density) { 140.dp.toPx() }
            clampPos()
        }
        // dấu góc tự ẩn sau vài giây
        LaunchedEffect(showHandles.value, widthPct) {
            if (showHandles.value) { delay(HANDLES_HIDE_MS); showHandles.value = false }
        }

        val footer: @Composable () -> Unit = {
            FloatFooter(
                onDragStart = { dragging = true; showHandles.value = false },
                onDrag = { d, fingerY ->
                    x += d.x; y += d.y
                    overDock = dockEnabled && fingerY > screenH - dockZone
                },
                onDragEnd = {
                    dragging = false
                    if (overDock) {
                        overDock = false
                        KeyboardMorph.toDocked()
                        settings.floating.set(false) // thả vào vùng xanh: về bàn phím thường (nở ra từ khay nổi)
                    } else {
                        clampPos(); save(); showHandles.value = true
                    }
                },
                onClose = {
                    if (land) {
                        // màn ngang: mũi tên ở đáy = chuyển bàn phím nổi thành bàn phím ngang (gắn đáy); mũi tên trên cùng đã lo việc đóng
                        KeyboardMorph.toDocked()
                        settings.floating.set(false)
                        return@FloatFooter
                    }
                    scope.launch {
                        exit.animateTo(1f, tween(220))
                        currentOnDismiss?.invoke()
                        delay(400)
                        exit.snapTo(0f) // lần hiện sau đã sẵn sàng
                    }
                },
            )
        }

        if (!x.isNaN() || kbH == 0) {
            Box(
                Modifier
                    .offset { IntOffset((if (x.isNaN()) 0f else x - margin).roundToInt(), (if (y.isNaN()) screenH else y - margin).roundToInt()) }
                    .graphicsLayer {
                        // trồi từ dưới lên khi hiện, trượt xuống + mờ dần khi đóng
                        // lượt đo kích thước đầu tiên (chưa có vị trí) thì ẩn hẳn
                        alpha = if (x.isNaN()) 0f else (1f - exit.value) * (1f - enter.value)
                        translationY = exit.value * 60.dp.toPx() + enter.value * 110.dp.toPx()
                    }
                    .onGloballyPositioned { if (kbH > 0 && !x.isNaN()) currentOnBounds?.invoke(it.boundsInWindow()) }
                    .padding(FrameMargin),
            ) {
                Box(
                    Modifier
                        .width(with(density) { kbW.toDp() })
                        .onSizeChanged {
                            kbH = it.height
                            if (!anchorBottom.isNaN()) y = anchorBottom - it.height
                        }
                        .onGloballyPositioned { if (!x.isNaN()) KeyboardMorph.floatRect = it.boundsOnScreen(view) }
                        .morphFrom(morph, ready = !x.isNaN() && kbH > 0)
                        // 4 dấu góc bo tròn ôm sát góc khay, cách mép một chút; hiện ra thì từ ngoài "khép" vào
                        .drawWithContent {
                            drawContent()
                            if (handlesAlpha > 0f) {
                                val out = BracketGap.toPx() + IosDimens.ResizeBracketStroke.toPx() / 2f + (1f - handlesAlpha) * 6.dp.toPx()
                                drawRoundBrackets(
                                    Rect(-out, -out, size.width + out, size.height + out),
                                    topRadius = IosDimens.FloatCorner.toPx() + out, bottomRadius = IosDimens.FloatCorner.toPx() + out,
                                    alpha = handlesAlpha,
                                )
                            }
                        }
                        // chạm vào phím thì giấu dấu góc (chỉ nhìn, không cướp cú chạm của phím)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val e = awaitPointerEvent(PointerEventPass.Initial)
                                    if (e.type == PointerEventType.Press) showHandles.value = false
                                }
                            }
                        },
                ) { content(footer) }
                if (showHandles.value) {
                    CornerHandles(
                        onResize = { corner, d ->
                            // kéo góc: mép đối diện đứng yên, khay to / nhỏ theo cả bề ngang LẪN bề cao
                            // (chiều cao đi theo bề ngang nên đổi độ dời dọc sang độ dời ngang theo tỉ lệ khay; lấy chiều kéo mạnh hơn)
                            val dwX = if (corner.left) -d.x else d.x
                            val dhY = if (corner.top) -d.y else d.y
                            val dwFromH = if (kbH > 0) dhY * (kbW / kbH.toFloat()) else 0f
                            val dw = if (kotlin.math.abs(dwX) >= kotlin.math.abs(dwFromH)) dwX else dwFromH
                            val old = kbW
                            widthPct = ((old + dw) / refW * 100f).coerceIn(settings.floatWidth.min.toFloat(), settings.floatWidth.max.toFloat())
                            val newW = refW * widthPct / 100f
                            if (corner.left) x += old - newW
                            // kéo góc trên: mép DƯỚI đứng yên. Chiều cao thật chỉ biết sau khi đo lại,
                            // nên ghi nhớ mép dưới rồi đặt lại y lúc đo xong (onSizeChanged)
                            if (corner.top && anchorBottom.isNaN()) anchorBottom = y + kbH
                            // cập nhật cỡ ngay khi kéo: chiều cao phím đi theo bề ngang (xem floatScale trong KeyboardView)
                            settings.floatWidth.set(widthPct.roundToInt())
                        },
                        onEnd = {
                            save()
                            // lần đo lại cuối cùng có thể đến sau khi thả tay: giữ mép dưới thêm một nhịp rồi mới thôi
                            scope.launch { delay(120); anchorBottom = Float.NaN; clampPos(); save() }
                        },
                    )
                }
            }
        }

        // vùng xanh ở đáy màn khi đang kéo xuống gần đáy: trồi từ mép dưới lên (chỉ dịch + mờ trong lúc VẼ, không dựng lại bố cục mỗi khung hình
        // nên mượt cả trong cửa sổ bàn phím hệ thống), bo góc theo góc bo thật của màn hình máy
        val nearBottom = dockEnabled && dragging && !y.isNaN() && y + kbH > screenH - dockZone * 2f
        val zoneShow by animateFloatAsState(
            if (nearBottom) 1f else 0f,
            if (nearBottom) spring(dampingRatio = 1f, stiffness = 420f) else tween(200, easing = Motion.Decelerate),
            label = "dockZone",
        )
        if (zoneShow > 0.001f) {
            val cornerPx = remember(nearBottom) { screenBottomCornerPx(view) ?: with(density) { IosDimens.TrayBottomCorner.toPx() } }
            val cornerDp = with(density) { cornerPx.toDp() }
            val zoneShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = cornerDp, bottomEnd = cornerDp)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(IosDimens.DockZoneHeight)
                    .graphicsLayer {
                        translationY = (1f - zoneShow.coerceIn(0f, 1f)) * size.height
                        alpha = (zoneShow * 1.5f).coerceIn(0f, 1f)
                    }
                    .background(Blue.copy(alpha = if (overDock) 0.55f else 0.30f), zoneShape)
                    .border(3.dp, Blue, zoneShape),
            )
        }
    }
}

// Bán kính góc bo ở đáy màn hình thật của máy (Android 12+); null nếu máy không báo
private fun screenBottomCornerPx(view: android.view.View): Float? {
    if (android.os.Build.VERSION.SDK_INT < 31) return null
    val insets = view.rootWindowInsets ?: return null
    val left = insets.getRoundedCorner(android.view.RoundedCorner.POSITION_BOTTOM_LEFT)?.radius
    val right = insets.getRoundedCorner(android.view.RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius
    return listOfNotNull(left, right).maxOrNull()?.takeIf { it > 0 }?.toFloat()
}

// Dải dưới đáy bàn phím nổi: thanh xám ở giữa để kéo, nút mũi tên xuống bên phải để ẩn bàn phím
@Composable
private fun FloatFooter(
    onDragStart: () -> Unit,
    onDrag: (Offset, Float) -> Unit, // (độ dời, toạ độ y của ngón tay trên màn)
    onDragEnd: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalKeyboardPalette.current
    val start by rememberUpdatedState(onDragStart)
    val move by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onDragEnd)
    val close by rememberUpdatedState(onClose)
    var top by remember { mutableFloatStateOf(0f) } // y của dải trên màn (để biết ngón tay đang ở đâu)
    Box(
        Modifier
            .fillMaxWidth()
            .height(IosDimens.FloatHandleHeight)
            .onGloballyPositioned { top = it.boundsInWindow().top },
    ) {
        // vùng kéo: rộng hơn hẳn thanh xám cho dễ trúng
        Box(
            Modifier
                .align(Alignment.Center)
                .width(160.dp)
                .height(IosDimens.FloatHandleHeight)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start() },
                        onDragEnd = { end() },
                        onDragCancel = { end() },
                    ) { change, amount ->
                        change.consume()
                        move(amount, top + change.position.y)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(72.dp).height(5.dp).background(palette.secondaryText.copy(alpha = 0.8f), CircleShape))
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(IosDimens.FloatHandleHeight)
                .pointerInput(Unit) { detectTapGestures { close() } },
            contentAlignment = Alignment.Center,
        ) { ChevronDownIcon(palette.secondaryText, size = 18.dp) }
    }
}

private class Corner(val left: Boolean, val top: Boolean)

private val FrameMargin = 14.dp // khoảng chừa quanh bàn phím nổi cho dấu góc

// 4 dấu góc xanh hình chữ L, nằm sát ngoài 4 góc khay; mỗi dấu có vùng chạm 44dp để kéo đổi cỡ
@Composable
private fun BoxScope.CornerHandles(onResize: (Corner, Offset) -> Unit, onEnd: () -> Unit) {
    val resize by rememberUpdatedState(onResize)
    val end by rememberUpdatedState(onEnd)
    listOf(Corner(true, true), Corner(false, true), Corner(true, false), Corner(false, false)).forEach { c ->
        val align = when {
            c.left && c.top -> Alignment.TopStart
            c.top -> Alignment.TopEnd
            c.left -> Alignment.BottomStart
            else -> Alignment.BottomEnd
        }
        Box(
            Modifier
                .align(align)
                .offset(x = if (c.left) -FrameMargin else FrameMargin, y = if (c.top) -FrameMargin else FrameMargin)
                .size(44.dp)
                .systemGestureExclusion()
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = { end() }, onDragCancel = { end() }) { change, amount ->
                        change.consume()
                        resize(c, amount)
                    }
                }
        )
    }
}

private val BracketGap = 1.dp // khe hở giữa dấu góc và mép bàn phím (~2-3 px)

// Vẽ 4 dấu góc: mỗi dấu là một cung tròn ôm theo góc bo của `rect` + hai cánh ngắn chạy dọc hai cạnh
private fun DrawScope.drawRoundBrackets(rect: Rect, topRadius: Float, bottomRadius: Float, alpha: Float = 1f) {
    val arm = IosDimens.ResizeBracket.toPx()
    val stroke = Stroke(width = IosDimens.ResizeBracketStroke.toPx(), cap = StrokeCap.Round)
    val color = Blue.copy(alpha = alpha)
    fun corner(r: Float, build: Path.(Float) -> Unit) {
        val rr = r.coerceAtLeast(0f)
        drawPath(Path().apply { build(rr) }, color, style = stroke)
    }
    val (l, t, r, b) = listOf(rect.left, rect.top, rect.right, rect.bottom)
    corner(topRadius) { rr -> // trên trái
        moveTo(l, t + rr + arm); lineTo(l, t + rr)
        arcTo(Rect(l, t, l + 2 * rr, t + 2 * rr), 180f, 90f, false); lineTo(l + rr + arm, t)
    }
    corner(topRadius) { rr -> // trên phải
        moveTo(r - rr - arm, t); lineTo(r - rr, t)
        arcTo(Rect(r - 2 * rr, t, r, t + 2 * rr), 270f, 90f, false); lineTo(r, t + rr + arm)
    }
    corner(bottomRadius) { rr -> // dưới phải
        moveTo(r, b - rr - arm); lineTo(r, b - rr)
        arcTo(Rect(r - 2 * rr, b - 2 * rr, r, b), 0f, 90f, false); lineTo(r - rr - arm, b)
    }
    corner(bottomRadius) { rr -> // dưới trái
        moveTo(l + rr + arm, b); lineTo(l + rr, b)
        arcTo(Rect(l, b - 2 * rr, l + 2 * rr, b), 90f, 90f, false); lineTo(l, b - rr - arm)
    }
}

// ---------------------------------------------------------------- bàn phím gắn đáy + chế độ chỉnh cỡ

// Bàn phím thường (gắn đáy). Có thể thu hẹp (gõ một tay) và đặt lệch trái / phải theo cài đặt.
// adjusting = true: phủ lớp chỉnh cỡ lên trên (dấu góc + thanh giữa các cạnh + 3 nút ở giữa).
@Composable
fun DockedKeyboard(
    settings: KeyboardSettings,
    adjusting: Boolean,
    onAdjustDone: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fullW = constraints.maxWidth.toFloat()
        var widthPct by remember { mutableFloatStateOf(settings.dockWidth.value.toFloat()) }
        var shiftPct by remember { mutableFloatStateOf(settings.dockShift.value.toFloat()) }
        // cài đặt đổi ở chỗ khác (vd. Reset) thì cập nhật theo
        LaunchedEffect(settings.dockWidth.value, settings.dockShift.value) {
            widthPct = settings.dockWidth.value.toFloat(); shiftPct = settings.dockShift.value.toFloat()
        }
        val w = fullW * widthPct / 100f
        val x = (fullW - w) * shiftPct / 100f
        val blurRadius by animateDpAsState(if (adjusting) 7.dp else 0.dp, tween(260), label = "adjustBlur")
        var heightAcc by remember(adjusting) { mutableFloatStateOf(settings.keyHeightPercent.value.toFloat()) }
        var liftAcc by remember(adjusting) { mutableFloatStateOf(settings.bottomSpacing.value.toFloat()) }
        val keyRowsPx = with(density) { (IosDimens.KeyHeight * 4).toPx() }
        fun setEdges(left: Float, right: Float) {
            // left / right: vị trí mép trái / phải mới (px)
            val newW = (right - left).coerceIn(fullW * settings.dockWidth.min / 100f, fullW)
            widthPct = newW / fullW * 100f
            val free = fullW - newW
            shiftPct = if (free < 1f) 50f else (left.coerceIn(0f, free) / free * 100f)
            settings.dockWidth.set(widthPct.roundToInt())
            settings.dockShift.set(shiftPct.roundToInt())
        }
        fun addHeight(dyUp: Float) {
            heightAcc = (heightAcc + dyUp / keyRowsPx * 100f).coerceIn(settings.keyHeightPercent.min.toFloat(), settings.keyHeightPercent.max.toFloat())
            settings.keyHeightPercent.set(heightAcc.roundToInt())
        }
        fun addLift(dyUp: Float) {
            liftAcc = (liftAcc + with(density) { dyUp.toDp().value }).coerceIn(0f, settings.bottomSpacing.max.toFloat())
            settings.bottomSpacing.set(liftAcc.roundToInt())
        }
        val morph = remember { KeyboardMorph.take() } // vừa thôi nổi: nở ra từ khay nổi
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), 0) }
                .width(with(density) { w.toDp() })
                .onGloballyPositioned { KeyboardMorph.dockRect = it.boundsOnScreen(view) }
                .morphFrom(morph),
        ) {
            // đang chỉnh: bàn phím mờ đục đi để 3 nút + dấu góc nổi bật
            Box(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier) { content() }
            AnimatedVisibility(
                visible = adjusting,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(240)) + scaleIn(tween(320, easing = Motion.Decelerate), initialScale = 0.97f),
                exit = fadeOut(tween(180)),
            ) {
                Box(Modifier.fillMaxSize()) {
                    AdjustOverlay(
                        topRadius = IosDimens.TrayCorner,
                        bottomRadius = 0.dp,
                        onEdge = { edge, d ->
                            when (edge) {
                                Edge.LEFT -> setEdges(x + d.x, x + w)
                                Edge.RIGHT -> setEdges(x, x + w + d.x)
                                Edge.TOP -> addHeight(-d.y)
                                Edge.BOTTOM -> addLift(-d.y)
                                Edge.TOP_LEFT -> { setEdges(x + d.x, x + w); addHeight(-d.y) }
                                Edge.TOP_RIGHT -> { setEdges(x, x + w + d.x); addHeight(-d.y) }
                                Edge.BOTTOM_LEFT -> { setEdges(x + d.x, x + w); addLift(-d.y) }
                                Edge.BOTTOM_RIGHT -> { setEdges(x, x + w + d.x); addLift(-d.y) }
                            }
                        },
                        onMove = { d ->
                            val free = fullW - w
                            if (free >= 1f) {
                                shiftPct = ((x + d.x).coerceIn(0f, free) / free * 100f)
                                settings.dockShift.set(shiftPct.roundToInt())
                            }
                            addLift(-d.y)
                        },
                        onReset = {
                            settings.dockWidth.reset(); settings.dockShift.reset()
                            settings.keyHeightPercent.reset(); settings.bottomSpacing.reset()
                            heightAcc = settings.keyHeightPercent.value.toFloat(); liftAcc = settings.bottomSpacing.value.toFloat()
                        },
                        onDone = onAdjustDone,
                    )
                }
            }
        }
    }
}

private enum class Edge { LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

// Lớp chỉnh cỡ phủ lên bàn phím: làm tối phím (không gõ được), dấu góc + 4 thanh giữa cạnh để kéo,
// giữa là 3 nút tròn: đặt lại | kéo để di chuyển | xong
@Composable
private fun BoxScope.AdjustOverlay(
    topRadius: Dp,
    bottomRadius: Dp,
    onEdge: (Edge, Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    val edge by rememberUpdatedState(onEdge)
    val move by rememberUpdatedState(onMove)
    val reset by rememberUpdatedState(onReset)
    val done by rememberUpdatedState(onDone)
    Box(
        Modifier
            .matchParentSize()
            // lớp kính đục: tối + hơi trắng sữa phủ lên bàn phím đã làm mờ
            .background(Color(0xFF1C1C1E).copy(alpha = 0.42f), RoundedCornerShape(topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius))
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius))
            .drawWithContent {
                drawContent()
                // dấu góc bo tròn nằm bên trong khay, cách mép một chút
                val inset = BracketGap.toPx() + IosDimens.ResizeBracketStroke.toPx() / 2f
                drawRoundBrackets(
                    Rect(inset, inset, size.width - inset, size.height - inset),
                    topRadius = topRadius.toPx() - inset, bottomRadius = bottomRadius.toPx() - inset,
                )
            }
            // nuốt mọi cú chạm để phím bên dưới không gõ ra chữ
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } },
    )
    val bar = Modifier.background(Blue, CircleShape)
    // thanh giữa 4 cạnh
    EdgeHandle(Alignment.TopCenter, 64.dp, 44.dp, { edge(Edge.TOP, it) }) { Box(bar.width(46.dp).height(6.dp)) }
    EdgeHandle(Alignment.BottomCenter, 64.dp, 44.dp, { edge(Edge.BOTTOM, it) }) { Box(bar.width(46.dp).height(6.dp)) }
    EdgeHandle(Alignment.CenterStart, 44.dp, 64.dp, { edge(Edge.LEFT, it) }) { Box(bar.width(6.dp).height(46.dp)) }
    EdgeHandle(Alignment.CenterEnd, 44.dp, 64.dp, { edge(Edge.RIGHT, it) }) { Box(bar.width(6.dp).height(46.dp)) }
    // 4 dấu góc
    listOf(
        Triple(Alignment.TopStart, Edge.TOP_LEFT, Corner(true, true)),
        Triple(Alignment.TopEnd, Edge.TOP_RIGHT, Corner(false, true)),
        Triple(Alignment.BottomStart, Edge.BOTTOM_LEFT, Corner(true, false)),
        Triple(Alignment.BottomEnd, Edge.BOTTOM_RIGHT, Corner(false, false)),
    ).forEach { (align, e, _) ->
        Box(
            Modifier
                .align(align)
                .size(48.dp)
                .systemGestureExclusion()
                .pointerInput(Unit) { detectDragGestures { ch, d -> ch.consume(); edge(e, d) } },
        )
    }
    // 3 nút ở giữa
    Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(Blue, Modifier.pointerInput(Unit) { detectTapGestures { reset() } }) { ResetIcon(Color.White, size = 22.dp) }
        RoundButton(Color(0xFF2C2C2E), Modifier.pointerInput(Unit) { detectDragGestures { ch, d -> ch.consume(); move(d) } }) {
            FloatKeyboardIcon(Blue, size = 24.dp)
        }
        RoundButton(Blue, Modifier.pointerInput(Unit) { detectTapGestures { done() } }) { CheckIcon(Color.White, size = 22.dp) }
    }
}

@Composable
private fun BoxScope.EdgeHandle(align: Alignment, w: Dp, h: Dp, onDrag: (Offset) -> Unit, content: @Composable () -> Unit) {
    val drag by rememberUpdatedState(onDrag)
    Box(
        Modifier
            .align(align)
            .size(w, h)
            .systemGestureExclusion() // sát mép màn: không để vuốt "Quay lại" của Android cướp mất cú kéo
            .pointerInput(Unit) { detectDragGestures { ch, d -> ch.consume(); drag(d) } },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun RoundButton(color: Color, modifier: Modifier, icon: @Composable () -> Unit) {
    Box(modifier.size(IosDimens.AdjustButton).shadow(6.dp, CircleShape).background(color, CircleShape), contentAlignment = Alignment.Center) { icon() }
}
