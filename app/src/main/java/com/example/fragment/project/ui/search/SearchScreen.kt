package com.example.fragment.project.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.WanViewModel
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.ClearTextField
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.SwipeRefreshBox
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    key: String,
    wanViewModel: WanViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val wanUiState by wanViewModel.uiState.collectAsStateWithLifecycle()
    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    var searchText by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(searchText.isNotBlank()) {
        searchText = ""
        searchViewModel.clearArticles()
    }
    LaunchedEffect(Unit) {
        delay(350)
        if (searchText.isBlank()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(15.dp, 8.dp, 0.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClearTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    onClear = {
                        searchText = ""
                        searchViewModel.clearArticles()
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clipToBounds()
                        .background(WanTheme.alphaGray)
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle.Default.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 13.sp,
                        background = Color.Transparent,
                    ),
                    placeholder = {
                        Text(
                            text = key.ifBlank { "多个关键词请用空格隔开" },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp, 5.dp, 10.dp, 5.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.isNotBlank()) {
                                searchViewModel.getHome(searchText)
                            } else {
                                searchViewModel.clearArticles()
                            }
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                Text(
                    text = "取消",
                    modifier = Modifier
                        .clickable { onNavigateUp() }
                        .padding(horizontal = 15.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    ) { innerPadding ->
        LoadingContent(isLoading = wanUiState.isLoading) {
            Column(modifier = Modifier.padding(innerPadding)) {
                if (!searchUiState.isSearch) {
                    Text(
                        text = "大家都在搜",
                        modifier = Modifier.padding(15.dp),
                        fontSize = 16.sp,
                    )
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        wanUiState.hotKeyResult.forEach {
                            Box(modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)) {
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        searchText = it.name
                                        searchViewModel.getHome(searchText)
                                    },
                                    modifier = Modifier
                                        .height(40.dp)
                                        .padding(top = 5.dp, bottom = 5.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp)
                                ) {
                                    Text(
                                        text = it.name,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    if (searchUiState.searchHistoryResult.isNotEmpty()) {
                        Text(
                            text = "历史搜索",
                            modifier = Modifier.padding(15.dp),
                            fontSize = 16.sp,
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            itemsIndexed(searchUiState.searchHistoryResult) { _, item ->
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            focusManager.clearFocus()
                                            searchText = item.value
                                            searchViewModel.getHome(searchText)
                                        }
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .height(45.dp)
                                        .padding(horizontal = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.value,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                    )
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_delete),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clickable {
                                                searchViewModel.deleteHistory(item)
                                            }
                                            .size(30.dp)
                                            .padding(10.dp, 5.dp, 0.dp, 5.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    SwipeRefreshBox(
                        items = searchUiState.articlesResult,
                        isRefreshing = searchUiState.isRefreshing,
                        isLoading = searchUiState.isLoading,
                        isFinishing = searchUiState.isFinishing,
                        onRefresh = { searchViewModel.getHome(searchText) },
                        onLoad = { searchViewModel.getNext(searchText) },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        key = { _, item -> item.id },
                    ) { _, item ->
                        ArticleCard(
                            data = item,
                            onNavigateToLogin = onNavigateToLogin,
                            onNavigateToSystem = onNavigateToSystem,
                            onNavigateToUser = onNavigateToUser,
                            onNavigateToWeb = onNavigateToWeb,
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SearchScreenPreview() {
    WanTheme { SearchScreen(key = "") }
}