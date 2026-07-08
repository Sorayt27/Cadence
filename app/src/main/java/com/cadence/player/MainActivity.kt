package com.cadence.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cadence.player.ui.AppViewModel
import com.cadence.player.ui.components.MiniPlayer
import com.cadence.player.ui.screens.*
import com.cadence.player.ui.theme.CadenceTheme
import java.net.URLDecoder
import java.net.URLEncoder

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    BottomTab("home",     "Listen Now", Icons.Default.Home),
    BottomTab("library",  "Library",    Icons.Default.LibraryMusic),
    BottomTab("search",   "Search",     Icons.Default.Search),
    BottomTab("settings", "Settings",   Icons.Default.Settings),
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CadenceTheme {
                val vm: AppViewModel = viewModel()
                // Connect player inside composition so it's lifecycle-aware
                DisposableEffect(Unit) {
                    vm.player.connect()
                    onDispose { vm.player.disconnect() }
                }
                CadenceApp(vm)
            }
        }
    }
}

@Composable
private fun CadenceApp(vm: AppViewModel) {
    val context = LocalContext.current

    // ── Permission state ─────────────────────────────────────────────────
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission())
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        // Refresh is triggered by the LaunchedEffect reacting to the state change below
    }

    // Trigger initial permission request on first composition (if not yet granted)
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(audioPermission())
        }
    }

    // Load library whenever permission transitions to granted
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) vm.refresh()
    }

    if (!permissionGranted) {
        PermissionScreen { permissionLauncher.launch(audioPermission()) }
        return
    }

    // ── App scaffold ──────────────────────────────────────────────────────
    val app = context.applicationContext as MusicApplication
    val artworkCache  = remember { app.container.artworkCache }
    val settings      = remember { app.container.settingsStorage }
    val navController = rememberNavController()
    val playbackState by vm.player.state.collectAsState()
    var showNowPlaying by remember { mutableStateOf(false) }

    // Full-screen Now Playing overlay (sits on top of everything)
    if (showNowPlaying && playbackState.currentSong != null) {
        NowPlayingScreen(
            player       = vm.player,
            repository   = vm.repository,
            artworkCache = artworkCache,
            onDismiss    = { showNowPlaying = false },
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column {
                MiniPlayer(
                    state        = playbackState,
                    artworkCache = artworkCache,
                    onTap        = { if (playbackState.currentSong != null) showNowPlaying = true },
                    onPlayPause  = { vm.player.playPause() },
                    onSkipNext   = { vm.player.skipNext() },
                )
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    val currentEntry by navController.currentBackStackEntryAsState()
                    val currentDest = currentEntry?.destination
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDest?.hierarchy?.any { it.route == tab.route } == true,
                            onClick  = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "home",
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    repository       = vm.repository,
                    player           = vm.player,
                    artworkCache     = artworkCache,
                    onOpenNowPlaying = { showNowPlaying = true },
                )
            }
            composable("library") {
                LibraryScreen(
                    repository       = vm.repository,
                    player           = vm.player,
                    artworkCache     = artworkCache,
                    onAlbumClick     = { navController.navigate("album/$it") },
                    onArtistClick    = { navController.navigate("artist/${URLEncoder.encode(it, "UTF-8")}") },
                    onPlaylistClick  = { navController.navigate("playlist/$it") },
                    onOpenNowPlaying = { showNowPlaying = true },
                )
            }
            composable("search") {
                SearchScreen(
                    repository       = vm.repository,
                    player           = vm.player,
                    artworkCache     = artworkCache,
                    onAlbumClick     = { navController.navigate("album/$it") },
                    onArtistClick    = { navController.navigate("artist/${URLEncoder.encode(it, "UTF-8")}") },
                    onOpenNowPlaying = { showNowPlaying = true },
                )
            }
            composable("settings") {
                SettingsScreen(
                    settingsStorage      = settings,
                    onClearRecentlyPlayed = { vm.repository.clearRecentlyPlayed() },
                    onClearFavorites      = { vm.repository.clearFavorites() },
                )
            }

            // ── Detail screens ───────────────────────────────────────────
            composable(
                route     = "album/{albumId}",
                arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
            ) { back ->
                AlbumDetailScreen(
                    albumId          = back.arguments!!.getLong("albumId"),
                    repository       = vm.repository,
                    player           = vm.player,
                    artworkCache     = artworkCache,
                    onBack           = { navController.popBackStack() },
                    onOpenNowPlaying = { showNowPlaying = true },
                )
            }
            composable(
                route     = "artist/{artistName}",
                arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
            ) { back ->
                ArtistDetailScreen(
                    artistName       = URLDecoder.decode(
                        back.arguments!!.getString("artistName") ?: "", "UTF-8"
                    ),
                    repository       = vm.repository,
                    player           = vm.player,
                    artworkCache     = artworkCache,
                    onAlbumClick     = { navController.navigate("album/$it") },
                    onBack           = { navController.popBackStack() },
                    onOpenNowPlaying = { showNowPlaying = true },
                )
            }
            composable(
                route     = "playlist/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
            ) { back ->
                PlaylistDetailScreen(
                    playlistId       = back.arguments!!.getString("playlistId") ?: "",
                    repository       = vm.repository,
                    player           = vm.player,
                    artworkCache     = artworkCache,
                    onBack           = { navController.popBackStack() },
                    onOpenNowPlaying = { showNowPlaying = true },
                )
            }
        }
    }
}

// ── Permission screen ──────────────────────────────────────────────────────

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(40.dp),
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Allow Cadence to access your music library to get started.",
                style     = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Allow Access")
            }
        }
    }
}

private fun audioPermission() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE
