#include "apk_signature.h"

#include <dlfcn.h>
#include <fcntl.h>
#include <unistd.h>

#include <algorithm>
#include <cstring>

namespace apksig {
namespace {

// ============================================================================ SHA-256

constexpr uint32_t kRoundConstants[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
};

constexpr uint32_t kInitialState[8] = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
};

inline uint32_t rotr(uint32_t x, unsigned n) {
    return (x >> n) | (x << (32 - n));
}

inline uint32_t be32(const uint8_t *p) {
    return (static_cast<uint32_t>(p[0]) << 24) | (static_cast<uint32_t>(p[1]) << 16) |
           (static_cast<uint32_t>(p[2]) << 8) | static_cast<uint32_t>(p[3]);
}

void compressBlock(uint32_t state[8], const uint8_t block[64]) {
    uint32_t w[64];
    for (int i = 0; i < 16; ++i) {
        w[i] = be32(block + 4 * i);
    }
    for (int i = 16; i < 64; ++i) {
        const uint32_t s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
        const uint32_t s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
        w[i] = w[i - 16] + s0 + w[i - 7] + s1;
    }

    uint32_t a = state[0], b = state[1], c = state[2], d = state[3];
    uint32_t e = state[4], f = state[5], g = state[6], h = state[7];
    for (int i = 0; i < 64; ++i) {
        const uint32_t s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
        const uint32_t ch = (e & f) ^ (~e & g);
        const uint32_t t1 = h + s1 + ch + kRoundConstants[i] + w[i];
        const uint32_t s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
        const uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
        const uint32_t t2 = s0 + maj;
        h = g;
        g = f;
        f = e;
        e = d + t1;
        d = c;
        c = b;
        b = a;
        a = t1 + t2;
    }
    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
    state[4] += e;
    state[5] += f;
    state[6] += g;
    state[7] += h;
}

int hexValue(char ch) {
    if (ch >= '0' && ch <= '9') return ch - '0';
    if (ch >= 'a' && ch <= 'f') return ch - 'a' + 10;
    if (ch >= 'A' && ch <= 'F') return ch - 'A' + 10;
    return -1;
}

// ============================================================================ APK 解析

constexpr uint32_t kEocdSignature = 0x06054b50;
constexpr std::size_t kEocdMinSize = 22;
constexpr std::size_t kMaxCommentSize = 65535;
constexpr std::size_t kFooterSize = 24;  // 8 字节 size + 16 字节 magic
constexpr char kSigBlockMagic[] = "APK Sig Block 42";
constexpr uint32_t kV2BlockId = 0x7109871a;
constexpr uint32_t kV3BlockId = 0xf05368c0;
constexpr uint64_t kMaxSigningBlock = 1u << 20;

uint32_t le32(const uint8_t *p) {
    return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) | (static_cast<uint32_t>(p[3]) << 24);
}

uint64_t le64(const uint8_t *p) {
    return static_cast<uint64_t>(le32(p)) | (static_cast<uint64_t>(le32(p + 4)) << 32);
}

bool readAt(int fd, uint64_t offset, uint8_t *buf, std::size_t len) {
    std::size_t done = 0;
    while (done < len) {
        const ssize_t n = pread(fd, buf + done, len - done, static_cast<off_t>(offset + done));
        if (n <= 0) return false;
        done += static_cast<std::size_t>(n);
    }
    return true;
}

/** 长度前缀结构的游标：所有读取都做边界检查，越界即失败。 */
struct Cursor {
    const uint8_t *p;
    const uint8_t *end;

    std::size_t remaining() const { return static_cast<std::size_t>(end - p); }

    bool readU32(uint32_t &out) {
        if (remaining() < 4) return false;
        out = le32(p);
        p += 4;
        return true;
    }

    bool take(std::size_t n, Cursor &sub) {
        if (remaining() < n) return false;
        sub = Cursor{p, p + n};
        p += n;
        return true;
    }
};

