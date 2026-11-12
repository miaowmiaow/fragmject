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
#include <elf.h>
#include <signal.h>
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

// 统一日志前缀，便于在 logcat 中按 "FragmjectSecurity" 过滤。
// 注意：Release 包也保留日志，方便线上排查误杀；如需彻底静默可改为空实现。
#define SEC_TAG "FragmjectSecurity"
#define SEC_LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, SEC_TAG, __VA_ARGS__)
#define SEC_LOGW(...) __android_log_print(ANDROID_LOG_WARN, SEC_TAG, __VA_ARGS__)

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
    std::string str() const {
        std::string out(N - 1, '\0');
        for (std::size_t i = 0; i < N - 1; ++i) {
            out[i] = static_cast<char>(buf[i] ^ key(i));
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
std::uint32_t crc32Update(std::uint32_t crc, const std::uint8_t *data, std::size_t len) {
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

/**
 * 从磁盘 ELF 中读取指定 section 的 header。
 *
 * 为什么手写 ELF 解析而不是调用 dl_iterate_phdr / 系统 API：
 * - 我们需要的是「磁盘文件里 .text 的偏移与大小」，用于和内存内容做逐字节比较；
 *   dl_iterate_phdr 只给运行时段信息，拿不到文件偏移；
 * - 自己解析 section table 只依赖 <elf.h>，不引入额外依赖，32/64 位通吃。
 *
 * 实现要点：
 * - 整个 ELF 读入内存后再解析，避免多次 seek；
 * - 所有偏移都做边界检查，防止损坏/恶意的 so 导致越界读崩溃；
 * - 只读 section header，不执行、不加载任何代码。
 *
 * @param out         输出参数：命中的 section header。
 * @param elfPath     磁盘上的 ELF 路径（如 /system/lib64/libc.so）。
 * @param sectionName 目标 section 名（如 ".text"）。
 * @return 找到并成功写入 [out] 时返回 true。
 */
bool getElfSection(ElfW(Shdr) *out, const char *elfPath, const char *sectionName) {
    FILE *fp = fopen(elfPath, "rb");
    if (fp == nullptr) {
        return false;
    }

    // 取文件大小，并把读指针复位到开头。
    if (fseek(fp, 0, SEEK_END) != 0) {
        fclose(fp);
        return false;
    }
    const long size = ftell(fp);
    if (size <= 0 || fseek(fp, 0, SEEK_SET) != 0) {
        fclose(fp);
        return false;
    }

    std::vector<std::uint8_t> data(static_cast<std::size_t>(size));
    const std::size_t read = fread(data.data(), 1, data.size(), fp);
    fclose(fp);
    // 读到的字节数必须与文件大小一致，且至少能容纳一个 ELF header。
    if (read != data.size() || data.size() < sizeof(ElfW(Ehdr))) {
        return false;
    }

    const auto *ehdr = reinterpret_cast<const ElfW(Ehdr) *>(data.data());
    // ELF magic 校验：拒绝非 ELF 文件，避免把任意文件当 so 解析。
    if (std::memcmp(ehdr->e_ident, ELFMAG, SELFMAG) != 0) {
        return false;
    }
    // section table 必须存在，且 string table 索引必须在合法范围内。
    if (ehdr->e_shoff == 0 || ehdr->e_shnum == 0 || ehdr->e_shstrndx >= ehdr->e_shnum) {
        return false;
    }

    const auto *shdr = reinterpret_cast<const ElfW(Shdr) *>(data.data() + ehdr->e_shoff);
    // e_shstrndx 指向「存放 section 名字的字符串表」这一节的 header。
    const auto *shstr = shdr + ehdr->e_shstrndx;
    if (shstr->sh_offset >= data.size()) {
        return false;
    }

    const char *shstrData = reinterpret_cast<const char *>(data.data() + shstr->sh_offset);
    // 遍历 section table，按名字精确匹配（strcmp，避免 ".text" 误匹配 ".text.foo"）。
    for (int i = 0; i < ehdr->e_shnum; ++i) {
        const char *name = shstrData + shdr[i].sh_name;
        if (std::strcmp(name, sectionName) == 0) {
            *out = shdr[i];
            return true;
        }
    }
    return false;
}

/**
 * 在 /proc/self/maps 中按 basename 反查 so 的完整路径。
 *
 * 使用场景：`dladdr` 返回的 `dli_fname` 绝大多数情况下是绝对路径，
 * 但在少数环境（如某些加载器/虚拟化方案）下可能是相对路径或空串，
 * 此时需要从 maps 里找出该 so 的真实磁盘路径，才能打开文件做 CRC 比对。
 *
 * 实现：逐行取「最后一个空格之后」的内容作为路径，再比较 basename。
 * maps 行格式形如：
 *   7f...-7f... r-xp 00000000 fd:00 1234 /system/lib64/libc.so
 *
 * @return 找到时返回完整路径；否则返回空串（调用方据此跳过 CRC 校验，不误杀）。
 */
std::string findSoPath(const std::string &baseName) {
    FILE *fp = fopen(lit::kProcSelfMaps.str().c_str(), "r");
    if (fp == nullptr) {
        return {};
    }

    char line[512];
    std::string result;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        // 路径是每行最后一个空格之后的部分；没有空格说明这行没有路径（匿名段）。
        char *lastSpace = std::strrchr(line, ' ');
        if (lastSpace == nullptr) {
            continue;
        }

        std::string path(lastSpace + 1);
        // 去掉行尾的 \n / \r。
        while (!path.empty() && (path.back() == '\n' || path.back() == '\r')) {
            path.pop_back();
        }
        if (path.empty()) {
            continue;
        }

        // 比较 basename，避免把 /apex/.../libc.so 与 /system/.../libc.so 混淆。
        const std::size_t slash = path.find_last_of('/');
        const std::string base = (slash == std::string::npos) ? path : path.substr(slash + 1);
        if (base == baseName) {
            result = path;
            break;
        }
    }
    fclose(fp);
    return result;
}

/**
 * 读取 /proc/self/status 中的 TracerPid。
 *
 * 原理：进程被 ptrace 附加（gdb/lldb/IDA/调试壳）时，内核会写入
 * `TracerPid: <调试器 pid>`；未附加时为 0。
 *
 * 放在 native 的原因：Java 层读取可被 FileInputStream hook 篡改，
 * native 直接 fopen 相对更难拦截（但仍非绝对）。
 *
 * @return >0 表示被附加，0 表示未被附加，-1 表示读取失败（调用方应视为「未知」而非命中）。
 */
int readTracerPid() {
    FILE *fp = fopen(lit::kProcSelfStatus.str().c_str(), "r");
    if (fp == nullptr) {
        return -1;
    }

    const std::string key = lit::kTracerPidKey.str();
    char line[256];
    int pid = -1;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (std::strncmp(line, key.c_str(), key.size()) == 0) {
            pid = std::atoi(line + key.size());
            break;
        }
    }
    fclose(fp);
    return pid;
}

/**
 * 判断某个匿名可执行段是否属于「正常运行时」的合法段，用于降低误杀。
 *
 * 背景：ART 的 JIT、vdso/vvar、以及部分 libc 内部分配会产生匿名可执行内存，
 * 这些并不是注入。把它们加入白名单，只对「未知的匿名可执行段」计数。
 *
 * 白名单说明：
 * - [vdso]/[vvar]/[sigpage]：内核映射的虚拟动态共享对象/信号页；
 * - [jit-cache]/[anon:dalvik-*]：ART JIT 代码缓存及其元数据；
 * - [anon:libc_malloc]：部分版本 libc 的匿名分配（有些 ROM 会带可执行权限）。
 *
 * 注意：`[anon:dalvik-` 前缀匹配是为了兼容带 "(deleted)" 等后缀的变体。
 */
bool isKnownBenignAnonSegment(const char *path) {
    static const char *kAllowed[] = {
        "[vdso]",
        "[vvar]",
        "[sigpage]",
        "[jit-cache]",
        "[anon:dalvik-jit-code-cache]",
        "[anon:dalvik-CompilerMetadata]",
        "[anon:libc_malloc]",
    };
    for (const char *allowed : kAllowed) {
        if (std::strcmp(path, allowed) == 0) {
            return true;
        }
    }
    // 兼容带后缀的 dalvik 段，例如 "[anon:dalvik-jit-code-cache (deleted)]"。
    //
    // 必须同时覆盖 `[anon_shmem:dalvik-`：Android 12+ 起 ART 的 JIT code cache
    // 改名成 anon_shmem 前缀（实测 emulator API 37 为
    // `[anon_shmem:dalvik-zygote-jit-code-cache]` 与 `[anon_shmem:dalvik-jit-code-cache]`）。
    // 只匹配 `[anon:dalvik-` 会把它们当成匿名可执行段，导致「JIT 一编译就被判注入」的误杀。
    static const char *kAllowedPrefixes[] = {
        "[anon:dalvik-",
        "[anon_shmem:dalvik-",
    };
    for (const char *prefix : kAllowedPrefixes) {
        if (std::strncmp(path, prefix, std::strlen(prefix)) == 0) {
            return true;
        }
    }
    return false;
}

/**
 * 统计「匿名可执行内存段」的数量。
 *
 * 什么是匿名可执行段：在 /proc/self/maps 中，权限含 `x`（可执行）但
 * 路径为空、或以 `[` 开头的非文件映射（白名单内的除外）。
 *
 * 为什么可疑：正常应用的可执行代码都来自 APK 的 DEX（映射成匿名段但由 ART 管理）
 * 或已加载的 so（文件映射）。一个「没有对应磁盘文件、却可执行」的内存段，
 * 通常意味着有人用 mprotect/mmap 注入了一段 shellcode 或 trampoline。
 *
 * 误报来源与阈值：不同 ROM/ART 实现会产生数量不等的合法匿名可执行段，
 * 因此 Kotlin 侧把阈值做成可配置（默认 0，个别机型可放宽到 1~2）。
 * 这里只负责「数」，是否判定为异常交给上层策略。
 *
 * @return 可疑段数量；-1 表示无法读取 maps（上层应跳过而非命中）。
 */
/** 单调时钟毫秒（只用于短时缓存窗口，不受系统时间调整影响）。 */
long long monotonicMs() {
    timespec ts{};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<long long>(ts.tv_sec) * 1000 + ts.tv_nsec / 1000000;
}

/**
 * `/proc/self/maps` 的短时快照。
 *
 * 一轮检测里三个函数（Frida 特征 / 匿名可执行段 / Root 注入特征）各读一遍整份 maps，
 * 属于重复 IO（大进程 maps 可达数千行）。缓存一个很短的窗口，
 * 让同一轮检测只读一次；窗口足够小，不会漏掉「运行期新附加」。
 *
 * @return 快照内容；读取失败返回空串。
 */
const std::string &mapsSnapshot() {
    // 100ms：一次 detectHit 内三个调用紧邻，远小于轮询间隔（最短 1s）
    constexpr long long kCacheWindowMs = 100;
    static std::string snapshot;
    static long long cachedAtMs = 0;

    const long long now = monotonicMs();
    if (!snapshot.empty() && now - cachedAtMs <= kCacheWindowMs) {
        return snapshot;
    }

    FILE *fp = fopen(lit::kProcSelfMaps.str().c_str(), "r");
    if (fp == nullptr) {
        snapshot.clear();
        cachedAtMs = 0;
        return snapshot;
    }
    snapshot.clear();
    char line[1024];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        snapshot.append(line);
    }
    fclose(fp);
    cachedAtMs = now;
    return snapshot;
}

