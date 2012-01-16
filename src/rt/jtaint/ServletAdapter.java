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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.SimpleAdviceAdapter;

final class ServletAdapter extends ClassAdapter implements Opcodes
{
    private final Map instrumentedMethods;
    private final Klass[] servletKlasses;
    private String className;
    private int version;

    public ServletAdapter(ClassVisitor cv, ServletContextAdapter sca) {
        super(cv);
        instrumentedMethods = sca.instrumentedMethods();
        Set s = new HashSet(instrumentedMethods.values());
        servletKlasses = (Klass[]) s.toArray(new Klass[s.size()]);
    }

    public void visit(int version, int access, String name, String signature, 
                      String superName, String[] interfaces) 
    {
        cv.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.version = version;
    }

    public MethodVisitor visitMethod(final int access, final String name, 
                                     final String desc, String signature, 
                                     String[] exceptions) 
    {  
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, 
                                          exceptions);

        if ("<init>".equals(name)) 
            return new CheckClassInitAdapter(servletKlasses, mv, 
                                             className, access, name, desc);

        Klass k = (Klass) instrumentedMethods.get(new MethodDecl(access, name, 
                                                                 desc));

        if (k == null || (k.isExact() && !className.equals(k.internalName())))
            return mv;

        if (k == ServletContextAdapter.HTTPSERVLETREQUEST 
                && "getPathTranslated".equals(name)) {
            buildGetPathTranslatedWrapper(mv); 
        } else if (k == ServletContextAdapter.SERVLET 
                   || k == ServletContextAdapter.HTTPSERVLET) {
            buildServletWrapper(mv, k, name, desc);
        } else if (k == ServletContextAdapter.SERVLETRESPONSE) {
            buildHtmlValidatorWrapper(mv, name, desc);
        } else {
            buildTaintedReturnWrapper(mv, k, access, name, desc);
        }

