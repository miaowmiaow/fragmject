# DEX 壳交付说明（H6 / S0~S2）

> 面向：发布负责人、值班同学、后续维护者。
> 能力演进与实测明细见 [`hardening-assessment.md`](./hardening-assessment.md) 的 H6 小节；本文只讲**怎么用、怎么验、怎么回滚、出事怎么查**。
> 最后验证：2026-11-11，Android 16（真机 PGEM10 / API 36）与 Android 17（模拟器 / API 37 / 16 KB page）。
> 2026-11-11 起，安全库已从 `:app` 搬到顶层 **`:security`** 模块（`com.example.security`，so 名 `libappsecurity.so`）。

---

## 一、它挡什么、不挡什么

| 攻击动作 | 无壳 | 有壳 |
|---|---|---|
| `jadx` / `apktool d` 读业务代码 | 直接可读 | 只能看到 10 KB 的壳与密文载荷 |
| `apktool b` 改包再分发 | 几分钟 | 需先逆向壳并重新打包 |
| 批量扫描（字符串/URL/SDK 探测） | 命中 | 失效 |
| **内存 dump（Frida / BlackDex）** | — | **仍然能脱**，壳只挡静态分析 |
| **native so 被分析** | — | 不受保护（另有 H3：`.text/.rodata` CRC + 特征串下沉） |
| 服务端信任 | — | 无变化：鉴权与风控必须在服务端 |

前提：壳依赖 hidden API 反射（`LoadedApk.mClassLoader` / `mApplication` / `ActivityThread.mInitialApplication`）。已在 API 36/37 验证可写；**升级 Android 大版本必须复验**。

---

## 二、开关与产物（先看这一节，选错产物会得到装不上的包）

### 2.1 场景 → 完整命令 → 产物

所有命令都在**仓库根目录**执行，产物统一落在 `app/build/outputs/apk/release/`。

| 场景 | 完整命令 | 产物文件 | 可发布 |
|---|---|---|---|
| 常规 release（无加固） | `./gradlew :app:assembleRelease` | `app-release.apk` | ✅ |
| 仅字符串加密 | `./gradlew :app:assembleRelease -PenableStringFog=true` | `app-release.apk` | ✅ |
| 仅资源混淆 | `./gradlew :app:assembleRelease -PenableResObf=true` | `app-release-resobf.apk` | ✅ |
| 仅打壳 | `./gradlew :app:assembleRelease -PenableDexShell=true` | `app-release-shell.apk` | ✅（**只发它**） |
| 壳 + 资源混淆 | `./gradlew :app:assembleRelease -PenableDexShell=true -PenableResObf=true` | `app-release-shell.apk` | ✅（**只发它**） |
| 三项全开（= CI 配置） | `./gradlew :app:assembleRelease -PenableStringFog=true -PenableResObf=true -PenableDexShell=true` | `app-release-shell.apk` | ✅（**只发它**） |

### 2.2 发布用的完整命令（可直接复制）

```bash
cd /path/to/fragmject

# 全量加固（字符串加密 + 资源混淆 + DEX 壳）
./gradlew :app:assembleRelease \
  -PenableStringFog=true \
  -PenableResObf=true \
  -PenableDexShell=true

# 产物（只有这一个可以上传）
ls -l app/build/outputs/apk/release/app-release-shell.apk
```

需要**干净产物**（改了壳代码、或怀疑增量缓存有问题）时加 `clean`：

```bash
./gradlew clean :app:assembleRelease \
  -PenableStringFog=true -PenableResObf=true -PenableDexShell=true
```

壳相关构建缓存异常时，额外清掉 CMake 与壳 dex 生成目录：

```bash
rm -rf app/.cxx app/build/generated/shell
./gradlew :app:assembleRelease -PenableDexShell=true
```

### 2.3 可选参数

| 参数 | 作用 | 默认 |
|---|---|---|
| `-PstringFogKey=<int>` | 字符串加密密钥（同一次构建内所有类共用），换版本时可轮换 | `0x5A3C` |

```bash
# 指定密钥（十进制；示例 4660 == 0x1234）
./gradlew :app:assembleRelease -PenableStringFog=true -PstringFogKey=4660
```

签名摘要（H1）**没有开关**：构建期从 `keystore.properties` 自动计算并注入
`BuildConfig.EXPECTED_SIG_SHA256`，release 恒开。

