/* Copyright 2009 Michael Dalton */
package jtaint;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Properties;

public final class ByteCodeUtil implements Opcodes
{
    private static final String internalPrefix = "@internal@";

    public static String internalName(String s) {
        return internalPrefix + s;
    }

    public static void buildGetter(ClassVisitor cv, String owner, 
                                   String fieldName, String fieldDesc,
                                   int accFlags, String methodName)
    {
        MethodVisitor mv = cv.visitMethod(accFlags + ACC_PUBLIC, methodName, 
                                          "()" + fieldDesc, null, null);
        Type t = Type.getType(fieldDesc);

        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, fieldName, fieldDesc);
        mv.visitInsn(t.getOpcode(IRETURN));

        mv.visitMaxs(t.getSize(), 1);
        mv.visitEnd();
    }

    public static void buildSetter(ClassVisitor cv, String owner, 
                                   String fieldName, String fieldDesc,
                                   int accFlags, String methodName)
    {
        MethodVisitor mv = cv.visitMethod(accFlags + ACC_PUBLIC, methodName,
                                          "(" + fieldDesc + ")V", null, null);
        Type t = Type.getType(fieldDesc);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(t.getOpcode(ILOAD), 1);
        mv.visitFieldInsn(PUTFIELD, owner, fieldName, fieldDesc);
        mv.visitInsn(RETURN);

        mv.visitMaxs(1 + t.getSize(), 1 + t.getSize());
        mv.visitEnd();
    }


    /* We do _not_ inherit ACC_SYNCHRONIZED - if the wrapper is synchronized,
     * the wrappee should be private and unsynchronized
     */
    private static int inheritedFlags =  ACC_FINAL + ACC_STRICT + ACC_STATIC 
                                         + ACC_VARARGS;

    public static int inheritAccessFlags(int flags) {
        return flags & inheritedFlags;
    }

    public static String appendArgument(String methodDesc, String argDesc) {
        Type[] t = Type.getArgumentTypes(methodDesc);
        int len = t.length;
        Type[] u = new Type[len + 1];

        System.arraycopy(t, 0, u, 0, len);
        u[len] = Type.getObjectType(argDesc);

        return Type.getMethodDescriptor(Type.getReturnType(methodDesc), u);
    }
}
