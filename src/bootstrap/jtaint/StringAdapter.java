/*
 *  Copyright 2009-2012 Michael Dalton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jtaint;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;

/** Add taint tracking to String objects. Methods that create Strings
 * are instrumented to propagate taint. Additionally, new constructors
 * and methods are provided to initialize taint and query taint information,
 * respectively.
 */

public class StringAdapter extends StubAdapter implements Opcodes
{
    private static final List methodList;
    private String className;
    private int version;

    static {
        List l = new ArrayList();
        l.add(new MethodDecl(ACC_PUBLIC, "concat", 
                    "(Ljava/lang/String;)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "replace", "(CC)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "subSequence", 
                    "(II)Ljava/lang/CharSequence;"));
        l.add(new MethodDecl(ACC_PUBLIC, "substring", "(I)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "substring",
                                "(II)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "toLowerCase",
                    "(Ljava/util/Locale;)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "toLowerCase", 
                    "()Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "toUpperCase",
                    "(Ljava/util/Locale;)Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "toUpperCase", 
                    "()Ljava/lang/String;"));
        l.add(new MethodDecl(ACC_PUBLIC, "trim", "()Ljava/lang/String;"));
        methodList = l;
    }

    public StringAdapter(ClassVisitor cv) { super(cv, methodList); }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        this.className = name;
        this.version = version;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    /** Wrap String methods that create new Strings so that taint is propagated.
     * Wrappers call the original method, and then call a helper routine in
     * jtaint with the original String object, method arguments, and the
     * return value from the original method. The jtaint helper then 
     * returns a String with the appropriate taint value.
     */

    private void buildTaintWrapper(MethodVisitor mv, String name, String desc)
    {
        mv.visitCode();

        Type[] t = Type.getArgumentTypes(desc);

        mv.visitVarInsn(ALOAD, 0);
        int l = 1;
        for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);

        mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                           ByteCodeUtil.internalName(name), desc);
        mv.visitVarInsn(ASTORE, l);

        mv.visitVarInsn(ALOAD, 0);
        l = 1;
        for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);
        mv.visitVarInsn(ALOAD, l);

        /* We call the jtaint helper method by passing the arguments
         * this_object, arg1, arg2, ..., argN, result_object, so append
         * and prepend an extra java/lang/String object to the arg list.
         */
        Type[] u = new Type[t.length+2];
        Type stringType = Type.getObjectType("java/lang/String");
        u[0] = u[u.length - 1] = stringType;
        System.arraycopy(t, 0, u, 1, t.length);

        String helperDesc  = Type.getMethodDescriptor(stringType, u);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/StringUtil",
                                name, helperDesc);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(l + 1, l + 1);
        mv.visitEnd();
    }

    /** Force to{Upper/Lower}Case() to return 
     * to{Upper/LowerCase}(java.util.Locale.getDefault())
     */
    private void replaceChangeCase(MethodVisitor mv, String name) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Locale", "getDefault",
                "()Ljava/util/Locale;");
        mv.visitMethodInsn(INVOKEVIRTUAL, className, name, 
                           "(Ljava/util/Locale;)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /** Force substring(begin) to return substring(begin, this.count) */
    private void replaceSubstring(MethodVisitor mv) {
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

    /** Force subSequence(begin, end) to return substring(begin, end) */
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

    /** Force the StringBuilder/StringBuffer constructors to convert to
     * a String and invoke the String constructor
     */
    private void replaceConstructorStringBuilder(MethodVisitor mv,
                                                 String name) 
    {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, name,
                           "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESPECIAL, className,
                           "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private void copyField(MethodVisitor mv, String name, String desc) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETFIELD, className, name, desc);
        mv.visitFieldInsn(PUTFIELD, className, name, desc);
    }

    /** Force the String constructor to just copy all fields */
    private void replaceConstructorString(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        copyField(mv, "offset", "I");
        copyField(mv, "count", "I");
        copyField(mv, "value", "[C");
        copyField(mv, ByteCodeUtil.internalName("tainted"), "Z");

        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    public MethodVisitor visitMethod(final int access, 
            final String name, final String desc, String signature, 
            String[] exceptions) 
    {
        final MethodVisitor mv = super.visitMethod(access, name, desc, 
                signature, exceptions);

        if ("<init>".equals(name)) {
            if ("(Ljava/lang/String;)V".equals(desc)) {
                replaceConstructorString(mv);
                return null;
            } else if ("(Ljava/lang/StringBuilder;)V".equals(desc)) {
                replaceConstructorStringBuilder(mv, "java/lang/StringBuilder");
                return null;
            } else if("(Ljava/lang/StringBuffer;)V".equals(desc)) {
                replaceConstructorStringBuilder(mv, "java/lang/StringBuffer");
                return null;
            } else 
                return new TaintedInitAdapter(mv, className, access, desc);
        }

        if (!methodList.contains(new MethodDecl(access, name, desc))) 
            return mv;

        /* Check for methods we replace */

        if ("subSequence".equals(name)) {
            replaceSubSequence(mv);
            return null;
        }

        if ("substring".equals(name) && "(I)Ljava/lang/String;".equals(desc)) {
            replaceSubstring(mv);
            return null;
        }

        if (("toLowerCase".equals(name) || "toUpperCase".equals(name)) &&
                "()Ljava/lang/String;".equals(desc)) {
            replaceChangeCase(mv, name);
            return null;
        }

        /* Otherwise rename and wrap */
        buildTaintWrapper(mv, name, desc);
        return super.visitMethod(access, ByteCodeUtil.internalName(name), desc,
                                 signature, exceptions);
    }

    /** Export package-private java.lang methods for use by jtaint helper 
     * functions. 
     */
    private void buildExportWrapper(ClassVisitor cv, String exportOwner, 
                                          String name, String desc)
    {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, 
                                          ByteCodeUtil.internalName(name), 
                                          desc, null, null);
        mv.visitCode();
        Type[] t = Type.getArgumentTypes(desc);

        int l = 0;
        for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);
        mv.visitMethodInsn(INVOKESTATIC, exportOwner, name, desc);

        mv.visitInsn(Type.getReturnType(desc).getOpcode(IRETURN));
        mv.visitMaxs(l, l);
        mv.visitEnd();
    }

    /** Create new method that compares its argument to the package-private
     * constant Character.ERROR. This must be exported for jtaint helper
     * methods. Equivalent to the following Java code:
     *
     * public static boolean isError(int c) {
     *     return c == Character.ERROR;
     * }
     */
    private void addIsErrorMethod(ClassVisitor cv) {
        boolean isError = false;
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC,
                                          ByteCodeUtil.internalName("isError"),
                                          "(I)Z", null, null);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);

        /* Test to see if java/lang/Character uses ERROR or CHAR_ERROR 
         * If ERROR cannot be found, an getDeclaredFields throws an exception
         */
        try {
            Character.class.getDeclaredField("ERROR");
            isError = true;
        } catch (Throwable th) { /* ignore */ } 
        
        if (isError)
            mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "ERROR", "I");
        else
            mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "CHAR_ERROR", 
                              "C");

        Label l = new Label();
        mv.visitJumpInsn(IF_ICMPEQ, l);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);

        mv.visitLabel(l);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);

        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /** Create a new method that returns a Taint object representing the taint
     * for this String. Equivalent to the following Java code:
     * 
     * public Taint taint() {
     *     if (!tainted) {
     *         return null;
     *     } else {
     *         return jtaint.StringUtil.stringToTaint(value, count);
     *     }
     * }
     */

    private void addTaintMethod(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC,
                                          ByteCodeUtil.internalName("taint"),
                                          "()Ljtaint/Taint;", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("tainted"), "Z");

        Label l = new Label();
        mv.visitJumpInsn(IFNE, l);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);

        mv.visitLabel(l);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "value", "[C");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "count", "I");
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/StringUtil",
                           "stringToTaint", "([CI)Ljtaint/Taint;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4,1);
        mv.visitEnd();
    }

    /** Add a new constructor to a create a (partially or fully) tainted 
     * String. Equivalent to the following Java code:
     *
     * public String(String original, Taint t) {
     *     super();
     *     this.count = original.count;
     *
     *     if (!t.isTainted()) {
     *         this.offset = original.offset;
     *         this.value = original.value;
     *         this.tainted = original.tainted;
     *         return
     *     }
     *
     *     this.offset = 0;
     *     this.value = jtaint.StringUtil.taintToString(original, t)
     *     if (this.value.length == this.count)
     *         this.tainted = false;
     *     else
     *         this.tainted = true;
     *     return;
     * The final check (if value.length == count) is true only when an error
     * occurs during the execution of taintToString
     */

    private void addConstructor(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>",
                                "(Ljava/lang/String;Ljtaint/Taint;)V",
                                 null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        copyField(mv, "count", "I");

        Label l0 = new Label();
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "jtaint/Taint",
                           "isTainted", "()Z");
        mv.visitJumpInsn(IFNE, l0);

        /* Taint object is actually untainted, copy all fields and return */
        copyField(mv, "offset", "I");
        copyField(mv, "value", "[C");
        copyField(mv, ByteCodeUtil.internalName("tainted"), "Z");
        mv.visitInsn(RETURN);

        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTFIELD, className, "offset", "I");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/StringUtil",
                           "taintToString", 
                           "(Ljava/lang/String;Ljtaint/Taint;)[C");
        mv.visitInsn(DUP_X1);
        mv.visitFieldInsn(PUTFIELD, className, "value", "[C");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "count", "I");

        Label l1 = new Label();
        mv.visitJumpInsn(IF_ICMPEQ, l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTFIELD, className, 
                          ByteCodeUtil.internalName("tainted"), "Z");
        mv.visitInsn(RETURN);

        mv.visitLabel(l1);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(PUTFIELD, className, 
                          ByteCodeUtil.internalName("tainted"), "Z");

        mv.visitInsn(RETURN);

        mv.visitMaxs(3, 3);
        mv.visitEnd(); 
    }

    public void onEndBuildStubs() {
        String internalField = ByteCodeUtil.internalName("tainted"); 
        cv.visitField(ACC_PRIVATE + ACC_TRANSIENT + ACC_FINAL,
                internalField, "Z", null, null).visitEnd();
        ByteCodeUtil.buildGetter(cv, className, internalField, "Z", ACC_FINAL, 
                                 ByteCodeUtil.internalName("isTainted"));

        /* Build export wrappers for jtaint/StringUtil */

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            buildExportWrapper(cv, "java/lang/ConditionalSpecialCasing",
                               "toLowerCaseEx", 
                               "(Ljava/lang/String;ILjava/util/Locale;)I");

            buildExportWrapper(cv, "java/lang/ConditionalSpecialCasing",
                               "toUpperCaseEx", 
                               "(Ljava/lang/String;ILjava/util/Locale;)I");

            buildExportWrapper(cv, "java/lang/ConditionalSpecialCasing",
                               "toLowerCaseCharArray", 
                               "(Ljava/lang/String;ILjava/util/Locale;)[C");

            buildExportWrapper(cv, "java/lang/ConditionalSpecialCasing",
                               "toUpperCaseCharArray", 
                               "(Ljava/lang/String;ILjava/util/Locale;)[C");
        } else /* VmInfo.version() == VmInfo.VERSION1_4 */ {
            buildExportWrapper(cv, "java/lang/Character", "toUpperCaseEx", 
                               "(C)C");
            buildExportWrapper(cv, "java/lang/Character", 
                               "toUpperCaseCharArray", "(C)[C");
        }

        addIsErrorMethod(cv);
        addTaintMethod(cv);
        addConstructor(cv);
    }

    /** By default, initialize the new boolean field 'tainted' to false for
     * all constructors.
     */
    private static class TaintedInitAdapter extends AdviceAdapter 
    {
        private int savedThis;

        public TaintedInitAdapter(MethodVisitor mv, String owner,
                                            int access, String desc) {
            super(mv, owner, access, "<init>", desc);
        }

        protected void visitSuper(final int opcode, final String owner, 
                                  final String name, final String desc)
        {
            mv.visitMethodInsn(opcode, owner, name, desc);

            savedThis = newLocal(Type.getObjectType(className));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ASTORE, savedThis);
        }

        protected void onMethodExit(final int opcode) {
            if (!superInitialized || opcode != RETURN)
                return;

            mv.visitVarInsn(ALOAD, savedThis);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, 
                              ByteCodeUtil.internalName("tainted"), "Z");
        }

        public void visitMaxs(int nStack, int nLocals) {
            super.visitMaxs(nStack + 2, nLocals);
        };
    };

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new StringAdapter(cv);
        }
    }
}
