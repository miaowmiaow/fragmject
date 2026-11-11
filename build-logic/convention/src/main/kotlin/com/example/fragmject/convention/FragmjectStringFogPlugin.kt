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
 * ============================ 做法 ============================
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
 * ============================ 为什么默认关闭 ============================
 * 插桩会改变字节码，任何「按字符串名查找」的代码都可能被打断。
 * 因此默认由 `-PenableStringFog=true` 显式开启，且 release 以外的验证先跑冒烟。
 *
 * ============================ 白名单 ============================
 * - 只处理 `com/example/fragmject/` 下的类，三方库不碰；
 * - 跳过 `com/example/fragmject/app/security/`：安全检测的特征串与判定逻辑不接受改写；
 * - 跳过长度 < [MIN_CHARS] 的常量（`":"`、`"/"` 这类多半是协议分隔符，收益低风险高）；
 * - 跳过紧随其后的**反射/按名查找**调用（`Class.forName`、`Enum.valueOf`、`getIdentifier` 等），
 *   这些 API 的语义就是「名字必须是明文」。
 */

/** 插桩开关：`-PenableStringFog=true` 才生效。 */
private const val PROP_ENABLE = "enableStringFog"

/** 解密密钥：可用 `-PstringFogKey=<int>` 覆盖；同一次构建内所有类共用。 */
private const val PROP_KEY = "stringFogKey"

/** 默认密钥：只是「不让密文一眼看穿」，不是密码学密钥。 */
private const val DEFAULT_KEY = 0x5A3C

/** 注入的解密方法名（加 `$` 前缀，避免与业务方法冲突）。 */
private const val DECODE_METHOD = "\$fog\$d"

/** 解密方法描述符。 */
private const val DECODE_DESC = "(Ljava/lang/String;)Ljava/lang/String;"

/** 只加密长度 ≥ 4 的常量。 */
private const val MIN_CHARS = 4

/** 只插桩本项目代码。 */
private const val OWN_PACKAGE = "com/example/fragmject/"

/** 不插桩的包 / 类。 */
private val EXCLUDED_PREFIXES = listOf(
    "com/example/fragmject/app/security/",
)

/**
 * 不插桩的类名片段：**Hilt 生成的聚合类**。
 *
 * `HiltModules_*` / `*_LazyMapKey` 里的字符串是 R8 `-identifiernamestring` 的目标
 * （`lazyClassKeyName = "com.example...XxxViewModel"`）。一旦被替换成解密调用，
 * R8 就无法追踪「哪些标识符字符串流入该字段」，警告原文：
 * `Cannot determine what identifier string flows to ... lazyClassKeyName`，
 * 后果是多模块 Hilt 聚合在运行时找不到模块。
 */
private val EXCLUDED_NAME_PARTS = listOf(
    "_Hilt",
    "LazyMapKey",
)

/**
 * 「名字必须是明文」的 API：命中时保留原字符串。
 *
 * 只收真正按名查找的入口，宁可少加密也不要线上崩：
 * `Class.forName` / `getDeclaredMethod` 一类反射、`Enum.valueOf`、按名取资源。
 */
private val REFLECTIVE_APIS: Set<Pair<String, String>> = buildSet {
    val classLookups = listOf(
        "forName",
        "getMethod",
        "getDeclaredMethod",
        "getField",
        "getDeclaredField",
        "getConstructor",
        "getDeclaredConstructor",
        "getResource",
        "getResourceAsStream",
    )
    classLookups.forEach { add("java/lang/Class" to it) }
    listOf("loadClass", "findClass", "findLibrary", "getResource", "getResourceAsStream")
        .forEach { add("java/lang/ClassLoader" to it) }
    add("java/lang/Enum" to "valueOf")
    add("android/content/res/Resources" to "getIdentifier")
}

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

/** 开关关闭时返回 null（不注册任何 ASM 任务，零开销）。 */
private fun Project.stringFogKeyOrNull(): Int? {
    val enabled = providers.gradleProperty(PROP_ENABLE).map(String::toBoolean).getOrElse(false)
    if (!enabled) return null
    return providers.gradleProperty(PROP_KEY).map(String::toInt).getOrElse(DEFAULT_KEY)
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

    override fun isInstrumentable(classData: ClassData): Boolean {
        val name = classData.className.replace('.', '/')
        if (!name.startsWith(OWN_PACKAGE)) return false
        if (EXCLUDED_PREFIXES.any { name.startsWith(it) }) return false
        if (EXCLUDED_NAME_PARTS.any { name.contains(it) }) return false
        return true
    }

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor =
        FogClassVisitor(nextClassVisitor, parameters.get().key.get())
}

/** 类级访问器：改方法 + 在需要时注入解密方法。 */
private class FogClassVisitor(
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
     */
    private fun emitDecodeMethod() {
        val mv = super.visitMethod(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            DECODE_METHOD,
            DECODE_DESC,
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
            if (plain != null && plain.length >= MIN_CHARS && !isReflectiveUse(insns, index)) {
                insn.cst = encrypt(plain, key)
                insns.insert(
                    insn,
                    MethodInsnNode(Opcodes.INVOKESTATIC, owner, DECODE_METHOD, DECODE_DESC, false),
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
    return (next.owner to next.name) in REFLECTIVE_APIS
}

/** 与注入的解密方法逐字符对应：`c[i] ^ (key + i)`。 */
private fun encrypt(plain: String, key: Int): String {
    val chars = CharArray(plain.length) { i ->
        ((plain[i].code xor (key + i)) and 0xFFFF).toChar()
    }
    return String(chars)
}
