@file:Suppress("unused")

package com.example.lilt

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import com.example.lilt.ui.theme.AppTypography
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

// --- THEME COLORS & MODELS ---

val TextSecondary = Color(0xFFB3B3B3)
val CardBackground = Color(0xFF1E1E1E)

@Serializable
data class EnergySongDto(
    val title: String? = null,
    val artist: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("audioUrl") val audioUrl: String? = null,
    @SerialName("playFrom") val playFrom: String? = null
)

typealias EnergySongsResponse = List<EnergySongDto>

data class Song(
    val title: String,
    val artist: String,
    val imageUrl: String,
    val audioUrl: String,
    val playTime: String
)

// --- API & REPOSITORY ---

interface EnergyApi {
    @GET(".")
    suspend fun getPlayouts(): List<EnergySongDto>
}

object ApiClient {
    private const val BASE_URL = "https://energy.ch/api/channels/bern/playouts/"

    val api: EnergyApi by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(client)
            .build()
            .create(EnergyApi::class.java)
    }
}

class EnergyRepository(
    private val api: EnergyApi = ApiClient.api
) {
    private var cachedSongs: List<Song> = emptyList()
    private var lastCacheTime: Long = 0
    private val cacheDurationMs = 5 * 60 * 1000 // 5 minutes

    private val inputFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    }
    private val outFormat by lazy {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    // Client for the summary API
    private val summaryClient = OkHttpClient()

    suspend fun fetchSongs(forceReload: Boolean = false): List<Song> {
        if (!forceReload && cachedSongs.isNotEmpty() && (System.currentTimeMillis() - lastCacheTime < cacheDurationMs)) {
            return cachedSongs
        }

        val dtoList = api.getPlayouts()
        val songs = dtoList.mapNotNull { dto ->
            val title = dto.title?.ifBlank { null } ?: return@mapNotNull null
            val artist = dto.artist?.ifBlank { null } ?: "Unknown"
            val image = dto.imageUrl ?: "https://placehold.co/160x160?text=M"
            val audio = dto.audioUrl ?: return@mapNotNull null
            val playTime = try {
                dto.playFrom?.let { pf ->
                    inputFormat.parse(pf)?.let(outFormat::format)
                } ?: ""
            } catch (_: Throwable) {
                ""
            }
            Song(title, artist, image, audio, playTime)
        }
        cachedSongs = songs
        lastCacheTime = System.currentTimeMillis()
        return songs
    }

    suspend fun fetchSongSummary(song: Song): String {
        // URL-encode song and artist names to handle special characters
        val songNameEncoded = URLEncoder.encode(song.title, "UTF-8")
        val artistNameEncoded = URLEncoder.encode(song.artist, "UTF-8")

        val url = "https://text.pollinations.ai/give%20summary%20of%20${songNameEncoded}%20by%20${artistNameEncoded}"
        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            try {
                val response = summaryClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string() ?: "Could not retrieve summary."
                } else {
                    "Summary not available (Error: ${response.code})."
                }
            } catch (e: Exception) {
                "Failed to load summary: ${e.message}"
            }
        }
    }
}

// --- PLAYER CONTROLLER ---

class PlayerController(private val app: Application) {
    @UnstableApi
    val exo: ExoPlayer = ExoPlayer.Builder(app).build()

    fun play(url: String) {
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exo.setMediaItem(mediaItem)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun pause() {
        exo.playWhenReady = false
    }

    fun isPlaying(): Boolean = exo.isPlaying

    fun release() {
        exo.release()
    }
}

// --- VIEW MODEL ---

class EnergyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = EnergyRepository()
    internal val player = PlayerController(app)

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var randomSongs by mutableStateOf<List<Song>>(emptyList()) // State for featured songs
        private set
    var selectedSong by mutableStateOf<Song?>(null)
        private set
    var showPlayer by mutableStateOf(false)
        private set

