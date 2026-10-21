package com.example.fragmject.feature.search.ui.search

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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.NAV_TRANSITION_DURATION_MS
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.History
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.ClearTextField
import com.example.fragmject.core.ui.components.CollectInteraction
import com.example.fragmject.core.ui.components.ContentPhase
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.SkeletonRows
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData
import com.example.fragmject.feature.search.impl.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** 顶部搜索框的交互回调。 */
@Stable
private class SearchBarActions(
    val onTextChange: (String) -> Unit,
    val onSubmit: (String) -> Unit,
    val onExit: () -> Unit,
    val onClear: () -> Unit,
    val onCancel: () -> Unit,
)

/** 热搜/历史搜索区的交互回调。 */
@Stable
private class SearchSuggestionActions(
    val onKeywordClick: (String) -> Unit,
    val onHistoryClick: (String) -> Unit,
    val onHistoryDelete: (History) -> Unit,
    val onRetry: () -> Unit,
)

@Composable
fun SearchScreen(
    key: String,
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val onNavigateUp = LocalOnNavigateUp.current
    val isShowingSearchResults by searchViewModel.isShowingSearchResults.collectAsStateWithLifecycle()
    val isHotKeyLoading by searchViewModel.isHotKeyLoading.collectAsStateWithLifecycle()
    val hotKeyResult by searchViewModel.hotKeyResult.collectAsStateWithLifecycle()
    val searchHistoryResult by searchViewModel.searchHistoryResult.collectAsStateWithLifecycle()
    val hotKeyError by searchViewModel.hotKeyError.collectAsStateWithLifecycle()
    val hotKeyPhase = hotKeyPhase(isHotKeyLoading, hotKeyError, hotKeyResult)
    val pagingItems = searchViewModel.pagingFlow.collectAsLazyPagingItems()
    val collectInteraction = rememberCollectInteraction(searchViewModel.collectState)
    var searchText by rememberSaveable { mutableStateOf(key) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(searchText.isNotBlank()) {
        searchText = ""
        searchViewModel.exitSearchResults()
    }
    SearchTextEffects(
        key = key,
        searchText = searchText,
        onInitialKey = { initialKey ->
            searchText = initialKey
            searchViewModel.submitSearch(initialKey)
        },
        focusRequester = focusRequester,
        keyboardController = keyboardController,
    )
    val barActions = SearchBarActions(
        onTextChange = { searchText = it },
        onSubmit = searchViewModel::submitSearch,
        onExit = searchViewModel::exitSearchResults,
        onClear = {
            searchText = ""
            searchViewModel.exitSearchResults()
        },
        onCancel = onNavigateUp,
    )
    val suggestionActions = SearchSuggestionActions(
        onKeywordClick = { name ->
            focusManager.clearFocus()
            searchText = name
            searchViewModel.submitSearch(name)
        },
        onHistoryClick = { value ->
            focusManager.clearFocus()
            searchText = value
            searchViewModel.submitSearch(value)
        },
        onHistoryDelete = searchViewModel::deleteHistory,
        onRetry = searchViewModel::retryHotKey,
    )
    Scaffold(
        topBar = {
            SearchTopBar(
                searchText = searchText,
                placeholder = key.ifBlank { "多个关键词请用空格隔开" },
                focusRequester = focusRequester,
                actions = barActions,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!isShowingSearchResults) {
                SearchSuggestionPanel(
                    phase = hotKeyPhase,
                    hotKeys = hotKeyResult,
                    history = searchHistoryResult,
                    actions = suggestionActions,
                )
            } else {
                SearchResults(pagingItems = pagingItems, collectInteraction = collectInteraction)
            }
        }
    }
}

/** 顶部搜索框：输入、清除、键盘搜索与取消返回。 */
@Composable
private fun SearchTopBar(
    searchText: String,
    placeholder: String,
    focusRequester: FocusRequester,
    actions: SearchBarActions,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
            onValueChange = actions.onTextChange,
            onClear = actions.onClear,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clipToBounds()
                .background(AppColors.alphaGray)
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
                    text = placeholder,
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
                        actions.onSubmit(searchText)
                    } else {
                        actions.onExit()
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
                .clickable { actions.onCancel() }
                .padding(horizontal = 15.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** 未出搜索结果时的热搜 + 历史搜索。 */
@Composable
private fun SearchSuggestionPanel(
    phase: ContentPhase,
    hotKeys: List<HotKey>,
    history: List<History>,
    actions: SearchSuggestionActions,
) {
    ContentStateBox(
        phase = phase,
        loadingContent = { SkeletonRows(it) },
        emptyContent = { },
        onRetry = actions.onRetry,
    ) {
        HotKeySection(hotKeys = hotKeys, onClick = actions.onKeywordClick)
    }
    if (history.isNotEmpty()) {
        SearchHistorySection(
            history = history,
            onClick = actions.onHistoryClick,
            onDelete = actions.onHistoryDelete,
        )
    }
}

/** 热搜词区域。 */
@Composable
private fun HotKeySection(
    hotKeys: List<HotKey>,
    onClick: (String) -> Unit,
) {
    Text(
        text = "大家都在搜",
        modifier = Modifier.padding(15.dp),
        fontSize = 16.sp,
    )
    FlowRow(modifier = Modifier.fillMaxWidth()) {
        hotKeys.forEach { hotKey ->
            Box(modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)) {
                Button(
                    onClick = { onClick(hotKey.name) },
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
                        text = hotKey.name,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/** 历史搜索列表，右侧图标删除单条。 */
@Composable
private fun SearchHistorySection(
    history: List<History>,
    onClick: (String) -> Unit,
    onDelete: (History) -> Unit,
) {
    Text(
        text = "历史搜索",
        modifier = Modifier.padding(15.dp),
        fontSize = 16.sp,
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        itemsIndexed(
            history,
            key = { _, item -> item.id }
        ) { _, item ->
            Row(
                modifier = Modifier
                    .clickable { onClick(item.value) }
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
                        .clickable { onDelete(item) }
                        .size(30.dp)
                        .padding(10.dp, 5.dp, 0.dp, 5.dp),
                )
            }
        }
    }
}

/** 搜索结果列表：内容态骨架 + 下拉刷新 + 分页卡片。 */
@Composable
private fun SearchResults(
    pagingItems: LazyPagingItems<Article>,
    collectInteraction: CollectInteraction,
) {
    val articleNavigator = LocalNavigators.current.article
    val userNavigator = LocalNavigators.current.user
    val homeNavigator = LocalNavigators.current.home
    val pictureNavigator = LocalNavigators.current.picture
    ContentStateBox(
        phase = pagingItems.contentPhase(),
        modifier = Modifier.fillMaxSize(),
        minShowMillis = NAV_TRANSITION_DURATION_MS,
        onRetry = { pagingItems.retry() },
    ) {
        PagingSwipeRefreshBox(
            pagingItems = pagingItems,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            key = { it.id },
        ) { item ->
            FeedCard(
                data = rememberArticleFeedData(item, collectInteraction.overrides),
                onItemClick = { articleNavigator.openArticle(it) },
                onUserClick = { userNavigator.openUserProfile(it) },
                onFooterClick = { homeNavigator.openSystemTree(it) },
                onToggleClick = collectInteraction.onToggleClick,
                onCoverClick = { url -> pictureNavigator.openPicturePreview(listOf(url)) },
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            )
        }
    }
}

/** 搜索词与焦点联动：入参 key 自动发起搜索；清空后延时重新聚焦并拉起键盘。 */
@Composable
private fun SearchTextEffects(
    key: String,
    searchText: String,
    onInitialKey: (String) -> Unit,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
) {
    val currentOnInitialKey by rememberUpdatedState(onInitialKey)
    LaunchedEffect(key) {
        if (key.isNotBlank()) currentOnInitialKey(key)
    }
    LaunchedEffect(searchText) {
        if (searchText.isNotBlank()) return@LaunchedEffect
        delay(350.milliseconds)
        if (searchText.isBlank()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

/** 热搜区内容态：错误 > 加载中 > 空 > 内容。 */
private fun hotKeyPhase(
    isLoading: Boolean,
    error: String,
    hotKeys: List<HotKey>,
): ContentPhase = when {
    error.isNotBlank() -> ContentPhase.Error
    isLoading -> ContentPhase.Loading
    hotKeys.isEmpty() -> ContentPhase.Empty
    else -> ContentPhase.Content
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SearchScreenPreview() {
    AppTheme { SearchScreen(key = "") }
}
