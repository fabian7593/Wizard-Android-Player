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

@Composable
fun CustomVideoSlider(
    currentTime: Long,
    videoLength: Long,
    onSeekChanged: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier // ✅ AHORA SÍ
) {
    var sliderPosition by remember { mutableStateOf(currentTime.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val isFocused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val focusColor = Color(0xFF1976D2)
    val trackHeight = 4.dp
    val thumbRadius = 10.dp
    val thumbColor = if (isFocused.value || isDragging) focusColor else Color.Cyan
    val activeColor = Color(0xFF1976D2)

    val inactiveColor = Color.Gray
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
            .onFocusChanged { focusState -> isFocused.value = focusState.isFocused }
            .onKeyEvent { event ->
                if (!isFocused.value) return@onKeyEvent false

                when {
                    event.key == Key.DirectionRight && event.type == KeyEventType.KeyDown -> {
                        sliderPosition = (sliderPosition + 5).coerceAtMost(videoLength.toFloat())
                        onSeekChanged(sliderPosition.toLong())
                        onSeekFinished(sliderPosition.toLong())
                        true
                    }

                    event.key == Key.DirectionLeft && event.type == KeyEventType.KeyDown -> {
                        sliderPosition = (sliderPosition - 5).coerceAtLeast(0f)
                        onSeekChanged(sliderPosition.toLong())
                        onSeekFinished(sliderPosition.toLong())
                        true
                    }

                    event.key == Key.DirectionUp || event.key == Key.DirectionDown -> false
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
                    }
                )
            }
            .pointerInput(videoLength) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
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
                    }
                )
            }
            .focusable()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val thumbPx = with(density) { thumbRadius.toPx() }
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
