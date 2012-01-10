/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import org.objectweb.asm.Opcodes;

public class MethodDecl implements Opcodes
{
    private final int acc;
    private final String name;
    private final String desc;

    public MethodDecl(int acc, String name, String desc) {
        this.acc = acc;
        this.name = name;
        this.desc = desc;
    }

    public int access() { return acc; }

    public String name() { return name; }

    public String type() { return desc; }

    /* We emulate java member equivalence semantics -- two methods are 
     * equivalent if they have the same simple name and the same type 
     * descriptor. Because we do not currently handle private overriding
     * methods, we also require that two equivalent objects must have the
     * same accessibility (ACC_{PRIVATE,PUBLIC,PROTECTED,STATIC} bits are
     * the same). Note that we also require the two return types to be
     * equivalent, as these are included in the method descriptor. The JVM
     * also requires this, and later versions of Java(1.5 and above) that
     * support covariant return types create bridge methods to maintain
     * the illusion that two methods have the same type iff their argument
     * and return type descriptors are the same.
     */

    private static final int acc_flags =
        ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_ABSTRACT;
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        else if (!(obj instanceof MethodDecl))
            return false;

        MethodDecl m = (MethodDecl) obj;
        if (name.equals(m.name) && desc.equals(m.desc)
                && (acc & acc_flags) == (m.acc & acc_flags))
            return true;

        return false;
    }

    public int hashCode() {
        return name.hashCode() +  desc.hashCode();
    }
}
