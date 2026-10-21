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
