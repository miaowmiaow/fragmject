package com.example.fragmject.core.ui.components

import androidx.compose.runtime.Immutable

/**
 * 通用信息流卡片的 UI 层展示模型。
 *
 * 由 ArticleCardUiState 泛化而来：剥离「章节 / 新 / 置顶 / 分类」等文章业务语义，
 * 统一抽象为 footer（底部信息，[footerText]/[footerId]）与 [footerBadges]（角标）两个通用槽位。
 * 零领域模型依赖，业务映射由各 feature 调用方完成。
 *
 * 标 [Immutable]：所有字段均为 val 且类型不可变（List 而非 MutableList），
 * 让 Compose 编译器直接信任其稳定性。
 */
@Immutable
data class FeedCardUiState(
    val id: String,
    val title: String,
    val desc: String,
    val date: String,
    val userId: String,
    val link: String,
    val footerId: String,
    val footerText: String,
    val coverUrl: String,
    val footerBadges: List<FooterBadge>,
    val selected: Boolean,
    // 预计算的派生字段：避免在每次重组时重复做字符串拼接与资源 ID 查找。
    val displayName: String,
    val avatarResId: Int,
)

/**
 * 卡片底部角标。
 */
@Immutable
data class FooterBadge(
    val text: String,
)
