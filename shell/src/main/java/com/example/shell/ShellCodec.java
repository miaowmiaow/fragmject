package com.example.shell;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * 载荷编解码协议：**壳端的唯一实现**。
 *
 * ============================ 为什么单独一个类 ============================
 * 打包端（`build-logic` 的 `ShellPayload`）必须与壳端逐字节一致，但两者分属不同构建、
 * 不同语言（Kotlin / Java）。此前各写一份，任一侧改动都只能靠真机「装上即崩」才发现。
 * 现在算法收敛到这里，并加了一条跨侧一致性单测：测试在运行时用 `javax.tools.JavaCompiler`
 * 现编译本文件，与 Kotlin 侧逐字节比对（见 `ShellCodecParityTest`）。
 * 因此：**改这个类等于改打包协议**，必须同时跑 `:build-logic:convention:test`。
 *
 * ============================ 顺序 ============================
 * 打包端：`encode(raw) = XOR(deflate(raw))`；壳端：`decode(chunk) = inflate(XOR(chunk))`。
 * 顺序不可反：先 XOR 会让载荷变成高熵数据、zip 压不动（实测壳包 11.70 MB → 15.20 MB）。
 *
 * 本类是纯 Java、零依赖（只用 `java.util.zip`），符合 `:shell` 模块「纯 Java、零依赖」的约束。
 */
public final class ShellCodec {

    /** 与打包端 `ShellPayload.XOR_BASE` 一致。 */
    public static final int XOR_BASE = 0x5A;

    private ShellCodec() {
    }

    /** 打包端使用：先 deflate 再 XOR。 */
    public static byte[] encode(byte[] raw) {
        return xor(deflate(raw));
    }

    /** 壳端使用：先 XOR 还原，再 inflate 得到原始 dex。 */
    public static byte[] decode(byte[] chunk) throws DataFormatException {
        return inflate(xor(chunk));
    }

    /** 逐字节 XOR（索引参与密钥，自反）：`out[i] = data[i] ^ (XOR_BASE + i)`。 */
    public static byte[] xor(byte[] data) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ (XOR_BASE + i));
        }
        return out;
    }

    /** 最大压缩率 deflate。 */
    public static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 2));
        byte[] buffer = new byte[64 * 1024];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return out.toByteArray();
    }

    /** 解压 deflate 数据（载荷损坏时抛 [DataFormatException]，由壳 fail-closed）。 */
    public static byte[] inflate(byte[] compressed) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, compressed.length * 3));
        byte[] buffer = new byte[64 * 1024];
        while (!inflater.finished()) {
            int read = inflater.inflate(buffer);
            if (read == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) {
                    break;
                }
            } else {
                out.write(buffer, 0, read);
            }
        }
        inflater.end();
        return out.toByteArray();
    }
}
