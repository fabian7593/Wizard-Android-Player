package com.example.vlc.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.vlc.ui.theme.primeColor

@Composable
fun CustomVideoSlider(
    currentTime: Long,
    videoLength: Long,
    onSeekChanged: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    focusColor: Color? = null,
    inactiveColor: Color? = null,
    activeColor: Color? = null,
    onFocusDown: (() -> Unit)? = null,
    onUserInteracted: (() -> Unit)? = null,
    modifier: Modifier = Modifier,

) {

    var sliderPosition by remember { mutableStateOf(currentTime.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val isFocused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val keyHoldStartTime = remember { mutableStateOf<Long?>(null) }

    val focusColor = focusColor ?: Color.White
    val trackHeight = 4.dp
    val thumbRadius = 10.dp
    val activeColor = activeColor ?: primeColor
    val thumbColor = if (isFocused.value || isDragging) focusColor else activeColor


    val inactiveColor = inactiveColor ?: Color.Gray
    val density = LocalDensity.current

    LaunchedEffect(currentTime) {
        if (!isDragging && !isFocused.value) {
            sliderPosition = currentTime.toFloat()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbRadius * 2)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                if (!focusState.isFocused) {
                    keyHoldStartTime.value = null
                }
            }
            .onKeyEvent { event ->
                if (!isFocused.value) return@onKeyEvent false

                onUserInteracted?.invoke()

                when {
                    event.key == Key.DirectionRight && event.type == KeyEventType.KeyDown -> {
                        val now = System.currentTimeMillis()
                        val startTime = keyHoldStartTime.value ?: now
                        keyHoldStartTime.value = startTime

                        val heldDuration = now - startTime
                        val seekIncrement = when {
                            heldDuration >= 10_000 -> 30L
                            heldDuration >= 5_000 -> 20L
                            else -> 10L
                        }

                        sliderPosition = (sliderPosition + seekIncrement).coerceAtMost(videoLength.toFloat())
                        onSeekChanged(sliderPosition.toLong())
                        onSeekFinished(sliderPosition.toLong())
                        true
                    }


                    event.key == Key.DirectionLeft && event.type == KeyEventType.KeyDown -> {
                        val now = System.currentTimeMillis()
                        val startTime = keyHoldStartTime.value ?: now
                        keyHoldStartTime.value = startTime

                        val heldDuration = now - startTime
                        val seekIncrement = when {
                            heldDuration >= 10_000 -> 30L
                            heldDuration >= 5_000 -> 20L
                            else -> 10L
                        }

                        sliderPosition = (sliderPosition - seekIncrement).coerceAtLeast(0f)
                        onSeekChanged(sliderPosition.toLong())
                        onSeekFinished(sliderPosition.toLong())
                        true
                    }


                    //event.key == Key.DirectionUp || event.key == Key.DirectionDown -> false
                    event.key == Key.DirectionDown && event.type == KeyEventType.KeyDown -> {
                        onFocusDown?.invoke()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(videoLength) {
                detectTapGestures(
                    onTap = { offset ->
                        val width = size.width.toFloat()
                        val clickedFraction = (offset.x / width).coerceIn(0f, 1f)
                        val newTime = (clickedFraction * videoLength).toLong()
                        sliderPosition = newTime.toFloat()
                        onSeekChanged(newTime)
                        onSeekFinished(newTime)
                        onUserInteracted?.invoke()
                    }
                )
            }
            .pointerInput(videoLength) {
                detectDragGestures(
                    onDragStart = {
                        onUserInteracted?.invoke()
                        isDragging = true
                                  },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished(sliderPosition.toLong())
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        val width = size.width
                        val newX = (sliderPosition / videoLength.toFloat()) * width + dragAmount.x
                        val newPos = ((newX / width) * videoLength).coerceIn(0f, videoLength.toFloat())
                        sliderPosition = newPos
                        onSeekChanged(newPos.toLong())
                        change.consume()
                        onUserInteracted?.invoke()
                    }
                )
            }
            .focusable()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val dynamicThumbRadius = if (isFocused.value) thumbRadius * 1.5f else thumbRadius
            val thumbPx = with(density) { dynamicThumbRadius.toPx() }

            val trackY = canvasHeight / 2
            val progress = sliderPosition / videoLength.toFloat()
            val thumbX = progress * canvasWidth

            drawLine(
                color = inactiveColor,
                start = Offset(0f, trackY),
                end = Offset(canvasWidth, trackY),
                strokeWidth = with(density) { trackHeight.toPx() }
            )

            drawLine(
                color = activeColor,
                start = Offset(0f, trackY),
                end = Offset(thumbX, trackY),
                strokeWidth = with(density) { trackHeight.toPx() }
            )

            drawCircle(
                color = thumbColor,
                radius = thumbPx,
                center = Offset(thumbX, trackY)
            )
        }
    }
}
