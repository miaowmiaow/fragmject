package com.example.fragmject.app.devtools

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer
import java.util.zip.ZipFile

/**
 * H6（DEX 壳）S0 可行性探针 —— **仅 debug 变体存在**。
 *
 * ============================ 要回答的三个问题 ============================
 * 1. `InMemoryDexClassLoader` 能否在本机（Android 16 真机 / 16KB page 模拟器）上
 *    从内存字节里定义类？——壳的加载器基础；
 * 2. `ActivityThread.mBoundApplication.info.mClassLoader` 这些 hidden API
 *    在 Android 16 上是否仍可反射读写？——壳能否把系统 classloader 换掉；
 * 3. 替换后能否还原（探针只在 debug 运行，必须还原，避免破坏当前进程）。
 *
 * 结果直接打到 logcat：`adb logcat -s ShellProbe`。
 *
 * ============================ 为什么用 ContentProvider ============================
 * 系统在 `Application.onCreate` 之前创建 ContentProvider，这与真实壳必须完成
 * classloader 替换的时间窗一致；用 MainActivity 验证会晚一步，结论不可信。
 */
class ShellProbeProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        runCatching { probe(context?.applicationInfo?.sourceDir.orEmpty()) }
            .onFailure { Log.e(TAG, "probe failed", it) }
        return true
    }

    private fun probe(apkPath: String) {
        Log.i(TAG, "device=${Build.MANUFACTURER}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT} " +
            "abi=${Build.SUPPORTED_ABIS.firstOrNull()}")

        val dexBytes = readDexBytes(apkPath) ?: run {
            Log.e(TAG, "read dex failed: $apkPath")
            return
        }
        Log.i(TAG, "dex bytes=${dexBytes.size}")

        // ---- 1. InMemoryDexClassLoader 能否从内存字节定义类 ----
        val loader = runCatching {
            InMemoryDexClassLoader(ByteBuffer.wrap(dexBytes), javaClass.classLoader)
        }.getOrElse {
            Log.e(TAG, "InMemoryDexClassLoader 构造失败: ${it.javaClass.name}: ${it.message}")
            return
        }
        Log.i(TAG, "InMemoryDexClassLoader 构造成功")

        var resolved = 0
        CANDIDATES.forEach { name ->
            runCatching { Class.forName(name, false, loader) }
                .onSuccess {
                    resolved++
                    Log.i(TAG, "loader 命中: $name")
                }
                .onFailure { Log.w(TAG, "loader 未命中: $name (${it.javaClass.simpleName})") }
        }
        Log.i(TAG, "loader 命中类数=$resolved/${CANDIDATES.size}")

        // ---- 2. hidden API：读取并替换 LoadedApk.mClassLoader ----
        runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentThread = activityThreadClass
                .getDeclaredMethod("currentActivityThread")
                .apply { isAccessible = true }
                .invoke(null)
            val boundApp = activityThreadClass
                .getDeclaredField("mBoundApplication")
                .apply { isAccessible = true }
                .get(currentThread)
            val loadedApk = boundApp.javaClass
                .getDeclaredField("info")
                .apply { isAccessible = true }
                .get(boundApp)

            val classLoaderField = loadedApk.javaClass
                .getDeclaredField("mClassLoader")
                .apply { isAccessible = true }
            val original = classLoaderField.get(loadedApk) as ClassLoader
            Log.i(TAG, "mClassLoader 可读: ${original.javaClass.name}")

            classLoaderField.set(loadedApk, loader)
            val replaced = classLoaderField.get(loadedApk) === loader
            Log.i(TAG, "mClassLoader 替换成功=$replaced")

            // 壳的关键假设：替换 classloader 后，真实 Application 能被「按名加载 + 实例化」。
            // 这里只验证「类初始化 + 构造」能否走通（Hilt 的生成父类在类加载时初始化），
            // 真正的组件创建要在真实壳里验证。
            runCatching {
                val appClass = Class.forName(REAL_APPLICATION, true, loader)
                Log.i(TAG, "真实 Application 类加载成功（由 payload loader 定义）")
                appClass.getDeclaredConstructor().newInstance()
                Log.w(TAG, "真实 Application 实例化成功（注意：这是第二个实例，仅用于验证构造链）")
            }.onFailure {
                Log.e(TAG, "真实 Application 加载/实例化失败: ${it.javaClass.name}: ${it.message}")
            }

            // 系统托管的应用实例字段：壳最终必须替换它，否则 getApplicationContext() 仍指向 stub
            val initialAppField = activityThreadClass
                .getDeclaredField("mInitialApplication")
                .apply { isAccessible = true }
            Log.i(TAG, "mInitialApplication 可读=${initialAppField.get(currentThread) != null}")

            // 探针必须还原：不还原会让后续类加载走到我们构造的 loader 上，进程行为异常
            classLoaderField.set(loadedApk, original)
            Log.i(TAG, "mClassLoader 已还原=${classLoaderField.get(loadedApk) === original}")
        }.onFailure {
            Log.e(TAG, "hidden API 反射失败: ${it.javaClass.name}: ${it.message}")
        }
    }

    /** 从 APK 里取一个较大的 dex 作为 payload（优先 classes2.dex）。 */
    private fun readDexBytes(apkPath: String): ByteArray? = runCatching {
        ZipFile(apkPath).use { zip ->
            val entry = zip.getEntry("classes2.dex") ?: zip.getEntry("classes.dex")
            entry?.let { zip.getInputStream(it).readBytes() }
        }
    }.getOrNull()

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        const val TAG = "ShellProbe"

        /** 真实（非 stub）Application：壳最终要让系统使用它。 */
        const val REAL_APPLICATION = "com.example.fragmject.app.FragmjectApplication"

        /** 覆盖 core / feature / app 的若干类，任一枚命中即可证明 loader 能定义类。 */
        val CANDIDATES = listOf(
            "com.example.fragmject.app.MainActivity",
            "com.example.fragmject.app.security.SecurityGuard",
            "com.example.fragmject.core.model.Article",
            "com.example.fragmject.core.network.http.OkHttpClients",
            "com.example.fragmject.feature.home.ui.home.HomeScreenKt",
            "com.example.fragmject.core.designsystem.AppThemeKt",
            "com.example.fragmject.app.navigation.AppNavGraphKt",
        )
    }
}
