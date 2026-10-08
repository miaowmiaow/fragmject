package com.example.fragmject.core.data.repository.paging

import androidx.paging.PagingConfig

/**
 * 项目统一分页配置：每页 20 条，禁用占位符。
 *
 * 所有纯网络分页器共享同一份配置，避免 [PagingConfig] 在各 Repository 重复。
 */
val DEFAULT_PAGING_CONFIG: PagingConfig = PagingConfig(
    pageSize = 20,
    enablePlaceholders = false,
    // 限制内存中保留的条目数：默认 MAX_SIZE_UNBOUNDED 会让已加载的页全部常驻，
    // 配合各 ViewModel 的 cachedIn(viewModelScope) 无限下滑时内存单调增长。
    // 10 页（200 条）足够覆盖回看场景；下限受 pageSize + 2*prefetchDistance 约束。
    maxSize = 200,
)
