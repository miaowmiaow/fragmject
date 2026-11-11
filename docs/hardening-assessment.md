# 加固能力评估：六项能力的难度与价值

本文对商业加固方案常见的六项能力（DEX 保护、数据文件加密、SO 保护、资源文件加密、反调试/反篡改、运行环境安全）逐项评估**实现难度**与**对本项目的实际价值**，给出做/不做的结论与优先级。

结论先行：这六项是加固厂商的**打包能力清单**，不是按收益排序的建议。对本项目而言，**只有两项值得投入**（签名保护 + SO 保护），两项近乎零收益（数据文件加密、assets 加密），一项成本极高收益有限（DEX 壳），一项已完成转入维护（运行环境检测）。

导航：
- 现有实现：`app/src/main/java/com/example/fragmject/app/security/`
  （入口 `SecurityGuard`、引擎 `SecurityEngine`、配置 `SecurityConfig`、事实抽象 `SecurityFacts`、
  native 桥 `SecurityNative`，以及 Detectors / Environment / Integrity / Signatures / Lifecycle 五个文件）
- 混淆基线：`app/proguard-rules.pro`、`app/build.gradle.kts`（AGP 9.5.1 / minSdk 26 / release 仅 arm64-v8a）

## 一、评估口径

**价值**（1~5）= 阻断的攻击面 × 攻击者动机 × 本项目资产价值。只看"能不能防住"，不看"厂商宣传的防护维度数量"。

**难度**（1~5）= 自研工作量 + 长期维护成本（每次 AGP / Android 大版本都要复验）+ 稳定性风险。

**性价比** = 价值 / 难度，用于排序；评分为主观估计，用于排序而非精确计量。

**攻击者模型**（本项目现实威胁）：

| 层级 | 画像 | 手段 | 占比 |
|---|---|---|---|
| L1 批量改包者 | 工作室 / 黑产 | `apktool d` → 改 `baseUrl`、插 SDK、去广告 → `apktool b` 重签分发 | 绝大多数 |
| L2 人工逆向 | 竞品 / 爬虫 / 破解爱好者 | jadx 读逻辑、Frida hook、抓包 | 少数 |
| L3 专业脱壳 | 安全研究 / 黑产上游 | 脱壳机（BlackDex / FRIDA-DEXDump）、IDA 分析 so | 极少 |

设计原则：**优先阻断 L1**（成本最低、覆盖面最大），对 L2 做成本抬高，对 L3 不做"防住"的幻想。

## 二、总览

| # | 能力 | 主要防谁 | 难度 | 价值 | 性价比 | 本项目结论 |
|---|---|---|---|---|---|---|
| 5 | 反调试 / 反篡改 / 签名保护 | L1 全部、L2 部分 | 2 | 5 | 2.5 | **签名校验已启用（H1）**，其余已就绪 |
| 3 | SO 文件保护 | L3 部分、L2 部分 | 3 | 4 | 1.33 | **做**（中等强度） |
| 6 | 运行环境安全（Root/模拟器） | 环境风险 | 2 | 3 | 1.5 | 已完成分级处置（H5），转维护 |
| 1 | DEX 文件保护（壳/分离/VMP） | L1 批量工具 | 5 | 2 | 0.4 | 条件触发（被二次打包再做） |
| 2 | 数据文件加密 | 本地取证（需 root） | 2 | 1.5 | 0.75 | **不做** |
| 4 | 资源文件加密（assets） | 资源盗用 | 2 | 1 | 0.5 | **不做**（release 无 assets）；已用资源混淆（H4）替代 |

排序（建议投入顺序）：**5 → 3 → 6（维护）→ 1（条件）→ 2 / 4（不做）**。

## 三、逐项分析

### 3.1 反调试、逆向、篡改（难度 2 / 价值 5）

**作用**：检测调试器附加（TracerPid、`Debug.isDebuggerConnected`）、Frida 注入（线程名 / 落地文件 / 端口）、APK 签名与产物完整性；命中即阻断。

**本项目现状**：检测链已完整（`SecurityGuard` + `SecurityDetectors` + `SecurityEnvironmentChecks` + `SecurityIntegrityChecks` + native `security.cpp`）。曾经的**关键空洞**——签名校验从不生效——已由 H1 补齐（见下）。

`SecurityIntegrityChecks.kt` 的签名校验入口保留「空集合 = 不启用」的降级语义：

```kotlin
internal fun verifySignature(facts: SecurityFacts, config: SecurityConfig): Boolean {
    if (config.expectedSignatureSha256.isEmpty()) return true
```

**H1 已落地（2026-11-11）**：
1. `app/build.gradle.kts` 在配置期用 `KeyStore`（JKS / PKCS12 自适应）读出签名证书，算 SHA-256 后
   经 `buildConfigField("String", "EXPECTED_SIG_SHA256", ...)` 注入；取不到（无 keystore / 别名或密码不对）
   时注入空串，构建不失败、运行期自动不启用。
2. 组合根 `FragmjectApplication` 把注入值按逗号拆成 `Set<String>` 传给 `SecurityConfig`
   （多签名 / 渠道重签就用逗号分隔）。
3. 运行期 `verifySignature` 按集合比对，忽略大小写与冒号；查询失败判失败（宁可误杀不放行）。
4. `nonMinifiedRelease` 采集变体仍整体跳过，不受影响。

**实测（PGEM10 / Android 16）**：

| 场景 | 结果 |
|---|---|
| 正常 release 包 | 启动正常、`security check hit` 计数 0、进程存活 ✅ |
| 用另一张证书 `apksigner` 改签后安装 | `E AppSecurityGuard: security check hit: APK 签名异常，应用即将关闭` → 1.2s 后 `jniKillSelf invoked` → 进程消失 ✅ |

**剩余可选项**：把命中改为「上报 + 阻断」双路径（当前只有阻断 + 本地 `Log.e`），需要服务端口径。

**风险**：误杀（多签名 APK、渠道重签）。国内渠道若二次签名，必须按渠道配置对应证书摘要，否则上线即崩。

### 3.2 SO 文件保护（难度 3 / 价值 4）

**作用**：so 代码加壳 / 动态混淆 / 防篡改，防止 so 被静态分析或运行时 patch。

**本项目现状（H3 后）**：`libfragmject_security.so` 已具备——release 默认 strip 调试符号、`JNI_OnLoad + RegisterNatives` 动态注册（`.dynsym` 只剩 `JNI_OnLoad`）、`.text` 与 `.rodata` 双段 CRC（`verifySelfSoTextCrc` / `verifySelfSoDataCrc`，两处独立判定）、Frida 线程/落地文件与 Root 路径的特征表已下沉 native（Kotlin 侧清零，实测 dex 中 0 命中）。

仍存在的缺口（已如实记录）：
- CRC32 非密码学哈希，理论上可碰撞；覆盖 `.text + .rodata`，但不覆盖 `.data/.got`（PLT/GOT hook 仍无效）；
- 特征串已用 `constexpr` 常量（`lit` 命名空间）存放，so 的 `.rodata` 中 `frida` / `magisk` / `/data/adb` / `/system/bin/su` 均 0 命中（详见 H3 小节）；
- Java 层 `block()` 仍是单点，patch 该分支可绕过（但 native CRC 与签名校验各自独立触发，不共用该分支）。

**为什么价值高**：本项目的 so 是**检测链的根**——所有"防调试"判定最终都在 native 侧。一旦 so 被 patch，"Root/模拟器/Frida 检测"整体失效。保护 so 等于保护检测能力本身。

**落地动作（1~2 周，按收益排序）**：
1. JNI 改 `RegisterNatives` 动态注册 + 方法名混淆，把 `-keep` 收窄到只剩类名（回收一部分 R8 收益）；
2. 关键常量（特征串、阈值）全部移到 native，Java 层不留明文；
3. 完整性校验从 CRC32 扩展到 `.text + .rodata`，并做**多处独立校验点**（避免 patch 一处全绕过）；
4. 如仍不够：so 加壳（自研 loader 解密 section）+ OLLVM 控制流平坦化。**代价大**：需自编译 NDK 工具链、CI 变重、崩溃栈几乎不可读，建议作为最后手段。

