package com.eastwood.tools.plugins.repo

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ASMTransform2 extends Transform {
    @Override
    public String getName() {
        return "ASMTransform"
    }

/**
 * 只需要class文件输入
 * @return
 */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 如果是app使用插件，则传递所有的class进来，如果是lib使用的话，仅传递对应lib project的class过来即可
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
//        if (mType == PROJECT_TYPE.LIB) {
//            return TransformManager.PROJECT_ONLY
//        } else {
            return TransformManager.SCOPE_FULL_PROJECT
//        }
    }

    @Override
    public boolean isIncremental() {
        return false
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        if (!transformInvocation.isIncremental()) {
            //不是增量编译删除所有的outputProvider
            transformInvocation.getOutputProvider().deleteAll()
        }


//消费型输入，可以从中获取jar包和class文件夹路径。需要输出给下一个任务
        Collection<TransformInput> inputs = transformInvocation.getInputs()
//引用型输入，无需输出。
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs()
//OutputProvider管理输出路径，如果消费型输入为空，你会发现OutputProvider == null
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR)
//将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
//                transformJar(jarInput.getFile(), dest)
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {

                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY)
//将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                processDir(directoryInput.getFile(), dest)
            }


        }
    }


    private void processDir(File inputDir, File outputDir) throws IOException {
        processFile(inputDir, inputDir, outputDir)
    }

    private void processFile(File root, File file, File outputDir) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                processFile(root, child, outputDir)
            }
        } else if (file.getName().endsWith(".class")) {
            handleClassFile(file, root, outputDir)
        }
    }

    private void handleClassFile(File classFile, File inputDir, File outputDir) throws IOException {
        // Read class bytes
        byte[] classBytes = readBytes(classFile)

        // Setup ASM components
        ClassReader classReader = new ClassReader(classBytes)
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, classWriter) {
            @Override
             MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                if ("onStart".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                         void visitCode() {
                            // Insert code: if (1000 < 1) { finish() }
//                            super.visitCode()
                            mv.visitIntInsn(Opcodes.SIPUSH, 1000)
                            mv.visitInsn(Opcodes.ICONST_1)
                            Label label = new Label()
                            mv.visitJumpInsn(Opcodes.IF_ICMPGE, label)
                            mv.visitVarInsn(Opcodes.ALOAD, 0)
                            mv.visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    "android/app/Activity",
                                    "finish",
                                    "()V",
                                    false
                            )
                            mv.visitLabel(label)
                            System.out.println("========================>插入成功="+name)
                        }
                    }
                }else{
                    return mv
                }

            }
        }

        classReader.accept(cv, ClassReader.EXPAND_FRAMES)
        byte[] modifiedClass = classWriter.toByteArray()

        // Write to output
        String relativePath = inputDir.toPath().relativize(classFile.toPath()).toString()
        File outputFile = new File(outputDir, relativePath)
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs()
        }
        writeBytes(outputFile, modifiedClass)
    }

    private static byte[] readBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file)
        byte[] bytes = new byte[(int) file.length()]
        fis.read(bytes)
        return bytes

    }

    private static void writeBytes(File file, byte[] bytes) throws IOException {
        FileOutputStream fos = new FileOutputStream(file)
        fos.write(bytes)

    }
}
