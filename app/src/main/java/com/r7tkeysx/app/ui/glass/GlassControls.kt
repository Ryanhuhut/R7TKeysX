package com.r7tkeysx.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.r7tkeysx.app.ui.theme.IosColors
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

private val TrackOff = Color(0xFF787880).copy(alpha = 0.36f)
private val TickColor = Color(0xFF5A5A60)

// Núm kính dùng chung cho công tắc và thanh trượt. Lúc nghỉ chỉ là núm trắng đơn giản (rất nhẹ cho CPU/GPU, trang có hàng chục núm);
// khi đang nhấn / đang trượt về chỗ cũ mới vẽ kính khúc xạ như thư viện.
@Composable
private fun GlassThumb(
    anim: DampedDragAnimation,
    trackBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    backdropScale: Float,
    lensHeight: androidx.compose.ui.unit.Dp,
    lensAmount: androidx.compose.ui.unit.Dp,
    velocityDivisor: Float,
) {
    val active = anim.pressProgress > 0.002f || abs(anim.scaleX - 1f) > 0.003f || abs(anim.velocity) > 0.05f
    val glassBackdrop = rememberBackdrop(trackBackdrop) { drawBackdrop ->
        val progress = anim.pressProgress
        scale(lerp(2f / 3f, backdropScale, progress), lerp(0f, backdropScale, progress)) { drawBackdrop() }
    }
    Box(
        modifier
            .then(
                if (active) {
                    Modifier.drawBackdrop(
                        backdrop = glassBackdrop,
                        shape = { CircleShape },
                        effects = {
                            val progress = anim.pressProgress
                            blur(8f.dp.toPx() * (1f - progress))
                            lens(lensHeight.toPx() * progress, lensAmount.toPx() * progress, chromaticAberration = true)
                        },
                        highlight = {
                            val progress = anim.pressProgress
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = progress,
                            )
                        },
                        shadow = { Shadow(radius = 4f.dp, color = Color.Black.copy(alpha = 0.18f)) },
                        innerShadow = {
                            val progress = anim.pressProgress
                            InnerShadow(radius = 4f.dp * progress, alpha = progress)
                        },
                        layerBlock = {
                            scaleX = anim.scaleX
                            scaleY = anim.scaleY
                            val velocity = anim.velocity / velocityDivisor
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = anim.pressProgress
                            drawRect(Color.White.copy(alpha = 1f - progress))
                        },
                    )
                } else {
                    Modifier.shadow(2.dp, CircleShape).background(Color.White, CircleShape)
                },
            )
            .size(width, height),
    )
}

