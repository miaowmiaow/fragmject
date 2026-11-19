// JNI 注册表、进程加固、APK 签名守护线程（自 security.cpp 拆出后保留）。

#include "sec_common.h"
#include "sec_jni.h"

namespace {


/**
 * APK 签名证书比对。
 *
 * 期望摘要来自构建期编译进 so 的 SEC_EXPECTED_SIG_SHA256（不在 dex 中）；
 * APK 路径由 dladdr 从本库的真实加载位置推导，不取 Java 传入的 sourceDir。
 *
 * @return 0=匹配；1=不匹配；-1=无法判定（未配置期望摘要、路径推导失败、签名块结构异常）。
 */

/** 原生守护线程的复验间隔（秒）。间隔越短，运行期被替换后的暴露时间越短。 */
constexpr unsigned kSignatureGuardIntervalSec = 5;

/**
 * 守护线程心跳周期（以复验次数计）。12 次 × 5 秒 ≈ 60 秒输出一条。
 *
 * 心跳证明守护线程仍在周期性运行：现有的 `signature guard started` 只说明启动过一次，
 * 无法区分「线程存活」与「线程已退出 / 被挂起」。心跳只读计数，不影响复验与终止路径。
 */
constexpr unsigned kSignatureGuardHeartbeatEvery = 12;

/**
 * 判定为篡改后的终止延迟（毫秒），与 SecurityConfig.blockGraceMs 对齐。
 *
 * 为什么不立即终止：若在 JNI_OnLoad（Application 启动早期）就 SIGKILL，界面尚未显示，
 * ActivityManager 会把它当作启动失败并反复拉起，实测 12 秒内 86 次。延迟到界面显示之后再终止，
 * 与 Java 侧阻断路径的行为一致，只产生一次启动。
 */
constexpr unsigned kMismatchKillDelayMs = 1200;

void terminateAfterGrace() {
    usleep(kMismatchKillDelayMs * 1000);
    kill(getpid(), SIGKILL);
}


/**
 * APK 签名证书比对的核心实现，供 JNI 入口与原生守护线程共用。
 *
 * 期望摘要来自构建期编译进 so 的 SEC_EXPECTED_SIG_SHA256；
 * APK 路径由 dladdr 从本库的真实加载位置推导，不取 Java 传入的 sourceDir。
 *
 * 只有 kMismatch 才会触发终止；kUnknown（无法判定）只由调用方决定如何处理，
 * 避免在个别 ROM 或特殊安装形式下因读取失败而误杀。
 */
int verifyApkSignatureNative() {
    static const std::vector<apksig::Digest> expected = [] {
        std::vector<apksig::Digest> out;
        if (!apksig::parseDigestList(SEC_EXPECTED_SIG_SHA256, out)) {
            out.clear();
        }
        return out;
    }();
    if (expected.empty()) return apksig::kUnknown;

    static const std::string apkPath =
            apksig::resolveApkPathFromLibrary(reinterpret_cast<const void *>(&verifyApkSignatureNative));
    if (apkPath.empty()) return apksig::kUnknown;
    return apksig::verifyApkCertificates(apkPath, expected);
}


/**
 * 原生守护线程：不经过 Java 层，周期性复验签名。
 *
 * 为什么需要：Java 侧的阻断判定位于 dex 中，apktool 可以删除分支后重签（已在模拟器上复现）。
 * 守护线程在 native 中独立执行终止，不依赖 Java 分支。
 */
void *signatureGuardLoop(void *) {
    SEC_LOGD("signature guard started, interval=%us", kSignatureGuardIntervalSec);
    // 线程内局部计数：只有本线程读写，无需加锁。
    unsigned checks = 0;
    for (;;) {
        if (verifyApkSignatureNative() == apksig::kMismatch) {
            SEC_LOGE("native signature mismatch, terminating after %ums", kMismatchKillDelayMs);
            terminateAfterGrace();
        }
        ++checks;
        if (checks % kSignatureGuardHeartbeatEvery == 0) {
            SEC_LOGD("signature guard heartbeat: checks=%u interval=%us", checks, kSignatureGuardIntervalSec);
        }
        sleep(kSignatureGuardIntervalSec);
    }
    return nullptr;
}


/**
 * 在 JNI_OnLoad 中启动守护：分离线程周期复验。线程首次复验不等待，因此加载后立即检查。
 * 若线程无法创建，则同步校验一次并按宽限期终止，保证篡改包仍会被拦截。
 */
void startSignatureGuard() {
    static bool started = false;
    if (started) return;
    started = true;

    pthread_attr_t attr;
    if (pthread_attr_init(&attr) != 0) {
        SEC_LOGW("signature guard: pthread_attr_init failed");
        if (verifyApkSignatureNative() == apksig::kMismatch) terminateAfterGrace();
        return;
    }
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_t thread;
    const int rc = pthread_create(&thread, &attr, signatureGuardLoop, nullptr);
    pthread_attr_destroy(&attr);
    if (rc != 0) {
        SEC_LOGW("signature guard: pthread_create failed rc=%d, falling back to one-shot check", rc);
        if (verifyApkSignatureNative() == apksig::kMismatch) terminateAfterGrace();
    }
}

}  // namespace

