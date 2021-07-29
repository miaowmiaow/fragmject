# 前言
学习```Kotlin```有一段时间了，想写一个项目总结收获，就有了这个可能是东半球最简洁的玩安卓客户端，在此感谢[玩Android](https://www.wanandroid.com/) 的开放API。
# 简介
适合初学者入门的项目，通过对Kotlin的系统运用，实现的一个功能完备符合主流市场标准App。  
虽然本项目的定位是入门级，但是该有的知识点却一点不少，对理解其他项目设计思想和封装技巧也很有帮助。  
学习本项目你将有如下收获： 
- Kotlin实战（函数进阶，泛型，反射，协程...） 
- MVVM开发架构（ViewBinding，ViewModel，LiveData，RoomDatabase...）
- 单Activity多Fragment项目设计
- 暗夜模式
- 屏幕录制
- 字节码插桩
# 截图展示
| ![1.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621158973.png) | ![2.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155363.png) | ![3.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155387.png) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![4.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155408.png) | ![5.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155418.png) | ![6.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155439.png) |
# ViewBinding
通过视图绑定功能，您可以更轻松地编写可与视图交互的代码。在模块中启用视图绑定之后，系统会为该模块中的每个 XML 布局文件生成一个绑定类。绑定类的实例包含对在相应布局中具有 ID 的所有视图的直接引用。
### 与 findViewById 的区别
与使用 findViewById 相比，视图绑定具有一些很显著的优点：
- Null 安全：由于视图绑定会创建对视图的直接引用，因此不存在因视图 ID 无效而引发 Null 指针异常的风险。此外，如果视图仅出现在布局的某些配置中，则绑定类中包含其引用的字段会使用 @Nullable 标记。
- 类型安全：每个绑定类中的字段均具有与它们在 XML 文件中引用的视图相匹配的类型。这意味着不存在发生类转换异常的风险。
这些差异意味着布局和代码之间的不兼容将会导致构建在编译时（而非运行时）失败。
### 与 DataBinding 的对比
视图绑定和数据绑定均会生成可用于直接引用视图的绑定类。但是，视图绑定旨在处理更简单的用例，与数据绑定相比，具有以下优势：
- 更快的编译速度：视图绑定不需要处理注释，因此编译时间更短。
- 易于使用：视图绑定不需要特别标记的 XML 布局文件，因此在应用中采用速度更快。在模块中启用视图绑定后，它会自动应用于该模块的所有布局。
反过来，与数据绑定相比，视图绑定也具有以下限制：
- 视图绑定不支持布局变量或布局表达式，因此不能用于直接在 XML 布局文件中声明动态界面内容。
- 视图绑定不支持双向数据绑定。
考虑到这些因素，在某些情况下，最好在项目中同时使用视图绑定和数据绑定。您可以在需要高级功能的布局中使用数据绑定，而在不需要高级功能的布局中使用视图绑定。
# LiveData
LiveData 是一种可观察的数据存储器类。与常规的可观察类不同，LiveData 具有生命周期感知能力，意指它遵循其他应用组件（如 Activity、Fragment 或 Service）的生命周期。这种感知能力可确保 LiveData 仅更新处于活跃生命周期状态的应用组件观察者。
### 使用 LiveData 的优势:
- 不会发生内存泄漏，观察者会绑定到 Lifecycle 对象，并在其关联的生命周期遭到销毁后进行自我清理。
- 不会因 Activity 停止而导致崩溃，如果观察者的生命周期处于非活跃状态（如返回栈中的 Activity），则它不会接收任何 LiveData 事件。
- 不再需要手动处理生命周期，界面组件只是观察相关数据，不会停止或恢复观察。LiveData 将自动管理所有这些操作，因为它在观察时可以感知相关的生命周期状态变化。
- 数据始终保持最新状态，如果生命周期变为非活跃状态，它会在再次变为活跃状态时接收最新的数据。例如，曾经在后台的 Activity 会在返回前台后立即接收最新的数据。
- 适当的配置更改，如果由于配置更改（如设备旋转）而重新创建了 Activity 或 Fragment，它会立即接收最新的可用数据。
- 共享资源，您可以使用单例模式扩展 LiveData 对象以封装系统服务，以便在应用中共享它们。LiveData 对象连接到系统服务一次，然后需要相应资源的任何观察者只需观察 LiveData 对象。
# ViewModel
ViewModel 类旨在以注重生命周期的方式存储和管理界面相关的数据。ViewModel 类让数据可在发生屏幕旋转等配置更改后继续留存。
# 协程
协程是一种并发设计模式，您可以在 Android 平台上使用它来简化异步执行的代码。
### 协程的特点包括：
- 轻量：您可以在单个线程上运行多个协程，因为协程支持挂起，不会使正在运行协程的线程阻塞。挂起比阻塞节省内存，且支持多个并行操作。
- 内存泄漏更少：使用结构化并发机制在一个作用域内执行多项操作。
- 内置取消支持：取消操作会自动在运行中的整个协程层次结构内传播。
- Jetpack 集成：许多 Jetpack 库都包含提供全面协程支持的扩展。某些库还提供自己的协程作用域，可供您用于结构化并发。
# Fragment + LiveData + ViewModel + 协程
### 以项目中 MainFragment 为例
#### 1、MainViewModel 代码如下：
```
class MainViewModel :  ViewModel() {
    
    val hotKeyResult = MutableLiveData<HotKeyListBean>()

    // 获取热词接口
    fun getHotKey() {
        // 通过viewModelScope创建一个协程
        viewModelScope.launch {
            // 构建请求体，传入请求参数
            val request = HttpRequest("hotkey/json")
            // 以get方式发起网络请求
            val response = get<HotKeyListBean>(request)
            // 通过LiveData更新数据
            hotKeyResult.postValue(response)
        }
    }
    
}
```
#### 2、MainFragment 代码如下：
```
class MainFragment : Fragment() {

    // 使用 'by viewModels()' Kotlin属性委托获取 MainViewModel
    private val viewModel: MainViewModel by viewModels()
    private val hotKeyAdapter = HotKeyAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 观察 hotKeyResult 的变化来更新UI
        viewModel.hotKeyResult.observe(viewLifecycleOwner, { result ->
            result.data?.apply {
                if (result.errorCode == "0") {
                    hotKeyAdapter.setNewData(this)
                }
            }
        })
        // 调用获取热词接口
        viewModel.getHotKey()
    }

}
```
# 基于LiveData封装的消息总线LiveDataBus
LiveDataBus具有生命周期感知，在Android系统中使用调用者不需要调用反注册，相比EventBus和RxBus使用更为方便，并且没有内存泄漏风险。  
1、发送事件
```
   SimpleLiveBus.with<String>("key").postEvent("value")
```
2、接收事件
```
   SimpleLiveBus.with<String>("key").observe(viewLifecycleOwner, { it ->
        println(it)
   })
```
3、接收粘滞事件
```
   SimpleLiveBus.with<String>("key").observeSticky(viewLifecycleOwner, { it ->
        println(it)
   })
```
# 基于RoomDatabase封装的SimpleDBHelper
1、存储数据
 ```
    SimpleDBHelper.set(“key”, "value")
 ```
2、获取数据
  ```
     SimpleDBHelper.get(“key”)
  ```
# 字节码插桩
相关知识已发布在掘金上，可点击下面链接跳转查看
- [最通俗易懂的字节码插桩实战（Gradle + ASM）—— 优雅的打印方法执行时间](https://juejin.cn/post/6986848837797658637)
- [最通俗易懂的字节码插桩实战（Gradle + ASM）—— 自动埋点](https://juejin.cn/post/6985366891447451662)
# 主要开源框架
- [square/okhttp](https://github.com/square/okhttp)
- [square/retrofit](https://github.com/square/retrofit)
- [google/gson](https://github.com/google/gson)
- [bumptech/glide](https://github.com/bumptech/glide)
- [tencent/x5](https://x5.tencent.com/)
- [Meituan-Dianping/walle](https://github.com/Meituan-Dianping/walle)
# Thanks
  感谢所有优秀的开源项目 ^_^   
  如果喜欢的话希望给个 Star 或 Fork ^_^  
  谢谢~~  
# LICENSE
```
Copyright 2021 miaowmiaow

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
