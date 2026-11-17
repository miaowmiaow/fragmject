#pragma once

//
// Fragmject 应用安全检测 —— native 层实现。
//
// ============================ 为什么需要 native 层 ============================
// 纯 Java/Kotlin 实现的检测有两个结构性弱点：
//   1. 检测方法的返回值可被 Frida 直接 hook（例如把 `Debug.isDebuggerConnected()`
//      改成恒返回 false，或在 Java 层 patch 掉整个判断分支）；
//   2. 特征字符串（"frida-agent"、"/proc/self/status" 等）会以明文留在 DEX 里，
//      用 `strings`/`jadx` 一扫就能定位检测逻辑并逐个 patch。
//
// 因此本文件把三类「关键判定」下沉到 native：
//   - TracerPid（是否被 ptrace 附加）；
//   - 匿名可执行内存段（是否有无文件映射的可执行代码，典型内存注入特征）；
//   - libc `.text` 的「磁盘 vs 内存」CRC32 比对（发现 inline hook）。
//
// 同时：
//   - 特征字符串（Frida 线程名/落地文件、su 与 Magisk 路径）集中在 native，Java 层已清零；
//     但 `XS(...)` 宏只在运行期构造临时对象，编译器仍会保留字面量——见 [XorString] 的
//     「重要限制」说明，`strings` 仍能看到明文，这是已知待优化项；
//   - 命中后由 [jniKillSelf] 直接 `kill(getpid(), SIGKILL)`，不经过 Java 分支。
//
// ============================ 安全边界（务必知晓）============================
// 这些仍是「用户态静态特征检测」，不是不可绕过的强保护：
//   - /proc 内容可被 hook/伪造；ptrace 可被更底层的注入方式绕过；
//   - CRC32 不是密码学哈希，理论上可构造碰撞；更常见的是直接 patch 掉检测点；
//   - 只校验 libc 的 `.text`，不校验壳自身或其它 so，也不覆盖「只 hook 壳自身」的场景。
// 它的定位是抬高自动化扫描与低水平逆向的成本，不能替代服务端鉴权与密钥管理。
//

#include <jni.h>
#include <android/log.h>
#include <dirent.h>
#include <dlfcn.h>
#include <link.h>
#include <elf.h>
#include <signal.h>
#include <pthread.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/ptrace.h>
#include <sys/mman.h>
#include <unistd.h>

#include <array>
#include <cctype>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <string>
#include <vector>

#include "apk_signature.h"
#include "expected_sig.h"

// 统一日志前缀，便于在 logcat 中按 "FragmjectSecurity" 过滤。
// 注意：Release 包也保留日志，方便线上排查误杀；如需彻底静默可改为空实现。
#define SEC_TAG "FragmjectSecurity"
#define SEC_LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, SEC_TAG, __VA_ARGS__)
#define SEC_LOGW(...) __android_log_print(ANDROID_LOG_WARN, SEC_TAG, __VA_ARGS__)
#define SEC_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, SEC_TAG, __VA_ARGS__)

// bionic 的 <elf.h> 不一定提供 glibc 风格的 ElfW 宏（本工程在 NDK 28 上即缺失），
// 这里按位宽补齐：64 位展开为 Elf64_Shdr，32 位展开为 Elf32_Shdr。
// 用 #ifndef 保护，避免与未来 bionic 版本自带的定义冲突。
#ifndef ElfW
#if defined(__LP64__)
#define ElfW(type) Elf64_##type
#else
#define ElfW(type) Elf32_##type
#endif
#endif

namespace {

/**
 * XOR 字符串混淆。
 *
 * 原理：逐字节与派生密钥异或后存入 [buf]，运行时用 [str] 还原。
 *
 * ============================ 重要限制（实测） ============================
 * 只有**以 constexpr 对象形式声明**时才真正消除明文，例如：
 *
 * ```
 * constexpr auto kName = XorString("frida-agent");   // buf 编译期算好，字面量不落盘
 * ... kName.str() ...
 * ```
 *
 * 而便捷宏 `XS("frida-agent")` 会在**运行期**构造临时对象，编译器必须保留字面量本身
 * 作为构造参数——实测 `strings libappsecurity.so` 能看到明文。
 *
 * 因此本文件的约定是：
 * - **敏感字符串（特征名、路径、JNI 名字）一律放 `lit` 命名空间的 constexpr 常量**；
 * - `XS(...)` 只用于非敏感的日志/调试文案。
 *
 * 实测（2026-11-11）：改造后 `strings` 中 `frida` / `magisk` / `zygisk` / `/data/adb` /
 * `/system/bin/su` 全部 0 命中。仍未清除的只有错误日志里的类名 `SecurityNative`
 * （仅出现在 `JNI_OnLoad` 失败分支，不影响运行期）。
 *
 * 密钥为什么不存起来：`key(i)` 由索引直接推导（0x5A + i * 31），
 * 无需在二进制里保存密钥本身。这不是密码学意义上的加密，
 * 目的只是抬高静态扫描成本，属于「提高成本」而非「保密」。
 */
template <std::size_t N>
struct XorString {
    char buf[N];

