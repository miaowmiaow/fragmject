# 认知负担 / 上手成本 / 代码量 评审

> 评审视角：在分层架构已稳定的前提下，审查**认知负担、上手成本、代码量**三类成本。
> 允许通过**调整/删减业务逻辑**来降低复杂度（C 档）。
> 本轮为只读评审，未修改任何代码。

---

## 一、基线数字（评审时实测）

| 项 | 数量 |
|---|---|
| Gradle 模块 | **34**（`:app` + 14 `core:*` + 16 `feature:*:api/impl` + 3 `build-logic`） |
| Kotlin 文件 | 376 |
| `src/main` 源码行数 | **27,506** |
| UseCase | 10 |
| Repository 端口 / 实现 | 16 / 20 |
| ViewModel / Screen | 22 / 43 |
| Navigator 契约 / NavKey 文件 | 8 / 10 |
| 强制约束 | Konsist 15 条 + Gradle 守卫 4 张白名单 + 10 个约定插件 |

**核心矛盾**：27.5K 行业务体量，配了 34 个模块与 21+ 条强制规则。分层本身正确，但**单位业务量的架构开销偏高**，这是认知负担的主要来源。

---

## 二、认知负担来源

### 2.1 必要成本（分层机制，保留）

`feature:api/impl` 拆分、`core:domain` 端口、依赖守卫、Konsist 规则——这些是此前修复架构问题的机制，不在削减范围。

### 2.2 可改成本（自身设计，本次重点）

| # | 问题 | 实证 |
|---|---|---|
| 1 | **三层零逻辑链** | `HistoryRepository`（domain）→ `HistoryLocalDataSource`（data-contract，签名逐字相同）→ `HistoryRepositoryImpl`（7 个方法全是 `= local.xxx()`，**0 行逻辑**）。`Schedule`、`Theme` 同构 |
| 2 | **8 个 `feature:*:api` 零外部消费者** | 逐个核验：8 个 api **全部只有自己的 impl 依赖**。而守卫 R2/R3 又禁止 feature 跨域依赖 → api 层的存在理由与规则互斥 |
| 3 | **4 份逐字相同的 mapper + 为它写的补偿测试** | 4 份 `ArticleFeedMapper`；项目专门写了 `ArticleFeedMapperConsistencyTest`（57 行）断言「必须存在 4 份且函数体相等」。**为复制而写测试**，根因是 `:core:ui` 不允许依赖 `:core:model` |
| 4 | **8 份 NavModule + 8 份 NavRegistration 样板** | 8 个 `XxxNavModule.kt` 各 21 行 `@Module @InstallIn @Provides @IntoSet`，仅类名不同 |
| 5 | **两套导航注入心智模型** | home 走 `HomeNavActions` data class 传入；user / search / collection 直接读 `LocalNavigators`。新人看 home 学一套、看 user 学另一套 |
| 6 | **UiState 4 种形状并存** | `Loading/Success/Error`、`Success/Error`、`Loading/Success`、单变体（3 处）。另有 20 个 `(this as? Success)?.x ?: default` 访问器 |
| 7 | **三个同义词** | `contract`（`core:data-contract`）／`api`（`feature:*:api`）／`端口`（domain KDoc）——指的都是「只声明不实现」 |
| 8 | **feature 目录组织不一致** | home/collection/demo 有 `components/`，user/search 没有；picture 用 `model/state/utils/` |

---

## 三、优化清单

### A 档：纯删除，零风险，立即做

| 项 | 净减 | 说明 |
|---|---|---|
| ✅ 约定插件去掉 M2 `material`（已落地） | 1 行 + 1 别名 | **已核验**：本体引用 0；23 处均为 `material.icons`（独立 artifact）。项目全量 M3 |
| ✅ 删 `libs.androidx.room3.paging`（已落地） | 1 行 + 1 别名 | 源码零 import |
| ✅ 删 `FEATURE_EXTRA_CORE_DEPENDENCIES`（已落地） | 1 变量 + 15 行解释注释 | 现为 `emptyMap()`，为「未来可能破例」保留的死扩展点 |
| ✅ 删 `Vector2D`、`Error.code` ×4、`ProjectTreeUiState.isLoading`（已落地） | ~35 行 | 全仓零引用 |

