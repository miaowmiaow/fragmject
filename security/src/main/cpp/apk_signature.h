// APK 签名证书校验（native 侧）。
//
// 为什么放在 native：Java 层 PackageManager 的返回值可被 hook 伪造，期望摘要也是 dex 常量。
// 这里的期望摘要由构建期编译进 so；APK 路径由 dladdr 从本库的真实加载位置推导，
// 不取 Java 传入的 sourceDir，因此 Java 层改路径无效。
//
// 边界：只读取 APK 签名块中的证书并比对摘要，不重新验证签名密码学（平台在安装时已验证；
// 应用进程无法写入 /data/app 下的 APK）。

#pragma once

#include <array>
#include <cstdint>
#include <string>
#include <vector>

namespace apksig {

using Digest = std::array<uint8_t, 32>;

/** [verifyApkCertificates] 的返回值：匹配。 */
constexpr int kMatch = 0;
/** [verifyApkCertificates] 的返回值：不匹配。 */
constexpr int kMismatch = 1;
/** [verifyApkCertificates] 的返回值：无法判定（期望为空、文件不可读、结构异常）。 */
constexpr int kUnknown = -1;

/** SHA-256（FIPS 180-4）。 */
Digest sha256(const uint8_t *data, std::size_t len);

/**
 * 解析逗号分隔的十六进制摘要（每项 64 位 hex，忽略冒号与空白，大小写均可）。
 * 任一项非法返回 false；空串返回 false。
 */
bool parseDigestList(const std::string &text, std::vector<Digest> &out);

/** 由 so 的加载路径推导 APK 路径；无法推导返回空串。纯字符串函数，便于测试。 */
std::string apkPathFromLibraryPath(const std::string &libPath);

/** 通过 dladdr 取得 [anchor] 所在 so 的加载路径，再推导 APK 路径。 */
std::string resolveApkPathFromLibrary(const void *anchor);

/**
 * 读取 APK 签名块（优先 v3，其次 v2）中的证书，任一证书摘要与 [expected] 匹配即返回 [kMatch]。
 * 返回 [kUnknown] 表示无法判定。
 */
int verifyApkCertificates(const std::string &apkPath, const std::vector<Digest> &expected);

}  // namespace apksig
