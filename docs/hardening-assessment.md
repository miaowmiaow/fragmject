# 加固能力评估与决策记录

对商业加固方案六项能力（DEX 保护、数据文件加密、SO 保护、资源文件加密、反调试/反篡改、运行环境安全）的评估结论、人工边界与防回归清单。

结论先行：这六项是加固厂商的**打包能力清单**，不是按收益排序的建议。对本项目**只有两项值得投入**（签名保护 + SO 保护）；assets 加密近乎零收益；数据文件加密**全库不做、字段级按需**；DEX 壳**条件触发**；运行环境检测已完成、转维护。

**阅读方式**

- 各立项的现行状态以 §五 为准；**测试数量、覆盖范围等会随代码变化的数字不在本文维护**，以对应测试类为准（`security/src/test/.../SecurityDetectorsTest.kt`、`SecurityPolicyTest.kt`、`SecurityEventTest.kt`，以及 `app/src/test/.../architecture/` 下的 `Architecture*Test`）。
- 2026-11-10 ~ 11-20 的逐次评审记录与实测证据（原 1849 行）已归档到 git 历史：`git show 662ace90:docs/hardening-assessment.md`，按日期查某次改动用 `git log --follow -p -- docs/hardening-assessment.md`。本文 §六 把其中需要长期记住的部分压成了清单。
- 交付与运维（壳的开关、产物、排障、回滚）见 [`dex-shell-handover.md`](./dex-shell-handover.md)，不要在本文重复。

## 一、评估口径

**价值**（1~5）= 阻断的攻击面 × 攻击者动机 × 本项目资产价值。只看"能不能防住"，不看厂商宣传的防护维度数量。
**难度**（1~5）= 自研工作量 + 长期维护成本（每次 AGP / Android 大版本都要复验）+ 稳定性风险。
**性价比** = 价值 / 难度，仅用于排序，是主观估计。

| 层级 | 画像 | 手段 | 占比 |
|---|---|---|---|
| L1 批量改包者 | 工作室 / 黑产 | `apktool d` → 改 `baseUrl`、插 SDK、去广告 → `apktool b` 重签分发 | 绝大多数 |
| L2 人工逆向 | 竞品 / 爬虫 / 破解爱好者 | jadx 读逻辑、Frida hook、抓包 | 少数 |
| L3 专业脱壳 | 安全研究 / 黑产上游 | 脱壳机（BlackDex / FRIDA-DEXDump）、IDA 分析 so | 极少 |

设计原则：**优先阻断 L1**（成本最低、覆盖面最大），对 L2 抬高成本，对 L3 不做"防住"的幻想。

## 二、六项能力结论

| # | 能力 | 主要防谁 | 难度 | 价值 | 性价比 | 结论 |
|---|---|---|---|---|---|---|
| 5 | 反调试 / 反篡改 / 签名保护 | L1 全部、L2 部分 | 2 | 5 | 2.5 | **做**（H1 已完成） |
| 3 | SO 文件保护 | L3 部分、L2 部分 | 3 | 4 | 1.33 | **做**（H3 已完成，中等强度） |
| 6 | 运行环境安全（Root/模拟器） | 环境风险 | 2 | 3 | 1.5 | 已完成（H5），转维护 |
| 1 | DEX 文件保护（壳/分离/VMP） | L1 批量工具 | 5 | 2 | 0.4 | 条件触发（H6，被二次打包再做） |
| 2 | 数据文件加密 | 本地取证（需 root） | 2 | 1.5 | 0.75 | **全库不做**，字段级按需 |
| 4 | 资源文件加密（assets） | 资源盗用 | 2 | 1 | 0.5 | **不做**（release 无 assets），用资源混淆替代 |

## 三、逐项结论

### 3.1 反调试 / 逆向 / 篡改（价值 5，H1 已完成）

**现防线（四层，互相独立，不共用 Java 分支）**：
1. 期望证书摘要**编译进 so**（CMake 注入 `expected_sig.h`），native 用 `dladdr` 从 so 真实加载位置推导 APK 路径读签名块（优先 v3 再 v2）比对；
2. native 守护线程每 5s 复验，`kMismatch` 输出后 1200ms `SIGKILL`——删掉 Java 阻断分支不再能放行；
3. 库内**不存在**跳过开关（`debugBypass` / `skipBuildTypes` 已删除），是否安装由宿主在编译期按 `BuildConfig.DEBUG` 决定，release 中该分支被 R8 折叠；
4. `requireNative = true`：so 缺失或加载失败即阻断（`internal.native_unavailable`），无静默降级。

