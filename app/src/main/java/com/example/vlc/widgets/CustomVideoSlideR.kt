package com.example.vlc.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun CustomVideoSlider(
    currentTime: Long,
    videoLength: Long,
    onSeekChanged: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit
) {
    var sliderPosition by remember { mutableStateOf(currentTime.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    val focusColor = Color(0xFF1976D2)

    val trackHeight = 4.dp
    val thumbRadius = 10.dp
    val thumbColor = if (isDragging) focusColor else Color.Cyan
    val activeColor = Color.White
    val inactiveColor = Color.Gray

    val density = LocalDensity.current

    LaunchedEffect(currentTime) {
        if (!isDragging) {
            sliderPosition = currentTime.toFloat()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbRadius * 2)
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
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished(sliderPosition.toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
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
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val thumbPx = with(density) { thumbRadius.toPx() }
            val trackY = canvasHeight / 2

            val progress = sliderPosition / videoLength.toFloat()
            val thumbX = progress * canvasWidth

            // Fondo inactivo
            drawLine(
                color = inactiveColor,
                start = Offset(0f, trackY),
                end = Offset(canvasWidth, trackY),
                strokeWidth = with(density) { trackHeight.toPx() }
            )

            // Progreso activo
            drawLine(
                color = activeColor,
                start = Offset(0f, trackY),
                end = Offset(thumbX, trackY),
                strokeWidth = with(density) { trackHeight.toPx() }
            )

            // Thumb
            drawCircle(
                color = thumbColor,
                radius = thumbPx,
                center = Offset(thumbX, trackY)
            )
        }
    }
}
