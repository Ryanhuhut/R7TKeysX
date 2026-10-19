package com.r7tkeysx.app.ui.settings

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.r7tkeysx.app.data.KeyboardSettings
import com.r7tkeysx.app.ui.keyboard.CheckIcon
import com.r7tkeysx.app.ui.keyboard.TrashIcon
import com.r7tkeysx.app.ui.keyboard.pressScale
import com.r7tkeysx.app.ui.theme.IosColors
import com.r7tkeysx.app.ui.theme.IosDimens
import com.r7tkeysx.app.ui.theme.Motion
import com.r7tkeysx.app.voice.ModelState
import com.r7tkeysx.app.voice.NetworkKind
import com.r7tkeysx.app.voice.VoiceInput
import com.r7tkeysx.app.voice.VoiceMode
import com.r7tkeysx.app.voice.VoiceModel
import com.r7tkeysx.app.voice.VoiceModelManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TrashRed = Color(0xFFEB5545)
private val SwipeCommit = 96.dp            // vuốt sang trái quá chừng này là xóa
private const val SwipeFlingVelocity = 2400f // hoặc hất rất nhanh hơn chừng này (px/giây)

// Trang Voice Input: chọn chế độ (một ngôn ngữ / đa ngôn ngữ), tải model, vuốt trái để xóa model.
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun VoiceInputPage(settings: KeyboardSettings) {
    val context = LocalContext.current
    val models = remember { VoiceModelManager.get(context) }
    val voice = remember { VoiceInput.get(context) }
    var micGranted by remember { mutableStateOf(voice.hasMicPermission()) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    var confirmMobile by remember { mutableStateOf<VoiceModel?>(null) } // đang hỏi: tải bằng dữ liệu di động?
    var confirmDelete by remember { mutableStateOf<VoiceModel?>(null) } // đang hỏi: chắc chắn xóa model?
    LaunchedEffect(Unit) { models.resumePending() } // đã chọn từ trước mà lúc đó chưa có Wi-Fi: có Wi-Fi rồi thì tải luôn

    val mode = settings.voiceMode.value

    Group(footer = "Tap the microphone key and speak. Text appears each time you pause; tap again or stay quiet to stop.") {
        ToggleRow("Show Microphone Key", settings.showMicKey.value, settings.showMicKey::set)
    }

    Group("Voice recognition", footer = "Models download once over Wi-Fi (on mobile data you are asked first), then work fully offline.") {
        VoiceMode.entries.forEachIndexed { i, m ->
            if (i > 0) Divider()
            ModeRow(m, selected = mode == m) { settings.voiceMode.set(m) }
        }
    }

    // danh sách model đổi theo chế độ: mờ + trượt nhẹ, khung đổi cao mượt
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(tween(300, delayMillis = 80)) + slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 10 }) togetherWith
                fadeOut(tween(140)) using SizeTransform(clip = false) { _, _ -> tween(340, easing = FastOutSlowInEasing) }
        },
        label = "voiceModels",
    ) { m ->
        Column {
            if (m == VoiceMode.ONE_LANGUAGE) {
                Group(
                    "Language models",
                    footer = "The model that matches your keyboard language listens. Other keyboard languages use English. " +
                        "Swipe a downloaded model to the left to delete it.",
                ) {
                    VoiceModel.perLanguageModels.forEachIndexed { i, model ->
                        if (i > 0) Divider()
                        ModelRow(
                            model = model, state = models.state(model), selected = null,
                            onClick = {
                                if (models.state(model).let { it is ModelState.NotInstalled || it is ModelState.Failed }) {
                                    if (models.request(model) == NetworkKind.CELLULAR) confirmMobile = model
                                }
                            },
                            onCancel = { models.cancel(model) },
                            onDeleteRequest = { confirmDelete = model },
                        )
                    }
                }
            } else {
                Group(
                    "Multilingual model",
                    footer = "Pick one. They understand many languages by themselves; bigger ones are more accurate but slower " +
                        "and need more memory (Qwen about 1.5 GB). Swipe a downloaded model to the left to delete it.",
                ) {
                    VoiceModel.multilingualModels.forEachIndexed { i, model ->
                        if (i > 0) Divider()
                        ModelRow(
                            model = model, state = models.state(model), selected = settings.voiceMultiModel.value == model,
                            onClick = {
                                settings.voiceMultiModel.set(model)
                                if (models.state(model).let { it is ModelState.NotInstalled || it is ModelState.Failed }) {
                                    if (models.request(model) == NetworkKind.CELLULAR) confirmMobile = model
                                }
                            },
                            onCancel = {
                                models.cancel(model)
                                // đang chọn đúng model vừa huỷ (chưa tải xong): chuyển dấu chọn sang một model đã có sẵn
                                if (settings.voiceMultiModel.value == model) {
                                    VoiceModel.multilingualModels.firstOrNull { models.isInstalled(it) }?.let { settings.voiceMultiModel.set(it) }
                                }
                            },
                            onDeleteRequest = { confirmDelete = model },
                        )
                    }
                }
            }
        }
    }

    Group("Microphone") {
        if (micGranted) InfoRow("Microphone", "Allowed ✓")
        else ChevronRow("Microphone", "Tap to allow") { askMic.launch(Manifest.permission.RECORD_AUDIO) }
    }

    confirmMobile?.let { model ->
        VoiceDialog(
            title = "Use mobile data?",
            message = "You are not on Wi-Fi. ${model.label} is about ${model.sizeLabel}. Download it now with mobile data, or wait and it will download automatically when Wi-Fi is available.",
            confirmLabel = "Download now", cancelLabel = "Wait for Wi-Fi",
            onConfirm = { confirmMobile = null; models.download(model) },
            onDismiss = { confirmMobile = null; models.postponeToWifi(model) },
        )
    }
    confirmDelete?.let { model ->
        VoiceDialog(
            title = "Are you sure?",
            message = "${model.label} (${model.sizeLabel}) will be removed from this phone. You will need to download it again to use it.",
            confirmLabel = "Delete", confirmDestructive = true, cancelLabel = "Cancel",
            onConfirm = {
                confirmDelete = null
                models.delete(model)
                if (settings.voiceMultiModel.value == model) {
                    VoiceModel.multilingualModels.firstOrNull { it != model && models.isInstalled(it) }?.let { settings.voiceMultiModel.set(it) }
                }
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

// Dòng chọn chế độ: tên + mô tả, dấu tick xanh bật lên có hiệu ứng phóng nhẹ
@Composable
private fun ModeRow(mode: VoiceMode, selected: Boolean, onClick: () -> Unit) {
    val currentClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (pressed) IosColors.RowPressed else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { currentClick() })
            }
            .padding(horizontal = IosDimens.CardPadding + 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(mode.label, color = IosColors.PrimaryText, fontSize = 18.sp)
            Spacer(Modifier.height(3.dp))
            Text(mode.description, color = IosColors.SecondaryText, fontSize = 14.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.width(12.dp))
        TickSlot(selected)
    }
}

@Composable
private fun TickSlot(selected: Boolean) {
    Box(Modifier.size(22.dp)) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(160)) + scaleIn(spring(0.6f, 600f), initialScale = 0.4f),
            exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.6f),
        ) { CheckIcon(IosColors.Accent) }
    }
}

