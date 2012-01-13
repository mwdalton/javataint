/* Copyright 2009 Michael Dalton */
package jtaint;

import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

final class FilterContextAdapter extends ClassAdapter implements Opcodes
{
    private boolean instrumented;

    public FilterContextAdapter(ClassVisitor cv) {
        super(cv);
    }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        cv.visit(version, access, name, signature, superName, interfaces);

        if ((access & (ACC_INTERFACE|ACC_ANNOTATION)) != 0) 
            return;

        if (Configuration.xssFilters.containsKey(name)) 
            instrumented = true;
    }

    public boolean instrumented()        { return instrumented; }
}