**不建议**：`so 动态混淆`（运行时自修改代码）——与 Android 14+ 的 W^X / 只读映射策略冲突，稳定性和兼容性风险远大于收益。

### 3.3 运行环境安全（难度 2 / 价值 3）

**作用**：识别 Root、模拟器、注入框架等高风险环境。

**本项目现状**：已实现且分层清晰（`SecurityEnvironmentChecks.kt`：su/Magisk/Zygisk 路径、Root 管理应用包名、`build.prop`、模拟器 Build 特征 + qemu 设备节点 + 多信号打分阈值 2；native 侧另有 maps 特征扫描）。

**H5 已完成分级处置（2026-11-11）**：
- 检测项统一返回 `SecurityHit(reason, severity)`，级别为 `WARN` / `CRITICAL`；
- Root / 模拟器由 `SecurityConfig.rootAction` / `emulatorAction` 三态控制（`IGNORE` / `WARN` / `BLOCK`），**默认 WARN**；
- 只有 `CRITICAL`（调试器、Frida、匿名段、CRC、签名、so/资源完整性）才终止进程；
- WARN 按原因去重（离开前台时清空），避免按秒刷日志；
- 新增环形缓冲（20 条）+ `SecurityGuard.securityEvents()`，供误杀取证。

**为什么默认 WARN**：定制 ROM、云手机、投屏、自动化测试都会命中这两项；一律终止等于用留存换"安全感"，而内容类应用的主链路（浏览/收藏）不因 Root 受影响。需要强约束的业务改成 `Action.BLOCK` 即可，无需改检测逻辑。

**结论**：不再增加检测项，投入转向"误报率数据 + 服务端风控联动"。

### 3.4 DEX 文件保护（难度 5 / 价值 2）

**作用**：DEX 加壳、DEX 分离、DEX 代码 VMP，防止 dex 被反编译/脱壳。

**三种子能力的真实可行性**：

| 子能力 | 自研可行性 | 说明 |
|---|---|---|
| DEX 加壳（抽 dex + 运行时解密） | 可行，2~4 周 | 与 Hilt **硬冲突**：Hilt 要求被系统托管的 Application 实现 `GeneratedComponentManager`，代理转发方案会直接抛异常，只能替换 `LoadedApk.mClassLoader`（hidden API，Android 14+ 需实测） |
| DEX 分离（拆多 dex + 动态加载） | 部分可行 | 与加壳同一套加载机制，收益更小（只是把部分代码后置加载） |
| DEX-VMP | **不可自研** | 需自定义指令集 + 解释器 + 与 D8/R8 协同，属商业能力（易盾/梆梆/几维），自研成本以年计 |

**价值判断为何只有 2**：
- 对 L1 有效（`apktool` 一把梭失效），但 L1 的动机主要是"改 baseUrl / 插 SDK"，这类需求在**加壳后仍需脱壳**才能完成，确实被挡住；
- 对 L3 **无效**：dex 解密后必须在内存交给 ART，BlackDex / FRIDA-DEXDump 可直接 dump 明文；
- 本项目无自研算法、无支付密钥、无授权逻辑，dex 里"值得保护的东西"很少——R8 混淆 + 字符串加密已覆盖大部分静态阅读需求。

**结论**：**暂不做**。触发条件：线上确认出现二次打包分发（渠道投诉 / 用户反馈异常包名签名）。届时做最小壳（加壳，不做 VMP），并与 Hilt 的兼容性单独验证。

**若要做的前置条件**（必须先满足，否则不要开工）：
- 接受 hidden API 反射路线（`LoadedApk.mClassLoader`）；
- 有独立分支 + 独立构建流水线（不改动日常 `assembleRelease`）；
- 有真机矩阵（Android 8 ~ 16）验证 ContentProvider 时序、Hilt 初始化、Baseline Profile 生效。

### 3.5 数据文件加密（难度 2 / 价值 1.5）

**作用**：本地文件、数据库、缓存加密，防止设备被 root 后数据被读取。

**本项目现状**：
- 登录态：Cookie 由系统 `CookieManager`（WebView）与 OkHttp `CookieJar` 管理，不在应用自建明文文件里；
- 结构化数据：Room（`AppDatabase` / `KVDatabase`）存收藏、搜索历史、KV 缓存；
- 缓存：图片/网页缓存（`CacheUtils`、`WebResourceCacheManager`）。

**为什么价值低**：
1. 加密密钥必须存在客户端，root 设备上依然可拿到（`EncryptedFile`/SQLCipher 的密钥最终来自 Android Keystore，但 Keystore 只在"硬件支持 + 未 root"时提供真实隔离）；
2. 数据本身低敏感：收藏与浏览历史不构成账号风险，且服务端才是权威数据源；
3. 引入 SQLCipher 会带来约 2~4 MB 包体、Room3 集成适配、迁移风险，收益与成本严重倒挂。

**结论**：**不做**。若未来出现"本地存 token / 生物识别凭据 / 支付凭证"，只对**该字段**用 Android Keystore 包装，不做全库加密。

### 3.6 资源文件加密（难度 2 / 价值 1）

**作用**：assets 内资源加密压缩，防止素材/配置被直接提取。

**本项目现状**：**release 包没有 assets**（`app/src/main/assets` 不存在；只有 `app/src/debug/assets/json` 与 `js`，用于 mock 数据，不进 release）。图片资源走网络 + Coil，不打包。

**结论**：**零收益，不做**。这一项对"素材型"应用（游戏、离线词典、漫画）价值高，对本项目不适用。

**替代建议（已落地为 H4）**：如果真实诉求是"防止 `res/` 里的 UI 布局与字符串被直接读"，那是**资源混淆**（`resources.arsc` 路径缩短），与 assets 加密是两件事，收益更高。AGP 9 无官方 AndResGuard 支持，最终采用 `aapt2 optimize --shorten-resource-paths` 后处理 + 重签，见 §七 H4。

## 四、落位建议（结合现有代码）

> 本节是最早的粗粒度阶段划分（P0~P3），**已被 §七「立项清单」取代**：编号、范围、改动清单、验收与门禁以 §七 为准，此处保留作为演进记录。

| 阶段 | 内容 | 落点 | 预估 | 验收 |
|---|---|---|---|---|
| P0 | 启用签名校验（构建期注入摘要） | `SecurityConfig` + `app/build.gradle.kts` + `SecurityIntegrityChecks` | 1 天 | release 包改签后启动即阻断；正常包不误杀 |
| P1 | SO 加固：动态注册 JNI、常量下沉、多处完整性校验 | `security.cpp` + `proguard-rules.pro` + `SecurityGuard` | 1~2 周 | `-keep` 收窄到类名；patch 单点不再绕过；真机冒烟 |
| P2 | 环境检测误报治理 + 分级处置 | `SecurityConfig` + 上报通道 | 持续 | 误报率可观测；主链路不被阻断 |
| P3 | DEX 最小壳（条件触发） | 新增 `:shell` 模块 + 后处理任务 | 2~4 周 | Hilt/Compose/Nav3/WebView 全量回归 |
| — | 数据文件加密、assets 加密 | — | — | 不做 |

## 五、复用已有结论：与三项具体手段的关系

本文件与"自研资源混淆 / DEX 壳 / 字符串加密"的评估对应关系：

| 手段 | 归属能力 | 难度 | 价值 | 结论 |
|---|---|---|---|---|
| 字符串加密 | 混淆（DEX 保护的弱化版） | 中 | 4 | **已完成（H2）**，release 加 `-PenableStringFog=true` 即生效 |
| 资源混淆（res） | 资源文件加密的替代 | 中 | 3 | 做，优先 `aapt2` 后处理路线 |
| DEX 壳 | DEX 文件保护 | 高 | 2 | 条件触发 |

