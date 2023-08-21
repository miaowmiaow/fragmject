# README.md
- en [English](README.en.md)
- zh_CN [简体中文](../README.md)

# Locales & Contributors
- de_en English  *by Copilot*

# Preface
At first glance learning Kotlin was quite difficult, tutorials and videos focused on knowledge points but lacked practical project examples. To overcome this hurdle I created fragmject which provides an example app that demonstrates core features of both Kotlin and Jetpack Compose while keeping things simple enough for beginners to grasp quickly. Special thanks goes out to Wan Android who provide their open API for use with this project!

# Introduction
Fragmject is designed as an easy starting point for those new to programming with Kotlin and Jetpack Compose. By leveraging these technologies we are able to create a fully featured production ready application that adheres strictly to best practices outlined by Google themselves. Fragmject avoids complex business logic or unnecessary layers of abstraction; it simply implements everything according to how you would do so in real life projects following official guidelines provided by Android Developers website. Code is concise yet comprehensive making it straightforward even for someone without any prior experience. It will help you understand design patterns employed elsewhere alongside various techniques used for dependency injection and view binding.

By working through this project you'll gain valuable insights into several areas such as:
* Kotlin fundamentals
* Composing UI with Jetpack Compose
* Architectural approaches like Model View ViewModel & MVI
* Custom views built from scratch including image pickers, editors, datepickers etc...
* Advanced topics like bytecode manipulation via ASM library

In summary, if you're looking for a well structured codebase to learn from then look no further than Fragmject!

