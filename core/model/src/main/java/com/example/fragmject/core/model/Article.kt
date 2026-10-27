package com.example.fragmject.core.model

/**
 * 文章分页信封（服务端 wire 形状）。
 *
 * 与 [Article] 同属**共享协议模型**：单层落点，不做 DTO / 领域模型 / UI 模型三套拆分。
 * 决策与约束依据见 `docs/adr/0001-shared-protocol-model.md`；UI 侧的差异化模型由
 * `core:ui` 的 `ArticleFeedMapper`（[Article] → `FeedCardUIState`）承担。
 *
 * 不要在本类旁新增 `ArticleDataDto` 一类的变体：`ArchitectureTest` 规则 29 会拦截，
 * 且 `core:domain` 只能依赖 `core:model`，拆出 DTO 会让 7 个领域端口签名断裂。
 */
data class ArticleData(
    val curPage: String = "",
    val datas: List<Article>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
)

data class Article(
    val apkLink: String = "",
    val audit: String = "",
    val author: String = "",
    val canEdit: Boolean = false,
    val chapterId: String = "",
    val chapterName: String = "",
    val collect: Boolean = false,
    val courseId: String = "",
    val desc: String = "",
    val descMd: String = "",
    val envelopePic: String = "",
    val top: Boolean = false,
    val fresh: Boolean = false,
    val host: String = "",
    val id: String = "0",
    val link: String = "",
    val niceDate: String = "",
    val niceShareDate: String = "",
    val origin: String = "",
    val prefix: String = "",
    val projectLink: String = "",
    val publishTime: String = "",
    val realSuperChapterId: String = "",
    val selfVisible: String = "",
    val shareDate: String = "",
    val shareUser: String = "",
    val superChapterId: String = "",
    val superChapterName: String = "",
    val tags: List<ArticleTag>? = null,
    val title: String = "",
    val type: String = "",
    val userId: String = "0",
    val visible: String = "",
    val zan: String = "",
    val banners: List<Banner>? = null,
    val viewType: Int = 1
)

data class ArticleTag(
    val name: String = "",
    val url: String = ""
)