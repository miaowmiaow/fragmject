# 依赖策略（版本选型 + 模块依赖治理）

> 本文件记录 fragmject 的两类约定：
> 1. **依赖版本选型与升级**（§1–§4），避免「版本冒进」演变为不可控的技术债；
> 2. **模块依赖治理**（§6），约定「何时可以新增模块」「新能力落点在哪」，避免模块数量随业务迭代持续膨胀。

## 1. 当前策略

项目刻意采用**前沿版本**（Bleeding-edge），以第一时间跟进 Android / Kotlin / Compose 生态的新 API：

| 依赖 | 现状 | 说明 |
|---|---|---|
| AGP | 9.4.1 | 高于正式版，使用 Canary/RC 分支 |
| Kotlin | 2.4.20 | 跟随官方最新 |
| KSP | 2.3.6 | 需与 Kotlin 版本对齐 |
| Room3 | 3.0.3 | `androidx.room3` 尚处 alpha，API 演进频繁 |
| Compose BOM | 2026.09.00 | 跟随月度 BOM |
| Media3 | 1.11.1 | 跟随官方发布 |

## 2. 升级原则

1. **KSP 与 Kotlin 必须联动**：KSP 版本与 Kotlin 编译器版本强绑定，升级时先对齐再发布。
2. **Room3 单独 review**：Room3 处于 alpha，每次升级必须人工 review 迁移 API（如 `suspend fun migrate(connection: SQLiteConnection)` 签名变化），并在 CI 中跑通 `schemas/` 的 migration 校验。
3. **AGP 升 Canary 需谨慎**：新 AGP 可能改变默认行为或引入 breaking change，升级后需全量构建验证。
4. **Compose BOM 月度跟进**：BOM 由 Google 统一管理子版本，仅需升级 BOM 版本，禁止单独 pin 子模块版本。

## 3. 自动化（Renovate）

- 配置文件：`.github/renovate.json`
- 默认策略：`rangeStrategy = bump`，每周一自动开 PR。
- 已分组：`Room3 (alpha)`、`AGP`、`Kotlin & KSP`、`Compose BOM`，便于分组 review 与回滚。

## 4. 升级 checklist

- [ ] `libs.versions.toml` 中版本号与注释同步更新
- [ ] `./gradlew build` 全量构建通过
- [ ] Room3 升级时，核对 `schemas/` 下各版本 json 是否存在、auto-migration 是否有兜底
- [ ] 升级后跑一遍 `:app:compileDebugKotlin` 确认 Hilt KSP 依赖图无误
- [ ] 若涉及 Compose 稳定性，同步更新 `compose_stability_config.conf` 或对应 `@Immutable` 注解

## 5. 架构分层约定（UseCase 边界）

领域层 `core:domain` 同时提供 Repository 接口与 UseCase，二者边界约定如下：

- **UseCase 承担领域校验、聚合或业务编排**：例如 `LoginUseCase` 校验输入，`HomeHeaderAggregateUseCase` 聚合同一 Repository 的 banner + 置顶文章；不以 Repository 数量判断是否需要 UseCase。
- **单 Repository 的单步查询/写入由 ViewModel 直接依赖 Repository 接口**：不再为「一个 UseCase 仅透传调用一个 Repository 方法」的形式性封装新建 UseCase，避免产生大量透传型 UseCase。
- 领域输入校验（如登录的空值/格式校验）归 UseCase；数据持久化副作用归 data 层 Adapter。

> 这是刻意为之的务实分层，不追求 UseCase 全覆盖。

