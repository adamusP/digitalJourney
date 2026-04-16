package com.example.digitaljourney.ui

import android.Manifest
import com.example.digitaljourney.ui.theme.DigitalJourneyTheme

import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.os.Build

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.lifecycleScope
import com.example.digitaljourney.data.managers.GoogleCalendarAuth
import com.example.digitaljourney.data.managers.SpotifyAuthManager
import com.example.digitaljourney.data.repositories.AuthRepository

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

import kotlinx.coroutines.launch

import com.example.digitaljourney.ui.screens.LogListScreen
import com.example.digitaljourney.ui.screens.LogScreen
import com.example.digitaljourney.ui.screens.MonthScreen
import com.example.digitaljourney.ui.screens.SearchScreen
import com.example.digitaljourney.ui.screens.SettingsScreen
import com.example.digitaljourney.ui.viewmodel.AppStartupViewModel
import com.example.digitaljourney.ui.viewmodel.DayViewModel
import com.example.digitaljourney.ui.viewmodel.LogViewModel
import com.example.digitaljourney.ui.viewmodel.MonthViewModel
import com.example.digitaljourney.ui.viewmodel.SearchViewModel
import com.example.digitaljourney.ui.viewmodel.SettingsViewModel
import java.time.Instant
import java.time.ZoneId


class MainActivity : ComponentActivity() {

    // capture the access token once, for the fetch button
    private lateinit var authService: AuthorizationService

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: run {
            Log.e("SpotifyAuth", "Auth failed: result data was null")
            return@registerForActivityResult
        }