    // 编译期完成异或；注意连同结尾 '\0' 一起处理，保证 buf 与源串等长。
    constexpr XorString(const char (&s)[N]) : buf{} {
        for (std::size_t i = 0; i < N; ++i) {
            buf[i] = static_cast<char>(s[i] ^ key(i));
        }
    }

    // 按索引派生密钥。乘 31 让相邻字节的密钥差异足够大，避免出现大段相同字节。
    static constexpr char key(std::size_t i) {
        return static_cast<char>(0x5A + i * 31);
    }

    // 运行时还原为明文。只返回前 N-1 个字符（去掉结尾 '\0'）。
    // 用 volatile 读取密文：-O2 下若解码循环被常量折叠，明文会直接落入 .rodata（实测 lsposed 因此泄漏）。
    std::string str() const {
        std::string out(N - 1, '\0');
        const volatile char *src = buf;
        for (std::size_t i = 0; i < N - 1; ++i) {
            out[i] = static_cast<char>(src[i] ^ key(i));
        }
        return out;
    }
};

// 便捷宏：XS("...") 等价于「运行期构造 + 还原」。
// 注意 CTAD（类模板参数推导）需要 C++17，见 CMakeLists 的 cxx_std_17。
//
// 再次强调 [XorString] 的「重要限制」：XS 会在运行期构造临时对象，
// 编译器必须保留字面量。**敏感字符串一律用下面 lit 命名空间里的 constexpr 常量**。
#define XS(literal) (XorString(literal).str())

/**
 * 敏感字面量集中区：用 `constexpr` 对象声明，字面量只在编译期参与计算，不落 .rodata。
 *
 * 用法：`lit::kFridaServer.str()`（每次调用生成一个新 std::string，调用点应放在
 * 函数的 `static const` 里，只初始化一次）。
 */
namespace lit {

// 文件路径
constexpr auto kProcSelfMaps = XorString("/proc/self/maps");
constexpr auto kProcSelfTask = XorString("/proc/self/task");
constexpr auto kProcTaskPrefix = XorString("/proc/self/task/");
constexpr auto kComm = XorString("/comm");
constexpr auto kProcFd = XorString("/proc/self/fd");
constexpr auto kDataLocalTmp = XorString("/data/local/tmp");
constexpr auto kProcSelfStatus = XorString("/proc/self/status");
constexpr auto kTracerPidKey = XorString("TracerPid:");
constexpr auto kLibcSo = XorString("libc.so");
constexpr auto kTextSection = XorString(".text");
constexpr auto kSecurityNativeClass = XorString("com/example/security/SecurityNative");
constexpr auto kRodataSection = XorString(".rodata");

// Frida 线程名
constexpr auto kGumJsLoop = XorString("gum-js-loop");
constexpr auto kGmain = XorString("gmain");
constexpr auto kGdbus = XorString("gdbus");
constexpr auto kPoolFrida = XorString("pool-frida");
constexpr auto kLinjector = XorString("linjector");

// Frida 落地文件 / maps 特征
constexpr auto kFridaServer = XorString("frida-server");
constexpr auto kFridaAgent = XorString("frida-agent");
constexpr auto kFridaGadget = XorString("frida-gadget");
constexpr auto kLibFrida = XorString("libfrida");
constexpr auto kLibGadget = XorString("libgadget.so");
constexpr auto kReFridaServer = XorString("re.frida.server");

// Root 注入特征（maps）
constexpr auto kZygisk = XorString("zygisk");
constexpr auto kRiru = XorString("riru");
constexpr auto kMagisk = XorString("magisk");
constexpr auto kLsposed = XorString("lsposed");
constexpr auto kXposed = XorString("xposed");

// Root 文件路径（su / Magisk 目录）
constexpr auto kSuSystemBin = XorString("/system/bin/su");
constexpr auto kSuSystemXbin = XorString("/system/xbin/su");
constexpr auto kSuSbin = XorString("/sbin/su");
constexpr auto kSuBinSu = XorString("/su/bin/su");
constexpr auto kSuSdXbin = XorString("/system/sd/xbin/su");
constexpr auto kSuFailsafe = XorString("/system/bin/failsafe/su");
constexpr auto kSuDataLocal = XorString("/data/local/su");
constexpr auto kSuperuserApk = XorString("/system/app/Superuser.apk");
constexpr auto kMagiskSbin = XorString("/sbin/.magisk");
constexpr auto kMagiskData = XorString("/data/adb/magisk");
constexpr auto kMagiskModules = XorString("/data/adb/modules");
constexpr auto kZygiskDir = XorString("/data/adb/zygisk");
constexpr auto kRiruDir = XorString("/data/adb/riru");
constexpr auto kMagiskUnblock = XorString("/dev/.magisk.unblock");


// JNI 方法名与签名（RegisterNatives 参数），同样以 constexpr 常量保存
constexpr auto kNameInstallAntiPtrace = XorString("installAntiPtrace");
constexpr auto kNameReadTracerPid = XorString("readTracerPid");
constexpr auto kNameCountAnonymousExecSegments = XorString("countAnonymousExecSegments");
constexpr auto kNameFindFridaInMaps = XorString("findFridaInMaps");
constexpr auto kNameFindRootInMaps = XorString("findRootInMaps");
constexpr auto kNameFindFridaThreads = XorString("findFridaThreads");
constexpr auto kNameFindFridaFiles = XorString("findFridaFiles");
constexpr auto kNameFindRootFiles = XorString("findRootFiles");
constexpr auto kNameVerifyLibcTextCrc = XorString("verifyLibcTextCrc");
constexpr auto kNameVerifySelfSoTextCrc = XorString("verifySelfSoTextCrc");
constexpr auto kNameVerifySelfSoDataCrc = XorString("verifySelfSoDataCrc");
constexpr auto kNameKillSelf = XorString("killSelf");
constexpr auto kNameHardenProcess = XorString("hardenProcess");
constexpr auto kNameCountPltHooks = XorString("countPltHooks");
constexpr auto kNameVerifyApkSignature = XorString("verifyApkSignature");
constexpr auto kSigInt = XorString("()I");
constexpr auto kSigVoid = XorString("()V");
constexpr auto kSigStringInt = XorString("(Ljava/lang/String;)I");

// 日志文案（避免出现 "frida"/"root" 之类关键词）
constexpr auto kLogFridaThread = XorString("thread sig hit");
constexpr auto kLogFridaFile = XorString("file sig hit");
constexpr auto kLogRootPath = XorString("root path hit");
constexpr auto kLogHardenDone = XorString("process hardened: dumpable=0, core=0");
constexpr auto kLogHardenFailed = XorString("prctl(PR_SET_DUMPABLE) failed");

}  // namespace lit

// CRC32（IEEE 802.3）反射多项式。与 zlib/minizip 使用的实现一致。
constexpr std::uint32_t kCrc32Poly = 0xEDB88320u;

/**
 * 标准 CRC32 增量计算。
 *
 * 为什么用 CRC32 而不是 SHA-256：
 * - 这里比较的是「同一台机器上、同一份文件」的磁盘内容与内存内容，
 *   目标是发现「内存页被改写」这一事实，而不是抵抗碰撞攻击；
 * - CRC32 足够快（libc .text 通常 1~3MB，单次计算在毫秒级），
 *   适合在启动路径和轮询中反复执行。
 *
 * 线程安全：CRC 表用函数内 static 初始化（C++11 起保证只初始化一次且线程安全），
 * 因此检测线程与其它调用方并发调用是安全的。
 *
 * @param crc 上一次计算的中间值；首次传 0。
 */
inline std::uint32_t crc32Update(std::uint32_t crc, const std::uint8_t *data, std::size_t len) {
    static const auto table = [] {
        std::array<std::uint32_t, 256> t{};
        for (std::uint32_t i = 0; i < 256; ++i) {
            std::uint32_t c = i;
            for (int k = 0; k < 8; ++k) {
                c = (c & 1u) ? (kCrc32Poly ^ (c >> 1)) : (c >> 1);
            }
            t[i] = c;
        }
        return t;
    }();

    crc = ~crc;
    for (std::size_t i = 0; i < len; ++i) {
        crc = table[(crc ^ data[i]) & 0xFFu] ^ (crc >> 8);
    }
    return ~crc;
}
}  // namespace
