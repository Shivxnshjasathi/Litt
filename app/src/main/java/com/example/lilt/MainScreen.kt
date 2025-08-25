package com.example.lilt

import android.R
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lilt.ui.theme.AppTypography
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

// Sealed class to define the navigation items for the bottom bar
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("energyPlayer", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Saved : BottomNavItem("saved", "Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    object Profile : BottomNavItem("profile", "Top Charts", Icons.Filled.InsertChartOutlined, Icons.Filled.InsertChart)
}

// This is the main screen that will host the bottom navigation and the content screens
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.height(104.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Saved,
                    BottomNavItem.Profile
                )
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.1f else 1.0f,
                        animationSpec = tween(300), label = "icon_scale"
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                modifier = Modifier.scale(scale)
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        BottomNavGraph(
            navController = navController,
            paddingValues = innerPadding,
            authViewModel = authViewModel
        )
    }
}

// Navigation graph for the screens accessible from the bottom bar
@Composable
fun BottomNavGraph(
    navController: NavController,
    paddingValues: PaddingValues,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController as androidx.navigation.NavHostController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(BottomNavItem.Home.route) {
            EnergyPlayerScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(BottomNavItem.Saved.route) { SavedSongsScreen() }
        composable(BottomNavItem.Profile.route) { TopCharts() }
    }
}


// --- Placeholder Screens with Animation ---

@Composable
fun AnimatedScreen(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f, animationSpec = tween(500)),
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

class SavedSongsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _savedSongs = MutableStateFlow<List<Song>>(emptyList())
    val savedSongs = _savedSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchSavedSongs()
    }

    // Made public to be called from the UI
    fun fetchSavedSongs() {
        _isLoading.value = true // Show loading indicator on refresh
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            return
        }

        db.collection("users").document(userId)
            .collection("saved_songs")
            .get()
            .addOnSuccessListener { documents ->
                val songs = documents.map { it.toObject(Song::class.java) }
                _savedSongs.value = songs
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }
}

@Composable
fun SavedSongsScreen(savedSongsViewModel: SavedSongsViewModel = viewModel()) {
    val savedSongs by savedSongsViewModel.savedSongs.collectAsState()
    val isLoading by savedSongsViewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val energyViewModel: EnergyViewModel = viewModel()

    AnimatedScreen {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Keep background gradient visible
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { savedSongsViewModel.fetchSavedSongs() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reload Songs",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (savedSongs.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "No Saved Songs",
                            modifier = Modifier.size(100.dp),

                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = "No Saved Songs",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (energyViewModel.selectedSong != null) 80.dp else 0.dp),
                        contentPadding = innerPadding
                    ) {
                        item {
                            Text(
                                text = "Saved Songs",
                                style = AppTypography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                        items(savedSongs) { song ->
                            SongRow(
                                song = song,
                                onSongClick = { energyViewModel.onSongClick(song) },
                                onMoreClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/results?search_query=${song.title}+${song.artist}")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = energyViewModel.selectedSong != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MiniPlayer(
                        song = energyViewModel.selectedSong!!,
                        isPlaying = energyViewModel.isPlaying,
                        progress = if (energyViewModel.totalDuration > 0) energyViewModel.currentPosition.toFloat() / energyViewModel.totalDuration else 0f,
                        onPlayPause = { energyViewModel.playPause() },
                        onTap = { energyViewModel.openFullScreenPlayer() }
                    )
                }

                AnimatedVisibility(
                    visible = energyViewModel.showPlayer,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FullScreenPlayer(
                        song = energyViewModel.selectedSong!!,
                        isPlaying = energyViewModel.isPlaying,
                        onPlayPause = { energyViewModel.playPause() },
                        onClose = { energyViewModel.closePlayer() },
                        currentPosition = energyViewModel.currentPosition,
                        totalDuration = energyViewModel.totalDuration,
                        onSeek = { energyViewModel.onSeek(it) },
                        onSkipPrevious = { energyViewModel.skipPrevious() },
                        onSkipNext = { energyViewModel.skipNext() },
                        onSaveSong = { energyViewModel.saveSongToFavorites(energyViewModel.selectedSong!!) },
                        getDominantColor = {
                            energyViewModel.getDominantColorForSong(
                                context,
                                energyViewModel.selectedSong!!.imageUrl
                            )
                        }
                    )
                }
            }
        }
    }
}