## 六、需要守住的边界

1. **客户端加固不构成安全边界**：服务端鉴权、密钥管理、风控分级不能被壳或检测替代；客户端只提供"信号"，不做最终裁决。
2. **加固等级要与资产价值匹配**：本项目的资产是"客户端代码 + 用户体验"，不是资金或授权；过度加固会以启动耗时、崩溃归因、发版效率换取零边际安全。
3. **每项能力都要有回滚开关**：新增加固项一律进 `SecurityConfig`（布尔或 `Action` 三态），由宿主注入，便于线上出问题时灰度关闭，而不是回滚整个版本。

## 七、立项清单（按价值排序）

排序原则：先按价值降序，价值相同按难度升序；被其他立项依赖的先做；不立项项单列并写明触发条件。

| 编号 | 立项 | 归属能力 | 价值 | 难度 | 工期 | 前置依赖 | 状态 |
|---|---|---|---|---|---|---|---|
| **H1** | 签名保护（构建期注入摘要 + 多摘要匹配） | 反篡改 | 5 | 2 | 1.5 人日 | 无 | **已完成（2026-11-11）** |
| **H2** | 字符串加密（ASM 插桩 + 类内解密） | DEX 保护（弱化版） | 4 | 3 | 4 人日 | 无 | **已完成（2026-11-11）** |
| **H3** | SO 加固（常量下沉 + 完整性扩展） | SO 保护 | 4 | 3 | 5 人日 | 无（与 H2 可并行） | **已完成（2026-11-11）** |
| **H4** | 资源混淆（aapt2 路径缩短后处理） | 资源保护（替代 assets 加密） | 3 | 3 | 4 人日 | H1（重签必须同证书） | **已完成（2026-11-11）** |
| **H5** | 环境检测分级处置 + 误报观测 | 运行环境安全 | 3 | 2 | 3 人日 | 无 | **已完成（2026-11-11）** |
| **H6** | DEX 最小壳（不含 VMP） | DEX 保护 | 2 | 5 | 15~20 人日 | H1、H3 | **S0+S1 已验证可做（最小壳跑通）；S2 待触发** |
| **H7** | 库化前置改造（阶段一） | 交付形态 | 4 | 2 | 2 人日 | 无 | **已完成（见 §九）** |
| — | 数据文件加密 | 数据保护 | 1.5 | 2 | — | — | 不立项 |
| — | assets 资源加密 | 资源保护 | 1 | 2 | — | — | 不立项 |

合计立项投入：约 **20 人日**（不含 H6）。**H1~H5、H7 完成；H6 的 S0/S1 也已完成**（最小壳在 Android 16/17 跑通），S2 工程化待触发。

---

### H1 签名保护（价值 5 / 1.5 人日）— **已完成**

**目标**：release 包被改包重签后，启动 1.2s 内被阻断；正常包（含国内渠道重签，配置后）不受影响。

**范围**：只做"证书摘要比对"，不做 APK 全量签名校验（v2/v3 由系统保证，重复实现无收益）。

**实际改动清单**：

| 文件 | 改动 |
|---|---|
| `app/build.gradle.kts` | 新增 `signingCertSha256()`：用 `KeyStore`（JKS / PKCS12 自适应）读出签名证书后算 SHA-256；`buildConfigField("String", "EXPECTED_SIG_SHA256", ...)` 注入到所有变体；失败注入空串，不阻断构建 |
| `FragmjectApplication.kt` | `attachBaseContext` 里把 `BuildConfig.EXPECTED_SIG_SHA256` 按逗号拆成 `Set<String>` 传给 `SecurityConfig`（`String.toDigestSet()`） |
| `SecurityConfig.kt` | 复用已有 `expectedSignatureSha256: Set<String>`（空 = 不启用） |
| `SecurityIntegrityChecks.kt` | 复用已有实现：集合比对、忽略大小写与冒号；查询异常判失败 |

**任务实际耗时**：摘要计算 + 注入约 0.3 天，粘连与验证约 0.3 天（远低于 1.5 人日预估，因为 `SecurityConfig` / `verifySignature` 在阶段一已就位）。

**验收（已全部达成）**：
- ✅ `assembleRelease` 后 `BuildConfig.EXPECTED_SIG_SHA256 = "0768F702…C2AC"`，与 `apksigner verify --print-certs` 输出的证书摘要一致；
- ✅ 正常包真机启动：`security check hit` 计数 0、进程存活；
- ✅ 用另一张证书改签后安装：`security check hit: APK 签名异常，应用即将关闭` → `jniKillSelf invoked`（+1.2s）→ 进程消失；
- ✅ `nonMinifiedRelease` 仍整体跳过；
- ✅ `detekt` + `:app:testDebugUnitTest` 通过。

**风险与回滚**：
- 渠道重签未登记摘要 → 上线即全部误杀。**上线前必须逐渠道核对证书**，多渠道时把多个摘要写进 `EXPECTED_SIG_SHA256`（逗号分隔）；仍保留「空串 = 不启用」作为发版级回滚开关。
- 与 H4 的耦合：H4 重签必须使用**同一张证书**，否则 H1 会阻断自己的产物。

---

### H2 字符串加密（价值 4 / 4 人日）— **已完成**

**目标**：release dex 中不出现业务明文（URL、提示文案、检测特征串）；运行时透明解密，业务代码零改动。

**范围**：只加密 `LDC` 常量池字符串；不做反射/加密类名混淆（已由 R8 覆盖）。

**实际实现（与最初设计的差异）**：

最初设计是「集中字符串表 + 共享 `StringFog` 运行时类 + native 解密」。实施时改为**更简单、依赖更少的方案**：

1. **不建字符串表**：`LDC "明文"` 直接替换为 `LDC "密文"` + `invokestatic Owner.$fog$d(String)`；
2. **不建共享运行时类**：解密方法由插件**注入到每个被改写的类**里，避免新增 `core:stringfog` 模块和全模块依赖边（`§6.2` 门槛）；
3. **不调 native**：解密就是逐字符 XOR（`c[i] ^ (key + i)`），注入到类里约 30 行字节码。密钥在构建期由 `-PstringFogKey` 指定（默认 `0x5A3C`）。

**实际改动清单**：

| 文件 | 改动 |
|---|---|
| `gradle/libs.versions.toml` | 新增 `asm = "9.9"`、`asm` / `asm-tree` 两个构建期依赖 |
| `build-logic/convention/build.gradle.kts` | `implementation(libs.asm)` + `implementation(libs.asm.tree)` |
| `build-logic/.../FragmjectStringFogPlugin.kt`（新） | 开关 `-PenableStringFog`、密钥 `-PstringFogKey`；`transformClassesWith(FogVisitorFactory, InstrumentationScope.PROJECT)`；`COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES` |
| `FragmjectAndroidApplicationPlugin` / `...LibraryPlugin` | 各加一行挂载，保证 `:app` 与所有 library 模块同时插桩 |
| `app/src/main/cpp/security.cpp` | **未改**（方案调整后不需要 native 解密） |

**插桩规则**：
- 只处理 `com/example/fragmject/` 下的类；跳过 `app/security/`（检测特征串不改写）；
- 跳过长度 < 4 的常量（`":"`、`"/"` 这类多半是协议分隔符）；
- 跳过紧随其后的「按名查找」调用：`Class.forName/getMethod/getDeclaredMethod/getField/...`、
  `ClassLoader.loadClass/findClass`、`Enum.valueOf`、`Resources.getIdentifier`；
- **跳过 Hilt 生成类**（`_Hilt*`、`*LazyMapKey`）——见下面的踩坑记录。

**踩坑记录（三个，都值得写进经验）**：

