package com.example.fragment.project.data.repository

import androidx.annotation.VisibleForTesting

/**
 * 极轻量 DI 替代：单点暴露所有 Repository 实例。
 *
 * 设计动机：
 * - 不引入 Hilt/Koin，避免学习成本与构建配置膨胀；
 * - 单测/Mock 时通过 [setForTest] 注入替身实现，恢复用 [resetForTest]。
 *
 * 使用方式：
 * ```kotlin
 * class HomeViewModel(
 *     private val articleRepo: ArticleRepository = WanRepositoryProvider.article
 * ) : BaseViewModel()
 * ```
 *
 * 默认参数让 ViewModel 在生产代码中保持零样板，同时为测试预留替换入口。
 */
object WanRepositoryProvider {

    @Volatile
    private var articleImpl: ArticleRepository = ArticleRepositoryImpl()

    @Volatile
    private var projectImpl: ProjectRepository = ProjectRepositoryImpl()

    @Volatile
    private var userImpl: UserRepository = UserRepositoryImpl()

    @Volatile
    private var myImpl: MyRepository = MyRepositoryImpl()

    @Volatile
    private var commonImpl: CommonRepository = CommonRepositoryImpl()

    val article: ArticleRepository get() = articleImpl
    val project: ProjectRepository get() = projectImpl
    val user: UserRepository get() = userImpl
    val my: MyRepository get() = myImpl
    val common: CommonRepository get() = commonImpl

    @VisibleForTesting
    fun setForTest(
        article: ArticleRepository? = null,
        project: ProjectRepository? = null,
        user: UserRepository? = null,
        my: MyRepository? = null,
        common: CommonRepository? = null,
    ) {
        article?.let { articleImpl = it }
        project?.let { projectImpl = it }
        user?.let { userImpl = it }
        my?.let { myImpl = it }
        common?.let { commonImpl = it }
    }

    @VisibleForTesting
    fun resetForTest() {
        articleImpl = ArticleRepositoryImpl()
        projectImpl = ProjectRepositoryImpl()
        userImpl = UserRepositoryImpl()
        myImpl = MyRepositoryImpl()
        commonImpl = CommonRepositoryImpl()
    }
}