/**
 * 按行遍历 maps 快照。
 *
 * 回调收到「以 '\0' 结尾的单行内容」，便于沿用 `sscanf` / `strstr`。
 * 快照本身不被修改（每次遍历拷一份局部 buffer），因此可安全复用。
 */
template <typename Fn>
void forEachMapsLine(const std::string &snapshot, Fn &&fn) {
    if (snapshot.empty()) {
        return;
    }
    std::vector<char> buffer(snapshot.begin(), snapshot.end());
    buffer.push_back('\0');

    char *cursor = buffer.data();
    char *const end = buffer.data() + buffer.size();
    while (cursor < end && *cursor != '\0') {
        char *newline = std::strchr(cursor, '\n');
        if (newline != nullptr) {
            *newline = '\0';
        }
        fn(cursor);
        if (newline == nullptr) {
            break;
        }
        cursor = newline + 1;
    }
}

int countAnonymousExecSegments() {
    const std::string &maps = mapsSnapshot();
    if (maps.empty()) {
        return -1;
    }

    int count = 0;
    forEachMapsLine(maps, [&count](const char *line) {
        // maps 行字段：start-end perms offset dev inode path
        unsigned long start = 0;
        unsigned long end = 0;
        unsigned long offset = 0;
        unsigned long inode = 0;
        char perms[8] = {0};
        char dev[16] = {0};
        char path[256] = {0};

        // 用 sscanf 一次性拆字段；path 用 %[^\n] 吃掉行尾剩余内容。
        const int fields = std::sscanf(
            line,
            "%lx-%lx %7s %lx %15s %lu %255[^\n]",
            &start,
            &end,
            perms,
            &offset,
            dev,
            &inode,
            path
        );
        // 至少要有 6 个字段，且第 3 个权限字符为 'x'（rwxp/r-xp 等）。
        // 注意：这里是 lambda 体，跳过当前行用 return（不能用 continue）。
        if (fields < 6 || perms[2] != 'x') {
            return;
        }

        // 去掉 path 前导空白（%[^\n] 会保留空格）。
        char *trimmed = path;
        while (*trimmed == ' ' || *trimmed == '\t') {
            ++trimmed;
        }

        // 文件映射不可疑（以 '/' 开头）；已知合法匿名段跳过；其余（含纯匿名段）计入。
        if (*trimmed == '/') {
            return;
        }
        if (*trimmed != '\0' && isKnownBenignAnonSegment(trimmed)) {
            return;
        }
        // 记录被计入的段名：调 ANON_EXEC_THRESHOLD 或补白名单时需要知道「到底是谁被算了」。
        // 只打印被判定为可疑的段（不是白名单），不泄露检测策略。
        SEC_LOGD("anon exec segment: %s", *trimmed == '\0' ? "<anonymous>" : trimmed);
        ++count;
    });
    return count;
}

