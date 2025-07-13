package com.example.vlc.widgets


import androidx.compose.foundation.focusable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isFocused: MutableState<Boolean>,
    icon: ImageVector,
    description: String,
    tint: Color
) {
    val focusManager = LocalFocusManager.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused.value = it.isFocused }
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
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (isFocused.value) Color.Cyan else tint
        )
    }
}
