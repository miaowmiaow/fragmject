# 分层架构指南

本文档是「**新增代码该放哪里**」的第一入口：模块分层、各层职责、依赖方向、feature 内部目录约定，以及机器可执行的约束清单。

导航（NavKey / Navigator 契约）的专门说明见 [navigation.md](./navigation.md)。
依赖策略与例外管理的完整论述见 [`dependencies-policy.md`](./dependencies-policy.md)。

## 一、全景：一个组合根 + 三层 core + 两层 feature

```
        ┌──────────────────────────────────────────┐
        │  :app   组合根：Hilt 聚合 + 导航映射       │
        └───────────────┬──────────────────────────┘
                        │ 依赖所有 feature:impl
        ┌───────────────┴──────────────────────────┐
        │  feature:{name}:api   对外契约（仅 NavKey）│  ← 跨域跳转只经它
        │  feature:{name}:impl  页面 / ViewModel     │
        └───────────────┬──────────────────────────┘
                        │ 依赖 core（白名单内）
        ┌───────────────┴──────────────────────────┐
        │  core:*   共享能力（按职责再分层，见下表）  │
        └───────────────┬──────────────────────────┘
                        │
        ┌───────────────┴──────────────────────────┐
        │  core:android-platform + 第三方 SDK        │  ← 仅 core 内部可见
        └──────────────────────────────────────────┘
```

### core 模块按职责分组（共 16 个）

| 类别 | 模块 | 职责 | 可依赖的 core |
|---|---|---|---|
| **数据模型** | `core:model` | 跨模块共享的纯数据类（零依赖叶子） | — |
| **领域契约** | `core:domain` | Repository 端口、UseCase、领域结果模型、平台能力契约 | model |
| **数据契约** | `core:data-contract` | **适配器侧**端口（Remote/Local DataSource）+ HTTP 响应模型 | model |
| **数据实现** | `core:data-repository` | 领域端口的实现（Hilt `@Binds`） | domain / data-contract / android-platform / model |
| | `core:database` | KV + Room；**登记例外**：直接实现 4 个 domain 端口 | domain / data-contract / android-platform / model |
| | `core:network` | OkHttp + Retrofit；`CookieStore` 只注入不实现 | domain / data-contract / android-platform / model |
| **平台能力** | `core:android-platform` | Android 原语（零依赖叶子，**仅 core 内部消费**） | — |
| **引擎模块** | `core:webview` / `qrscanner` / `image-editor` | 平台 UI 能力（详见 §四） | 见矩阵 |
| **导航** | `core:navigation-contract` | NavKey 标记、语义 Navigator 接口（纯契约） | — |
| | `core:navigation-runtime` | 注册机制、DetailPane 渲染、ViewModelStore | — |
| **设计系统** | `core:designsystem` | 颜色 / 间距 / 主题（**不含组件**） | — |
| **通用 UI** | `core:ui` | 通用 Compose 组件（含业务 UI 映射，登记例外） | designsystem / model / domain |
| **全局装饰层** | `core:pet` | 漂浮宠物（跨全部页面的绘制层，**仅组合根挂载，feature 不得使用**） | designsystem |
| **测试工具** | `core:testing` | 共享测试 fake 与 JUnit 规则（仅 `testImplementation` 消费，不进生产包） | domain（fake 需要端口类型） |

> `core:ui` 的依赖边一共三条：`designsystem` 属**常规边**，`model` / `domain` 两条属**登记例外**（`CORE_EXCEPTION_EDGES`，见 `dependencies-policy.md` §6.6）——`ArticleFeedMapper`、`CollectInteraction` 需要这两个零 Android 依赖的契约层（此前仅依赖 designsystem 时，4 个 feature 各复制了一份映射）。业务文案/图标必须参数化可覆盖，且不得下探 data/network/database（规则 23）。
>
> 公开签名白名单：`core:ui` 对外暴露的类型只允许是 `Article`/`Banner` 等 model、`*State` 与 `DomainResult` 一类结果类型；**不得**把 `repository` / `usecase` 类型带进组件签名（新增导出类型需在此登记）。`core:pet` 的公开类型登记见 §四。
>
> **模型分层已定案**：`core:model` 是共享协议模型唯一落点，不拆 DTO / 领域模型 / UI 模型（UI 侧只保留 `FeedCardUIState` 这一层）。依据、被否决方案与重启触发条件见 [ADR-0001](./adr/0001-shared-protocol-model.md)；守卫为 `ArchitectureTest` 规则 29，评审不再重复论证。

### 认证链路的分层落点（2026-10 调整）

