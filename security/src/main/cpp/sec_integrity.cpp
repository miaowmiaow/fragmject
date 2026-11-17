// 完整性校验：so 段 CRC、libc 代码段 CRC（自 security.cpp 拆出，逻辑不变）。

#include "sec_common.h"
#include "sec_jni.h"

namespace {


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