| 问题 | 现象 | 处理 |
|---|---|---|
| `asm` 不含 tree API | `org.objectweb.asm.tree.*` 全部 unresolved | tree 是独立构件 `asm-tree`，必须单独依赖 |
| AGP 9 帧模式枚举改名 | `COMPUTE_FRAMES_FOR_ALL_METHODS` 不存在 | 实际只有 `COPY_FRAMES` / `..._INSTRUMENTED_METHODS` / `..._INSTRUMENTED_CLASSES` / `..._ALL_CLASSES`，用 `INSTRUMENTED_CLASSES`（同时覆盖新增的解密方法） |
| Hilt 依赖字符串被加密 | R8 警告 `Cannot determine what identifier string flows to ..._LazyMapKey.lazyClassKeyName`，会导致多模块 Hilt 运行时找不到模块 | `isInstrumentable` 跳过 `_Hilt*` 与 `*LazyMapKey` 类 |

**实测验收（release，`:app:assembleRelease -PenableStringFog=true`）**：

| 检查点 | 实测 | 结论 |
|---|---|---|
| dex 明文消失 | 未加密包 `strings classes*.dex \| grep wanandroid` = **7**；加密包 = **0** | ✅ |
| 解密方法注入 | `classes.dex` / `classes2.dex` 中均出现 `$fog$d` | ✅ |
| 运行期解密正确 | 模拟器启动后底部导航（dex 常量）渲染为 `首页/导航/项目/我的`，列表、图片正常 | ✅ |
| Hilt/Room/Compose 未被打断 | 启动无崩溃，数据正常加载；R8 的 `-identifiernamestring` 警告消失 | ✅ |
| 单测 & 静态检查 | `-PenableStringFog=true` 下 `testDebugUnitTest` 24 例全绿、`detekt` 通过 | ✅ |
| 体积 | 11.68 MB → **11.85 MB**（+164 KB，+1.4%） | ✅ 可接受 |
| 启动耗时 | 模拟器采样波动大（加密 1050/1088/1666ms，未加密 1340/1519ms），**未做严谨 A/B** | ⚠️ 未量化 |

**开关与回滚**：默认关闭，发布时显式加 `-PenableStringFog=true`；出问题去掉该参数重打包即可，产物行为与加密前完全一致。

**风险**：
- 漏白名单 → 运行时 `ClassNotFoundException` / 序列化失败，且错误点分散。排查方式：先关闭开关确认是插桩引起，再按崩溃栈定位那一处调用。
- 密钥是固定常量（可被逆向出）——这是「抬高门槛」而非「保密」，符合本项的定位。

---

### H3 SO 加固（价值 4 / 5 人日）— **已完成**

**目标**：patch 单点不再能关掉检测链；特征串不在 Java 层明文。

**范围**：不加壳、不上 OLLVM（见"不做"）。

**实际改动**：

| 文件 | 改动 |
|---|---|
| `security.cpp` | 新增 `countFridaThreads()` / `countFridaFiles()` / `countRootFiles()`：特征表全部搬到 native，Kotlin 只拿命中数 |
| `security.cpp` | 抽出 `verifySectionCrc(path, section)`，`.text` 与 `.rodata` 共用；新增 `verifySelfSoDataCrc` |
| `security.cpp` | JNI 注册从 8 个方法扩到 **12 个**（新增 3 个扫描 + 1 个 `.rodata` 校验） |
| `SecurityNative` / `NativeChecks` | 同步 4 个新方法；接口化便于单测 |
| `SecurityEnvironmentChecks` | 改为调用 native 命中数；Kotlin 侧删除 `FRIDA_THREAD_SIGNATURES` / `FRIDA_FILE_SIGNATURES` / `ROOT_FILE_PATHS` / `MAGISK_PATHS` 四张表 |
| `SecuritySignatures.kt` | 只剩 `FRIDA_PORTS`（Java 建 socket 需要）与 `ROOT_APP_PACKAGES`（PackageManager 查询需要） |
| `SecurityFacts` / `AndroidSecurityFacts` | 删除 `threadNames` / `isDirectory` / `listNames` 三个已无调用点的方法 |
| `SecurityIntegrityChecks` | 新增 `verifySelfSoDataIntegrity`（`.rodata` 独立校验） |
| `SecurityEngine` | 一次性完整性校验抽成 `startOneShotIntegrityChecks()` + `firstIntegrityFailure()`（同时解决 detekt 圈复杂度 15/15 超限），并接入第 4 项校验 |

**实测验收（模拟器 + 真机）**：

| 检查点 | 实测 | 结论 |
|---|---|---|
| JNI 注册数 | `JNI_OnLoad: registered 12 methods` | ✅ |
| `.rodata` 校验执行 | `self so .rodata crc file=5b082546 mem=5b082546 size=15820` | ✅ |
| dex 中特征串 | `frida-server` / `gum-js-loop` / `pool-frida` / `zygisk` / `/data/adb/magisk` / `/system/bin/su` **全部 0 命中**（改前可从 Java 表里读到） | ✅ |
| 真机无副作用 | 启动 572ms、`warn=0`、`hit=0`、进程存活 | ✅ |
| 模拟器分级仍生效 | `security warn: 检测到模拟器环境`，进程存活 | ✅ |
| 单测 / 静态检查 | 24 例全绿（Frida/Root 改为由 fake native 驱动）、`detekt` 通过 | ✅ |
| `nm -D` 只剩 `JNI_OnLoad` | H7 已达成 | ✅ |

**关于「二进制无明文」：曾经不成立，现已修复**

首轮 H3 完成后实测 `strings libfragmject_security.so` 仍能看到 `frida-server`、`/data/adb/magisk`、
`/system/bin/su`。根因是 `XS(...)` 宏在**运行期**构造 `XorString` 临时对象，编译器必须保留字面量作为
构造参数；只有 `constexpr` 对象形式才能真正消除。

修复（同日完成）：
1. 新增 `lit` 命名空间，把**特征名、路径、JNI 方法名与签名**统一声明为 `constexpr auto` 常量；
2. 45 处调用点改为 `lit::kXxx.str()`；特征表改为 `static const std::string`（只解码一次）；
3. 顺带把 C++ 侧含 `Frida` 的标识符改名（`countFridaThreads` → `countThreadSignals` 等），
   避免符号名泄露。

复测结果：`strings` 中 `frida` / `magisk` / `zygisk` / `lsposed` / `gum-js` / `linjector` /
`/data/adb` / `/system/bin/su` **全部 0 命中**；运行期 `registered 12 methods` 正常，
`.text`/`.rodata` CRC 一致。唯一残留是错误日志里的类名 `SecurityNative`（仅 `JNI_OnLoad` 失败分支）。

这条经验已写进 `security.cpp` 的 `XorString` 注释：**敏感串必须用 `lit` 常量，`XS` 只用于非敏感文案**。

**风险与回滚**：
- `.rodata` 校验在某些 ROM 上可能因段重定位/压缩导致误判 → 当前以「不一致才阻断」处理，若线上误杀可关掉该项；
- 动态注册失败会 `UnsatisfiedLinkError`，现有「降级为纯 JVM 检测」路径保留（已验证：删 lib 后仍能阻断）。

---

### H4 资源混淆（价值 3 / 4 人日）— **已完成**

**目标**：`resources.arsc` 中的资源路径被缩短为无意义短名，提高改包者定位资源的成本。

**范围**：只做路径缩短，不做资源名重命名（`R.xxx` 名保留，避免 `constraint_referenced_ids` 一类字符串引用失效）。

**实现方式（与最初计划不同）**：原计划 fork `AndResGuard`，实施时确认官方已归档、社区 fork 只到 AGP 8，于是改用 **AAPT2 自带的产物后处理**，完全不碰 AGP 内部 API。

**实际改动**：

