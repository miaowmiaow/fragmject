package com.example.plugin.privacy

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class PrivacyTransform extends Transform {

    @Override
    String getName() {
        return "Privacy"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        Collection<TransformInput> inputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        if (!transformInvocation.isIncremental()) {
            outputProvider.deleteAll()
        }
        inputs.each { input ->
            //遍历目录
            input.directoryInputs.each { directoryInput ->
                handleDirectoryInput(directoryInput, outputProvider)
            }
            // 遍历jar 第三方引入的 class
            input.jarInputs.each { jarInput ->
                handleJarInput(jarInput, outputProvider)
            }
        }
    }

    static void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        if (directoryInput.file.isDirectory()) {
            directoryInput.file.eachFileRecurse { file ->
                if (filterClass(file.name)) {
                    ClassReader classReader = new ClassReader(file.bytes)
                    FileOutputStream fos = new FileOutputStream(file.parentFile.absolutePath + File.separator + name)
                    fos.write(modifyClass(classReader))
                    fos.close()
                }
            }
        }
        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    static void handleJarInput(JarInput jarInput, TransformOutputProvider outputProvider) {
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tmpFile = new File(jarInput.file.getParent() + File.separator + "temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
                if (filterClass(entryName)) {
                    jarOutputStream.putNextEntry(zipEntry)
                    ClassReader classReader = new ClassReader(sourceClassBytes)
                    jarOutputStream.write(modifyClass(classReader))
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            jarFile.close()
            def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }

    static byte[] modifyClass(ClassReader classReader) {
        ClassNode classNode = new ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        List<MethodNode> methods = classNode.methods
        for (methodNode in methods) {
            InsnList instructions = methodNode.instructions
            ListIterator<AbstractInsnNode> iterator = instructions.iterator()
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next()
                PrivacyEntity find = null
                if (insnNode instanceof FieldInsnNode) {
                    find = PrivacyPlugin.PRIVACY_FIELDS.find {
                        it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }
                }
                if (insnNode instanceof MethodInsnNode) {
                    find = PrivacyPlugin.PRIVACY_METHODS.find {
                        it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }
                }
                if (find != null) {
                    printTargetNode(classNode, methodNode, insnNode)
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    static void printTargetNode(ClassNode classNode, MethodNode methodNode, AbstractInsnNode insnNode) {
        String str = "---------------------------------------------------------------------------\n"
        str += classNode.name + "." + methodNode.name + "->" + methodNode.desc + " \n"
        str += insnNode.owner + "." + insnNode.name + "->" + insnNode.desc + " \n"
        InsnList insnList = new InsnList()
        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
        insnList.add(new LdcInsnNode(str))
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false))
        methodNode.instructions.insertBefore(insnNode, insnList)
        println(str)
    }

    static boolean filterClass(String name) {
        return (name.endsWith(".class")
                && !name.startsWith("R\$")
                && !name.startsWith("R2\$")
                && "R.class" != name
                && "R2.class" != name
                && "BuildConfig.class" != name)
    }

}