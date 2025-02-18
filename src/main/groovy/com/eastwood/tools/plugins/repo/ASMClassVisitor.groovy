package com.eastwood.tools.plugins.repo

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ASMClassVisitor extends ClassVisitor {
    ASMClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM7, cv)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
        if (name == "onStart" && desc == "()V") {
            mv = new ASMMethodVisitor(mv)
            System.out.println("========================>插入成功="+name)
        }
        return mv
    }
}

