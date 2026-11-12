package com.mexo.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val image: String,
    val audio: String,
    val duration: Int,
    val license: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MexoMusicApp() }
    }
}

private val bg = Color(0xFF090B12)
private val panel = Color(0xFF171B28)
private val accent = Color(0xFF9B7BFF)
private val muted = Color(0xFF9AA0B2)

@Composable
fun MexoMusicApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(context).build() }
    var query by remember { mutableStateOf("chill") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    var current by remember { mutableStateOf<Track?>(null) }
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Discover") }

    fun searchSongs(term: String) {
        scope.launch {
            loading = true
            error = ""
            try {
                tracks = JamendoApi.search(term)
                if (tracks.isEmpty()) error = "No tracks found. Try another search."
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Check your internet and Client ID."
            } finally { loading = false }
        }
    }

    fun playTrack(track: Track) {
        current = track
        player.setMediaItem(MediaItem.fromUri(track.audio))
        player.prepare()
        player.play()
        playing = true
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("mexo_music", 0)
        favorites = prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()
        searchSongs("chill")
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent, background = bg, surface = panel,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Box(Modifier.fillMaxSize().background(bg)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(accent, Color(0xFF53D6C7)))),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MEXO", fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("MUSIC DISCOVERY", color = muted, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Icon(Icons.Default.Equalizer, null, tint = accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(26.dp))
                Text(if (tab == "Discover") "Find your sound." else "Your collection.", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Independent music, made for your mood.", color = muted, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(panel).padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = muted)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search songs, artists, genres...", color = muted, fontSize = 14.sp)
                            inner()
                        }
                    )
                    IconButton(onClick = { if (query.isNotBlank()) searchSongs(query.trim()) }) {
                        Icon(Icons.Default.ArrowForward, null, tint = accent)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = tab == "Discover", onClick = { tab = "Discover" }, label = { Text("Discover") })
                    FilterChip(selected = tab == "Playlist", onClick = { tab = "Playlist" }, label = { Text("♥ Playlist") })
                    if (tab == "Discover") {
                        listOf("Chill", "Rock", "Focus").forEach { tag ->
                            AssistChip(onClick = { query = tag.lowercase(); searchSongs(tag.lowercase()) }, label = { Text(tag) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = accent)
                    Spacer(Modifier.height(10.dp))
                }
                if (error.isNotBlank()) Text(error, color = Color(0xFFFF9A9A), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                val shown = if (tab == "Playlist") tracks.filter { favorites.contains(it.id) } else tracks
                Text(if (tab == "Playlist") "SAVED TRACKS" else "MADE FOR YOU",
                    color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            favorite = favorites.contains(track.id),
                            onPlay = { playTrack(track) },
                            onFavorite = {
                                favorites = if (favorites.contains(track.id)) favorites - track.id else favorites + track.id
                                context.getSharedPreferences("mexo_music", 0).edit()
                                    .putStringSet("favorites", favorites).apply()
                            }
                        )
                    }
                    if (tab == "Playlist" && shown.isEmpty()) {
                        item { Text("Your playlist is empty. Tap ♥ on a track to save it.", color = muted, modifier = Modifier.padding(16.dp)) }
                    }
                }
                current?.let { track ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF22263A)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(track.image, null, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = muted, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = {
                            if (playing) { player.pause(); playing = false }
                            else { player.play(); playing = true }
                        }) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = accent)
                        }
                        IconButton(onClick = {
                            val next = tracks.indexOfFirst { it.id == track.id } + 1
                            if (next in tracks.indices) playTrack(tracks[next])
                        }) { Icon(Icons.Default.SkipNext, null) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavigationItem("Home", Icons.Default.Home, tab == "Discover") { tab = "Discover" }
                    NavigationItem("Playlist", Icons.Default.Favorite, tab == "Playlist") { tab = "Playlist" }
                }
            }
        }
    }
}

@Composable
fun TrackRow(track: Track, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(panel)
            .clickable { onPlay() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(track.image, contentDescription = "Album cover",
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatDuration(track.duration), color = muted, fontSize = 10.sp)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                null, tint = if (favorite) accent else muted)
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Default.PlayCircleFilled, null, tint = accent, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
fun NavigationItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable { onClick() }.padding(horizontal = 22.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) accent else muted)
        Text(label, color = if (selected) accent else muted, fontSize = 10.sp)
    }
}

private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

object JamendoApi {
    private val client = OkHttpClient()
    suspend fun search(term: String): List<Track> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(term, "UTF-8")
        val url = "https://api.jamendo.com/v3.0/tracks/?client_id=${BuildConfig.JAMENDO_CLIENT_ID}&format=json&limit=30&search=$q&type=single%20albumtrack&audioformat=mp32&imagesize=300&include=licenses"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Jamendo API error: HTTP ${response.code}")
            val body = response.body?.string() ?: error("Empty API response")
            val json = JSONObject(body)
            val headers = json.optJSONObject("headers")
            if (headers?.optString("status") == "error") error(headers.optString("error_message", "Jamendo API error"))
            val array = json.optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val audio = item.optString("audio")
                    if (audio.isBlank() || audio == "null") continue
                    add(Track(
                        id = item.optString("id"),
                        name = item.optString("name", "Unknown track"),
                        artist = item.optString("artist_name", "Unknown artist"),
                        image = item.optString("image"),
                        audio = audio,
                        duration = item.optInt("duration"),
                        license = item.optString("license_ccurl")
                    ))
                }
            }
        }
    }
}
