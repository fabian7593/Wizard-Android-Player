package com.example.vlc.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun ScrollableDialogList(
    title: String,
    items: List<Pair<Int, String>>,
    onItemSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onUserInteracted: (() -> Unit)? = null
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxDialogHeight = screenHeight * 0.6f

    val firstItemFocusRequester = FocusRequester()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Box(
                modifier = Modifier.heightIn(max = maxDialogHeight)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    itemsIndexed(items) { index, (id, name) ->

                        val itemFocusRequester =
                            if (index == 0) firstItemFocusRequester else FocusRequester()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUserInteracted?.invoke()
                                    onItemSelected(id)
                                    onDismiss()
                                }
                                .focusRequester(itemFocusRequester)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp &&
                                        keyEvent.key == Key.Enter
                                    ) {
                                        onUserInteracted?.invoke()
                                        onItemSelected(id)
                                        onDismiss()
                                        true
                                    } else {
                                        onUserInteracted?.invoke()
                                        false
                                    }
                                }
                        ) {
                            Text(text = name)
                        }

                        if (index == 0) {
                            LaunchedEffect(Unit) {
                                firstItemFocusRequester.requestFocus()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