        return cv.visitMethod(
                ByteCodeUtil.inheritAccessFlags(access) + ACC_PRIVATE,
                ByteCodeUtil.internalName(name), desc, signature, exceptions);
    }


    public void visitEnd() {
        Klass[] k = servletKlasses;
        for (int i = 0; i < k.length; i++) 
            cv.visitField(ACC_PRIVATE + ACC_FINAL + ACC_TRANSIENT,
                          ByteCodeUtil.internalName("is" + k[i].simpleName()),
                          "Z", null, null).visitEnd();
        cv.visitEnd();
    }

    /* Equivalent to the following Java code:
     * (Where T is String, Hashtable, StringBuffer, etc)
     * public T fn(arg1...argN) {
     *     T retval = realfn(arg1...argN);
     *     if (isServlet)
     *         retval = jtaint.StringUtil.toTainted(retval);
     *     return retval;
     * }
     * XXX TODO cache this result and redo TaintedEnumeration
     *
     * XXX This is very tricky. So in general some classes need access to
     * org.objectweb.asm.commons.AdviceAdapter. However, this class requires 
     * org.object.asm.commons.LocalVariableSorer, which requires ClassReader
     * to be called with ClassReader.EXPAND_FRAMES. This is all well and good,
     * but it means that _any_ method not generated internally by JavaTaint
     * _must_ only call visitFrame() with F_NEW frames. This is because
     * org.objectweb.asm.MethodWriter requires any method with F_NEW frames to
     * have only frames of type F_NEW, and not to mix compressed and 
     * uncompressed frames. Fortunately for us, the only time we call 
     * visitFrame, we generate our own methods and can use compressed frames
     * with impugnity. However, in the future this may change. You are warned.
     */

    private void buildTaintedReturnWrapper(MethodVisitor mv, Klass k, 
                                           int access, String name, String desc)
    {
        mv.visitCode();
        Type[] args = Type.getArgumentTypes(desc);
        Type ret = Type.getReturnType(desc);
        boolean isStatic = (access & ACC_STATIC) != 0;
        int l = 0;

        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            l = 1;
        }
        
        for (int i = 0; i < args.length; l += args[i].getSize(), i++)
            mv.visitVarInsn(args[i].getOpcode(ILOAD), l);
        mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKESPECIAL, className, 
                           ByteCodeUtil.internalName(name), desc);

        String taintDesc = Type.getMethodDescriptor(ret, new Type[] { ret });

        if (k.isExact()) {
            /* We already know that we need to wrap...no runtime check needed */
            mv.visitMethodInsn(INVOKESTATIC, "jtaint/StringUtil",
                    "toTainted",taintDesc);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(l, l);
            mv.visitEnd();
            return;
        }

        /* Now taint the return type, if we need to */
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                         ByteCodeUtil.internalName("is"+k.simpleName()), "Z");
        Label l0 = new Label();
        mv.visitJumpInsn(IFEQ, l0);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/StringUtil",
                           "toTainted",taintDesc);

        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { ret.getInternalName() });
        mv.visitInsn(ARETURN);
        mv.visitMaxs(Math.max(l, 2), l);
        mv.visitEnd();
    }

    /* Equivalent to the following Java code
     * public void service(ServletRequest req, ServletResponse res) {
     *     try {
     *         if (isServlet)
     *             preService(req.getParameterMap(), req.getRemoteHost(),
     *                        req.getRemoteAddr());
     *         real_service(req, res);
     *         if (isServlet)
     *             postService();
     *         return;
     *     } catch (Throwable th) {
     *         if (isServlet)
     *             postService();
     *         throw th;
     *     }
     * }
     */         
     
    private void buildServletWrapper(MethodVisitor mv, Klass k, String name, 
                                     String desc) 
    {
        mv.visitCode();

        Label start = new Label(), end = new Label(), handler = new Label();
        mv.visitTryCatchBlock(start, end, handler, null);
        mv.visitLabel(start);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("is" + k.simpleName()), 
                          "Z");
        Label l0 = new Label();
        mv.visitJumpInsn(IFEQ, l0);

        Type[] t = Type.getArgumentTypes(desc);

        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, t[0].getInternalName(), 
                "getParameterMap", "()Ljava/util/Map;");

        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, t[0].getInternalName(),
                           "getRemoteHost", "()Ljava/lang/String;"); 

        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, t[0].getInternalName(), 
                           "getRemoteAddr", "()Ljava/lang/String;"); 

        mv.visitMethodInsn(INVOKESTATIC, "jtaint/HttpUtil",
                           "preService", 
                           "(Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V");
        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);

        mv.visitVarInsn(ALOAD, 0);
        int l = 1;

        for (int i = 0; i < t.length; l += t[i].getSize(), i++)
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);
        mv.visitMethodInsn(INVOKESPECIAL, className, 
                           ByteCodeUtil.internalName(name), desc);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("is" + k.simpleName()), 
                          "Z");
        Label l1 = new Label();
        mv.visitJumpInsn(IFEQ, l1);

        mv.visitMethodInsn(INVOKESTATIC, "jtaint/HttpUtil",
                          "postService", "()V");
        mv.visitLabel(l1);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(RETURN);
        mv.visitLabel(end);

        mv.visitLabel(handler);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "java/lang/Throwable" });

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("is" + k.simpleName()), 
                          "Z");
        Label l2 = new Label();
        mv.visitJumpInsn(IFEQ, l2);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/HttpUtil",
                           "postService", "()V");
        mv.visitLabel(l2);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "java/lang/Throwable" });

        mv.visitInsn(ATHROW);
        mv.visitMaxs(Math.max(l, 3), l);
        mv.visitEnd();
    }

    /* Equivalent to the following java Code
     *
     * public String getPathTranslated() {
     *     String s = realGetPathTranslated();
     *     if (isHttpServletRequest)
     *        s = jtaint.HttpUtil.getPathTranslated(s, this);
     *     return s;
     * }
     */

    private void buildGetPathTranslatedWrapper(MethodVisitor mv) {
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, className, 
                           ByteCodeUtil.internalName("getPathTranslated"),
                           "()Ljava/lang/String;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("isHttpServletRequest"), 
                          "Z");
        Label l0 = new Label();
        mv.visitJumpInsn(IFEQ, l0);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/HttpUtil",
                           "getPathTranslated",
                           "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");

        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "java/lang/String" });
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private void buildHtmlValidatorWrapper(MethodVisitor mv, String name, 
                                           String desc) 
    {
        mv.visitCode();

        Type[] t = Type.getArgumentTypes(desc);
        Type r   = Type.getReturnType(desc);

        mv.visitVarInsn(ALOAD, 0);
        int l    = 1;

        for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);
        mv.visitMethodInsn(INVOKESPECIAL, className,
                           ByteCodeUtil.internalName(name), desc);

        Label l0 = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, l0);
       
        mv.visitInsn(DUP); 
        mv.visitMethodInsn(INVOKEVIRTUAL, r.getInternalName(), 
                           ByteCodeUtil.internalName("getHtmlValidator"),
                           "()Ljtaint/HtmlValidator;");
        mv.visitJumpInsn(IFNONNULL, l0);

        /* Okay, we have a valid print object and null html validator, time 
         * to initialize...
         */

        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/HttpUtil",
                           "getHtmlValidator", 
                           "(Ljava/lang/Object;)Ljtaint/HtmlValidator;");
        mv.visitMethodInsn(INVOKEVIRTUAL, r.getInternalName(), 
                           ByteCodeUtil.internalName("setHtmlValidator"),
                           "(Ljtaint/HtmlValidator;)V");

        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { r.getInternalName() });
        mv.visitInsn(ARETURN);

        mv.visitMaxs(Math.max(l, 3), l);
        mv.visitEnd();
    }

    private static final class CheckClassInitAdapter extends SimpleAdviceAdapter
    {
        private final Klass[] k;

        public CheckClassInitAdapter(Klass[] k, MethodVisitor mv, 
                                     String owner, int access, String name, 
                                     String desc) 
        {
            super(mv, owner, access, name, desc); 
            this.k = k;
        }

        private void prologue() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object",
                               "getClass", "()Ljava/lang/Class;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class",
                               "getClassLoader", 
                               "()Ljava/lang/ClassLoader;");
        }

        /* Very important: JVM Specification 5.3.5 guarantees that 
         * for a given class C, all superclasses and superinterfaces of
         * C are loaded before C is.
         */
        
        private void checkAssignable(Klass k) {
            mv.visitInsn(DUP2);
            mv.visitLdcInsn(k.name());
            mv.visitMethodInsn(INVOKESTATIC, "jtaint/ClassUtil",
                               "isInstance", 
                               "(Ljava/lang/Object;Ljava/lang/ClassLoader;Ljava/lang/String;)Z");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(SWAP);
            mv.visitFieldInsn(PUTFIELD, className, 
                    ByteCodeUtil.internalName("is" + k.simpleName()), "Z");
        }

        public void onMethodEnter() {
            if (!superInitialized || k.length == 0) return;

            prologue();

            for (int i = 0; i < k.length; i++) 
                    checkAssignable(k[i]);
            mv.visitInsn(POP2);
        }

        /* When instrumenting a constructor, we are not guaranteed that the
         * stack will be empty after the call to super(). Thus, up to nStack
         * elements may be present on the stack when our instrumentation
         * code is invoked, so we adjust the total in visitMaxs() accordingly.
         */
        public void visitMaxs(int nStack, int nLocals) {
            if (!superInitialized || k.length == 0)
                mv.visitMaxs(nStack, nLocals);
            else
                mv.visitMaxs(nStack + 5, nLocals);
        }
    }
}
