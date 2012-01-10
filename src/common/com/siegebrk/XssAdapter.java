/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class XssAdapter extends StubAdapter implements Opcodes
{
    private final List instrumentedMethods;
    private String className;
    private int version;

    private static List xssMethods(String name) {
        if ("java/io/PrintWriter".equals(name))
            return XssContextAdapter.printWriterMethods;
        else if ("java/io/OutputStream".equals(name))
            return XssContextAdapter.outputStreamMethods;
        throw new RuntimeException("Unknown class " + name);
    }

    public XssAdapter(ClassVisitor cv, String className) {
        super(cv, xssMethods(className));
        instrumentedMethods = xssMethods(className);
    }

    public XssAdapter(ClassVisitor cv, XssContextAdapter xca) {
        super(cv, null);
        instrumentedMethods = xca.instrumentedMethods();
    }

    public void visit(int version, int access, String name, String signature, 
                      String superName, String[] interfaces) 
    {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.version = version;
    }

    private void buildGenericWrapper(final MethodVisitor mv,
                                     final String methodName,
                                     final String desc)
    {
        new XssLockBuilder(mv, version, className, methodName, desc) {
            public void onUnlocked() {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                        ByteCodeUtil.internalName("getHtmlValidator"),
                        "()Lcom/siegebrk/HtmlValidator;");

                Type[] t = Type.getArgumentTypes(methodDesc);
                for (int i = 0, l = 1; i < t.length; l += t[i].getSize(), i++)
                    mv.visitVarInsn(t[i].getOpcode(ILOAD), l);

                mv.visitMethodInsn(INVOKEVIRTUAL, "com/siegebrk/HtmlValidator",
                                   methodName, 
                                   Type.getMethodDescriptor(Type.VOID_TYPE, t));
            }
        }.build();
    }

    /* This is called for write(int) methods. This situation is complex
     * because ServletOutputStream.write(int i) is defined in 
     * java.io.OutputStream to write the value (byte) i to the underlying
     * buffer. However, PrintWriter.write(int i) is defined in java.io.Writer
     * to write (char) i to the underlying stream. 
     *
     * Thus we perform a simple instanceof check against java.io.PrintWriter
     * to ensure that we perform the correct version (integer-to-byte or
     * integer-to-character) before calling the underlying HTML validator.
     * We don't perform the instanceof check using ServletOutputStream because
     * this class is not included in rt.jar and thus is not guaranteed to 
     * be loaded.
     */
    private void buildWriteIntWrapper(final MethodVisitor mv,
                                      final String methodName,
                                      final String desc)
    {
        new XssLockBuilder(mv,version,className,methodName,desc) {
            public void onUnlocked() {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                        ByteCodeUtil.internalName("getHtmlValidator"),
                        "()Lcom/siegebrk/HtmlValidator;");
                
                if ("javax/servlet/ServletOutputStream".equals(className)) {
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitInsn(I2B);
                    mv.visitMethodInsn(INVOKEVIRTUAL, 
                                       "com/siegebrk/HtmlValidator", "write", 
                                       "(B)V");
                } else if ("java/io/PrintWriter".equals(className)) {
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitInsn(I2C);
                    mv.visitMethodInsn(INVOKEVIRTUAL, 
                                       "com/siegebrk/HtmlValidator", "print", 
                                       "(C)V");
                } else {  
                    /* Runtime check required */
                    Label l0 = new Label();
                    Label l1 = new Label();
                    
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitTypeInsn(INSTANCEOF, "java/io/PrintWriter");
                    mv.visitJumpInsn(IFNE, l1);

                    /* If-branch, we're a ServletOutputStream.  */
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitInsn(I2B);
                    mv.visitMethodInsn(INVOKEVIRTUAL, 
                                       "com/siegebrk/HtmlValidator", "write", 
                                       "(B)V");
                    mv.visitJumpInsn(GOTO, l0);

                    /* Else-branch, we're a PrintWriter  */
                    mv.visitLabel(l1);
                    if (version == V1_6)
                        mv.visitFrame(F_SAME1, 0, null, 1, 
                                new Object[] { "com/siegebrk/HtmlValidator" });

                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitInsn(I2C);
                    mv.visitMethodInsn(INVOKEVIRTUAL, 
                                       "com/siegebrk/HtmlValidator", "print", 
                                       "(C)V");
                    mv.visitLabel(l0);
                    if (version == V1_6) {
                        mv.visitFrame(F_SAME, 0, null, 0 , null);
                        /* We can't end on a visitFrame because 
                         * InstrumentationLockAdapter performs a visitFrame 
                         * after this method completes. Two consecutive 
                         * visitFrames cause the Java 6 type checker to barf, 
                         * so just pad with a single NOP.
                         */
                        mv.visitInsn(NOP);
                    }
                } 
            }
        }.build();
    }

    public MethodVisitor visitMethod(final int access, final String name, 
                                     final String desc, String signature, 
                                     String[] exceptions) 
    {
        final MethodVisitor mv = super.visitMethod(access, name, desc, 
                                                   signature, exceptions);

        if (!instrumentedMethods.contains(new MethodDecl(access, name, desc)))
            return mv;

        /* Else wrap and rename */
        if ("write".equals(name) && "(I)V".equals(desc))
            buildWriteIntWrapper(mv, name, desc);
        else
            buildGenericWrapper(mv, name, desc);

        return super.visitMethod(ByteCodeUtil.inheritAccessFlags(access) 
                                        + ACC_PRIVATE,
                                 ByteCodeUtil.internalName(name), desc, 
                                 signature, exceptions);
    }

    public void onEndBuildStubs() {
        InstrumentationLockBuilder.visitEnd(cv, className);

        cv.visitField(ACC_PRIVATE + ACC_TRANSIENT, 
                      ByteCodeUtil.internalName("htmlValidator"), 
                      "Lcom/siegebrk/HtmlValidator;", null, null).visitEnd();
        ByteCodeUtil.buildGetter(cv, className, 
                                 ByteCodeUtil.internalName("htmlValidator"),
                                 "Lcom/siegebrk/HtmlValidator;", 0,
                                 ByteCodeUtil.internalName("getHtmlValidator"));
        ByteCodeUtil.buildSetter(cv, className, 
                                 ByteCodeUtil.internalName("htmlValidator"),
                                 "Lcom/siegebrk/HtmlValidator;", 0,
                                 ByteCodeUtil.internalName("setHtmlValidator"));
    }

    public static InstrumentationBuilder builder(String buildClass) { 
        return Builder.getInstance(buildClass); 
    }

    private static abstract class XssLockBuilder
        extends InstrumentationLockBuilder
    {
        public XssLockBuilder(MethodVisitor mv, int version,
                              String className, String methodName, 
                              String methodDesc) 
        {
            super(mv, version, className, methodName, methodDesc);
        }

        /* if HtmlValidator is null, don't even bother with inc/dec of
         * lock, just call wrapped method and exit 
         */
        public void onMethodEnter() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                    ByteCodeUtil.internalName("getHtmlValidator"),
                    "()Lcom/siegebrk/HtmlValidator;");
            Label l0 = new Label();
            mv.visitJumpInsn(IFNONNULL, l0);

            mv.visitVarInsn(ALOAD, 0);
            int l = 1;
            Type[] t = Type.getArgumentTypes(methodDesc);

            for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
                mv.visitVarInsn(t[i].getOpcode(ILOAD), l);

            mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                    ByteCodeUtil.internalName(methodName), methodDesc);
            mv.visitInsn(Type.getReturnType(methodDesc).getOpcode(IRETURN));

            mv.visitLabel(l0);
            if (version == V1_6)
                mv.visitFrame(F_SAME, 0, null, 0, null);
        }
    }


    private static class Builder implements InstrumentationBuilder {
        private static final Builder OUTPUTSTREAM;
        private static final Builder PRINTWRITER;
            
        static {
            OUTPUTSTREAM = new Builder("java/io/OutputStream");
            PRINTWRITER  = new Builder("java/io/PrintWriter");
        }

        private final String builderClass;

        public Builder(String builderClass) {
            this.builderClass = builderClass;
        }

        public static InstrumentationBuilder getInstance(String s) { 
            if ("java/io/OutputStream".equals(s))
                return OUTPUTSTREAM;
            else if ("java/io/PrintWriter".equals(s))
                return PRINTWRITER;
            else
                throw new RuntimeException("Unknown class " + s);
        }

        public ClassVisitor build(ClassVisitor cv) {
            return new XssAdapter(cv, builderClass);
        }
    }
}