- Repository 的结果类型应覆盖预期网络、业务和持久化失败；协程取消原样传播。分页业务错误必须进入 `LoadResult.Error`，不能伪装成空列表或末页。
- **业务码**：服务端 `errorCode` 的取值只在 `ApiCode`（data-contract）定义，成功与否只经 `HttpResponse.isSuccess` 判断；生产代码禁止对 `errorCode` 做字面量比较（`ArchitectureTest` 规则 18 守卫）。服务端「要求登录」(`-1001`) 只在 `toFailureCode()` 一处映射为领域码 `DomainResult.ERROR_UNAUTHENTICATED`，其余业务码原样透传。
- **结果类型选型**（新增端口按此选，不再新造）：带数据的查询/刷新用 `DomainResult`；无数据写操作（分享、收藏、下载、清缓存）用 `SimpleResult`；登录/注册用 `AuthResult`；媒体保存用 `MediaSaveResult`。网络调用统一经 `fetchAsDomainResult` / `fetchAsSimpleResult`，不在仓库里手写 try/catch；`core:data-repository` 不得使用 `runCatching`（会吞掉协程取消，`ArchitectureTest` 规则 19 守卫），也不得对外暴露 `kotlin.Result`。
- 「失败可降级为无数据」的读取（如用户积分）约定为：失败返回 `null`，由 UI 展示默认值；业务失败即使携带 data 也不视为有效数据。
- `core:domain` 源码不依赖 Android framework 类型，但当前仍使用 Android library 插件，并公开接受 `PagingData` 耦合；不等同于完全技术无关的领域核心。
- `core:model` 当前包含共享协议模型；仅在接口变化或业务约束需要时分离 DTO/领域模型/UI 模型，不机械增加三套映射。
- 平台相关 UI 引擎保留在对应 core 模块。WebView 对业务层只提供脚本文本、图片长按等语义能力，不通过公开容器回调暴露原生 WebView。
- 显式退出以本地用户与业务 Cookie 清理完成为成功；远端退出尽力执行。服务端 `-1001` 的全局失效、跨账户状态清理与 Cookie 过期检测尚未统一，不能把本地用户存在等同于服务端会话有效。
- 导航图绑定必须有所有权并在组合退出时释放。登录回跳与详情目标保留至 Activity 配置变更，不承诺进程死亡恢复。
- 登录态为三态（`AuthStatus`：未知/已登录/未登录）。守卫只在**确认未登录**时拦截；未知时暂存受保护目标，待状态确定后再放行或跳转登录。深层链接中的受保护目标同样经 `navigate` 守卫。
- 外部可控输入必须白名单校验：WebView 主动加载仅允许 http(s) 与应用内置 `file:///android_asset/`（`WebViewCommons.isLoadableUrl`），web 深链仅允许 http(s)。网页申请摄像头/麦克风仅限 https 来源，每次弹窗展示来源并由用户确认，且只授予受支持的资源。

## 6. 模块依赖治理（防膨胀）

### 6.1 核心立场

**模块是「边界的强制手段」，不是「分类工具」。**

当某个约束无法用包名/包结构强制、必须在 Gradle 层可执行时，才值得新增模块；仅为「把同一类技术代码放一起」而拆模块，会让 `core` 持续膨胀，且每次新增能力都要重新争论归属。

因此本项目的默认动作是：**用「包」组织，用「契约」隔离，最后才考虑「模块」。**

### 6.2 新增模块门槛（必须满足其一）

| 条件 | 说明 |
|---|---|
| ① 存在**领域边界** | 不同业务上下文、不同变更频率、不同负责人 |
| ② 需要 **Gradle 层可执行的约束** | 如「feature 不得依赖数据实现」，靠包名无法强制 |
| ③ 需要**物理隔离** | 独立发布 / 独立编译 / 动态交付 |

三条都不满足时，**用包组织**（如 `data-repository/media/`、`data-repository/system/`），并靠 §6.5 的既有规则约束方向。

> 反例（本项目已否决）：为「把平台工具类按 UI/数据/基础设施分类」而把 `core:android-platform` 拆成 3 个新模块。该诉求的真实原因是若干业务能力缺契约，而非分类不当。

### 6.3 新能力落点决策树

新增一个能力（尤其是需要 Android 平台 API 的能力）时，按顺序判断：