### B 档：小幅重构，收益明确

| 项 | 净减 | 代价 |
|---|---|---|
| ✅ **4 份 mapper 下沉 `core:ui`**（已落地） | **-177 行**（含删一致性测试 57 行） | 白名单加 1 项：`:core:ui → :core:model`。`core:model` 零依赖，无传递污染 |
| ✅ **删 `CoinRankDao` / `CoinRankEntity` / `CoinRankMapping`**（已落地） | -87 行 + **1 张 Room 表** | 积分排行榜已改为纯网络分页，Room 缓存层为遗留死代码；version 13→14 新增删表迁移 |
| ✅ **删 `ShareArticleUseCase` / `LogoutUseCase`（内联重构）**（已落地） | 2 文件 | 二者为单 Repository 单步透传（零编排），内联为 ViewModel 直接依赖 Repository 端口，符合 §5 UseCase 边界 |
| ❌ **8 份 NavModule → 约定插件 / 单模块生成**（不做） | — | 净减仅 ~30 行，却要引入 130+ 行私有 KSP/生成器与「隐式注册」的理解/调试负担，不如保留 Hilt 显式样板 |
| ✅ **3 条零逻辑链各删 1 个接口**（已落地） | -6 文件（3 契约 + 3 透传 Impl） | `XxxLocalDataSourceImpl` 直接实现 domain 端口；`core:database` 新增 `core:domain` 依赖（数据实现依赖领域抽象，方向正确） |
| ✅ **UiState 统一（单变体降级）**（已落地） | -15 行 | 3 个单变体 sealed 降 data class；`AsyncUiState<T>` / `ListUiState<T>` 统一因语义差异大不做 |
| ✅ **收藏交互样板 ×6 → 一个 Composable**（已落地） | 净减 ~19 行 | 6 处样板收敛为 `rememberCollectInteraction`，放 `core:ui`，与 `FeedCard` 配套 |
| ✅ **TitleBar 默认返回箭头**（已落地） | -62 行 | 7 处复制的 `IconButton + ArrowBack` 收敛为 `TitleBar(onNavigateUp)` 默认箭头 |
| ❌ **新增 `fragmject.android.feature` 约定插件**（不做） | — | 收益小（净减 ~46 行）且适配性差：吸收的是会随 feature 职责分化的依赖，非 library/compose/hilt 那类「绝对基线」，显式声明更有价值 |
| ✅ **`config.properties` 三处重复加载归一**（已落地） | 2 段 | `ProjectExtensions` 新增 `loadConfigProperties()`，`app` / `network` 改用它 |

### C 档：需业务拍板（含业务逻辑调整）

| 项 | 净减 | 代价 |
|---|---|---|
| **删 `feature:demo` 整体** | **~172 KB / 30 文件** + 1 个 Navigator + 1 条规则豁免 | 失去组件演示画廊与农历日历（`LunarUtils` 单文件 27.5 KB）。demo 一个模块 ≈ home + user 之和的 1.3 倍，也是 CameraX / MLKit 被打进 dex 的根因 |
| ❌ **合并 `MyCollect` / `MyShare`**（不做） | — | 重复已很薄（均委托 `PagingArticleListPage`），完整合并需引入 `@AssistedInject` 参数化 ViewModel，净减仅 ~18 行，抵不上机制成本 |
| **`feature:*:api` 合入 impl** | **-8 模块 -8 build 文件 -8 行 include** | 失去「未来跨 feature 复用」的物理位置；但当前规则已禁止跨域，且 8 个 api 零外部消费者 |
| ✅ **`NavFlowScope` 三件套降级为 picture 内部状态**（已落地） | -3 类型 -1 CompositionLocal -22 行组合根 | 只有 picture 一个实现 |
| **`core:webview` 自建资源缓存退化为系统缓存** | ~45 KB | 失去 HTML 落盘、mtime 淘汰、DNS 预取、WebView 池化 |
| ✅ **`app` 单 flavor 单 dimension 删除**（已落地） | 变体减半，任务名回 `testDebugUnitTest` | 仅当无付费版计划 |

