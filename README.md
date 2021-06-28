# 前言
学习```Kotlin```有一段时间了，想写一个项目总结收获，就有了这个可能是东半球最简洁的玩安卓客户端，在此感谢[玩Android](https://www.wanandroid.com/) 的开放API。
# 简介
适合初学者入门的项目，通过对Kotlin的系统运用，实现的一个功能完备符合主流市场标准App。  
虽然本项目的定位是入门级，但是该有的知识点却一点不少，对理解其他项目设计思想和封装技巧也很有帮助。  
学习本项目你将有如下收获：  
- Kotlin函数进阶，泛型，反射，协程的运用
- ViewBinding的运用
- ViewModel的运用
- LiveData的运用
- Room数据库的运用
- MVVM开发架构
- 单Activity多Fragment
- 字节码插桩
- 暗夜模式
- 屏幕录制
# 截图展示
| ![1.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621158973.png) | ![2.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155363.png) | ![3.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155387.png) |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![4.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155408.png) | ![5.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155418.png) | ![6.jpg](https://gitee.com/zhao.git/PictureWarehouse/raw/master/FragmentProject/Screenshot_1621155439.png) |
# Fragment + ViewMode + LiveData
1、ViewModel是一个抽象类通过继承实现我们自己的ViewModel类，代码如下:
```
   class MyViewModel : ViewModel(){
      // 使用LiveData存放数据
      val result = MutableLiveData<String>()
      
      fun getData(){
         val data = "Test" //这里获取数据为示例代码，请根据实际情况从网络或本地获取
         result.postValue(data) //设置Livedata的值，通过Livedata通知Frament刷新界面
      }
   }
```
2、Fragment里面初始化ViewModel，并观察观察Viewmodel的Livedata，如果Livedata的值改变会通知Frament，代码如下:
```
   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      val viewModel = ViewModelProvider(this as ViewModelStoreOwner).get(MyViewModel::class.java)
      viewModel.result.observe(viewLifecycleOwner, {it->
         println(it)
      })
   }
```
3、ViewModel获取数据（网络、数据库），代码如下:
```
   viewModel.getData()
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
# Retrofit2+Kolin协程
使用suspend定义Api，无需使用Call类型返回结果，直接返回data class类型，代码如下:
```
interface ApiService {
    @GET
    suspend fun get(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>
    ): ResponseBody
}
```
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
