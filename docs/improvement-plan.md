# 技术债与优化待办

本文档记录 2026-10 全量评审的**扣分项**与可执行优化方案（评审总分 76/100）。只写"待办"，已完成项见 §零，不重复排期。

优先级约定：

| 级别 | 含义 | 处理节奏 |
|---|---|---|
| **P0** | 影响正确性、CI 可信度或结构性风险 | 下个迭代内完成 |
| **P1** | 降低长期维护成本，有明确收益 | 一个季度内 |
| **P2** | 工艺打磨，可择机 | 有空档再做 |

使用方式：完成一项就把 `[ ]` 改成 `[x]`，并在条目末尾补一行 `> 完成于 YYYY-MM-DD，commit <sha>`，便于回溯。

---

## 零、已完成（2026-10，勿重复排期）

- [x] **lint 策略集中化**：`build-logic` 的 `FragmjectAndroidApplicationPlugin` / `FragmjectAndroidLibraryPlugin` 统一调用 `Lint.configureFragmjectLint(baseline)`，开启 `abortOnError` / `checkReleaseBuilds`，告警不阻断。
- [x] **lint baseline 落地**：按模块生成 `lint-baseline.xml`（`./gradlew updateLintBaseline`），当前工程零 issue，基线为空。
- [x] **移除无用权限**：`app/src/main/AndroidManifest.xml` 删除无使用者的 `FOREGROUND_SERVICE`。
- [x] **修复 `core:data-repository` 单测**：`logout` 语义变更后测试未同步（期望抛异常 → 实际只记日志），且 `android.util.Log.w` 在 JVM 单测中 `not mocked`；已在模块 `build.gradle.kts` 配 `unitTests.isReturnDefaultValues = true` 并按新契约重写用例。
- [x] **T-02 CI 执行 lint**：`ci.yml` 新增 `lintDebug` 步骤，失败时上传 lint 报告（2026-10-20）。
- [x] **T-03 CI 构建 Debug APK**：`ci.yml` 新增 `assembleDebug` 步骤（2026-10-20）。
- [x] **lint 不使用 baseline**：曾为 33 个模块生成 `lint-baseline.xml`，后判定"每模块一份基线"不可维护，全部删除；6 条存量告警改为代码级修复 + 一条按需全局 `disable`（详见 T-13）。
- [x] **T-04 覆盖率度量**：约定插件接入 Gradle 原生 JaCoCo，新增 `unitTestCoverageReport` 任务；AGP 9 已移除内置覆盖率能力，详见 T-04 的踩坑记录（2026-10-20）。
- [x] **T-01 首批样板测试**：`HomeViewModelTest` + `SettingViewModelTest`，共 7 个用例，作为后续 feature 模块的模板（2026-10-20）。
- [x] **T-01 覆盖 7/9 feature 模块**：feature 层用例从 1 增至 51；`MainDispatcherRule` 已收敛到 `core:testing`（2026-10-20）。
- [x] **`core:testing` 模块**：统一存放 ViewModel 测试基础设施，7 个 feature 模块通过 `testImplementation(project(":core:testing"))` 引用（测试配置不参与依赖守卫的边计算）。
- [x] **T-05 前置：`LunarUtils` 特征测试**：新增 7 个用例锁住现有行为，并修复「农历三十显示为卅十」「摩羯座误写作摩蝎座」两处用户可见错误。
- [x] **T-05 全部完成**：5 个超 500 行文件全部拆分到 ≤500 行（`AppSecurityGuard` 1053→493+496+118，`NightSwitchButton` 886→114+353+449+132，`LunarUtils` 753→341+306+141，`PictureEditorView` 555→441+168，`WebViewContainer` 533→286+194+120+103）。
- [x] **T-05 之一：`LunarUtils` 拆分完成**：753 行 → `LunarTables`(141) + `LunarAlmanac`(306) + `LunarUtils`(341)，公开 API 保持一行委托，7 个特征测试全程绿。其余 4 个超大类（`AppSecurityGuard` 1053、`NightSwitchButton` 886、`PictureEditorView` 555、`WebViewContainer` 533）待拆。
- [x] **T-01 第二批 feature 测试**：新增 `ProjectTree` / `System` / `Nav` / `My` / `MyCoin` / `BrowseHistory` 六个 ViewModel 测试类共 18 例，feature 层用例 51 → 69（2026-10-20）。
- [x] **T-07 第二批契约测试**：补 `Search` / `Project` / `HomeNav` / `System` 四个仓储的刷新契约，并新增 `CollectStateImplTest`（4 例）、`ThemeStateImplTest`（3 例）、`RegisterUseCaseTest`（6 例）（2026-10-20）。
- [x] **T-06 detekt 接入**：`./gradlew detekt` 扫描全仓 `main` 源码，77 条存量写入相对路径 `config/detekt/detekt-baseline.xml`，CI 增加步骤并上传报告；已实测新增违规会失败（2026-10-20）。
- [x] **T-15 时钟抽象**：`Clock` / `TimeOffsetProvider` 注入 `AuthenticatorViewModel` 并由 `TimeModule` 提供绑定，新增 5 例测试覆盖正常刷新、漂移阈值、SNTP 失败降级与退订停 ticker（2026-10-20）。
- [x] **T-14 picture 解阻塞（方案 2）**：`save()` 改为接收 `saveAction: suspend () -> SaveResult?`，`ImageEditorEngine` 抽为接口 + `DefaultImageEditorEngine`，新增 5 例测试（2026-10-20）。
- [x] **T-01 完成**：补齐 authenticator 三个 ViewModel、collection 两个、`Rank`、`ProjectList`、demo 农历/日历纯逻辑，feature 层共 121 例，逻辑层覆盖率 75.6%（2026-10-20）。
- [x] **T-04 覆盖率门槛**：新增 `verifyUnitTestCoverage`，CI 校验 feature 50% / `core:data-repository` 55% / `core:domain` 80%，口径排除 Compose UI 与生成类（2026-10-20）。
- [x] **T-03 release 构建**：本地 `:app:assembleRelease` 全链路通过（R8 + 资源收缩 + arm64 native + 签名，3m1s），新增 `release-weekly.yml` 每周构建并上传 APK（2026-10-20）。
- [x] **T-11 mapping 归档**：release workflow 从 `config.properties` 读 `versionName`，按 `r8-mapping-<version>` 归档 mapping 与 APK，缺失即失败（2026-10-20）。
- [x] **T-08 升级验证清单**：新增 `.github/pull_request_template.md`，升级 PR 强制填写三步验证与联动版本约束（2026-10-20）。
- [x] **T-07 完成**：接入 Robolectric 覆盖 `AlbumRepositoryImpl` / `MediaRepositoryImpl`，13/13 仓储均有测试；JaCoCo 补 `includeNoLocationClasses` 后 `core:data-repository` 覆盖 74.4%（2026-10-20）。

---

## 一、扣分项总览