---

## 四、建议执行顺序

1. **A 档**（纯删除，无回退风险）
2. **B 档**按收益排：`mapper 下沉 > NavModule 生成 > 零逻辑链合并 > UiState 统一 > 其余`
3. **C 档**逐项拍板

单点收益最大且成本最低的是 **mapper 下沉**：改 1 行白名单 + 删 4 份重复 + 删 57 行补偿测试，同时消除「改一处要改四处」的隐性规则。

---

## 五、待拍板项

1. **C 档做哪几项？** 尤其 `feature:demo` 删不删——它是代码量与构建耗时（CameraX / MLKit 进 dex）的双重最大项，但也是唯一的组件演示入口。
2. **「三个同义词」是否统一？** 建议统一为「契约」：`feature:*:api` → `*:contract`，与 `core:data-contract`、`core:navigation-contract` 对齐。纯改名 + 文档成本，能显著减少新人的概念翻译开销。

---

## 六、已解决项（本次评审中发现的结构性改进）

`core:image-editor` 拆分后（1801 行 / 10 文件，`ImageEditorEngine`、`ImageSaveCoordinator`、`components/Picture*View`、`layer/*`）：

- `feature:picture:impl` 的依赖由 `:core:data-repository` 改为 `:core:image-editor`
- `BitmapImageHandle` 那条 feature→data 的越权边**已不存在**
- `FEATURE_EXTRA_CORE_DEPENDENCIES` 因此归零

即此前评审中标记的「feature→data 实现层唯一越权缺口」已通过**业务能力下沉到独立 core 模块**的方式真正解决，而不是靠例外放行。这是本轮值得记录的正向变化。

**4 份 mapper 下沉 `core:ui`**（评审后已落地）——问题 #3 的 4 份逐字相同 `ArticleFeedMapper` 已合并为 `core:ui` 下的一份共享 mapper，并删除 4 份 feature 副本与 57 行的 `ArticleFeedMapperConsistencyTest` 补偿测试：

- 依赖矩阵 `CORE_ALLOWED_DEPENDENCIES` 中 `:core:ui` 由 `setOf(":core:designsystem")` 扩为 `setOf(":core:designsystem", ":core:model")`，并在 `core/ui/build.gradle.kts` 声明 `implementation(project(":core:model"))`
- 6 处调用点 import 由 `feature.*.mapper.toFeedCardUIState` 改为 `core.ui.mapper.toFeedCardUIState`
- 这是**规则修正而非例外放行**：`core:model` 为零依赖叶子，`data-repository/network/database/domain/data-contract` 均已依赖它，唯独 `core:ui` 此前被排除；`ui → data-repository/network/database` 的禁令仍完好
- 验证：`verifyModuleDependencies` 与 `:core:ui:compileDebugKotlin` 均 BUILD SUCCESSFUL

**A 档纯删除已落地**——评审中标记的 4 项零风险删除已全部完成：

- 约定插件去掉 M2 `material`（删 1 行依赖 + toml 1 别名）
- 删 `libs.androidx.room3.paging`（`core/database` 1 行 + toml 1 别名）
- 删 `FEATURE_EXTRA_CORE_DEPENDENCIES`（变量定义 + 15 行注释 + 1 处使用点）
- 删 `Vector2D`、`Error.code` ×4、`ProjectTreeUiState.isLoading`

其中 `Error.code` ×4 涉及 `HeaderError` / `NavUiState.Error` / `MainUiState.Error` / `ProjectTreeUiState.Error` 四个 Error 类型的 `code` 字段删除，5 处赋值点同步改为仅传 `message`；grep 已确认这些 `code` 字段无任何读取方（全部为赋值侧，读取值源自 `DomainResult`），lint 无错误。

