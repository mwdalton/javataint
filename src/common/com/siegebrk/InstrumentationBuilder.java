/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import org.objectweb.asm.ClassVisitor;

public interface InstrumentationBuilder
{
    public ClassVisitor build(ClassVisitor cv);
}