| 编号 | 维度 | 问题 | 优先级 | 预估 | 状态 |
|---|---|---|---|---|---|
| T-01 | 测试纵深 | feature 层 13,550 行主代码仅 1 个测试类 | P0 | 3–5 人日 | 已完成（9/9 模块，121 个用例，逻辑层覆盖 75.6%） |
| T-02 | CI | 流水线不跑 lint，静态检查形同虚设 | P0 | 0.5 人日 | 已完成 |
| T-03 | CI | 流水线不构建 APK，只跑单测 | P0 | 0.5 人日 | 已完成（Debug + 每周 Release） |
| T-04 | 度量 | 无覆盖率统计，"测试够不够"无据可依 | P1 | 1 人日 | 已完成 |
| T-05 | 代码质量 | 5 个文件超 500 行，最大 1053 行 | P1 | 2–3 人日 | 已完成（5/5，安全项待真机验证） |
| T-06 | 代码质量 | 无 detekt / ktlint / spotless | P1 | 1 人日 | 已完成（detekt 接入，存量 baseline 豁免） |
| T-07 | 契约防护 | 实现与测试存在漂移（已发生一次） | P1 | 1 人日 | 已完成（13/13 仓储有测试覆盖） |
| T-08 | 依赖 | 版本次新，升级风险无验证清单 | P1 | 0.5 人日 | 已完成 |
| T-10 | 发布 | 无 Baseline Profile / 无启动性能度量 | P2 | 2 人日 | 进行中（模块与自有 profile 采集链路已落地，待设备采集数据） |
| T-11 | 发布 | R8 mapping 文件未归档流程化 | P2 | 0.5 人日 | 已完成（按版本号归档） |
| T-12 | 文档 | 本地复现与排障入口缺失 | P2 | 0.5 人日 | 已完成 |
| T-13 | 代码质量 | 6 条 lint 存量告警（曾冻结进基线，后改为直接修掉） | P2 | 0.5 人日 | 已完成 |
| T-14 | 可测性 | `ImageEditorEngine` 是具体类且 API 暴露 `Bitmap`，`PictureEditorViewModel` 无法单测 | P2 | 1 人日 | 已完成（方案 2） |
| T-15 | 可测性 | `AuthenticatorViewModel` 直接依赖 `SntpTimeSource`（UDP）与 `System.currentTimeMillis`，无时钟抽象 | P2 | 1–2 人日 | 已完成（时钟/漂移端口注入） |

> T-13 明细与最终处置（2026-10-20，基线方案已整体废弃，见 T-02 实施记录）：
>
> | 模块 | 问题 id | 处置 |
> |---|---|---|
> | `core:designsystem` | `ComposableNaming` | 局部 `@Suppress("ComposableNaming")`：`rememberXxx` 是 Compose 工厂函数命名约定 |
> | `core:designsystem` | `ConfigurationScreenWidthHeight` ×4 | 改用 `LocalWindowInfo.current.containerSize` + `LocalDensity` 换算，顺带修掉分屏/自由窗口下尺寸滞后 |
> | `core:ui` | `ModifierFactoryExtensionFunction` | `shimmerModifier` 改为 `Modifier` 扩展函数，调用点由 `.then(...)` 改为链式调用 |
> | `core:image-editor` | `IconMissingDensityFolder` | 约定插件全局 `disable`，理由：图标只提供 `drawable-xxhdpi`，由系统按密度缩放，属有意的体积取舍 |
> | `feature:picture:impl` | `IconMissingDensityFolder` | 同上 |

---

## 二、P0

### T-01 feature 层测试缺失

**证据**

```
feature   主代码 13,550 行    测试 102 行（仅 MainViewModelTest）
core      主代码 15,008 行    测试 2,532 行
```

core 侧有 17 个测试文件覆盖并发、取消、清理失败等边界；feature 侧 13 个 `impl` 模块里只有 `feature:home:impl` 有一个 ViewModel 测试。

**方案**

1. 复用 `feature/home/impl/src/test/.../MainDispatcherRule.kt`，为每个 `impl` 的 ViewModel 建测试类。
2. 每个 ViewModel 至少覆盖三态：**成功** / **业务失败**（返回 error 码）/ **异常与取消**（`CancellationException` 必须重抛，不能降级成 Error 态）。
3. 高频页优先：`home` / `article` / `collection` / `user` / `search`。
4. Compose 层不为每个页面写快照测试，只对**关键交互**各写 1–2 条（列表点击、分页到底、空态渲染），放在 `androidTest`。

**验收**

- [x] 建立可复用的样板：`HomeViewModelTest`（4 例）+ `SettingViewModelTest`（4 例），含 fake 端口、`MainDispatcherRule` 用法
- [x] 每个**可测**的 `feature:*:impl` 至少 1 个 ViewModel 测试类 —— 9/9 完成（home、user、search、article、collection、auth、demo、authenticator、picture）
- [x] `authenticator` 解阻塞：`Clock` / `TimeOffsetProvider` 注入后新增 `AuthenticatorViewModelTest`（5 例），见 T-15
- [x] `picture` 解阻塞：方案 2 改造后新增 `PictureEditorViewModelTest`（5 例），见 T-14
- [x] 有状态 ViewModel 的 Loading / Success / Error / Cancelled 分支均有断言 —— 刷新类（`ProjectTree` / `System` / `Nav`）覆盖成功、业务失败、错误态优先、重试恢复与取消重抛；保存类（`PictureEditor`）覆盖成功/失败/异常/取消；纯分页包装（`Rank` / `MyShare` / `MyCollect` / `ProjectList`）没有状态机，只校验流装配
- [x] feature 层逻辑代码行覆盖率 ≥ 50% —— 实测 **75.6%**（1147/1518），口径见 T-04：排除 Compose UI 与生成类

**实施记录（第一批，2026-10-20）**

- `feature:home:impl` → `HomeViewModelTest`：头部成功填充、失败保留旧值并暴露错误、失败后重试清除错误（3 例；该模块共 5 例）。
- `feature:user:impl` → `SettingViewModelTest`：登出成功 / 仓储返回 false / 仓储抛异常三条分支均断言事件出口与**退出 Loading 态**，另加清缓存失败消息透传（4 例）。
- `feature:user:impl` 的 `build.gradle.kts` 需开 `unitTests.isReturnDefaultValues = true`（`SettingViewModel` 异常分支调用 `Log.e`），并补 `kotlinx-coroutines-test` 测试依赖。
- `feature:search:impl` → `SearchViewModelTest`（7 例）：热搜词成功/失败/重试三态、历史与热词收集、提交搜索、退出搜索、删除历史。
- `feature:article:impl` → `WebViewModelTest`（8 例）：书签与当前 URL 匹配、书签增删、浏览记录写入、图片保存三条分流（null / http URL / base64）。
- `feature:collection:impl` → `ShareArticleViewModelTest`（6 例）：标题/链接空校验不发起请求、成功消息透传与空消息兜底、失败保持 success=false、`resetMessage` 只清消息不影响表单。
- `feature:user:impl` → `UserViewModelTest`（4 例）：积分加载、无数据回落默认值、同 userId 重复 `init` 只请求一次、userId 变化重新拉取。
- `feature:auth:impl` → `LoginViewModelTest`（5 例）+ `RegisterViewModelTest`（5 例）：空用户名/空密码/两次密码不一致均在 UseCase 层拦下且不触达仓储，成功与失败态映射，`resetMessage` 复位。
- `feature:demo:impl` → `CalendarViewModelTest`（7 例）：初始化选中今日并加载日程、切换日期加载、同日期不重复加载、日程增删的持久化，以及**落盘失败回滚**（乐观更新必须回到操作前快照）。
- **测试发现的真实缺陷并已修复**：`SearchViewModel#refreshHotKey` 成功分支原先不关闭 loading，只依赖 Room 写入后再次推送 `observeHotKey` 收尾；当"刷新成功但数据未变化"时不会重发，热搜区会永久转圈。已改为两个分支都显式收尾。
- `MainDispatcherRule` 在各 feature 模块各持一份（避免为测试工具新增共享模块）；目前已复制到 4 个模块，达到 5 个以上时抽 `core:testing`。
- **该约定已执行**：`MainDispatcherRule` 已收敛到 `core:testing`，7 个 feature 模块通过 `testImplementation(project(":core:testing"))` 引用。