**B 档「零逻辑链合并」+「config.properties 归一」已落地**：

- `config.properties` 三处重复加载归一：`ProjectExtensions` 新增公共扩展 `Project.loadConfigProperties()`，`app` / `network` 的 build 脚本改用它，消除 2 段重复的 `Properties().load(FileInputStream(...))`。
- 3 条零逻辑链（History / Schedule / Theme）各删 1 个接口：
  - 删 `core:data-contract` 的 `HistoryLocalDataSource` / `ScheduleLocalDataSource` / `ThemeLocalDataSource` 3 个契约接口；
  - 删 `core:data-repository` 的 `HistoryRepositoryImpl` / `ScheduleRepositoryImpl` / `ThemeRepositoryImpl` 3 个纯透传实现；
  - `core:database` 的 `XxxLocalDataSourceImpl` 改为直接实现 domain 的 `XxxRepository` 端口；
  - `LocalDataSourceModule` 的 3 条 `@Binds` 由「绑定到 LocalDataSource 契约」改为「绑定到 domain 端口」，`RepositoryModule` 删掉对应 3 条 `@Binds`；
  - **架构代价（文档表格未写明）**：`core:database` 新增 `core:domain` 依赖（`CORE_ALLOWED_DEPENDENCIES` + build 脚本同步）。数据实现直接依赖领域抽象，方向正确，但打破了「database 只依赖 data-contract + model」的原有隔离，需留意这一依赖方向变化。

> 注：本轮改动涉及约定插件、依赖声明与 4 个 ViewModel，编译验证因系统负载未执行；待环境空闲后再跑 `verifyModuleDependencies` 与 `:feature:home:impl:compileDebugKotlin` 兜底。

**B 档「导航心智模型统一」已落地**——问题 #5 的「两套导航注入心智模型」已统一到 `LocalNavigators` 单一模式：

- 删除 `HomeNavActions` data class 与 `homeNavActions()` 转换函数（整个 `HomeNavigationMapper.kt`）
- home 的 4 个 Screen（`HomeScreen` / `ProjectScreen` / `SystemScreen` / `NavScreen`）删除 `actions` 参数，改为函数内读取 `LocalNavigators.current`，用 `remember(navigators)` 缓存 `article::openArticle` / `user::openUserProfile` / `home::openSystemTree` 三个方法引用
- `MainScreen` 删除 `articleNavigator` / `homeNavigator` 局部变量、`homeActions` 构造块及全链路传递（5 处函数签名 + 3 处位置参数 + 5 处命名参数）
- `HomeNavRegistration` 的 `SystemScreen` 调用去掉 `homeNavActions(...)` 包装
- `MyScreen` 的 `MyNavActions` 亦一并统一：删除 `MyNavActions` data class 与 `myNavActions()` 转换函数（整个 `MyNavigationMapper.kt`），`MyScreen` 删除 `actions` 参数，函数内读取 `LocalNavigators.current`，用 `remember(navigators)` 缓存 `user::openUserProfile` / `auth::openLogin` / `demo::openDemo` / `user::openMyCoin` / `collection::openMyCollect` / `collection::openMyShare` / `user::openBrowseHistory` / `user::openSetting` 八个方法引用；`MainScreen` 删除 `myActions` 构造块及全链路传递

**C 档「`app` 单 flavor 单 dimension 删除」已落地**——确认无付费版计划后执行：

- 删除 `app/build.gradle.kts` 的 `flavorDimensions += "tier"` 与 `productFlavors { create("free") { ... } }` 整块（单 flavor 单 dimension 无存在必要）
- `manifestPlaceholders`（`app_channel_value` / `app_name_value`）由 free flavor 收敛到 `defaultConfig`，manifest 两处 `${...}` 占位符解析不变
- 原 free flavor 的 `applicationIdSuffix = ".free"` 一并删除，applicationId 回到 `com.example.fragment.project`（与 deep link 的 `android:host` 一致）
- 任务名回退：`compileFreeDebugKotlin` → `compileDebugKotlin`、`testFreeDebugUnitTest` → `testDebugUnitTest`，已同步更新 `dependencies-policy.md`（2 处）与 `ArchitectureTest.kt` 注释（1 处）