原先 `UserRepositoryImpl` 一处 165 行，同时承担 transport、Cookie 快照/回滚、落库与登出策略，
业务编排埋在数据层、只能带 Room + CookieStore 一起测。现拆成三段，每段只做一件事：

| 角色 | 落点 | 职责 |
|---|---|---|
| `AuthGateway` | `core:domain` 端口 / `core:data-repository` 的 `AuthGatewayImpl` | 发请求 + 把 `DataResponse` 翻译成 `AuthExchange`；不写库、不清 Cookie、不持锁 |
| `AuthSessionCoordinator` | `core:domain/usecase` | Cookie 快照/回滚、落库时机、登出异常优先级（会话不变量都在这里） |
| `UserRepository` | `core:domain` 端口 / `UserRepositoryImpl` | 仅 observe / save / clear 三个存储原子操作 |

判断新代码归属：出现「失败要回滚」「写完要清理」「先后顺序有讲究」→ 属于 `usecase`；
只是读写一个数据源 → 留在 Repository。

## 二、依赖方向（谁能依赖谁）

| 从 → 到 | 是否允许 | 强制方式 |
|---|---|---|
| `:app` → `feature:*:impl` / `core:*` | 白名单内允许（`APP_ALLOWED_DEPENDENCIES`） | Gradle R-聚合 |
| `:app` → `core:android-platform` | ❌ 禁止（平台能力须经 domain 端口） | 白名单未声明 |
| `:app` → `core:pet` | ✅ 允许（**登记例外**：全局装饰层「漂浮宠物」只有组合根能挂，feature 之间又禁止互相依赖） | `APP_ALLOWED_DEPENDENCIES` |
| `feature:*:impl` → `feature:*:impl` / 其他 `api` | ❌ 禁止 | Gradle R2/R3 + 规则 3/4 |
| `feature:*:api` → `feature:*:impl` / 其他 `api` | ❌ 禁止 | Gradle R6/R10 |
| `feature:*:api` → `core:*` | 仅 `core:navigation-contract` | Gradle R9 + 规则 16 |
| `feature:*:impl` → `core:*` | 白名单内（9 个，不含任何数据实现模块） | Gradle R8 |
| `feature:*` → `core:pet` | ❌ 禁止（宠物是全局装饰层，只能由组合根挂一次；白名单不含 `:core:pet`） | Gradle R8 |
| `core:*` → `feature:*` / `:app` | ❌ 禁止 | Gradle R4/R7 + 规则 5/8 |
| `core:domain` → 任何数据模块 | ❌ 禁止 | Gradle R1 + 规则 1 |
| `core:*` → `core:*` | 正向矩阵 `CORE_ALLOWED_DEPENDENCIES`，未声明即失败 | Gradle R-core |

**关键的禁止组合（源码层二次拦截）**：feature 不得 import 数据实现层 / `core:android-platform`（规则 12）；
`android.webkit.*` 仅限 `core:webview`（规则 13）；`:app` 不得引用数据实现层（规则 21，白名单登记制）。

## 三、feature 模块内部结构

```
feature/{name}/api/.../          只放 NavKey（@Serializable），无 Compose / 无 Hilt / 无工具类
feature/{name}/impl/.../
  ui/<功能>/    Screen + ViewModel（页面级，按功能分子目录）
  components/   本 feature 内**被多处复用**的组件
  model/        跨 ui/<功能>/ 与 components/ 共用的 UI 模型与状态
                （个别 feature 用 state/ 命名，二选一即可，不要两个都建）
  nav/          NavContentContributor 注册
  di/           Hilt 模块（@IntoSet 贡献导航注册等）
  assets/       本地 HTML / JS 等资源
  utils/        工具（**复数**；只允许本 feature 使用）
```

### 归属决策树

```
一个 Composable 放哪？
├─ 只有 1 处使用 → 就近放 ui/<功能>/（与使用者同目录）
└─ 被多处使用 → 被谁用？
   ├─ 只被 components/ 用     → components/
   ├─ 被 ui/<功能>/ 和 components/ 都用 → model/? 不：Composable 放 components/
   └─ 被多个 ui/<功能>/ 用     → components/

一个数据类 / 状态类放哪？
├─ 只被一个 Screen 用 → 该文件内 private
├─ 被 ui/X 与 ui/Y 共用 → model/
└─ 被 ui/ 与 components/ 共用 → model/（**不要**挂在任一方，否则产生反向依赖）
```

### 三条反模式

