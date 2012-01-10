/* Copyright 2009 Michael Dalton */
package com.siegebrk;

/** Klass represents a Java class that may be instrumented at runtime. We
 *  identify classes by their full name (for example, java.lang.String).
 *  We further divide Klasses into two types: exact, which are only 
 *  instrumented if a loaded class exactly matches the name of our Klass object,
 *  and inexact, where we instrument any class that has a method with the
 *  type name and signature of a security-relevant method in our Klass.
 *  In the latter case, we ensure that such instrumentation performs runtime
 *  type checks so that any security operations occur only if the class is
 *  assignment compatible with Klass's name. 
 *
 *  Exact classes are used when we know the user will not subclass or override
 *  the class in question (for example, when instrumenting 
 *  java.servlet.http.HttpUtils, where we instrument only static methods).
 *  In other cases, for safety reasons, we must instrument arbitrary classes
 *  that may be subclassing a security-relevant class, determining only at
 *  runtime if they are truly a subclass or just a false positive. We can't
 *  tell at load time as the JVM may not have loaded their parent class 
 *  before the defineClass method is called in java.lang.ClassLoader.
 */
 
final class Klass
{
    private final String name;
    private final boolean isExact; 

    public Klass(String name, boolean isExact) {
        this.name = name;
        this.isExact = isExact;
    }

    public Klass(String name) {
        this(name, false);
    }

    public boolean isExact() { return isExact; }

    public String name() { return name; }

    public String internalName() {
        return name.replace('.', '/');
    }

    public String simpleName() {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public String toString() { return name; }
}
