package com.example.fragment.project.ui.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.ReorderLazyVerticalGrid
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun GridSortScreen(
    viewModel: PhotosGridViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = rememberLazyGridState()
    ReorderLazyVerticalGrid(
        items = uiState.result,
        key = { _, item -> item.id },
        onMove = { from, to ->
            viewModel.move(from, to)
        },
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) { _, item ->
        Image(
            painter = rememberAsyncImagePainter(item.getAvatarRes()),
            contentDescription = null,
            modifier = Modifier.aspectRatio(1f)
        )
    }
}

data class PhotosGridUiState(
    var result: MutableList<Photo> = ArrayList(),
    val updateTime: Long = 0
)

class PhotosGridViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(PhotosGridUiState())

    val uiState: StateFlow<PhotosGridUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { state ->
            List(50) {
                state.result.add(Photo(it))
            }
            state.copy(updateTime = System.nanoTime())
        }
    }

    fun move(from: Int, to: Int) {
        _uiState.update { state ->
            state.result.add(to, state.result.removeAt(from))
            state.copy(updateTime = System.nanoTime())
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun GridSortScreenPreview() {
    WanTheme { GridSortScreen() }
}