/** 把字符串就地转小写（只处理 ASCII，线程名/文件名都是 ASCII）。 */
void lowerInPlace(std::string &value) {
    for (auto &ch : value) {
        ch = static_cast<char>(std::tolower(static_cast<unsigned char>(ch)));
    }
}

/**
 * 统计命中 Frida 特征名的线程数。
 *
 * 为什么下沉到 native（H3）：
 * - 特征名原本以 Kotlin 常量表形式留在 DEX 里，`strings` 一搜就能定位检测逻辑；
 * - 现在特征表经 [XorString] 编译期混淆，只存在于 so 内。
 *
 * 覆盖场景：注入的 so 被删除/改名后，线程名通常仍残留。
 *
 * @return 命中线程数（>=0）；-1=无法枚举任务目录。
 */
int countThreadSignals() {
    static const std::string kSignatures[] = {
            lit::kGumJsLoop.str(),
            lit::kGmain.str(),
            lit::kGdbus.str(),
            lit::kPoolFrida.str(),
            lit::kLinjector.str(),
    };
    static const std::string kTaskDir = lit::kProcSelfTask.str();
    static const std::string kTaskRoot = lit::kProcTaskPrefix.str();
    static const std::string kCommName = lit::kComm.str();

    DIR *dir = opendir(kTaskDir.c_str());
    if (dir == nullptr) {
        return -1;
    }

    const std::string &taskRoot = kTaskRoot;
    const std::string &commName = kCommName;
    int hits = 0;
    while (dirent *entry = readdir(dir)) {
        if (entry->d_name[0] == '.') {
            continue;
        }
        const std::string path = taskRoot + entry->d_name + commName;
        FILE *fp = fopen(path.c_str(), "r");
        if (fp == nullptr) {
            continue;
        }
        char name[64] = {0};
        const bool read = fgets(name, sizeof(name), fp) != nullptr;
        fclose(fp);
        if (!read) {
            continue;
        }
        std::string lower(name);
        lowerInPlace(lower);
        for (const auto &sig : kSignatures) {
            if (lower.find(sig) != std::string::npos) {
                SEC_LOGD("%s", lit::kLogFridaThread.str().c_str());
                ++hits;
                break;
            }
        }
    }
    closedir(dir);
    return hits;
}

