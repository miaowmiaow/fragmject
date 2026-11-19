package com.example.fragmject.convention

import org.gradle.api.GradleException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * 字符串插桩的策略与协议测试。
 *
 * 覆盖三类此前完全无测试的点：
 * 1. 编解码口径（Kotlin 侧 [StringFogPolicy.encrypt] ↔ 注入字节码的 `$fog$d`）；
 * 2. 白名单 / 排除 / 反射 API 判定（线上崩溃与 R8 聚合失效都发生在这里）；
 * 3. **字节码级真实执行**：生成样例类 → 过插桩访问器 → `defineClass` 加载 → 调用，
 *    断言返回值仍为原文。这条用例同时锁死「常量替换位置（insert 在 LDC 之后）」
 *    「解密方法注入」「帧重算」三个隐性契约——任何一个改错，产物都会 verify error 或返回乱码。
 */
class StringFogPolicyTest {

    // ---------------- 编解码 ----------------

    @Test
    fun `加解密往返一致（含中文与代理对）`() {
        val samples = listOf("hello world", "中文文案", "😀 emoji", "a", "abcd")
        listOf(StringFogPolicy.DEFAULT_KEY, 4660, 0, 0xFFFF).forEach { key ->
            samples.forEach { plain ->
                assertEquals(
                    "key=$key plain=$plain",
                    plain,
                    StringFogPolicy.decrypt(StringFogPolicy.encrypt(plain, key), key),
                )
            }
        }
    }

    @Test
    fun `密文与明文不同且长度不变`() {
        val plain = "wanandroid.com"
        val cipher = StringFogPolicy.encrypt(plain, StringFogPolicy.DEFAULT_KEY)
        assertEquals(plain.length, cipher.length)
        assertNotEquals(plain, cipher)
    }

    @Test
    fun `逐索引异或口径与注入字节码一致`() {
        val key = StringFogPolicy.DEFAULT_KEY
        val plain = "abcd"
        val cipher = StringFogPolicy.encrypt(plain, key)
        plain.indices.forEach { i ->
            // 注入的 $fog$d 用 (KEY + i)，不是常量 KEY
            assertEquals(((plain[i].code xor (key + i)) and 0xFFFF).toChar(), cipher[i])
        }
    }

    // ---------------- 策略 ----------------

    @Test
    fun `只插桩本项目与安全库的类`() {
        assertTrue(StringFogPolicy.shouldInstrument("com/example/fragmject/app/MainActivity"))
        assertTrue(StringFogPolicy.shouldInstrument("com/example/security/SecurityGuard"))
        assertFalse(StringFogPolicy.shouldInstrument("androidx/core/net/UriKt"))
        assertFalse(StringFogPolicy.shouldInstrument("com/example/other/Foo"))
    }

    @Test
    fun `Hilt 聚合类不插桩`() {
        assertFalse(
            StringFogPolicy.shouldInstrument(
                "com/example/fragmject/app/DaggerFragmjectApplication_HiltComponents_SingletonC",
            ),
        )
        assertFalse(StringFogPolicy.shouldInstrument("com/example/fragmject/app/SearchViewModel_LazyMapKey"))
        assertTrue(StringFogPolicy.shouldInstrument("com/example/fragmject/app/FragmjectApplication"))
    }

    @Test
    fun `过短与 null 常量不加密`() {
        assertFalse(StringFogPolicy.shouldEncrypt(null))
        assertFalse(StringFogPolicy.shouldEncrypt(""))
        assertFalse(StringFogPolicy.shouldEncrypt("abc"))
        assertTrue(StringFogPolicy.shouldEncrypt("abcd"))
        assertTrue(StringFogPolicy.shouldEncrypt("中文中文"))
    }

    @Test
    fun `按名查找的 API 判定`() {
        assertTrue(StringFogPolicy.isReflectiveCall("java/lang/Class", "forName"))
        assertTrue(StringFogPolicy.isReflectiveCall("java/lang/Enum", "valueOf"))
        assertTrue(StringFogPolicy.isReflectiveCall("android/content/res/Resources", "getIdentifier"))
        assertFalse(StringFogPolicy.isReflectiveCall("java/lang/String", "valueOf"))
        assertFalse(StringFogPolicy.isReflectiveCall(null, "forName"))
        assertFalse(StringFogPolicy.isReflectiveCall("java/lang/Class", null))
    }

    // ---------------- 开关参数 ----------------

    @Test
    fun `密钥参数未设置或为空时用默认值`() {
        assertEquals(StringFogPolicy.DEFAULT_KEY, parseStringFogKey(null))
        assertEquals(StringFogPolicy.DEFAULT_KEY, parseStringFogKey(""))
        assertEquals(StringFogPolicy.DEFAULT_KEY, parseStringFogKey("   "))
    }