**残余边界（不可消除，只抬高成本）**：期望摘要在 so `.rodata` 中是明文，可改写后重打包；CRC 只能发现内存补丁，发现不了重打包；patch so 的成本高于改 smali，但未封死；真正的信任根在服务端 / 设备认证。
**风险**：渠道重签未登记摘要 → 上线即误杀。多渠道把多个摘要逗号分隔写入 `EXPECTED_SIG_SHA256`。

### 3.2 SO 文件保护（价值 4，H3 已完成）

**现状**：release 默认 strip；`JNI_OnLoad + RegisterNatives` 动态注册（导出符号只剩 `JNI_OnLoad`）；`.text` 与 `.rodata` **逐页 CRC**（页大小取 `sysconf(_SC_PAGESIZE)`，16 KB 设备自动 16 KB）并报出 `mismatched / first` 定位被改页；RWX 映射检测；Frida / Root 特征表下沉 native（Kotlin 侧清零）；GOT/PLT 劫持检测（22 个敏感导入，**默认关闭**）。

**已知缺口（如实）**：CRC32 非密码学哈希，理论上可构造逐页碰撞；不覆盖 `.data` / `.got`（故补了 GOT 校验，但默认关）；RWX 只抓"放宽权限后未恢复"的 hook，hook 结束恢复 r-x 就抓不到；Java `block()` 仍是单点，但 native 侧各项校验各自独立触发。

**为什么价值高**：so 是检测链的根 —— 所有"防调试"判定最终落在 native，保护 so 等于保护检测能力本身。
**不做**：so 加壳 + OLLVM（需自建 NDK 工具链，CI 变重、崩溃栈不可读）；so 运行期自修改（与 Android 14+ W^X 冲突）。

### 3.3 运行环境安全（价值 3，H5 已完成，转维护）

检测项统一返回 `SecurityHit(reason, severity)`，仅在 `CRITICAL`（调试器 / Frida / 匿名段 / CRC / 签名 / 完整性）终止进程；Root / 模拟器由 `rootAction` / `emulatorAction` 三态控制，**默认 WARN**。理由：定制 ROM、云手机、投屏、自动化测试都会命中，内容类应用主链路（浏览 / 收藏）不因 Root 受影响；需要强约束的业务改 `Action.BLOCK` 即可，不必改检测逻辑。

**结论**：不再增加检测项，投入转向"误报率数据 + 服务端风控联动"。

### 3.4 DEX 文件保护（价值 2，H6 条件立项，S0~S2 已完成）

可行性已被实测证实（不是空谈）：`InMemoryDexClassLoader` 从内存构造 + 反射替换 `LoadedApk.mClassLoader` 在 Android 16/17、16 KB 页设备上均可用；最小壳（抽 dex → deflate → XOR → STORED 载荷 + 壳 dex）实测壳包 +36.7 KB、冷启动 +51 ms，在"增量 < 150 ms"门槛内；载荷 SHA-256 校验与壳 dex CRC 校验均 fail-closed。

**已知硬约束**：与 Hilt 硬冲突（要求被系统托管的 Application 实现 `GeneratedComponentManager`），只能走替换 `mClassLoader` 的 hidden API 路线；DEX-VMP 不可自研（需自定义指令集 + 解释器 + 与 D8/R8 协同，属商业能力）。

**触发条件（满足任一才继续投入）**：线上确认出现二次打包分发；渠道投诉异常签名包；出现明确的"去广告改包"传播。
**若要做的前置条件**：接受 hidden API 反射路线；独立分支 + 独立流水线（不动日常 `assembleRelease`）；真机矩阵覆盖 Android 8~16 的 ContentProvider 时序、Hilt 初始化、Baseline Profile。

### 3.5 数据文件加密（价值 1.5，全库不做，字段级按需）

