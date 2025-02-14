package com.eastwood.tools.plugins.repo

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.gradle.api.Project

class AsmTransform extends Transform {
    private Project project

    AsmTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "AsmTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) {
        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                File dest = transformInvocation.outputProvider.getContentLocation(
                        dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                processDir(dirInput.file, dest)
                FileUtils.deleteDirectory(dirInput.file)
            }

//            input.jarInputs.each { JarInput jarInput ->
//                File dest = transformInvocation.outputProvider.getContentLocation(
//                        jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
//                processJar(jarInput.file, dest)
//                FileUtils.deleteQuietly(jarInput.file)
//            }
        }
    }

    private static void processDir(File inputDir, File outputDir) {
        if (!outputDir.exists()) outputDir.mkdirs()
        inputDir.eachFileRecurse { File file ->
            if (file.isFile() && file.name.endsWith(".class")) {
                File outputFile = new File(outputDir, file.absolutePath.substring(inputDir.absolutePath.length()))
                if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()
                outputFile.bytes = processClass(file.bytes)
            }
        }
    }

    private static void processJar(File inputJar, File outputJar) {
        if (outputJar.parentFile.exists()) outputJar.parentFile.mkdirs()
        outputJar.bytes = processClass(inputJar.bytes)
    }

    private static byte[] processClass(byte[] srcBytes) {
        ClassReader cr = new ClassReader(srcBytes)
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
        cr.accept(new AsmClassVisitor(cw), ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }
}