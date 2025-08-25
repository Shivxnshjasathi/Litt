package com.example.lilt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.lilt.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

// --- DATA MODELS ---

@Serializable
data class ChartResponse(
    val date: String,
    val data: List<ChartItem>
)

@Serializable
data class ChartItem(
    val name: String,
    val artist: String? = null, // Artist can be null for Artist 100 chart
    val image: String?,
    val rank: Int,
    @SerialName("last_week_rank") val lastWeekRank: Int?,
    @SerialName("peak_rank") val peakRank: Int?,
    @SerialName("weeks_on_chart") val weeksOnChart: Int?
)

// --- VIEWMODEL ---

class BillboardViewModel : ViewModel() {
    var hot100 by mutableStateOf<List<ChartItem>>(emptyList())
    var billboard200 by mutableStateOf<List<ChartItem>>(emptyList())
    var global200 by mutableStateOf<List<ChartItem>>(emptyList())
    var artist100 by mutableStateOf<List<ChartItem>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = "BillboardViewModel"

    init {
        fetchAllCharts()
    }

    private fun fetchAllCharts() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            Log.d(TAG, "Starting to fetch all charts...")
            try {
                // Simulate network delay for showing shimmer
                kotlinx.coroutines.delay(1500)
                hot100 = fetchChart("https://raw.githubusercontent.com/KoreanThinker/billboard-json/main/billboard-hot-100/recent.json")
                billboard200 = fetchChart("https://raw.githubusercontent.com/KoreanThinker/billboard-json/main/billboard-200/recent.json")
                global200 = fetchChart("https://raw.githubusercontent.com/KoreanThinker/billboard-json/main/billboard-global-200/recent.json")
                artist100 = fetchChart("https://raw.githubusercontent.com/KoreanThinker/billboard-json/main/billboard-artist-100/recent.json")
                Log.d(TAG, "All charts fetched successfully.")
            } catch (e: Exception) {
                errorMessage = "Failed to load charts: ${e.javaClass.simpleName} - ${e.message}"
                Log.e(TAG, "Error fetching charts: $errorMessage", e)
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun fetchChart(url: String): List<ChartItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching chart from URL: $url")
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Response for $url: $responseBody")
                responseBody?.let {
                    val chartResponse = json.decodeFromString<ChartResponse>(it)
                    Log.d(TAG, "Parsed ${chartResponse.data.size} items from $url")
                    chartResponse.data
                } ?: emptyList()
            } else {
                Log.e(TAG, "Failed to fetch chart from $url. Response code: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching chart from $url", e)
            emptyList()
        }
    }
}


// --- COMPOSABLE ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopCharts() {
    val viewModel = remember { BillboardViewModel() }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Hot 100", "Board 200", "Fav 100", "Artist 100")

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
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Top Charts",
                            style = AppTypography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    },
                    modifier = Modifier.statusBarsPadding(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF1DB954) // Spotify Green
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(title, style = AppTypography.bodyMedium)
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    }, label = "tabContentAnimation"
                ) { targetIndex ->
                    val chartData = when (targetIndex) {
                        0 -> viewModel.hot100
                        1 -> viewModel.billboard200
                        2 -> viewModel.global200
                        3 -> viewModel.artist100
                        else -> emptyList()
                    }

                    if (viewModel.isLoading) {
                        LoadingChartGrid()
                    } else if (viewModel.errorMessage != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.errorMessage!!,
                                style = AppTypography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        ChartGrid(chartItems = chartData)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartGrid(chartItems: List<ChartItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(chartItems, key = { it.rank }) { item ->
            ChartTileView(item = item, modifier = Modifier.animateItemPlacement(
                tween(durationMillis = 300)
            ))
        }
    }
}

@Composable
fun LoadingChartGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(10) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
fun ChartTileView(item: ChartItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isImageLoading by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable {
                openYouTube(context, item.name, item.artist)
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoading = false }
            )
            if (isImageLoading) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect())
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 300f
                        )
                    )
            )
            Text(
                text = "#${item.rank}",
                style = AppTypography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = item.name,
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.artist?.let {
                    Text(
                        text = it,
                        style = AppTypography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun openYouTube(context: Context, name: String, artist: String?) {
    val searchQuery = URLEncoder.encode("$name ${artist ?: ""}", "UTF-8")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$searchQuery"))
    context.startActivity(intent)
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width,
        targetValue = 2 * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF3A3A3A),
                Color(0xFF505050),
                Color(0xFF3A3A3A),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width, size.height)
        )
    )
        .onGloballyPositioned {
            size = it.size.toSize()
        }
}