**已做**：OTP `secret`（Base32 种子）入库前经 `KeystoreSecretCipher` 加密（AES-256-GCM，密钥由 Keystore 持有、不导出、随机 IV），Room 表 `otp_account` 只存密文列。登录态在系统 `CookieManager` 与 OkHttp `CookieJar`，`User.token` 标 `@Ignore` 不落库。
**其余为明文**：Room（收藏 / 搜索历史 / 用户资料 / KV）与图片缓存；WebView 缓存的隔离靠"按身份分区 + 落盘闸门 + 登出清目录"（见 §六 #29~#33），不靠加密。
**为什么全库不做**：密钥必须存在客户端，root 设备仍可取；这些数据本身低敏感且服务端才是权威源；SQLCipher 约带来 2~4 MB 包体 + Room 集成与迁移风险，收益成本倒挂。
**规则**：将来出现"本地存 token / 支付凭证 / 生物识别"等新敏感字段，**只对该字段**用 Keystore 包装。

### 3.6 资源文件加密（价值 1，不做）

release 包**没有 assets**（`app/src/main/assets` 不存在，只有 `app/src/debug/assets` 的 mock 数据），图片走网络 + Coil 不打包。对素材型应用（游戏 / 离线词典 / 漫画）价值高，对本项目不适用。真实诉求若是"防止 `res/` 布局与字符串被读"，那是**资源混淆**（H4），与 assets 加密是两件事。

## 四、需要守住的边界

1. **客户端加固不构成安全边界**：服务端鉴权、密钥管理、风控分级不能被壳或检测替代；客户端只提供信号，不做最终裁决。
2. **加固等级要与资产价值匹配**：本项目的资产是"客户端代码 + 用户体验"，不是资金或授权；过度加固以启动耗时、崩溃归因、发版效率换零边际安全。
3. **每项能力都要有回滚开关**：新增加固项一律进 `SecurityConfig`（布尔或三态 `Action`），由宿主注入，线上出问题灰度关闭，而不是回滚整个版本。

## 五、立项状态

| 编号 | 立项 | 价值 | 开关 / 产物 | 状态 |
|---|---|---|---|---|
| H1 | 签名保护（摘要编译进 so + native 守护） | 5 | 默认生效；`SecurityConfig.expectedSignatureSha256` 仅作 JVM 回退 | 已完成 |
| H2 | 字符串加密（ASM 插桩 + 类内解密） | 4 | `-PenableStringFog=true`，密钥 `-PstringFogKey`（十进制） | 已完成 |
| H3 | SO 加固（特征下沉 + 逐页 CRC + RWX） | 4 | 默认生效；`SecurityCheck.PLT_HOOK` 默认关 | 已完成 |
| H4 | 资源混淆（aapt2 缩短路径后处理） | 3 | `-PenableResObf=true` → `app-release-resobf.apk` | 已完成 |
| H5 | 环境检测分级处置 | 3 | 默认 `Action.WARN`，可升 `BLOCK` | 已完成 |
| H6 | DEX 最小壳（不含 VMP） | 2 | `-PenableDexShell=true` → `app-release-shell.apk` | S0+S1+S2 已完成，条件立项 |
| H7 | 库化前置改造（迁入 `:security`） | 4 | `implementation(project(":security"))` | 已完成（阶段一，阶段二搬到顶层 `:security`） |

**不立项项与重新评估的触发条件**

| 项 | 不做的理由 | 触发条件 |
|---|---|---|
| 数据文件全库加密 | 见 §3.5 | 本地开始存 token / 支付凭证 / 生物识别数据 |
| assets 资源加密 | release 无 assets | 引入离线素材包 / 词典 / 模型文件 |
| DEX-VMP | 需自定义指令集与解释器，自研成本以年计 | 有付费加固预算 |
| so 加壳 + OLLVM | 需自建 NDK 工具链，CI 变重、崩溃栈不可读 | so 中承载核心算法（当前只有检测逻辑） |

**执行约束与门禁**（每项加固都适用）

