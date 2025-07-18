package com.example.vlc.widgets

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.vlc.ui.theme.primeColor

fun Context.isTelevision(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

@Composable
fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isFocused: MutableState<Boolean>,
    icon: ImageVector,
    description: String,
    tint: Color,
    onUserInteracted: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val isTV = remember { context.isTelevision() }

    val iconSize = 48.dp

    Box(
        modifier = modifier
            .size(iconSize)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                isFocused.value = it.isFocused
                if (it.isFocused) {
                    onUserInteracted?.invoke()
                }
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> {
                            focusManager.moveFocus(FocusDirection.Right)
                            true
                        }
                        Key.DirectionLeft -> {
                            focusManager.moveFocus(FocusDirection.Left)
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .background(
                color = when {
                    isTV && isFocused.value && !enabled -> Color.Gray.copy(alpha = 0.3f)
                    isTV && isFocused.value -> Color.White
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .focusable()
            .clickable(
                enabled = enabled,
                onClick = {
                    onUserInteracted?.invoke()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (!enabled) tint.copy(alpha = 0.4f)
            else if (isFocused.value && isTV) primeColor
            else tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
