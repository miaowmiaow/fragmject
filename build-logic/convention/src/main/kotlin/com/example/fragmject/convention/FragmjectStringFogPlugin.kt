package com.example.fragmject.convention

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * 字符串加密插桩（H2）。
 *
 * ============================ 做什么 ============================
 * 在 R8 **之前**（`Instrumentation.transformClassesWith`）把 `LDC "明文"` 换成：
 *
 * ```
 * LDC "密文"
 * INVOKESTATIC <当前类>.$fog$d(Ljava/lang/String;)Ljava/lang/String;
 * ```
 *
 * 解密方法 `$fog$d` 由插件**注入到每个被改写的类**，而不是放一个共享运行时类：
 * - 免去新增 `core:stringfog` 模块以及所有模块对它的依赖边（`§6.2` 门槛）；
 * - 每个模块独立插桩，不需要跨模块共享字符串表；
 * - 代价是每个含字符串的类多一个几十字节的静态方法，R8 之后可忽略。
 *
 * ============================ 分工 ============================
 * 本文件只负责 **Gradle / AGP / ASM 编排**：
 * 开关与密钥读取、变体挂载、类筛选入口、方法级改写、字节码注入。
 * 白名单、反射 API 表、编解密口径这类「策略与协议」在 [StringFogPolicy]（纯逻辑，有单测）。
 * 两者的边界不能含糊：`$fog$d` 的字节码必须与 [StringFogPolicy.encrypt] 逐字符对应。
 *
 * ============================ 为什么默认关闭 ============================
 * 插桩会改变字节码，任何「按字符串名查找」的代码都可能被打断。
 * 因此默认由 `-PenableStringFog=true` 显式开启，且 release 以外的验证先跑冒烟。
 */

/** 插桩开关：`-PenableStringFog=true` 才生效。 */
private const val PROP_ENABLE = "enableStringFog"

/** 解密密钥：可用 `-PstringFogKey=<int>` 覆盖；同一次构建内所有类共用。 */
private const val PROP_KEY = "stringFogKey"

/** application 模块挂载点（由 `FragmjectAndroidApplicationPlugin` 调用）。 */
internal fun Project.configureStringFogForApplication() {
    val key = stringFogKeyOrNull() ?: return
    extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
        variant.applyStringFog(key)
    }
}

/** library 模块挂载点（由 `FragmjectAndroidLibraryPlugin` 调用）。 */
internal fun Project.configureStringFogForLibrary() {
    val key = stringFogKeyOrNull() ?: return
    extensions.getByType(LibraryAndroidComponentsExtension::class.java).onVariants { variant ->
        variant.applyStringFog(key)
    }
}

/**
 * 安全库自身的插桩挂载点（由 `fragmject.security.stringfog` 调用）。
 *
 * 与普通 library 不同：默认开启（库的检测串是它的核心资产，不应依赖宿主是否打开开关）。
 * `-PdisableStringFog=true` 可关闭（仅用于排查插桩问题）。
 */
internal fun Project.configureStringFogForSecurityLibrary() {
    if (providers.gradleProperty("disableStringFog").map(String::toBoolean).getOrElse(false)) return
    val key = stringFogKey()
    extensions.getByType(LibraryAndroidComponentsExtension::class.java).onVariants { variant ->
        variant.applyStringFog(key)
    }
}

/**
 * 安全库的字符串插桩插件（`fragmject.security.stringfog`）。
 *
 * 为什么是独立插件：`:security` 直接使用 `com.android.library`，不经过 `fragmject.android.library`，
 * 之前库里的检测特征串一直是 dex 明文（实测 `injection.frida_*` 等命中码可直接 grep）。
 */
class FragmjectSecurityStringFogPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.library") {
            configureStringFogForSecurityLibrary()
        }
    }
}

/** 开关关闭时返回 null（不注册任何 ASM 任务，零开销）。 */
private fun Project.stringFogKeyOrNull(): Int? {
    // 先解析再判开关：参数写错一律失败，避免「-PstringFogKey=abc 但构建照过」的静默
    val key = stringFogKey()
    val enabled = providers.gradleProperty(PROP_ENABLE).map(String::toBoolean).getOrElse(false)
    return if (enabled) key else null
}

private fun Project.stringFogKey(): Int =
    parseStringFogKey(providers.gradleProperty(PROP_KEY).orNull)

/**
 * 解析 `-P[stringFogKey]` 的原始值。
 *
 * 合法：未设置 / 空串 → [StringFogPolicy.DEFAULT_KEY]；十进制整数（如 `4660`）。
 * 非法：报错必须带上**属性名与实际值**。原实现直接用 `String::toInt`，`-PstringFogKey=abc`
 * 只会抛一个没有上下文的 `NumberFormatException`，排查时看不出是哪个参数写错了。
 */
internal fun parseStringFogKey(raw: String?): Int {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return StringFogPolicy.DEFAULT_KEY
    return value.toIntOrNull() ?: throw GradleException(
        "-P$PROP_KEY=$value 不是合法整数：只接受十进制（例如 4660；" +
            "十六进制请先换算，如 0x1234 写成 4660）",
    )
}

/** 给单个变体注册 ASM 访问器。 */
private fun Variant.applyStringFog(key: Int) {
    // PROJECT 而非 ALL：只插桩本模块的类（三方库由 isInstrumentable 也会被过滤，没必要让 AGP 遍历依赖）
    instrumentation.transformClassesWith(FogVisitorFactory::class.java, InstrumentationScope.PROJECT) { params ->
        params.key.set(key)
    }
    // 新增了方法并改写了指令，必须重算帧
    // （INSTRUMENTED_CLASSES：被插桩的类整体重算，覆盖我们注入的解密方法）
    instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES)
}

