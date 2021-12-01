## 前言
刚开始学习```Kotlin```其实挺痛苦的，相关的书籍或视频偏向于知识点的讲解看完好像还是不会做项目，开源的项目内容太多用来上手实在不合适。
想要一个项目，它不用多么的炫酷吊炸天，只要代码够简单，知识够详细，内容够全面，于是就有了这个入门级的项目。
在此感谢[玩Android](https://www.wanandroid.com/) 提供的[开放API](https://wanandroid.com/blog/show/2)。
## 简介
适合初学者入门的项目，通过对Kotlin的系统运用，实现的一个功能完备符合主流市场标准App。 
它够简单，够全面，够详细，能让你快速上手的kotlin项目。
虽然本项目的定位是入门级，但是该有的知识点却一点不少，对理解其他项目设计思想和封装技巧也很有帮助。  
学习本项目你将有如下收获： 
- Kotlin（函数进阶，泛型，反射，协程...） 
- MVVM开发架构（ViewModel，LiveData...）
- 单Activity多Fragment（Navigation）
- 暗夜模式
- 屏幕录制
- 图片编辑
- 字节码插桩
## 截图展示
| ![1.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/device-2021-11-25-100723.png) | ![2.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/device-2021-11-25-100805.png) |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![3.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/device-2021-11-25-101002.png) | ![4.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/device-2021-11-29-171800.png) |
## 项目目录结构
```
├── app                                  app
|   └── src 
|       └── main 
|       |   └── java                     源码目录
|       |   |   ├── activity             Activity目录
|       |   |   |   └── MainActivity     项目唯一Activity
|       |   |   ├── adapter              Adapter目录
|       |   |   ├── fragment             Fragment目录
|       |   |   ├── model                ViewModel目录
|       |   |   └── App                  Application
|       |   |
|       |   └── res                      资源目录
|       |   |   └── navigation           导航图目录
|       |   |
|       |   └── AndroidManifest.xml      配置文件
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
|       |       ├── db                   Database目录
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
├── module-user                          用户模块（目录同app，不再展开）
├── module-wan                           玩Android功能模块（目录同app，不再展开）
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
## Kotlin
Kotlin 是一种富有表现力且简洁的编程语言，不仅可以减少常见代码错误，还可以轻松集成到现有应用中。
- [学习Kotlin编程语言1](https://developer.android.google.cn/kotlin/learn?hl=zh_cn)
- [学习Kotlin编程语言2](https://play.kotlinlang.org/byExample/overview)
- [开发中常见的Kotlin模式](https://developer.android.google.cn/kotlin/common-patterns?hl=zh_cn)
- [将Kotlin添加到现有应用](https://developer.android.google.cn/kotlin/add-kotlin?hl=zh_cn)
## ViewBinding
通过视图绑定功能，您可以更轻松地编写可与视图交互的代码。与使用 findViewById 相比，视图绑定具有 Null 安全，类型安全等很显著的优点。
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
## Navigation
Navigation 是 Android Jetpack 组件之一，主要是用于 Fragment 路由导航的框架，通过 Navigation 我们可以设计出单 Activity 应用架构。
- [一文看懂Navigation](https://juejin.cn/post/7036296113573347364)
## 基于RoomDatabase封装的KVDatabase
通过键值对的方式来存储数据，不用再去关心RoomDatabase的复杂操作。
### 快速使用
```
1、存储数据
KVDatabase.set(key: String, value: String)

2、获取数据
KVDatabase.get(key: String)
```
## 基于SharedFlow封装的消息总线SharedFlowBus
- [SharedFlowBus：30行代码实现消息总线你确定不看吗](https://juejin.cn/post/7028067962200260615)
### 快速使用
```
// 发送消息
SharedFlowBus.with(objectKey: Class<T>).tryEmit(value: T)
// 发送粘性消息
SharedFlowBus.withSticky(objectKey: Class<T>).tryEmit(value: T)

// 订阅消息
SharedFlowBus.on(objectKey: Class<T>).observe(owner){ it ->
    println(it)
}
// 订阅粘性消息
SharedFlowBus.onSticky(objectKey: Class<T>).observe(owner){ it ->
    println(it)
}
```
## 动态权限申请
- [超详细 —— 自己动手撸一个Android动态权限申请库](https://juejin.cn/post/6991471901704978440)
## 字节码插桩
- [最通俗易懂的字节码插桩实战（Gradle + ASM）—— 优雅的打印方法执行时间](https://juejin.cn/post/6986848837797658637)
- [最通俗易懂的字节码插桩实战（Gradle + ASM）—— 自动埋点](https://juejin.cn/post/6985366891447451662)
## 图片编辑器
- [巨丝滑 —— 自己动手撸一个图片编辑器（支持长图）](https://juejin.cn/post/7013274417766039560)
### 截图展示
| ![5.gif](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/VID_20210929142301.gif) | ![6.gif](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/VID_20210929141429.gif) |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
### 接入
第 1 步:在工程的`build.gradle`中添加：
```
allprojects {
    repositories {
		...
		mavenCentral()
	}
}
```
第2步：在应用的`build.gradle`中添加：
```
dependencies {
    implementation 'com.github.miaowmiaow.fragmject:miaow-picture:1.2.6'
}
```
### 快速使用
```
PictureEditorDialog.newInstance()
    .setBitmapPath(path)
    .setEditorFinishCallback(object : EditorFinishCallback {
        override fun onFinish(path: String) {
            val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options())
        }
    })
    .show(childFragmentManager)