1. 每项独立 PR、独立开关，**不合并**为一次大改动，便于定位与回滚；开关默认关闭，发布时逐项打开并记录版本号。
2. 改完全部代码后**统一跑一次**构建 + 单测，不做阶段性反复编译。
3. 壳类产物统一用 `scripts/verify-shell-apk.sh` 断言（结构 / 启动 / 四条日志 / 进程存活 / 无 `check hit`）；CI `security-matrix` 每周二在 x86_64 上跑同一脚本，构建带三项加固开关 + `-PenablePltCheck=true`。
4. API 面用 `:security:apiDump` / `:security:apiCheck`（基线 `security/api/security.api`），破坏性改动必须显式更新基线。
5. 门禁结论**以退出码为准**，不要 grep `BUILD SUCCESSFUL`。

## 六、踩坑与防回归清单

每条都是一个曾经真实发生、且不写下来就会重犯的缺陷。列是"现象 → 根因 → 现行防线"。

### 6.1 防护链自身

| # | 现象 | 根因 | 现行防线 |
|---|---|---|---|
| 1 | 改 manifest 置 `debuggable=true` 后全部检测失效（含签名校验） | `start()` 第一行 `if (isDebuggable) return`，位置在验签之前 | debuggable 只作"篡改判定"；生产包先同步验签 |
| 2 | apktool 把签名判定分支 nop 掉即绕过（已复现） | 阻断决策由 dex 中可删改的 Java 分支做出 | native 守护线程独立复验 + SIGKILL，不经 Java |
| 3 | `debugBypass` / `skipBuildTypes` 可被改写 | 开关是 dex 内编译期常量 | 库内删除全部跳过开关，改由宿主编译期决定是否 install |
| 4 | 在 `JNI_OnLoad` 里立即终止导致 12s 内被拉起 86 次 | 界面尚未显示，ActivityManager 反复重拉 | 延迟到 1200ms 后再 SIGKILL，只产生一次启动 |
| 5 | 删掉 Java 阻断分支后进程不退出且无任何信号 | so 加载失败走静默降级 | `requireNative = true`，缺失即 CRITICAL |
| 6 | 并发下 `onHit` 回调两次、重复调度 kill | `block()` 先判断后赋值 | `AtomicBoolean.compareAndSet` |
| 7 | `warn()` 刷屏，段数一变就重复 | 用含运行期数字的文案做去重键 | `SecurityHit.dedupeKey`（截断到全角括号） |
| 8 | native kill 抛异常导致连 `Process.killProcess` 都没执行 | 无兜底 | `try/finally` 保证 Java 兜底必执行 |
| 9 | 线上误杀时没有任何证据可查 | `-assumenosideeffects` 把 `Log.v/d/i/w` 当无副作用裁掉，`block()` 用的是 `Log.w` | 安全关键路径统一 `Log.e` |
| 10 | 命中后只是 1.2s 闪退，用户分不清崩溃与策略终止 | 宿主没有提示层 | `MainActivity` 最前读 `isBlocked()` 渲染"安全提示 + 原因"，跳过首页预热 |
| 11 | 每轮检测解析 3 次 `/proc/self/maps`，13~37 ms/轮 | 三处检测各自读一次 | native 100ms 短时快照共用；`injectionScanIntervalMs`（默认 5s）降频 → 稳态 5~8 ms/轮 |
| 12 | release 包 JIT 活跃时被"匿名可执行段"误杀 | Android 12+ ART JIT 段名从 `[anon:dalvik-` 改为 `[anon_shmem:dalvik-`，白名单前缀只匹配前者；且默认阈值 0 | 前缀补齐；默认 `anonExecThreshold=1`；命中文案带实际段数与阈值 |
| 13 | 安全检测"看起来有、实际永远不成立"（多次踩） | 例：RWX 探针按文件名在 maps 找 so，但 `extractNativeLibs=false` 时 so 直接从 APK mmap，永远匹配不到；逐页 CRC 检查的是 `crc32Update` 所在页而非被校验 section 首地址 | 改为"按代码地址查所属映射"、校验 `memBase`；**每个检测都必须有"故意触发"的探针验证** |
| 14 | 降级机制被反过来利用：hook 检测使其抛异常即关掉全部阻断 | 降级对所有命中生效 | 降级只放松 `env.*` 与内部事件，`integrity./injection./debugger./tamper.` 始终阻断 |
| 15 | GOT 校验首次运行 SIGSEGV | bionic 的 `DT_*` 可能是绝对地址也可能是相对基址偏移，把偏移当指针解引用即崩 | 统一归一化（`value < base → base + value`）+ 边界校验，越界 fail-open 返回 `-1` |