    @Test
    fun `密钥参数只接受十进制（含轮换与零值）`() {
        assertEquals(4660, parseStringFogKey("4660"))
        assertEquals(0, parseStringFogKey("0"))
        assertEquals(4660, parseStringFogKey(" 4660 "))
    }

    @Test
    fun `非法密钥参数报错要带属性名与原值`() {
        // 0x1234 也不接受：原实现是 String::toInt，只认十进制；这里把口径固定下来并给出换算提示
        listOf("abc", "0x1234", "12.5", "99999999999999").forEach { raw ->
            val error = assertThrows(GradleException::class.java) { parseStringFogKey(raw) }
            val message = error.message.orEmpty()
            assertTrue("报错缺少属性名与原值：$message", message.contains("stringFogKey=$raw"))
            assertTrue("报错缺少取值口径提示：$message", message.contains("十进制"))
        }
    }

    // ---------------- 字节码级（真实执行） ----------------

    @Test
    fun `插桩后的类能加载并返回原文`() {
        val cls = loadAndInstrument("com/example/fragmject/FogSample", ::buildSampleClass)

        assertEquals("hello world", cls.getDeclaredMethod("plain").invoke(null))
        assertEquals("中文文案", cls.getDeclaredMethod("chinese").invoke(null))
        // 解密方法确实被注入到被改写的类里
        assertNotNull(cls.getDeclaredMethod(StringFogPolicy.DECODE_METHOD, String::class.java))
    }

    @Test
    fun `密钥轮换后插桩仍返回原文且常量池无明文`() {
        val name = "com/example/fragmject/FogSample4660"
        val instrumented = instrument(buildSampleClass(name), 4660)
        assertFalse("明文仍留在常量池里", String(instrumented, Charsets.ISO_8859_1).contains("hello world"))
        assertEquals("hello world", load(name, instrumented).getDeclaredMethod("plain").invoke(null))
    }

    @Test
    fun `过短常量保持明文`() {
        val cls = loadAndInstrument("com/example/fragmject/FogShortSample", ::buildShortOnlyClass)
        assertEquals("abc", cls.getDeclaredMethod("short").invoke(null))
    }

    @Test
    fun `反射调用前的常量保持明文`() {
        val cls = loadAndInstrument("com/example/fragmject/FogReflectSample", ::buildReflectClass)
        // 若常量被加密，Class.forName 会拿到乱码并抛 ClassNotFoundException
        assertEquals(String::class.java, cls.getDeclaredMethod("byName").invoke(null))
    }

    @Test
    fun `没有可加密常量时不注入解密方法`() {
        val cls = loadAndInstrument("com/example/fragmject/FogNoopSample", ::buildShortOnlyClass)
        assertTrue(
            "未改写任何字符串时不应留下死方法",
            cls.declaredMethods.none { it.name == StringFogPolicy.DECODE_METHOD },
        )
    }

    // ---------------- 工具 ----------------

    /** 走真实的「ClassReader → FogClassVisitor → ClassWriter」链路，与 AGP 插桩时的结构一致。 */
    private fun instrument(bytes: ByteArray, key: Int): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        reader.accept(FogClassVisitor(writer, key), ClassReader.EXPAND_FRAMES)
        return writer.toByteArray()
    }

    private fun loadAndInstrument(
        internalName: String,
        build: (String) -> ByteArray,
        key: Int = StringFogPolicy.DEFAULT_KEY,
    ): Class<*> = load(internalName, instrument(build(internalName), key))

    private fun load(internalName: String, instrumented: ByteArray): Class<*> {
        val binaryName = internalName.replace('/', '.')
        val loader = object : ClassLoader(StringFogPolicyTest::class.java.classLoader) {
            fun define(): Class<*> = defineClass(binaryName, instrumented, 0, instrumented.size)
        }
        return loader.define()
    }

    private fun buildSampleClass(internalName: String): ByteArray = buildClass(internalName) {
        stringReturningMethod("plain", "hello world")
        stringReturningMethod("chinese", "中文文案")
    }

    private fun buildShortOnlyClass(internalName: String): ByteArray = buildClass(internalName) {
        stringReturningMethod("short", "abc")
    }

    private fun buildReflectClass(internalName: String): ByteArray = buildClass(internalName) {
        val mv: MethodVisitor = visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "byName",
            "()Ljava/lang/Class;",
            null,
            null,
        )
        mv.visitCode()
        mv.visitLdcInsn("java.lang.String")
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/Class",
            "forName",
            "(Ljava/lang/String;)Ljava/lang/Class;",
            false,
        )
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 0)
        mv.visitEnd()
    }

    private fun buildClass(internalName: String, build: ClassVisitor.() -> Unit): ByteArray {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null)
        writer.build()
        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun ClassVisitor.stringReturningMethod(name: String, value: String) {
        val mv: MethodVisitor = visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            name,
            "()Ljava/lang/String;",
            null,
            null,
        )
        mv.visitCode()
        mv.visitLdcInsn(value)
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 0)
        mv.visitEnd()
    }
}
