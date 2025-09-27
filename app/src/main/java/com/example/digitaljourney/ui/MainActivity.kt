package com.example.digitaljourney.ui

import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import coil.compose.rememberAsyncImagePainter

import com.example.digitaljourney.ui.theme.DigitalJourneyTheme
import com.example.digitaljourney.model.LogCollectorWorker
import com.example.digitaljourney.model.LogEntity

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

import androidx.lifecycle.ViewModelProvider

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import java.time.Instant
import java.time.ZoneId

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

import com.example.digitaljourney.data.TokenManager

import com.example.digitaljourney.data.PhotosRepositoryImpl
import com.example.digitaljourney.data.SpotifyRepositoryImpl
import com.example.digitaljourney.data.LocationRepositoryImpl
import com.example.digitaljourney.data.LogSyncManager

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.digitaljourney.data.WeatherRepository

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults

import androidx.compose.ui.Alignment

import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var authService: AuthorizationService

    // capture the access token once, for the fetch button
    private var spotifyAccessToken: String? = null

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = AuthorizationResponse.fromIntent(result.data!!)
        val ex       = AuthorizationException.fromIntent(result.data!!)
        if (response != null) {
            authService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, tokenEx ->
                if (tokenResponse?.accessToken != null) {
                    spotifyAccessToken = tokenResponse.accessToken
                    TokenManager.saveTokens(
                        applicationContext,
                        spotifyAccessToken!!,
                        tokenResponse.refreshToken // may be null if Spotify didn’t return it yet
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
//                        composable(Screen.Stats.route) {
//                            Text("Stats view")
//                        }
                        composable(Screen.Other.route) {
                            OtherScreen(
                                onAuthenticate = { startSpotifyLogin() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startSpotifyLogin() {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.spotify.com/authorize"),
            Uri.parse("https://accounts.spotify.com/api/token")
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            "b240fef49f7946efa918336ada7aabab",
            ResponseTypeValues.CODE,
            Uri.parse("digitaljourney://callback")
        ).setScopes("user-read-recently-played")
            .build()

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        authLauncher.launch(authIntent)
    }
}

sealed class Screen(val route: String, val title: String) {
    object Day : Screen("day", "Day")
    object Month : Screen("month", "Month")
    //object Stats : Screen("stats", "Stats")
    object Other : Screen("other", "Other")
}

val bottomTabs = listOf(
    Screen.Day,
    Screen.Month,
    //Screen.Stats,
    Screen.Other
)

@Composable
fun LogListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logsForDay.collectAsState()
    val selectedDate by viewModel.selectedDate
    val photos by viewModel.photosForDay

    // Fetch photos for *today* when screen opens
    LaunchedEffect(selectedDate) {

        val sync = LogSyncManager(
            context,
            SpotifyRepositoryImpl(),
            PhotosRepositoryImpl(),
            LocationRepositoryImpl(),
            WeatherRepository()
        )
        sync.syncNow()

        viewModel.loadPhotosForDate(context, selectedDate)
    }

    // Merge DB logs + photo logs
    val allLogs = remember(logs, photos) {
        val photoEntities = photos.map { log ->
            LogEntity(
                id = -1, // dummy id
                type = "photo",
                data = log.uri.toString(),
                secondaryData = "",
                timestamp = log.timestamp
            )
        }
        (logs + photoEntities).sortedBy { it.timestamp }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(modifier = modifier.fillMaxWidth()
//        .pointerInput(Unit) {
//        detectHorizontalDragGestures { _, dragAmount ->
//            val threshold = 100f // swipe distance in pixels before it counts
//            if (dragAmount > threshold) {
//                viewModel.previousDay()
//            } else if (dragAmount < -threshold) {
//                viewModel.nextDay()
//            }
//        }
//    }
    ) {
        // --- Header with arrows ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.previousDay() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }
            Text(selectedDate.format(dateFormatter), fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.nextDay() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Previous month"
                )
            }
        }

        if (allLogs.isEmpty()) {
            // --- Empty state ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("No activity for this day.")
            }
        } else {

            LazyColumn(modifier = modifier.fillMaxWidth()) {
                items(allLogs) { log: LogEntity ->
                    val dt = Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault())
                    val date = dt.format(dateFormatter)
                    val time = dt.format(timeFormatter)

                    Column(modifier = Modifier.padding(8.dp)) {
                        // log content depending on type
                        when (log.type) {
                            "spotify" -> {
                                Text("$time 🎵")
                                Text(log.data, fontWeight = FontWeight.SemiBold)
                                Text(log.secondaryData)
                            }

                            "photo" -> {
                                Text("$time 📸")
                                Image(
                                    painter = rememberAsyncImagePainter(Uri.parse(log.data)),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .height(250.dp)
                                )
                            }

                            "location" -> {
                                Text("$time 📍")
                                Text(log.data, fontWeight = FontWeight.SemiBold)
                            }

                            "weather" -> {
                                Text("$time ${log.secondaryData}")
                                Text(log.data, fontWeight = FontWeight.SemiBold)
                            }

                            "mood" -> {
                                Text("$time 😊")
                                Text(log.data)
                            }

                            else -> Text(log.data)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthScreen(
    viewModel: MainViewModel,
    onDaySelected: (LocalDate) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Column(modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        // --- Month header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.previousMonth() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }
            Text(selectedMonth.format(dateFormatter), fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.nextMonth() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Weekday headers ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- Grid of days ---
        val daysInMonth = selectedMonth.lengthOfMonth()
        val firstDayOfWeek = selectedMonth.dayOfWeek.value - 1 // Monday = 0
        val totalCells = daysInMonth + firstDayOfWeek

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            for (weekStart in 0 until totalCells step 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayIndex in 0..6) {
                        val cellIndex = weekStart + dayIndex
                        if (cellIndex < firstDayOfWeek || cellIndex >= daysInMonth + firstDayOfWeek) {
                            Box(modifier = Modifier.size(40.dp)) { }
                        } else {
                            val day = cellIndex - firstDayOfWeek + 1
                            Button(
                                onClick = {
                                    val clickedDate = selectedMonth.withDayOfMonth(day)
                                    viewModel.setDate(clickedDate)
                                    onDaySelected(clickedDate)
                                },
                                modifier = Modifier
                                    .size(40.dp) // adjust height/width here
                                    .padding(horizontal = 2.dp), //  space between buttons
                                shape = RoundedCornerShape(16.dp), // less round, more square
                                contentPadding = PaddingValues(8.dp), // centers text nicely
                                colors = ButtonDefaults.buttonColors()

                            ) {
                                Text(day.toString())
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(128.dp))

        Button(
            onClick = {
                val todayDate = LocalDate.now()
                viewModel.setDate(todayDate)
                onDaySelected(todayDate)
            },
            modifier = Modifier
                .padding(16.dp)    // space around button
                .height(48.dp)     // make it taller
                .width(120.dp)     // make it wider
        ) {
            Text(
                "Today",
                //fontSize = 20.sp,       // bigger text
                fontWeight = FontWeight.Bold
            )
        }

    }
}

@Composable
fun OtherScreen(
    onAuthenticate: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Spacer(modifier = Modifier.height(128.dp))
        Button(onClick = onAuthenticate) {
            Text("Connect to Spotify")
        }
    }
}