```
需要 Android platform API / 第三方 SDK API？
├─ 否 → 直接落在领域层（core:domain）或 UI 层（core:ui / feature impl）
└─ 是 → 谁消费？
   ├─ 仅 core 内部（data-repository / network / webview / player）
   │     → 实现放 core:android-platform，不建契约
   ├─ 被 feature 消费（绝大多数情况）
   │     → 契约（接口 + 模型）放 core:domain
   │       实现放 core:data-repository（或其对应的数据实现模块）
   │       feature 只依赖契约 ★
   └─ 想让 feature 直接调用平台类
         → 禁止。说明缺契约，回到上一条
```

★ **这是本项目的关键约束**：`feature` 不直接依赖 `core:android-platform`。它由 Gradle 守卫白名单 + Konsist 规则 12 双重强制（见 §6.5）。

### 6.4 契约归属：`core:domain`，而非 `feature:*:api`

经决策，本项目**统一将业务能力契约放 `core:domain`**，而非各 feature 自己的 `api` 模块：

| 模块 | 承载内容 |
|---|---|
| `feature:*:api` | **仅对外契约**：`NavKey` 路由键、跨 feature 语义 Navigator |
| `core:domain` | **业务能力端口**：Repository 接口、UseCase、领域结果模型、平台能力契约 |
| `core:data-repository` | 上述端口的实现（Hilt `@Binds` 绑定） |
| `core:database` | **例外**：直接实现 `HistoryRepository` / `ScheduleRepository` / `ThemeRepository` 三个 domain 端口（零逻辑链合并，见 §6.6），其余通过 data-contract 本地端口间接实现 |

理由：

1. 若允许 feature 私有端口放进自己的 `api`，该模块会同时承载「对外路由契约」与「给自己用的内部端口」两种语义，后续无法判断某个端口能否被别的 feature 引用；
2. 「单消费者端口是否该上移」是一个会反复出现的判断题，统一放 `core:domain` 可消除这类判断；
3. `core:domain` 的膨胀是可预期的、可拆分的（见 §6.6），而语义混杂的 `api` 模块难以拆分。

**已落地的例子**：

- `SystemStorage`（`core/domain/system/`）— 缓存目录与清理，实现 `SystemStorageImpl` 在 `core/data-repository/system/`

> 注：原先的 `MediaEditor` / `ImageHandle` / `ImageSource` 端口已随「图片编辑引擎化」重构为 `core:image-editor` 引擎——画布、裁剪、编码、落盘整体收敛进引擎，不再以领域端口形式暴露。这类「平台相关 UI 能力」应走 core 引擎而非 domain 端口，详见 §6.10。

### 6.5 现有约束清单（CI 可执行）

**A. Gradle 依赖守卫** — `build-logic/convention/.../FragmjectAndroidDependencyGuardPlugin.kt`，任务 `./gradlew verifyModuleDependencies`

| 规则 | 内容 |
|---|---|
| 环检测 | 模块依赖图必须是 DAG |
| app 聚合 | 所有 `feature:*:impl` 必须被 `:app` 依赖 |
| app 白名单 | `APP_ALLOWED_DEPENDENCIES` 正向声明 |
| R4/R7 | `core` 不得反向依赖 `feature`；core/feature 不得依赖 `:app` |
| R1/R5 | `domain` 不依赖 data/network/database；`data-repository` 不依赖 network/database |
| R2/R3/R6 | feature impl 不依赖其他 feature 的 impl/api；feature api 不依赖 feature impl |
| R8 | feature → core 必须在 `FEATURE_ALLOWED_CORE_DEPENDENCIES` 内 |
| R9 | `feature:*:api` → core 走 `FEATURE_API_ALLOWED_CORE_DEPENDENCIES` 正向声明（当前仅 `:core:navigation-contract`）；`feature:*:impl` → core 走 `FEATURE_ALLOWED_CORE_DEPENDENCIES`（更宽）。契约层不得引入 domain/ui/runtime 等机制模块 |
| R10 | `feature:*:api` 不得依赖其他 `feature:*:api`（契约层禁横向边；补 Gradle R2/R3 与 Konsist 规则 3 的交叉盲区） |
| core→core 矩阵 | `CORE_ALLOWED_DEPENDENCIES` 正向声明；`core:ui` 三条边属登记例外，见 §6.6 |

