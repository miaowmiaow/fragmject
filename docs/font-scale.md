# 应用内字体缩放（Font Scale）

> 状态：已实现并验收（2026-11-25）。设置页滑块 **13~20sp、1sp 步进**，拖动松手后全局生效，
> 不需要重启 Activity。本文记录**为什么这样实现**、**布局兼容改了什么**、
> 以及**如何证明没有截断**。

## 一、结论先行

| 问题 | 结论 |
|---|---|
| 能否改 `MaterialTheme.typography` 实现 | **不能**：全仓硬编码 `N.sp` 143 处，`MaterialTheme.typography` 只有 7 处，改 typography 只能覆盖 4.8% 的文字 |
| 唯一可行手段 | 覆盖 `LocalDensity.fontScale`（Compose 只有这一个全局乘法钩子） |
| 与系统字体缩放的关系 | **相乘**：`系统 fontScale × 应用倍率`，尊重无障碍设置 |
| 滑块单位 | **正文字号 sp**（13~20），不是倍率。用户对「16sp」有直觉，对「1.15x」没有 |
| 上限 20sp 的依据 | 20sp ≈ 1.43x，Roborazzi 在 1.0x / 1.28x / 1.43x 三档出图，6 个组件无硬裁 |
| 密集布局（日历） | **不叠加**应用倍率，只跟随系统字体（见 §4.5） |
| WebView 正文 | **未同步**（见 §八），属于已知缺口 |

## 二、为什么只能覆盖 `LocalDensity.fontScale`

### 2.1 数据

```
硬编码 "N.sp"               143 处
MaterialTheme.typography      7 处
```

项目绝大部分文字直接写 `fontSize = 14.sp`，定义在 `TextStyle` 里的极少。

### 2.2 三种方案对比

| 方案 | 覆盖范围 | 即时生效 | 代价 |
|---|---|---|---|
| **A. 覆盖 `LocalDensity(fontScale)`**（采用） | 所有 sp：143 处字面量 + 7 处 typography | ✅ 无需重建 Activity | 一个 Composable 包裹 + 一个参数 |
| B. 自定义 `Typography` | 仅 7 处 | ✅ | 成本低但**基本无效** |
| C. `Activity.applyOverrideConfiguration(fontScale)` | 全部（含非 Compose） | ❌ 必须 `recreate()` | 闪屏、丢状态，体验差 |

### 2.3 为什么不是「每处 +1sp」

Compose 的 `fontScale` 语义是**乘法**：`14.sp → 14 × fontScale`，没有加法钩子，`N.sp` 在编译期就是常量。

要做「每个字号都 +N sp」必须：

1. 在 `core:designsystem` 定义语义 token（title / body / label…）；
2. 把全仓 143 处裸 sp 换成 token；
3. token 内部读 `LocalFontOffset: TextUnit` 做 `base + offset`；
4. 重写 Material3 `Typography` 的 15 个 style，让组件内字号也走同一套偏移。

成本 3~5 天，且从此「禁止写裸 sp」要进架构守卫。当前用「滑块控制正文字号、内部换算成倍率」的近似方案：UI 上仍是 1sp 步进，实现是等比缩放。

## 三、实现链路

| 层 | 文件 | 职责 |
|---|---|---|
| domain | `core/domain/.../text/FontScale.kt` | 取值域（13~20）、默认 14、夹紧、`bodyFontScale(sp)` 换算 |
| domain | `core/domain/.../text/FontScaleState.kt` | 应用级 `StateFlow` 契约 |
| domain | `core/domain/.../repository/FontScaleRepository.kt` | 读写端口（缺键回退、越界夹紧由实现兜住） |
| data | `core/data-repository/.../FontScaleRepositoryImpl.kt` | 走通用 `KeyValueLocalDataSource`，key = `body_font_sp` |
| data | `core/data-repository/.../FontScaleStateImpl.kt` | `stateIn(appScope, Eagerly, 14f)`，照 `ThemeStateImpl` |
| designsystem | `core/designsystem/.../AppTheme.kt` | `fontScale` 参数 + `LocalDensity` 覆盖 |
| designsystem | `core/designsystem/.../LocalAppFontScale.kt` | 暴露应用倍率，供密集布局还原系统缩放 |
| feature | `feature/user/impl/.../setting/SettingScreen.kt` | `SettingFontScaleRow` 滑块 + 说明文案 |
| app | `app/.../MainActivity.kt` | 注入 `FontScaleState`，两处 `AppTheme` 传 `fontScale` |

核心那段（`AppTheme.kt`）：