**C 档「`NavFlowScope` 三件套降级为 picture 内部状态」已落地**——确认只有 picture 一个流程后执行：

- 删除 `core:navigation-runtime` 的 `NavFlowScope` / `NavFlowScopeContributor` / `LocalNavFlowScopes` 三件套（`NavFlowScope.kt` 整个文件）
- 删除 `PictureFlowScopeContributor.kt`
- `PictureFlowState` 改 Hilt `@Singleton @Inject`，去掉 `: NavFlowScope` 实现与 `close()`，新增 `reset()` 归零方法（等价原「每次流程新建实例」语义）
- `PictureNavModule` 删除 `providePictureFlowScopeContributor` 注册
- `MainActivity` 删除 `flowScopeContributors` 注入与传参
- `AppNavGraph` 删除 `flowScopeContributors` 参数 + 22 行通用流程作用域代码 + `LocalNavFlowScopes provides`
- `PictureNavRegistration` 改为经 Hilt entryPoint（`PictureFlowStateEntryPoint`）取回 @Singleton，删除 `PictureFlowScopeUnavailable` 兜底（单例永非空）
- `PictureSelectorScreen` 进入流程时调用 `flowState.reset()` 归零跨流程残留状态
- `PictureFlowNavKey` 标记接口保留（app 组合根的选图结果清理仍依赖它）

**B 档「TitleBar 默认返回箭头」已落地**——评审标记的 6 处实际为 **7 处**，已全部收敛为 `TitleBar` 的默认返回箭头：

- `core:designsystem/TitleBar.kt` 新增 `onNavigateUp: (() -> Unit)? = null` 参数与私有 `DefaultBackIcon`：传 `onNavigateUp` 即渲染统一返回箭头（`contentDescription = "返回"`），否则沿用 `navigationIcon` 自定义槽位
- 7 处调用点（`WebScreen` / `RankScreen` / `SettingScreen` / `BrowseHistoryScreen` / `PagingArticleListPage` / `ShareArticleScreen` / `SystemScreen`）由 `navigationIcon = { IconButton + ArrowBack }` 改为 `onNavigateUp = onNavigateUp`，并清理随之失效的 `Icons` / `ArrowBack` / `Icon` / `IconButton` / `MaterialTheme` import
- 保留未合并：`DemoScreen` 的 `navigationIcon = { IconButton + Menu }`（抽屉菜单，非返回箭头）；`CollapsingHeader` 自带返回箭头（折叠头部，布局语义与 tint 上下文不同）；`AccountForm` / `Picture*` 的自定义返回（图片选择器白色 tint）
- 净减约 62 行：7 处调用点 -67 行 + import -24 行，`TitleBar` 新增默认实现 +29 行

**B 档「收藏交互样板 ×6 → 一个 Composable」已落地**——Home/Search/User/System/Project/PagingArticleListPage 六处重复的「overrides 收集 + onToggleClick 生成 + collectFailed Toast」三段样板收敛为 `core:ui` 的单个 Composable：

- `core:ui/components/CollectInteraction.kt` 新增 `CollectInteraction` 与 `rememberCollectInteraction(collectState)`：收集覆盖层、生成稳定 `onToggleClick`、统一收集失败 Toast，返回 `overrides` + `onToggleClick`
- 6 处调用点由约 9~10 行样板收敛为 `val collectInteraction = rememberCollectInteraction(xxx.collectState)`，并清理失效的 `Toast` / `LocalContext` / `LaunchedEffect` / `collectAsStateWithLifecycle` import
- **架构代价（文档表格未写明）**：`core:ui` 新增 `:core:domain` 依赖（引用 `CollectState` 契约），同步改 `CORE_ALLOWED_DEPENDENCIES` 白名单与 `core/ui/build.gradle.kts`（含新增 `lifecycle-runtime-compose`）。domain 为零 Android 依赖的纯契约层，方向正确（UI → 领域契约），无传递污染
- 净减约 19 行：6 处样板 -48 行 + import -10 行，新增封装文件 +39 行

