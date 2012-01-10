/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;


/* In many classes in SiegeBreaker, we perform instrumentation or analysis
 * on a particular set of methods. For example, we update taint information
 * each time append() or insert() is called on a StringBuffer. However, 
 * often-times these methods are implemented in terms of one another. 
 * For example, StringBuffer.deleteCharAt(i) may be defined as
 * StringBuffer.delete(i, i+1). We don't want to 'double-count' our
 * instrumentation -- in the above example, we could naively perform the delete
 * operation _twice_ - once when deleteCharAt() is called, and once when
 * delete() is called in the body of deleteCharAt().
 *
 * To avoid this situation we increment a simple per-object counter when
 * entering an instrumented method, and then invoke the original method. When 
 * the original method finishes, we decrement the counter. If the counter is 
 * now zero, then and only then do we execute the instrumentation code 
 * (e.g., updating taint in the case of StringBuffer). 
 *
 * Effectively our counter acts as a simple recursive lock. However, its
 * purpose is to prevent instrumentation code from being executed 
 * twice (or more) for a given instrumented method -- this 'lock' is not
 * intended to provide any form of synchronization between multiple threads.
 *
 * As a final caveat, we often instrument a hierarchy of classes which
 * together implement an interface, such as ServletOutputStream. Each
 * instrumented class will have its own lock field, which is a recipe for
 * disaster (i.e. if class A has a lock and its superclass B has a lock, then
 * A may think the object is locked when B does not or vice-versa because there
 * are two different lock fields, one used by methods defined in A, and one
 * by methods defined in B).
 *
 * To prevent this from occuring, we access lock fields solely invoking 
 * getter/setter methods via the invokevirtual instruction. Any instrumented
 * class will override the getter/setter methods in its superclasses, thus
 * ensuring that all superclasses use the same lock.
 *
 * Here is a sample instrumentation of StringBuffer.deleteCharAt(i)
 *   public StringBuffer deleteCharAt(int i) {
 *     onMethodEnter();
 *     incLock();
 *     try {
 *         realDeleteCharAt(i);
 *         if (decAndTestLock() == 0);
 *             onUnlocked();
 *         return this;
 *     } catch (Throwable th) {
 *         decAndTestLock();
 *         throw th;
 *     }
 *   }
 *
 * If an exception occurs, it is assumed that no data was updated and 
 * thus no instrumentation code should be executed, so we only decrement the
 * lock accordingly.
 */

public abstract class InstrumentationLockBuilder implements Opcodes
{
    protected final MethodVisitor mv;
    protected final int version;
    protected final String className, methodName, methodDesc;

    public InstrumentationLockBuilder(MethodVisitor mv, int version,
                                      String className, String methodName, 
                                      String methodDesc)
    {
        this.mv = mv;
        this.version = version;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    public final void build() {
        mv.visitCode();
        boolean isVoid = Type.VOID_TYPE.equals(Type.getReturnType(methodDesc));

        onMethodEnter();

        /* Increment lock */
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className,
                           ByteCodeUtil.internalName("incLock"), "()V");

        /* Now wrap the real method invocation */
        Label start = new Label(), end = new Label(), handler = new Label();
        mv.visitTryCatchBlock(start, end, handler, null);
        mv.visitLabel(start);

        mv.visitVarInsn(ALOAD, 0);
        int l = 1;
        Type[] t = Type.getArgumentTypes(methodDesc);

        for (int i = 0; i < t.length; l += t[i].getSize(), i++) 
            mv.visitVarInsn(t[i].getOpcode(ILOAD), l);

        mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                ByteCodeUtil.internalName(methodName), methodDesc);

        if (!isVoid)  
            mv.visitInsn(POP);

        /* Now decrement lock, and see if the result is zero. If so, 
         * we're now unlocked and can update the taint information
         */

        mv.visitVarInsn(ALOAD, 0); 
        mv.visitMethodInsn(INVOKEVIRTUAL, className,
                ByteCodeUtil.internalName("decAndTestLock"), 
                "()I");

        Label l0 = new Label();
        mv.visitJumpInsn(IFNE, l0);
        onUnlocked();

        mv.visitLabel(l0);
        if (version == V1_6)
            mv.visitFrame(F_SAME, 0, null, 0, null);

        /* XXX TODO All classes that use InstrumentationLockBuilder currently
         * return either void or the current class. We do not handle the
         * general case correctly at the moment.
         */
        if (!isVoid) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);
        } else {
            mv.visitInsn(RETURN);
        }

        /* Otherwise an exception occurred, decrement lock and re-throw */
        mv.visitLabel(end);
        mv.visitLabel(handler);

        if (version == V1_6)
            mv.visitFrame(F_SAME1, 0, null, 1, 
                    new Object[] { "java/lang/Throwable" });

        mv.visitVarInsn(ALOAD, 0); 
        mv.visitMethodInsn(INVOKEVIRTUAL, className, 
                ByteCodeUtil.internalName("decAndTestLock"),
                "()I");
        mv.visitInsn(POP);
        mv.visitInsn(ATHROW);

        /* We want to call the appropriate TaintUtil function with 
         * with the arguments supplied to the instrumented method.
         * However, any arugments that were originally String or 
         * StringBuilder are transformed into Taint arguments with
         * an additional 'length' argument, so at worst we require twice 
         * the number of original arguments
         */
        mv.visitMaxs(Math.max(2 * l + 1, 5), l);
        mv.visitEnd();
    }

    protected void onMethodEnter() { }

    protected abstract void onUnlocked(); 

    private static final void buildIncLock(ClassVisitor cv, String className) {
        MethodVisitor mv;
        
        mv = cv.visitMethod(ACC_PUBLIC, ByteCodeUtil.internalName("incLock"),
                            "()V", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, className,
                          ByteCodeUtil.internalName("lock"), "I");
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitFieldInsn(PUTFIELD, className, 
                          ByteCodeUtil.internalName("lock"), "I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 1);

        mv.visitEnd();
    }

    private static final void buildDecAndTestLock(ClassVisitor cv, 
                                                  String className) 
    {
        MethodVisitor mv;
        
        mv = cv.visitMethod(ACC_PUBLIC, 
                            ByteCodeUtil.internalName("decAndTestLock"),
                            "()I", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, className,
                          ByteCodeUtil.internalName("lock"), "I");
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitInsn(DUP_X1);
        mv.visitFieldInsn(PUTFIELD, className, 
                          ByteCodeUtil.internalName("lock"), "I");
        mv.visitInsn(IRETURN);
        mv.visitMaxs(3, 1);

        mv.visitEnd();
    }


    public static final void visitEnd(ClassVisitor cv, String className) {
        cv.visitField(ACC_PRIVATE + ACC_TRANSIENT,
                      ByteCodeUtil.internalName("lock"), 
                      "I", null, null).visitEnd();

        buildIncLock(cv, className);
        buildDecAndTestLock(cv, className);
    }
}