**实施记录（第二批，2026-10-20）**

- `feature:home:impl` → `ProjectTreeViewModelTest`（4 例）、`SystemViewModelTest`（2 例）、`NavViewModelTest`（4 例）、`MyViewModelTest`（3 例）：
  - 缓存先出、刷新失败转 `Error` 并保留服务端消息、错误态优先（缓存发射不覆盖错误页）、显式重试恢复 `Success`；
  - 导航与体系树两条流刷新互不牵连，`retry()` / `retryTree()` 只重刷各自目标；
  - `pagingFlow(cid)` 懒创建并按 cid 复用；未登录时 `MyViewModel` 回落空用户。
- `feature:user:impl` → `MyCoinViewModelTest`（2 例）、`BrowseHistoryViewModelTest`（3 例）：积分返回 `null` 时降级为默认 `Coin`；书签与浏览记录合并发布，写操作委托仓库。
- `feature:authenticator:impl` → `AuthenticatorViewModelTest`（5 例）：固定时钟下验证码可复现、漂移超阈值展示、小漂移隐藏、SNTP 返回 `null` 静默降级、退订后 ticker 停止（见 T-15）。
- `feature:picture:impl` → `PictureEditorViewModelTest`（5 例）：保存成功 / 返回 null / 抛异常 / 取消不降级 / 重复点击只执行一次（见 T-14）。
- feature 层用例累计 51 → 79；`authenticator` 与 `picture` 两个原本被可测性阻塞的模块均已解阻塞，9/9 `impl` 模块都有 ViewModel 测试。

**实施记录（第三批，2026-10-20：补齐剩余 ViewModel 与 demo 纯逻辑）**

- `feature:authenticator:impl` → `AuthenticatorAddViewModelTest`（10 例：校验短路、密钥归一化、周期兜底、重复保存、扫码填表、单/多账户迁移导入、不可识别内容）、`AuthenticatorTransferViewModelTest`（4 例：默认不选中、toggle、全选反选、格式切换独立）、`AuthenticatorExportQrViewModelTest`（4 例：未初始化、单账户 otpauth、未知 id 过滤、批量 migration 与分批 caption）。
- `feature:collection:impl` → `MyCollectViewModelTest` / `MyShareViewModelTest`（各 1 例：分页流装配与 `CollectState` 透传）。
- `feature:home:impl` → `ProjectListViewModelTest`（1 例：分页流按 cid 缓存复用）。
- `feature:user:impl` → `RankViewModelTest`（1 例：分页流装配）。
- `feature:demo:impl` → `LunarAlmanacTest`（8 例：生肖、农历日文案含二十/三十、星座边界、节气范围、干支、十二建除宜忌、岁破/月破/杨公忌、整月宜忌）、`CalendarModelTest`（5 例：年范围与月/周计数、月索引往返、公历日反查含农历、周索引往返、本地日期落区间）。
- feature 层用例累计 79 → **121**；逻辑层覆盖率从 31.2% 提升到 75.6%。
- **测试发现的待评估问题（未改生产代码）**：`BrowseHistoryViewModel` 用 `zip` 合并书签与浏览记录，`zip` 按对配对，单条流发射不会产生新组合值；若两个 Room 查询监听的表不同，只新增浏览记录时历史列表可能不刷新。建议评估改为 `combine`。

---

### T-02 CI 未执行 lint

**证据** `.github/workflows/ci.yml` 只跑：

```
./gradlew verifyModuleDependencies verifyComposeStabilityConfig testDebugUnitTest
```

lint 已在约定插件中配置，但流水线从不触发，等于没有。

**方案**

在 CI 的 gradle 命令里追加 `lintDebug`，并把 `**/build/reports/lint-results-*.html` 加入失败时的 artifact 上传。baseline 已就位，存量问题不会阻塞。

**验收**

- [x] PR 中引入 lint error 时 CI 必须失败 —— 本地已验证：基线缺失时构建直接 `Aborting build since new baseline file was created`
- [x] lint 报告在 CI 失败时可下载（`lint-reports` artifact）

**实施记录**

- 约定插件给所有模块配了 `baseline`，但只有部分模块有基线文件，导致首次 `lintDebug` 报 "new baseline file was created" 并中止。当时执行 `./gradlew updateLintBaseline` 为全部模块生成了基线。
- **该方案已被废弃**：AGP 的 baseline 按模块独立维护，33 个模块就是 33 份文件；对照 nowinandroid（全仓零 baseline，靠约定插件统一配置 + 直接修）后，改为不使用基线，6 条存量告警直接处理（见 T-13）。
- 现行 lint 策略：`abortOnError` + 不设 baseline + 只对 `IconMissingDensityFolder` 做一条有理由的全局 `disable`。

---

### T-03 CI 不构建 APK

**证据** 同 T-02：流水线从未执行 `assembleDebug` / `assembleRelease`，R8、资源合并、native 编译、签名配置全部未在 CI 覆盖。当前 `app` 含 NDK 28 的 C++ 代码，一旦 native 编译坏掉，CI 仍是绿的。

**方案**

1. 新增 `assembleDebug` 步骤（PR 必跑），验证完整打包链路。
2. nightly 或 `main` 分支追加 `assembleRelease`，验证 R8 规则与 `minifyEnabled = true` 路径。
3. native 只编 `arm64-v8a` 已在 `abiFilters` 中声明，CI 无需额外 ABI。

**验收**

- [x] PR 流水线包含 `assembleDebug` —— 已加入 `ci.yml`；本地增量 3s，CI 全量耗时待首次运行观察
- [x] release 变体至少每周构建一次并有产物 —— 新增 `.github/workflows/release-weekly.yml`（每周一 04:00 北京时间 + 手动触发），执行 `:app:assembleRelease` 并上传 APK 与 mapping

**实施记录**

