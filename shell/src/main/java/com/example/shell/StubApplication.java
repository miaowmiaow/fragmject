package com.example.shell;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.List;

import dalvik.system.InMemoryDexClassLoader;

/**
 * DEX 壳的 stub（H6 / S1 最小实现）。
 *
 * ============================ 运行流程 ============================
 * 系统按 manifest 的 `android:name` 实例化本类（此时业务 dex 已被打包器加密进 assets）：
 *
 * 1. `attachBaseContext`：读出 `assets/payload/dex-*.bin` → 解密 → `InMemoryDexClassLoader`；
 * 2. 反射替换 `LoadedApk.mClassLoader`，让后续类解析（含 ContentProvider）走 payload loader；
 * 3. 用 payload loader 实例化真实 Application，并替换 `LoadedApk.mApplication` 与
 *    `ActivityThread.mInitialApplication`——Hilt 要求「系统托管的实例」提供
 *    `GeneratedComponentManager`，只换 loader 不够；
 * 4. 把 `base` 交给真实 Application（`attachBaseContext`），`onCreate` 由本类在
 *    系统回调时转发；
 * 5. 其余生命周期回调（onTrimMemory / onLowMemory / onConfigurationChanged）一并转发。
 *
 * ============================ 已知边界（S1） ============================
 * - 只做「能启动 + Hilt 正常」，不做 DEX 分片、反 dump、二次校验；
 * - 载荷用固定 XOR 混淆（非加密），密钥与打包任务一致；
 * - 依赖 hidden API 反射，Android 版本升级需复验（S0 已在 API 36/37 验证字段可写）。
 */
public class StubApplication extends Application {

    private static final String TAG = "DexShell";

    /** 真实 Application 的全限定名（与 :app 保持一致）。 */
    private static final String REAL_APPLICATION = "com.example.fragmject.app.FragmjectApplication";

    /** 与打包任务一致的 XOR 密钥基值。 */
    private static final int XOR_BASE = 0x5A;

