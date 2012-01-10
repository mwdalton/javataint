/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;

/** Add taint tracking to StringBuilder (and StringBuffer objects). Methods 
 * that modify the StringBuilder/StringBuffer are instrumented to 
 * propagate taint. Additionally, new constructors and methods are provided to 
 * initialize taint and query taint information, respectively.
 *
 * This class is actually a skeleton that is instantiated by replacing 
 * @builder@ with StringBuffer or StringBuilder, respectively. See build.xml.
 */

public class @builder@Adapter extends StubAdapter implements Opcodes
{
    private static final List methodList;
    private String className;
    private int version;

    static {
        List l = new ArrayList();
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(Z)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(C)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "([C)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "([CII)Ljava/lang/@builder@;"));

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            l.add(new MethodDecl(ACC_PUBLIC, "append", 
                        "(Ljava/lang/CharSequence;)Ljava/lang/@builder@;"));
            l.add(new MethodDecl(ACC_PUBLIC, "append", 
                        "(Ljava/lang/CharSequence;II)Ljava/lang/@builder@;"));
        }

        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(D)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(F)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(I)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(J)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(Ljava/lang/Object;)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "append",
                             "(Ljava/lang/String;)Ljava/lang/@builder@;")); 
        l.add(new MethodDecl(ACC_PUBLIC, "append", 
                             "(Ljava/lang/StringBuffer;)Ljava/lang/@builder@;"));
        if (VmInfo.version() >= VmInfo.VERSION1_5)
            l.add(new MethodDecl(ACC_PUBLIC, "appendCodePoint", 
                        "(I)Ljava/lang/@builder@;"));

        l.add(new MethodDecl(ACC_PUBLIC, "delete", 
                             "(II)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "deleteCharAt", 
                             "(I)Ljava/lang/@builder@;"));
        
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(IZ)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(IC)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(I[C)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(I[CII)Ljava/lang/@builder@;"));

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                        "(ILjava/lang/CharSequence;)Ljava/lang/@builder@;"));
            l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                        "(ILjava/lang/CharSequence;II)Ljava/lang/@builder@;"));
        }
        
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(ID)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(IF)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(II)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(IJ)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(ILjava/lang/Object;)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "insert", 
                             "(ILjava/lang/String;)Ljava/lang/@builder@;"));

        l.add(new MethodDecl(ACC_PUBLIC, "replace", 
                             "(IILjava/lang/String;)Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "reverse", 
                             "()Ljava/lang/@builder@;"));
        l.add(new MethodDecl(ACC_PUBLIC, "setCharAt", 
                             "(IC)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "setLength", 
                             "(I)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "subSequence", 
                             "(II)Ljava/lang/CharSequence;"));
        l.add(new MethodDecl(ACC_PUBLIC, "substring", 
                             "(I)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "substring", 
                             "(II)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "toString", 
                             "()Ljava/lang/String;"));
        methodList = l;
    }

    public @builder@Adapter(ClassVisitor cv) { super(cv, methodList); }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        this.className = name;
        this.version = version;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    /** Equivalent to the following Java code:
     *
     * public method(arg1...argN?, CharSequence cs, int begin, int end) {
     *     return method(arg1...argN?, String.valueOf(cs).substring(begin,end));
     * }
     */

    private void replaceCharSequenceII(MethodVisitor mv, String name, 
                                       String desc, int argOffset)
    {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        int l = 1;  
       
        Type[] t = Type.getArgumentTypes(desc); 
        Type   r = Type.getReturnType(desc);
        Type[] u = new Type[argOffset + 1];

        System.arraycopy(t, 0, u, 0, argOffset);
        u[argOffset] = Type.getObjectType("java/lang/String");

        for (int i = 0; i < argOffset; l += t[i].getSize(), i++)
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);

        mv.visitVarInsn(ALOAD, l); /* CharSequence */
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", 
                           "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, l+1); /* beginIndex */
        mv.visitVarInsn(ILOAD, l+2); /* endIndex */
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring",
                           "(II)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, className, name, 
                           Type.getMethodDescriptor(r, u));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(l + 3, l + 3);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code:
     *
     * public StringBuilder(String s) {
     *     this(s.length() + 16);
     *     append(s);
     * }
     */
    private void replaceConstructorString(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length",
                           "()I");
        mv.visitIntInsn(BIPUSH, 16);
        mv.visitInsn(IADD);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "(I)V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "append", 
                           "(Ljava/lang/String;)Ljava/lang/@builder@;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code:
     * public StringBuilder(CharSequence cs) {
     *     this(cs.toString());
     * }
     */
    private void replaceConstructorCharSequence(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object",
                           "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>",
                           "(Ljava/lang/String;)V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code: 
     * String subString(int begin) {
     *     return substring(begin, count);
     * }
     */
    private void replaceSubstringI(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "count", "I");
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "substring", 
                           "(II)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code: 
     * public String substring(int begin, int end) {
     *     return toString().substring(begin, end);
     * }
     */
    private void replaceSubstringII(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "toString", 
                           "()Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                           "substring", "(II)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code: 
     * CharSequence subSequence(int begin, int end) {
     *     return substring(begin, end);
     * }
     */

    private void replaceSubSequence(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "substring",
                       "(II)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code: 
     * public String toString() {
     *     s = new String(value, 0, count);
     *     if (taint == null || !taint.isTainted())
     *         return s;
     *     return new String(s, taint);
     * }
     */
    private void replaceToString(MethodVisitor mv) {
        mv.visitCode();

        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "value", "[C");
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "count", "I");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>",
                           "([CII)V");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className,
                      ByteCodeUtil.internalName("taint"), 
                      "Lcom/siegebrk/Taint;");
        Label l0 = new Label();
        mv.visitJumpInsn(IFNONNULL, l0);
        mv.visitInsn(ARETURN);

        mv.visitLabel(l0);
        if (version == V1_6) 
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "java/lang/String" });
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("taint"),
                          "Lcom/siegebrk/Taint;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/siegebrk/Taint",
                           "isTainted", "()Z");

        Label l1 = new Label();
        mv.visitJumpInsn(IFNE, l1);
        mv.visitInsn(ARETURN);

        mv.visitLabel(l1);
        if (version == V1_6) 
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "java/lang/String" });
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP_X1);
        mv.visitInsn(SWAP);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("taint"),
                          "Lcom/siegebrk/Taint;");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>",
                           "(Ljava/lang/String;Lcom/siegebrk/Taint;)V");

        mv.visitInsn(ARETURN);
        mv.visitMaxs(5, 1);
        mv.visitEnd();
    }

    /** Equivalent to the following Java code:
     * public StringBuilder appendCodePoint(int c) {
     *     return append(Character.toChars(c));
     * }
     */
    private void replaceAppendCodePoint(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character",
                           "toChars", "(I)[C");
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "append", 
                           "([C)Ljava/lang/@builder@;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /** When called on a function methodName, define methodName to convert its
     * arguments to a String using String.valueOf, and then recursively invoke
     * methodName on the String value. Used for instrumenting append and insert
     * methods.
     *
     * Instrumenting the method methodName produces bytecode equivalent to the
     * following Java code:
     *
     * public StringBuilder methodName(T savedArg0...savedArgN, T arg0...argM) {
     *     return methodName(savedArg0...savedArgN, String.valueOf(arg0...argM);
     * }
     */
    private void replaceWithStringConverter(MethodVisitor mv, String name, 
                                            String desc, int argsSaved) 
    {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        int l = 1;
        
        Type[] t = Type.getArgumentTypes(desc);
        for (int i = 0; i < t.length; l += t[i].getSize(), i++)
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);


        Type stringType = Type.getObjectType("java/lang/String");
        Type[] valueArgs = new Type[t.length - argsSaved];
        System.arraycopy(t, argsSaved, valueArgs, 0, t.length - argsSaved);

        if (valueArgs.length == 1 && valueArgs[0].getSort() == Type.OBJECT)
            valueArgs[0] = Type.getObjectType("java/lang/Object");

        mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf",
                           Type.getMethodDescriptor(stringType, valueArgs));

        Type methodRet = Type.getReturnType(desc);
        Type[] methodArgs = new Type[argsSaved + 1];
        System.arraycopy(t, 0, methodArgs, 0, argsSaved);
        methodArgs[argsSaved] = stringType;

        mv.visitMethodInsn(INVOKEVIRTUAL, className, name, 
                           Type.getMethodDescriptor(methodRet, methodArgs));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(l, l);
        mv.visitEnd();
    }

    private void buildStringWrapper(final MethodVisitor mv, 
                                    final String methodName,
                                    final String desc) 
    {
        final int offset = "append".equals(methodName) ? 0 : 1;

        new InstrumentationLockBuilder(mv, version, className, methodName, desc)
        {
            public void onUnlocked() { 
                String taintDesc;

                if (offset == 0) /* Append */
                    taintDesc = "(Lcom/siegebrk/Taint;ILcom/siegebrk/Taint;I)Lcom/siegebrk/Taint;";
                else             /* Insert */
                    taintDesc = "(Lcom/siegebrk/Taint;IILcom/siegebrk/Taint;I)Lcom/siegebrk/Taint;";

                /* Handle null arguments safely */
                mv.visitVarInsn(ALOAD, offset + 1);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/String",
                                   "valueOf", 
                                   "(Ljava/lang/Object;)Ljava/lang/String;");
                mv.visitVarInsn(ASTORE, offset + 1);


                /* Now update taint -- the string argument 
                 * or the current StringBuilder may have a null taint, 
                 * so we rely on a helper method
                 */
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, 
                        ByteCodeUtil.internalName("taint"), 
                        "Lcom/siegebrk/Taint;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "count", "I");

                /* Fun situation -- Note here that the count has already been
                 * updated by the append function, so we actually need to
                 * subtract the string argument's length to compute the correct
                 * value to pass to TaintUtil. TaintUtil is expected the 
                 * number of characters in the StringBuilder _BEFORE_ the 
                 * append
                 */
                mv.visitVarInsn(ALOAD, offset + 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                                   "length", "()I");
                mv.visitInsn(ISUB);

                if (offset == 1) 
                    mv.visitVarInsn(ILOAD, offset);

                mv.visitVarInsn(ALOAD, offset + 1);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                                   ByteCodeUtil.internalName("taint"), 
                                   "()Lcom/siegebrk/Taint;");
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                                   "length", "()I");

                mv.visitMethodInsn(INVOKESTATIC, "com/siegebrk/TaintUtil",
                                   methodName, taintDesc);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(SWAP);
                mv.visitFieldInsn(PUTFIELD, className, 
                                  ByteCodeUtil.internalName("taint"), 
                                  "Lcom/siegebrk/Taint;");
            }
        }.build();
    }

    private void buildDeleteCharAtWrapper(final MethodVisitor mv, 
                                          final String methodName,
                                          final String desc) 
    {
        new InstrumentationLockBuilder(mv, version, className, methodName, desc)
        {
            public void onUnlocked() { 
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, 
                        ByteCodeUtil.internalName("taint"), 
                        "Lcom/siegebrk/Taint;");
                mv.visitInsn(DUP);
                Label l = new Label();
                mv.visitJumpInsn(IFNULL, l);

                mv.visitVarInsn(ILOAD, 1);
                mv.visitIincInsn(1, 1);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/siegebrk/Taint",
                        "delete", "(II)Lcom/siegebrk/Taint;");

                mv.visitLabel(l);
                if (version == V1_6)
                    mv.visitFrame(F_SAME1, 0, null, 1,  
                                  new Object[] { "com/siegebrk/Taint" });
                mv.visitInsn(POP);
            }

            public void visitMaxs(int nStack, int nLocal) {
                mv.visitMaxs(Math.max(nStack, 3), nLocal);
            }
        }.build();
    }

    private void buildReplaceWrapper(final MethodVisitor mv, 
                                     final String methodName,
                                     final String desc) 
    {
        new InstrumentationLockBuilder(mv, version, className, methodName, desc)
        {
            public void onUnlocked() { 

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className,  
                                  ByteCodeUtil.internalName("taint"), 
                                  "Lcom/siegebrk/Taint;");
                             
                /* Fixup -- TaintUtil is expecting the length of 
                 * StringBuilder _before_ the replace occurred.  For
                 * a call to replace(int begin, int end, String s), 
                 * count + (end - begin) - s.length() is the old count
                 */

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "count", "I");
                mv.visitVarInsn(ILOAD, 2);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitInsn(ISUB);
                mv.visitInsn(IADD);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                                   "length", "()I");
                mv.visitInsn(ISUB);

                /* Now load begin/end */
                mv.visitVarInsn(ILOAD, 1);
                mv.visitVarInsn(ILOAD, 2);

                /* And finally string taint/length */

                mv.visitVarInsn(ALOAD, 3);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                                   ByteCodeUtil.internalName("taint"),
                                   "()Lcom/siegebrk/Taint;");
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                                   "length", "()I");
                mv.visitMethodInsn(INVOKESTATIC, "com/siegebrk/TaintUtil",
                                   "replace", 
                                   "(Lcom/siegebrk/Taint;IIILcom/siegebrk/Taint;I)Lcom/siegebrk/Taint;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(SWAP);
                mv.visitFieldInsn(PUTFIELD, className, 
                                  ByteCodeUtil.internalName("taint"),
                                  "Lcom/siegebrk/Taint;");
            }
        }.build();
    }

    private void buildSetCharAtWrapper(final MethodVisitor mv, 
                                       final String methodName,
                                       final String desc) 
    {
        new InstrumentationLockBuilder(mv, version, className, methodName, desc)
        {
            public void onUnlocked() { 
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, 
                                  ByteCodeUtil.internalName("taint"), 
                                  "Lcom/siegebrk/Taint;");
                                  
                mv.visitInsn(DUP);
                Label l = new Label();
                mv.visitJumpInsn(IFNULL, l);
               
                mv.visitInsn(DUP); 
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/siegebrk/Taint",
                        "clear", "(I)V");

                mv.visitLabel(l);
                if (version == V1_6)
                    mv.visitFrame(F_SAME1, 0, null, 1,  
                                  new Object[] { "com/siegebrk/Taint" });
                mv.visitInsn(POP);

            }
        }.build();
    }

    private void buildGenericWrapper(final MethodVisitor mv, 
                                     final String methodName,
                                     final String desc) 
    {
        new InstrumentationLockBuilder(mv, version, className, methodName, desc)
        {
            public void onUnlocked() { 
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, 
                                  ByteCodeUtil.internalName("taint"), 
                                  "Lcom/siegebrk/Taint;");
                mv.visitInsn(DUP);
                Label l0 = new Label();
                mv.visitJumpInsn(IFNULL, l0);

                Type[] t = Type.getArgumentTypes(desc);
                Type r = Type.getObjectType("com/siegebrk/Taint");
                int l = 1;
                for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
                    mv.visitVarInsn(t[i].getOpcode(ILOAD), l);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/siegebrk/Taint", 
                                   methodName, Type.getMethodDescriptor(r, t));

                mv.visitLabel(l0);
                if (version == V1_6)
                    mv.visitFrame(F_SAME1, 0, null, 1,  
                            new Object[] { "com/siegebrk/Taint" });
                mv.visitInsn(POP);
            }
        }.build();
    }

    public MethodVisitor visitMethod(final int access, 
            final String name, final String desc, String signature, 
            String[] exceptions) 
    {
        final MethodVisitor mv = super.visitMethod(access, name, desc, 
                signature, exceptions);
        final int offset = "append".equals(name) ? 0 : 1;

        /* First instrument all 'special' methods - constructors or private
         * methods
         */
        if ("<init>".equals(name)) {
            if ("(Ljava/lang/String;)V".equals(desc)) {
                replaceConstructorString(mv);
                return null;
            } else if ("(Ljava/lang/CharSequence;)V".equals(desc)) {
                replaceConstructorCharSequence(mv);
                return null;
            } 
            return mv;
        } 

        /* Any non-special method that is to be instrumented is on methodList */
        if (!methodList.contains(new MethodDecl(access, name, desc)))
            return mv;

        if ("toString".equals(name)) {
            replaceToString(mv);
            return null;
        } else if ("subSequence".equals(name)) {
            replaceSubSequence(mv);
            return null;
        } else if ("substring".equals(name)) {
            if ("(I)Ljava/lang/String;".equals(desc)) 
                replaceSubstringI(mv);
            else
                replaceSubstringII(mv);
            return null;
        } else if ("appendCodePoint".equals(name)) {
            replaceAppendCodePoint(mv);
            return null;
        } else if (("append".equals(name) && 
             "(Ljava/lang/CharSequence;II)Ljava/lang/@builder@;".equals(desc))
                   || ("insert".equals(name) && 
             "(ILjava/lang/CharSequence;II)Ljava/lang/@builder@;".equals(desc)))
        {
            replaceCharSequenceII(mv, name, desc, offset);
            return null;
        } else if (("append".equals(name) &&
                    !"(Ljava/lang/String;)Ljava/lang/@builder@;".equals(desc))
                   || ("insert".equals(name) && 
                    !"(ILjava/lang/String;)Ljava/lang/@builder@;".equals(desc)))
        {
            /* Convert the append or insert arguments to String, and
             * invoke append(String) or insert(offset, String)
             */
            replaceWithStringConverter(mv, name, desc, offset);
            return null;
        }


        /* Build wrapper */
        if ("delete".equals(name) || "reverse".equals(name) || 
                "setLength".equals(name)) 
            buildGenericWrapper(mv, name, desc);
        else if ("append".equals(name) || "insert".equals(name)) 
            buildStringWrapper(mv, name, desc); 
        else if ("deleteCharAt".equals(name)) 
            buildDeleteCharAtWrapper(mv, name, desc);
        else if ("replace".equals(name)) 
            buildReplaceWrapper(mv, name, desc);
        else /* "setCharAt".equals(name) */ 
            buildSetCharAtWrapper(mv, name, desc);

        return super.visitMethod(ByteCodeUtil.inheritAccessFlags(access) 
                                        + ACC_PRIVATE,
                                 ByteCodeUtil.internalName(name), desc, 
                                 signature, exceptions);
    }

    public void onEndBuildStubs() {
        cv.visitField(ACC_PRIVATE + ACC_TRANSIENT,
                      ByteCodeUtil.internalName("taint"), 
                      "Lcom/siegebrk/Taint;", null, null).visitEnd();
       
        InstrumentationLockBuilder.visitEnd(cv, className);
    }

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new @builder@Adapter(cv);
        }
    }
}
