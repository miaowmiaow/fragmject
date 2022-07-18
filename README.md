## 前言
刚开始学习 `Kotlin` 其实挺痛苦的，相关的书籍或视频偏向于知识点的讲解。   
开源项目为了满足业务需求，代码层层封装，用来上手实在不合适，于是便有了 `fragmject` 项目。   
在此感谢 [玩Android](https://www.wanandroid.com/) 提供的 [开放API](https://wanandroid.com/blog/show/2) 。

## 简介
`fragmject` 是一个为初学者准备的上手项目。   
通过对 `Kotlin` 和 `Jetpack` 的系统运用，实现的一个功能完备符合主流市场标准 App。   
`fragmject` 没有复杂的业务和多余的封装， 完全依照 [Android Developer](https://developer.android.google.cn/) 官方的写法。   
代码简单，内容全面，快速上手，对理解其他项目设计思想和封装技巧也很有帮助。   

学习本项目你将有如下收获： 
- Kotlin（函数进阶，泛型，反射，协程...） 
- MVVM（ViewModel，LiveData...）
- 单 Activity 应用架构（Navigation...）
- 常用封装(图片选择器、图片编辑器、日期控件、全面屏沉浸、屏幕录制、字节码插桩...)

## 开发环境
为了您能正常运行本项目，请使用 `Android Studio Bumblebee (2021.1.1) 🐝` 和 `Android Gradle 7.1.2` 或者以上版本。   
[Download Android Studio | Android Developer](https://developer.android.google.cn/studio?hl=zh-cn#downloads/)

## 前置知识
在学习前希望您能了解以下知识，这将帮助您更快的上手本项目。
- [Kotlin 语言学习 | Android Developer](https://developer.android.google.cn/kotlin/learn?hl=zh_cn)
- [Kotlin 代码示例 | Android Developer](https://play.kotlinlang.org/byExample/overview)
- [ViewBinding 使用入门 | Android Developer](https://developer.android.google.cn/topic/libraries/view-binding?hl=zh-cn)
- [LiveData 使用入门 | Android Developer](https://developer.android.google.cn/topic/libraries/architecture/livedata?hl=zh_cn)
- [ViewModel 使用入门 | Android Developer](https://developer.android.google.cn/topic/libraries/architecture/viewmodel?hl=zh_cn)
- [协程 使用入门 | Android Developer](https://developer.android.google.cn/kotlin/coroutines?hl=zh_cn)
- [Navigation 使用入门 | Android Developer](https://developer.android.google.cn/guide/navigation/navigation-getting-started?hl=zh_cn)
- [一文看懂MVVM | 掘金](https://juejin.cn/post/7058542176375930887)

## 截图展示
| ![1.jpg](https://raw.githubusercontent.com/miaowmiaow/fragmject/master/screenshot/device-2022-03-24-095501.png) | ![2.jpg](https://raw.githubusercontent.com/miaowmiaow/fragmject/master/screenshot/device-2022-03-24-095527.png) | ![3.jpg](https://raw.githubusercontent.com/miaowmiaow/fragmject/master/screenshot/device-2022-03-24-100242.png) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |

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
├── library-base                         基础library（存放通用封装源码）
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
├── library-common                       公共library（存放各个 module 公用源码及资源）
|   └── src 
|       └── main 
|       |   └── java                     源码目录
|       |       ├── bean                 实体类目录
|       |       └── constant             常量配置目录
|       | 
|       └── build.gradle                 模块构建配置
| 
├── miaow-picture                        图片编辑器模块（目录同app，不再展开）
├── miaow-plugin                         插件模块
|   └── src 
|       └── main 
|           └── groovy                   源码目录
|           | 
|           └── resources                配置目录
|               └── statistic.properties 插件配置
├── module-user                          用户模块（目录同app，不再展开）
├── module-wan                           玩Android功能模块（目录同app，不再展开）
| 
├── repos                                插件生成目录
|
├── build.gradle                         项目构建配置
├── config.gradle                        gradle编译文件 gradle依赖配置
├── config.properties                    项目配置
├── gradle.properties                    gradle配置
└── settings.gradle                      项目依赖配置
```

## KVDatabase
通过对 `RoomDatabase` 进行封装，从而更方便的实现数据持久化。   

#### 快速使用
```
// 存储数据
KVDatabase.set(key: String, value: String)

// 获取数据
KVDatabase.get(key: String)
```

## WebView 优化及 H5 秒开实践
[满满的 WebView 优化干货，让你的 H5 实现秒开体验](https://juejin.cn/post/7043706765879279629)

## SharedFlowBus
基于 `SharedFlow` 封装的消息总线。   

[SharedFlowBus：30行代码实现消息总线你确定不看吗](https://juejin.cn/post/7028067962200260615)

#### 快速使用
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

## 字节码插桩
[最通俗易懂的字节码插桩实战 —— 优雅的打印方法执行时间](https://juejin.cn/post/6986848837797658637)

[最通俗易懂的字节码插桩实战 —— 自动埋点](https://juejin.cn/post/6985366891447451662)

#### 隐私合规 ———— 替换目标字段或方法
在 `MiaowPlugin` 添加 `ScanBean` 并配置目标字段或方法以及对应的替换字段或方法
```
ScanBean(
    owner = "android/os/Build",
    name = "BRAND",
    desc = "Ljava/lang/String;",
    replaceOpcode = Opcodes.INVOKESTATIC,
    replaceOwner = "com/example/fragment/library/common/utils/BuildUtils",
    replaceName = "getBrand",
    "()Ljava/lang/String;"
)
```
#### 性能扫描 ———— 打印方法执行时间
在 `MiaowPlugin` 添加 `TimeBean` 并配置打印目标或范围
```
TimeBean( //以包名和执行时间为条件
    "com/example/fragment/library/base",
    time = 50L
)
```
#### 性能扫描 ———— 打印方法执行时间
在 `MiaowPlugin` 添加 `TraceBean` 并配置埋点目标以及对应埋点方法
```
TraceBean(
    owner = "Landroid/view/View\$OnClickListener;",
    name = "onClick",
    desc = "(Landroid/view/View;)V",
    traceOwner = "com/example/fragment/library/common/utils/StatisticHelper",
    traceName = "viewOnClick",
    traceDesc = "(Landroid/view/View;)V" //参数应在desc范围之内
)
```

配置完成后 `gradle` 执行 `publish` 任务生成插件
在根目录 `setting.gradle` 添加本地插件源
```
pluginManagement {
    repositories {
        maven {
            url uri('repo')
        }
    }
}
```
在根目录 `build.gradle` 添加插件依赖
```
buildscript {
    dependencies {
        classpath 'com.example.miaow:plugin:1.0.0'
    }
}
```
在app目录 `build.gradle` apply插件
```
plugins {
    id 'miaow'
}
```

## 动态权限申请
[自己动手撸一个动态权限申请库](https://juejin.cn/post/6991471901704978440)

## 图片编辑器
[自己动手撸一个图片编辑器（支持长图）](https://juejin.cn/post/7013274417766039560)
### 截图展示
| ![5.gif](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4676b80c4f4b4b99821f9d36d1e78e9b~tplv-k3u1fbpfcp-watermark.awebp?) | ![6.gif](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3a12c1c4bc524c9fa3edcea71e95d71f~tplv-k3u1fbpfcp-watermark.awebp?) | ![7.gif](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6c448aaa731f47e8b63ffe54ba25ad5b~tplv-k3u1fbpfcp-watermark.awebp?) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
#### 快速使用
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
1. 通过 `PictureEditorDialog` 调用图片编辑器
2. 通过 `setBitmapPath(path)` 传入图片路径
3. 通过 `setEditorFinishCallback(callback)` 获取编辑后的图片地址

如果觉得 `PictureEditorDialog` 不能满足需求，还可以通过 `PictureEditorView` 来自定义样式
#### 自定义使用
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
1. 通过 `setBitmapPath(path)` 传入图片路径
2. 通过 `setMode(mode)` 设置编辑模式，分别有：涂鸦，橡皮擦，马赛克，贴纸
3. 通过 `setGraffitiColor(color)` 设置涂鸦画笔颜色
4. 通过 `setSticker(StickerAttrs(bitmap))` 设置贴纸
5. 通过 `graffitiUndo()` 涂鸦撤销
6. 通过 `mosaicUndo()` 马赛克撤销
7. 通过 `saveBitmap()` 保存编辑图片

`PictureEditorView` 就介绍到这里，具体使用请查看 `PictureEditorDialog`
#### 图片裁剪
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
1. 通过 `setBitmapResource(bitmap)` 传入裁剪图片
2. 通过 `clip.rotate()` 图片旋转
3. 通过 `clip.reset()` 图片重置
4. 通过 `clip.saveBitmap()` 保存裁剪框内图片

`PictureClipView` 就介绍到这里，具体使用请查看 `PictureClipDialog`

## 主要开源框架
- [coil-kt/coil](https://github.com/coil-kt/coil)
- [google/gson](https://github.com/google/gson)
- [sourceforge/pinyin4j](http://pinyin4j.sourceforge.net/)
- [square/okhttp](https://github.com/square/okhttp)
- [square/retrofit](https://github.com/square/retrofit)
- [tencent/x5](https://x5.tencent.com/)

## Gitee镜像
- [fragmject](https://gitee.com/zhao.git/FragmentProject.git)

## About me
- QQ : 237934622
- WeChat : zst_1116
- Email : <zst_1116@icloud.com>
- JueJin：[miaowmiaow](https://juejin.cn/user/3342971112791422)

## Thanks
感谢所有优秀的开源项目 ^_^   
如果喜欢的话希望给个 Star 或 Fork ^_^  
谢谢~~  