> ⚠️ **开壳后 `app-release.apk` 与 `app-release-resobf.apk` 不可发布**：它们的 manifest 已被替换为
> `com.example.shell.StubApplication`，但里面没有壳 dex，装上必然启动即崩。
> 这是「manifest 占位符 + 后处理打壳」的必然结果——只要构建时带了 `-PenableDexShell`，
> 唯一有效产物就是 `app-release-shell.apk`。

**CI**：`.github/workflows/release-weekly.yml` 已开启全部三个开关（字符串加密 + 资源混淆 + 打壳），
上传的 artifact 就是 `app-release-shell.apk`，并在上传前校验：载荷存在、业务 dex 已抽走
（`classes.dex < 100 KB` 且无 `classes2.dex`）、资源短路径生效、签名证书与对齐。

---

## 三、构建图（改这些地方前先看）

```
:shell:assembleRelease            （纯 Java 壳模块，零依赖）
        │
        ▼
:app:generateShellDex             d8 → shell.dex（+ 打印 CRC 供日志核对）
        │                          ↑ preReleaseBuild / buildCMakeRelease 依赖它
        ▼
        │
        ▼
:app:packageRelease               app-release.apk（manifest 已指向 StubApplication）
        │
        ├──(可选) :app:obfuscateReleaseResources   aapt2 路径缩短 + 重签 → app-release-resobf.apk
        │
        ▼
:app:packShellRelease             载荷加密 + 换壳 dex + 对齐 + 重签 → app-release-shell.apk
```

要点：

1. **顺序不能反**：壳 dex 必须先于 native 编译（CRC 是编译期常量），再先于打包（要被塞进 APK）；
2. 资源混淆与打壳**已串行**（打壳消费 resobf 产物）。并行处理同一个 `app-release.apk` 会产出
   「manifest 指向壳、但没有壳 dex」的坏包；
3. 改了 `:shell` 代码后 CRC 会变，`generateShellDex` 会自动重算，无需手工同步；
4. native 已迁到 `:security/src/main/cpp`，改 CMake/so 名请看 `security/build.gradle.kts`。

---

## 四、运行时链路与三重校验

```
系统实例化 StubApplication（壳 dex）
  └─ attachBaseContext
       ├─ 读 assets/payload/dex-*.bin
       ├─ 校验 payload 整体 SHA-256（对应 assets/payload/sum.txt）      ← 校验 1
       ├─ 建 InMemoryDexClassLoader（先 XOR 还原，再 inflate）
       ├─ 校验自身 classes.dex 的 CRC32（期望值在 assets/payload/shell.txt） ← 校验 2
       ├─ 反射替换 LoadedApk.mClassLoader
       ├─ 用 payload loader 实例化真实 Application
       ├─ 替换 LoadedApk.mApplication / ActivityThread.mInitialApplication
       └─ 调用真实 Application.attachBaseContext
  └─ onCreate
       ├─ 再替换一次 application 引用（系统会覆盖一次）
       └─ 调用真实 Application.onCreate（Hilt 组件在此创建）
```

第三重校验是 H1 的 APK 签名（`SecurityGuard` 启动时比对证书摘要）。三者相互独立：

| 校验 | 失败日志 | 语义 |
|---|---|---|
| payload 摘要 | `DexShell: payload sum mismatch: payload modified` | payload 被替换 → 不加载 |
| 壳 dex CRC | `FragmjectSecurity: shell dex crc mismatch: actual=… expected=…` | 壳代码被替换 → 不加载 |
| APK 签名（H1） | `AppSecurity: security check hit: APK 签名异常` | 整包被重签 → 1.2s 后终止 |

---

## 五、上线检查清单（逐条打勾，缺一不可）

```bash
BT=~/Library/Android/sdk/build-tools/37.0.0
APK=app/build/outputs/apk/release/app-release-shell.apk
```

1. **产物正确**：`ls -l $APK` 存在，且**只上传它**；
2. **结构正确**：
   ```bash
   unzip -l $APK | grep -E " classes\.dex|assets/payload"
   # 期望：classes.dex 约 10 KB（不是几 MB）、存在 assets/payload/dex-*.bin 与 sum.txt
   ```
3. **签名正确**（与 H1 期望一致，否则装上也活不过 1.2s）：
   ```bash
   $BT/apksigner verify --print-certs $APK | grep "SHA-256 digest"
   ```
4. **对齐正确**（16 KB 设备必需）：
   ```bash
   $BT/zipalign -c -p 4 $APK && echo align-ok
   ```
