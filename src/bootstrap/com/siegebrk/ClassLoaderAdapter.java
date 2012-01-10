/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.List;
import java.util.ArrayList;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ClassLoaderAdapter extends ClassAdapter implements Opcodes
{
    private String className;

    private static final List methodList;

    static {
        List l = new ArrayList();
        l.add(new MethodDecl(ACC_PRIVATE, "defineClass0", 
                "(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;"));

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            l.add(new MethodDecl(ACC_PRIVATE, "defineClass1", 
                        "(Ljava/lang/String;[BIILjava/security/ProtectionDomain;Ljava/lang/String;)Ljava/lang/Class;"));
            l.add(new MethodDecl(ACC_PRIVATE, "defineClass2", 
                        "(Ljava/lang/String;Ljava/nio/ByteBuffer;IILjava/security/ProtectionDomain;Ljava/lang/String;)Ljava/lang/Class;"));
        }

        methodList = l;
    }

    public ClassLoaderAdapter(ClassVisitor cv) { super(cv); }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        this.className = name;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(final int access, 
            final String name, final String desc, String signature, 
            String[] exceptions) 
    {
        return new WrapDefineClassCallAdapter(cv.visitMethod(access, name, desc,
                                                        signature, exceptions));
    }

    private void addDefineClassWrapper(String methodName, String methodDesc) {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE,
                                       ByteCodeUtil.internalName("defineClass"),
                                       methodDesc, null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0); /* This */
        mv.visitVarInsn(ALOAD, 1); /* String name */
        mv.visitVarInsn(ALOAD, 2); /* Buffer (byte[] or ByteBuffer */
        mv.visitVarInsn(ILOAD, 3); /* offset */
        mv.visitVarInsn(ILOAD, 4); /* len */

        Type[] args = Type.getArgumentTypes(methodDesc);
        Type[] newArgs = new Type[3];
        System.arraycopy(args, 1, newArgs, 0, 3); /* Buffer, offset, len */
        String newDesc = Type.getMethodDescriptor(newArgs[0], newArgs);

        mv.visitMethodInsn(INVOKESTATIC, "com/siegebrk/InstrumentationUtils",
                           "instrument", newDesc);

        if ("Ljava/nio/ByteBuffer;".equals(newArgs[0].getDescriptor())) {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
                               "position", "()I");
            mv.visitInsn(SWAP);
            mv.visitInsn(DUP_X1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
                               "remaining", "()I");
        } else {
            mv.visitInsn(DUP);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(SWAP);
        }

        /* Now push all additional arguments */
        int l = 5;
        for (int i = 4; i < args.length; l += args[i].getSize(), i++)
            mv.visitVarInsn(args[i].getOpcode(ILOAD), l);
        mv.visitMethodInsn(INVOKESPECIAL, className, methodName, methodDesc);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(l, l);
        mv.visitEnd();
    }

    private void addExportWrapper() {
        String desc = "(Ljava/lang/String;)Ljava/lang/Class;";
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, 
                                   ByteCodeUtil.internalName("findLoadedClass"),
                                   desc, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "findLoadedClass", desc);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    public void visitEnd() {
        for (int i = 0; i < methodList.size(); i++) {
            MethodDecl m = (MethodDecl) methodList.get(i);
            addDefineClassWrapper(m.name(), m.type());
        }

        addExportWrapper();
        cv.visitEnd();
    }

    private static class WrapDefineClassCallAdapter extends MethodAdapter 
    {
        public WrapDefineClassCallAdapter(MethodVisitor mv) { super(mv); }

        public void visitMethodInsn(int opcode, String owner, String name,
                                    String desc) 
        {
            if (opcode != INVOKESPECIAL 
               || !"java/lang/ClassLoader".equals(owner)
               || !methodList.contains(new MethodDecl(ACC_PRIVATE, name, desc)))
            {
                mv.visitMethodInsn(opcode, owner, name, desc);
                return;
            }

            mv.visitMethodInsn(opcode, owner,
                               ByteCodeUtil.internalName("defineClass"),
                               desc);
        }
    }

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new ClassLoaderAdapter(cv);
        }
    }
}