/** 插桩参数：只有密钥一个输入，保证配置缓存可命中。 */
interface FogParameters : InstrumentationParameters {
    @get:Input
    val key: Property<Int>
}

/** 访问器工厂：决定哪些类进入插桩。 */
abstract class FogVisitorFactory : AsmClassVisitorFactory<FogParameters> {

    override fun isInstrumentable(classData: ClassData): Boolean =
        // AGP 给的是 `.` 分隔的全限定名，策略层统一用 `/` 分隔的内部名
        StringFogPolicy.shouldInstrument(classData.className.replace('.', '/'))

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor =
        FogClassVisitor(nextClassVisitor, parameters.get().key.get())
}

/**
 * 类级访问器：改方法 + 在需要时注入解密方法。
 *
 * `internal` 而非 `private`：单测需要自己搭 `ClassReader -> 本访问器 -> ClassWriter` 的链路，
 * 验证「插桩后的类真的能加载并返回原文」（见 `StringFogPolicyTest`）。
 */
internal class FogClassVisitor(
    next: ClassVisitor,
    private val key: Int,
) : ClassVisitor(Opcodes.ASM9, next) {

    private var className: String? = null
    private var replaced = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?,
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val delegate = super.visitMethod(access, name, descriptor, signature, exceptions)
        val owner = className ?: return delegate

        // 用 MethodNode 缓冲整个方法，才能「看见」指令的下一条（判断是否反射调用）
        return object : MethodNode(Opcodes.ASM9, access, name, descriptor, signature, exceptions) {
            override fun visitEnd() {
                if (rewriteStrings(this, owner, key)) replaced = true
                accept(delegate)
            }
        }
    }

    override fun visitEnd() {
        if (replaced) emitDecodeMethod()
        super.visitEnd()
    }

    /**
     * 注入 `private static String $fog$d(String)`。
     *
     * 等价 Java 源码：
     * ```
     * private static String $fog$d(String s) {
     *     char[] c = s.toCharArray();
     *     for (int i = 0; i < c.length; i++) c[i] = (char) (c[i] ^ (KEY + i));
     *     return new String(c);
     * }
     * ```
     * 口径必须与 [StringFogPolicy.encrypt] 一致（`StringFogPolicyTest` 会真实执行这段字节码）。
     */
    private fun emitDecodeMethod() {
        val mv = super.visitMethod(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            StringFogPolicy.DECODE_METHOD,
            StringFogPolicy.DECODE_DESC,
            null,
            null,
        )
        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "toCharArray",
            "()[C",
            false,
        )
        mv.visitVarInsn(Opcodes.ASTORE, 1)

        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ISTORE, 2)

        val loop = org.objectweb.asm.Label()
        val end = org.objectweb.asm.Label()

        mv.visitLabel(loop)
        mv.visitVarInsn(Opcodes.ILOAD, 2)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, end)

        // c[i] = (char) (c[i] ^ (KEY + i))
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitVarInsn(Opcodes.ILOAD, 2)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitVarInsn(Opcodes.ILOAD, 2)
        mv.visitInsn(Opcodes.CALOAD)
        mv.visitLdcInsn(key)
        mv.visitVarInsn(Opcodes.ILOAD, 2)
        mv.visitInsn(Opcodes.IADD)
        mv.visitInsn(Opcodes.IXOR)
        mv.visitInsn(Opcodes.I2C)
        mv.visitInsn(Opcodes.CASTORE)

        mv.visitIincInsn(2, 1)
        mv.visitJumpInsn(Opcodes.GOTO, loop)

        mv.visitLabel(end)
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String")
        mv.visitInsn(Opcodes.DUP)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false)
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(8, 4)
        mv.visitEnd()
    }
}

/**
 * 把方法内所有明文常量替换为「密文 + 解密调用」。
 *
 * 注意 `InsnList.insert(location, insn)` 是插在 `location` **之后**：
 * 用成 `insertBefore` 会让解密调用拿到栈上别的值，产物直接 verify error。
 * 该契约由 `StringFogPolicyTest` 的「插桩后真实执行」用例兜底。
 *
 * @return 是否至少替换了一处（决定是否需要注入解密方法）。
 */
private fun rewriteStrings(node: MethodNode, owner: String, key: Int): Boolean {
    val insns = node.instructions
    var index = 0
    var changed = false

    while (index < insns.size()) {
        val insn = insns[index]
        if (insn is LdcInsnNode) {
            val plain = insn.cst as? String
            if (StringFogPolicy.shouldEncrypt(plain) && !isReflectiveUse(insns, index)) {
                insn.cst = StringFogPolicy.encrypt(plain!!, key)
                insns.insert(
                    insn,
                    MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        owner,
                        StringFogPolicy.DECODE_METHOD,
                        StringFogPolicy.DECODE_DESC,
                        false,
                    ),
                )
                changed = true
                index++ // 跳过刚插入的解密调用
            }
        }
        index++
    }
    return changed
}

/** 下一条指令是否是「按名查找」的调用。 */
private fun isReflectiveUse(insns: InsnList, index: Int): Boolean {
    val next: AbstractInsnNode = insns[index].next ?: return false
    if (next !is MethodInsnNode) return false
    return StringFogPolicy.isReflectiveCall(next.owner, next.name)
}