| 文件 | 改动 |
|---|---|
| `build-logic/.../FragmjectResourceObfuscation.kt`（新） | `ObfuscateResourcesTask`：`aapt2 optimize --shorten-resource-paths` → `zipalign -p -f 4` → `apksigner` 重签；工具链从 `sdkComponents.sdkDirectory/build-tools` 取最高版本 |
| 同上 | 签名配置复用 `keystore.properties`（`storeFile` 相对 app 模块解析），不引入第二份签名来源 |
| `FragmjectAndroidApplicationPlugin` | 加一行 `configureResourceObfuscation()`，挂到 `packageRelease` 之后、`assembleRelease` 收尾 |

**实测验收（模拟器）**：

| 检查点 | 实测 | 结论 |
|---|---|---|
| 任务执行 | `> Task :app:obfuscateReleaseResources` + `资源混淆完成：…/app-release-resobf.apk（11462 KB）` | ✅ |
| 资源路径缩短 | `res/Dg`、`res/GQ`、`res/Mg`、`res/Nw`（原为 `res/drawable-*/…` 等长路径） | ✅ |
| 对齐 | `zipalign -c -p 4` 通过（含 so 页对齐） | ✅ |
| 证书 | `apksigner verify --print-certs` → `0768F702…C2AC`，与 H1 期望值一致 | ✅ |
| 安装启动 | 首页列表、Banner、图标、底部导航、图片全部正常；`crash=0` | ✅ |
| H1 联动 | 重签后 `security check hit` 计数 0（没有被自己的签名校验判成改包） | ✅ |
| 体积 | 11,699,019 → **11,737,987**（**+39 KB**） | ⚠️ 未下降：`zipalign -p` 页对齐的填充大于路径缩短的收益 |

**使用方式**：`./gradlew :app:assembleRelease -PenableResObf=true`，发布取 `app/build/outputs/apk/release/app-release-resobf.apk`。

**风险与回滚**：
- 重签链路出错会产出**不可安装/不可更新**的包 → 默认关闭，只在发布流水线显式开启，产物名带 `-resobf` 便于区分；
- 若出现 `getIdentifier("drawable","xxx")` 式按名取资源会被打断（当前仓库零调用，新增需登记）；
- 回滚：去掉 `-PenableResObf` 参数即可，行为与未混淆完全一致。

---

### H5 环境检测分级处置（价值 3 / 3 人日）— **已完成**

**目标**：Root/模拟器不再"一律杀进程"，改为分级处置；误报可观测、可关闭。

**实际改动清单**：

| 文件 | 改动 |
|---|---|
| `SecurityHit.kt`（新） | `SecurityHit(reason, severity)`、`Severity{WARN, CRITICAL}`、`Action{IGNORE, WARN, BLOCK}` |
| `SecurityConfig.kt` | `enforceRootDetection` / `enforceEmulatorDetection` 两个布尔 → `rootAction` / `emulatorAction` 三态，默认 `WARN`；`onHit` 回调类型改为 `(SecurityHit) -> Unit` |
| `SecurityDetectors.kt` | 全部判定函数返回 `SecurityHit?`；顺序调整为「调试器 → Frida → CRC → 端口 → Root/模拟器」，前四项固定 CRITICAL |
| `SecurityEngine.kt` | `block(hit)` / `warn(hit)` 分流；WARN 按原因去重（离开前台清空）；环形缓冲 20 条 + `recentEvents()` |
| `SecurityGuard.kt` | 新增公开 API `securityEvents(): List<String>`，供反馈导出 |

**实际验收（模拟器 + 真机）**：

| 场景 | 实测 | 结论 |
|---|---|---|
| 模拟器 release 启动 | `E AppSecurityGuard: security warn: 检测到模拟器环境`，**进程存活**，无 `check hit` | ✅ 不再误杀 |
| WARN 去重 | 启动 8s 内 warn 计数 = **1**（此前会每秒一条） | ✅ |
| 切后台再回前台 | warn 计数 1 → **2**（回前台再记一次），进程仍存活 | ✅ |
| CRITICAL 仍阻断 | 改签包：先 `warn`（模拟器）再 `check hit: APK 签名异常` → `jniKillSelf` → 进程消失 | ✅ 分级不影响阻断 |
| 真机（无风险特征） | warn = 0、hit = 0、冷启动 468ms | ✅ 行为不变 |
| 单测 | `SecurityDetectorsTest` **24 例**（新增：默认 WARN、BLOCK 升级 CRITICAL、IGNORE 不检测、CRITICAL 优先于 WARN） | ✅ |
| 静态检查 | `detekt` | ✅ |

**风险**：降级会被批改为"削弱防护"——依据是内容类应用主链路（浏览/收藏）不因 Root 阻断，误杀成本高于防护收益；需要强约束的版本把 `rootAction` / `emulatorAction` 改成 `Action.BLOCK` 即可。

---

### H6 DEX 最小壳（条件立项 / 15~20 人日）

> 交付与运维（开关、产物、上线检查清单、排障、回滚）见 [`dex-shell-handover.md`](./dex-shell-handover.md)。

**触发条件**（满足任一才开工）：线上确认出现二次打包分发；渠道投诉异常签名包；出现明确的"去广告改包"传播。

**S0 实测（2026-11-11，已完成第一步）**：

探针 `app/src/debug/.../devtools/ShellProbeProvider.kt`（仅 debug 变体，用 ContentProvider 复刻
「Application.onCreate 之前」的时序），真机 PGEM10 / Android 16（API 36）与模拟器
sdk_gphone16k_arm64 / Android 17（API 37，16 KB page）双端各跑一次：

| 检查项 | 真机 API 36 | 模拟器 API 37（16 KB） | 说明 |
|---|---|---|---|
| `InMemoryDexClassLoader` 从内存字节构造 | ✅ | ✅ | 壳的加载器基础；16 KB 页设备无障碍 |
| 用该 loader 解析真实类 | ✅ 7/7 | ✅ 7/7 | 覆盖 app / core / feature 三类包 |
| `ActivityThread.mBoundApplication` 反射 | ✅ 可读 | ✅ 可读 | hidden API 未被封禁 |
| `LoadedApk.mClassLoader` 读取 | ✅ `PathClassLoader` | ✅ `PathClassLoader` | |
| `LoadedApk.mClassLoader` **替换**并还原 | ✅ 替换/还原均成功 | ✅ | 替换后进程未异常 |
| 真实 `FragmjectApplication` 经 payload loader 类加载 | ✅ | ✅ | Hilt 生成的父类在类加载期初始化，未抛异常 |
| 真实 Application 构造（第二个实例，仅验证构造链） | ✅ | ✅ | 不等价于 Hilt 组件创建 |
| `ActivityThread.mInitialApplication` 可读 | ✅ | ✅ | 壳最终要替换它 |

结论（S0 第一步）：**两个最大疑点已排除** —— 16 KB 页设备上内存 dex 可用；Android 16/17 上
`mClassLoader` 仍可反射替换。

**S1 最小壳（2026-11-11，已完成并跑通）**：

新增 `:shell`（纯 Java、零依赖）与 `packShellRelease` 打包任务，链路为：
抽 `classes*.dex` → XOR 混淆写入 `assets/payload/dex-<n>.bin` → 删除原 dex →
塞入壳 `classes.dex` → `zipalign -p 4` → 同证书重签。
manifest 不改二进制：`android:name` 使用 `${applicationName}` 占位符，开壳时构建期替换为
`com.example.shell.StubApplication`。

| 检查点 | 实测 | 结论 |
|---|---|---|
| 壳包结构 | 唯一 `classes.dex` 仅 **6.4 KB**；业务 dex 变成 2 个载荷分片 | ✅ |
| 载荷是否可读 | `strings assets/payload/*.bin \| grep wanandroid` = **0** | ✅ 不能直接 jadx |
| 真机（PGEM10 / API 36） | 启动 735ms、进程存活、crash 0 | ✅ |
| 模拟器（API 37 / 16KB） | 启动 1343ms、首页完整渲染（Banner/列表/底部导航） | ✅ |
| Hilt 初始化 | `real application onCreate done`，Activity 正常注入 | ✅ |
| 安全模块在壳内可用 | `JNI_OnLoad: registered 12 methods`、双段 CRC 一致、`security warn` 正常 | ✅ |
| 签名校验 | 重签后与 H1 期望摘要一致，`check hit` = 0 | ✅ |

