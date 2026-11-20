# 导航分层与新增导航指南

本文档说明 `core:navigation-contract` 与 `core:navigation-runtime` 的分工，以及**新增一个 feature 导航**的标准步骤。

## 一、两个模块的分工

| 模块 | 定位 | 内容 |
|---|---|---|
| `core:navigation-contract` | **纯契约（端口）** | 语义 `Navigator` 接口、`Local*Navigator` CompositionLocal、`RequiresAuth` / `DetailPaneNavKey` / `FlowScopedNavKey` 标记接口 |
| `core:navigation-runtime` | **机制（运行时）** | `NavContentRegistry` / `NavContentContributor` 注册机制、`DetailPane` 渲染支持与 ViewModelStore |

**核心约束（R9，编译期强制）**：`feature:*:api` 只允许依赖 `navigation-contract`，不得依赖 `navigation-runtime`。因此：

- 声明 NavKey（含实现 `RequiresAuth` / `DetailPaneNavKey` 标记）→ 在 **api 模块**，只依赖 contract。
- 注册渲染器、流程作用域 → 在 **impl 模块**，依赖 runtime。

## 二、新增 feature 导航：4 步

以新增「打开收藏页」为例，覆盖跨域导航（其他 feature 想跳进来）与本 feature 内部渲染两种情形。

### 第 1 步：api 模块定义 NavKey

```kotlin
// feature/collection/api/.../CollectionNavKey.kt
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import com.example.fragmject.core.navigation.contract.RequiresAuth
import kotlinx.serialization.Serializable

@Serializable
object MyCollectNavKey : RequiresAuth, DetailPaneNavKey
```

- 需要登录 → 实现 `RequiresAuth`
- 大屏详情面板渲染（而非全屏推入）→ 实现 `DetailPaneNavKey`
- 属于同一条多步流程（如选图 → 预览 → 编辑）→ 实现 `FlowScopedNavKey`
- 三者都是 `core:navigation-contract` 里的纯标记接口；**未实现** `DetailPaneNavKey` 的即全屏页，
  请在 NavKey 的 KDoc 里写明「全屏页，故意不实现 DetailPaneNavKey」，避免后来者误以为是遗漏

### 第 2 步：impl 模块注册渲染器（NavContentContributor）

```kotlin
// feature/collection/impl/.../nav/CollectionNavContentContributor.kt
object CollectionNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<MyCollectNavKey> { navKey ->
            MyCollectScreen() // 渲染本路由对应的 Composable
        }
    }
}
```

```kotlin
// feature/collection/impl/.../di/CollectionNavModule.kt
@Module
@InstallIn(SingletonComponent::class)
object CollectionNavModule {
    @Provides
    @IntoSet
    fun provideCollectionNavContributor(): NavContentContributor = CollectionNavContentContributor
}
```

app 组合根通过 `Set<NavContentContributor>` 自动聚合所有贡献者，**无需显式 import 任何 feature**。

### 第 3 步（仅跨域导航需要）：定义语义 Navigator 契约

当**其他 feature** 需要跳进本 feature 时，为消除「feature 互相依赖 NavKey」，在 contract 定义语义接口：

```kotlin
// core/navigation-contract/.../contract/CollectionNavigator.kt
interface CollectionNavigator {
    // 注意：方法**不要写 `= Unit` 默认实现**——一旦给了默认实现，
    // 新增动作时漏改 AppXxxNavigator 也能编译通过，运行时表现为「点击无反应」。
    fun openMyCollect()
}
```

同时在 `Navigators.kt` 的 `Navigators` 聚合体与 `noop()` 中补充 `collection` 字段
（字段漏加编译器发现不了，由 `ArchitectureTest` 规则 20 兜底）。

新增一个语义动作共需改 4 处：接口 → `Navigators` 字段 → `noop()` 空实现 → `AppXxxNavigator`，
后两处漏改会直接编译失败。

### 第 4 步：app 组合根实现 Navigator 并绑定

```kotlin
// app/.../navigation/AppNavigators.kt
@Singleton
class AppCollectionNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : CollectionNavigator {
    override fun openMyCollect() = dispatcher.navigate(MyCollectNavKey)
}
```

```kotlin
// app/.../navigation/NavigationModule.kt
@Binds
abstract fun bindCollectionNavigator(impl: AppCollectionNavigator): CollectionNavigator
```

消费方在 Composable 中取用：

```kotlin
val collectionNavigator = LocalNavigators.current.collection
collectionNavigator.openMyCollect()
```

## 三、关键约束速查