- 本地验证：`./gradlew lintDebug assembleDebug` 成功，产物 `app/build/outputs/apk/debug/app-debug.apk`（41 MB）。
- Release 本地全链路验证（2026-10-20）：`./gradlew :app:assembleRelease` 成功，耗时 3m1s，1229 个任务；产物 `app/build/outputs/apk/release/app-release.apk`（11 MB）、`app/build/outputs/mapping/release/mapping.txt`（93 MB）、`baselineProfiles/` 目录已生成。R8、资源收缩、arm64-v8a native 编译与测试签名均通过。
- **发布包启动即崩（2026-10-20 修复）**：`assembleRelease` 成功 ≠ 能运行。真机与模拟器上 release 包一启动就 `java.lang.ClassCastException`，retrace 后落在 `core:network` 的 `NetworkModule.provideUserRemoteDataSource`（`retrofit.create(UserService::class.java)`）。根因：R8 把 `core:data-contract` 的 `*RemoteDataSource` 契约接口**合并**进了 `core:network` 的 `internal` Service 接口（mapping 里已无契约接口条目），随后把 `retrofit.create()` 返回值处的 `checkcast` 判定为必然失败，直接优化成 `throw new ClassCastException()`（dex 中可见 R8 合成方法 `()V { new ClassCastException; throw }`）。修复：`app/proguard-rules.pro` 增加 `-keep interface com.example.fragmject.core.data.contract.remote.** { *; }`，阻止契约接口参与合并；重打后在模拟器上启动正常、crash buffer 为空。教训：发布验证必须包含**一次冷启动冒烟**，仅构建成功不足以证明可发布。
- **同类问题二：release 首屏无数据（2026-10-20 修复）**：崩溃修好后首屏仍只有错误态与「重试」。临时在 `NetworkResultMapper` 加日志定位到 `IllegalArgumentException: HTTP method annotation is required ... for method CommonRemoteDataSource.fetchHotKey`。根因：`-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }` 的 `allowshrinking` 加上动态 Proxy 不可见调用，R8 把 internal Service 接口上带 `@GET/@POST` 的覆写方法裁掉，运行时 Retrofit 只能解析到 data-contract 父接口中**无注解**的方法。修复：`-keep,allowobfuscation interface com.example.fragmject.core.network.service.** { *; }`（整体保留服务接口，仍允许重命名）。验证：真机 release 首屏 banner 与文章列表正常渲染。两条 keep 规则的共性是：**Retrofit 的动态代理对 R8 不可见**，凡参与运行期反射/代理的类型都必须显式保留。

---

## 三、P1

### T-04 接入覆盖率度量

**现状** 全仓无 JaCoCo / Kover 依赖，覆盖率不可见。

**方案（已实施，与原计划不同）** 不用 Kover，改用 Gradle 原生 JaCoCo：

- 原计划假设 AGP 提供 `enableUnitTestCoverage` / `createDebugUnitTestCoverageReport`，实测**AGP 9.4.1 已移除**该能力（整个 AGP jar 里搜不到该属性/任务）。
- 最终方案：约定插件里 `pluginManager.apply("jacoco")`，给所有 `Test` 任务注入 agent，并注册聚合任务 `unitTestCoverageReport`（HTML + XML，输出到 `build/reports/jacoco/unitTestCoverageReport/`）。
- CI 增加 `./gradlew unitTestCoverageReport`，报告以 `coverage-reports` artifact 上传（`if: always()`）。

**踩坑记录** 首次生成的报告 0% 覆盖：AGP 9 把 Kotlin 编译输出从 `build/tmp/kotlin-classes/debug` 移到了 `build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`，取错目录会导致 class id 与 exec 对不上。已在新旧两个路径都取。

**当前基线（2026-10-20，行覆盖率）**

口径：只统计 JVM 可执行的逻辑层。`unitTestCoverageReport` 的 `classDirectories` 已排除 Compose UI（`*Screen` / `*Content` / `*Sheet` / `*Card` / `*Page` 等）、`di/`、`nav/`、`components/` 与 Hilt/注解处理器生成类。

> 旧的全量口径（含 Compose）已废弃：feature 层 7518 行中约 5268 行是 Composable，JVM 单测永远覆盖不到，纳入分母只会让数字永久停在 10% 左右，失去指导意义。

| 模块 | 覆盖 / 总行 | 覆盖率 | 门槛 |
|---|---|---|---|
| `core:domain` | 346 / 409 | 84.6% | 80% |
| `core:data-repository` | 384 / 516 | 74.4% | 55% |
| feature 逻辑层（9 模块合计） | 1147 / 1518 | 75.6% | 50% |
| `feature:picture:impl` | 18 / 200 | 9.0% | — |

> `feature:picture:impl` 的缺口集中在 `PictureFlowState`（144 行）：它直接持有 `android.net.Uri`，JVM 无法构造；引入 Robolectric 后可补，暂列为已知缺口。

**验收**

- [x] `./gradlew unitTestCoverageReport` 产出可用报告（33 个模块全部可执行）
- [x] CI 生成并上传每模块覆盖率
- [x] 设定覆盖率门槛 —— 新增根任务 `verifyUnitTestCoverage`（解析各模块 JaCoCo XML，低于阈值抛 `GradleException`），CI 在覆盖率报告之后执行；阈值为 feature 逻辑层 50%、`core:data-repository` 55%、`core:domain` 80%

**实施记录（2026-10-20）**

- 在约定插件 `configureUnitTestCoverage` 中把 `classDirectories` 改为过滤后的文件树，排除 Compose UI 与生成类，使报告口径与门槛一致。
- 新增 `verifyUnitTestCoverage`：读取 `build/reports/jacoco/unitTestCoverageReport/*.xml` 的 `LINE` counter（关闭外部 DTD 加载），按模块汇总后比对各阈值。
- 实测：feature 75.6%、`core:data-repository` 74.4%、`core:domain` 84.6%，三条门槛均有缓冲。CI 增加 `Unit test coverage gates` 步骤。
- T-07 引入 Robolectric 后发现其 sandbox classloader 会让项目类计为 0 覆盖；已在 JaCoCo agent 上开启 `includeNoLocationClasses` 并排除 `jdk.internal.*`，`core:data-repository` 覆盖率从 62.0% 修正为 74.4%。

---

### T-05 超大类拆分

**证据**（主代码，按行数降序）

| 文件 | 行数 |
|---|---|
| `app/.../security/AppSecurityGuard.kt` | 1053 |
| `core/ui/.../components/NightSwitchButton.kt` | 886 |
| `feature/demo/impl/.../calendar/LunarUtils.kt` | 753 |
| `core/image-editor/.../PictureEditorView.kt` | 555 |
| `core/webview/.../WebViewContainer.kt` | 533 |

**方案**

- `AppSecurityGuard`：按检测项拆成 `checks/` 下的独立检测器（ptrace / TracerPid / 匿名可执行段 / CRC32 / Frida 特征），聚合器只负责调度与结果合并。
- `NightSwitchButton`：抽出动画与绘制常量到同包 `internal` 文件，Compose 主体保留交互。
- `LunarUtils`：属于纯算法，按"历年/节气/转换"拆文件，并补单测（纯函数、零依赖，测试成本极低）。
- `WebViewContainer`：把池化、拦截、生命周期各自独立。

**验收**

- [x] 单文件 ≤ 500 行 —— 5/5 达标，并新增**架构规则 28**（`production files stay under five hundred lines`）防止回潮
- [x] 拆分过程不改行为 —— 每步单独编译验证；`LunarUtils` 有 7 个特征测试兜底，最终全量 `lintDebug + testDebugUnitTest` 通过

**实施记录（2026-10-20，拆分前置工作）**

- 已为 `LunarUtils` 补齐 **7 个特征测试**（`LunarUtilsTest`），锁住春节/腊月三十/闰二月/大小月/日期差/星座/1900 前降级等现有行为。拆分后这些断言必须原样通过。
- 补测试过程中发现并修复 2 处用户可见文案错误：
  - `getChinaDayString(30)` 返回「卅十」，农历三十显示错误 → 改为「三十」；
  - 星座表 `"摩蝎座"` 笔误 → 改为「摩羯座」。