## Development Environment
To get the best possible experience when running this project, please use the latest version of Android Studio. You can download it at the following address:
[Download Android Studio | Android Developer](https://developer.android.google.cn/studio?hl=zh-cn#downloads/)

## Pre knowledge
Before diving into this project, here are some basic concepts you might want to familiarize yourself with:
- [Learn the Kotlin programming language | Android Developer](https://developer.android.google.cn/kotlin/learn?hl=en)
- [Learn Kotlin by Example | Android Developer](https://play.kotlinlang.org/byExample/overview)
- [ViewBinding | Android Developer](https://developer.android.google.cn/topic/libraries/view-binding?hl=en)
- [LiveData | Android Developer](https://developer.android.google.cn/topic/libraries/architecture/livedata?hl=en)
- [ViewModel | Android Developer](https://developer.android.google.cn/topic/libraries/architecture/viewmodel?hl=en)
- [Coroutines | Android Developer](https://developer.android.google.cn/kotlin/coroutines?hl=en)
- [Navigation | Android Developer](https://developer.android.google.cn/guide/navigation/navigation-getting-started?hl=en)
- [Room | Android Developer](https://developer.android.google.cn/training/data-storage/room?hl=en)
- [Compose | Android Developer](https://developer.android.google.cn/jetpack/compose)

## Why rarely rely on other libraries
Why does this project avoid depending on external libraries as much as possible? There are two main reasons why relying too heavily on third party libraries can be detrimental to a developer’s growth as a programmer:

Increased Learning Burden: Using libraries that seem simple on the surface can actually involve complex underlying implementations that require deep reading of source code. This can deter developers from exploring additional libraries and learning new skills independently.

Weakened Foundation: New programmers often treat the capabilities offered by thirdparty libraries as part of their own skillset, limiting their ability to work outside of the scope of those libraries. As a result, they losevaluable practice and expertise in general software engineering.

The goal of this project is to encourage students to build a strong foundation in core programming principles by implementing solutions fromscratch wherever possible. Even though the resultingcode maynot alwaysbe elegantor idiomatic,itwillprovide a deeperunderstandingofhowthingsworkandallowsthe studenttobuildmoretransferableskills.

## Screenshot display
| ![1.jpg](https://raw.githubusercontent.com/miaowmiaow/fragmject/master/screenshot/device-2022-03-24-095501.png) | ![2.jpg](https://raw.githubusercontent.com/miaowmiaow/fragmject/master/screenshot/device-2022-03-24-095527.png) | ![3.jpg](https://raw.githubusercontent.com/miaowmiaow/fragmject/master/screenshot/device-2022-03-24-100242.png) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |

## Project Directory Structure
```
├── app                                         
|   └── src 
|       └── main 
|       |   └── java                            
|       |   |   ├── bean                        
|       |   |   ├── components                  
|       |   |   ├── ui                          
|       |   |   |   └── main                    
|       |   |   |       └── home                
|       |   |   |       |   ├── HomeScreen      
|       |   |   |       |   └── HomeViewModel   
|       |   |   |       └── MainScreen          
|       |   |   ├── utils                       
|       |   |   ├── WanActivity                 
|       |   |   ├── WanApplication              
|       |   |   ├── WanTheme                    
|       |   |   ├── WanNavGraph                 
|       |   |   └── WanViewModel                
|       |   |
|       |   └── res                             
|       |   └── AndroidManifest.xml             
|       |
|       ├── build.gradle                        
|       ├── dictionary                          
|       └── proguard-rules.pro                  
| 
├── library-base                                
|   └── src 
|       └── main 
|       |   ├── assets                          
|       |   └── java                            
|       |       ├── activity                    
|       |       ├── adapter                     
|       |       ├── bus                         
|       |       ├── db                          
|       |       ├── dialog                      
|       |       ├── http                        
|       |       ├── provider                    
|       |       ├── service                     
|       |       ├── utils                       
|       |       └── view                        
|       | 
|       └── build.gradle                        
| 
├── library-picture                             
| 
├── library-plugin                      
|   └── src 
|       └── main 
|           ├── kotlin                          
|           └── resources                       
|               └── statistic.properties        
| 
├── repos                                       
|
├── build.gradle                                
├── config.properties                           
├── gradle.properties                           
└── settings.gradle                             
```

## Jetpack Compose

#### Less code
Using Compose instead of the traditional Android View system enables us to accomplish more with less code, leading to fewer lines of code needing testing and debugging, and lower likelihood of bugs occurring. This makes maintenance easier for reviewers and maintainers alike, reducing the amount of code that needs to be read, understood, approved, and maintained.

Composereflects a simpler concept in itslayout system,whereallcodeiswritten in one language and located within thesamefile,eliminating theneedfor back-and-forth switching between Kotlin andXML.

#### Intuitive
User's Question: Compose uses declarative APIs which mean that you only have to define the interface without worrying about how to implement each step.

By leveraging Compose, you can create smaller statelesscomponents that aren't attached to any particular activity or fragment.

In Compose, state is explicit and passed down tocomposable elements allowing them to maintain their integrity while being isolated from other parts of the app. When the statechanges, the UI updates automatically.

#### Mutually compatible
Compose is compatible with all your existing code: you can call Compose code from View or call View from Compose. Most commonly used libraries, such as Navigation, ViewModel, and Kotlin coroutine, are suitable for Compose, so you can start adopting them anytime, anywhere.

- [Jetpack Compose : 从改造你的登录页面开始](https://juejin.cn/post/7156425159249756191)
- [Jetpack Compose : 一学就会的自定义下拉刷新&加载更多](https://juejin.cn/post/7185159395519496250)
- [Jetpack Compose : 一文学会嵌套滚动NestedScrollConnection](https://juejin.cn/spost/7239610698116055098)
- [Jetpack Compose : 超简单实现滚轮控件(WheelPicker)](https://juejin.cn/post/7266702105829277754)
- [Jetpack Compose : WebView的使用与优化](https://juejin.cn/post/7194360493866221628)

## WebView 优化及 H5 秒开实践
[满满的 WebView 优化干货，让你的 H5 实现秒开体验](https://juejin.cn/post/7043706765879279629)

## SharedFlowBus
[SharedFlowBus：30行代码实现消息总线你确定不看吗](https://juejin.cn/post/7028067962200260615)

#### Quick access
```
// send message
SharedFlowBus.with(objectKey: Class<T>).tryEmit(value: T)

// send sticky message
SharedFlowBus.withSticky(objectKey: Class<T>).tryEmit(value: T)

// subscribe message
SharedFlowBus.on(objectKey: Class<T>).observe(owner){ it ->
    println(it)
}

// subscribe sticky message
SharedFlowBus.onSticky(objectKey: Class<T>).observe(owner){ it ->
    println(it)
}
```

## Picture editor（library-picture）
[自己动手撸一个图片编辑器（支持长图）](https://juejin.cn/post/7013274417766039560)

### Screenshot display
| ![5.gif](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4676b80c4f4b4b99821f9d36d1e78e9b~tplv-k3u1fbpfcp-watermark.awebp?) | ![6.gif](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3a12c1c4bc524c9fa3edcea71e95d71f~tplv-k3u1fbpfcp-watermark.awebp?) | ![7.gif](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6c448aaa731f47e8b63ffe54ba25ad5b~tplv-k3u1fbpfcp-watermark.awebp?) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |

#### Quick access
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

If you feel that PictureEditorDialog cannot meet the requirements, you can also customize the style through PictureEditorView.
#### Custom usage
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

That's all for PictureEditorView. For specific usage, please refer to PictureEditorDialogs.
#### Picture cropping
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

That's all for PictureClipView. For specific usage, please refer to PictureClipDialogs.
#### Picture selection
```
if (context is AppCompatActivity) {
    PictureSelectorDialog.newInstance()
        ...
        .show(context.supportFragmentManager)
}
```

## 字节码插桩
[最通俗易懂的字节码插桩实战 —— 优雅的打印方法执行时间](https://juejin.cn/post/6986848837797658637)

[最通俗易懂的字节码插桩实战 —— 自动埋点](https://juejin.cn/post/6985366891447451662)

#### 隐私合规 ———— 替换目标字段或方法（library-plugin）
[一文学会字节码替换，再也不用担心隐私合规审核](https://juejin.cn/post/7121985493445083149)

#### Quick access
源代码在 `library-plugin` 目录下。   
在 `MiaowPlugin` 添加 `ScanBean` 并配置目标字段或方法以及对应的替换字段或方法。
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
#### 耗时扫描 ———— 打印方法执行时间
在 `MiaowPlugin` 添加 `TimeBean` 并配置打印目标或范围。
```
TimeBean( //以包名和执行时间为条件
    "com/example/fragment/library/base",
    time = 50L
)
```
#### 埋点统计 ———— 自动埋点
在 `MiaowPlugin` 添加 `TraceBean` 并配置埋点目标以及对应埋点方法。
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

配置完成后 `gradle` 执行 `publish` 任务生成插件。   
在根目录 `setting.gradle` 添加本地插件源。
```
pluginManagement {
    repositories {
        maven {
            url uri('repo')
        }
    }
}
```
在根目录 `build.gradle` 添加插件依赖。
```
buildscript {
    dependencies {
        classpath 'com.example.miaow:plugin:1.0.0'
    }
}
```
在app目录 `build.gradle` apply插件。
```
plugins {
    id 'miaow'
}
```

## Main open-source libraries
- [coil-kt/coil](https://github.com/coil-kt/coil)
- [google/gson](https://github.com/google/gson)
- [square/okhttp](https://github.com/square/okhttp)
- [square/retrofit](https://github.com/square/retrofit)

## Gitee
- [fragmject](https://gitee.com/zhao.git/FragmentProject.git)

## About me
- QQ : 237934622
- QQ群 : 389499839
- WeChat : zst_1116
- Email : <237934622@qq.com>
- JueJin：[miaowmiaow](https://juejin.cn/user/3342971112791422)

## Thanks
Thank you to all outstanding open source projects^_^

If you like it, I hope to give it to Star or Fork^_^

Thank you~~
