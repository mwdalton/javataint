/* Copyright 2009 Michael Dalton */
package jtaint;

public final class ClassUtil
{
    public static boolean isInstance(Object o, ClassLoader cl, 
                                     String className) {
        if (cl == null) 
            return false;

        Class c = cl.@internal@findLoadedClass(className);

        if (c == null)
            return false;
        return c.isInstance(o);
    }
}
