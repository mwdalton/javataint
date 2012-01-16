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

import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.SimpleAdviceAdapter;

final class SqlAdapter extends ClassAdapter implements Opcodes
{
    private final Map instrumentedMethods;
    private final boolean isConnection;
    private String className;
    private int version;

    public SqlAdapter(ClassVisitor cv, SqlContextAdapter sqa) {
        super(cv);
        Map im = sqa.instrumentedMethods();
        isConnection = im.containsValue(SqlContextAdapter.CONNECTION);
        instrumentedMethods = im;
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

        if ("<init>".equals(name)) {
           if (isConnection)
               mv =  new LockObjectInitAdapter(mv,className,access,name,desc);
           return mv;
        }

        MethodDecl md = new MethodDecl(access, name, desc);
        final Klass k = (Klass) instrumentedMethods.get(md);

        if (k == null || (k.isExact() && !className.equals(k.internalName())))
            return mv;

        return new SimpleAdviceAdapter(mv, className, access, name, desc) {
            protected void onMethodEnter() {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "jtaint/SqlUtil",
                                   "validateSql" + k.simpleName(),
                                   "(Ljava/lang/String;Ljava/lang/Object;)V");
            }
            public void visitMaxs(int nStack, int nLocals) {
                mv.visitMaxs(Math.max(2, nStack), nLocals);
            }
        };
    }


    public void visitEnd() {
        if (isConnection) {
            cv.visitField(ACC_PRIVATE + ACC_FINAL + ACC_TRANSIENT,
                    ByteCodeUtil.internalName("lockObj"),
                    "Ljava/lang/Object;", null, null).visitEnd();
            cv.visitField(ACC_PRIVATE + ACC_TRANSIENT,
                    ByteCodeUtil.internalName("validator"),
                    "Ljtaint/SqlValidator;", null, null).visitEnd();
            addSqlValidator();
        }

        cv.visitEnd();
    }

    /* XXX This routine uses double checked locking - not safe in Java 1.4.
     * This very subtle, and depends on Java 1.5's memory model guarantees
     * concerning possible field values ('no writes out of thin air'), 
     * and the semantics of final. The first requirement ensures that another 
     * thread will see either validator == null or validator == the correct sql 
     * validator object. The second requirement ensures that another thread
     * will see a fully initialized SqlValidator object if validator is 
     * non-null, because all classes that implement SqlValidator must be 
     * immutable and contain only final-qualified instance/static fields.
     *
     * This approach does not require 'validator' to be declared as a volatile
     * field, and thus is considerably more subtle than standard double-checked
     * locking. We can avoid the volatile qualification because all of our 
     * SqlValidator objects are immutable -- and the semantics of final fields 
     * guarantee that the constructed SqlValidator object will be visible to any
     * other thread with up-to-date values without any further synchronization 
     * (and thus the instructions initializing the object will not be reordered
     * with respect to the initialization of the validator field).
     *
     * See comment below for an explanation of the Thread.holdsLock call, which
     * is needed to break possible infinite recursion.
     *
     * References: Java Programming Language Specification 
     *             17.4.4 (Default value init happens-before thread start) 
     *             17.4.{7,8} ('Out of thin air' values forbidden)
     *             17.5 (Final field semantics)
     *             17.7 (Atomicity of reference reads/writes)
     *
     * Equivalent to the following Java code:
     *
     * public sqlValidator() {
     *     if (validator != null) 
     *         return validator;
     *
     *     if (Thread.holdsLock(lockObj))
     *         return EmptySqlValidator.INSTANCE;
     *
     *     synchronized(lockObj) {
     *         if (validator != null) 
     *             return validator;
     *
     *         validator = SqlUtil.getSqlValidator(this);
     *         return validator;
     *     }
     * }
     */
     
    private void addSqlValidator() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC
                                      //[ifJava4]
                                      + ACC_SYNCHRONIZED
                                      //[fiJava4] 
                                      ,
                                      ByteCodeUtil.internalName("sqlValidator"),
                                      "()Ljtaint/SqlValidator;", null,
                                       null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("validator"), 
                          "Ljtaint/SqlValidator;");
        mv.visitInsn(DUP);
       
        Label l0 = new Label();
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitInsn(ARETURN);

        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "jtaint/SqlValidator" });

        mv.visitInsn(POP);

        /* XXX This is an industrial-sized barrel of fun. We have to avoid
         * infinite recursion here when initializing the validator field --
         * i.e. when sqlValidator is called for the first time. In this case,
         * what can happen is:
         * connection.sqlValidator -> jtaint.SqlUtil.getSqlValidator
         * -> Connection.getDatabaseMetadata 
         * -> Connection.sqlValidator ->
         * -> jtaint.SqlUtil.getSqlValidator
         * -> Connection.getDatabaseMetadata
         * ... (repeat last three steps forever), where -> denotes a method call
         * So if we ever find that we already own the lock that we are about
         * to acquire, then we return an EmptySqlValidator to break
         * the recursion(Note that once the recursion unwinds, the validator
         * field will be correctly set, so we will begin returning the correct
         * sql validator. This corner case applies only during initialization).
         */

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className,
                          ByteCodeUtil.internalName("lockObj"), 
                          "Ljava/lang/Object;");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread",
                           "holdsLock", "(Ljava/lang/Object;)Z");
        Label l1 = new Label();
        mv.visitJumpInsn(IFEQ, l1);

        /* Break the recursion */
        mv.visitFieldInsn(GETSTATIC, "jtaint/EmptySqlValidator",
                          "INSTANCE", "Ljtaint/EmptySqlValidator;");
        mv.visitInsn(ARETURN);


        /* No recursion -- acquire the lock and initialize our field */
        mv.visitLabel(l1);
        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                          new Object[] { "java/lang/Object" });
        mv.visitInsn(DUP);
        mv.visitInsn(MONITORENTER);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, 
                          ByteCodeUtil.internalName("validator"), 
                          "Ljtaint/SqlValidator;");
        mv.visitInsn(DUP);
        Label l2 = new Label();
        mv.visitJumpInsn(IFNULL, l2);
        mv.visitInsn(SWAP);
        mv.visitInsn(MONITOREXIT);
        mv.visitInsn(ARETURN);

        mv.visitLabel(l2);
        if (version == V1_6)
            mv.visitFrame(F_FULL, 1, new Object[] { className }, 2, 
                          new Object[] { "java/lang/Object",          
                                         "jtaint/SqlValidator" });
        mv.visitInsn(POP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/SqlUtil", "getSqlValidator", 
                           "(Ljava/lang/Object;)Ljtaint/SqlValidator;");
        mv.visitInsn(DUP_X1);
        mv.visitFieldInsn(PUTFIELD, className, 
                          ByteCodeUtil.internalName("validator"), 
                          "Ljtaint/SqlValidator;");
        mv.visitInsn(SWAP);
        mv.visitInsn(MONITOREXIT);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(4, 1); 
        mv.visitEnd();
    }

    private static final class LockObjectInitAdapter extends SimpleAdviceAdapter
    {
        public LockObjectInitAdapter(MethodVisitor mv, String owner, int access,
                                     String name, String desc) 
        {
            super(mv, owner, access, name, desc);
        }

        public void onMethodEnter() {
            if (!superInitialized) 
                return;

            mv.visitTypeInsn(NEW, "java/lang/Object");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", 
                               "()V");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(SWAP);
            mv.visitFieldInsn(PUTFIELD, className, 
                              ByteCodeUtil.internalName("lockObj"),
                              "Ljava/lang/Object;");
        }

        public void visitMaxs(int nStack, int nLocals) {
            if (!superInitialized)
                mv.visitMaxs(nStack, nLocals);
            else
                mv.visitMaxs(nStack+ 2, nLocals);
        }
    }
}
