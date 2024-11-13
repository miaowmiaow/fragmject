package com.example.fragment.project.ui.exoplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    control: ExoPlayerControl,
    playListener: Player.Listener = object : Player.Listener {},
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var playWhenReady by remember { mutableStateOf(true) }
    var currentMediaItemIndex by remember { mutableIntStateOf(0) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerView, control) {
        playerView?.let {
            with(control) {
                handleControlEvents(
                    onPlay = {
                        it.player?.play()
                    },
                    onPause = {
                        it.player?.pause()
                    },
                    onPrevious = {
                        it.player?.seekToPrevious()
                    },
                    onNext = {
                        it.player?.seekToNext()
                    }
                )
            }
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
                setFullscreenButtonClickListener {  }
                player = ExoPlayer.Builder(context).build().also {
                    it.trackSelectionParameters =
                        it.trackSelectionParameters.buildUpon().setMaxVideoSizeSd().build()
                    it.addListener(playListener)
                    it.setMediaItems(mediaItems)
                    it.playWhenReady = playWhenReady
                    it.seekTo(currentMediaItemIndex, playbackPosition)
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
                playbackPosition = this.currentPosition
                currentMediaItemIndex = this.currentMediaItemIndex
                playWhenReady = this.playWhenReady
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
    }

    private val controlEvents: MutableSharedFlow<ControlEvent> = MutableSharedFlow()

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        onPlay: () -> Unit = {},
        onPause: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
    ) = withContext(Dispatchers.Main) {
        controlEvents.debounce(350).collect { event ->
            when (event) {
                ControlEvent.Play -> onPlay()
                ControlEvent.Pause -> onPause()
                ControlEvent.Previous -> onPrevious()
                ControlEvent.Next -> onNext()
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
}

@Composable
fun rememberExoPlayerControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ExoPlayerControl = remember(coroutineScope) { ExoPlayerControl(coroutineScope) }