// Một dòng model: tên + mô tả; bên phải: dung lượng / vòng tròn tiến độ (model nhỏ) / "Ready" / tick chọn.
// Model to đang tải thì mở ra thanh tiến độ dài kèm nút Cancel. Vuốt sang trái (khi đã tải) để xóa.
@Composable
private fun ModelRow(
    model: VoiceModel,
    state: ModelState,
    selected: Boolean?, // null = chế độ không có khái niệm "đang chọn" (mỗi ngôn ngữ một model)
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val currentClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    val downloading = state as? ModelState.Downloading
    val installed = state == ModelState.Installed
    // khi tải xong vòng phải chạy nốt tới 100% (state đổi sang Installed thì progress không được tụt về 0)
    val progress by animateFloatAsState(
        if (installed) 1f else downloading?.progress ?: 0f, tween(450, easing = LinearOutSlowInEasing), label = "progress",
    )
    // 0 -> 1: vòng tròn chuyển thành đĩa xanh có dấu tích; bắt đầu SAU khi vòng đã đầy (delay); trang mở khi đã tải sẵn thì đứng yên
    val done by animateFloatAsState(
        if (installed) 1f else 0f, tween(380, delayMillis = if (installed) 420 else 0, easing = FastOutSlowInEasing), label = "done",
    )
    val kind = when (state) {
        ModelState.Installed -> 0
        is ModelState.Downloading -> 1
        is ModelState.Failed -> 2
        ModelState.NotInstalled -> 3
    }

    SwipeToDeleteBox(enabled = installed, onDelete = onDeleteRequest) {
        Column(Modifier.fillMaxWidth().background(IosColors.Card).background(if (pressed) IosColors.RowPressed else Color.Transparent)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { currentClick() })
                    }
                    .padding(horizontal = IosDimens.CardPadding + 4.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(model.label, color = IosColors.PrimaryText, fontSize = 18.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(model.description, color = IosColors.SecondaryText, fontSize = 14.sp, lineHeight = 19.sp)
                }
                Spacer(Modifier.width(12.dp))
                // bên phải: [chữ trạng thái] [Ready] [vòng tròn / tích xanh]. Tải xong: vòng đầy -> hóa tích xanh -> chữ "Ready" mọc ra bên trái nó
                val showIcon = installed || (downloading != null && model.small)
                val statusText = when {
                    downloading != null -> "${(progress * 100).toInt()}%"
                    state is ModelState.Failed -> "Retry"
                    else -> model.sizeLabel
                }
                AnimatedVisibility(
                    visible = !showIcon,
                    enter = fadeIn(tween(200)), exit = fadeOut(tween(140)) + shrinkHorizontally(tween(200)),
                ) { Text(statusText, color = if (state is ModelState.Failed) TrashRed else IosColors.SecondaryText, fontSize = 16.sp, maxLines = 1) }
                AnimatedVisibility(
                    visible = installed,
                    enter = expandHorizontally(tween(340, delayMillis = 700, easing = FastOutSlowInEasing), expandFrom = Alignment.End) + fadeIn(tween(240, delayMillis = 760)),
                    exit = shrinkHorizontally(tween(200), shrinkTowards = Alignment.End) + fadeOut(tween(120)),
                ) { Text("Ready", color = IosColors.SecondaryText, fontSize = 16.sp, maxLines = 1, modifier = Modifier.padding(end = 8.dp)) }
                AnimatedVisibility(
                    visible = showIcon,
                    enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.5f), exit = fadeOut(tween(140)),
                ) { ProgressToCheck(progress, done, 26.dp) }
                if (selected != null) {
                    Spacer(Modifier.width(10.dp))
                    TickSlot(selected)
                }
            }
            // model to đang tải: thanh dài mở ra mượt + nút Cancel
            AnimatedVisibility(
                visible = downloading != null && !model.small,
                enter = expandVertically(tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(240)),
                exit = shrinkVertically(tween(260)) + fadeOut(tween(160)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = IosDimens.CardPadding + 4.dp, end = IosDimens.CardPadding + 4.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Color(0xFF3A3A3C))) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0.02f, 1f)) // luôn có một đoạn nhỏ bo tròn để thấy bắt đầu
                                .clip(CircleShape)
                                .background(IosColors.Accent),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Cancel", color = IosColors.Accent, fontSize = 15.sp, modifier = Modifier.pointerInput(Unit) { detectTapGestures { onCancel() } })
                }
            }
        }
    }
}