// Công tắc kính (phỏng theo LiquidToggle của thư viện): núm trắng, nhấn giữ thì hóa thành thấu kính khúc xạ cái nền xanh/xám bên dưới.
@Composable
fun GlassToggle(
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!LocalGlassEnabled.current) return PlainToggle(selected, onSelect, modifier, enabled)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentEnabled by rememberUpdatedState(enabled)
    val density = LocalDensity.current
    val trackW = 68.dp
    val trackH = 32.dp
    val thumbW = 44.dp
    val thumbH = 28.dp
    val pad = 2.dp
    val dragWidth = with(density) { (trackW - thumbW - pad * 2).toPx() }
    val padPx = with(density) { pad.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (!currentEnabled) return@DampedDragAnimation
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    currentOnSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (currentSelected) 0f else 1f
                    currentOnSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!currentEnabled) return@DampedDragAnimation
                if (!didDrag) didDrag = dragAmount.x != 0f
                fraction = (fraction + dragAmount.x / dragWidth).fastCoerceIn(0f, 1f)
            },
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }.collectLatest { dampedDragAnimation.updateValue(it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { currentSelected }.collectLatest { isSelected ->
            val target = if (isSelected) 1f else 0f
            if (target != fraction) {
                fraction = target
                dampedDragAnimation.animateToValue(target)
            }
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    // Cả công tắc (không chỉ núm) là vùng chạm: chạm đâu cũng bật/tắt, kéo từ đâu cũng được; cao 48dp cho dễ trúng
    Box(modifier.size(trackW, 48.dp).then(dampedDragAnimation.modifier), contentAlignment = Alignment.Center) {
      Box(Modifier.size(trackW, trackH), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(CircleShape)
                .drawBehind { drawRect(androidx.compose.ui.graphics.lerp(TrackOff, IosColors.StatusGreen, dampedDragAnimation.value)) }
                .size(trackW, trackH),
        )
        GlassThumb(
            anim = dampedDragAnimation,
            trackBackdrop = trackBackdrop,
            width = thumbW,
            height = thumbH,
            modifier = Modifier
                .graphicsLayer { translationX = lerp(padPx, padPx + dragWidth, dampedDragAnimation.value) }
                .semantics { role = Role.Switch },
            backdropScale = 0.75f,
            lensHeight = 5.dp,
            lensAmount = 10.dp,
            velocityDivisor = 50f,
        )
      }
    }
}

// Thanh trượt kính (phỏng theo LiquidSlider): kéo hoặc chạm vào thanh; núm trắng phồng thành thấu kính khi nhấn.
@Composable
fun GlassSlider(
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ticks: Int = 0, // > 1: vẽ chấm nhỏ dưới thanh tại mỗi nấc (các thanh có ít giá trị)
) {
    if (!LocalGlassEnabled.current) return PlainSlider(value, min, max, onChange, modifier, enabled, ticks)
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentEnabled by rememberUpdatedState(enabled)
    val range = min.toFloat()..max.toFloat()
    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
        val trackWidth = constraints.maxWidth
        val density = LocalDensity.current
        val thumbPx = with(density) { 46.dp.toPx() }
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentValue.toFloat(),
                valueRange = range,
                visibilityThreshold = (max - min) / 1000f,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = { if (didDrag && currentEnabled) currentOnChange(targetValue.fastRoundToInt()); didDrag = false },
                onDrag = { _, dragAmount ->
                    if (!currentEnabled) return@DampedDragAnimation
                    if (!didDrag) didDrag = dragAmount.x != 0f
                    val delta = (range.endInclusive - range.start) * (dragAmount.x / (trackWidth - thumbPx))
                    currentOnChange((targetValue + delta).coerceIn(range).fastRoundToInt())
                },
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentValue.toFloat() }.collectLatest { v ->
                if (dampedDragAnimation.targetValue != v) dampedDragAnimation.updateValue(v)
            }
        }
        // x trên màn hình -> giá trị (tâm núm đi theo ngón tay)
        fun valueAt(x: Float): Int =
            (range.start + (range.endInclusive - range.start) * ((x - thumbPx / 2f) / (trackWidth - thumbPx))).coerceIn(range).fastRoundToInt()

        // Vùng thanh ray (cao 48dp): chạm = nhảy tới chỗ đó, kéo ngang từ BẤT KỲ chỗ nào trên thanh cũng chỉnh được
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .drawBehind {
                    if (ticks > 1) {
                        val r = 1.8.dp.toPx()
                        val left = thumbPx / 2f
                        val span = size.width - thumbPx
                        for (i in 0 until ticks) {
                            drawCircle(TickColor, r, androidx.compose.ui.geometry.Offset(left + span * i / (ticks - 1), size.height / 2f + 17.dp.toPx()))
                        }
                    }
                }
                .pointerInput(animationScope) {
                    detectTapGestures { position -> if (currentEnabled) currentOnChange(valueAt(position.x)) }
                }
                .pointerInput(animationScope) {
                    detectHorizontalDragGestures(
                        onDragStart = { position ->
                            if (currentEnabled) { dampedDragAnimation.press(); currentOnChange(valueAt(position.x)) }
                        },
                        onDragEnd = { dampedDragAnimation.release() },
                        onDragCancel = { dampedDragAnimation.release() },
                    ) { change, _ ->
                        if (currentEnabled) currentOnChange(valueAt(change.position.x))
                        change.consume()
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.layerBackdrop(trackBackdrop)) {
                Box(Modifier.clip(CircleShape).background(TrackOff).height(8.dp).fillMaxWidth())
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(IosColors.Accent)
                        .height(8.dp)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val width = ((constraints.maxWidth - thumbPx) * dampedDragAnimation.progress + thumbPx / 2f).fastRoundToInt()
                            layout(width, placeable.height) { placeable.place(0, 0) }
                        },
                )
            }
        }

        // Núm: vùng chạm cao 48dp (to hơn hình núm) và nuốt cú kéo ngay từ đầu, nên đặt ngón lên núm là kéo được dù chếch lên xuống
        Box(
            Modifier
                .size(46.dp, 48.dp)
                // núm luôn nằm gọn trong thanh (không thò ra ngoài hai đầu)
                .graphicsLayer { translationX = (trackWidth - size.width) * dampedDragAnimation.progress }
                .then(dampedDragAnimation.modifier),
            contentAlignment = Alignment.Center,
        ) {
            GlassThumb(
                anim = dampedDragAnimation,
                trackBackdrop = trackBackdrop,
                width = 46.dp,
                height = 28.dp,
                backdropScale = 1f,
                lensHeight = 10.dp,
                lensAmount = 14.dp,
                velocityDivisor = 10f,
            )
        }
    }
}
