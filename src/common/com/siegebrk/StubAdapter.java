/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.BitSet;
import java.util.List;

/* XXX This handles all cases except when a subclass creates a less permissive
 * 'overriding' method (e.g., private) that cannot be invoked by other code.
 * In that case, we instrument the private method, but not the publicly
 * accessible 'overriden' method in the superclass, which could break things.
 * This gets really thorny if the overriding method is protected or default
 * access rather than private, so we ignore it for now since it shouldn't occur
 * in practice, at least for the classes we are instrumenting in the JDK.
 */

/** A simple adapter that, given a list of method prototypes, adds these methods
 * to the existing class if they are not found. Any methods that are added
 * merely call the superclass with the same method name and arguments. Useful
 * when all instrumentation must be performed in the current class, and some
 * methods are defined only in a superclass.
 */

public class StubAdapter extends ClassAdapter implements Opcodes
{
    List stubList;
    BitSet found;
    String superName;

    public StubAdapter(ClassVisitor cv, List stubList) {
        super(cv);

        if (stubList == null)
            return;

        this.stubList = stubList;
        this.found = new BitSet(stubList.size());
    }

    public void visit(int version, int access, String name,
                                String signature, String superName,
                                String[] interfaces) {
        this.superName = superName;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    private void buildStub(MethodDecl m) {
        String name = m.name(),
               desc = m.type();
        int    acc = m.access();
        Type[] args = Type.getArgumentTypes(desc);
        Type ret = Type.getReturnType(desc);

        /* We want any subclasses to visit the generated stub, so we invoke 
         * this.visitMethod() (which should be overridden by relevant
         * subclasses).
         */
        MethodVisitor mv = visitMethod(acc, name, desc, null, null);

        if (mv == null) return;

        mv.visitCode();
       
        int l = 1;
        mv.visitVarInsn(ALOAD, 0);
        
        for (int i = 0; i < args.length; l += args[i].getSize(), i++) 
            mv.visitVarInsn(args[i].getOpcode(ILOAD), l);

        mv.visitMethodInsn(INVOKESPECIAL, superName, name, desc);
        mv.visitInsn(ret.getOpcode(IRETURN));

        mv.visitMaxs(Math.max(l, ret.getSize()), l);
        mv.visitEnd();
    }

    public MethodVisitor visitMethod(int access, String name, String desc, 
                                     String signature, String[] exceptions) 
    {
        MethodDecl m = new MethodDecl(access, name, desc);

        if (stubList != null) {
            int i = stubList.indexOf(m);

            if (i != -1)
                found.set(i);
        }
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    public final void visitEnd() {
        onBeginBuildStubs();

        if (stubList != null) {
            int len = stubList.size();
            for (int i = found.nextClearBit(0); i >= 0 && i < len; 
                     i = found.nextClearBit(i+1)) 
                buildStub((MethodDecl) stubList.get(i));
        }

        onEndBuildStubs();
        cv.visitEnd();
    }

    protected void onBeginBuildStubs() { }

    protected void onEndBuildStubs() { }
}
