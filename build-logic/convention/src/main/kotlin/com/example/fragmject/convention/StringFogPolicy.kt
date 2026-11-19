package com.example.fragmject.convention

/**
 * 字符串插桩的**策略与协议**（纯逻辑，与 Gradle / AGP / ASM 无关，可在 JVM 单测中直接覆盖）。
 *
 * 这里放三类东西，它们都必须与「注入的字节码」严格一致，否则产物会在运行时崩溃或静默失效：
 * 1. 白名单 / 排除规则（[shouldInstrument]）：决定哪些类被插桩；
 * 2. 反射 API 表（[isReflectiveCall]）：这些调用后面的常量必须保留明文（语义就是按名查找）；
 * 3. 编解码（[encrypt] / [decrypt]）：与注入的 `$fog$d` 逐字符对应。
 *
 * 原先这些规则散落在 [FragmjectStringFogPlugin] 的 `private` 常量与函数里，
 * 既无法单测、也没有任何机制阻止「Kotlin 侧加密」与「字节码侧解密」各自漂移。
 */
internal object StringFogPolicy {

    /** 默认密钥：只是「不让密文一眼看穿」，不是密码学密钥（可用 `-PstringFogKey` 覆盖）。 */
    const val DEFAULT_KEY = 0x5A3C

    /** 只加密长度 ≥ 4 的常量（`":"`、`"/"` 这类多半是协议分隔符，收益低风险高）。 */
    const val MIN_CHARS = 4

    /** 注入的解密方法名（`$` 前缀避免与业务方法冲突）。 */
    const val DECODE_METHOD = "\$fog\$d"

    /** 解密方法描述符。 */
    const val DECODE_DESC = "(Ljava/lang/String;)Ljava/lang/String;"

    /**
     * 插桩的包前缀（`/` 分隔的类内部名）。
     * - `com/example/fragmject/`：应用自身（application / 普通 library 模块）；
     * - `com/example/security/`：安全库自身——库的 AAR 不会被宿主的插桩任务触碰，
     *   因此库里的检测特征串必须在库自己的构建中加密。
     */
    val OWN_PACKAGES: List<String> = listOf("com/example/fragmject/", "com/example/security/")

    /**
     * 不插桩的包前缀。当前为空：原 `com/example/fragmject/app/security/` 已搬迁到 `:security`
     * （由 [OWN_PACKAGES] 覆盖且必须插桩）。将来若出现「字符串必须保持明文」的包，在此登记。
     */
    val EXCLUDED_PREFIXES: List<String> = emptyList()

    /**
     * 不插桩的类名片段：**Hilt 生成的聚合类**。
     *
     * `HiltModules_*` / `*_LazyMapKey` 里的字符串是 R8 `-identifiernamestring` 的目标
     * （`lazyClassKeyName = "com.example...XxxViewModel"`）。一旦被替换成解密调用，R8 就
     * 无法追踪「哪些标识符字符串流入该字段」（`Cannot determine what identifier string flows to …`），
     * 后果是多模块 Hilt 聚合在运行时找不到模块。
     */
    val EXCLUDED_NAME_PARTS: List<String> = listOf("_Hilt", "LazyMapKey")

    /**
     * 「名字必须是明文」的 API：命中时保留原字符串。
     *
     * 只收真正按名查找的入口，宁可少加密也不要线上崩：
     * `Class.forName` / `getDeclaredMethod` 一类反射、`Enum.valueOf`、按名取资源。
     */
    val REFLECTIVE_APIS: Set<Pair<String, String>> = buildSet {
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

    /**
     * 该类是否需要插桩。[internalClassName] 必须是 `/` 分隔的内部名
     * （AGP 的 `ClassData.className` 是 `.` 分隔，调用方负责转换）。
     */
    fun shouldInstrument(internalClassName: String): Boolean {
        if (OWN_PACKAGES.none { internalClassName.startsWith(it) }) return false
        if (EXCLUDED_PREFIXES.any { internalClassName.startsWith(it) }) return false
        if (EXCLUDED_NAME_PARTS.any { internalClassName.contains(it) }) return false
        return true
    }

    /** 紧跟常量之后的那条调用是否是「按名查找」（是则常量必须保持明文）。 */
    fun isReflectiveCall(owner: String?, name: String?): Boolean {
        if (owner == null || name == null) return false
        return (owner to name) in REFLECTIVE_APIS
    }

    /** 该常量是否需要加密：`null`（非字符串常量）与过短字符串都不动。 */
    fun shouldEncrypt(text: String?): Boolean = text != null && text.length >= MIN_CHARS

    /** 与注入的解密方法逐字符对应：`c[i] ^ (key + i)`。 */
    fun encrypt(plain: String, key: Int): String = String(
        CharArray(plain.length) { i -> ((plain[i].code xor (key + i)) and 0xFFFF).toChar() },
    )

    /**
     * [encrypt] 的逆运算，同时是**注入字节码的对照实现**。
     *
     * 目前唯一使用者是单测：round-trip 与「插桩后真实执行」两条断言都依赖它。
     * 生产侧的解密在 [FragmjectStringFogPlugin] 注入的 `$fog$d` 字节码里，
     * 两处口径必须一致，任一处改动都会让产物启动即崩。
     */
    fun decrypt(cipher: String, key: Int): String = String(
        CharArray(cipher.length) { i -> ((cipher[i].code xor (key + i)) and 0xFFFF).toChar() },
    )
}