1. **`components/` 反向 import `ui/<功能>/`** —— 共享层依赖叶子层，形成包级循环。被发现过两次（`AccountCard`→`ui.list.OtpCodeItem`、`ScanFrameOverlay`→`ui.add.ScanState`），解法是抽 `model/` 中立包。
2. **UI 模型寄生在 ViewModel 源文件里** —— 一旦组件要用，就会让组件依赖 `ui/<功能>/`。UI 模型一律独立文件放 `model/`。
3. **「只有 1 处使用」就往 `components/` 塞** —— 要看消费者在哪：消费者在 `components/` 内，就该留在 `components/`；消费者是页面，就该放 `ui/<功能>/`。

## 四、core 模块的准入与归属

新增模块前先过 `dependencies-policy.md` §6.2 的三条门槛（领域边界 / Gradle 可执行约束 / 物理隔离）；
不满足就用**包**组织，不要建模块。

### 引擎模块是一类特殊落点

`core:webview`、`core:qrscanner`、`core:image-editor` 属于**引擎模块**：它们持有 Android platform API 或第三方 SDK，且**以 UI 形态存在**，无法用 domain 端口表达（历史上 `MediaEditor` / `ImageHandle` / `ImageSource` 三个端口正是因签名里出现平台类型而被 `core:image-editor` 取代）。

> ⚠️ **不要用「当前只有 1 个消费者」去否决引擎模块。** 引擎化的目的是消灭平台类型泄漏与 domain 端口污染，不是共享复用——消费者数量是结果，不是判据。`core:image-editor` 仅 `picture/impl` 消费属预期。
> 反过来也要注意：这条是「不要因为消费者少而下沉」，**不等于**「即使能力已下线也必须留着模块」——
> 播放能力随 `core:player` 与「视频播放」示例页一起移除，正属于后者。

### designsystem 与 ui 的边界

| | `core:designsystem` | `core:ui` |
|---|---|---|
| 内容 | `AppColors` / `AppSpacing` / `AppTheme` / `LocalWindowSizeClass` | `components/` / `mapper/` / `utils/` |
| 禁止 | import 任何项目内模块；出现 Composable 组件 | 下探 data / network / database |
| 新增色值/间距 | 一律入 `AppColors` / `AppSpacing` | 组件内不得出现 `Color(0xFF...)` |

业务语义在 `core:ui` 里必须**参数化**：文案走 `ArticleFeedLabels`，图标走 `FeedCard` 的 `toggleCheckedIconRes` / `toggleUncheckedIconRes`；默认值只为兼容现有调用方，不代表固定语义。

### 全局装饰层（`core:pet`，2026-11 由 `core/ui/pet` 拆出）

漂浮宠物是跨**全部页面**的绘制层（由组合根挂载，feature 不直接消费），
行为逻辑（游动/打盹/受惊/跃起/穿越）只在 UI 层成立，因此不适合下沉到 domain。

**成模块依据**是 `dependencies-policy.md` §6.2 条件②（需要 Gradle 层可执行的约束）：
「只有组合根能用、feature 不得使用」靠包名无法强制，只能靠模块边界 + `FEATURE_ALLOWED_CORE_DEPENDENCIES`
不收录来实现。它**不是引擎模块**——§6.10 的准入要求「需被 feature 直接调用」，而宠物恰恰相反，
也不持有平台 API 或第三方 SDK。允许依赖只有 `core:designsystem`（颜色令牌）。

对外导出的公开类型（按本文档要求**先登记再使用**）：

| 类型 | 用途 |
|---|---|
| `FishPet` / `FishPetDefaultHeadRadius` | 组件本体与尺寸基准（设置页按倍数缩放） |
| `FishPetTap` / `FishPetSwipe` / `FishPetTeleport` | 外部事件：普通点击 / 快速划过 / 换页穿越 |
| `Modifier.fishPointerObserver` | 父容器触摸观察器（只观察不消费，**只上报位置**） |

> 穿越只在**导航返回栈变化**时发生（跳详情、返回、深链）；底部 tab 切换属于同页内容替换，
> 宠物维持"游过去"，因此不再需要 `feature → 组合根` 的页面切换信号。

限定：

- 以上类型只承载**事件与参数**，签名里不得出现 repository / usecase；
- 观察器不持有可写状态、不读时钟：事件 id、时间戳、穿越判定统一由组合根的
  `PetInputHub`（`:app`）组装；`SystemClock` 不得出现在本模块；
- 宠物内部的绘制与行为实现（`FishPainter` / `FishSwimRuntime` / `FishLeap` 等）一律 `internal`，
  包括绘制入口 `drawFish`——没有第二个消费者就不对外暴露；
