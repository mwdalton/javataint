/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.SimpleAdviceAdapter;

/** Instrument the java.io.File class to prevent directory traversal attacks.
 * Specifically, let the pathname of a File object be p, and its untainted
 * prefix be u. We require that 
 * File(p).getCanonicalPath().startsWith(File(u).getCanonicalPath()).
 * Or in other words, an untrusted suffix must not 'escape' from the untainted
 * prefix of a file path. 
 *
 * To accomplish this goal, we add a final String field 'untainted prefix' to
 * each File (to track which the untainted prefix of a pathname), and 
 * throw a security exception if a File object is created that violates our
 * rules.
 */

public class FileAdapter extends ClassAdapter implements Opcodes
{
    private String className;

    private static final List ctorList;

    static {
        List l = new ArrayList();
        l.add("(Ljava/lang/String;)V");
        l.add("(Ljava/lang/String;Ljava/lang/String;)V");
        l.add("(Ljava/io/File;Ljava/lang/String;)V");
        l.add("(Ljava/net/URI;)V");
        ctorList = l;
    }

    public FileAdapter(ClassVisitor cv) { 
        super(cv); 
    }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        className = name;
        cv.visit(version, access, name, signature, superName, interfaces);
    }


    /** Instrument File class constructor to follow directory traversal security
     * rules. Each constructor must call super() or invoke another constructor.
     * In the former case, we initialize the untaintedPrefix field directly.
     * In the latter case, we calculate the value for the untainted prefix 
     * field, and pass it as an additional argument to the invoked constructor.
     *
     * As a consequence, each constructor that is called by another constructor
     * must be 'cloned', so that an extra argument containing the initialization
     * value for untaintedPrefix can be passed. Clones are identical to the
     * original method, except that they initialize untaintedPrefix to the
     * value supplied as an argument, rather than computing the appropriate 
     * value directly. A nested static class, CloneOptimizer, performs a pass 
     * over the classfile to determine which methods must be cloned.
     */
    public MethodVisitor visitMethod(final int access, 
            final String name, final String desc, String signature, 
            String[] exceptions) 
    {
        MethodVisitor mv1 = cv.visitMethod(access, name, desc, signature,
                exceptions);

        if (!"<init>".equals(name))
            return mv1;

        if (!CloneOptimizer.cloneList.contains(desc)) {
            if (ctorList.contains(desc))
                return new KnownFileConstructorAdapter(mv1, className, access, 
                                                       desc);
            else
                return new UnknownFileConstructorAdapter(mv1, className, access,
                                                         desc);
        }
                    
        String cloneDesc = ByteCodeUtil.appendArgument(desc, 
                                              "com/siegebrk/ConstructorString");
        int cloneAccess = ByteCodeUtil.inheritAccessFlags(access) + ACC_PRIVATE;

        MethodVisitor mv2 = cv.visitMethod(cloneAccess, name, cloneDesc, 
                                           signature, exceptions);

        if (ctorList.contains(desc)) 
            return new MultiMethodAdapter(
                    new KnownFileConstructorAdapter(mv1, className, access, 
                                                    desc),
                    new ClonedFileConstructorAdapter(mv2, className, 
                                                     cloneAccess, cloneDesc));
        else
            return new MultiMethodAdapter(
                    new UnknownFileConstructorAdapter(mv1, className, access, 
                                                      desc),
                    new ClonedFileConstructorAdapter(mv2, className, 
                                                     cloneAccess, cloneDesc));
    }

    public final void visitEnd() {
        String internalField = ByteCodeUtil.internalName("untaintedPrefix");
        cv.visitField(ACC_PRIVATE + ACC_TRANSIENT + ACC_FINAL, internalField, 
                      "Ljava/lang/String;", null, null).visitEnd();
        ByteCodeUtil.buildGetter(cv, className, internalField, 
                                 "Ljava/lang/String;", ACC_FINAL,
                                 /* Getter method has same name as field */
                                 internalField);
        cv.visitEnd();
    }

    private static class ClonedFileConstructorAdapter extends AdviceAdapter 
    {
        private int savedThis, savedNew;
        private final int newVar;

        public ClonedFileConstructorAdapter(MethodVisitor mv, String owner,
                                            int access, String desc) 
        {
            super(mv, owner, access, "<init>", desc);

            /* Find the index of the last local variable */
            Type[] t = Type.getArgumentTypes(desc);

            int l = 1;
            for (int i = 0; i < t.length - 1; i++) 
                l += t[i].getSize();
            this.newVar = l;
        }

        protected void visitSuper(final int opcode, final String owner, 
                                  final String name, final String desc)
        {
            mv.visitMethodInsn(opcode, owner, name, desc);
            savedThis = newLocal(Type.getObjectType(className));
            savedNew  = newLocal(Type.getObjectType(
                        "com/siegebrk/ConstructorString"));

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ASTORE, savedThis);
            mv.visitVarInsn(ALOAD, newVar);
            mv.visitVarInsn(ASTORE, savedNew);
        }

        /** Pass the supplied value for untaintedPrefix to the invoked
         * constructor
         */
        protected void visitThis(final int opcode, final String owner, 
                                 final String name, final String desc)
        {
            mv.visitVarInsn(ALOAD, newVar);
            mv.visitMethodInsn(opcode, owner, name, 
                               ByteCodeUtil.appendArgument(desc,
                                            "com/siegebrk/ConstructorString"));
        }

        /** Initialize untaintedPrefix to the supplied value */
        protected void onMethodExit(int opcode) {
            if (!superInitialized || opcode != RETURN)
                return;

            mv.visitVarInsn(ALOAD, savedThis);
            mv.visitVarInsn(ALOAD, savedNew);
            mv.visitMethodInsn(INVOKEVIRTUAL, 
                    "com/siegebrk/ConstructorString", "toString", 
                    "()Ljava/lang/String;");
            mv.visitFieldInsn(PUTFIELD, className,
                    ByteCodeUtil.internalName("untaintedPrefix"), 
                    "Ljava/lang/String;");
        }

        public void visitMaxs(int nStack, int nLocals) {
            /* The 'cloned' constructor has one more argument than the
             * actual constructor
             */
            super.visitMaxs(nStack + 2, nLocals + 1);
        }
    }

    private static class KnownFileConstructorAdapter extends AdviceAdapter 
    {
        private int savedThis;
        private final Type[] args;

        public KnownFileConstructorAdapter(MethodVisitor mv, String owner,
                                           int access, String desc) {
            super(mv, owner, access, "<init>", desc);
            this.args = Type.getArgumentTypes(desc);
        }

        protected void visitSuper(final int opcode, final String owner, 
                                  final String name, final String desc)
        {
            mv.visitMethodInsn(opcode, owner, name, desc);

            savedThis = newLocal(Type.getObjectType(className));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ASTORE, savedThis);

            for (int i = 0, l = 1; i < args.length; 
                     l += args[i].getSize(), i++) {
                newLocal(args[i]); /* labels returned are consecutive */
                mv.visitVarInsn(args[i].getOpcode(ILOAD), l);
                mv.visitVarInsn(args[i].getOpcode(ISTORE), savedThis + l);
            }
        }

        private String replaceReturnValue(String methodDesc, String retDesc) {
            return Type.getMethodDescriptor(Type.getObjectType(retDesc),
                    Type.getArgumentTypes(methodDesc));
        }

        private void invokeValidateFile(int offset) {
            for (int i = 0, l = offset + 1; i < args.length; 
                    l += args[i].getSize(), i++)
                mv.visitVarInsn(ALOAD, l);

            mv.visitVarInsn(ALOAD, offset);
            mv.visitMethodInsn(INVOKESTATIC, "com/siegebrk/FileUtil",
                    "validateFile", replaceReturnValue(
                        ByteCodeUtil.appendArgument(methodDesc, className),
                                "java/lang/String"));
        }

        /** Compute appropriate value for untaintedPrefix using com.siegebrk
         * helper routines, and pass this value to the invoked constructor
         */
        protected void visitThis(final int opcode, final String owner, 
                                 final String name, final String desc) 
        {
            mv.visitTypeInsn(NEW, "com/siegebrk/ConstructorString");
            mv.visitInsn(DUP);

            invokeValidateFile(0);

            mv.visitMethodInsn(INVOKESPECIAL, "com/siegebrk/ConstructorString",
                               "<init>", "(Ljava/lang/String;)V");
            mv.visitMethodInsn(opcode, owner, name, 
                               ByteCodeUtil.appendArgument(desc,
                                            "com/siegebrk/ConstructorString"));
        }

        /** For constructor invocations that call super(), initialize the
         * untaintedPrefix field.
         */
        protected void onMethodExit(int opcode) {
            if (!superInitialized || opcode != RETURN)
                return;

            mv.visitVarInsn(ALOAD, savedThis);
            invokeValidateFile(savedThis);
            mv.visitFieldInsn(PUTFIELD, className,
                              ByteCodeUtil.internalName("untaintedPrefix"), 
                              "Ljava/lang/String;");
        }

        public void visitMaxs(int nStack, int nLocals) {
            /* XXX If you add support for File constructors with more than two
             * arguments (or two arguments if one operand is a long/double)
             * update this value. LocalVariablesSorter updates nLocals for us.
             */
            super.visitMaxs(nStack + 5, nLocals);
        }
    }

    private static class UnknownFileConstructorAdapter extends AdviceAdapter 
    {
        private int savedThis;
        private final Type[] args;


        public UnknownFileConstructorAdapter(MethodVisitor mv, String owner,
                                             int access, String desc) {
            super(mv, owner, access, "<init>", desc);
            this.args = Type.getArgumentTypes(desc);
        }

        protected void visitSuper(final int opcode, final String owner, 
                                  final String name, final String desc)
        {
            mv.visitMethodInsn(opcode, owner, name, desc);

            savedThis = newLocal(Type.getObjectType(className));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ASTORE, savedThis);
        }

        protected void visitThis(final int opcode, final String owner, 
                                 final String name, final String desc) 
        {
            mv.visitTypeInsn(NEW, "com/siegebrk/ConstructorString");
            mv.visitInsn(DUP);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL, "com/siegebrk/ConstructorString",
                               "<init>", "(Ljava/lang/String;)V");
            mv.visitMethodInsn(opcode, owner, name, 
                               ByteCodeUtil.appendArgument(desc, 
                                   "com/siegebrk/ConstructorString"));
        }

        protected void onMethodExit(int opcode) {
            if (!superInitialized || opcode != RETURN)
                return;

            mv.visitVarInsn(ALOAD, savedThis);
            mv.visitInsn(ACONST_NULL);
            mv.visitFieldInsn(PUTFIELD, className,
                              ByteCodeUtil.internalName("untaintedPrefix"), 
                              "Ljava/lang/String;");
        }

        public void visitMaxs(int nStack, int nLocals) {
            /* LocalVariablesSorter updates nLocals for us */
            super.visitMaxs(nStack + 3, nLocals);
        }
    }

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new FileAdapter(cv);
        }
    }

    public static class CloneOptimizer extends EmptyClassVisitor 
    {
        private static final List cloneList = new ArrayList();
        private String className;

        public void visit(int version, int access, String name, 
                String signature, String superName, String[] interfaces) 
        {
            className = name;
        }

        public MethodVisitor visitMethod(final int access, 
                final String name, final String desc, String signature, 
                String[] exceptions) 
        {
            if (!"<init>".equals(name))
                return null;

            return new SimpleAdviceAdapter(new EmptyMethodVisitor(), className,
                                           access, name, desc) {
                protected void visitThis(final int opcode, 
                        final String methodOwner, final String name, 
                        final String desc)
                {
                    cloneList.add(desc);
                }
            };
        }

        public static AnalysisBuilder builder(){ return Builder.getInstance(); }

        private static class Builder implements AnalysisBuilder {
            private static final Builder b = new Builder();

            public static AnalysisBuilder getInstance() { return b; }

            public ClassVisitor build() {
                return new CloneOptimizer();
            }
        }
    }
}