- **拆分已完成（`LunarUtils` 753 → 341 + 306 + 141）**：
  - `LunarTables.kt`（141 行）：12 张静态数据表，`internal object`；
  - `LunarAlmanac.kt`（306 行）：生肖、干支、农历日文案、星座、节气日期、宜忌，`internal object`；
  - `LunarUtils.kt`（341 行）：日期互转算法 + 公开门面（`getYiJi`/`getConstellation` 等保留一行委托，调用点零改动）。
  - 拆分分两批提交验证，7 个特征测试在两步之后均保持全绿。
- **其余超大类拆分结果（2026-10-20）**：

  | 原文件 | 原行数 | 拆分后 |
  |---|---|---|
  | `NightSwitchButton.kt` | 886 | 114 / 353（Sky+Cloud+Stars）/ 449（SunAndMoon+Sun+Moon）/ 132（配色与常量） |
  | `WebViewContainer.kt` | 533 | 286 / 194（Clients）/ 120（权限与下载）/ 103（WebViewControl） |
  | `PictureEditorView.kt` | 555 | 441（State）/ 168（Canvas 与 remember） |
  | `AppSecurityGuard.kt` | 1053 | **未拆，已回滚** |

- **`AppSecurityGuard` 拆分（第二次尝试成功）**：1053 → 493（守卫：状态 + 公开 API + 生命周期）+ 496（检测项/`ensureNativeLoaded`/`detectReason`/`block`，以 `AppSecurityGuard` 扩展函数形式提供）+ 118（静态特征表，顶层 `internal val`，免对象前缀）。
  - 第一次尝试按固定行号 + 正则批量改写，因 KDoc/注解边界切错与 `private in file` 越权访问而失败，已 `git checkout` 回滚。第二次改为「锚点定位 + 缩进匹配收尾大括号 + 仅对实操引用的私有成员放开 `internal`」，一次编译通过。
  - 设计取舍：检测项做成扩展函数而非独立对象，可直接访问守卫状态，调用点零改动；代价是守卫内 18 个私有成员被提为 `internal`（仅 app 模块内可见）。
  - **仍需人工验证**：编译通过不等于防护生效，需真机/模拟器冒烟 Frida 端口、Root 管理应用、模拟器特征、签名校验四项。
- 顺带记录的既存问题（**未修**，需单独评估）：
  - `LUNAR_FESTIVAL` 的除夕键是 `"0100"`，但查询键是 `"月日"`（如 `"1230"`），**永远不会命中**；且腊月为小月时除夕是廿九，正确做法应由「次日是否为正月初一」推导。
  - `CHINESE_DATE_FORMAT` 是 `object` 内共享的 `SimpleDateFormat` 实例，`SimpleDateFormat` 非线程安全，`getDaysOfTwoDate` 并发调用有数据竞争风险。

---

### T-06 静态分析工具缺失

**方案** 优先 detekt（Kotlin 语义级，能查复杂度与坏味道），初期只开低噪音规则集：

```kotlin
// detekt.yml（节选）
complexity:
  LongMethod: { threshold: 80 }
  LongParameterList: { threshold: 8 }
  TooManyFunctions: { threshold: 50 }
style:
  MagicNumber: { active: false }   # Compose 里数值密集，先关
```

先跑一次全量看基线告警量，再决定是否启用 `baseline.xml`；接入 CI 时只对**新增**代码阻断。

**验收**

- [x] `./gradlew detekt` 可在本地执行 —— 扫描全仓 `main` 源码，增量执行约 1s（首次含依赖下载约 36s）
- [x] CI 中新增违规会失败，存量问题不阻断 —— 已实测：临时新增 `TooGenericExceptionCaught` 用例后 `detekt` 失败（1 weighted issue），删除后恢复通过

**实施记录（2026-10-20）**

- 接入 `io.gitlab.arturbosch.detekt` 1.23.8：根 `build.gradle.kts` 应用插件并显式指定源码树（各模块 `src/main`，排除 `build-logic`），不接类型解析，无需先编译各模块。
- 规则策略：继承默认规则集，关闭 `FunctionNaming`（Compose 组合函数首字母大写属约定，规则误报）、`ReturnCount`（守卫式早返回）、`NewLineAtEndOfFile`（格式化工具职责）、`MagicNumber`（Compose 数值密集）；`LongMethod` 阈值 80、`LongParameterList` 8、`TooManyFunctions` 50、`MaxLineLength` 140。
- 首次全量 225 条告警，关闭上述四条后剩 **77 条**，固化在 `config/detekt/detekt-baseline.xml`。
- baseline 与报告统一以 `rootDir` 作为 `basePath` 生成**相对路径签名**（实测文件内零绝对路径），避免本地 `/Users/...` 与 CI `/home/runner/...` 路径差异导致 baseline 全部失配。
- CI 新增 `./gradlew detekt` 步骤，失败时上传 `build/reports/detekt/` 报告。

---

### T-07 实现与测试的契约漂移

**背景** `101dd23d` 改了 `UserRepositoryImpl#logout` 的语义（远端成功 + 本地清理失败不再抛异常），测试未同步，CI 红。这类漂移在 core 侧已被 `RepositoryResultContractTest` 部分拦截，但覆盖不全。

**方案**

1. 把 `RepositoryResultContractTest` 的模式推广：每个 `RepositoryImpl` 都有一份"契约测试"，集中验证 成功/业务失败/异常/取消 四条主路径，与具体页面解耦。
2. PR 模板加一条自检：**改动 `core:data-repository` 的行为语义时，必须同步该模块测试**。
3. 关键语义（如"远端成功则本地失败不翻转结果"）在实现处保留注释，并在测试命名里复述同一句话，便于 review 对照。

**验收**

- [x] 每个 `*RepositoryImpl` 均有对应的契约测试类 —— 13/13 全覆盖：契约测试 `My` / `UserCenter` / `Download` / `Home` / `Search` / `Project` / `HomeNav` / `System`（集中在 `RepositoryResultContractTest`）、专项 `UserRepositoryImplTest`；`CoinRankRepositoryImpl` / `MyCollectRepositoryImpl` 的取数逻辑由 `NetworkPagingSourcesTest` 覆盖；平台耦合的 `AlbumRepositoryImpl` / `MediaRepositoryImpl` 由 Robolectric 测试覆盖
- [x] `logout` 的两个分支（远端成功 / 远端失败）都有显式用例 —— `UserRepositoryImplTest` 已覆盖 `logout success`、`logout rejected remotely`、`logout network failure`、`logout cancelled in flight`、`logout cookie cleanup failure` 五条

**实施记录（2026-10-20）**