过程中修掉 4 个真实问题（都值得记住）：

| 现象 | 根因 | 处理 |
|---|---|---|
| `NoSuchMethodException: FragmjectApplication.<init>` | manifest 改指 stub 后，R8 看不到真实 Application 的实例化点，裁掉了无参构造 | proguard 增加 `-keepclassmembers class * extends android.app.Application { <init>(); }` |
| `UnsatisfiedLinkError: couldn't find libfragmject_security.so` | 业务 dex 由 `InMemoryDexClassLoader` 加载，它没有 APK 的 native 搜索路径 | `SecurityNative.load()` 增加 `nativeLibraryDir` 与 `apk!/lib/<abi>/` 兜底 |
| `Hilt Activity must be attached to an @HiltAndroidApp Application. Found: StubApplication` | 系统在 `makeApplication` 返回后会执行 `mInitialApplication = <stub>`，把壳在 `attachBaseContext` 里的替换覆盖掉 | 在 `StubApplication.onCreate` 再替换一次 `LoadedApk.mApplication` / `mInitialApplication` |
| `Module with the Main dispatcher is missing` | parent 用了 `systemClassLoader`，`ServiceLoader` 找不到 APK 里的 `META-INF/services` | parent 改为系统原本的 `PathClassLoader`（只提供资源，类仍从 payload 解析） |

**S2 第一阶段（2026-11-11，已完成）**：

1. **载荷完整性校验（fail-closed）**：打包时对全部密文载荷算 SHA-256 写入
   `assets/payload/sum.txt`，壳启动先比对再解密；不一致直接抛异常、不再加载业务代码。
   实测：翻转 `dex-0.bin` 一个字节并重签安装后，日志
   `payload sum mismatch: payload modified` → `IllegalStateException: payload integrity check failed`
   → 进程起不来（后续 provider 初始化因类缺失必然失败）。这正是期望行为。
2. **体积回归修复**：初版是「先 XOR 再让 zip 压缩」，XOR 把数据变成高熵，zip 完全压不动，
   壳包从 11.70 MB 涨到 **15.20 MB（+30%）**。改为 **deflate → XOR → STORED** 后
   `11.70 MB → 11.74 MB`，只 **+36.7 KB（+0.3%）**。
3. **启动开销 A/B（模拟器 API 37，各 5 次冷启动取中位数）**：
   常规包 **933 ms** / 壳包 **984 ms**，增量 **+51 ms（+5.5%）**，在立项时设定的
   「冷启动增量 < 150 ms」门槛内。

**S2 第二阶段（2026-11-11，已完成）：壳 dex 自校验**

此前唯一没有完整性保护的就是壳自己。做法是**把期望值锚定在 native**：

1. 新增 `generateShellDex` 任务：d8 产出壳 dex 后，把它整包的 **CRC32 写成 C 头文件**
   （`shell_crc.h`），并在构建图上排在 native 编译之前（`preReleaseBuild` 依赖它）；
2. `security.cpp` 用 `__has_include` 引入该常量，新增 JNI `verifyShellDexCrc(int)`；
   头文件缺失（未打壳）时返回 `-1`，即「不启用」，不影响普通构建；
3. 壳启动时（payload loader 建好之后）算自己 `classes.dex` 的 CRC32，通过反射调用
   `SecurityNative.verifyShellDexCrc(crc)`；返回 `1` 直接抛异常（fail-closed）。

为什么期望值不放 assets：壳 dex 被替换时，攻击者可以顺手改掉任何放在 APK 里的期望值；
常量在 so 里就必须同时改 so，而改 so 会撞上 `.text/.rodata` 自校验与 APK 签名校验。

实测：

| 场景 | 日志 | 结果 |
|---|---|---|
| 正常壳包 | `shell dex check=0`、`payload sum ok` | ✅ 启动正常 |
| `classes.dex` 追加 1 字节（CRC 变化、dex 仍可加载）并重签 | `FragmjectSecurity: shell dex crc mismatch: actual=97e55d8f expected=38ee772d` → `IllegalStateException: shell dex integrity check failed` | ✅ 进程崩溃（fail-closed） |
| 未打壳的常规包 | `verifyShellDexCrc` 返回 `-1` | ✅ 不启用、零影响 |

实现过程踩了 4 个坑（都记在代码注释里）：

| 现象 | 根因 | 处理 |
|---|---|---|
| `ClassNotFoundException: SecurityNative` | 校验放在了 payload loader 之前，壳自己的 classloader 只有壳 dex | 改到 loader 创建之后，`Class.forName(..., payloadLoader)` |
| `JNI_OnLoad: RegisterNatives failed` | `-keepclasseswithmembernames` 只「保名」不阻止裁剪，反射调用的 native 方法被 R8 删了 | 改用禁止裁剪的 `-keepclassmembers ... { native <methods>; }` |
| 构建失败（proguard 解析） | proguard 注释只能用 `#` | 改注释写法 |
| `shell dex check=-1`（校验没生效） | CMake 参数新增后旧 `.cxx` 缓存未失效，头文件没进 include 路径 | 清 `app/.cxx` 重新配置 |

**S2 剩余（仍未做）**：
- 反 dump（内存 dex 仍可被 Frida/BlackDex 脱，壳只挡静态分析）；
- 崩溃栈还原（mapping 只对应原始 dex，壳包崩溃需额外映射）；
- 更细的分片策略（当前 2 片一次性解压，>20MB dex 时需改按需加载）。

**仍未做（完整 H6 / S2 范围）**：
1. 载荷分片策略与多 dex 的启动耗时优化（当前 2 片一次性全解）；
2. 反 dump（内存 dex 仍是可 dump 的，壳只挡静态分析）；
3. 壳自身完整性校验、与 H1/H3 的联动（如壳被替换的检测）；
4. 崩溃栈还原（mapping 只对应原始 dex，壳包崩溃需额外映射）；
5. 体积/启动的严谨 A/B（当前样本只够证明「能跑」）。

**结论**：H6 的技术可行性已被 S1 证伪风险 → **证实可做**（Android 16/17 双端跑通），
剩余工作是工程化与对抗加固。是否继续投入取决于是否真的出现二次打包分发。

**S0 可行性验证（2~3 人日，先做，失败即终止）**：
- 写最小 demo：`StubApplication` + `InMemoryDexClassLoader` + 反射替换 `LoadedApk.mClassLoader`；
- 在 Android 8 / 12 / 14 / 16 真机验证；同时验证 Hilt `@HiltAndroidApp` 正常初始化；
- 结论写入本文档；不可行则终止 H6 或降级为"DEX 分离 + 关键类 native 化"。

**若可行，改动清单**：

| 文件 / 模块 | 改动 |
|---|---|
| `:shell`（新模块，纯 Java） | `StubApplication`、`DexPayloadLoader`（解密 + `InMemoryDexClassLoader`）、`ClassLoaderHooks` |
| `app/build.gradle.kts` | `packShellRelease` 后处理任务：抽 dex → AES-GCM 加密 → 写 `assets/payload/` → 删除原 dex → 放入壳 dex → 改 manifest `android:name` → 重签 |
| `app/src/main/AndroidManifest.xml` | `android:name` 指向 `StubApplication`（仅 release，由 manifest 合并/占位符控制） |
| `proguard-rules.pro` | 壳类 keep；保留 `mapping.txt` 与原始 dex 行号以便崩溃还原 |

**验收**：Hilt / Compose / Nav3 typed routes / WebView / Coil / Baseline Profile 全量回归；冷启动增量 < 150ms；`apktool d` 仅见壳；`strings` 无业务明文。