### 6.2 DEX 壳与插桩

| # | 现象 | 根因 | 现行防线 |
|---|---|---|---|
| 16 | 载荷按需加载（每分片一个 `InMemoryDexClassLoader`）启动即崩 `IllegalAccessError` | R8 访问检查以"包名 + ClassLoader"为界，拆 loader 后跨分片包级访问失效；分片解析还更慢 | **已实现并否决**，已回退。eager 单 loader 一次性解压是正解；将来单 dex 过大应走按 feature 动态加载，不要在壳内切分片 |
| 17 | 壳包体积从 11.70 MB 涨到 15.20 MB | 先 XOR 再 zip 压缩，高熵压不动 | 打包顺序固定为 **deflate → XOR → STORED**（+0.3%） |
| 18 | 冷启动每次一条 `Unable to instantiate appComponentFactory` | `androidx.core` 注入该属性指向的类随业务 dex 进了载荷，`LoadedApk.createAppFactory` 早于壳的 `attachBaseContext` | 打包器二进制剔除该属性；失败语义是 warn 不 fail，日志出现"manifest 未改写"要当回事 |
| 19 | Hilt 报 `must be attached to an @HiltAndroidApp Application. Found: StubApplication` | 系统在 `makeApplication` 返回后覆盖 `mInitialApplication` | 在 `StubApplication.onCreate` 再替换一次 `mApplication` / `mInitialApplication` |
| 20 | `UnsatisfiedLinkError: libappsecurity.so` 找不到 | 业务 dex 由内存 loader 加载，没有 APK 的 native 搜索路径 | `nativeLibraryDir` + `apk!/lib/<abi>/` 双兜底 |
| 21 | `NoSuchMethodException: <App>.<init>` | manifest 改指 stub 后 R8 看不到真实 Application 实例化点，裁掉了无参构造 | keep `* extends android.app.Application { <init>(); }` |
| 22 | `Module with the Main dispatcher is missing` | payload loader 的 parent 用 `systemClassLoader`，`ServiceLoader` 找不到 APK 内 `META-INF/services` | parent 改回系统原本的 `PathClassLoader` |
| 23 | `RegisterNatives failed` | `-keepclasseswithmembernames` 只保名不防裁剪，反射调用的 native 方法被删 | 改 `-keepclassmembers ... { native <methods>; }` |
| 24 | StringFog 加密了 Hilt 依赖字符串，多模块 Hilt 运行时找不到模块 | R8 警告 `Cannot determine what identifier string flows to ..._LazyMapKey` | 插桩跳过 `_Hilt*` 与 `*LazyMapKey`；`Class.forName`/`Enum.valueOf`/`getIdentifier` 后的常量也保留明文 |
| 25 | 打包端（Kotlin）与壳端（Java）两份编解码实现漂移，只有真机"装上即崩"才发现 | 对称协议跨模块跨语言各写一份 | 单一实现 `ShellCodec.java` + `ShellCodecParityTest` 现编译比对，改任一侧必须跑 `:build-logic:convention:test` |

### 6.3 构建、日志与缓存

