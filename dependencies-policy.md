# 依赖策略（版本选型 + 模块依赖治理）

> 本文件记录 fragmject 的两类约定：
> 1. **依赖版本选型与升级**（§1–§4），避免「版本冒进」演变为不可控的技术债；
> 2. **模块依赖治理**（§6），约定「何时可以新增模块」「新能力落点在哪」，避免模块数量随业务迭代持续膨胀。

## 1. 当前策略

项目刻意采用**前沿版本**（Bleeding-edge），以第一时间跟进 Android / Kotlin / Compose 生态的新 API：

| 依赖 | 现状 | 说明 |
|---|---|---|
| AGP | 9.4.0 | 高于正式版，使用 Canary/RC 分支 |
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
- [ ] 升级后跑一遍 `:app:compileFreeDebugKotlin` 确认 Hilt KSP 依赖图无误
- [ ] 若涉及 Compose 稳定性，同步更新 `compose_stability_config.conf` 或对应 `@Immutable` 注解

## 5. 架构分层约定（UseCase 边界）

领域层 `core:domain` 同时提供 Repository 接口与 UseCase，二者边界约定如下：

- **UseCase 仅用于「跨多个 Repository 的编排」**：当一个操作需要聚合多个领域端口（例如 `HomeHeaderAggregateUseCase` 聚合 banner + 置顶文章）时，才新增 UseCase。
- **单 Repository 的单步查询/写入由 ViewModel 直接依赖 Repository 接口**：不再为「一个 UseCase 仅透传调用一个 Repository 方法」的形式性封装新建 UseCase，避免产生大量透传型 UseCase。
- 领域输入校验（如登录的空值/格式校验）归 UseCase；数据持久化副作用归 data 层 Adapter。

> 这是刻意为之的务实分层，不追求 UseCase 全覆盖；现有代码保持现状，新增代码遵循上述边界即可。

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

理由：

1. 若允许 feature 私有端口放进自己的 `api`，该模块会同时承载「对外路由契约」与「给自己用的内部端口」两种语义，后续无法判断某个端口能否被别的 feature 引用；
2. 「单消费者端口是否该上移」是一个会反复出现的判断题，统一放 `core:domain` 可消除这类判断；
3. `core:domain` 的膨胀是可预期的、可拆分的（见 §6.6），而语义混杂的 `api` 模块难以拆分。

**已落地的两个例子**：

- `MediaEditor` / `ImageHandle` / `ImageSource`（`core/domain/media/`）— 位图解码与编辑导出，实现 `MediaEditorImpl` 在 `core/data-repository/media/`
- `SystemStorage`（`core/domain/system/`）— 缓存目录与清理，实现 `SystemStorageImpl` 在 `core/data-repository/system/`

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

**B. Konsist 源码规则** — `app/src/test/.../ArchitectureTest.kt`，16 条 + 1 条守卫测试
运行：`./gradlew :app:testFreeDebugUnitTest --tests "*ArchitectureTest"`

规则 1–7（分层方向）、8–11（Android framework 白名单/黑名单）、**12（feature 不得依赖数据平台与数据实现）**、**13（`android.webkit.*` 仅限 `core:webview`）**、**14（feature 内必须使用 `hiltViewModel()`）**、**15（`CookieStore` 实现仅限 `core:webview`）**、**16（`core:data-repository` 的 `media` 包对外只暴露 `BitmapImageHandle`）**，以及 `boundary selectors match real files`（防止选择器写错导致规则空转）。

> Gradle 守卫与 Konsist 互补：`implementation(project(...))` 不产生 import 时 Konsist 感知不到；反之 Gradle 看不到源码内的越界引用。二者必须同时维护。

### 6.6 例外管理

例外不是「开了就不管」，必须**限定到具体模块 + 写明理由 + 同步两侧配置**：

| 例外 | 位置 | 理由 |
|---|---|---|
| `:feature:picture:impl` → `:core:data-repository` | `FEATURE_EXTRA_CORE_DEPENDENCIES` | 画布持有真实 `android.graphics.Bitmap`，需 `BitmapImageHandle` 包装后才能交给 `MediaEditor` |
| Konsist 规则 12 放行 `core.data.repository.media.BitmapImageHandle` | `DATA_EXCEPTION_IMPORT` | 同上 |
| Konsist 规则 13 放行 `feature/demo/impl` | 规则内 `filterNot` | demo 是平台 API 演示模块，业务 feature 不得效仿 |
| Konsist 规则 9 白名单 `android.graphics.Bitmap` / `android.net.Uri` | 规则内 `setOf(...)` | 限 `media` 子包内使用 |
| `:app` 组合根使用 lifecycle 的 `viewModel()` | 规则 14 仅过滤 `/feature/` | `AppNavViewModel` 在 `NavDisplay` 之外，本就应是 Activity 作用域 |
| 大屏 DetailPane 用 `ProvideDetailPaneViewModelStore` 自建 owner | `core:navigation-runtime` | DetailPane 不在 `NavEntry` 内，拿不到 entry 级 store，需按 key 提供等价作用域 |
| `CookieStore` 实现放 `core:webview`，由 `:app` 聚合注入 | Gradle 守卫矩阵 + Konsist 规则 15 | network 不得依赖 webview；实现必须经 Hilt 绑定，network 只允许注入使用 |
| `:feature:picture:impl` → `:core:data-repository`（仅 `BitmapImageHandle`） | `FEATURE_EXTRA_CORE_DEPENDENCIES` + 规则 12 + **规则 16** | 画布持有真实 `Bitmap`，必须构造/拆包领域句柄；上移到 domain 会把 `Bitmap.compress` 平台行为带进领域层。三重锁定防扩散 |

新增例外时同步改 Gradle 矩阵与 Konsist 规则，并在本表登记；**任何例外都应能回答「为什么不用契约」。**

### 6.7 `core:domain` 的拆分触发条件

`core:domain` 是当前唯一承载全部领域契约的模块，业务增长时它才是真正会膨胀的地方（当前 34 个 main kt 文件）。

**现在不拆**，用 `repository/` `usecase/` `media/` `system/` `result/` 等包组织即可。触发拆分的阈值：

- `core/domain/src/main` 文件数 **> 40**；或
- 出现 **2 个以上**明显独立的业务上下文同时高频变更。

届时按限界上下文拆分（该方案同样需满足 §6.2 门槛）：

```
core:domain → domain:content   （文章 / 收藏 / 搜索 / 积分）
              domain:identity  （登录注册 / 会话）
              domain:media     （媒体与相册 / 图片编辑）
              domain:system    （主题 / 缓存 / 存储）
```

### 6.8 `core:android-platform` 的定位

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

### 6.9 变更 checklist

新增能力或模块时逐项确认：

- [ ] 是否真的需要新模块？（对照 §6.2 三条门槛）
- [ ] 若 feature 需要该能力：契约是否在 `core:domain`、实现是否在 `core:data-repository`？
- [ ] 是否引入了 `feature → core:android-platform` 边？（应为否）
- [ ] 新增的 `android.*` 导入是否落在规则 9/10/11 的白名单内？
- [ ] Gradle 矩阵与 Konsist 规则是否同步更新？
- [ ] 若新增例外：是否在 §6.6 登记并写明理由？
- [ ] `./gradlew verifyModuleDependencies` 与 `--tests "*ArchitectureTest"` 均通过
- [ ] **反向验证**：故意构造一处违规，确认对应规则确实 fail（证明非空转）
