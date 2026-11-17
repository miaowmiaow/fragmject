// GOT/PLT 劫持检测（自 security.cpp 拆出，逻辑不变）。

#include "sec_common.h"
#include "sec_jni.h"

namespace {


/** Frida 落地文件数。 */
/**
 * GOT / PLT 劫持检测。
 *
 * ============================ 检测什么 ============================
 * 攻击者最常用的两种 hook：
 * 1. **inline hook**：直接改函数开头几个字节（`.text` 逐页 CRC 已覆盖）；
 * 2. **GOT 劫持**：把本次 so 的「导入函数指针表」里某一项改成自己的实现
 *    （例如把 `fopen` 指到"过滤掉 frida-server 的版本"）。改的是数据段，**代码段完全不变**，
 *    所以逐页 CRC 抓不到 —— 这正是本函数要补的缺口。
 *
 * ============================ 怎么做 ============================
 * 不解析磁盘 ELF：通过 `dl_iterate_phdr` 拿到本模块的 `PT_DYNAMIC`，
 * 从动态表读出 `DT_SYMTAB / DT_STRTAB / DT_JMPREL / DT_RELA`（都是**运行期地址**），
 * 再对每个「导入符号」比对：
 *   GOT 表项里的地址  vs  dlsym(RTLD_DEFAULT, 符号名) 的结果
 * 不一致即视为被劫持。
 *
 * 只比对白名单里的「安全敏感导入」，避免把 IFUNC 解析差异或厂商私有实现当成 hook
 * —— 误报在这个模块上代价很高。
 *
 * @return 被劫持的条目数（>=0）；-1=无法解析（视为不启用，放行）
 */

constexpr int kRelocJumpSlotAArch64 = 1026;
constexpr int kRelocGlobDatAArch64 = 1025;
constexpr int kRelocJumpSlotX8664 = 7;
constexpr int kRelocGlobDatX8664 = 6;
constexpr int kRelocJumpSlotArm = 22;
constexpr int kRelocGlobDatArm = 21;

/** 安全敏感的导入符号：被改就意味着检测能力可能被"合法地"绕过。 */
bool isSensitiveImport(const char *name) {
    static const char *const kNames[] = {
            "open", "openat", "fopen", "fgets", "fread", "opendir", "readdir",
            "access", "stat", "lstat", "readlink", "kill", "prctl", "ptrace",
            "dladdr", "dlsym", "strstr", "memcmp", "snprintf", "sprintf",
            "getpid", "syscall",
    };
    for (const char *candidate : kNames) {
        if (std::strcmp(name, candidate) == 0) {
            return true;
        }
    }
    return false;
}


struct DynInfo {
    std::uintptr_t symtab = 0;
    std::uintptr_t strtab = 0;
    std::uintptr_t jmprel = 0;
    std::size_t pltrelsz = 0;
    std::uintptr_t rela = 0;
    std::size_t relasz = 0;
    std::uintptr_t rel = 0;
    std::size_t relsz = 0;
    bool valid = false;
};


int collectDynCallback(dl_phdr_info *info, size_t, void *data) {
    auto *want = static_cast<std::pair<std::uintptr_t, DynInfo *> *>(data);
    if (info->dlpi_addr != want->first) {
        return 0;
    }
    for (int i = 0; i < info->dlpi_phnum; ++i) {
        const ElfW(Phdr) &ph = info->dlpi_phdr[i];
        if (ph.p_type != PT_DYNAMIC) {
            continue;
        }
        auto *dyn = reinterpret_cast<ElfW(Dyn) *>(info->dlpi_addr + ph.p_vaddr);
        DynInfo &out = *want->second;
        for (; dyn->d_tag != DT_NULL; ++dyn) {
            switch (dyn->d_tag) {
                case DT_SYMTAB: out.symtab = dyn->d_un.d_ptr; break;
                case DT_STRTAB: out.strtab = dyn->d_un.d_ptr; break;
                case DT_JMPREL: out.jmprel = dyn->d_un.d_ptr; break;
                case DT_PLTRELSZ: out.pltrelsz = dyn->d_un.d_val; break;
                case DT_RELA: out.rela = dyn->d_un.d_ptr; break;
                case DT_RELASZ: out.relasz = dyn->d_un.d_val; break;
                case DT_REL: out.rel = dyn->d_un.d_ptr; break;
                case DT_RELSZ: out.relsz = dyn->d_un.d_val; break;
                default: break;
            }
        }
        out.valid = out.symtab != 0 && out.strtab != 0;
        return 1;
    }
    return 0;
}


int relocationType(std::uintptr_t info) {
#if defined(__aarch64__) || defined(__x86_64__)
    return static_cast<int>(info & 0xffffffff);
#else
    return static_cast<int>(info & 0xff);
#endif
}


std::uintptr_t relocationSymbol(std::uintptr_t info) {
#if defined(__aarch64__) || defined(__x86_64__)
    return info >> 32;
#else
    return info >> 8;
#endif
}


bool isOurSlotType(int type) {
#if defined(__aarch64__)
    return type == kRelocJumpSlotAArch64 || type == kRelocGlobDatAArch64;
#elif defined(__x86_64__)
    return type == kRelocJumpSlotX8664 || type == kRelocGlobDatX8664;
#else
    return type == kRelocJumpSlotArm || type == kRelocGlobDatArm;
#endif
}

}  // namespace


