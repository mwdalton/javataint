/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

/* StringMaker is a JRockit-only class used for string optimizations. If the
 * JRockit JVM detects a number of sequential appends to a StringBuilder or
 * Buffer followed by a call to the toString() method 
 * (i.e. bytecode equivalent to 'str1 + str2 + str3' in Java), it replaces
 * the StringBuilder with a StringMaker.
 *
 * Internally, StringMaker has an array of Strings that it converts to a
 * single String once toString() is called. This is significantly more efficient
 * than StringBuilder/Buffer, as those classes have an internal char array that
 * contains the characters of each appended string, and that array must 
 * potentially be re-sized each time an append occurs. StringMaker delays
 * the allocation of this array until toString() is called, avoiding all the
 * intermediate re-sizing that occurs between the initial and final append
 * operations.
 *
 * For more information see:
 * http://developers.sun.com/learning/javaoneonline/2007/pdf/TS-2171.pdf
 */

public class StringMakerAdapter extends ClassAdapter implements Opcodes
{
    private String className;

    public StringMakerAdapter(ClassVisitor cv) { super(cv); }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        this.className = name;
        cv.visit(version, access, name, signature, superName, interfaces);
    }
    
    private void buildToStringWrapper(MethodVisitor mv)
    {
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "strings",
                          "[Ljava/lang/String;");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "size", "I");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className,
                           ByteCodeUtil.internalName("toString"),
                           "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "com/siegebrk/StringUtil",
                           "concat", "([Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 1); 
        mv.visitEnd();
    }

    public MethodVisitor visitMethod(final int access, 
            final String name, final String desc, String signature, 
            String[] exceptions) 
    {
        final MethodVisitor mv = cv.visitMethod(access, name, desc, 
                                                signature, exceptions);
        if (!"toString".equals(name) || !"()Ljava/lang/String;".equals(desc))
            return mv;

        /* Rename and wrap invocations of toString() */
        buildToStringWrapper(mv);
        return cv.visitMethod(access, ByteCodeUtil.internalName(name),
                              desc, signature, exceptions);
    }
    
    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new StringMakerAdapter(cv);
        }
    }
}