```kotlin
// 只改 fontScale，density 原样保留：dp 尺寸（图标、间距、固定高度）不跟着变，
// 只有 sp 文字缩放。这就是「字号设置」与「整体缩放」的区别，别把 density 一起改。
val density = LocalDensity.current
val scaledDensity = remember(density, fontScale) {
    Density(density = density.density, fontScale = density.fontScale * fontScale)
}
CompositionLocalProvider(
    LocalDensity provides scaledDensity,
    LocalAppFontScale provides fontScale,
) { MaterialTheme(colorScheme = appColorScheme, content = content) }
```

## 四、五个关键决策

### 4.1 单位是 sp，不是倍率

落库存**字号**（`body_font_sp = "16.0"`），不存倍率。将来基准字号从 14 调到 15 时，历史设置仍然是「用户想要的字号」，不会整体偏移。

### 4.2 与系统字体相乘，而不是替换

`density.fontScale × 应用倍率`。如果直接替换系统值，用户把系统字体调大后进应用反而变小，违背无障碍预期。

代价是两个倍率会叠加（系统 1.3x × 应用 1.43x ≈ 1.86x），所以上限压在 20sp。

### 4.3 只改 `fontScale`，不动 `density`

`Density(density, fontScale)` 的 `density` 必须原样传回。改 density 会让图标、间距、圆角一起放大，那是「整体缩放」而不是「字号设置」。

### 4.4 应用级 `StateFlow`（Eagerly），保证重建后首帧准确

`MainActivity` 用冷 `Flow` 时只能给 `initialValue = 14f`，而切深色模式、分屏、折叠屏展开都会重建 Activity —— 首帧会按 1.0 倍渲染再跳大，用户看到「闪一下」。

`FontScaleStateImpl` 用 `SharingStarted.Eagerly` 常驻，重建后首帧就是真实值。冷启动首帧仍是默认值（Room 首发射是异步的），这是已知限制。

### 4.5 密集布局：`LocalAppFontScale` 排除应用倍率

日历这类**单元格固定比例 + 圆形裁剪**的表格，字号叠加会直接被裁（详见 §5.2）。它们读 `LocalAppFontScale` 把倍率还原：

```kotlin
val systemOnly = Density(density.density, density.fontScale / appScale)
```

这样**系统字体放大仍然生效**，只是不叠加应用内额外放大。设置页滑块下方有一行说明，避免用户以为设置失灵。

## 五、布局兼容（PR2）

### 5.1 三类固定尺寸

静态扫描 `height(4x~5x.dp)` 只能覆盖第一类。真正会出问题的是后两类。

| 类型 | 例子 | 风险 | 处理 |
|---|---|---|---|
| 显式固定高度 | 约 30 处 `height(45.dp)`（顶栏与列表行） | 低：45dp 容得下 24sp 单行（≈29dp 行高） | 未改（见 5.3） |
| **隐式固定高度** | 日历 `aspectRatio(1f)` + `clip(CircleShape)` | **高**：高度锁成列宽，文字放大即溢出 | 排除应用倍率（§4.5） |
| 小容器 | `ArrowRightItem` 描述 `Box(height(17.dp))` | 中：10sp → 12.9sp ≈ 18dp 被裁 | 改 `heightIn(min = 17.dp)` + `maxLines/overflow` |

### 5.2 日历：隐式固定高度的典型

**现象**（2026-11-25 真机，18sp）：农历/节日小字被圆形裁剪切掉一半，「元旦」只剩半截。

**根因**：

```kotlin
Column(
    modifier = Modifier
        .aspectRatio(1f)      // 高度 = 列宽（屏宽/7），与字号无关
        .clip(CircleShape)    // 超出圆形的部分直接切掉
) {
    Text(day, fontSize = 16.sp)
    Text(lunarLabel, fontSize = 10.sp, maxLines = 1, overflow = Ellipsis)
}
```

`maxLines` 只能管水平省略，管不了垂直裁切。

**为什么不改成「行高随字号增长」**：那需要同时改 `MonthPager` 的行高计算、单元格高度约束、选中态圆形（会变成椭圆），涉及视觉决策；而日历的定位是密集信息表，系统字体的放大已经足够覆盖无障碍需求。因此当前选择「不叠加应用倍率」，改动只在一处 `CompositionLocalProvider`。

### 5.3 Row 横向挤压：项目基础较好

逐项检查结果：

| 组件 | 结论 |
|---|---|
| `TabBar` | `SecondaryScrollableTabRow`，可横向滚动，不会截断 |
| `FeedCard` | 头像固定 + 文字 `weight(1f)`，两行都 `maxLines=1 + Ellipsis` |
| `ClearTextField` / `SettingRowLabel` / `AuthenticatorScreen` 验证码行 | 都有 `weight(1f)` |
| `ArrowRightItem` | 发现 17dp 固定容器问题 → 已修 |
| `TitleBar` | 长标题走 `basicMarquee()` 横向滚动，静态截图看似被切，运行时全文可见 |

