// maps 扫描：TracerPid、匿名可执行段、Frida/Root 特征（自 security.cpp 拆出，逻辑不变）。

#include "sec_common.h"
#include "sec_jni.h"

namespace {


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
    // 100ms：一轮检测内三个调用紧邻，远小于轮询间隔（最短 1s）
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

}  // namespace


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