- Activity 重建后的**位置延续**由本模块自己承担（`FishMotion` 进 SavedState，见 `FishMotionSaver`）：
  组合根不需要持有/传入运动状态，也不要为此扩大 `configChanges`。

### 设置类数据的落点：端口 + 适配器（+ 应用级状态）

新增设置项（开关 / 滑杆 / 枚举）按这套落点写，**不要**再让 `core:database` 直接实现 domain 端口：

1. `core:data-contract`：适配器侧端口。字符串 KV 用通用的 `KeyValueLocalDataSource`（一个 key 一个字符串，
   不解释字段语义）；
2. `core:database`：`KeyValueLocalDataSourceImpl` 只转发读写并记录写入失败，不含任何字段语义；
3. `core:data-repository`：`XxxRepositoryImpl` 负责定义 key、缺键回退默认值、范围夹紧
   （出厂默认值不合法比"没有设置"更难排查：透明度 0 = 鱼看不见，用户无法自助恢复）；
4. 需要「Activity 重建后首帧就是真实值」时再加一层应用级状态：`core:domain` 的 `XxxState`
   （`StateFlow` 契约）+ `core:data-repository` 的 `XxxStateImpl`
   （`stateIn(appScope, SharingStarted.Eagerly, 默认值)`），组合根直接读它，不再订阅冷 Flow。
   > 它只解决**进程存活时的重建**；冷启动首帧仍是默认值（数据源异步首发射），属已知限制。

存量例外：深色模式仍由 `core:database` 的 `ThemeLocalDataSourceImpl` 直接实现 `ThemeRepository`
（规则 17 白名单成员）。**禁止新增同类实现**；迁移时改走上面第 1–3 步，并同步收窄规则 17 白名单。

## 五、机器可执行的约束

两套守卫**互补**：Gradle 校验「声明的边」，Konsist 校验「源码里的 import」。
`implementation(project(...))` 不产生 import 时 Konsist 感知不到；反之 Gradle 看不到越界引用。二者必须同时维护。

```bash
./gradlew verifyModuleDependencies                             # Gradle 守卫（配置缓存兼容）
./gradlew :app:testDebugUnitTest --tests "*Architecture*"      # Konsist 守卫
```

两个守卫都可与 `check` 同批执行，不需要 `--no-configuration-cache`：依赖图在配置阶段采集成任务输入，
执行阶段不访问 `Project`。

### 资源守卫（`scripts/verify-splash-avd.py`）

开屏 `AnimatedVectorDrawable` 的失败模式是**静默**的：某个 `<target>` 找不到同名节点，
或 `pathData` 形变两侧命令序列不一致时，整份动画不播，但编译 / lint / 打包全部通过。
因此单独一条零依赖检查，CI 在构建前执行：

```bash
python3 scripts/verify-splash-avd.py     # 退出码 0=通过
```

检查三件事：目标名可解析、形变两侧命令序列一致、`pathData` 命令合法。
改完 `app/src/main/res/drawable/splash_bg.xml` 请先跑它——这个文件的数据由几何脚本生成，
参数记在文件头注释里。

### Gradle 守卫（`FragmjectAndroidDependencyGuardPlugin.kt`）

| 规则 | 内容 |
|---|---|
| 环检测 | 模块依赖图必须是 DAG |
| app 聚合 | 所有 `feature:*:impl` 必须被 `:app` 依赖 |
| app 白名单 | `APP_ALLOWED_DEPENDENCIES` 正向声明 |
| R1/R5 | `domain` 不依赖数据模块；`data-repository` 不依赖 network/database |
| R2/R3/R6 | feature impl 不依赖其他 feature 的 impl/api；feature api 不依赖 feature impl |
| **R10** | `feature:*:api` 不得依赖其他 `feature:*:api`（补 R2/R3 与规则 3 的交叉盲区） |
| R8/R9 | feature → core 白名单；`feature:*:api` → core 仅 `navigation-contract` |
| core→core | `CORE_ALLOWED_DEPENDENCIES` 正向矩阵 |

### Konsist 守卫（`app/src/test/.../architecture/`）

- 规则 1–11：分层方向 + Android framework 白名单/黑名单
  （含「domain 仅用 paging-common 不用 paging-runtime」、「契约层不得依赖 Android framework」；
  规则 11 对 UI 层（`core:ui` + `core:pet`）禁 `android.provider.*` / `ContentResolver` / `android.media.*` / `android.database.*`）