**B. 源码与 API 回归规则** — `app/src/test/.../ArchitectureTest.kt`，含 Konsist import 检查、公开 WebView 容器参数检查与选择器守卫
运行：`./gradlew :app:testDebugUnitTest --tests "*ArchitectureTest"`

规则 1–7（分层方向）、8–11（Android framework 白名单/黑名单）、**12（feature 不得依赖数据平台与数据实现）**、**13（`android.webkit.*` 仅限 `core:webview`）**、**14（feature 内必须使用 `hiltViewModel()`）**、**15（`CookieStore` 实现仅限 `core:webview`）**、16（feature api 仅依赖导航契约）、17（database 领域端口例外白名单）、**18/19（业务成功码与 `runCatching`）**、**20（导航契约完整接入：方法禁止默认实现 + `Navigators` 字段/`noop()`/app 实现必须齐全）**、**21（`:app` 不得引用数据实现层，白名单登记制）**、**22（`core:designsystem` 不得 import 任何项目内模块，只放颜色/间距/主题）**、**23（`core:ui` 不得下探 data/network/database）**，以及公开 WebView 容器参数回归检查与 `boundary selectors match real files`（防止选择器写错导致规则空转）。

> Gradle 守卫与 Konsist 互补：`implementation(project(...))` 不产生 import 时 Konsist 感知不到；反之 Gradle 看不到源码内的越界引用。二者必须同时维护。

### 6.6 例外管理

例外不是「开了就不管」，必须**限定到具体模块 + 写明理由 + 同步两侧配置**：

| 例外 | 位置 | 理由 |
|---|---|---|
| Konsist 规则 13 放行 `feature/demo/impl` | 规则内 `filterNot` | demo 是平台 API 演示模块，业务 feature 不得效仿 |
| `:app` 组合根使用 lifecycle 的 `viewModel()` | 规则 14 仅过滤 `/feature/` | `AppNavViewModel` 在 `NavDisplay` 之外，本就应是 Activity 作用域 |
| 大屏 DetailPane 用 `ProvideDetailPaneViewModelStore` 自建 owner | `core:navigation-runtime` | DetailPane 不在 `NavEntry` 内，拿不到 entry 级 store，需按 key 提供等价作用域 |
| `CookieStore` 实现放 `core:webview`，由 `:app` 聚合注入 | Gradle 守卫矩阵 + Konsist 规则 15 | network 不得依赖 webview；实现必须经 Hilt 绑定，network 只允许注入使用 |
| `core:database` 直接实现 `History/Schedule/Theme/OtpAccount` 四个 domain 端口 | `LocalDataSourceModule` + 守卫矩阵注释 | 四条链均无中间透传层（domain 端口与 KV/Room 适配器签名一致）。`OtpAccount` 的额外理由：密钥须在落库前用 Keystore 加密，而 `data-repository` 不得依赖 `database`（R5），**加密与 DAO 无法同处一层**，故实现必须落在 database。**禁止继续扩散**：除上述四条外，其他 domain 端口的实现仍必须落在 `core:data-repository` |
| 新增引擎模块 `core:qrscanner` | `settings.gradle.kts` + 守卫矩阵 | §6.2 门槛 + §6.10 引擎类别：CameraX / ML Kit 为第三方 SDK 需集中持有；扫码属平台相关 UI 能力，且需被 demo 与 Authenticator 复用（feature 之间不得互相依赖），必须有共享落点 |
| `core:ui` 依赖 `core:model` / `core:domain` | 守卫矩阵注释 + Konsist 规则 23 | `ArticleFeedMapper`（Article → FeedCardUIState）与 `CollectInteraction`（CollectState）需要这两个零 Android 依赖的契约层；此前仅依赖 designsystem 导致 4 个 feature 各复制一份映射。**限定条件**：只允许这三条边，业务文案与图标必须参数化可覆盖（`ArticleFeedLabels` / `FeedCard` 的 `toggleCheckedIconRes`），且不得下探 data/network/database（规则 23） |
| `:app` 的 `FragmjectApplication` import `core.network.di.ImageOkHttpClient` | 规则 21 白名单 `APP_DATA_IMPORT_ALLOWLIST` | Coil 需要「图片专用」OkHttpClient 的 qualifier。彻底消除需把 ImageLoader 装配下沉到同时可见 coil 与 network 的 core 模块，属独立重构 |