        val response = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)

        if (response == null) {
            Log.e("SpotifyAuth", "Auth failed: $ex")
            return@registerForActivityResult
        }

        val settingsViewModel = androidx.lifecycle.ViewModelProvider(this)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            val authRepository = AuthRepository(this@MainActivity)
            authRepository.exchangeSpotifyToken(authService, response)
                .onFailure {
                    Log.e("SpotifyAuth", "Token exchange failed", it)
                    settingsViewModel.onSpotifyTokenExchangeFailed(it.message ?: "Spotify auth failed")
                }
        }
    }

    private val googleAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val settingsViewModel = androidx.lifecycle.ViewModelProvider(this)[SettingsViewModel::class.java]
        settingsViewModel.continueGoogleLoginAfterResolution()
    }

    private val notificationOpenLogEvent = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("open_log_screen", false)) {
            notificationOpenLogEvent.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authService = AuthorizationService(this)

        setContent {
            val context = LocalContext.current

            val dayViewModel: DayViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val monthViewModel: MonthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val searchViewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val appStartupViewModel: AppStartupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val googleResolution by settingsViewModel.googleAuthNeedsResolution
            val googleAuthError by settingsViewModel.googleAuthError
            val spotifyAuthError by settingsViewModel.spotifyAuthError

            val darkModeEnabled by settingsViewModel.darkModeEnabled

            LaunchedEffect(Unit) {
                appStartupViewModel.initializeApp()
            }

            // making system icons normal in dark mode
            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkModeEnabled
                insetsController.isAppearanceLightNavigationBars = !darkModeEnabled
            }

            val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    settingsViewModel.setNotificationsEnabled(true)
                } else {
                    Log.w("Notifications", "Notification permission denied")
                    settingsViewModel.setNotificationsEnabled(false)
                }
            }

            DigitalJourneyTheme(darkTheme = darkModeEnabled) {
                val navController = rememberNavController()

                fun navigateToTopLevel(route: String) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                val launchedFromNotification = remember {
                    intent?.getBooleanExtra("open_log_screen", false) == true
                }

                val startDestination = if (launchedFromNotification) {
                    Screen.Log.route
                } else {
                    Screen.Day.route
                }


                val openLogFromNotification by notificationOpenLogEvent



                LaunchedEffect(openLogFromNotification) {
                    if (openLogFromNotification) {
                        navigateToTopLevel(Screen.Log.route)
                        notificationOpenLogEvent.value = false
                    }
                }

                LaunchedEffect(googleResolution) {
                    val pendingIntent = googleResolution
                    if (pendingIntent != null) {
                        googleAuthorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                        settingsViewModel.clearGoogleResolution()
                    }
                }

                LaunchedEffect(googleAuthError) {
                    googleAuthError?.let {
                        Log.e("GoogleAuth", it)
                        settingsViewModel.clearGoogleAuthError()
                    }
                }

                LaunchedEffect(spotifyAuthError) {
                    spotifyAuthError?.let {
                        Log.e("SpotifyAuth", it)
                        settingsViewModel.clearSpotifyAuthError()
                    }
                }

                Scaffold(

                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        NavigationBar(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 8.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            bottomTabs.forEach { screen ->
                                NavigationBarItem(
                                    icon = {
                                        when (screen.route) {
                                            Screen.Day.route -> Icon(Icons.Filled.WbSunny, contentDescription = "Day")
                                            Screen.Month.route -> Icon(Icons.Filled.CalendarMonth, contentDescription = "Month")
                                            Screen.Log.route -> Icon(Icons.Filled.EditNote, contentDescription = "Log")
                                            Screen.Search.route -> Icon(Icons.Filled.Search, contentDescription = "Search")
                                            Screen.Settings.route -> Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                        }
                                    },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navigateToTopLevel(screen.route)
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(padding),
                        enterTransition = {
                            val initialIndex = tabIndex(initialState.destination.route)
                            val targetIndex = tabIndex(targetState.destination.route)

                            if (targetIndex > initialIndex) {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(220)
                                )
                            } else {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(220)
                                )
                            }
                        },
                        exitTransition = {
                            val initialIndex = tabIndex(initialState.destination.route)
                            val targetIndex = tabIndex(targetState.destination.route)

                            if (targetIndex > initialIndex) {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(220)
                                )
                            } else {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(220)
                                )
                            }
                        },
                        popEnterTransition = {
                            val initialIndex = tabIndex(initialState.destination.route)
                            val targetIndex = tabIndex(targetState.destination.route)

                            if (targetIndex > initialIndex) {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(220)
                                )
                            } else {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(220)
                                )
                            }
                        },
                        popExitTransition = {
                            val initialIndex = tabIndex(initialState.destination.route)
                            val targetIndex = tabIndex(targetState.destination.route)

                            if (targetIndex > initialIndex) {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(220)
                                )
                            } else {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(220)
                                )
                            }
                        }

                    ) {
                        composable(Screen.Day.route) {
                            LogListScreen(viewModel = dayViewModel)
                        }
                        composable(Screen.Month.route) {
                            MonthScreen(
                                viewModel = monthViewModel,
                                onDaySelected = { date ->
                                    dayViewModel.setDate(date)
                                    navController.navigate(Screen.Day.route)
                                }
                            )
                        }

                        composable(Screen.Log.route) {
                            val logViewModel: LogViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            LogScreen(viewModel = logViewModel)
                        }

                        composable(Screen.Search.route) {
                            SearchScreen(
                                viewModel = searchViewModel,
                                onResultClick = { timestamp ->
                                    dayViewModel.highlightLog(timestamp)
                                    dayViewModel.setDate(
                                        Instant.ofEpochMilli(timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                    )
                                    navController.navigate(Screen.Day.route)
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onAuthenticateSpotify = { startSpotifyLogin() },
                                onAuthenticateGoogle = { startGoogleLogin(settingsViewModel) },
                                onRequestNotificationPermission = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()  // Release the ServiceConnection
    }

    private fun startSpotifyLogin() {
        val authRequest = SpotifyAuthManager.buildAuthRequest()
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        authLauncher.launch(authIntent)
    }

    private fun startGoogleLogin(settingsViewModel: SettingsViewModel) {
        lifecycleScope.launch {
            try {
                val result = GoogleCalendarAuth.signInAndAuthorize(this@MainActivity)
                settingsViewModel.onGoogleLoginResult(result)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                Log.w("GoogleAuth", "User canceled or account reauth failed", e)
                settingsViewModel.onGoogleLoginResult(
                    GoogleCalendarAuth.AuthResult.Error("Google sign-in was canceled or account reauth failed")
                )
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                Log.e("GoogleAuth", "Credential Manager failed", e)
                settingsViewModel.onGoogleLoginResult(
                    GoogleCalendarAuth.AuthResult.Error("Credential Manager failed: ${e.message}")
                )
            } catch (e: Exception) {
                Log.e("GoogleAuth", "Unexpected Google sign-in failure", e)
                settingsViewModel.onGoogleLoginResult(
                    GoogleCalendarAuth.AuthResult.Error("Unexpected Google sign-in failure")
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Day : Screen("day", "Day")
    object Month : Screen("month", "Month")
    object Log : Screen("log", "Log")
    object Search : Screen("search", "Search")
    object Settings : Screen("settings", "Settings")
}

fun tabIndex(route: String?): Int {
    return when (route) {
        Screen.Day.route -> 0
        Screen.Month.route -> 1
        Screen.Log.route -> 2
        Screen.Search.route -> 3
        Screen.Settings.route -> 4
        else -> 0
    }
}

val bottomTabs = listOf(
    Screen.Day,
    Screen.Month,
    Screen.Log,
    Screen.Search,
    Screen.Settings
)