    // State for song summary
    var songSummary by mutableStateOf<String?>(null)
        private set
    var isSummaryLoading by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableStateOf(0L)
        private set
    var totalDuration by mutableStateOf(0L)
        private set
    private var progressUpdaterJob: Job? = null
    private var autoRefreshJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@EnergyViewModel.isPlaying = isPlaying
            if (isPlaying) {
                startProgressUpdater()
            } else {
                stopProgressUpdater()
            }
        }
    }

    init {
        player.exo.addListener(playerListener)
        fetchSongs()
        startAutoRefresh()
    }

    fun fetchSongs(forceReload: Boolean = false) {
        viewModelScope.launch {
            try {
                loading = true
                error = null
                val fetchedSongs = repo.fetchSongs(forceReload)
                songs = fetchedSongs
                // Shuffle the list and take 3 for the featured section
                randomSongs = fetchedSongs.shuffled().take(5)
            } catch (t: Throwable) {
                error = t.message ?: "Unknown error"
            } finally {
                loading = false
            }
        }
    }

    fun onSongClick(song: Song) {
        if (selectedSong?.audioUrl == song.audioUrl) {
            playPause()
        } else {
            selectedSong = song
            player.play(song.audioUrl)
            fetchSongSummary(song)
        }
    }

    private fun fetchSongSummary(song: Song) {
        viewModelScope.launch {
            isSummaryLoading = true
            songSummary = null // Clear previous summary
            try {
                val summary = repo.fetchSongSummary(song)
                songSummary = summary.trim().replace("\"", "") // Clean up the response
            } catch (t: Throwable) {
                songSummary = "Error loading summary." // Set an error message
            } finally {
                isSummaryLoading = false
            }
        }
    }

    fun openFullScreenPlayer() {
        showPlayer = true
    }

    fun playPause() {
        if (player.isPlaying()) {
            player.pause()
        } else {
            selectedSong?.let { player.play(it.audioUrl) }
        }
    }

    fun closePlayer() {
        showPlayer = false
    }

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressUpdaterJob = viewModelScope.launch {
            while (true) {
                currentPosition = player.exo.currentPosition
                totalDuration = player.exo.duration.coerceAtLeast(0L)
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdater() {
        progressUpdaterJob?.cancel()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(300_000)
                fetchSongs(forceReload = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.exo.removeListener(playerListener)
        autoRefreshJob?.cancel()
        stopProgressUpdater()
        player.release()
    }
}

@Composable
fun HeaderImageWithGradient(imageUrl: String?) {
    Box(modifier = Modifier.height(300.dp)) {
        Image(
            painter = rememberAsyncImagePainter(
                model = imageUrl,
                //error = painterResource(id = R.drawable.landingscreenbg),
                // placeholder = painterResource(id = R.drawable.landingscreenbg)
            ),
            contentDescription = "Header background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                        startY = 300f
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyPlayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm: EnergyViewModel = remember {
        val app = context.applicationContext as Application
        ViewModelProvider.AndroidViewModelFactory.getInstance(app)
            .create(EnergyViewModel::class.java)
    }

    BackHandler(enabled = vm.showPlayer) { vm.closePlayer() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Get the image URL from the first song, or null if the list is empty
            val headerImageUrl = vm.songs.firstOrNull()?.imageUrl
            HeaderImageWithGradient(imageUrl = headerImageUrl)

            when {
                vm.error != null -> ErrorState(vm.error!!, onRetry = { vm.fetchSongs(forceReload = true) })
                vm.loading && vm.songs.isEmpty() -> LoadingState()
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = if (vm.selectedSong != null) 80.dp else 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(220.dp)) // Pushes content below header
                        Text(
                            text = "Picked For You",
                            style = AppTypography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(16.dp)
                        )

                        if (vm.randomSongs.isNotEmpty()) {
                            FeaturedSongs(
                                songs = vm.randomSongs,
                                onSongClick = vm::onSongClick
                            )
                        }

                        Spacer(Modifier.height(36.dp))
                        Text(
                            text = "Popular Suggestions",
                            style = AppTypography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(8.dp))

                        vm.songs.forEach { song ->
                            SongRow(
                                song = song,
                                onSongClick = { vm.onSongClick(song) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { vm.fetchSongs(forceReload = true) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (vm.selectedSong != null) {
                MiniPlayer(
                    song = vm.selectedSong!!,
                    isPlaying = vm.isPlaying,
                    onPlayPause = vm::playPause,
                    onTap = vm::openFullScreenPlayer,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            AnimatedVisibility(
                visible = vm.showPlayer && vm.selectedSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                vm.selectedSong?.let {
                    FullScreenPlayer(
                        song = it,
                        isPlaying = vm.isPlaying,
                        onPlayPause = vm::playPause,
                        onClose = vm::closePlayer,
                        currentPosition = vm.currentPosition,
                        totalDuration = vm.totalDuration,
                        summary = vm.songSummary,
                        isSummaryLoading = vm.isSummaryLoading
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error loading songs", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun FeaturedSongs(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(songs) { song ->
            FeaturedSongCard(song = song, onSongClick = { onSongClick(song) })
        }
    }
}

@Composable
fun FeaturedSongCard(song: Song, onSongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onSongClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(song.imageUrl),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.secondary,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongRow(
    song: Song,
    onSongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSongClick)
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(song.imageUrl),
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(song.playTime, color = TextSecondary, fontSize = 12.sp)
        IconButton(onClick = { /* TODO: Handle more options */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary
            )
        }
    }
}

// --- PLAYER COMPOSABLES ---

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardBackground,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onTap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(song.imageUrl),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun FullScreenPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    currentPosition: Long,
    totalDuration: Long,
    summary: String?,
    isSummaryLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f) // Ensures the Box is always a square
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground) // Placeholder background
            ) {
                Image(
                    painter = rememberAsyncImagePainter(song.imageUrl),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize() // Image fills the square Box
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                song.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song.artist,
                fontSize = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // --- SONG SUMMARY SECTION ---
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Allow this box to take up remaining space
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isSummaryLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                        !summary.isNullOrBlank() -> {
                            Text(
                                text = summary,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                        else -> {
                            // Empty state if API call fails or returns nothing
                            Text(
                                text = "No summary available.",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            // --- END SUMMARY SECTION ---

            Spacer(Modifier.height(16.dp))

            val progress = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = CardBackground
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), color = TextSecondary, fontSize = 12.sp)
                Text(formatTime(totalDuration), color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {

                IconButton(onClick = { /*TODO: Previous*/ }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = { /*TODO: Next*/ }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                }

            }
        }
    }
}