    private Application real;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            install(base);
        } catch (Throwable t) {
            // 壳安装失败必须记录下来：此时业务代码尚未加载，任何异常都会表现为「启动即闪退」
            Log.e(TAG, "shell install failed", t);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (real == null) {
            return;
        }
        // 关键时序：系统在 makeApplication 返回后会执行 `mInitialApplication = <stub>`，
        // 把我们在 attachBaseContext 里做的替换覆盖掉；LoadedApk.mApplication 同理。
        // 因此必须在 onCreate（系统赋值之后）再换一次，否则 Activity 拿到的仍是 stub，
        // Hilt 会抛 “Hilt Activity must be attached to an @HiltAndroidApp Application”。
        swapApplicationReferences();
        try {
            real.onCreate();
            Log.i(TAG, "real application onCreate done");
        } catch (Throwable t) {
            Log.e(TAG, "real application onCreate failed", t);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (real != null) {
            real.onTrimMemory(level);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (real != null) {
            real.onLowMemory();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (real != null) {
            real.onConfigurationChanged(newConfig);
        }
    }

    // ------------------------------------------------------------------
    // 安装流程
    // ------------------------------------------------------------------

    private void install(Context base) throws Exception {
        verifyShellDex();

        ByteBuffer[] payload = readPayloads(base);
        Log.i(TAG, "payload chunks=" + payload.length);

        // parent 必须是「系统原本给应用的那个 PathClassLoader」，不是 systemClassLoader：
        // 业务 dex 已从 APK 移除，parent 只负责提供 **资源**（如 META-INF/services 里的
        // ServiceLoader 声明），类解析会落到 payload 上。用 systemClassLoader 会导致
        // kotlinx-coroutines-android 的 MainDispatcherFactory 找不到，抛
        // “Module with the Main dispatcher is missing”。
        ClassLoader payloadLoader = new InMemoryDexClassLoader(payload, getClassLoader());
        Class<?> realClass = Class.forName(REAL_APPLICATION, true, payloadLoader);
        Log.i(TAG, "real application class loaded: " + realClass.getName());

        Object instance = realClass.getDeclaredConstructor().newInstance();
        real = (Application) instance;

        // 顺序很重要：先换 loader（后续按名解析走 payload），再换 application 引用，
        // 最后才让真实 Application 执行 attachBaseContext。
        Object loadedApk = loadedApk();
        setField(loadedApk, "mClassLoader", payloadLoader);
        swapApplicationReferences();
        Log.i(TAG, "classloader & application references swapped");

        Method attach = Class.forName("android.content.ContextWrapper")
                .getDeclaredMethod("attachBaseContext", Context.class);
        attach.setAccessible(true);
        attach.invoke(real, base);
        Log.i(TAG, "real attachBaseContext done");
    }

    /**
     * 校验壳 dex 自身（H6/S2）。
     *
     * 期望值放在 `assets/payload/shell.txt`（打包任务写入），与业务载荷同一个信任域；
     * **不是**强锚点——真正的锚是 H1 的签名校验。这里的作用是让「只替换 classes.dex、
     * 顺手保留其余文件」这类粗糙改包直接失效（否则壳被换掉后连检测都不存在了）。
     *
     * 失败语义：不一致直接抛异常（fail-closed），不加载业务代码。
     */
    private void verifyShellDex() {
        try {
            byte[] expectedRaw = readAsset(this, "payload/shell.txt");
            if (expectedRaw == null) {
                Log.w(TAG, "no shell.txt, shell dex check skipped");
                return;
            }
            String expected = new String(expectedRaw, StandardCharsets.UTF_8).trim().toLowerCase();
            int actual = crcOfEntry(getApplicationInfo().sourceDir, "classes.dex");
            String actualHex = String.format("%08x", actual & 0xFFFFFFFFL);
            if (!actualHex.equals(expected)) {
                Log.e(TAG, "shell dex crc mismatch: actual=" + actualHex + " expected=" + expected);
                throw new IllegalStateException("shell dex integrity check failed");
            }
            Log.i(TAG, "shell dex check ok");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable t) {
            Log.w(TAG, "shell dex check error: " + t.getClass().getName());
        }
    }

    /** 计算 APK 内某个 entry 的 CRC32（低 32 位）。 */
    private static int crcOfEntry(String apkPath, String entryName) throws Exception {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(apkPath)) {
            java.util.zip.ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new IllegalStateException("missing " + entryName);
            }
            CRC32 crc = new CRC32();
            try (InputStream input = zip.getInputStream(entry)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) > 0) {
                    crc.update(buffer, 0, read);
                }
            }
            return (int) crc.getValue();
        }
    }

    /** 把 `LoadedApk.mApplication` 与 `ActivityThread.mInitialApplication` 指向真实实例。 */
    private void swapApplicationReferences() {
        if (real == null) {
            return;
        }
        try {
            setField(loadedApk(), "mApplication", real);
            setField(activityThread(), "mInitialApplication", real);
        } catch (Throwable t) {
            Log.w(TAG, "swap application references failed: " + t.getClass().getName());
        }
    }

    /**
     * 读取、校验并解密全部载荷分片（`assets/payload/dex-<n>.bin`，从 0 开始连续）。
     *
     * 先对**密文**算整体 SHA-256 与 `assets/payload/sum.txt` 比对，再解密：
     * - 摘要错了说明 payload 被替换（打包器与壳是同一次构建产出的，正常不会不匹配）；
     * - 失败时抛异常向上冒泡 → 壳不加载业务代码 → 应用启动即失败（fail-closed），
     *   而不是"跳过校验继续跑"。
     */
    private static ByteBuffer[] readPayloads(Context context) throws Exception {
        List<byte[]> chunks = new ArrayList<>();
        for (int index = 0; ; index++) {
            byte[] raw = readAsset(context, "payload/dex-" + index + ".bin");
            if (raw == null) {
                break;
            }
            chunks.add(raw);
        }
        if (chunks.isEmpty()) {
            throw new IOException("no payload found in assets/payload");
        }
        verifyPayloadSum(context, chunks);

        List<ByteBuffer> buffers = new ArrayList<>(chunks.size());
        for (byte[] chunk : chunks) {
            // 与打包任务相反的顺序：先 XOR 还原，再 inflate 得到原始 dex
            buffers.add(ByteBuffer.wrap(inflate(decode(chunk))));
        }
        return buffers.toArray(new ByteBuffer[0]);
    }

    /** 校验 payload 整体 SHA-256；不一致直接抛异常（fail-closed）。 */
    private static void verifyPayloadSum(Context context, List<byte[]> chunks) throws Exception {
        byte[] expectedRaw = readAsset(context, "payload/sum.txt");
        if (expectedRaw == null) {
            throw new IOException("missing payload/sum.txt");
        }
        String expected = new String(expectedRaw, StandardCharsets.UTF_8).trim().toLowerCase();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (byte[] chunk : chunks) {
            digest.update(chunk);
        }
        byte[] hashed = digest.digest();
        StringBuilder actual = new StringBuilder(hashed.length * 2);
        for (byte value : hashed) {
            actual.append(String.format("%02x", value & 0xFF));
        }
        if (!actual.toString().equals(expected)) {
            Log.e(TAG, "payload sum mismatch: payload modified");
            throw new IllegalStateException("payload integrity check failed");
        }
        Log.i(TAG, "payload sum ok (" + chunks.size() + " chunks)");
    }

    private static byte[] readAsset(Context context, String name) {
        try (InputStream input = context.getAssets().open(name)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(8192, input.available()));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** 与打包任务的 deflate 对应：载荷是「先压缩再混淆」的，需先解码再解压。 */
    private static byte[] inflate(byte[] compressed) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 3);
        byte[] buffer = new byte[64 * 1024];
        while (!inflater.finished()) {
            int read = inflater.inflate(buffer);
            if (read == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) {
                    break;
                }
            } else {
                out.write(buffer, 0, read);
            }
        }
        inflater.end();
        return out.toByteArray();
    }

    /** 与打包任务对应的可逆混淆（不是密码学加密，只阻止 `unzip` 后直接 jadx）。 */
    private static byte[] decode(byte[] data) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ (XOR_BASE + i));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 反射工具
    // ------------------------------------------------------------------

    private static Object activityThread() throws Exception {
        Class<?> clazz = Class.forName("android.app.ActivityThread");
        Method current = clazz.getDeclaredMethod("currentActivityThread");
        current.setAccessible(true);
        return current.invoke(null);
    }

    private static Object loadedApk() throws Exception {
        Object thread = activityThread();
        Object boundApp = readField(thread, "mBoundApplication");
        return readField(boundApp, "info");
    }

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Throwable t) {
            // 字段在部分版本可能不存在（例如 mApplication 被改名），不阻断安装流程，
            // 但需要留痕：Hilt 若因此失败，日志里能看到是哪一步没生效。
            Log.w(TAG, "setField failed: " + name + " (" + t.getClass().getName() + ")");
        }
    }
}