| # | 现象 | 根因 | 现行防线 |
|---|---|---|---|
| 26 | CI 单测全红，堆栈不指向业务代码（`SharedSecrets` 不可访问） | Robolectric 4.17 + JDK 16+ 强封装；只加 `--add-opens` 不够，需要 `--add-exports jdk.internal.access` | 约定插件 `configureUnitTestJvmArgs()` 统一给所有 `Test` 任务加 opens + exports |
| 27 | `verifyModuleDependencies` 让整批配置缓存条目被丢弃 | `doLast` 在执行期遍历 `root.subprojects` / `configurations` | 前移到 `projectsEvaluated` 采集为 `@Input`，抽成任务类；两处刻意 fail-loud（图为空 / 文件无法归属即失败） |
| 28 | `-PstringFogKey=abc` 只抛 `NumberFormatException`，定位不到参数 | 直接 `map(String::toInt)` | `parseStringFogKey` 纯函数带属性名与取值口径报错；解析放在开关判断之前 |
| 29 | 自建 WebView 缓存凭空放行 `no-store` / `private` / `Vary` 响应 | 拦截发生在原生缓存之前，源站指令不再生效；资源 key 只含 URL，命中还无差别注入 `ACAO: *` | `WebCachePolicy` 纯函数闸门；只缓存 `GET`；`json` 移出白名单；CORS 只回显合法 `Origin`，其余降级 `*`、永不设 `Allow-Credentials` |
| 30 | 带凭据取到的资源被按 URL 落盘，之后任何页面命中 | 缓存键只按 URL + 透传全部请求头 | 条目分匿名 / `.cred`，`.cred` **仅同源可命中**；请求头改白名单转发；HTML 键含 Cookie |
| 31 | 登出后缓存与池化 WebView 残留上一账号内容 | 登出只清 Cookie 与用户表；`releaseAll()` 只处理"此刻在池里"的实例 | `SessionCacheCleaner` 在撤销 Cookie 之后清目录 + 释放池；池加**会话代次**，迟到 `recycle()` 的代次落后直接销毁不入池 |
| 32 | `Response.header()` 只返回最后一个同名头，`no-store` 被后面的 `max-age` 覆盖，静默放行 | HTTP 允许多行同名头 | `Headers.cacheRelevantHeaders()` 用 `values()` 全量取出后按 `", "` 合并（`Content-Type` 保持单值） |
| 33 | 密码明文进 logcat、进任务快照、进无障碍节点 | `HttpLoggingInterceptor.Level.BODY`；`rememberSaveable` 存密码；`TextField + PasswordVisualTransformation` | `DebugHttpLogger` 一律不打印请求体；表单改 `remember`（架构守卫规则 32 拦 `rememberSaveable` / `rememberTextFieldState`）；密码框改 `SecureTextField`（规则 33 禁 `PasswordVisualTransformation(`） |
| 34 | 剪贴板"30 秒自动清空"实际接近不生效（真机验证） | 切到后台后读取剪贴板得到 null，清除条件不成立 | 已删除该功能，只保留 `EXTRA_IS_SENSITIVE`（系统预览不显示明文） |

## 七、已知限制与已接受的空白

1. **CRC32 不是密码学哈希**，理论上可构造逐页碰撞；真正的锚是签名校验与 native 侧判定。
2. **复验间隔 5s**：替换后的篡改最多有 5s 暴露时间（启动时首次检查立即执行）。
3. **Hook 框架真实注入未验证**：当前无可用 root 设备（真机未 root，模拟器修补需要 rootAVD 的 macOS 工具链）。包名信号已用空壳探针在真机验证，`XposedBridge` 类信号只有单测覆盖；`frida-server` 常驻 + 非常规端口/改名的组合同样缺真机证据。恢复条件：具备可 root 的测试设备。
4. **`ROOT_APP_PACKAGES` 在 Android 11+ 未声明 `<queries>` 时查不到**（已决策暂不处理，`env.root` 默认 WARN，su 文件与 maps 检测不受影响）。
5. **落地文件检测只覆盖精确同名**（`/data/local/tmp` 在 Android 10+ 不可列目录，改为 `access()` 探测），带版本号的文件名不覆盖。
6. **方法入口完整性（Hook 检测第二期）未做**，依赖 ART 内部结构，需按 Android 版本实测后再定。
7. **`shell` 模块不做单元测试**（2026-11-19 决定）：其行为只通过真机回归验证，与 `core:model` 等无 JVM 可测分支的模块同口径，不为凑数补测。池化 WebView 的池逻辑同样无 JVM 单测（依赖真实 WebView）。
8. **ABI 矩阵不完整**：本地只能跑 arm64 镜像，x86_64 运行结果依赖 CI；32 位 ABI 不在矩阵内。
9. **Baseline Profile 采集会整体改写已跟踪文件**（`app/src/release/generated/baselineProfiles/*.txt`，约 5 万行 diff），提交前必须人工确认。
10. **Gradle `StartupBenchmark` 在真机（PGEM10）不可用**：perfetto `--background-wait` 无超时，被厂商冻结器冻结后永久挂起。真机启动耗时一律用 `am start -S -W`。
11. **采集变体（`nonMinifiedRelease`）会安装防护**，实测不影响 `generateBaselineProfile`；启动基准参考值（release 真机冷启动 ~717 ms 中位数）为含守卫的上界，无对照组。