结论：横向挤压不是主要风险源，**隐式固定高度才是**。

## 六、视觉基线（Roborazzi）

### 6.1 为什么需要

字号截断只有渲染出来才看得见。静态扫描只能覆盖 `height(dp)`，漏掉 `aspectRatio`、`clip`、`Spacer` 组合出来的隐式固定高度——日历就是被漏掉的那一类。

### 6.2 覆盖范围

JVM 渲染（Robolectric + `GraphicsMode.NATIVE`），不需要模拟器：

| 模块 | 组件 | 档位 |
|---|---|---|
| `core:ui` | `ArrowRightItem` | 1.0x / 1.28x / 1.43x |
| `core:ui` | `TitleBar` | 1.0x / 1.28x / 1.43x |
| `core:ui` | `FeedCard` | 1.0x / 1.43x |
| `core:ui` | Slider + Switch（主题色回归） | 1.0x 浅色 / 深色 |
| `feature:demo` | 日历 `DayContent` | 1.0x / 1.28x |
| `feature:picture` | `EditorTopBar` | 1.0x / 1.43x |
| `feature:auth` | `AccountForm` | 1.0x / 1.43x |

1.28x ≈ 18sp，1.43x ≈ 20sp。图落在各模块 `build/screenshots/`。

### 6.3 用法

```bash
# 出图（record 模式已写进 build.gradle 的 systemProperty）
./gradlew :core:ui:testDebugUnitTest --tests "*ScreenshotTest*"
```

CI 在 `ci.yml` 里以 `if: always()` 上传 `**/build/screenshots/`（保留 14 天），review 时直接看图。

### 6.4 为什么没有接像素 diff

Roborazzi 的 compare 模式对字体渲染、JDK 版本、系统字体的细微差异很敏感，容易出现「代码没动但像素差 1%」的红灯，维护成本高于收益。当前做法是「出图 + artifact 人工 review」，已经解决了「截断能被看见」这个核心问题。

## 七、验收清单

- [ ] 设置页拖动滑块，松手后全局文字立即变化，图标/间距不变
- [ ] 切深色模式、进分屏（Activity 重建）后字号不闪回 1.0 倍
- [ ] 20sp 下：首页列表、设置页、文章详情顶栏、登录页无硬裁
- [ ] 日历在 18sp / 20sp 下农历完整（不叠加应用倍率）
- [ ] 系统字体调大后：普通页面叠加放大、日历跟随系统但不再叠加
- [ ] 冷启动首帧字号正确（Room 异步首发射前为默认值）

## 八、已知边界

1. **WebView 正文不跟随**：文章详情是网页，`LocalDensity` 管不到。要同步需把倍率写进 `webSettings.textZoom`，并监听倍率变化更新已创建的 WebView（本次未做）。
2. **未覆盖的截图场景**：Authenticator 列表（`private` + 强依赖 ViewModel）、首页 `MainScreen`、文章详情整页、图片选择器网格。这些需要把 `private` 改 `internal` 或注入 fake ViewModel，属于对生产代码结构的侵入，本次未做。
3. **图标不随字号放大**：`Icon` 用 dp 尺寸，只放大文字会让图标相对变小。这是「字号设置」的预期行为，不是缺陷。
4. **系统字体极端放大时仍可能裁**：应用倍率被排除了，但系统字体调到 1.5x 以上时，日历正方形单元格仍可能不够（既有行为，不是本次引入）。

## 九、踩坑记录

| 现象 | 根因 | 现行防线 |
|---|---|---|
| 18sp 下日历农历被切半截 | `aspectRatio(1f)` + `clip(CircleShape)` 构成的**隐式**固定高度，静态 grep `height(dp)` 扫不到 | 日历排除应用倍率；新增 Roborazzi 截图覆盖 |
| `Slider(state = …)` 编译失败：`No parameter with name 'state' found` | 旧重载被标记 deprecated，但替代品（`SliderState` 重载）在当前 Material3 版本里**还不存在** | `@Suppress("DEPRECATION")` + 注释写明迁移方式（`LaunchedEffect` 单向同步） |
| 测试 APK 资源链接失败：`resource style/Theme.AppCompat.DayNight.NoActionBar not found` | `ui-test-manifest` 的 Activity 主题引用 AppCompat，模块没有该依赖 | `testImplementation(libs.appcompat)`，并按 `dependencies-policy.md §7` 登记到许可清单 |
| lint 报 `FileUtils.copy` 等 `NewApi` | 把方法抽成独立函数后，lint 无法跨方法推导 `SDK_INT` 分支 | 在方法上显式 `@RequiresApi(Q)`（与本次字号无关，但同属「拆分后守卫丢失」这一类） |
