package com.example.fragment.project.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.data.HotKey
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.ClearTextField
import com.example.fragment.project.components.SwipeRefresh
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    key: String,
    hotKeyData: List<HotKey>?,
    searchHistoryData: List<String>,
    viewModel: SearchViewModel = viewModel(),
    onSearchHistory: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
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
                    .background(colorResource(R.color.theme))
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
                        viewModel.clearArticles()
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clipToBounds()
                        .background(colorResource(R.color.three_nine_gray))
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle.Default.copy(
                        color = colorResource(R.color.text_fff),
                        fontSize = 13.sp,
                        background = colorResource(R.color.transparent),
                    ),
                    placeholder = {
                        Text(
                            text = key.ifBlank { "多个关键词请用空格隔开" },
                            color = colorResource(R.color.text_999),
                            fontSize = 13.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp, 5.dp, 10.dp, 5.dp),
                            tint = colorResource(R.color.white)
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.isNotBlank()) {
                                onSearchHistory(true, searchText)
                                viewModel.getHome(searchText)
                            } else {
                                viewModel.clearArticles()
                            }
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = colorResource(id = R.color.transparent),
                        unfocusedIndicatorColor = colorResource(id = R.color.transparent),
                    )
                )
                Text(
                    text = "取消",
                    modifier = Modifier
                        .clickable { onNavigateUp() }
                        .padding(horizontal = 15.dp),
                    fontSize = 14.sp,
                    color = colorResource(R.color.white),
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!uiState.isSearch) {
                Text(
                    text = "大家都在搜",
                    modifier = Modifier.padding(15.dp),
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_333),
                )
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    hotKeyData?.forEach {
                        Box(modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    searchText = it.name
                                    onSearchHistory(true, searchText)
                                    viewModel.getHome(searchText)
                                },
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(top = 5.dp, bottom = 5.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(R.color.gray_e5),
                                    contentColor = colorResource(R.color.text_666)
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
                if (searchHistoryData.isNotEmpty()) {
                    Text(
                        text = "历史搜索",
                        modifier = Modifier.padding(15.dp),
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_333),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        itemsIndexed(searchHistoryData) { _, item ->
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        focusManager.clearFocus()
                                        searchText = item
                                        viewModel.getHome(searchText)
                                    }
                                    .background(colorResource(R.color.white))
                                    .height(45.dp)
                                    .padding(horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f),
                                    color = colorResource(id = R.color.text_333),
                                    fontSize = 14.sp,
                                )
                                Icon(
                                    painter = painterResource(R.mipmap.ic_delete),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clickable { onSearchHistory(false, item) }
                                        .size(30.dp)
                                        .padding(10.dp, 5.dp, 0.dp, 5.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                SwipeRefresh(
                    items = uiState.articlesResult,
                    refreshing = uiState.refreshing,
                    loading = uiState.loading,
                    finishing = uiState.finishing,
                    onRefresh = { viewModel.getHome(searchText) },
                    onLoad = { viewModel.getNext(searchText) },
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