jint jniCountPltHooks(JNIEnv *, jobject) {
    Dl_info self{};
    if (dladdr(reinterpret_cast<const void *>(&crc32Update), &self) == 0 ||
        self.dli_fbase == nullptr) {
        return -1;
    }
    const auto base = reinterpret_cast<std::uintptr_t>(self.dli_fbase);

    DynInfo dynInfo;
    std::pair<std::uintptr_t, DynInfo *> arg{base, &dynInfo};
    dl_iterate_phdr(collectDynCallback, &arg);
    if (!dynInfo.valid) {
        return -1;
    }

    // bionic 在不同版本上给出的 DT_* 可能是「绝对地址」也可能是「相对加载基址的偏移」，
    // 统一归一化，否则会把偏移当指针解引用直接 SIGSEGV（实测踩到：fault addr = 0x136c0）。
    const auto normalize = [base](std::uintptr_t value) {
        return value != 0 && value < base ? base + value : value;
    };
    dynInfo.symtab = normalize(dynInfo.symtab);
    dynInfo.strtab = normalize(dynInfo.strtab);
    dynInfo.jmprel = normalize(dynInfo.jmprel);
    dynInfo.rela = normalize(dynInfo.rela);
    dynInfo.rel = normalize(dynInfo.rel);

    SEC_LOGD("plt check: base=%lx sym=%lx str=%lx jmprel=%lx pltrelsz=%zu",
             static_cast<unsigned long>(base),
             static_cast<unsigned long>(dynInfo.symtab),
             static_cast<unsigned long>(dynInfo.strtab),
             static_cast<unsigned long>(dynInfo.jmprel),
             dynInfo.pltrelsz);

    // 归一化后仍然落在模块映射之外 → 放弃（fail-open，不能因为解析失败就杀用户）
    const auto inModule = [base](std::uintptr_t addr) {
        return addr >= base && addr < base + 0x10000000UL;
    };
    if (!inModule(dynInfo.symtab) || !inModule(dynInfo.strtab)) {
        SEC_LOGW("plt check: unexpected dyn pointers, skipped");
        return -1;
    }

    const auto *symtab = reinterpret_cast<const ElfW(Sym) *>(dynInfo.symtab);
    const auto *strtab = reinterpret_cast<const char *>(dynInfo.strtab);

    int checked = 0;
    int hooks = 0;

    auto scan = [&](const auto *relStart, std::size_t size, bool isRela) {
        if (relStart == nullptr || size == 0) {
            return;
        }
        const std::size_t count = size / (isRela ? sizeof(ElfW(Rela)) : sizeof(ElfW(Rel)));
        for (std::size_t i = 0; i < count; ++i) {
            std::uintptr_t info;
            std::uintptr_t offset;
            if (isRela) {
                info = relStart[i].r_info;
                offset = relStart[i].r_offset;
            } else {
                const auto *rel = reinterpret_cast<const ElfW(Rel) *>(relStart);
                info = rel[i].r_info;
                offset = rel[i].r_offset;
            }
            if (!isOurSlotType(relocationType(info))) {
                continue;
            }
            const std::uintptr_t symIndex = relocationSymbol(info);
            if (symIndex == 0) {
                continue;
            }
            const char *name = strtab + symtab[symIndex].st_name;
            if (name == nullptr || name[0] == '\0' || !isSensitiveImport(name)) {
                continue;
            }
            if (symIndex > 65535) {
                continue;
            }
            const std::uintptr_t slotAddr = base + offset;
            if (slotAddr < base || slotAddr >= base + 0x10000000UL) {
                continue;
            }
            auto **slot = reinterpret_cast<void **>(slotAddr);
            void *expected = dlsym(RTLD_DEFAULT, name);
            if (expected == nullptr) {
                continue;
            }
#ifdef SELF_GOT_PROBE
            // 仅测试构建：把 ptrace 的 GOT 表项改成本库函数地址（模拟一次 GOT 劫持）。
            // ptrace 在启动后不再调用，改写窗口内不会真的执行到它。
            static bool gotProbeDone = false;
            bool probeHere = false;
            long probePage = 0;
            if (!gotProbeDone && std::strcmp(name, "ptrace") == 0) {
                const long ps = sysconf(_SC_PAGESIZE);
                if (ps > 0) {
                    probePage = ps;
                    auto pageStart = reinterpret_cast<std::uintptr_t>(slot) &
                                     ~static_cast<std::uintptr_t>(ps - 1);
                    if (mprotect(reinterpret_cast<void *>(pageStart),
                                 static_cast<std::size_t>(ps),
                                 PROT_READ | PROT_WRITE) == 0) {
                        *slot = reinterpret_cast<void *>(&crc32Update);
                        probeHere = true;
                        gotProbeDone = true;
                        SEC_LOGW("got probe: overwrote ptrace slot");
                    }
                }
            }
#endif
            ++checked;
            if (*slot != expected) {
                ++hooks;
                SEC_LOGW("plt slot mismatch: %s actual=%p expected=%p", name, *slot, expected);
            }
#ifdef SELF_GOT_PROBE
            if (probeHere) {
                // 立刻恢复：既避免影响后续逻辑，也让「检测到即恢复」语义清楚
                *slot = expected;
                auto pageStart = reinterpret_cast<std::uintptr_t>(slot) &
                                 ~static_cast<std::uintptr_t>(probePage - 1);
                mprotect(reinterpret_cast<void *>(pageStart),
                         static_cast<std::size_t>(probePage),
                         PROT_READ);
            }
#endif
        }
    };

    scan(reinterpret_cast<const ElfW(Rela) *>(dynInfo.jmprel), dynInfo.pltrelsz, true);
    if (dynInfo.rela != 0) {
        scan(reinterpret_cast<const ElfW(Rela) *>(dynInfo.rela), dynInfo.relasz, true);
    } else if (dynInfo.rel != 0) {
        scan(reinterpret_cast<const ElfW(Rel) *>(dynInfo.rel), dynInfo.relsz, false);
    }

    SEC_LOGD("plt check: slots=%d hooks=%d", checked, hooks);
    return hooks;
}
