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
//   - 所有特征字符串通过 [XorString] 在编译期异或，二进制中不出现明文；
//   - 命中后由 [nativeKillSelf] 直接 `kill(getpid(), SIGKILL)`，不经过 Java 分支。
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
#include <dlfcn.h>
#include <elf.h>
#include <signal.h>
#include <sys/ptrace.h>
#include <unistd.h>

#include <array>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
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
 * 编译期 XOR 字符串混淆。
 *
 * 原理：把字符串字面量在 **编译期** 逐字节与派生密钥异或，结果存入 [buf]。
 * 由于构造是 `constexpr`，`XorString("frida-agent")` 的 `buf` 会在编译期算好，
 * 最终进入 .rodata 的是「异或后的字节」，而不是 "frida-agent" 明文。
 * 运行时调用 [str] 再按同样规则还原，因此不会出现明文常量。
 *
 * 为什么不用现成的 obfuscate 库（如 dpt-shell 用的 Adam Yaxley obfuscate.h）：
 * 这里只需覆盖少量短字符串，自己实现一个 40 行的模板即可，避免额外依赖与许可证传递。
 *
 * 密钥为什么不存起来：`key(i)` 由索引直接推导（0x5A + i * 31），
 * 无需在二进制里保存密钥本身。这不是密码学意义上的加密，
 * 目的只是让 `strings`/静态特征扫描无法直接命中，属于「提高成本」而非「保密」。
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

// 便捷宏：XS("...") 等价于「混淆存储 + 运行时还原」。
// 注意 CTAD（类模板参数推导）需要 C++17，见 CMakeLists 的 cxx_std_17。
#define XS(literal) (XorString(literal).str())

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
    FILE *fp = fopen(XS("/proc/self/maps").c_str(), "r");
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
    FILE *fp = fopen(XS("/proc/self/status").c_str(), "r");
    if (fp == nullptr) {
        return -1;
    }

    const std::string key = XS("TracerPid:");
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
    return std::strncmp(path, "[anon:dalvik-", 13) == 0;
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
int countAnonymousExecSegments() {
    FILE *fp = fopen(XS("/proc/self/maps").c_str(), "r");
    if (fp == nullptr) {
        return -1;
    }

    char line[512];
    int count = 0;
    while (fgets(line, sizeof(line), fp) != nullptr) {
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
        if (fields < 6 || perms[2] != 'x') {
            continue;
        }

        // 去掉 path 前导空白（%[^\n] 会保留空格）。
        char *trimmed = path;
        while (*trimmed == ' ' || *trimmed == '\t') {
            ++trimmed;
        }

        // 文件映射不可疑（以 '/' 开头）；已知合法匿名段跳过；其余（含纯匿名段）计入。
        if (*trimmed == '/') {
            continue;
        }
        if (*trimmed != '\0' && isKnownBenignAnonSegment(trimmed)) {
            continue;
        }
        ++count;
    }
    fclose(fp);
    return count;
}

}  // namespace

// ============================================================================
// JNI 入口
//
// 采用「静态注册」：函数名遵循 Java_<包名下划线>_<类名>_<方法名> 约定，
// 与 AppSecurityGuard.kt 中的 external fun 一一对应。
//
// 为什么不用 JNI_OnLoad + RegisterNatives：
// 动态注册更隐蔽（符号不出现在 .dynsym），但需要在 native 侧硬编码类名/签名，
// 且 Release 混淆下同样要保留类名。静态注册更简单、编译期即可暴露命名错误，
// 配合 proguard 的 keep 规则已足够；隐蔽性主要由 XorString 提供。
//
// 注意：所有方法都做了「失败返回 -1 / 不误杀」的设计——
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
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeInstallAntiPtrace(
        JNIEnv *, jobject) {
    const long result = ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
    return result == 0 ? 0 : -1;
}

/**
 * 返回 TracerPid。
 *
 * @return >0=被 native 调试器附加；0=未附加；-1=读取失败。
 */
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeReadTracerPid(
        JNIEnv *, jobject) {
    return readTracerPid();
}

