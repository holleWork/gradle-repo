package com.eastwood.tools.plugins.repo

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ASMMethodVisitor extends MethodVisitor {
    private Label originalCodeLabel = new Label()

    ASMMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM7, mv)
    }

    @Override
    void visitCode() {
        // 在方法最前面插入新逻辑
        insertNewLogic()
        // 原有方法体开始标记
        mv.visitLabel(originalCodeLabel)
        super.visitCode()  // 关键！触发原有字节码处理

    }

    @Override
    void visitMaxs(int maxStack, int maxLocals) {
        // 原有方法体结束标记
        super.visitMaxs(Math.max(maxStack, 4), maxLocals + 3)

    }
    private void insertNewLogic() {
        // 定义局部变量槽位（根据实际情况调整）
        int spSlot = 1   // SharedPreferences
        int cSlot = 2    // int c
        int editorSlot = 3 // Editor

        int  term = 100*100*10

        // ---------------------------------------------------------------
        // android.content.SharedPreferences sharedPreferences = getSharedPreferences("testPM", 0);
        // ---------------------------------------------------------------
        mv.visitVarInsn(Opcodes.ALOAD, 0)                 // this
        mv.visitLdcInsn("testPM")                         // spName
        mv.visitInsn(Opcodes.ICONST_0)                    // mode
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "androidx/appcompat/app/AppCompatActivity",
                "getSharedPreferences",
                "(Ljava/lang/String;I)Landroid/content/SharedPreferences;",
                false)
        mv.visitVarInsn(Opcodes.ASTORE, spSlot)           // 存储到槽位1

        // ---------------------------------------------------------------
        // int c = sharedPreferences.getInt("cs", 0);
        // ---------------------------------------------------------------
        mv.visitVarInsn(Opcodes.ALOAD, spSlot)            // 加载sp实例
        mv.visitLdcInsn("cs")                             // key
        mv.visitInsn(Opcodes.ICONST_0)                    // 默认值
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                "android/content/SharedPreferences",
                "getInt",
                "(Ljava/lang/String;I)I",
                true)
        mv.visitVarInsn(Opcodes.ISTORE, cSlot)            // 存储到槽位2

        // ---------------------------------------------------------------
        // SharedPreferences.Editor editor = sharedPreferences.edit();
        // ---------------------------------------------------------------
        mv.visitVarInsn(Opcodes.ALOAD, spSlot)            // 加载sp实例
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                "android/content/SharedPreferences",
                "edit",
                "()Landroid/content/SharedPreferences\$Editor;",
                true)
        mv.visitVarInsn(Opcodes.ASTORE, editorSlot)       // 存储到槽位3

        // ---------------------------------------------------------------
        // editor.putInt("cs", c++);
        // ---------------------------------------------------------------
        mv.visitVarInsn(Opcodes.ALOAD, editorSlot)        // 加载editor
        mv.visitLdcInsn("cs")                             // key
        mv.visitVarInsn(Opcodes.ILOAD, cSlot)
        // 将 c 的值加 1
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)

        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                "android/content/SharedPreferences\$Editor",
                "putInt",
                "(Ljava/lang/String;I)Landroid/content/SharedPreferences\$Editor;",
                true)
        mv.visitInsn(Opcodes.POP)                         // 丢弃返回值

        // ---------------------------------------------------------------
        // editor.apply();
        // ---------------------------------------------------------------
        mv.visitVarInsn(Opcodes.ALOAD, editorSlot)
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                "android/content/SharedPreferences\$Editor",
                "apply",
                "()V",
                true)

        // ---------------------------------------------------------------
        // if (c > 100) { finish(); return; }
        // ---------------------------------------------------------------
        mv.visitVarInsn(Opcodes.ILOAD, cSlot)             // 加载自增后的c值
        mv.visitLdcInsn(term)
        mv.visitJumpInsn(Opcodes.IF_ICMPLE, originalCodeLabel) // 不满足条件跳转

        // finish()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "androidx/appcompat/app/AppCompatActivity",
                "finish",
                "()V",
                false)

        // return
        mv.visitInsn(Opcodes.RETURN)

    }
}