// Vòng tiến độ (hai đầu cung bo tròn, chạy đầy theo %) rồi biến thành đĩa xanh có dấu tích:
// done 0 -> 1: vòng mờ đi, đĩa xanh phình ra từ giữa, dấu tích được vẽ nét dần
@Composable
private fun ProgressToCheck(progress: Float, done: Float, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val stroke = 3.5.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        val ringAlpha = (1f - done * 1.6f).coerceIn(0f, 1f)
        if (ringAlpha > 0f) {
            drawArc(Color(0xFF3A3A3C).copy(alpha = ringAlpha), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = (progress.coerceIn(0f, 1f) * 360f).coerceAtLeast(1f)
            drawArc(IosColors.Accent.copy(alpha = ringAlpha), -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (done > 0f) {
            val r = this.size.minDimension / 2f * FastOutSlowInEasing.transform(done.coerceIn(0f, 1f))
            drawCircle(IosColors.StatusGreen, r, center)
            val c = ((done - 0.45f) / 0.55f).coerceIn(0f, 1f)
            if (c > 0f) {
                val w = this.size.width
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.28f, w * 0.52f); lineTo(w * 0.43f, w * 0.67f); lineTo(w * 0.72f, w * 0.35f)
                }
                val seg = androidx.compose.ui.graphics.Path()
                val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                pm.getSegment(0f, pm.length * c, seg, true)
                drawPath(seg, Color.Black, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

// Hộp bọc một dòng: vuốt sang trái lộ vùng đỏ + thùng rác; vuốt đủ xa (hoặc hất mạnh) thì dòng trượt ra ngoài và gọi onDelete
// (để hỏi "Are you sure?"), rồi dòng trượt về chỗ cũ. enabled = false thì không vuốt được.
@Composable
private fun SwipeToDeleteBox(enabled: Boolean, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val currentDelete by rememberUpdatedState(onDelete)
    val view = LocalView.current
    val commitPx = with(LocalDensity.current) { SwipeCommit.toPx() }
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) } // 0 = đứng yên, âm = đang vuốt sang trái
    var widthPx by remember { mutableFloatStateOf(0f) }
    var committing by remember { mutableStateOf(false) }
    var pastThreshold by remember { mutableStateOf(false) }

    fun settleBack() {
        scope.launch { animate(offsetX, 0f, animationSpec = spring(0.8f, 380f)) { v, _ -> offsetX = v } }
    }
    fun commit() {
        committing = true
        scope.launch {
            animate(offsetX, -widthPx, animationSpec = tween(Motion.SWIPE_OUT_MS, easing = Motion.Decelerate)) { v, _ -> offsetX = v }
            currentDelete()
            delay(220)
            animate(offsetX, 0f, animationSpec = spring(0.85f, 320f)) { v, _ -> offsetX = v } // hộp thoại hiện rồi, dòng về lại chỗ cũ
            committing = false
            pastThreshold = false
        }
    }

    Box(Modifier.fillMaxWidth().onSizeChanged { widthPx = it.width.toFloat() }) {
        if (enabled) {
            // vùng đỏ lộ ra rộng dần theo ngón tay; thùng rác ở mép trái vùng đỏ, to dần rồi "nảy" khi quá ngưỡng xóa
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .layout { measurable, constraints ->
                            val w = (-offsetX).roundToInt().coerceAtLeast(0)
                            val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, constraints.maxHeight))
                            layout(w, placeable.height) { placeable.place(0, 0) }
                        }
                        .clipToBounds()
                        .background(TrashRed),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val reveal = (-offsetX / commitPx).coerceIn(0f, 1.4f)
                    TrashIcon(
                        Color.White,
                        modifier = Modifier
                            .padding(start = 34.dp)
                            .graphicsLayer {
                                val s = 0.55f + 0.45f * reveal.coerceAtMost(1f) + (if (reveal >= 1f) 0.18f else 0f)
                                scaleX = s; scaleY = s
                                alpha = (reveal * 1.6f).coerceIn(0f, 1f)
                            },
                    )
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { dx ->
                        if (!committing) {
                            offsetX = (offsetX + dx).coerceIn(-widthPx, 0f)
                            val past = offsetX <= -commitPx
                            if (past != pastThreshold) { pastThreshold = past; view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
                        }
                    },
                    orientation = Orientation.Horizontal,
                    enabled = enabled && !committing,
                    onDragStopped = { velocity -> if (offsetX <= -commitPx || velocity < -SwipeFlingVelocity) commit() else settleBack() },
                ),
        ) { content() }
    }
}

// Hộp thoại kiểu kính tối: phóng ra bằng lò xo, nút bấm có hiệu ứng nhún, đóng thì thu lại mượt
@Composable
internal fun VoiceDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    confirmDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(0.72f, 420f)) }
    fun close(action: () -> Unit) {
        scope.launch { appear.animateTo(0f, tween(170, easing = FastOutSlowInEasing)); action() }
    }
    Dialog(onDismissRequest = { close(onDismiss) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 36.dp)
                .widthIn(max = 340.dp)
                .graphicsLayer {
                    val t = appear.value
                    scaleX = 0.86f + 0.14f * t; scaleY = 0.86f + 0.14f * t
                    alpha = t.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFD1D1D6), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton(cancelLabel, Color(0xFF3A3A3C), Color.White, Modifier.weight(1f)) { close(onDismiss) }
                DialogButton(confirmLabel, if (confirmDestructive) TrashRed else IosColors.Accent, Color.White, Modifier.weight(1f)) { close(onConfirm) }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, background: Color, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    val currentClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .pressScale(pressed)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { currentClick() })
            }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
}
