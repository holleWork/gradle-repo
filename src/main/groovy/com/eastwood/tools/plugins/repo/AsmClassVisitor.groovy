package com.eastwood.tools.plugins.repo

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Label

class AsmClassVisitor extends ClassVisitor {
    AsmClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM7, cv)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
        if (name == "onStart" && desc == "()V") {
            mv = new AsmMethodVisitor(mv)
            System.out.println("========================>插入成功="+name)
        }
        return mv
    }
}

class AsmMethodVisitor extends MethodVisitor {
    private Label originalCodeLabel = new Label()

    AsmMethodVisitor(MethodVisitor mv) {
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
        super.visitMaxs(Math.max(maxStack, 4), maxLocals + 1)

    }
    private void insertNewLogic() {
        // int c = (Integer) SharedPreferencesUtil.getValue(...)
        mv.visitVarInsn(Opcodes.ALOAD, 0)                     // this

        mv.visitFieldInsn(Opcodes.GETSTATIC,
                "com/hhhc/mediacenter/M",
                "SPN",
                "Ljava/lang/String;")

        mv.visitLdcInsn("cishu")
                // 加载默认值 0
        mv.visitInsn(Opcodes.ICONST_0)

        // 调用 SharedPreferencesUtil.getValue 方法
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hhhc/mediacenter/util/SharedPreferencesUtil",
                "getValue",
                "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/Object;",
                false
        )

        // 将返回值强制转换为 Integer
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")

        // 调用 Integer.intValue() 方法
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Integer",
                "intValue",
                "()I",
                false
        )
        mv.visitVarInsn(Opcodes.ISTORE, 1)                  // 存储在局部变量槽1

        // 2. 调用 SharedPreferencesUtil.setValue 方法
        // 加载 this 对象
        mv.visitVarInsn(Opcodes.ALOAD, 0)

        // 加载 M.SPN 常量
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/hhhc/mediacenter/M", "SPN", "Ljava/lang/String;")

        // 加载字符串 "cishu"
        mv.visitLdcInsn("cishu")

        // 加载局部变量 c 的值
        mv.visitVarInsn(Opcodes.ILOAD, 1)

        // 将 c 的值加 1
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
// 调用 SharedPreferencesUtil.setValue 方法
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hhhc/mediacenter/util/SharedPreferencesUtil",
                "setValue",
                "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;I)V",
                false
        )

        // 3. 插入 if 逻辑
        // 加载局部变量 c
        mv.visitVarInsn(Opcodes.ILOAD, 1)

        // 加载常量 100
        mv.visitLdcInsn(100)

        // 比较 c 和 100
        // 不满足条件跳转到原有代码
        mv.visitJumpInsn(Opcodes.IF_ICMPLE, originalCodeLabel)

        // 调用 finish() 方法
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "androidx/appcompat/app/AppCompatActivity",
                "finish",
                "()V",
                false
        )

        // 插入 return 语句
        mv.visitInsn(Opcodes.RETURN)

    }
}