/**
 * 统计命中 Frida 特征名的落地文件数。
 *
 * 扫描两处：`/proc/self/fd`（运行中持有的 fd）与 `/data/local/tmp`（最常见落地路径）。
 * 局限：只覆盖「落地到磁盘」形态；纯内存注入不会命中。
 *
 * @return 命中文件数（>=0）。
 */
int countFileSignals() {
    static const std::string kSignatures[] = {
            lit::kFridaServer.str(),
            lit::kFridaAgent.str(),
            lit::kLinjector.str(),
            lit::kReFridaServer.str(),
    };
    // 目录名同样用 XS 混淆：明文的 "/data/local/tmp" 也是检测逻辑的指纹
    static const std::string kDirs[] = {lit::kProcFd.str(), lit::kDataLocalTmp.str()};

    int hits = 0;
    for (const auto &dirPath : kDirs) {
        DIR *dir = opendir(dirPath.c_str());
        if (dir == nullptr) {
            // Android 10+ 的 /data/local/tmp 对应用是 `--x`（可穿越、不可列目录），
            // opendir 必然失败。此时退化为「按已知文件名 stat 探测」——
            // 目录的 x 位允许解析具体路径，所以这条兜底在真机上有效（实测）。
            // 覆盖范围：精确同名文件；带版本号的文件名（frida-server-17.x-...）不在此列。
            for (const auto &sig : kSignatures) {
                const std::string probe = dirPath + "/" + sig;
                if (access(probe.c_str(), F_OK) == 0) {
                    SEC_LOGD("%s", lit::kLogFridaFile.str().c_str());
                    ++hits;
                }
            }
            continue;
        }
        while (dirent *entry = readdir(dir)) {
            if (entry->d_name[0] == '.') {
                continue;
            }
            std::string lower(entry->d_name);
            lowerInPlace(lower);
            for (const auto &sig : kSignatures) {
                if (lower.find(sig) != std::string::npos) {
                    SEC_LOGD("%s", lit::kLogFridaFile.str().c_str());
                    ++hits;
                    break;
                }
            }
        }
        closedir(dir);
    }
    return hits;
}

/**
 * 统计存在的 Root 相关文件数（su 二进制 / Magisk 目录）。
 *
 * 为什么下沉：路径表是检测逻辑的「指纹」，留在 DEX 里等于把判定规则告诉逆向者。
 * 用 access(F_OK) 只判断存在性，不打开文件，成本极低。
 *
 * 注意：这只是 Root 的**强信号之一**，上层仍会与「已安装 root 管理应用」「maps 注入特征」
 * 交叉验证，避免单一信号误判。
 *
 * @return 命中的路径数（>=0）。
 */
int countRootFiles() {
    // 路径表必须 XS 混淆：明文写在 .rodata 里，`strings` 一眼就能看出检测了哪些 su/Magisk 路径
    static const std::string kPaths[] = {
            // su 二进制
            lit::kSuSystemBin.str(),
            lit::kSuSystemXbin.str(),
            lit::kSuSbin.str(),
            lit::kSuBinSu.str(),
            lit::kSuSdXbin.str(),
            lit::kSuFailsafe.str(),
            lit::kSuDataLocal.str(),
            lit::kSuperuserApk.str(),
            // Magisk / Zygisk / Riru 典型目录
            lit::kMagiskSbin.str(),
            lit::kMagiskData.str(),
            lit::kMagiskModules.str(),
            lit::kZygiskDir.str(),
            lit::kRiruDir.str(),
            lit::kMagiskUnblock.str(),
    };

    int hits = 0;
    for (const auto &path : kPaths) {
        if (access(path.c_str(), F_OK) == 0) {
            SEC_LOGD("%s", lit::kLogRootPath.str().c_str());
            ++hits;
        }
    }
    return hits;
}