新增例外时同步改 Gradle 矩阵与 Konsist 规则，并在本表登记；**任何例外都应能回答「为什么不用契约」。**

### 6.7 `core:designsystem` 与 `core:ui` 的边界

| | `core:designsystem` | `core:ui` |
|---|---|---|
| 定位 | 设计令牌 + 主题 | 通用 Compose 组件（含业务 UI 映射，登记例外） |
| 允许内容 | `AppColors` / `AppSpacing` / `AppTheme` / `LocalWindowSizeClass` | `components/` / `mapper/` / `utils/` |
| 禁止 | import 任何项目内模块；出现 Composable 组件 | 下探 `data-repository` / `network` / `database` |
| 允许依赖 | 仅第三方（Compose / Material3） | `designsystem` + 例外 `model` / `domain` |

约定：

- **新增颜色/间距/圆角一律进 `AppColors` / `AppSpacing`**，组件内不许再出现 `Color(0xFF...)`；
  已收敛的骨架屏色、下拉刷新底色与提示文字色见 `AppColors.skeleton* / refresh*`。
- **组件一律进 `core:ui`**（`TitleBar` / `BackIcon` / `BottomNavItem` / `NightSwitchButton` 已从
  designsystem 迁入）。
- **业务语义必须参数化**：文案走 `ArticleFeedLabels`，图标走 `FeedCard` 的
  `toggleCheckedIconRes` / `toggleUncheckedIconRes`；默认值只为兼容现有调用方，不代表固定语义。
- **业务资源不进 core:ui**：`ic_web_*` / `ic_bottom_bar_*` / `ic_browser` / `ic_rank` 等已下放到
  各自 feature；core:ui 只保留组件自用的 `empty` / `ic_right` / `refreshing_*` / `avatar_*`，
  以及作为 FeedCard 默认值的中性 `ic_toggle_*`。

### 6.8 `core:domain` 的拆分触发条件

`core:domain` 是当前唯一承载全部领域契约的模块，业务增长时它才是真正会膨胀的地方（当前 34 个 main kt 文件）。

**现在不拆**，用 `repository/` `usecase/` `media/` `system/` `result/` 等包组织即可。文件数量只作为评审信号，不作为自动拆分阈值。考虑拆分时，应有以下证据：

- 明显独立的业务上下文具有不同负责人、变更频率或发布需求；
- 无关业务变更持续造成跨上下文修改、编译或测试影响；
- 新模块能建立明确且可执行的依赖边界，而不是仅增加分类目录。

满足 §6.2 门槛后，可按限界上下文拆分：

```
core:domain → domain:content   （文章 / 收藏 / 搜索 / 积分）
              domain:identity  （登录注册 / 会话）
              domain:media     （媒体与相册 / 图片编辑）
              domain:system    （主题 / 缓存 / 存储）
```

### 6.9 `core:android-platform` 的定位

它是**零依赖的叶子模块**，承载「多方共用的 Android 平台原语」。消费方**只限 core 内部**
（`data-repository` / `network` / `webview` / `player`）；`app` 与 `feature` 一律经
`core:domain` 端口间接使用，不得直接依赖（守卫白名单未声明即失败）。

职责用**包**划分，不按技术类别再拆模块（否则违反 §6.2 门槛）：

