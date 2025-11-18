package com.example.digitaljourney.ui

import com.example.digitaljourney.ui.theme.DigitalJourneyTheme
import com.example.digitaljourney.model.LogCollectorWorker
import com.example.digitaljourney.data.*

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.lifecycleScope

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

import java.util.concurrent.TimeUnit

import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    // capture the access token once, for the fetch button
    private lateinit var authService: AuthorizationService

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = AuthorizationResponse.fromIntent(result.data!!)
        val ex = AuthorizationException.fromIntent(result.data!!)
        if (response != null) {
            authService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, tokenEx ->
                if (tokenResponse?.accessToken != null) {
                    TokenManager.saveTokens(
                        this,
                        tokenResponse.accessToken!!,
                        tokenResponse.refreshToken
                    )
                } else {
                    android.util.Log.e("SpotifyAuth", "Token exchange failed: $tokenEx")
                }
            }
        } else {
            android.util.Log.e("SpotifyAuth", "Auth failed: $ex")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        authService = AuthorizationService(this)

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
                WeatherRepository()
            )
            sync.syncNow()
        }

        setContent {
            DigitalJourneyTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route


                            bottomTabs.forEach { screen ->
                                NavigationBarItem(
                                    icon = { /* optional: Icon(...) */ },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Day.route,
                        modifier = Modifier.padding(padding)
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
//                        composable(Screen.Stats.route) {
//                            Text("Stats view")
//                        }
                        composable(Screen.Other.route) {
                            OtherScreen(
                                onAuthenticate = { startSpotifyLogin() },
                                requestChessName = { username ->
                                    TokenManager.saveChessUsername(applicationContext, username) }
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

}

sealed class Screen(val route: String, val title: String) {
    object Day : Screen("day", "Day")
    object Month : Screen("month", "Month")
    object Log : Screen("log", "Log")
    //object Stats : Screen("stats", "Stats")
    object Other : Screen("other", "Other")
}

val bottomTabs = listOf(
    Screen.Day,
    Screen.Month,
    Screen.Log,
    //Screen.Stats,
    Screen.Other
)