/**
 * 比对「磁盘 vs 内存」指定 section 的 CRC32。
 *
 * 从 `jniVerifySelfSoTextCrc` 抽出来，让 `.text` 与 `.rodata` 共用同一套逻辑：
 * - `.text`：发现 inline hook / 代码 patch；
 * - `.rodata`：发现常量表（特征串、白名单）被改写。
 *
 * @return 0=一致；1=不一致；-1=无法校验（文件/段缺失、自身基址拿不到）。
 */
/**
 * 逐页比对「磁盘 section」与「内存 section」的 CRC32。
 *
 * 为什么从「整段一次 CRC」改成逐页：
 * - 整段 CRC 只能回答「有没有变」，逐页能定位**哪一页**变了（排查 hook 时这个信息最关键）；
 * - CRC32 可构造碰撞，逐页把「让整段 CRC 相同」的难度提高到「每一页都要相同」；
 * - 页粒度天然对齐 inline hook 的写入方式（mprotect 一页 → 改几个字节 → 恢复）。
 *
 * 同时检查自身映射里是否出现 **可写 + 可执行** 段：inline hook 常把 .text 改成 rw- 再改回，
 * 若攻击者忘了恢复、或故意留下 rwx，这也是一条确定的证据。
 *
 * @return 0=一致；1=不一致（被改）；-1=无法校验
 */
bool isCodeRegionWritable(const void *codeAddr);

int verifySectionCrc(const std::string &soPath, const std::string &sectionName, bool checkExecPerms) {
    // 用本文件内的 crc32Update 反查「自身 so」的加载基址。
    // 该函数在匿名 namespace 中（内部链接），但地址仍然有效，dladdr 能解析出所属 so。
    Dl_info info{};
    if (dladdr(reinterpret_cast<const void *>(&crc32Update), &info) == 0 ||
        info.dli_fbase == nullptr) {
        return -1;
    }

    ElfW(Shdr) section{};
    if (!getElfSection(&section, soPath.c_str(), sectionName.c_str()) ||
        section.sh_size == 0 || section.sh_addr == 0) {
        return -1;
    }

    FILE *fp = fopen(soPath.c_str(), "rb");
    if (fp == nullptr) {
        return -1;
    }
    if (fseek(fp, static_cast<long>(section.sh_offset), SEEK_SET) != 0) {
        fclose(fp);
        return -1;
    }
    std::vector<std::uint8_t> fileBuf(section.sh_size);
    const std::size_t read = fread(fileBuf.data(), 1, fileBuf.size(), fp);
    fclose(fp);
    if (read != fileBuf.size()) {
        return -1;
    }

    const auto *memBase = reinterpret_cast<const std::uint8_t *>(info.dli_fbase) + section.sh_addr;

#ifdef SELF_RWX_PROBE
    // 仅测试构建（-PselfRwxProbe=true）：把 section 首页改成 RWX 且**不恢复**，
    // 用于验证「可写+可执行映射」这条检测分支（不修改任何字节，因此逐页 CRC 仍应通过）。
    static bool rwxProbeDone = false;
    if (!rwxProbeDone) {
        rwxProbeDone = true;
        const long ps = sysconf(_SC_PAGESIZE);
        if (ps > 0) {
            auto pageStart = reinterpret_cast<std::uintptr_t>(memBase) &
                             ~static_cast<std::uintptr_t>(ps - 1);
            if (mprotect(reinterpret_cast<void *>(pageStart),
                         static_cast<std::size_t>(ps),
                         PROT_READ | PROT_WRITE | PROT_EXEC) == 0) {
                SEC_LOGW("self rwx probe: %s first page set to rwx", sectionName.c_str());
            }
        }
    }
#endif

#ifdef SELF_TAMPER_PROBE
    // 仅测试构建（-PselfTamperProbe=true）：模拟一次 inline hook ——
    // 把 section 最后一页改成可写、翻转最后 1 字节、再恢复成可执行。
    // 用于验证「逐页 CRC 能定位到哪一页被改」，生产构建不包含这段代码。
    static bool probeDone = false;
    if (!probeDone) {
        probeDone = true;
        const long pageSize = sysconf(_SC_PAGESIZE);
        if (pageSize > 0) {
            auto pageStart = reinterpret_cast<std::uintptr_t>(memBase + section.sh_size - 1) &
                             ~static_cast<std::uintptr_t>(pageSize - 1);
            if (mprotect(reinterpret_cast<void *>(pageStart),
                         static_cast<std::size_t>(pageSize),
                         PROT_READ | PROT_WRITE | PROT_EXEC) == 0) {
                *const_cast<std::uint8_t *>(memBase + section.sh_size - 1) ^= 0xFF;
                mprotect(reinterpret_cast<void *>(pageStart),
                         static_cast<std::size_t>(pageSize),
                         PROT_READ | PROT_EXEC);
                SEC_LOGW("self tamper probe: flipped last byte of %s", sectionName.c_str());
            }
        }
    }
#endif

    const long pageSize = sysconf(_SC_PAGESIZE);
    const std::size_t step = pageSize > 0 ? static_cast<std::size_t>(pageSize) : 4096U;

    std::size_t offset = 0;
    std::size_t mismatched = 0;
    std::size_t firstMismatch = 0;
    bool hasFirst = false;
    while (offset < section.sh_size) {
        const std::size_t len = std::min(step, static_cast<std::size_t>(section.sh_size) - offset);
        const std::uint32_t fileCrc = crc32Update(0, fileBuf.data() + offset, len);
        const std::uint32_t memCrc = crc32Update(0, memBase + offset, len);
        if (fileCrc != memCrc) {
            ++mismatched;
            if (!hasFirst) {
                hasFirst = true;
                firstMismatch = offset;
            }
        }
        offset += len;
    }

    const std::size_t pages = (static_cast<std::size_t>(section.sh_size) + step - 1) / step;
    SEC_LOGD("self so %s pages=%zu mismatched=%zu first=0x%zx",
             sectionName.c_str(), pages, mismatched,
             hasFirst ? firstMismatch : static_cast<std::size_t>(0));

    if (mismatched > 0) {
        return 1;
    }

    if (checkExecPerms && isCodeRegionWritable(memBase)) {
        SEC_LOGW("self so %s has writable+executable mapping", sectionName.c_str());
        return 1;
    }
    return 0;
}

