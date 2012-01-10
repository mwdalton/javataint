/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ConnectionAdapter extends ClassAdapter implements Opcodes
{
    public ConnectionAdapter(ClassVisitor cv) { super (cv); }

    public void visitEnd() {
        cv.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, 
                       ByteCodeUtil.internalName("sqlValidator"),
                       "()Lcom/siegebrk/SqlValidator;", null, null).visitEnd();
        cv.visitEnd();
    }

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new ConnectionAdapter(cv);
        }
    }
}