- `RepositoryResultContractTest` 增补 `SearchRepositoryImpl`、`ProjectRepositoryImpl`、`HomeNavRepositoryImpl`、`SystemRepositoryImpl` 的离线优先刷新契约，验证：成功才写缓存、业务失败即使带 data 也不污染缓存、`LOGIN_REQUIRED` → `ERROR_UNAUTHENTICATED`、IO 异常 → `ERROR_UNKNOWN`、取消原样重抛。文件内累计 18 例。
- 新增 `CollectStateImplTest`（4 例）：乐观覆盖先于请求生效、失败回滚并广播服务端消息、空消息兜底「操作失败」、连续点击以后一次意图为准且旧请求不回滚。
- 新增 `ThemeStateImplTest`（3 例）：默认浅色、跟随仓库发射、保留最新值。
- 新增 `RegisterUseCaseTest`（6 例）：四项本地校验均先于仓库调用短路，合法输入透传参数与结果。
- **平台耦合实现（Robolectric，2026-10-20）**：接入 `robolectric 4.16` + `androidx.test:core 1.7.0`，`@Config(sdk = [36])`（4.16 不支持 targetSdk 37）。
  - `AlbumRepositoryImplTest`：空 MediaStore 仍返回「所有照片」分组且封面为空串，保证选择器入口稳定。
  - `MediaRepositoryImplTest`（4 例）：视频/媒体文件缺失时快速失败且 `uri` 为空；`createImageUri` → `finishImageUri` → `deleteImageUri` 全流程不抛异常；字节写入在无 provider 时走完清理路径并返回结果。
  - 覆盖率采集需在 JaCoCo agent 上开启 `includeNoLocationClasses`（Robolectric sandbox classloader），并排除 `jdk.internal.*`；否则 Robolectric 测试计为 0 覆盖。该配置已加入约定插件，`core:data-repository` 覆盖率由 62.0% 升至 74.4%。

---

### T-08 依赖版本次新，缺升级验证清单

**现状** AGP 9.4.1、Kotlin 2.4.21、Compose BOM 2026.09.00、Room3 3.0.3、Retrofit 3.0.0、Navigation3 1.2.0、OkHttp 5.5.0、Hilt 2.60.1 —— 全部处于前沿版本。

**方案**

1. 在 `gradle/libs.versions.toml` 顶部加注释块，记录每项"当前版本 / 升级窗口 / 验证清单"。
2. 升级验证固定三步：`testDebugUnitTest` → `assembleDebug` → 真机冒烟（登录、列表分页、WebView、扫码、图片编辑）。
3. 对 AGP / Kotlin 这类联动版本，升级必须同一次提交完成，禁止单独抬一个。

**验收**

- [x] catalog 内有版本策略注释（`gradle/libs.versions.toml` 顶部：联动升级约束 + 三步验证）
- [x] 每次升级 PR 附带三步验证结论 —— 已新增 `.github/pull_request_template.md`，「依赖升级」小节强制勾选三步（`testDebugUnitTest` → `assembleDebug` → 真机冒烟五项）与联动版本约束

---

## 四、P2

### T-10 Baseline Profile 与启动度量

当前无 Baseline Profile，冷启动未度量。方案：`androidx.baselineprofile` 插件 + Macrobenchmark 模块，覆盖启动与主列表滚动；产出 profile 存入 `app/src/main/baseline-prof.txt`。

- [x] 有 Macrobenchmark 模块 —— `:benchmark`（`com.android.test`）已落地：`BaselineProfileGenerator`、`StartupBenchmark`（冷/温启动）、`HomeScrollBenchmark`（首页帧耗时）
- [x] 有基准数据 —— 真机 PGEM10 / Android 16 实测（2026-10-21），指标与结论见下方实施记录
- [x] 自有 baseline profile 已生成 —— `app/src/release/generated/baselineProfiles/baseline-prof.txt`，34,004 条规则 / 3.8 MB（非混淆口径），入库
- [x] release 构建自动打包 baseline profile —— `:app:assembleRelease` 已产出 `app/build/outputs/apk/release/baselineProfiles/{0,1}/app-release.dm`；来源是 AGP 对依赖库 profile 的合并，自有 profile 仍需 Macrobenchmark 生成

**实施记录（2026-10-20）** release 本地构建验证时确认 profile 打包任务（`expandReleaseArtProfileWildcards` / `compileReleaseArtProfile`）已执行并产出 `.dm` 文件，无需额外配置。

**实施记录（Macrobenchmark 模块，2026-10-20）**

- 新增 `:benchmark` 模块（`com.android.test` + `androidx.baselineprofile` 1.5.0 / `benchmark-macro-junit4` 1.5.0 / `uiautomator` 2.4.0），`targetProjectPath = ":app"`，`minSdk = 28`，`compileSdk` 复用 `config.properties`。
- app 侧新增 `androidx.baselineprofile` 插件与 `baselineProfile(project(":benchmark"))`；插件会自动创建 `benchmarkRelease` / `nonMinifiedRelease` 变体，因此 app 的变体数量增加，但都不参与 `assembleRelease`。
- app 新增 `androidx.profileinstaller`：API 26–27 没有安装期 profile 编译，需要它在首帧后异步 dexopt；API 28+ 走系统安装期路径，不受影响。
- 安全守卫在 `nonMinifiedRelease` 变体下跳过（`AppSecurityGuard.COLLECTION_BUILD_TYPE`）：模拟器 ART/JIT 的匿名可执行段会命中 `ANON_EXEC_THRESHOLD`，守卫 1.2s 后 SIGKILL 自身，导致 `Target package ... is not running`、采集必然失败。该变体不发布，`release`/`benchmarkRelease` 的守卫行为不变。
- 依赖守卫把 `:benchmark` 记为 `TEST_ONLY_MODULES`：该模块经 `baselineProfile` 配置被 app 驱动，又经 `targetProjectPath` 反向持有 `:app` 的 testedApk，这条边既非运行期依赖，也会被环检测误判，故整条边不参与方向校验。
- 插件把 :app 的 `benchmarkRelease` / `nonMinifiedRelease` 两个变体透传到测试模块，因此 `:benchmark` 没有 `debug` 变体，任务名带变体后缀（`assembleBenchmarkRelease` / `connectedBenchmarkReleaseAndroidTest`）。
- 采集命令（**需连接设备或模拟器**，仓库/CI 环境无法代跑）：
  1. 生成 profile：`./gradlew :app:generateBaselineProfile`
     - 默认只覆盖 release 变体，产物由 `copyReleaseBaselineProfileIntoSrc` 复制到 `app/src/release/generated/baselineProfiles/baseline-prof.txt`，需提交入库；
     - 也可显式调用 `./gradlew :app:generateReleaseBaselineProfile`。
  2. 基准数据：`./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`
     - 指标落在 `benchmark/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/<设备>/*-benchmarkData.json`（`benchmark/build/outputs/benchmarkResults/` 实测为空，不要在那里找）；
     - 单条用例：追加 `-Pandroid.testInstrumentationRunnerArguments.class=com.example.fragmject.benchmark.StartupBenchmark`；
     - 单条方法：追加 `-Pandroid.testInstrumentationRunnerArguments.class="com.example.fragmject.benchmark.StartupBenchmark#coldStartup"`（`#` 必须整体加引号，否则被 shell 当作注释截断）。
  3. 不接设备时的编译校验：`./gradlew verifyModuleDependencies :benchmark:assembleBenchmarkRelease`（已本地通过，5s 增量）。
