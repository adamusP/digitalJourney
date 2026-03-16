package com.example.digitaljourney.ui

import com.example.digitaljourney.ui.theme.DigitalJourneyTheme
import com.example.digitaljourney.model.LogCollectorWorker
import com.example.digitaljourney.data.*

import android.os.Bundle
import android.util.Log
import android.content.Intent

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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.credentials.CredentialManager

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

import java.util.concurrent.TimeUnit

import kotlinx.coroutines.launch

import com.example.digitaljourney.data.SettingsRepository
import com.example.digitaljourney.notifications.NotificationScheduler



class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

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

        authService.performTokenRequest(
            response.createTokenExchangeRequest()
        ) { tokenResponse, tokenEx ->
            val accessToken = tokenResponse?.accessToken
            if (accessToken != null) {
                TokenManager.saveSpotifyTokens(
                    this,
                    accessToken,
                    tokenResponse.refreshToken
                )
            } else {
                Log.e("SpotifyAuth", "Token exchange failed: $tokenEx")
            }
        }
    }

    private lateinit var credentialManager: CredentialManager

    private val googleAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        lifecycleScope.launch {
            when (val authResult = GoogleCalendarAuth.requestCalendarAccess(this@MainActivity)) {
                is GoogleCalendarAuth.AuthResult.Token -> {
                    TokenManager.saveGoogleTokens(this@MainActivity, authResult.accessToken, null)

                    try {
                        CalendarRepository.syncCalendarLogs(this@MainActivity)
                    } catch (e: Exception) {
                        Log.e("GoogleAuth", "Calendar sync failed", e)
                    }
                }

                is GoogleCalendarAuth.AuthResult.Error -> {
                    Log.e("GoogleAuth", authResult.message)
                }

                is GoogleCalendarAuth.AuthResult.NeedsResolution -> {
                    Log.e("GoogleAuth", "Authorization still requires resolution")
                }
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.w("Notifications", "Notification permission denied")
        }
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

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        authService = AuthorizationService(this)
        credentialManager = CredentialManager.create(this)


        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "log_collector",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LogCollectorWorker>(15, TimeUnit.MINUTES).build()
        )

        lifecycleScope.launch {
            val sync = LogSyncManager(
                this@MainActivity,
                SpotifyRepositoryImpl(),
                PhotosRepositoryImpl(),
                LocationRepositoryImpl(),
                WeatherRepository(),
                CalendarRepository
            )
            sync.syncNow()
        }

        setContent {
            val context = LocalContext.current
            val settingsRepository = remember { SettingsRepository(context) }
            var darkModeEnabled by remember {
                mutableStateOf(settingsRepository.isDarkModeEnabled())
            }

            // making system icons normal in dark mode
            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkModeEnabled
                insetsController.isAppearanceLightNavigationBars = !darkModeEnabled
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



                var notificationsEnabled by remember {
                    mutableStateOf(settingsRepository.isNotificationsEnabled())
                }

                val openLogFromNotification by notificationOpenLogEvent



                LaunchedEffect(openLogFromNotification) {
                    if (openLogFromNotification) {
                        navigateToTopLevel(Screen.Log.route)
                        notificationOpenLogEvent.value = false
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
                            LogListScreen(viewModel = viewModel)
                        }
                        composable(Screen.Month.route) {
                            MonthScreen(
                                viewModel = viewModel,
                                onDaySelected = { date ->
                                    viewModel.setDate(date)
                                    navController.navigate(Screen.Day.route)
                                }
                            )
                        }

                        composable(Screen.Log.route) {
                            LogScreen()
                        }

                        composable(Screen.Search.route) {
                            SearchScreen(
                                viewModel = viewModel,
                                onResultClick = {
                                    navController.navigate(Screen.Day.route)
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onAuthenticateSpotify = { startSpotifyLogin() },
                                onAuthenticateGoogle = { startGoogleLogin() },
                                requestChessName = { username ->
                                    TokenManager.saveChessUsername(applicationContext, username)
                                },
                                darkModeEnabled = darkModeEnabled,
                                onDarkModeChanged = { enabled ->
                                    darkModeEnabled = enabled
                                    settingsRepository.setDarkModeEnabled(enabled)
                                },
                                notificationsEnabled = notificationsEnabled,
                                onNotificationsChanged = { enabled ->
                                    notificationsEnabled = enabled
                                    settingsRepository.setNotificationsEnabled(enabled)

                                    if (enabled) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        NotificationScheduler.scheduleDailyReminder(context)
                                    } else {
                                        NotificationScheduler.cancelDailyReminder(context)
                                    }
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

    private fun startGoogleLogin() {

        lifecycleScope.launch {
            try {
                when (val result = GoogleCalendarAuth.signInAndAuthorize(this@MainActivity)) {
                    is GoogleCalendarAuth.AuthResult.Token -> {
                        TokenManager.saveGoogleTokens(this@MainActivity, result.accessToken, null)

                        try {
                            CalendarRepository.syncCalendarLogs(this@MainActivity)
                        } catch (e: Exception) {
                            Log.e("GoogleAuth", "Calendar sync failed", e)
                        }
                    }

                    is GoogleCalendarAuth.AuthResult.NeedsResolution -> {
                        val pendingIntent = result.pendingIntent
                        if (pendingIntent != null) {
                            googleAuthorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        } else {
                            Log.e("GoogleAuth", "Resolution required but pendingIntent was null")
                        }
                    }

                    is GoogleCalendarAuth.AuthResult.Error -> {
                        Log.e("GoogleAuth", result.message)
                    }
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                Log.w("GoogleAuth", "User canceled or account reauth failed", e)
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                Log.e("GoogleAuth", "Credential Manager failed", e)
            } catch (e: Exception) {
                Log.e("GoogleAuth", "Unexpected Google sign-in failure", e)
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