**B 档「删 CoinRankDao / CoinRankEntity / CoinRankMapping」已落地**——积分排行榜（CoinRank）早已改为纯网络分页（`CoinRankRepositoryImpl` 仅注入 `CommonRemoteDataSource`），Room 缓存层成为「被提供但无人注入」的死代码，是与 article 表同构的演进残留：

- 删 `CoinRankDao.kt` / `CoinRankEntity.kt` / `CoinRankMapping.kt` 三文件（`CoinRankDao` 全仓无消费者，`Entity` / `Mapping` 仅服务该 DAO）
- `AppDatabase`：`entities` 删 `CoinRankEntity::class`、删 `coinRankDao()` 抽象方法，`version 13 → 14`，新增 `MIGRATION_13_14`（`DROP TABLE coin_rank`，历史 `MIGRATION_9_10` 建表保留以维持升级链）
- `DatabaseModule`：删 `provideCoinRankDao`
- 净减约 87 行 + 1 张 Room 表；`14.json` schema 由编译时 Room 自动导出（历史 8~13.json 快照保留）

**B 档「删 ShareArticleUseCase / LogoutUseCase」已落地**——二者均为「单 Repository 单步透传」（零编排），内联为 ViewModel 直接依赖 Repository 端口：

- 删 `core/domain/usecase/ShareArticleUseCase.kt`（仅 `repo.shareArticle` 一行透传）与 `LogoutUseCase.kt`（仅 `userRepo.logout()` 一行透传）
- `ShareArticleViewModel`：注入 `ShareArticleUseCase` 改为 `MyRepository`，`shareArticle(...)` 改为 `myRepository.shareArticle(...)`
- `SettingViewModel`：删除多余的 `LogoutUseCase` 注入（本已同时注入 `UserRepository`），`logoutUseCase()` 改为 `userRepo.logout()`
- 符合 `dependencies-policy.md` §5：「单 Repository 的单步查询/写入由 ViewModel 直接依赖 Repository 接口」；清理的是早于该约定的透传型 UseCase。`CollectArticleUseCase`（收藏/取消分支）与 `HomeHeader/MainHeaderAggregateUseCase`（跨 Repository 编排）保留

**B 档「UiState 统一（单变体降级）」已落地**——3 个单变体 sealed 降为 data class，其余不统一：

- `ShareArticleUiState` / `BrowseHistoryUiState` / `MyUiState` 三个 sealed interface 仅有一个变体，sealed 多态无意义；降为 data class 后移除 9 个 `(this as? X)?.y ?: default` 访问器与 `updateSuccessFrom`，改直接 `update { it.copy(...) }`
- Screen 零改动（访问器名与 data class 属性名一致）
- **`AsyncUiState<T>` / `ListUiState<T>` 统一不做**：9 个 UiState 的「4 种形状」实为 4 种业务语义——`Login/Register` 有 `Idle`（表单初始态）、`Main` 是「部分数据 + 独立错误」、`Setting` 无 `Error`（本地观察）、列表页均走 Paging3 无需自定义 `ListUiState`。仅 `Nav/ProjectTree` 是标准三态，为 2 个用例引入「通用」`AsyncUiState<T>` 属过早抽象
- 净减约 15 行

---

## 附：结论可信度说明

| 类别 | 核验方式 |
|---|---|
| 基线数字、8 个 api 消费者、4 份 mapper、M2 引用数、`FEATURE_EXTRA` 为空、`core:image-editor` 存在 | 本次评审**实测核验** |
| 零逻辑链、死代码、样板份数、目录/命名不一致 | 全量扫描结论，**未逐条复核**，实施前需再确认 |

`docs/navigation.md` 已存在（导航上手文档），本次评审未改动。