/**
 * 返回匿名可执行段数量。
 *
 * @return >=0=段数；-1=读取 maps 失败。
 */
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeCountAnonymousExecSegments(
        JNIEnv *, jobject) {
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
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeFindFridaInMaps(
        JNIEnv *, jobject) {
    FILE *fp = fopen(XS("/proc/self/maps").c_str(), "r");
    if (fp == nullptr) {
        return -1;
    }

    // 覆盖 agent / gadget / gum / 注入器等常见形态；
    // 全部经 XS 混淆，避免在 .rodata 中出现可被 strings 命中的明文。
    const std::string signatures[] = {
        XS("frida-agent"),
        XS("frida-gadget"),
        XS("libfrida"),
        XS("libgadget.so"),
        XS("gum-js-loop"),
        XS("linjector"),
        XS("re.frida.server"),
    };

    char line[512];
    int hits = 0;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        for (const auto &sig : signatures) {
            if (std::strstr(line, sig.c_str()) != nullptr) {
                ++hits;
                break;
            }
        }
    }
    fclose(fp);
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
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeVerifyLibcTextCrc(
        JNIEnv *, jobject) {
    // 用 libc 里的 fopen 反查所属模块，得到 libc 基址与路径。
    Dl_info info{};
    if (dladdr(reinterpret_cast<const void *>(&fopen), &info) == 0 || info.dli_fbase == nullptr) {
        return -1;
    }

    // dladdr 一般返回绝对路径；若为相对路径则回退到 maps 反查。
    const std::string libcName = XS("libc.so");
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
    const std::string textName = XS(".text");
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
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeFindRootInMaps(
        JNIEnv *, jobject) {
    FILE *fp = fopen(XS("/proc/self/maps").c_str(), "r");
    if (fp == nullptr) {
        return -1;
    }

    const std::string signatures[] = {
        XS("zygisk"),
        XS("riru"),
        XS("magisk"),
        XS("lsposed"),
        XS("xposed"),
    };

    char line[512];
    int hits = 0;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        for (const auto &sig : signatures) {
            if (std::strstr(line, sig.c_str()) != nullptr) {
                ++hits;
                break;
            }
        }
    }
    fclose(fp);
    return hits;
}

/**
 * 校验「自身 so」的 .text：磁盘文件内容与内存映射内容的 CRC32 必须一致。
 *
 * ============================ 为什么要单独校验自身 so ============================
 * nativeVerifyLibcTextCrc 只能发现「libc 被 inline hook」；但攻击者也可能直接
 * 改写 libfragmject_security.so 自身的 .text（例如把 detectReason 的关键分支
 * patch 成空操作），这类攻击不会碰 libc，libc CRC 检测不到。
 * 因此需要把「安全组件自身」也纳入完整性校验。
 *
 * ============================ 磁盘内容从哪来 ============================
 * so 打包在 APK 的 `lib/<abi>/libfragmject_security.so` 中，native 无法直接
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
JNIEXPORT jint JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeVerifySelfSoTextCrc(
        JNIEnv *env, jobject, jstring selfSoPath) {
    if (selfSoPath == nullptr) {
        return -1;
    }

    const char *rawPath = env->GetStringUTFChars(selfSoPath, nullptr);
    if (rawPath == nullptr) {
        return -1;
    }
    std::string soPath(rawPath);
    env->ReleaseStringUTFChars(selfSoPath, rawPath);
    if (soPath.empty()) {
        return -1;
    }

    // 用本文件内的 crc32Update 反查「自身 so」的加载基址。
    // 该函数在匿名 namespace 中（内部链接），但地址仍然有效，dladdr 能解析出所属 so。
    Dl_info info{};
    if (dladdr(reinterpret_cast<const void *>(&crc32Update), &info) == 0 ||
        info.dli_fbase == nullptr) {
        return -1;
    }

    const std::string textName = XS(".text");
    ElfW(Shdr) textSection{};
    if (!getElfSection(&textSection, soPath.c_str(), textName.c_str()) ||
        textSection.sh_size == 0 || textSection.sh_addr == 0) {
        return -1;
    }

    FILE *fp = fopen(soPath.c_str(), "rb");
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

    const auto *memBase =
            reinterpret_cast<const std::uint8_t *>(info.dli_fbase) + textSection.sh_addr;
    const std::uint32_t fileCrc = crc32Update(0, fileBuf.data(), fileBuf.size());
    const std::uint32_t memCrc = crc32Update(0, memBase, textSection.sh_size);
    SEC_LOGD("self so .text crc file=%08x mem=%08x size=%zu",
             fileCrc, memCrc, static_cast<std::size_t>(textSection.sh_size));

    return fileCrc == memCrc ? 0 : 1;
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
JNIEXPORT void JNICALL
Java_com_example_fragmject_app_security_AppSecurityGuard_nativeKillSelf(
        JNIEnv *, jobject) {
    SEC_LOGW("nativeKillSelf invoked");
    kill(getpid(), SIGKILL);
}

}  // extern "C"
