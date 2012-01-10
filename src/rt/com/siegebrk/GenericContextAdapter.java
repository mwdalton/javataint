/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class GenericContextAdapter extends ClassAdapter implements Opcodes
{
    private String className;
    private boolean skip;
    private final Map methods;
    private Map instrumentedMethods;

    public GenericContextAdapter(ClassVisitor cv, Map methods) {
        super(cv);
        this.methods = methods;
    }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        /* We cannot insert instrumentation into class that have no method
         * bodies. We also do not instrument any Enums 
         */
        skip = (access & (ACC_INTERFACE|ACC_ANNOTATION|ACC_ENUM)) != 0;
        className = name;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(final int access, final String name, 
            final String desc, String signature, 
            String[] exceptions) 
    {
        final MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
                                                exceptions);
        if (skip) return mv;

        /* Subtle point: Two MethodDecls are not considered equal if one is
         * abstract and the other is not
         */
        MethodDecl md = new MethodDecl(access, name, desc);

        if (methods.containsKey(md)) {
            Klass k = (Klass) methods.get(md);
            if (k.isExact() && !className.equals(k.internalName()))
                return mv;
            if (instrumentedMethods == null)
                instrumentedMethods = new HashMap();
            instrumentedMethods.put(md, k);
        }
        return mv;
    }

    public Map instrumentedMethods() { return instrumentedMethods; }

    public boolean instrumented() { return instrumentedMethods != null; }
}
