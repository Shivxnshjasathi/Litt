package com.example.lilt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
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
    var isLoading by mutableStateOf(false)
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
            containerColor = Color.Transparent, // Make Scaffold transparent
            topBar = {
                TopAppBar(
                    title = { Text("Top Charts", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.statusBarsPadding(), // Added padding for the status bar
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make TopAppBar transparent
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent, // Make TabRow transparent
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
                            text = { Text(title) },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                // The main content area
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF1DB954)
                        )
                    } else if (viewModel.errorMessage != null) {
                        Text(
                            text = viewModel.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        val chartData = when (selectedTabIndex) {
                            0 -> viewModel.hot100
                            1 -> viewModel.billboard200
                            2 -> viewModel.global200
                            3 -> viewModel.artist100
                            else -> emptyList()
                        }
                        ChartGrid(chartItems = chartData)
                    }
                }
            }
        }
    }
}

@Composable
fun ChartGrid(chartItems: List<ChartItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(chartItems) { item ->
            ChartTileView(item = item)
        }
    }
}

@Composable
fun ChartTileView(item: ChartItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                openYouTube(context, item.name, item.artist)
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                            startY = 200f
                        )
                    )
            )
            Text(
                text = "#${item.rank}",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.artist?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
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