5. **无业务明文**（抽查）：
   ```bash
   unzip -p $APK assets/payload/dex-0.bin | strings | grep -c wanandroid   # 期望 0
   ```
6. **装机启动**（三个日志同时出现才算通）：
   ```bash
   adb install -r $APK
   adb logcat -c && adb shell am start -n com.example.fragment.project/com.example.fragmject.app.MainActivity
   adb logcat -d -s DexShell:V FragmjectSecurity:V
   # 期望：shell dex check ok / payload sum ok (N chunks) / registered 12 methods
   ```
7. **冒烟 8 项**：首页列表与分页、登录与自动登录、收藏、搜索历史、WebView 详情、扫码、
   图片编辑、深链跳转（Nav3 typed routes）；
8. **门槛**：体积增量 < 1 MB、冷启动增量 < 150 ms（当前实测 +36.7 KB / +51 ms）；
9. **归档**：`mapping.txt` 与壳包同版本归档（壳包崩溃栈需配合它还原）。

---

## 六、回滚

| 场景 | 做法 | 已验证 |
|---|---|---|
| 线上包出问题 | 用同一套代码去掉 `-PenableDexShell` 重新出常规包，同签名覆盖安装 | ✅ 壳包 → 常规包 `install -r` 成功，用户数据保留 |
| 只想临时观察 | 不要开开关，普通 release 包行为与打壳前完全一致 | ✅ |
| 壳包内含的逻辑出错 | 回滚到上一个常规包；不要尝试在壳包里 patch | — |

注意：**壳包与常规包签名相同**，所以可以互相覆盖升级；但两者 dex 结构不同，
热修复类方案（Tinker 等）请勿混用。

---

## 七、排障手册（日志 → 原因 → 处理）

| 日志 | 原因 | 处理 |
|---|---|---|
| `payload sum mismatch: payload modified` | payload 被替换/打包不一致 | 重新出包；检查是否有其他任务改过 `assets/payload` |
| `shell dex crc mismatch: actual=… expected=…` | 壳 dex 被替换，或 `shell.txt` 与打包 dex 不是同一次生成 | 删 `app/build/generated/shell` 重新构建 |
| `no shell.txt, shell dex check skipped` | 打包任务没写入壳清单（旧产物） | 重新出包；确认 `packShellRelease` 跑过 |
| `JNI_OnLoad: RegisterNatives failed` | R8 裁掉了只在 native 侧按名注册的方法 | 检查 `:security/consumer-rules.pro` 的 `-keepclassmembers … SecurityNative { native <methods>; }` 是否被删 |
| `load appsecurity failed, fallback to JVM checks` | so 不在预期路径（普通包不会出现） | 壳包内正常应能加载；若出现，检查 APK 内 `lib/<abi>/libappsecurity.so` 是否被压缩（必须 STORED） |
| `Module with the Main dispatcher is missing` | `InMemoryDexClassLoader` 的 parent 用了 systemClassLoader | parent 必须是系统原本的 `PathClassLoader`（提供 `META-INF/services` 资源） |
| `Hilt Activity must be attached to an @HiltAndroidApp Application` | `mInitialApplication` 在 `makeApplication` 之后被系统覆盖 | 已在 `StubApplication.onCreate` 二次替换；复现说明该逻辑被删 |
| 启动直接 `ClassNotFoundException: com.google.mlkit…` | 壳安装失败后进程继续跑（fail-closed 的表现） | 看前面第一条 `shell install failed` 的真实原因 |
| `security guard skipped (debugBypass)` | 当前是 debug 包（宿主编译期传入 `BuildConfig.DEBUG`） | 正常行为；release 包不应出现 |
| `security check hit: 生产包被标记为可调试（疑似改包）` | 生产包 manifest 里被加了 `android:debuggable="true"`（apktool 改包常见手法） | 安全库判定为改包并终止；确认是否有人手工改过 manifest |
| `native kill failed, fallback to Process.killProcess` | JNI `killSelf` 抛异常（so 被替换/方法被裁剪） | Java 兜底仍会终止进程；需排查 so 完整性 |

常用命令：

```bash
adb logcat -d -s DexShell:V FragmjectSecurity:V AppSecurity:E
adb logcat -d -b crash | head -40
adb shell pidof com.example.fragment.project
```

---

## 八、维护须知