```
如上所示：
1. 通过`PictureEditorDialog`调用图片编辑器
2. 通过`setBitmapPath(path)`传入图片路径
3. 通过`setEditorFinishCallback(callback)`获取编辑后的图片地址

**如果觉得`PictureEditorDialog`不能满足需求，还可以通过`PictureEditorView`来自定义样式**

### 自定义使用
```
<com.example.miaow.picture.editor.PictureEditorView
    android:id="@+id/pic_editor"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
```
picEditor.setBitmapPath(path)
picEditor.setMode(PictureEditorView.Mode.STICKER)
picEditor.setGraffitiColor(Color.parseColor("#ffffff"))
picEditor.setSticker(StickerAttrs(bitmap))
picEditor.graffitiUndo()
picEditor.mosaicUndo()
picEditor.saveBitmap()
```
如上所示：
1. 通过`setBitmapPath(path)`传入图片路径
2. 通过`setMode(mode)`设置编辑模式，分别有：涂鸦，橡皮擦，马赛克，贴纸
3. 通过`setGraffitiColor(color)`设置涂鸦画笔颜色
4. 通过`setSticker(StickerAttrs(bitmap))`设置贴纸
5. 通过`graffitiUndo()`涂鸦撤销
6. 通过`mosaicUndo()`马赛克撤销
7. 通过`saveBitmap()`保存编辑图片

`PictureEditorView`就介绍到这里，具体使用请查看`PictureEditorDialog`

### 图片裁剪
```
<com.example.miaow.picture.editor.PictureClipView
    android:id="@+id/clip"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
```
clip.setBitmapResource(bitmap)
clip.rotate()
clip.reset()
clip.saveBitmap()
```
如上所示：
1. 通过`setBitmapResource(bitmap)`传入裁剪图片
2. 通过`clip.rotate()`图片旋转
3. 通过`clip.reset()`图片重置
4. 通过`clip.saveBitmap()`保存裁剪框内图片

`PictureClipView`就介绍到这里，具体使用请查看`PictureClipDialog`

讲在最后，如无法加载图片，请确认存储权限

## 主要开源框架
- [square/okhttp](https://github.com/square/okhttp)
- [square/retrofit](https://github.com/square/retrofit)
- [google/gson](https://github.com/google/gson)
- [coil-kt/coil](https://github.com/coil-kt/coil)
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
