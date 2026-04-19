package com.example.digitaljourney.ui

import android.Manifest
import com.example.digitaljourney.ui.theme.DigitalJourneyTheme

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
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

import com.example.digitaljourney.ui.screens.LogListScreen
import com.example.digitaljourney.ui.screens.LogScreen
import com.example.digitaljourney.ui.screens.MonthScreen
import com.example.digitaljourney.ui.screens.SearchScreen
import com.example.digitaljourney.ui.screens.SettingsScreen
import com.example.digitaljourney.ui.viewmodel.MainActivityViewModel
import com.example.digitaljourney.ui.viewmodel.DayViewModel
import com.example.digitaljourney.ui.viewmodel.LogViewModel
import com.example.digitaljourney.ui.viewmodel.MonthViewModel
import com.example.digitaljourney.ui.viewmodel.SearchViewModel
import com.example.digitaljourney.ui.viewmodel.SettingsViewModel
import java.time.Instant
import java.time.ZoneId


class MainActivity : ComponentActivity() {

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        val ex = data?.let { AuthorizationException.fromIntent(it) }

        val mainViewModel = androidx.lifecycle.ViewModelProvider(this)[MainActivityViewModel::class.java]
        mainViewModel.handleSpotifyAuthResult(response, ex)
    }

    private val googleAuthorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val mainViewModel = androidx.lifecycle.ViewModelProvider(this)[MainActivityViewModel::class.java]
        mainViewModel.continueGoogleLoginAfterResolution()
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

        setContent {
            val mainViewModel: MainActivityViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val googleResolution by mainViewModel.googleAuthNeedsResolution
            val googleAuthError by mainViewModel.googleAuthError
            val spotifyAuthError by mainViewModel.spotifyAuthError
            val spotifyLoginIntent by mainViewModel.spotifyLoginIntent
            val notificationsEnabled by mainViewModel.notificationsEnabled

            val darkModeEnabled by mainViewModel.darkModeEnabled

            LaunchedEffect(Unit) {
                mainViewModel.initializeApp()
            }

            LaunchedEffect(spotifyLoginIntent) {
                spotifyLoginIntent?.let {
                    authLauncher.launch(it)
                    mainViewModel.clearSpotifyLoginIntent()
                }
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
                    mainViewModel.setNotificationsEnabled(true)
                } else {
                    Log.w("Notifications", "Notification permission denied")
                    mainViewModel.setNotificationsEnabled(false)
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
                        mainViewModel.clearGoogleResolution()
                    }
                }

                LaunchedEffect(googleAuthError) {
                    googleAuthError?.let {
                        Log.e("GoogleAuth", it)
                        mainViewModel.clearGoogleAuthError()
                    }
                }

                LaunchedEffect(spotifyAuthError) {
                    spotifyAuthError?.let {
                        Log.e("SpotifyAuth", it)
                        mainViewModel.clearSpotifyAuthError()
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
                        composable(
                            route = Screen.Day.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("date") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                    nullable = true
                                },
                                androidx.navigation.navArgument("highlight") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val dayViewModel: DayViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                            val dateArg = backStackEntry.arguments?.getString("date")
                            val highlightArg = backStackEntry.arguments?.getString("highlight")?.toLongOrNull()

                            LaunchedEffect(dateArg, highlightArg) {
                                if (!dateArg.isNullOrBlank()) {
                                    dayViewModel.setDate(java.time.LocalDate.parse(dateArg))
                                }

                                if (highlightArg != null) {
                                    dayViewModel.highlightLog(highlightArg)
                                }
                            }

                            LogListScreen(viewModel = dayViewModel)
                        }
                        composable(Screen.Month.route) {
                            val monthViewModel: MonthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            MonthScreen(
                                viewModel = monthViewModel,
                                onDaySelected = { date ->
                                    navController.navigate(
                                        Screen.Day.createRoute(date = date.toString())
                                    )
                                }
                            )
                        }

                        composable(Screen.Log.route) {
                            val logViewModel: LogViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            LogScreen(viewModel = logViewModel)
                        }

                        composable(Screen.Search.route) {
                            val searchViewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            SearchScreen(
                                viewModel = searchViewModel,
                                onResultClick = { timestamp ->
                                    val date = Instant.ofEpochMilli(timestamp)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()

                                    navController.navigate(
                                        Screen.Day.createRoute(
                                            date = date.toString(),
                                            highlight = timestamp
                                        )
                                    )
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                darkModeEnabled = darkModeEnabled,
                                notificationsEnabled = notificationsEnabled,
                                googleAuthError = googleAuthError,
                                spotifyAuthError = spotifyAuthError,
                                onAuthenticateSpotify = { mainViewModel.startSpotifyLogin() },
                                onAuthenticateGoogle = { mainViewModel.startGoogleLogin(this@MainActivity) },
                                onToggleDarkMode = { enabled -> mainViewModel.setDarkModeEnabled(enabled) },
                                onToggleNotifications = { enabled -> mainViewModel.setNotificationsEnabled(enabled) },
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
}

sealed class Screen(val route: String, val title: String) {
    object Day : Screen("day?date={date}&highlight={highlight}", "Day") {
        fun createRoute(date: String? = null, highlight: Long? = null): String {
            val dateValue = date ?: ""
            val highlightValue = highlight?.toString() ?: ""
            return "day?date=$dateValue&highlight=$highlightValue"
        }
    }
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
