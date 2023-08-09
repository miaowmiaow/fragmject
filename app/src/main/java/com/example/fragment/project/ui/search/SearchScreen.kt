package com.example.fragment.project.ui.search

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.bean.HotKeyBean
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.ClearTextField
import com.example.fragment.project.components.LoadingLayout
import com.example.fragment.project.components.SwipeRefresh

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    hotKey: List<HotKeyBean>?,
    key: String,
    viewModel: SearchViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchText by rememberSaveable { mutableStateOf(key) }
    Column {
        Row(
            modifier = Modifier
                .background(colorResource(R.color.theme))
                .fillMaxWidth()
                .height(45.dp)
                .padding(15.dp, 8.dp, 15.dp, 8.dp),
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
                    .background(colorResource(R.color.three_nine_gray), RoundedCornerShape(50))
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = TextStyle.Default.copy(
                    color = colorResource(R.color.text_fff),
                    fontSize = 13.sp,
                    background = colorResource(R.color.transparent),
                ),
                placeholder = {
                    Text(
                        text = "多个关键词请用空格隔开",
                        color = colorResource(R.color.text_fff),
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
                        viewModel.updateSearchHistory(searchText)
                        viewModel.getHome(searchText)
                    }
                ),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = colorResource(id = R.color.transparent),
                    unfocusedIndicatorColor = colorResource(id = R.color.transparent),
                )
            )
            Spacer(Modifier.width(15.dp))
            Text(
                text = "取消",
                modifier = Modifier.clickable {
                    if (context is AppCompatActivity) {
                        context.onBackPressedDispatcher.onBackPressed()
                    }
                },
                fontSize = 14.sp,
                color = colorResource(R.color.white),
            )
        }
        LoadingLayout(uiState.refreshing && !uiState.loading) {
            Column {
                if (searchText.isBlank() || !uiState.refreshing && uiState.articlesResult.isEmpty()) {
                    Text(
                        text = "大家都在搜",
                        modifier = Modifier.padding(15.dp),
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_333),
                    )
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        hotKey?.forEach {
                            Box(modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)) {
                                Button(
                                    onClick = {
                                        searchText = it.name
                                        viewModel.updateSearchHistory(searchText)
                                        viewModel.getHome(searchText)
                                    },
                                    modifier = Modifier
                                        .height(40.dp)
                                        .padding(top = 5.dp, bottom = 5.dp),
                                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = colorResource(R.color.gray_e5),
                                        contentColor = colorResource(R.color.text_666)
                                    ),
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
                    if (uiState.historyResult.isNotEmpty()) {
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
                            itemsIndexed(uiState.historyResult.toList()) { _, item ->
                                Row(
                                    modifier = Modifier
                                        .background(colorResource(R.color.white))
                                        .padding(start = 15.dp, end = 15.dp)
                                        .height(45.dp)
                                        .clickable {
                                            searchText = item
                                            viewModel.getHome(searchText)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.weight(1f),
                                        color = colorResource(id = R.color.text_333),
                                        fontSize = 14.sp,
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .padding(10.dp, 5.dp, 0.dp, 5.dp)
                                            .clickable { viewModel.removeSearchHistory(item) },
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
}