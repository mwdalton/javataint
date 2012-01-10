/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class XssContextAdapter extends ClassAdapter implements Opcodes
{
    public static final List outputStreamMethods;
    public static final List servletOutputStreamMethods;
    public static final List printWriterMethods;

    static {
        ArrayList l = new ArrayList();
     
        /* java.io.OutputStream */
        l.add(new MethodDecl(ACC_PUBLIC, "write", "([B)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "write", "([BII)V"));

        l.trimToSize();
        outputStreamMethods = l;

        /* javax.servlet.ServletOutputStream */
        l = new ArrayList();
        l.addAll(outputStreamMethods);
       
        /* This method is abstract in java.io.OutputStream -- it's first
         * non-abstract definition is in javax.servlet.ServletOutputStream */
        l.add(new MethodDecl(ACC_PUBLIC, "write", "(I)V"));

        l.add(new MethodDecl(ACC_PUBLIC, "print", "(Z)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(D)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(F)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(I)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(J)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(Ljava/lang/String;)V"));

        l.add(new MethodDecl(ACC_PUBLIC, "println", "()V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(Z)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(D)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(F)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(I)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(J)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(Ljava/lang/String;)V"));

        l.trimToSize();
        servletOutputStreamMethods = l;

        /* java.io.PrintWriter */
        l = new ArrayList();

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            l.add(new MethodDecl(ACC_PUBLIC, "append", 
                        "(C)Ljava/io/PrintWriter;"));
            l.add(new MethodDecl(ACC_PUBLIC, "append", 
                        "(Ljava/lang/CharSequence;)Ljava/io/PrintWriter;"));
            l.add(new MethodDecl(ACC_PUBLIC, "append", 
                        "(Ljava/lang/CharSequence;II)Ljava/io/PrintWriter;"));
        }

        l.add(new MethodDecl(ACC_PUBLIC, "print", "(Z)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "([C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(D)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(F)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(I)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(J)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(Ljava/lang/Object;)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "print", "(Ljava/lang/String;)V"));

        l.add(new MethodDecl(ACC_PUBLIC, "println", "()V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(Z)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "([C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(D)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(F)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(I)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(J)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(Ljava/lang/Object;)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "println", "(Ljava/lang/String;)V"));
       

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            l.add(new MethodDecl(ACC_PUBLIC, "format",
                        "(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;"));
            l.add(new MethodDecl(ACC_PUBLIC, "format",
                        "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;"));
            l.add(new MethodDecl(ACC_PUBLIC, "printf",
                        "(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;"));
            l.add(new MethodDecl(ACC_PUBLIC, "printf",
                        "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;"));
        }

        l.add(new MethodDecl(ACC_PUBLIC, "write", "([C)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "write", "([CII)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "write", "(I)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "write", "(Ljava/lang/String;)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "write", "(Ljava/lang/String;II)V"));

        l.trimToSize();
        printWriterMethods = l;
    }

    private boolean skip;
    private List instrumentedMethods;

    public XssContextAdapter(ClassVisitor cv) {
        super(cv);
    }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        /* We cannot insert instrumentation into class that have no method
         * bodies. We also do not instrument any Enums 
         */
        skip = (access & (ACC_INTERFACE|ACC_ANNOTATION|ACC_ENUM)) != 0;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(final int access, final String name, 
            final String desc, String signature, 
            String[] exceptions) 
    {
        final MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
                                                exceptions);
        if (skip) return mv;

        MethodDecl md = new MethodDecl(access, name, desc);

        if (servletOutputStreamMethods.contains(md) || 
                printWriterMethods.contains(md))
        {
            if (instrumentedMethods == null)
                instrumentedMethods = new ArrayList();
            instrumentedMethods.add(md);
        }
        return mv;
    }

    public List instrumentedMethods() { return instrumentedMethods; }

    public boolean instrumented()     { return instrumentedMethods != null; }
}