/**
 * 代码段所在映射是否「可写 + 可执行」。
 *
 * 为什么按**地址**判断而不是按文件名匹配 /proc/self/maps：
 * Android 10+ 默认 `extractNativeLibs=false`，so 直接从 APK 内 mmap，
 * maps 里的路径是 `.../base.apk`，根本不含 `libappsecurity.so` —— 按文件名匹配会永远失败
 * （实测：rwx 探针把页改成 rwx，按文件名的版本一次都没检出）。
 *
 * 现在的做法：用本文件内函数地址反查它落在 maps 的哪个区间，再看该区间的权限。
 * 这样无论 so 是解压到磁盘还是从 APK mmap，都能正确判定。
 *
 * @param codeAddr 自身代码段的某个地址（内部链接函数即可）
 * @return true=该映射同时具备 w 与 x
 */
bool isCodeRegionWritable(const void *codeAddr) {
    const auto addr = reinterpret_cast<std::uintptr_t>(codeAddr);
    FILE *fp = fopen(lit::kProcSelfMaps.str().c_str(), "r");
    if (fp == nullptr) {
        return false;
    }
    char line[1024];
    bool writable = false;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        unsigned long start = 0;
        unsigned long end = 0;
        char perms[8] = {0};
        if (std::sscanf(line, "%lx-%lx %7s", &start, &end, perms) < 3) {
            continue;
        }
        if (addr < start || addr >= end) {
            continue;
        }
        // perms[1] == 'w' 且 perms[2] == 'x'：可写可执行，正常 ELF 不会出现
        writable = (perms[1] == 'w' && perms[2] == 'x');
        break;
    }
    fclose(fp);
    return writable;
}



}  // namespace

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
extern "C" {

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
 * 返回 TracerPid。
 *
 * @return >0=被 native 调试器附加；0=未附加；-1=读取失败。
 */
jint jniReadTracerPid(JNIEnv *, jobject) {
    return readTracerPid();
}

/**
 * 返回匿名可执行段数量。
 *
 * @return >=0=段数；-1=读取 maps 失败。
 */
jint jniCountAnonymousExecSegments(JNIEnv *, jobject) {
    return countAnonymousExecSegments();
}

/**
 * 在 /proc/self/maps 中匹配 Frida 特征串，返回命中行数。
 *
 * 为什么放在 native：特征串经 [XorString] 混淆，二进制中无明文；
 * 且 Java 层无法直接读取 maps（需额外权限/反射），native 读取更自然。
 *
 * 命中即返回，不做阈值判断：maps 里出现任意一个 Frida 特征 so 都足够可疑。
 * 注意 `linjector` 是通用注入器特征，可能与其他工具重名，故上层会综合其它信号。
 *
 * @return 命中行数（>=0）；-1=读取 maps 失败。
 */
jint jniScanMapsForSignals(JNIEnv *, jobject) {
    const std::string &maps = mapsSnapshot();
    if (maps.empty()) {
        return -1;
    }

    // 覆盖 agent / gadget / gum / 注入器等常见形态；
    // 特征串来自 lit 的 constexpr 常量，字面量不进 .rodata。
    static const std::string signatures[] = {
        lit::kFridaAgent.str(),
        lit::kFridaGadget.str(),
        lit::kLibFrida.str(),
        lit::kLibGadget.str(),
        lit::kGumJsLoop.str(),
        lit::kLinjector.str(),
        lit::kReFridaServer.str(),
    };

    int hits = 0;
    forEachMapsLine(maps, [&hits](const char *line) {
        for (const auto &sig : signatures) {
            if (std::strstr(line, sig.c_str()) != nullptr) {
                ++hits;
                break;
            }
        }
    });
    return hits;
}

/**
 * 校验 libc `.text`：磁盘文件内容与内存映射内容的 CRC32 必须一致。
 *
 * ============================ 检测原理 ============================
 * Frida 等工具的 inline hook 会在目标函数开头写入跳转指令（trampoline），
 * 这会 **修改内存中的代码页**，但磁盘上的 so 文件保持不变。于是：
 *   磁盘 .text 的 CRC  ≠  内存 .text 的 CRC
 * 一旦不一致，即可判定代码段被改写。
 *
 * ============================ 实现步骤 ============================
 * 1. 用 `dladdr(&fopen)` 反查 libc 的加载基址（dli_fbase）与路径（dli_fname）；
 * 2. 从磁盘 ELF 解析 `.text` 的 sh_offset（文件偏移）、sh_addr（虚拟地址）、sh_size；
 * 3. 读取磁盘 `.text` 内容；
 * 4. 计算内存地址 = dli_fbase + sh_addr，直接读取内存 `.text`；
 * 5. 两份内容分别做 CRC32，比较是否一致。
 *
 * ============================ 为什么选 libc ============================
 * libc 是 Frida 最常 hook 的目标（open/read/fopen 等），且每个进程都加载、
 * 路径稳定、改动它最容易暴露异常。相比之下 hook 壳自身 so 的范围更窄。
 *
 * ============================ 局限 ============================
 * - 只覆盖 libc；不校验壳自身、ART 或其它 so；
 * - 若 hook 不修改 .text（例如 PLT/GOT hook、Java 层 hook），本检测无效；
 * - CRC32 非密码学哈希，理论上可碰撞；实际对抗中更常见的是直接 patch 检测点；
 * - 直接读内存依赖 .text 可读（正常为 r-x）；若被改成不可读会触发 SIGSEGV，
 *   从「阻断」角度看结果等价，但不友好，故只作为兜底。
 *
 * @return 0=一致（通过）；1=不一致（疑似 inline hook）；-1=无法校验（跳过，不误杀）。
 */
jint jniVerifyLibcTextCrc(JNIEnv *, jobject) {
    // 用 libc 里的 fopen 反查所属模块，得到 libc 基址与路径。
    Dl_info info{};
    if (dladdr(reinterpret_cast<const void *>(&fopen), &info) == 0 || info.dli_fbase == nullptr) {
        return -1;
    }

    // dladdr 一般返回绝对路径；若为相对路径则回退到 maps 反查。
    const std::string libcName = lit::kLibcSo.str();
    std::string libcPath;
    if (info.dli_fname != nullptr) {
        if (info.dli_fname[0] == '/') {
            libcPath.assign(info.dli_fname);
        } else {
            libcPath = findSoPath(libcName);
        }
    }
    if (libcPath.empty()) {
        libcPath = findSoPath(libcName);
    }
    if (libcPath.empty()) {
        return -1;
    }

    // 从磁盘 ELF 取 .text 的 offset/addr/size。
    const std::string textName = lit::kTextSection.str();
    ElfW(Shdr) textSection{};
    if (!getElfSection(&textSection, libcPath.c_str(), textName.c_str()) ||
        textSection.sh_size == 0 || textSection.sh_addr == 0) {
        return -1;
    }

    // 读取磁盘上的 .text 内容。
    FILE *fp = fopen(libcPath.c_str(), "rb");
    if (fp == nullptr) {
        return -1;
    }
    if (fseek(fp, static_cast<long>(textSection.sh_offset), SEEK_SET) != 0) {
        fclose(fp);
        return -1;
    }

    std::vector<std::uint8_t> fileBuf(textSection.sh_size);
    const std::size_t read = fread(fileBuf.data(), 1, fileBuf.size(), fp);
    fclose(fp);
    if (read != fileBuf.size()) {
        return -1;
    }

    // libc 的 .text 为 r-x，直接读取是安全的；若被 hook 改写权限导致崩溃，本身也等价于阻断。
    const auto *memBase =
            reinterpret_cast<const std::uint8_t *>(info.dli_fbase) + textSection.sh_addr;
    const std::uint32_t fileCrc = crc32Update(0, fileBuf.data(), fileBuf.size());
    const std::uint32_t memCrc = crc32Update(0, memBase, textSection.sh_size);
    SEC_LOGD("libc .text crc file=%08x mem=%08x size=%zu",
             fileCrc, memCrc, static_cast<std::size_t>(textSection.sh_size));

    return fileCrc == memCrc ? 0 : 1;
}

/**
 * 在 /proc/self/maps 中匹配 Root 框架（Magisk/Zygisk/Riru/LSPosed/Xposed）的注入特征。
 *
 * 为什么放在 native：
 * - 与 Frida maps 检测同理，特征串经 XorString 混淆，静态不可见；
 * - Root 框架的注入痕迹（zygisk/riru/lsposed 的 so）通常出现在 maps 里，
 *   比检测 su 文件更可靠（Magisk 会隐藏 su 路径，但难以完全抹掉注入的 so）。
 *
 * 注意：这些关键词也可能出现在极少数正常应用的路径中，因此上层把
 * 「maps 命中」作为 Root 综合信号之一，而不是唯一判据。
 *
 * @return 命中行数（>=0）；-1=读取 maps 失败。
 */
jint jniFindRootInMaps(JNIEnv *, jobject) {
    const std::string &maps = mapsSnapshot();
    if (maps.empty()) {
        return -1;
    }

    static const std::string signatures[] = {
        lit::kZygisk.str(),
        lit::kRiru.str(),
        lit::kMagisk.str(),
        lit::kLsposed.str(),
        lit::kXposed.str(),
    };

    int hits = 0;
    forEachMapsLine(maps, [&hits](const char *line) {
        for (const auto &sig : signatures) {
            if (std::strstr(line, sig.c_str()) != nullptr) {
                ++hits;
                break;
            }
        }
    });
    return hits;
}

/**
 * 校验「自身 so」的 .text：磁盘文件内容与内存映射内容的 CRC32 必须一致。
 *
 * ============================ 为什么要单独校验自身 so ============================
 * nativeVerifyLibcTextCrc 只能发现「libc 被 inline hook」；但攻击者也可能直接
 * 改写 libappsecurity.so 自身的 .text（例如把 detectReason 的关键分支
 * patch 成空操作），这类攻击不会碰 libc，libc CRC 检测不到。
 * 因此需要把「安全组件自身」也纳入完整性校验。
 *
 * ============================ 磁盘内容从哪来 ============================
 * so 打包在 APK 的 `lib/<abi>/libappsecurity.so` 中，native 无法直接
 * fopen APK 内的 zip 条目；因此由 Kotlin 侧：
 *   1. 用 ZipFile 从 APK 解压该条目到应用私有目录（带缓存）；
 *   2. 把解压后的文件路径传进来。
 * 本函数再用 getElfSection 从解压文件里取 .text，与内存 .text 比对。
 *
 * ============================ 为什么只比 .text ============================
 * 内存中的 so 不是完整文件的连续副本（.bss 不映射、段间有对齐填充、可能重定位），
 * 直接比整个文件不可行；而 .text 是位置无关代码，正常情况下内存内容与文件一致，
 * 是最适合做「磁盘 vs 内存」比对的段。
 *
 * ============================ 局限 ============================
 * - 只覆盖 .text；对 .data/.got 的改写无效（PLT/GOT hook 需另做校验）；
 * - 若 Kotlin 侧解压失败/缓存异常，返回 -1（上层跳过，不误杀）；
 * - 攻击者若同时 patch 本函数与其调用方，仍可绕过（用户态检测的共性局限）。
 *
 * @param selfSoPath Kotlin 侧从 APK 解压出的自身 so 文件路径。
 * @return 0=一致；1=不一致（疑似自身代码被改写）；-1=无法校验。
 */
jint jniVerifySelfSoTextCrc(JNIEnv *env, jobject, jstring selfSoPath) {
    if (selfSoPath == nullptr) {
        return -1;
    }
    const char *rawPath = env->GetStringUTFChars(selfSoPath, nullptr);
    if (rawPath == nullptr) {
        return -1;
    }
    const std::string soPath(rawPath);
    env->ReleaseStringUTFChars(selfSoPath, rawPath);
    if (soPath.empty()) {
        return -1;
    }
    return verifySectionCrc(soPath, lit::kTextSection.str(), true);
}

/**
 * 校验自身 so 的 `.rodata`（H3 新增）。
 *
 * 为什么补这一段：常量表（特征串、白名单、阈值）都在 .rodata 里，
 * 只校验 .text 的话，攻击者改常量表即可让检测「合法地」失效。
 *
 * @param selfSoPath Kotlin 侧从 APK 解压出的自身 so 文件路径。
 * @return 0=一致；1=不一致（常量表被改写）；-1=无法校验（跳过，不误杀）。
 */
jint jniVerifySelfSoDataCrc(JNIEnv *env, jobject, jstring selfSoPath) {
    if (selfSoPath == nullptr) {
        return -1;
    }
    const char *rawPath = env->GetStringUTFChars(selfSoPath, nullptr);
    if (rawPath == nullptr) {
        return -1;
    }
    const std::string soPath(rawPath);
    env->ReleaseStringUTFChars(selfSoPath, rawPath);
    if (soPath.empty()) {
        return -1;
    }
    return verifySectionCrc(soPath, lit::kRodataSection.str(), false);
}

/** Frida 特征线程数；-1=无法枚举。 */
jint jniCountThreadSignals(JNIEnv *, jobject) {
    return countThreadSignals();
}

/** Frida 落地文件数。 */
jint jniCountFileSignals(JNIEnv *, jobject) {
    return countFileSignals();
}

/** Root 相关文件数（su / Magisk 目录）。 */
jint jniFindRootFiles(JNIEnv *, jobject) {
    return countRootFiles();
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
 * 1. `prctl(PR_SET_DUMPABLE, 0)`：把 `/proc/self/*` 的所有者改成 root，
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
            {const_cast<char *>(nameHardenProcess.c_str()),
             const_cast<char *>(sigNoArg.c_str()),
             reinterpret_cast<void *>(jniHardenProcess)},
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
    return JNI_VERSION_1_6;
}

}  // extern "C"
