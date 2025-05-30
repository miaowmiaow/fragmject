package com.example.fragment.project.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.miaow.base.utils.CacheUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ExoPlayer(
    mediaItems: List<MediaItem>,
    modifier: Modifier = Modifier,
    control: ExoPlayerControl = rememberExoPlayerControl(),
    playListener: Player.Listener = object : Player.Listener {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    LaunchedEffect(playerView, control) {
        val player = playerView?.player ?: return@LaunchedEffect
        with(control) {
            handleControlEvents(
                onPlay = {
                    player.play()
                },
                onPause = {
                    player.pause()
                },
                onPrevious = {
                    player.seekToPrevious()
                },
                onNext = {
                    player.seekToNext()
                },
                onSeekTo = {
                    player.seekTo(it)
                }
            )
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                playerView?.player?.play()
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                playerView?.player?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setShowShuffleButton(true)
                setFullscreenButtonClickListener { }
                val cacheDir = CacheUtils.getDirFile(context, "exoplayer_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024)
                val databaseProvider = StandaloneDatabaseProvider(context)
                val cache = SimpleCache(cacheDir, evictor, databaseProvider)
                val cacheDataSourceFactory = CacheDataSource.Factory().setCache(cache)
                    .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                player = ExoPlayer.Builder(context).setMediaSourceFactory(
                    DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory)
                ).build().also {
                    it.trackSelectionParameters =
                        it.trackSelectionParameters.buildUpon().setMaxVideoSizeSd().build()
                    it.addListener(playListener)
                    it.setMediaItems(mediaItems)
                    it.prepare()
                }
                playerView = this
            }
        },
        modifier = modifier
            .focusable()
            .onKeyEvent {
                playerView?.dispatchKeyEvent(it.nativeKeyEvent) ?: false
            },
        onRelease = {
            it.player?.run {
                removeListener(playListener)
                release()
            }
            it.player = null
            playerView = null
        }
    )
}

@Stable
class ExoPlayerControl(
    private val scope: CoroutineScope
) {
    private sealed interface ControlEvent {
        data object Play : ControlEvent
        data object Pause : ControlEvent
        data object Previous : ControlEvent
        data object Next : ControlEvent
        data class SeekTo(val positionMs: Long) : ControlEvent
    }

    private val controlEvents: MutableSharedFlow<ControlEvent> = MutableSharedFlow()

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        onPlay: () -> Unit = {},
        onPause: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        onSeekTo: (positionMs: Long) -> Unit = {},
    ) = withContext(Dispatchers.Main) {
        controlEvents.debounce(350).collect { event ->
            when (event) {
                ControlEvent.Play -> onPlay()
                ControlEvent.Pause -> onPause()
                ControlEvent.Previous -> onPrevious()
                ControlEvent.Next -> onNext()
                is ControlEvent.SeekTo -> onSeekTo(event.positionMs)
            }
        }
    }

    fun play() {
        scope.launch { controlEvents.emit(ControlEvent.Play) }
    }

    fun pause() {
        scope.launch { controlEvents.emit(ControlEvent.Pause) }
    }

    fun previous() {
        scope.launch { controlEvents.emit(ControlEvent.Previous) }
    }

    fun next() {
        scope.launch { controlEvents.emit(ControlEvent.Next) }
    }

    fun seekTo(positionMs: Long) {
        scope.launch { controlEvents.emit(ControlEvent.SeekTo(positionMs)) }
    }
}

@Composable
fun rememberExoPlayerControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ExoPlayerControl = remember(coroutineScope) { ExoPlayerControl(coroutineScope) }