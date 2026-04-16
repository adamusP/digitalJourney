package com.example.digitaljourney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitaljourney.ui.viewmodel.SettingsViewModel
import com.example.digitaljourney.ui.emojiFor

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAuthenticateSpotify: () -> Unit,
    onAuthenticateGoogle: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    val darkModeEnabled by viewModel.darkModeEnabled
    val notificationsEnabled by viewModel.notificationsEnabled

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(30.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Settings",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(onClick = onAuthenticateSpotify, modifier = Modifier.width(250.dp)) {
                Text("Connect to Spotify ${emojiFor("spotify")}")
            }

            Button(onClick = { showDialog = true }, modifier = Modifier.width(250.dp)) {
                Text("Connect to Chess.com ${emojiFor("chess")}")
            }

            Button(onClick = onAuthenticateGoogle, modifier = Modifier.width(250.dp)) {
                Text("Connect to Google Calendar ${emojiFor("calendar")}")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notifications")
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                onRequestNotificationPermission()
                            } else {
                                viewModel.setNotificationsEnabled(true)
                            }
                        } else {
                            viewModel.setNotificationsEnabled(false)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark mode")
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = viewModel::setDarkModeEnabled
                )
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Enter Chess.com username") },
                    text = {
                        TextField(
                            value = username,
                            onValueChange = { username = it }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.saveChessUsername(username)
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
}