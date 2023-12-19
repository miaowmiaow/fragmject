package com.example.fragment.project.ui.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.fragment.project.components.ReorderLazyColumn
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 第一个 item 向下拖动时会出现异常滚动，待解决中
 */
@Composable
fun ColumnSortScreen(
    viewModel: ColumnViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = rememberLazyListState()

    ReorderLazyColumn(
        items = uiState.result,
        key = { _, item -> item.id },
        onMove = { from, to ->
            viewModel.move(from, to)
        },
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) { _, item ->
        Row(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
        ) {
            Image(
                painter = rememberAsyncImagePainter(item.getAvatarRes()),
                contentDescription = null,
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            )
        }
    }
}

data class ColumnUiState(
    var result: MutableList<Photo> = ArrayList(),
    val updateTime: Long = 0
)

class ColumnViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(ColumnUiState())

    val uiState: StateFlow<ColumnUiState> = _uiState.asStateFlow()

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