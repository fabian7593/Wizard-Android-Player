package com.example.vlc.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScrollableDialogList(
    title: String,
    items: List<Pair<Int, String>>,
    onItemSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    showDisableOption: Boolean = false,
    onDisable: (() -> Unit)? = null,
    disableLabel: String = "Desactivar"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                items.forEach { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemSelected(id)
                                onDismiss()
                            }
                            .focusable()
                            .padding(12.dp)
                    ) {
                        Text(text = name)
                    }
                }

                if (showDisableOption && onDisable != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDisable()
                                onDismiss()
                            }
                            .focusable()
                            .padding(12.dp)
                    ) {
                        Text(text = disableLabel)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