**风险**：hidden API 反射在厂商 ROM 上行为不一致；崩溃栈需二次还原；与 H1/H3 的阻断逻辑存在时序交叉，需单独回归。

---

### 不立项项（含触发条件）

| 项 | 不做的理由 | 重新评估的触发条件 |
|---|---|---|
| 数据文件加密 | 登录态在系统 `CookieManager` / OkHttp `CookieJar`；Room 仅存收藏与搜索历史；密钥仍在客户端 | 本地开始存 token / 支付凭证 / 生物识别数据 |
| assets 资源加密 | release 包无 assets（仅 `app/src/debug/assets` 的 mock 数据） | 引入离线素材包 / 词典 / 模型文件 |
| DEX-VMP | 需自定义指令集与解释器，属商业能力，自研成本以年计 | 有付费加固预算 |
| so 加壳 + OLLVM | 需自建 NDK 工具链，CI 变重、崩溃栈不可读 | so 中承载核心算法（当前仅有检测逻辑） |

## 八、排期与门禁

| 里程碑 | 内容 | 工期 | 门禁（每项完成时统一执行一次） |
|---|---|---|---|
| M1（第 1 周） | H1 + H5（**均已完成**） | 0（剩余） | `assembleRelease` + 改签验证 + 模拟器/真机验证 |
| M2（第 2 周） | H2（**已完成**） | 0（剩余） | `strings` 检查 + 模拟器/真机冒烟 |
| M3（第 3~4 周） | H3（**已完成**） | 0（剩余） | 双段 CRC + 特征表下沉 + 真机验证 |
| M4（第 5 周） | H4（**已完成**） | 0（剩余） | 路径缩短校验 + 对齐/证书校验 + 模拟器冒烟 |
| M5（条件） | H6 | 15~20 人日 | S0 结论 + 全量回归 |

**执行约束**：
1. 每项独立 PR、独立开关，**不合并**为一次大改动，便于定位与回滚；
2. 每项改完全部代码后**统一跑一次**构建 + 单测，不做阶段性反复编译；
3. 开关默认全部关闭，release 发布时逐项打开并记录版本号，便于线上问题二分定位；
4. 每项完成后回填"验收结果"到本文档对应小节，作为下次评审依据。

## 九、H7 完成记录：库化前置改造（阶段一）

**完成时间**：2026-11-10 ｜ **范围**：`:app/security` 内部解耦，**未**新建模块、**未**改包名与 so 名（留到阶段二搬家时一次改完，避免两次大范围改名）。

### 9.1 结构变化

| 改造前 | 改造后 | 职责 |
|---|---|---|
| `AppSecurityGuard`（object，359 行） | `SecurityGuard`（公开门面，~110 行） | `install()` / `isBlocked()` / `blockingMessage()` / 前后台回调 |
| 同上 | `SecurityEngine`（内部类，~300 行） | 轮询调度、退避、命中后的阻断与延迟终止 |
| 同上（native 声明） | `SecurityNative` + `NativeChecks`（接口） | `JNI_OnLoad` 动态注册；接口供单测替换 |
| `SecurityPolicy`（object 常量，130 行） | `SecurityConfig`（data class，公开） | 阈值与开关由宿主注入，默认值等价原常量 |
| 无 | `SecurityFacts` + `AndroidSecurityFacts` | 文件 / Build / PackageManager / Socket / APK 读取的唯一出口 |
| `SecurityDetectors`（扩展函数） | 同名文件（纯函数，参数注入） | 检测顺序与短路逻辑 |
| `SecurityEnvironmentChecks`（扩展函数） | 同名文件（纯函数） | Frida / Root / 模拟器 / build.prop |
| `SecurityIntegrityChecks`（扩展函数） | 同名文件（纯函数） | 签名、so CRC、资源摘要 |

### 9.2 五项改造要点

1. **配置对象化**：`SecurityConfig` 提供 15 个字段（`enabled` / `buildType` / `skipBuildTypes` /
   `expectedSignatureSha256` / `expectedResourceSha256` / `anonExecThreshold` /
   `enforceRootDetection` / `enforceEmulatorDetection` / 四组时间参数 / `blockGraceMs` / `onHit`），
   默认值与旧 `SecurityPolicy` 完全一致，因此**行为无变化**。
2. **入口统一为 `install()`**：`FragmjectApplication.attachBaseContext` 里一行
   `SecurityGuard.install(this, SecurityConfig(buildType = BuildConfig.BUILD_TYPE))`；重复调用幂等。
3. **去掉宿主 BuildConfig 依赖**：库侧不再读 `BuildConfig.DEBUG` / `BUILD_TYPE`，
   改用 `ApplicationInfo.FLAG_DEBUGGABLE` 判定可调试包；`BUILD_TYPE` 由组合根通过配置注入，
   用于跳过 `nonMinifiedRelease`（Baseline Profile 采集变体）。
4. **抽 `SecurityFacts`**：把「读文件 / Build 字段 / 包名 / 端口 / APK entry」收成一个接口，
   使检测项可在 JVM 单测里穷举分支（此前这些分支只能刷机手工验证）。
5. **JNI 改动态注册**：`JNI_OnLoad` + `RegisterNatives`，方法名与签名在 native 侧 XOR 存放；
   `CMakeLists.txt` 增加 `-fvisibility=hidden`；`proguard-rules.pro` 的 keep 从「整个类」
   收窄为 `-keepclasseswithmembernames ... SecurityNative { native <methods>; }`。

### 9.3 验证结果

| 验证项 | 命令 / 方法 | 结果 |
|---|---|---|
| Kotlin 编译 | `:app:testDebugUnitTest`（依赖 `compileDebugKotlin`） | 通过 |
| 新增单测 | `SecurityDetectorsTest` | **20 例全绿**（调试/Frida/Root/模拟器/端口/CRC/签名/资源/降级） |
| 架构规则 | `ArchitectureTest`（含 500 行上限、依赖守卫） | 通过 |
| 静态检查 | `./gradlew detekt`（根项目统一扫描） | 通过（首轮 9 条已修：`ModifierOrder`×8 改为 `external override`、`NestedBlockDepth`×1 抽 `writeApkEntry`） |
| native 编译 | `:app:externalNativeBuildDebug`（arm64-v8a + x86） | 通过 |
| so 导出符号 | `llvm-nm -D --defined-only libfragmject_security.so` | **只剩 `JNI_OnLoad`**，`Java_*` 全部消失 |
| 真机 + 模拟器运行 | 见 §9.4（PGEM10 / Android 16；sdk_gphone16k_arm64 / Android 17） | **通过**；模拟器阻断链路已闭环；root / frida 与 x86 降级路径暂缓 |

### 9.4 真机验证结果（2026-11-10，PGEM10 / Android 16 / arm64-v8a）

| # | 验证项 | 实测证据 | 结论 |
|---|---|---|---|
| 1 | Debug 包完全不启用 | `AppSec-Check` 线程 **0**；日志中无 `AppSecurityGuard` / `FragmjectSecurity` 任何输出（连 so 都未加载） | ✅ |
| 2 | Debug 可正常调试 | 断点调试无干扰（ptrace 占坑未执行） | ✅ |
| 3 | `nonMinifiedRelease` 跳过 | `security guard skipped for nonMinifiedRelease (collection build type)`；线程 0；so 未加载 | ✅ |
| 4 | native 动态注册 | `JNI_OnLoad: registered 8 methods`；无 `class not found` / `RegisterNatives failed` | ✅ |
| 5 | so 完整性与 libc CRC | `self so .text crc file=79b091eb mem=79b091eb`、`libc .text crc file=913b9cbd mem=913b9cbd`（一致） | ✅ |
| 6 | 轮询线程唯一 | `/proc/<pid>/task` 中 `AppSec-Check` 恰 **1** 个 | ✅ |
| 7 | Release 无误杀 | 5s 后进程存活，`security check hit` 计数 **0**，crash buffer 为空 | ✅ |
| 8 | 前后台联动 | 后台 40s 内 CRC 周期日志 **0** 条（轮询已暂停）；回前台后立即补做 CRC | ✅ |
| 9 | 冷启动 | `am start -W` TotalTime **426 / 416 / 410 ms**（无改动前基线，未做对比） | ✅ |
| 10 | 功能冒烟 | 首页列表、图片（Coil）、底部导航、返回栈均正常；单进程运行 | ✅ |
| 11 | 阻断链路 | 模拟器 / root / frida-server 三种触发器 | ✅ 模拟器已验（见 §9.5）；root / frida 暂缓 |
| 12 | 降级路径 | 「删 `lib/` 后重签」模拟 ABI 不匹配（见下） | ✅ |