/** signed data：[u32 digests 长度][digests][u32 certificates 长度][(u32 长度, DER)...]。 */
bool collectCertDigests(Cursor signedData, std::vector<Digest> &out) {
    uint32_t digestsLen = 0;
    Cursor digests{nullptr, nullptr};
    if (!signedData.readU32(digestsLen) || !signedData.take(digestsLen, digests)) return false;

    uint32_t certsLen = 0;
    Cursor certs{nullptr, nullptr};
    if (!signedData.readU32(certsLen) || !signedData.take(certsLen, certs)) return false;

    while (certs.remaining() > 0) {
        uint32_t certLen = 0;
        Cursor cert{nullptr, nullptr};
        if (!certs.readU32(certLen) || !certs.take(certLen, cert)) return false;
        out.push_back(sha256(cert.p, certLen));
    }
    return !out.empty();
}

/** 签名块 value：[u32 signers 长度][(u32 signer 长度, signer)...]；signer 首项为 signed data。 */
bool collectSignerCerts(Cursor value, std::vector<Digest> &out) {
    uint32_t signersLen = 0;
    Cursor signers{nullptr, nullptr};
    if (!value.readU32(signersLen) || !value.take(signersLen, signers)) return false;

    while (signers.remaining() > 0) {
        uint32_t signerLen = 0;
        Cursor signer{nullptr, nullptr};
        if (!signers.readU32(signerLen) || !signers.take(signerLen, signer)) return false;

        uint32_t signedLen = 0;
        Cursor signedData{nullptr, nullptr};
        if (!signer.readU32(signedLen) || !signer.take(signedLen, signedData)) return false;
        if (!collectCertDigests(signedData, out)) return false;
    }
    return !out.empty();
}

/** 从 APK 的签名块中收集所有证书摘要。优先 v3（轮换后的当前证书），没有则用 v2。 */
bool readSigningCertDigests(int fd, std::vector<Digest> &certs) {
    const off_t fileSize = lseek(fd, 0, SEEK_END);
    if (fileSize < static_cast<off_t>(kEocdMinSize)) return false;
    const auto size = static_cast<uint64_t>(fileSize);

    // 中央目录结束记录（EOCD）位于文件末尾，注释最长 65535 字节
    const auto tailLen = static_cast<std::size_t>(
            std::min<uint64_t>(size, kEocdMinSize + kMaxCommentSize));
    std::vector<uint8_t> tail(tailLen);
    if (!readAt(fd, size - tailLen, tail.data(), tailLen)) return false;

    std::size_t eocd = tailLen;
    for (std::size_t pos = tailLen - kEocdMinSize + 1; pos-- > 0;) {
        if (le32(&tail[pos]) == kEocdSignature) {
            eocd = pos;
            break;
        }
    }
    if (eocd == tailLen) return false;

    // 中央目录偏移紧接在签名块之后，签名块以 footer 结束
    const uint64_t cdOffset = le32(&tail[eocd + 16]);
    if (cdOffset < kFooterSize) return false;
    uint8_t footer[kFooterSize];
    if (!readAt(fd, cdOffset - kFooterSize, footer, kFooterSize)) return false;
    if (std::memcmp(footer + 8, kSigBlockMagic, 16) != 0) return false;

    const uint64_t blockSize = le64(footer);
    if (blockSize < kFooterSize || blockSize > kMaxSigningBlock || cdOffset < blockSize + 8) {
        return false;
    }
    const uint64_t blockStart = cdOffset - blockSize - 8;
    std::vector<uint8_t> block(static_cast<std::size_t>(blockSize + 8));
    if (!readAt(fd, blockStart, block.data(), block.size())) return false;
    if (le64(block.data()) != blockSize) return false;

    // 块内是 (u64 长度, u32 id, value) 序列，末尾 24 字节为 footer
    Cursor pairs{block.data() + 8, block.data() + block.size() - kFooterSize};
    Cursor v2{nullptr, nullptr};
    Cursor v3{nullptr, nullptr};
    bool hasV2 = false;
    bool hasV3 = false;
    while (pairs.remaining() >= 12) {
        const uint64_t len = le64(pairs.p);
        const uint32_t id = le32(pairs.p + 8);
        if (len < 4 || len > pairs.remaining() - 8) return false;
        const auto pairLen = static_cast<std::size_t>(len);
        Cursor value{pairs.p + 12, pairs.p + 8 + pairLen};
        if (id == kV3BlockId) {
            v3 = value;
            hasV3 = true;
        } else if (id == kV2BlockId) {
            v2 = value;
            hasV2 = true;
        }
        pairs.p += 8 + pairLen;
    }

    if (hasV3) return collectSignerCerts(v3, certs);
    return hasV2 && collectSignerCerts(v2, certs);
}

}  // namespace

