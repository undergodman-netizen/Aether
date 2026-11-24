package com.example

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.screens.SubjectListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.theme.AetherTheme
import com.example.viewmodel.VideoViewModel
import com.example.viewmodel.VideoViewModelFactory
import java.nio.charset.StandardCharsets
import android.util.Base64

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.components.AetherBottomBar
import com.example.ui.components.AetherNavigationRail

import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPreferences = getSharedPreferences("StudyMemory", MODE_PRIVATE)
        com.example.ui.theme.ThemeConfig.isDarkTheme = sharedPreferences.getBoolean("dark_theme", true)

        setContent {
            AetherTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"
                
                val windowSizeClass = calculateWindowSizeClass(this)
                val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                val hideNavigationBar = currentRoute.startsWith("player") || currentRoute.startsWith("pdf") || currentRoute.startsWith("videoShots")

                if (isWideScreen && !hideNavigationBar) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AetherNavigationRail(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { 
                                        saveState = true 
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            NavHostWrapper(navController, isWideScreen)
                        }
                    }
                } else if (!hideNavigationBar) {
                    Scaffold(
                        bottomBar = {
                            AetherBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo("home") { 
                                            saveState = true 
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        },
                        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            NavHostWrapper(navController, isWideScreen)
                        }
                    }
                } else {
                    NavHostWrapper(navController, isWideScreen)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun NavHostWrapper(navController: androidx.navigation.NavHostController, isWideScreen: Boolean) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable(
                        route = "home",
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        HomeScreen(
                            viewModel = koinViewModel(),
                            onNavigate = { route -> 
                                if (route == "library" || route == "home") {
                                    navController.navigate(route) {
                                        popUpTo("home") { 
                                            saveState = true 
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigate(route)
                                }
                            },
                            onVideoClick = { video ->
                                val encodedUri = Base64.encodeToString(video.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                val startPos = if (video.lastPlayedPosition > 0) video.lastPlayedPosition else 0L
                                navController.navigate("player/$encodedUri?startPosition=$startPos")
                            }
                        )
                    }

                    composable(
                        route = "library",
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        LibraryScreen(
                            viewModel = koinViewModel(),
                            onNavigate = { route ->
                                if (route == "home") {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(route)
                                }
                            }
                        )
                    }

                    composable(
                        route = "library/history",
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                    ) {
                        HistoryScreen(
                            viewModel = koinViewModel(),
                            onBack = { navController.popBackStack() },
                            onVideoClick = { video ->
                                val encodedUri = Base64.encodeToString(video.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                val startPos = if (video.lastPlayedPosition > 0) video.lastPlayedPosition else 0L
                                navController.navigate("player/$encodedUri?startPosition=$startPos")
                            }
                        )
                    }

                    composable(
                        route = "library/bookmarks",
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                    ) {
                        BookmarksScreen(
                            viewModel = koinViewModel(),
                            onBack = { navController.popBackStack() },
                            onGroupClick = { videoUri ->
                                val encodedUri = Base64.encodeToString(videoUri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                navController.navigate("videoShots/$encodedUri")
                            },
                            onShotClick = { videoUri, timestamp ->
                                val encodedUri = Base64.encodeToString(videoUri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                navController.navigate("player/$encodedUri?startPosition=$timestamp")
                            }
                        )
                    }
                    
                    composable(
                        route = "library/subjects",
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                    ) {
                        if (isWideScreen) {
                            var selectedSubject by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).border(1.dp, androidx.compose.ui.graphics.Color.DarkGray)) {
                                    SubjectListScreen(
                                        viewModel = koinViewModel(),
                                        onBack = { navController.popBackStack() },
                                        onNavigateToSubject = { subjectName ->
                                            selectedSubject = subjectName
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(2f)) {
                                    if (selectedSubject != null) {
                                        SubjectDetailScreen(
                                            subject = selectedSubject!!,
                                            viewModel = koinViewModel(),
                                            onBack = { selectedSubject = null },
                                            onNavigate = { route -> navController.navigate(route) },
                                            onVideoClick = { video ->
                                                val encodedUri = Base64.encodeToString(video.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                                navController.navigate("player/$encodedUri")
                                            },
                                            onPdfClick = { pdf ->
                                                val encodedUrl = Base64.encodeToString(pdf.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                                navController.navigate("pdf/$encodedUrl")
                                            }
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                            androidx.compose.material3.Text("Select a subject", color = androidx.compose.ui.graphics.Color.Gray)
                                        }
                                    }
                                }
                            }
                        } else {
                            SubjectListScreen(
                                viewModel = koinViewModel(),
                                onBack = { navController.popBackStack() },
                                onNavigateToSubject = { subjectName ->
                                    navController.navigate("library/subjects/${Uri.encode(subjectName)}")
                                }
                            )
                        }
                    }

                    composable(
                        route = "library/subjects/{subjectName}",
                        arguments = listOf(navArgument("subjectName") { type = NavType.StringType }),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                    ) { backStackEntry ->
                        val subjectName = Uri.decode(backStackEntry.arguments?.getString("subjectName") ?: "Other")
                        SubjectDetailScreen(
                            subject = subjectName,
                            viewModel = koinViewModel(),
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) },
                            onVideoClick = { video ->
                                val encodedUri = Base64.encodeToString(video.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                val startPos = if (video.lastPlayedPosition > 0) video.lastPlayedPosition else 0L
                                navController.navigate("player/$encodedUri?startPosition=$startPos")
                            },
                            onPdfClick = { pdf ->
                                val encodedUrl = Base64.encodeToString(pdf.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                navController.navigate("pdf/$encodedUrl")
                            }
                        )
                    }

                    composable(
                        route = "pdf/{encodedUrl}",
                        arguments = listOf(navArgument("encodedUrl") { type = NavType.StringType }),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(300)) }
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                        com.example.ui.screens.PdfViewerScreen(
                            encodedUrl = encodedUrl,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "player/{encodedUri}?startPosition={startPosition}",
                        arguments = listOf(
                            navArgument("encodedUri") { type = NavType.StringType },
                            navArgument("startPosition") { type = NavType.LongType; defaultValue = -1L }
                        ),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(300)) }
                    ) { backStackEntry ->
                        val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: ""
                        val startPosition = backStackEntry.arguments?.getLong("startPosition") ?: -1L
                        val uri = String(Base64.decode(encodedUri, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
                        PlayerScreen(
                            videoUri = uri,
                            viewModel = koinViewModel(),
                            startPositionMs = startPosition,
                            onBack = { navController.popBackStack() },
                            onOpenVideoShots = {
                                navController.navigate("videoShots/$encodedUri")
                            }
                        )
                    }

                    composable(
                        route = "videoShots/{encodedUri}",
                        arguments = listOf(navArgument("encodedUri") { type = NavType.StringType }),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(300)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(300)) }
                    ) { backStackEntry ->
                        val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: ""
                        val uri = String(Base64.decode(encodedUri, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
                        com.example.ui.screens.VideoShotsScreen(
                            videoUri = uri,
                            viewModel = koinViewModel(),
                            onBack = { navController.popBackStack() },
                            onShotClick = { timestamp ->
                                navController.navigate("player/$encodedUri?startPosition=$timestamp") {
                                    popUpTo("player/$encodedUri") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        route = "settings",
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        SettingsScreen()
                    }
                }
}