- **api 模块**：只能依赖 `:core:navigation-contract`（+ `androidx.navigation3.runtime` 以使用 `NavKey`）；依赖 `:core:navigation-runtime` 会被 R9 守卫规则在编译期拦截。
- **impl 模块**：依赖 `:core:navigation-runtime` 注册渲染器/流程作用域；跨域导航只经 `LocalNavigators`，不直接 import 其他 feature 的 NavKey。
- **app 组合根**：唯一「语义动作 → 具体 NavKey」的映射点（`AppNavigators.kt`），也是唯一聚合所有 feature 的地方。
- **entry 内的 `rememberSaveable`**：只在 `NavEntry.contentKey` 跨进程稳定时才可靠，见 §四。

## 四、进程重建：`contentKey` 陷阱与「页面状态不能只活在进程里」

返回栈由 `rememberNavBackStack` 持久化，**进程被杀后能整栈恢复**，但恢复出来的只是一个空壳进程：
所有 `@Singleton` 流程状态都是新实例。两类东西必须分开处理。

### 4.1 `rememberSaveable` 依赖 `contentKey`，而 `object` NavKey 的默认值不稳定

`rememberSaveable` 的状态由 `SaveableStateHolder` 以 `NavEntry.contentKey` 为键保存。库的默认值是：

```kotlin
"$key:${key::class}"
```

- `data class` NavKey（如 `PicturePreviewNavKey(uris, selectable)`）→ `toString()` 是**结构化**的，跨进程稳定 ✅
- `object` NavKey（如 `PictureSelectorNavKey`）→ 走 `Any.toString()` = `类名@identityHash`，**每次进程重启都变号** ❌

后果是静默的：该 entry 里所有 `rememberSaveable` 整批丢失（不报错、不崩溃），
表现为「重建后某个标记位莫名回到初始值」。图片流程踩过一次：选择器用
`rememberSaveable flowReset` 区分「新流程 / 流程内返回」，键不稳定 → 重建后回到选择器被当成新流程
→ `reset()` 把刚恢复的选中态又清空。

对策在组合根 `AppNavGraph`：注册 entry 时传入 `clazzContentKey = ::stableEntryContentKey`——
单例 key 用类名（本身即唯一、跨进程稳定），带参数的 key 保持库默认的结构化字符串
（**不能**退化成类名，否则同类的多个路由会共用同一份保存状态）。

> 新增 `object` NavKey 时不需要额外操作（组合根统一处理）；但**不要**在 feature 里自行调用
> `addEntryProvider` / `entry(...)` 绕过 `NavContentRegistry`，那样会拿不到这个稳定的 contentKey。

### 4.2 恢复后 UI 依赖的状态，必须能自己重建

判断标准：**「重建后的这一帧要读它吗？」要读，就不能只放在进程内的 `HashMap`/`StateFlow` 里。**

两种可接受的重建来源：

| 来源 | 适用 | 例子 |
|---|---|---|
| NavKey 携带的参数 | 路由本身的身份（页面该显示什么） | `PicturePreviewNavKey.uris` 决定预览哪几张图 |
| 持久化（domain 端口 + KV/Room） | 流程中产生、但 NavKey 里没有的状态 | ① 编辑后的「原图 → 新图」替换关系；② 当前选中项列表 |

图片流程的两个具体落点，可作为参考实现：

- `PictureEditRepository`：旧图 → 新图（编辑保存后原图会被删除，返回栈里的 NavKey 仍持原始 uri，
  不落库就会去解码一张已删除的图）；
- `PictureSelectionRepository`：当前选中项（不落库则重建后「取消选择」退回「选择」、缩略图无勾选，
  点「完成」还会把空列表报给调用方）。

两条实现约定：

- **落库顺序 = 因果顺序**：替换关系必须先写库、再删原图；反过来会在两者之间被进程退出打断，
  丢掉「原图已删」的唯一证据；
- **归零时机由流程入口决定**：`PictureFlowState.reset()`（进入选择器）除了清内存，也要清持久记录，
  否则上一次流程的残留会在下次进程重建时被恢复出来。恢复动作本身必须幂等
  （「内存非空就跳过」），避免覆盖进程内更新的状态。

## 五、真机验证这类修复的最小手法

`am kill` 只杀后台进程，任务栈与已保存的 saved state 仍在，是模拟「进程重建」最贴近真实的方式：

```bash
adb shell input keyevent KEYCODE_HOME
adb shell am kill <applicationId>
adb shell monkey -p <applicationId> -c android.intent.category.LAUNCHER 1   # 从最近任务恢复
```

排查时注意两点：

- Coil 有磁盘缓存（本项目在 `Android/data/<pkg>/cache/coil`），本地图可能被缓存掩盖成「看起来正常」，
  验证前先清掉；
- `content://` 属于其它应用的媒体，删除会静默失败（分区存储），要复现「原图已删」的场景，
  请用本应用自己写入相册的图片。