// ============================================================================ 公开接口

Digest sha256(const uint8_t *data, std::size_t len) {
    uint32_t state[8];
    std::memcpy(state, kInitialState, sizeof(state));

    const std::size_t fullBlocks = len / 64;
    for (std::size_t i = 0; i < fullBlocks; ++i) {
        compressBlock(state, data + 64 * i);
    }

    // 填充：0x80 + 零 + 64 位大端比特长度，总长为 64 的整数倍
    uint8_t tail[128] = {0};
    const std::size_t rem = len - fullBlocks * 64;
    if (rem > 0) {
        std::memcpy(tail, data + fullBlocks * 64, rem);
    }
    tail[rem] = 0x80;
    const std::size_t padLen = (rem < 56) ? 64 : 128;
    const uint64_t bitLen = static_cast<uint64_t>(len) * 8;
    for (std::size_t i = 0; i < 8; ++i) {
        tail[padLen - 1 - i] = static_cast<uint8_t>(bitLen >> (8 * i));
    }
    compressBlock(state, tail);
    if (padLen == 128) {
        compressBlock(state, tail + 64);
    }

    Digest out{};
    for (std::size_t i = 0; i < 8; ++i) {
        out[4 * i] = static_cast<uint8_t>(state[i] >> 24);
        out[4 * i + 1] = static_cast<uint8_t>(state[i] >> 16);
        out[4 * i + 2] = static_cast<uint8_t>(state[i] >> 8);
        out[4 * i + 3] = static_cast<uint8_t>(state[i]);
    }
    return out;
}

bool parseDigestList(const std::string &text, std::vector<Digest> &out) {
    out.clear();
    std::string hex;
    auto finishItem = [&]() -> bool {
        if (hex.size() != 64) return false;
        Digest digest{};
        for (std::size_t i = 0; i < digest.size(); ++i) {
            const int hi = hexValue(hex[2 * i]);
            const int lo = hexValue(hex[2 * i + 1]);
            if (hi < 0 || lo < 0) return false;
            digest[i] = static_cast<uint8_t>((hi << 4) | lo);
        }
        out.push_back(digest);
        hex.clear();
        return true;
    };

    for (const char ch : text) {
        if (ch == ',') {
            if (!finishItem()) return false;
        } else if (ch != ':' && ch != ' ' && ch != '\t') {
            hex.push_back(ch);
        }
    }
    if (!finishItem()) return false;
    return !out.empty();
}

std::string apkPathFromLibraryPath(const std::string &libPath) {
    // 直接从 APK 内加载：<apk>!/lib/<abi>/libx.so
    const std::string embedded = ".apk!/";
    const auto embeddedPos = libPath.find(embedded);
    if (embeddedPos != std::string::npos) {
        return libPath.substr(0, embeddedPos + 4);
    }
    // 解压加载：<pkgdir>/lib/<abi>/libx.so，APK 为 <pkgdir>/base.apk
    if (libPath.find("/lib/") == std::string::npos) return {};
    std::string dir = libPath;
    for (int i = 0; i < 3; ++i) {
        const auto slash = dir.rfind('/');
        if (slash == std::string::npos) return {};
        dir.erase(slash);
    }
    return dir + "/base.apk";
}

std::string resolveApkPathFromLibrary(const void *anchor) {
    Dl_info info{};
    if (dladdr(anchor, &info) == 0 || info.dli_fname == nullptr) return {};
    return apkPathFromLibraryPath(info.dli_fname);
}

int verifyApkCertificates(const std::string &apkPath, const std::vector<Digest> &expected) {
    if (expected.empty() || apkPath.empty()) return kUnknown;
    const int fd = open(apkPath.c_str(), O_RDONLY | O_CLOEXEC);
    if (fd < 0) return kUnknown;

    std::vector<Digest> certs;
    const bool parsed = readSigningCertDigests(fd, certs);
    close(fd);
    if (!parsed) return kUnknown;

    for (const Digest &cert : certs) {
        for (const Digest &want : expected) {
            if (cert == want) return kMatch;
        }
    }
    return kMismatch;
}

}  // namespace apksig
