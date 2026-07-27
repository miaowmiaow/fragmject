package com.example.miaow.picture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.miaow.base.vm.TRANSITION_TIME
import com.example.miaow.picture.ui.editor.PictureEditorScreen
import com.example.miaow.picture.ui.selector.PicturePreviewScreen
import com.example.miaow.picture.ui.selector.PictureSelectorScreen
import com.example.miaow.picture.ui.selector.PictureViewModel
import com.example.miaow.picture.ui.selector.PreviewMode
import kotlinx.serialization.Serializable

class PictureSelectorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: PictureViewModel = viewModel()
            val backStack = rememberNavBackStack(SelectorNavKey)

            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) backStack.removeLastOrNull()
                    else finish()
                },
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    slideInHorizontally(tween(TRANSITION_TIME)) { it } togetherWith
                            slideOutHorizontally(tween(TRANSITION_TIME)) { -it }
                },
                popTransitionSpec = {
                    slideInHorizontally(tween(TRANSITION_TIME)) { -it } togetherWith
                            slideOutHorizontally(tween(TRANSITION_TIME)) { it }
                },
                entryProvider = entryProvider {
                    entry<SelectorNavKey> {
                        PictureSelectorScreen(
                            modifier = Modifier.fillMaxSize(),
                            onFinish = { data ->
                                val intent = Intent().apply {
                                    putParcelableArrayListExtra("data", ArrayList(data))
                                }
                                setResult(RESULT_OK, intent)
                                finish()
                            },
                            onDismiss = { finish() },
                            onPreview = { positions ->
                                backStack.add(PreviewNavKey(positions))
                            },
                            viewModel = viewModel,
                        )
                    }
                    entry<PreviewNavKey> { route ->
                        PicturePreviewScreen(
                            mode = PreviewMode.SELECT,
                            origSelectPosition = route.positions,
                            previewPosition = 0,
                            onFinish = { selectedPositions ->
                                val data = selectedPositions.mapNotNull {
                                    viewModel.currAlbumResult.value.getOrNull(it)
                                }
                                val intent = Intent().apply {
                                    putParcelableArrayListExtra("data", ArrayList(data))
                                }
                                setResult(RESULT_OK, intent)
                                finish()
                            },
                            onDismiss = { backStack.removeLastOrNull() },
                            onOpenEditor = { uri ->
                                backStack.add(EditorNavKey(uri.toString()))
                            },
                            viewModel = viewModel,
                        )
                    }
                    entry<EditorNavKey> { route ->
                        val oldUri = route.oldUriString.toUri()
                        PictureEditorScreen(
                            bitmapUri = oldUri,
                            onFinish = { _, newUri ->
                                viewModel.updateMediaUri(oldUri, newUri)
                                backStack.removeLastOrNull()
                            },
                            onDismiss = { backStack.removeLastOrNull() },
                        )
                    }
                }
            )
        }
    }
}

@Serializable
object SelectorNavKey : NavKey

@Serializable
data class PreviewNavKey(val positions: List<Int>) : NavKey

@Serializable
data class EditorNavKey(val oldUriString: String) : NavKey