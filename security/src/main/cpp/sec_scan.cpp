// 线程名 / 文件 / Root 文件特征扫描（自 security.cpp 拆出，逻辑不变）。

#include "sec_common.h"
#include "sec_jni.h"

namespace {


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

}  // namespace


/** Frida 特征线程数；-1=无法枚举。 */
jint jniCountThreadSignals(JNIEnv *, jobject) {
    return countThreadSignals();
}


jint jniCountFileSignals(JNIEnv *, jobject) {
    return countFileSignals();
}


/** Root 相关文件数（su / Magisk 目录）。 */
jint jniFindRootFiles(JNIEnv *, jobject) {
    return countRootFiles();
}