**模拟器验证（sdk_gphone16k_arm64 / Android 17 / API 37 / arm64-v8a / 16 KB page）**

| 检查点 | 实测日志 | 结论 |
|---|---|---|
| so 在 16 KB page 镜像上可加载 | `nativeloader: ... libfragmject_security.so ... ok` + `JNI_OnLoad: registered 8 methods` | ✅ |
| 命中链路 | `E AppSecurityGuard: security check hit: 检测到模拟器环境` | ✅ |
| native 终止 | `W FragmjectSecurity: jniKillSelf invoked`（+1.2s） | ✅ |
| 进程确实消失 | `am start` 后 5s `pidof` 为空 | ✅ |
| 宽限时间准确 | 命中 17:46:07.894 → 终止 17:46:09.097 ≈ **1.20s** | ✅ |

**真机回归（PGEM10 / Android 16，白名单与日志修复后重新构建）**

浸泡条件：前台、亮屏、含 3 次列表滑动（触发 JIT），持续约 2 分钟。

| 检查点 | 实测 | 结论 |
|---|---|---|
| 进程稳定 | pid 全程未变，无重启 | ✅ |
| 轮询持续 | `libc .text crc` 3 轮：17:50:52 / 17:51:30 / 17:52:02（≈32s 间隔） | ✅ |
| 无误杀 | `security check hit` 计数 **0** | ✅ |
| 无可疑段 | `anon exec segment` 计数 **0** | ✅ |
| 线程唯一 | `AppSec-Check` 恰 1 个 | ✅ |
| 无崩溃 | crash buffer 0 行 | ✅ |
| 冷启动 | `am start -W` → **452 / 432 / 456 ms** | ✅ |

结论：ART JIT 白名单修复后，真机在 JIT 活跃场景下不再出现匿名段误杀；阻断链路（模拟器）与
不误杀（真机）两侧均已闭环。

**降级路径验证（模拟器，so 缺失场景）**

> 为什么不用 x86_64 模拟器：本机是 Apple M4（arm64），macOS on Apple Silicon 只支持 arm64-v8a
> 镜像，x86_64 镜像无法启动（`system-images/.../x86_64` 下只剩一个下载失败的空 `.installer` 目录）。
> 而 release 只编 arm64-v8a，真实设备永远不会 ABI 不匹配；真正会遇到的是「so 被删/损坏/被安全软件拦截」，
> 因此用「删 `lib/` 后重签」来构造更贴近现实。

构造方式：解包 release APK → 删除 `lib/` → 重新打包（`resources.arsc` 保持未压缩）→
`zipalign -p 4` → 用同一张证书 `apksigner sign`（避免签名不一致干扰结论）。

| 检查点 | 实测日志 | 结论 |
|---|---|---|
| 降级日志可见（Log.e，release 保留） | `E AppSecurityGuard: load fragmject_security failed, fallback to JVM checks` + `UnsatisfiedLinkError: dlopen failed: library "libfragmject_security.so" not found` | ✅ |
| JVM 侧检测仍生效 | `E AppSecurityGuard: security check hit: 检测到模拟器环境` | ✅ |
| 不走 native 终止 | 日志中**没有** `jniKillSelf invoked` | ✅ |
| Java 兜底终止有效 | 5s 后 `pidof` 为空 | ✅ |
| 混淆与 keep 生效 | 崩溃栈里 `SecurityGuard.install` 保留原名，内部 `SecurityNative.load` 显示为 `z16.李(SourceFile:50)` | ✅ |

### 9.5 真机 + 模拟器验证中发现并修复的问题

**问题**：首轮 debug 验证时日志出现 `JNI_OnLoad: registered 8 methods`——`install()` 里先
`SecurityNative().load()` 再交给 `start()`，可 `start()` 对 debug 包直接 return，于是 **debug 包白加载了一次 so**。
与「Debug 包不产生任何开销」的既有约定不符。

**修复**：`load()` 收进 `NativeChecks` 接口，调用点从 `SecurityGuard.install` 移到
`SecurityEngine.start()` 的跳过判定**之后**（debug / 采集变体判定通过才加载）。
复测确认 debug 包日志为空、线程为 0。

**问题 2（更严重）：release 下命中原因完全不可见。**
模拟器上阻断成功，但 logcat 里只有 native 的 `jniKillSelf invoked`，没有任何命中原因——
因为 `proguard-rules.pro` 的 `-assumenosideeffects` 把 `Log.v/d/i/w` 全部当作无副作用裁剪，
而 `block()` 用的正是 `Log.w`。后果是**线上误杀时没有任何可查证据**。

**修复**：安全关键路径改用 `Log.e`（不在裁剪列表内）：命中原因（`SecurityEngine.block`）、
so 加载失败降级（`SecurityNative.load`）、签名查询失败（`AndroidSecurityFacts`）。
复测后日志可见 `security check hit: 检测到模拟器环境`。

**问题 3（真实的误杀 bug）：ART JIT 匿名段白名单在新的段名格式下失效。**
首轮模拟器验证命中的不是「模拟器环境」，而是「异常匿名可执行内存段（疑似内存注入）」。
给判定加上段名日志后拿到根因：

```
anon exec segment: [anon_shmem:dalvik-zygote-jit-code-cache]
anon exec segment: [anon_shmem:dalvik-jit-code-cache]
security check hit: 检测到异常匿名可执行内存段（2 段，阈值 0）
```

`isKnownBenignAnonSegment` 只匹配 `[anon:dalvik-` 前缀，而 **Android 12+ 起 ART 的 JIT code cache
改名为 `[anon_shmem:dalvik-...]`**，于是 JIT 段被当成注入。真机上首轮未命中是因为 baseline profile
让代码在安装期就 AOT 完成、JIT cache 尚未映射；**只要运行中出现 JIT 编译，就会在某一轮检测被误杀**。

**修复**：白名单前缀补上 `[anon_shmem:dalvik-`；并在 native 侧对被计入的段打印
`anon exec segment: <name>`（debug 级，只打印可疑段、不打印白名单），便于后续调阈值/补白名单。
命中原因同时带上「实际段数 + 阈值」，如 `（2 段，阈值 0）`。
复测：同一环境不再命中匿名段，改由正确的「检测到模拟器环境」阻断。

### 9.6 阶段二待办（搬家）

1. 新建顶层 `:security`（`namespace = com.example.security`），搬 `Security*` 与 `cpp/`；
2. 同时把包名与 so 名中性化（`libfragmject_security.so` → 中性名），
   并同步 `JNI_OnLoad` 里的类名、`extractSelfSo` 的 entry 名、so 缓存键名；
3. 补 `consumer-rules.pro`（keep `SecurityNative` 的 native 方法 + 公开 API）；
4. 补 `maven-publish`、README（ABI / 16 KB page size / 签名配置说明）与最小示例；
5. `:app` 改为 `implementation(project(":security"))`，并在
   `FragmjectAndroidDependencyGuardPlugin.APP_ALLOWED_DEPENDENCIES` 增加一行。