| 包 | 内容 | 典型消费方 |
|---|---|---|
| `media/` | `MediaStoreUtils`、`MediaRow`、`BitmapCodec`、`BitmapDecoder`、`UriPathUtils` | data-repository |
| `file/` | `FileUtils` 门面及其 internal 实现（`FileIOUtils`/`FileSizeUtils`/`FileMimeUtils`） | network、data-repository |
| `cache/` | `CacheUtils`、`CacheDirs` | network、webview、player、data-repository |
| `digest/` | `DigestUtils` | webview |
| `app/` | `AppCoroutineScope`（进程级协程作用域，Hilt 单例） | data-repository、webview |

**跨模块的隐式契约必须集中定义**：缓存目录名由 `CacheDirs` 统一声明，
`core:player`（`EXOPLAYER`）、`core:network`（`OKHTTP`）、`core:webview`（`WEB`/`WEB_HTML`）、
`app`（`COIL`）与 `SystemStorage` 的清理排除表（`PROTECTED`）全部引用同一常量。
分散硬编码时，任一侧改名不报错但会**删掉正在使用的缓存**。

### 6.10 core 引擎模块（`webview` / `qrscanner` / `image-editor` / `player`）

这四个模块是一类**独立于「共享复用」逻辑之外的落点**，与 `core:ui` / `core:designsystem` 等「通用库」不同：

| 模块 | 持有的平台能力 / 第三方 SDK |
|---|---|
| `core:webview` | `android.webkit.*` 门面 + 池化 + CookieStore |
| `core:qrscanner` | CameraX / ML Kit |
| `core:image-editor` | 位图解码（`android-platform`）+ Canvas 自绘编辑 |
| `core:player` | media3 / ExoPlayer |

**准入条件**（满足其一，且需被 feature 直接调用；否则按 §6.3 走「domain 端口 + data 实现」路径）：

1. 需要 Android platform API 或第三方 SDK API，**且该能力以 UI 形态存在**（画布、相机、播放器等），无法用 domain 端口表达；
2. 该能力若做成 domain 端口，会导致端口签名里出现平台类型（历史上 `MediaEditor` / `ImageHandle` / `ImageSource` 三个端口正是因此被 `core:image-editor` 取代，见 §6.4 注）。

**明确排除一条误判**：**不得以「当前只有 1 个消费者」为由把引擎模块下沉回 feature。**
引擎化的目的是消灭平台类型泄漏与 domain 端口污染，不是共享复用；消费者数量是结果，不是判据。
（`core:image-editor` 当前仅 `feature/picture/impl` 消费、`core:player` 仅 `feature/demo/impl` 消费，均属预期。）

约束：

- 引擎可依赖 `core:domain`（如 `image-editor` 用 `MediaRepository` 落盘）与 `core:android-platform`，但**不得反向依赖 feature**；
- feature 依赖引擎必须走 `FEATURE_ALLOWED_CORE_DEPENDENCIES` 白名单（已含上述四个）；
- 引擎内部的业务语义（如图片保存结果）只表达「引擎输出」，不含具体业务规则。

### 6.11 变更 checklist

新增能力或模块时逐项确认：

- [ ] 是否真的需要新模块？（对照 §6.2 三条门槛；平台 UI 能力另见 §6.10 引擎类别）
- [ ] 若 feature 需要该能力：契约是否在 `core:domain`、实现是否在 `core:data-repository`？
- [ ] 是否引入了 `feature → core:android-platform` 边？（应为否）
- [ ] 新增的 `android.*` 导入是否落在规则 9/10/11 的白名单内？
- [ ] Gradle 矩阵与 Konsist 规则是否同步更新？
- [ ] 若新增例外：是否在 §6.6 登记并写明理由？
- [ ] `./gradlew verifyModuleDependencies` 与 `--tests "*ArchitectureTest"` 均通过
- [ ] **反向验证**：故意构造一处违规，确认对应规则确实 fail（证明非空转）
