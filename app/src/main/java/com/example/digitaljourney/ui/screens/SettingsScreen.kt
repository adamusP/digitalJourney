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
import androidx.compose.runtime.LaunchedEffect
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
    darkModeEnabled: Boolean,
    notificationsEnabled: Boolean,
    googleAuthError: String?,
    spotifyAuthError: String?,
    onAuthenticateSpotify: () -> Unit,
    onAuthenticateGoogle: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var showChessDialog by remember { mutableStateOf(false) }
    var showLetrDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    val totalLogs by viewModel.totalLogs

    LaunchedEffect(Unit) {
        viewModel.loadTotalLogs()
    }

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

            Button(
                onClick = {
                    username = viewModel.getLetrUsername()
                    showLetrDialog = true
                },
                modifier = Modifier.width(250.dp)
            ) {
                Text("Connect to Letterboxd ${emojiFor("movie")}")
            }

            Button(
                onClick = {
                    username = viewModel.getChessUsername()
                    showChessDialog = true
                },
                modifier = Modifier.width(250.dp)
            ) {
                Text("Connect to Chess.com ${emojiFor("chess")}")
            }

            Button(onClick = onAuthenticateGoogle, modifier = Modifier.width(250.dp)) {
                Text("Connect to Google Calendar ${emojiFor("calendar")}")
            }

            if (spotifyAuthError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = spotifyAuthError,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }

            if (googleAuthError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = googleAuthError,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
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
                                onToggleNotifications(true)
                            }
                        } else {
                            onToggleNotifications(false)
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
                    onCheckedChange = onToggleDarkMode
                )
            }

            Spacer(modifier = Modifier.height(100.dp))

            Text(
                text = "Total logs: $totalLogs",
                fontWeight = FontWeight.SemiBold
            )

            if (showLetrDialog) {
                AlertDialog(
                    onDismissRequest = { showLetrDialog = false },
                    title = { Text("Enter your Letterboxd username") },
                    text = {
                        TextField(
                            value = username,
                            onValueChange = { username = it }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.saveLetrUsername(username)
                            showLetrDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showLetrDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showChessDialog) {
                AlertDialog(
                    onDismissRequest = { showChessDialog = false },
                    title = { Text("Enter your Chess.com username") },
                    text = {
                        TextField(
                            value = username,
                            onValueChange = { username = it }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.saveChessUsername(username)
                            showChessDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showChessDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}