extern "C" {


// ============================================================================
// JNI 入口
//
// 采用「动态注册」：JNI_OnLoad 里用 RegisterNatives 绑定 [SecurityNative] 的方法。
//
// 为什么从静态注册改过来：
// - 静态注册要求 C++ 函数名是 Java_<包名下划线>_<类名>_<方法名>，于是 proguard 必须
//   keep 整个类（类名 + 全部成员名），检测逻辑完全不可混淆，且这些符号会出现在
//   .dynsym 里，`nm -D` 一望即知有哪些检测能力；
// - 动态注册后 .dynsym 不再有 Java_* 符号，keep 规则收窄为「类名 + native 方法名」，
//   其余成员可正常参与 R8 混淆。
//
// 代价（已知局限）：类名与方法名仍需保留，因为本文件按名注册（字符串经 XS 混淆，
// 二进制中无明文）。更彻底的隐藏属于「字符串加密 + 壳」的范畴。
//
// 失败语义：类找不到或注册失败时返回 JNI_ERR → System.loadLibrary 抛
// UnsatisfiedLinkError → Kotlin 侧降级为纯 JVM 检测，不崩溃。
//
// 所有方法都做了「失败返回 -1 / 不误杀」的设计——
// 检测能力失效时倾向于放行，而不是把正常用户拦在门外。
// ============================================================================

/**
 * 尝试用 ptrace(PTRACE_TRACEME)「占坑」。
 *
 * 原理：一个进程同一时间只能被一个 tracer 附加。自身先 TRACEME 后，
 * 外部调试器再用 PTRACE_ATTACH 会失败（EPERM），从而在「检测」之外
 * 增加一层「主动阻断附加」的能力。
 *
 * 局限：若进程已经被附加，TRACEME 会失败（返回 -1），这本身也是一个信号；
 * 但上层目前只记录日志、不直接判定（避免某些 ROM 的 ptrace 策略导致误杀）。
 *
 * @return 0=占坑成功，-1=失败（可能已被附加）。
 */
jint jniInstallAntiPtrace(JNIEnv *, jobject) {
    const long result = ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
    return result == 0 ? 0 : -1;
}


/**
 * 直接终止当前进程。
 *
 * 为什么用 SIGKILL 而不是 Java 的 Process.killProcess：
 * - 不经过 Java 层，攻击者无法通过 hook Java 方法取消终止；
 * - SIGKILL 不可被捕获/忽略，没有「清理钩子」可用。
 *
 * 为什么不在检测到异常的同一时刻立即调用，而是由上层延迟：
 * 延迟窗口用于让 UI 展示提示（更友好），且不削弱阻断效果——
 * 终止动作最终仍由 native 完成。
 */
/**
 * 进程内存加固（H6/S2 反 dump 的**可做部分**，见 README/文档的边界说明）。
 *
 * 做两件事：
 * 1. `prctl(PR_SET_DUMPABLE, 0)`：把 `/proc/self` 下各文件的所有者改成 root，
 *    使**同 uid 或非 root 的读取者**（adb shell、同用户恶意应用、bugreport 现场采集）
 *    无法再打开 `/proc/<pid>/mem`、`/proc/<pid>/maps`；
 * 2. `setrlimit(RLIMIT_CORE, 0)`：禁止 core dump，避免崩溃时把内存镜像落到磁盘。
 *
 * ============================ 明确挡不住什么 ============================
 * root 进程（BlackDex / FRIDA-DEXDump / 手工 gdb）仍然可以 ptrace 并读内存——
 * PR_SET_DUMPABLE 只改变 `/proc` 的访问主体，不阻止 root。所谓「反 dump」在这里
 * 是把「无 root 的取巧路径」堵死，而不是挡住专业脱壳。
 *
 * 副作用（已知、可接受、可回滚）：
 * - ANR / tombstone 仍能生成（debuggerd 以 root 身份运行）；
 * - 但 `am dumpheap`、部分厂商的现场采集工具可能取不到该进程数据；
 * - 因此做成 [SecurityConfig.antiDump] 可关，默认开。
 *
 * @return 0=已加固；-1=prctl 失败（例如被 seccomp 拦截）。
 */
jint jniHardenProcess(JNIEnv *, jobject) {
    // 1) 关闭 dumpable：/proc/self/* 归属 root
    if (prctl(PR_SET_DUMPABLE, 0, 0, 0, 0) != 0) {
        SEC_LOGW("%s", lit::kLogHardenFailed.str().c_str());
        return -1;
    }

    // 2) 关闭 core dump：即使崩溃也不落内存镜像
    struct rlimit noCore{};
    noCore.rlim_cur = 0;
    noCore.rlim_max = 0;
    setrlimit(RLIMIT_CORE, &noCore);

    // 读回内核里的真实状态：Android 各版本 /proc 归属策略不同，
    // 「设置成功」不等于「生效」，把实际值打出来便于线上核对。
    const int dumpable = prctl(PR_GET_DUMPABLE, 0, 0, 0, 0);
    SEC_LOGD("process hardened: dumpable=%d core=0", dumpable);
    (void) lit::kLogHardenDone;
    return 0;
}


/** JNI 入口：保持与 [SecurityNative.verifyApkSignature] 的契约（0/1/-1）。 */
jint jniVerifyApkSignature(JNIEnv *, jobject) {
    return verifyApkSignatureNative();
}


void jniKillSelf(JNIEnv *, jobject) {
    SEC_LOGW("jniKillSelf invoked");
    kill(getpid(), SIGKILL);
}


/**
 * 动态注册表：`id` 只用于本文件内部与 Kotlin 侧注释对齐，不参与运行时匹配。
 *
 * 方法名与签名必须与 [SecurityNative] 的 `external fun` 完全一致；
 * R8 下由 `-keepclasseswithmembernames` 保名。
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK || env == nullptr) {
        return JNI_ERR;
    }

    // 类名与方法名同样用 XS 混淆，二进制里不出现明文字符串。
    // 必须与 Kotlin 侧 SecurityNative 的包名/类名一致（R8 keep 保证不被重命名）。
    static const std::string className = lit::kSecurityNativeClass.str();
    jclass clazz = env->FindClass(className.c_str());
    if (clazz == nullptr) {
        // 类找不到通常意味着 keep 规则失效或包名被改：清掉挂起异常再返回
        env->ExceptionClear();
        SEC_LOGW("JNI_OnLoad: SecurityNative class not found");
        return JNI_ERR;
    }

    // 方法名/签名需要在注册期间保持稳定地址，故用 static 存储。
    static const std::string nameInstallAntiPtrace = lit::kNameInstallAntiPtrace.str();
    static const std::string nameReadTracerPid = lit::kNameReadTracerPid.str();
    static const std::string nameCountAnonymousExecSegments = lit::kNameCountAnonymousExecSegments.str();
    static const std::string nameFindFridaInMaps = lit::kNameFindFridaInMaps.str();
    static const std::string nameFindRootInMaps = lit::kNameFindRootInMaps.str();
    static const std::string nameVerifyLibcTextCrc = lit::kNameVerifyLibcTextCrc.str();
    static const std::string nameVerifySelfSoTextCrc = lit::kNameVerifySelfSoTextCrc.str();
    static const std::string nameVerifySelfSoDataCrc = lit::kNameVerifySelfSoDataCrc.str();
    static const std::string nameFindFridaThreads = lit::kNameFindFridaThreads.str();
    static const std::string nameFindFridaFiles = lit::kNameFindFridaFiles.str();
    static const std::string nameFindRootFiles = lit::kNameFindRootFiles.str();
    static const std::string nameKillSelf = lit::kNameKillSelf.str();
    static const std::string nameHardenProcess = lit::kNameHardenProcess.str();
    static const std::string nameCountPltHooks = lit::kNameCountPltHooks.str();
    static const std::string nameVerifyApkSignature = lit::kNameVerifyApkSignature.str();
    static const std::string sigNoArg = lit::kSigInt.str();
    static const std::string sigNoArgVoid = lit::kSigVoid.str();
    static const std::string sigStringArg = lit::kSigStringInt.str();

    const JNINativeMethod methods[] = {
            {const_cast<char *>(nameInstallAntiPtrace.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniInstallAntiPtrace)},
            {const_cast<char *>(nameReadTracerPid.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniReadTracerPid)},
            {const_cast<char *>(nameCountAnonymousExecSegments.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniCountAnonymousExecSegments)},
            {const_cast<char *>(nameFindFridaInMaps.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniScanMapsForSignals)},
            {const_cast<char *>(nameFindRootInMaps.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniFindRootInMaps)},
            {const_cast<char *>(nameVerifyLibcTextCrc.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniVerifyLibcTextCrc)},
            {const_cast<char *>(nameVerifySelfSoTextCrc.c_str()),
             const_cast<char *>(sigStringArg.c_str()),
             reinterpret_cast<void *>(jniVerifySelfSoTextCrc)},
            {const_cast<char *>(nameVerifySelfSoDataCrc.c_str()),
             const_cast<char *>(sigStringArg.c_str()),
             reinterpret_cast<void *>(jniVerifySelfSoDataCrc)},
            {const_cast<char *>(nameFindFridaThreads.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniCountThreadSignals)},
            {const_cast<char *>(nameFindFridaFiles.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniCountFileSignals)},
            {const_cast<char *>(nameFindRootFiles.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniFindRootFiles)},
            {const_cast<char *>(nameCountPltHooks.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniCountPltHooks)},
            {const_cast<char *>(nameHardenProcess.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniHardenProcess)},
            {const_cast<char *>(nameVerifyApkSignature.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniVerifyApkSignature)},
            {const_cast<char *>(nameKillSelf.c_str()),
             const_cast<char *>(sigNoArgVoid.c_str()),
             reinterpret_cast<void *>(jniKillSelf)},
    };

    const auto count = static_cast<jint>(sizeof(methods) / sizeof(methods[0]));
    if (env->RegisterNatives(clazz, methods, count) != JNI_OK) {
        env->ExceptionClear();
        SEC_LOGW("JNI_OnLoad: RegisterNatives failed");
        return JNI_ERR;
    }
    SEC_LOGD("JNI_OnLoad: registered %d methods", count);
    startSignatureGuard();
    return JNI_VERSION_1_6;
}

}  // extern "C"