- 设备约束：无 root 时需 **API 33+** 才能采集（`BaselineProfileRule` 依赖 profileable + shell 权限）；模拟器已通过 `androidx.benchmark.suppressErrors=EMULATOR` 放行，但模拟器数据只用于回归对比，不能当作真机性能结论。
- **采集环境结论（2026-10-20 实测）**：真机（PGEM10 / Android 16）在第 3 轮 `extractProfile`（`pm dump-profiles` + 跨分区 `mv`）上偶发永久卡死，换 USB 后依旧，手动执行同一命令却 1ms 返回 → 判定为该 ROM 的 shell 路径问题。采集改用 **AOSP 模拟器**（Pixel_10_Pro_XL / API 37）后一次通过，耗时 2m8s；真机只用于跑 `connectedBenchmarkReleaseAndroidTest` 出指标（该路径不涉及 `pm dump-profiles`）。
- `includeInStartupProfile = true` 已开启：否则插件告警「No startup profile rules were generated」，API 29+ 缺少冷启动专用规则集。
- 被测包名不硬编码：`benchmark/build.gradle.kts` 从 `config.properties` 的 `applicationId` 生成 `BuildConfig.APP_ID`，测试代码统一引用它，改包名只需改一处。

**实施记录（2026-10-21 真机基准与首页优化）**

- 环境：PGEM10 / Android 16（API 36）/ `benchmarkRelease` / `compilationMode = run-from-apk`，5 次迭代取中位。
- 冷启动基线（首页优化前）：`timeToInitialDisplay` P50 **369.01 ms**（min 363.77 / max 374.25，CV 2.0%）。
- 首页优化后（真机 4 次独立运行）：`timeToInitialDisplay` P50 **334~343 ms**（中位约 341 ms）；`timeToFullDisplay` P50 **853~1124 ms**（中位约 855 ms；单次运行只有 1~2/5 轮被采纳）。每次运行内部 `TTFD − TTID` 稳定在约 **515 ms**，该差值比单次 TTFD 更适合作为优化对照。
- `timeToFullDisplay` 依赖应用主动上报：首页 `HomeScreen` 在骨架屏结束（`ContentPhase != Loading`）时调用 `Activity.reportFullyDrawn()`，此前该指标始终缺失。
- **指标捕获率问题**：5 轮常只有 2~3 轮被采纳，其余被 `Skipping results from iter X, it didn't capture all metrics` 整轮丢弃（连 `timeToInitialDisplay` 一起丢）。属 `fullyDrawnReported` 与 launching 窗口的时序限制，去掉 Splash 150 ms 退出动画后由 2/5 提升到 3/5，无法彻底消除。**结论对比必须多次运行取中位，不能只看单次 P50。**
- 首屏网络耗时约 500 ms（`timeToFullDisplay - timeToInitialDisplay`）：
  - 进程启动早期预取第一页（`HomeFirstPagePrefetcher`，仅 LAUNCHER 冷启动触发，5 s TTL、失败回退、结果一次性消费）只回收约 20 ms；
  - 判定瓶颈在服务端往返本身，本地缓存方案（`RemoteMediator` + Room 新表 + 迁移）评估后**明确不做**。
- 首页其他改动：轮播（`LoopPager`）在列表滚动时暂停，避免自动动画与手势争抢帧预算。
- 真机（PGEM10 / Android 16）实测：`coldStartup` 与 `scrollHome` 均已跑通（`scrollHome` 5 轮 / 1m4s，`frameOverrunMs` 全程为负，无掉帧）；`warmStartup` 仍会卡死（多次复现，最长 24m55s 无产出）。
- **`warmStartup` 真机卡死根因（2026-10-21 定位）**：ColorOS 的 `OplusHansManager` 在 `pressHome()` 之后冻结了被测进程
  （logcat：`onUidHansStateChanged: uid = 10457, isFreeze = true, frozenStatus = 1`），随后宏基准的 bring-to-front 无法完成，
  `am instrument` 永久挂起，设备前台停在 Launcher、被测 Task 为 `visible=false`。
  关闭 AOSP 冻结开关（`settings put global cached_apps_freezer disabled`）只能让它多跑 1 轮（从"首轮即卡"变成"第 3 轮卡"），
  因为该 ROM 走的是自有 Hans 冻结通道，不受 AOSP 开关控制。这不是应用或用例代码的问题。
- 规避方式：真机只跑 `coldStartup` + `scrollHome`；温启动指标改用模拟器采集（已跑通，P50 334.62 ms）。
- **模拟器采集（2026-10-21，Pixel 10 Pro XL / API 37 / arm64）**：上述两条用例在模拟器上全部跑通，必要条件是三条同时满足：
  1. 变体只能用 `nonMinifiedRelease` —— `benchmarkRelease` 下 `AppSecurityGuard` 会把模拟器判为异常环境并 SIGKILL；
  2. `androidx.benchmark.skipOnEmulator` 已在 `benchmark/build.gradle.kts` 固化为 `false`（否则用例被 Assumption 跳过）；
  3. `androidx.benchmark.enabledRules` 必须用命令行覆盖为 `macrobenchmark`（该变体插件默认注入 `baselineprofile`，且不支持逗号叠加）：
     ```
     ./gradlew :benchmark:connectedNonMinifiedReleaseAndroidTest \
       -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=macrobenchmark \
       -Pandroid.testInstrumentationRunnerArguments.class="com.example.fragmject.benchmark.StartupBenchmark#warmStartup"
     ```
- 模拟器指标（Pixel 10 Pro XL / API 37 / `nonMinifiedRelease`，不可与真机横向比较，只作同环境回归基线）：
  - `coldStartup`：`timeToInitialDisplay` P50 1094.79 ms（5/5 有效样本）；
  - `warmStartup`：`timeToInitialDisplay` P50 334.62 ms（5/5 有效样本）；
  - `scrollHome`：`frameDurationCpuMs` P50 1.72 / P90 14.78 / P95 19.70 / P99 31.51；`frameOverrunMs` P50 −2.91 / P90 5.12 / P95 14.52 / P99 18.69（每轮约 71 帧）。
  - 两条启动用例在该环境都**捕获不到 `timeToFullDisplay`**：API 37 上 `fullyDrawnReported` 与 launching 窗口对不齐，指标整体缺失（缺失不会让用例失败，只是该 metric 为空）。
- **`reportFullyDrawn()` 的三条约束（踩坑记录）**：
  1. 不能在 `LaunchedEffect` 里直接调用 —— 此时内容尚未提交渲染，`StartupTimingQuery` 抛 `No RT frame slice associated with UI thread frame slice ends after reportFullyDrawn`，整条用例失败；
  2. 只等帧回调（`withFrameNanos`）仍不够：帧回调存在不代表该帧有渲染输出。当前实现用 `ViewTreeObserver.OnDrawListener` 等到真实绘制，再用 `View.post` 确保该帧已提交后上报；
  3. 必须**进程级只上报一次**：温启动复用同一进程，内容在 launch 之前就已绘制完成，此时上报必然匹配失败（模拟器 `warmStartup` 必现异常）。冷启动每轮新进程，不受影响。

### T-11 R8 mapping 归档

`release` 已开 `minifyEnabled = true`，但 `mapping.txt` 未纳入归档流程，线上崩溃栈无法还原。方案：CI 在 release 构建后把 `app/build/outputs/mapping/release/mapping.txt` 作为 artifact 保留，并按版本号归档。

- [x] 每次 release 构建都有 mapping 产物与版本号关联 —— `release-weekly.yml` 从 `config.properties` 读取 `versionName`，分别上传 `release-apk-<version>` 与 `r8-mapping-<version>`，保留 90 天；缺失时 `if-no-files-found: error` 直接失败

### T-12 本地复现与排障入口

