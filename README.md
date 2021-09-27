## 前言
学习```Kotlin```有一段时间了，想写一个项目总结收获，就有了这个可能是东半球最简洁的玩安卓客户端，在此感谢[玩Android](https://www.wanandroid.com/) 的开放API。
## 简介
适合初学者入门的项目，通过对Kotlin的系统运用，实现的一个功能完备符合主流市场标准App。  
虽然本项目的定位是入门级，但是该有的知识点却一点不少，对理解其他项目设计思想和封装技巧也很有帮助。  
学习本项目你将有如下收获： 
- Kotlin实战（函数进阶，泛型，反射，协程...） 
- MVVM开发架构（ViewBinding，ViewModel，LiveData，RoomDatabase...）
- 单Activity多Fragment项目设计
- 暗夜模式
- 屏幕录制
- 字节码插桩
## 截图展示
| ![1.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621158973.png) | ![2.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155363.png) | ![3.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155387.png) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![4.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155408.png) | ![5.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155418.png) | ![6.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155439.png) |
## 项目目录结构
```
├── app                                  app
|   └── src 
|       └── main 
|       |   └── java                     源码目录
|       |       ├── activity             Activity目录
|       |       |   └── MainActivity     项目唯一Activity
|       |       ├── adapter              Adapter目录
|       |       ├── fragment             Fragment目录
|       |       ├── model                ViewModel目录
|       |       └── App                  Application
|       | 
|       └── build.gradle                 模块构建配置
|       └── channel                      渠道配置文件
|       └── dictionary                   自定义混淆字典
|       └── proguard-rules.pro           代码混淆配置文件
| 
├── library-base                         基础library（存放通用的封装源码）
|   └── src 
|       └── main 
|       |   ├── assets                   原生文件目录
|       |   └── java                     源码目录
|       |       ├── bus                  消息总线目录
|       |       ├── dialog               Dialog目录
|       |       ├── http                 网络请求目录
|       |       ├── provider             ContentProvider目录
|       |       ├── utils                工具类目录
|       |       └── view                 自定义view目录
|       | 
|       └── build.gradle                 模块构建配置
| 
├── library-common                       公共library（存放各个 module 公共的源码及资源）
|   └── src 
|       └── main 
|       |   └── java                     源码目录
|       |       ├── bean                 实体类目录
|       |       └── constant             常量配置目录
|       | 
|       └── build.gradle                 模块构建配置
| 
├── miaow-picture                        图片编辑器（目录同app，不再展开）
├── module-faq                           问答模块（目录同app，不再展开）
├── module-home                          首页模块（目录同app，不再展开）
├── module-navigation                    导航模块（目录同app，不再展开）
├── module-project                       项目模块（目录同app，不再展开）
├── module-system                        体系模块（目录同app，不再展开）
├── module-user                          用户模块（目录同app，不再展开）
| 
├── plugin-statistic                     统计插件模块
|   └── src 
|       └── main 
|           └── groovy                   源码目录
|           |   ├── bp                   埋点统计目录
|           |   └── mt                   耗时统计目录
|           | 
|           └── resources                配置目录
|               └── statistic.properties 插件配置
| 
├── repos                                统计插件生成目录
|
├── build.gradle                         项目构建配置
├── config.gradle                        gradle编译文件 gradle依赖配置
├── config.properties                    项目配置
├── gradle.properties                    gradle配置
└── settings.gradle                      项目依赖配置
```
## ViewBinding
通过视图绑定功能，您可以更轻松地编写可与视图交互的代码。在模块中启用视图绑定之后，系统会为该模块中的每个 XML 布局文件生成一个绑定类。绑定类的实例包含对在相应布局中具有 ID 的所有视图的直接引用。
与使用 findViewById 相比，视图绑定具有 Null 安全，类型安全等很显著的优点。
- [轻松使用ViewBinding](https://developer.android.google.cn/topic/libraries/view-binding?hl=zh-cn)
## LiveData
LiveData 是一种可观察的数据存储器类，它具有生命周期感知能力，意指它遵循其他应用组件（如 Activity、Fragment 或 Service）的生命周期。
LiveData 的优势：不会发生内存泄漏，不会因 Activity 停止而导致崩溃，不再需要手动处理生命周期，数据始终保持最新状态，适当的配置更改，共享资源。
- [轻松使用LiveData](https://developer.android.google.cn/topic/libraries/architecture/livedata?hl=zh_cn)
## ViewModel
ViewModel 类旨在以注重生命周期的方式存储和管理界面相关的数据。ViewModel 类让数据可在发生屏幕旋转等配置更改后继续留存。
- [轻松使用ViewModel](https://developer.android.google.cn/topic/libraries/architecture/viewmodel?hl=zh_cn)
## 协程
协程是一种并发设计模式，您可以使用它来简化异步执行的代码。
协程的特点包括：轻量，内存泄漏更少，内置取消支持，Jetpack 集成。
- [轻松使用协程](https://developer.android.google.cn/kotlin/coroutines?hl=zh_cn)
## Fragment + LiveData + ViewModel + 协程
以项目中 MainFragment 为例：
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
## 基于LiveData封装的消息总线LiveDataBus
LiveDataBus具有生命周期感知，调用者不需要调用反注册，并且没有内存泄漏风险。  
```
    1、发送事件
    LiveDataBus.with<String>("key").postEvent("value")

    2、接收事件
    LiveDataBus.with<String>("key").observe(viewLifecycleOwner, { it ->
        println(it)
    })

    3、接收粘滞事件
    LiveDataBus.with<String>("key").observeSticky(viewLifecycleOwner, { it ->
        println(it)
    })
```
## 基于RoomDatabase封装的DBHelper
通过键值对的方式来存储数据，不用再去关心RoomDatabase的复杂操作。
```
    1、存储数据
    DBHelper.set(“key”, "value")

    2、获取数据

     DBHelper.get(“key”)
```
## 动态权限申请
- [超详细 —— 自己动手撸一个Android动态权限申请库](https://juejin.cn/post/6991471901704978440)
## 字节码插桩
- [最通俗易懂的字节码插桩实战（Gradle + ASM）—— 优雅的打印方法执行时间](https://juejin.cn/post/6986848837797658637)
- [最通俗易懂的字节码插桩实战（Gradle + ASM）—— 自动埋点](https://juejin.cn/post/6985366891447451662)
## 主要开源框架
- [square/okhttp](https://github.com/square/okhttp)
- [square/retrofit](https://github.com/square/retrofit)
- [google/gson](https://github.com/google/gson)
- [bumptech/glide](https://github.com/bumptech/glide)
- [tencent/x5](https://x5.tencent.com/)
- [Meituan-Dianping/walle](https://github.com/Meituan-Dianping/walle)
## Thanks
  感谢所有优秀的开源项目 ^_^   
  如果喜欢的话希望给个 Star 或 Fork ^_^  
  谢谢~~  
## LICENSE
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
