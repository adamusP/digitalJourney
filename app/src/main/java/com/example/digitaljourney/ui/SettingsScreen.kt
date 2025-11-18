package com.example.digitaljourney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OtherScreen(
    onAuthenticate: () -> Unit,
    requestChessName: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(128.dp))
        Button(onClick = onAuthenticate) {
            Text("Connect to Spotify")
        }
        Button(onClick = { showDialog = true }) {
            Text("Connect to Chess.com")
        }

        if (showDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enter Chess.com username") },
                text = {
                    androidx.compose.material3.TextField(
                        value = username,
                        onValueChange = { username = it }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        requestChessName(username)
                        showDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}