README 覆盖了环境要求，但缺"CI 红了怎么在本地复现"。方案：在 README 增加一节，列出 CI 等价命令（含 `JAVA_HOME` 指向 JDK 21+、`lintDebug`、`updateLintBaseline` 的正确用法）。

- [x] README 有 "本地复现 CI" 小节（含 `JAVA_HOME` 设置、五条命令、三类常见报错的排障提示）

---

### T-14 `picture` 模块可测性

**现状** `PictureEditorViewModel#save(produce: () -> Bitmap?)` 的 API 面直接暴露 `android.graphics.Bitmap`，`ImageEditorEngine` 是**具体类**（KDoc 明确记录了"有意不做接口分离"），构造需要 `Context` + `ImageSaveCoordinator`。JVM 单测里造不出 `Bitmap`、也 fake 不了引擎，除"produce 返回 null"外无可测路径。

**方案**（二选一，需决策）

1. 在 `core:image-editor` 内抽出 `ImageEditorEngine` 接口 + `DefaultImageEditorEngine` 实现，feature 依赖接口。代价：多一层抽象，且仍无法在 JVM 侧构造 `Bitmap`，只能注入返回 null 的 fake；
2. 让 ViewModel 依赖一个 `Bitmap` 无关的门面（如 `suspend fun saveFromSource(produce: () -> Any?): SaveResult?` 不合适）——更务实的做法是把"保存"下沉到引擎，ViewModel 只发起并接收 `SaveResult`，`produce` 由引擎内部提供。

推荐方案 2 的变体：ViewModel 不再接收 `produce`，改为 `save()` 由引擎自身按 UI 层传入的编辑参数重建位图。收益是可测且职责更清晰，但涉及编辑态数据的传递方式，需要与 UI 层一起改。

- [x] 决策并实施 —— **采用方案 2**（见下方实施记录）
- [x] `PictureEditorViewModel` 至少覆盖 保存成功 / 保存失败 / 重复点击不重复保存 —— 实际 5 例，另补异常与取消

**实施记录（2026-10-20，方案 2）**

- `PictureEditorViewModel#save` 不再接收 `produce: () -> Bitmap?`，改为接收 `saveAction: suspend () -> SaveResult?`；ViewModel 只做防重入、`isSaving` 翻转与结果分发，异常映射 `onError`、取消原样重抛。
- 新增 `suspend fun saveBitmap(produce: () -> Bitmap?)`，UI 在 `saveAction` 内组合：`{ viewModel.saveBitmap { editorState.saveBitmap() } }`；`PictureEditorScreen` 与 `PictureClipContent` 两处调用点已同步。
- **必要的最小接口抽离**：`ImageEditorEngine` 由具体类改为接口，原实现更名 `DefaultImageEditorEngine`，新增 `ImageEditorModule` 用 `@Binds` 绑定。否则 ViewModel 构造依赖 `Context`，JVM 侧仍无法创建实例——这是方案 2「可测」承诺的前置条件。
- 新增 `PictureEditorViewModelTest`（5 例）：成功透传 `SaveResult`、返回 null 报错、抛异常转 `onError`、协程取消不降级为失败、保存中重复点击只执行一次。
- 验证：`:feature:picture:impl:testDebugUnitTest` 5/5；`:app:hiltJavaCompileDebug` 通过；`detekt` 通过。

### T-15 `authenticator` 模块可测性

**现状** `AuthenticatorViewModel` 直接调用 `SntpTimeSource.queryOffsetMillis()`（UDP）与 `System.currentTimeMillis()`，且 `uiState` 用 `stateIn(WhileSubscribed)` 绑定 ticker。没有时钟/时间源抽象，单测等于测系统时间与网络。

**方案**

1. 定义 `interface Clock { fun nowMillis(): Long }` 与 `interface TimeOffsetProvider { suspend fun offsetMillis(): Long? }`，生产实现委托 `System.currentTimeMillis` / `SntpTimeSource`，测试注入固定时钟与可控偏移；
2. TOTP 计算本身是纯函数，抽到 `core:domain` 或本模块的纯 Kotlin 文件，直接对已知向量（RFC 6238 测试用例）做断言——这部分**不需要任何抽象即可测**，应优先做；
3. ticker 用 `TestDispatcher` + `advanceTimeBy` 驱动。

- [x] TOTP 纯函数抽离并对 RFC 6238 向量断言 —— **早已完成**：`core:domain/otp/Totp.kt` 为纯函数，`TotpTest` 已覆盖 RFC 6238 附录 B 的 6 组向量、6 位截断、时间片稳定性与 counterOffset
- [x] 时钟/偏移抽象注入，ViewModel 覆盖 正常刷新 / SNTP 失败降级 / 页面不可见停止 ticker

**实施记录（2026-10-20）**

- 新增 `utils/TimeProviders.kt`：`Clock`（`fun interface`，`nowMillis()`）与 `TimeOffsetProvider`（`suspend offsetMillis(): Long?`）；生产实现 `SystemClock` / `SntpOffsetProvider` 分别委托 `System.currentTimeMillis()` 与 `SntpTimeSource`。漂移阈值提为同包顶层常量 `DRIFT_THRESHOLD_MS`，避免 ViewModel 依赖具体探测实现。
- 新增 `di/TimeModule.kt`（`@InstallIn(SingletonComponent::class)`）提供两个端口的 Hilt 绑定；`AuthenticatorViewModel` 构造注入 `Clock` 与 `TimeOffsetProvider`，源码中不再直接调用 `System.currentTimeMillis()` / `SntpTimeSource`。
- 新增 `AuthenticatorViewModelTest`（5 例）：固定时钟下验证码可复现、漂移超阈值展示、小漂移隐藏、SNTP 返回 `null` 静默降级、退订后 ticker 停止（ticker 跑在 `viewModelScope`，用 `MainDispatcherRule.testDispatcher.scheduler` 推进时间；两层 `WhileSubscribed(5s)` 需等约 10s 才完全静默）。
- 验证：`:feature:authenticator:impl:testDebugUnitTest` 5/5 通过；`:app:hiltJavaCompileDebug` 通过，确认新端口绑定完整。

---

## 五、不列入待办（已裁决，勿再提出）

| 事项 | 结论 |
|---|---|
| 仓库内 `keystore.properties` / `keystore.key` | **测试专用密钥，入库为有意设计**，不做轮换、不加 `.gitignore`、不清理历史 |
| `network_security_config.xml` 的 `cleartextTrafficPermitted="true"` | **业务需要**（第三方文章域名不可枚举，仅作用于 WebView），已在文件内局部 `tools:ignore="InsecureBaseConfiguration"`；改进方向是打开 http 页面时提示"页面未加密"，不是收紧配置 |
| `app/src/main/AndroidManifest.xml` 的 `allowBackup="false"` | 有意关闭，避免会话与本地数据被备份导出 |

---

## 六、建议排期

| 批次 | 内容 | 产出 |
|---|---|---|
| 第一批（1 周） | T-02、T-03 | CI 覆盖 lint 与打包，流水线可信 |
| 第二批（2–3 周） | T-01、T-04 | feature 层测试骨架 + 覆盖率可视化 |
| 第三批（1 个月） | T-05、T-06、T-07 | 大类拆分、静态分析、契约测试推广 |
| 持续 | T-08、T-10、T-11、T-12 | 工程习惯与发布工艺 |