- 规则 12：feature 不得依赖数据平台与数据实现
- 规则 13：`android.webkit.*` 仅限 `core:webview`
- 规则 14：feature 内必须用 `hiltViewModel()`
- 规则 15：`CookieStore` 实现仅限 `core:webview`（`core:testing` 的 `FakeCookieStore` 只被 `testImplementation` 引用，作为测试替身豁免）
- 规则 16：`feature:*:api` 仅依赖导航契约
- 规则 17：`core:database` 只允许实现白名单内的 domain 端口
- 规则 18/19：业务成功码只能经 `ApiCode` 判断；`data-repository` 不得用 `runCatching`
- 规则 20：导航契约完整接入（方法禁止 `= Unit`；`Navigators` 字段 / `noop()` / app 实现必须齐全）
- 规则 21：`:app` 不得引用数据实现层（白名单登记制）
- 规则 22：`core:designsystem` 不得 import 任何项目内模块
- 规则 23：UI 层（`core:ui` + `core:pet`）不得下探 data / network / database（选择器共用 `isUiLayerFile()`，新增 UI 层模块只需改一处）
- 规则 24：`feature:*:impl` 不得硬编码 `Color(0x...)`（颜色一律进 `AppColors`，或集中到 `*Colors.kt`）
- 规则 28：单个生产文件不超过 500 行
- 规则 29：禁止新增 `*Dto` / `*DTO` 类（ADR-0001：`core:model` 为共享协议模型唯一落点）
- 规则 30：`core:model` 之外不得声明同名模型类（拦「原样复制」，与规则 29 拦「加后缀」互补）
- 规则 31：`CORE_EXCEPTION_EDGES` 与 `dependencies-policy.md` §6.6 双向一致（新增 core 例外未登记即失败；已删除的例外残留在文档里也失败）
- 规则 31b：`app` → `core:*` 的能力型边（非数据/Hilt 聚合）必须在 §6.6 登记，防止 app 侧新增例外只写在守卫注释里
- 另：`boundary selectors match real files` 防止选择器写错导致规则空转

## 六、常见误判（本项目踩过的坑）

1. **用「只有 1 个消费者」否决 core 引擎模块** —— 见 §四。
2. **契约方法写 `= Unit` 默认实现** —— 漏改 `AppXxxNavigator` 也能编译通过，运行时「点击无反应」。现已全部去掉默认实现（规则 20）。
3. **把「只有 1 处使用」的组件留在 `components/`** —— 要看消费者在哪，见 §三反模式 3。
4. **UI 模型定义在 ViewModel 源文件里** —— 见 §三反模式 2。
5. **通用组件里硬编码业务文案/图标** —— 必须参数化，见 §四。
6. **app 直接引用数据实现层** —— 装配应下沉到对应 core 模块；确需引用须登记进规则 21 白名单。
7. **大屏分栏与全屏页混淆** —— 未实现 `DetailPaneNavKey` 的即全屏页，请在 NavKey KDoc 里写明，避免后来者误以为是遗漏。
8. **跨 feature 直接引用 NavKey** —— 一律走语义 Navigator 接口（`LocalNavigators`）。

## 七、新增一个 feature 的步骤

1. `settings.gradle.kts` 增加 `include(":feature:{name}:api")` 与 `:impl`
2. api 模块：建 NavKey 路由表；`build.gradle.kts` 只声明 `api(project(":core:navigation-contract"))` + `navigation3.runtime`
3. impl 模块：按 §三建目录；`build.gradle.kts` 声明 core 白名单内的依赖
4. `nav/{Name}NavContentContributor.kt` 注册渲染器 + `di/` 里 `@IntoSet` 贡献
5. 需要新跳转时在 `core:navigation-contract` 加语义接口，并同步 `Navigators` 字段、`noop()`、`AppNavigators`、`@Binds`（4 处）：
   - **跨 feature** 跳转（其他 feature 要跳进来）：必须走语义接口，不得 import 目标 feature 的 NavKey（R3）；
   - **本 feature 内部**跳转：现阶段同样走语义接口——`NavigationDispatcher` 在 `:app`，feature 唯一可用的导航手段就是 `LocalNavigators`，没有第二条路。
   - 后续方向：若把「按 NavKey 导航」的能力下沉到 `core:navigation-*`，内部跳转改为直接用本 feature 的 NavKey，语义接口只保留跨域动作（届时同步本节）。
6. `app/build.gradle.kts` 加 `:feature:{name}:impl` 依赖（`APP_ALLOWED_DEPENDENCIES` 同步）
7. 跑 §五的两条命令 + `./gradlew :app:assembleDebug`
8. 对照 `dependencies-policy.md` §6.11 变更 checklist 逐项确认
