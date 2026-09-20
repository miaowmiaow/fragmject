package com.example.fragmject.core.data.paging

import androidx.paging.PagingConfig

/**
 * 项目统一分页配置：每页 20 条，禁用占位符。
 *
 * 所有纯网络分页器共享同一份配置，避免 [PagingConfig] 在各 Repository 重复。
 */
val DEFAULT_PAGING_CONFIG: PagingConfig = PagingConfig(
    pageSize = 20,
    enablePlaceholders = false,
)