1. **AGP / R8 / aapt2 升级后必须重跑**「打壳构建 + 装机冒烟」。历史踩坑：
   `-keepclasseswithmembernames` 不阻止裁剪（native 方法被删 → RegisterNatives 失败）、
   proguard 注释只认 `#`、AGP 9 的 `FramesComputationMode` 枚举改名；
2. **keep 规则分两处**：
   - 安全库的规则在 `:security/consumer-rules.pro`（随 AAR 自动合并，宿主不用写）；
   - App 自身只剩一条 `-keepclassmembers class * extends android.app.Application { <init>(); }`
     （壳反射实例化真实 Application，R8 看不到调用点）；
3. `:shell` 模块必须保持**纯 Java、零依赖**：引入 Kotlin stdlib 会让壳 dex 膨胀数十倍；
   且 `:app` 不能依赖它（壳类一旦进业务 dex，壳就失效了）；
4. `minSdk` 必须 ≥ 26（`InMemoryDexClassLoader`）；
5. so 必须以 STORED 打包且 4 字节/页对齐，否则 `INSTALL_FAILED_INVALID_APK`（手工重打包时最容易踩）；
6. 包体优化经验：**先 deflate 再 XOR**。反过来（先 XOR）会让载荷变成高熵数据、zip 压不动，
   实测壳包从 11.70 MB 涨到 15.20 MB。
7. **`SecurityConfig.debugBypass` 必须由宿主传 `BuildConfig.DEBUG`**：
   壳包/常规包的 `debugBypass` 都是 false，因此 release 包若被改成 `debuggable`，会被判为改包并阻断
   （实测 5s 内进程消失）。若宿主漏传（默认 false），本地 debug 也会跑完整检测，表现为「开发时启动就闪退」。
8. **日志 TAG 已统一为 `AppSecurity`**（2026-11-12 起）：旧文档里的
   `AppSecurityGuard` 是历史输出，`adb logcat -s AppSecurity` 才是当前过滤条件。

---

## 九、已知限制（交付时如实告知）

1. **挡不住内存 dump**：脱壳机可直接 dump 解密后的 dex，壳只提高静态分析成本；
2. **崩溃栈不完整**：`mapping.txt` 只对应原始 dex，壳包崩溃需配合映射还原；
3. **载荷一次性全解**：当前 2 片一次性解压（约 +51 ms 启动开销），dex 超 20 MB 需改按需加载；
4. **依赖 hidden API**：Android 大版本升级需复验（S0 已在 API 36/37 验证）；
5. **不构成安全边界**：任何「客户端自证清白」都可被绕过，服务端校验不可省。
6. **壳与安全库是两层，失败语义不同**：壳负责「业务 dex 不可静态读」，安全库负责「运行时自查」。
   壳被换掉时由壳自身的 `shell.txt`/`payload sum` 拦截（fail-closed）；改签包由 H1 拦截；
   manifest 改 `debuggable` 由安全库的「改包判定」拦截。三者互不替代，
   但**都没有**防住内存 dump —— 见第 1 条。

---

## 十、验收记录（本机实测）

| 项 | 数据 |
|---|---|
| 壳 dex 体积 | 10,496 B |
| 载荷分片 | 2 片（约 1.8 MB + 1.8 MB 压缩后） |
| 壳包 vs 常规包体积 | +36.7 KB（+0.3%） |
| 冷启动（模拟器，5 次中位数） | 常规 933 ms / 壳包 984 ms（+51 ms，+5.5%） |
| 正常启动日志 | `shell dex check ok` / `payload sum ok (2 chunks)` / `registered 12 methods` |
| payload 篡改 | `payload sum mismatch` → 进程崩溃（fail-closed） |
| 壳 dex 篡改 | `shell dex crc mismatch: actual=97e55d8f expected=38ee772d` → 崩溃 |
| 回滚（壳包 → 常规包） | 同签名覆盖安装成功，数据保留 |
| 与资源混淆叠加 | `res/Dg` 短路径 + payload + 10 KB 壳 dex 同时存在，装机正常 |
| 安全库 P0 修复（2026-11-12） | manifest 置 `debuggable=true` 而 `BuildConfig.DEBUG=false`：命中「生产包被标记为可调试（疑似改包）」，5s 内进程消失 |
| 正常包回归（同日） | 仅 `security warn: 检测到模拟器环境`，进程存活，冷启动 1335ms |
| debug 包（同日） | `security guard skipped (debugBypass)`，`AppSec-Check` 线程 0 |
