/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.nio.ByteBuffer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/* This class performs Dynamic Bytecode Instrumentation (DBI) on all classes 
 * loaded at runtime in the JVM. This class is called by various methods in
 * java.lang.ClassLoader by code inserted by com.siegebrk.ClassLoaderAdapter.
 *
 * We are interested in instrumenting classes that implement various java.sql
 * interfaces (Statement, Connection, RowSet), and various servlet interfaces
 * (javax.servlet.ServletRequest, javax.servlet.http.HttpServletRequest,
 * javax.servlet.http.Cookie, javax.servlet.http.HttpUtils).
 *
 * However, we cannot tell if a particular class implements these interfaces
 * at load time, because the superclasses and superinterfaces may not have
 * been loaded. For example, a superclass may implement one of the above
 * interfaces, or this class may implement a subinterface of one of the above
 * interfaces. 
 *
 * For this reason, we instrument any method that has the same method 
 * descriptor as the methods we wish to perform security checks on. At runtime,
 * we then perform checks on the class to ensure that our security checks
 * occur only when the class actually implements one of the above interfaces.
 *
 * For SQL classes, java.sql is already present in rt.jar, and guaranteed to
 * be available to the bootstrap loader. Thus all runtime SQL checks are 
 * 'instanceof' checks, ensuring that the current object is an instance of
 * one of the above java.sql interfaces. 
 *
 * For Servlet classes, javax.servlet is not present in rt.jar. The web
 * application server must provide the various javax.servlet interfaces. 
 * Thus, we cannot directly reference javax.servlet interfaces because
 * com.siegebrk.* classes are loaded by the Java bootstrap classloader, and
 * thus cannot directly reference a class loaded by the java web application's
 * classloaders (if any). Furthermore, if this tool is used on a java
 * program that is not a servlet, then there may be no javax.servlet package
 * present at all. 
 *
 * As a consequence, we must use reflection to dynamically probe for the
 * existence of the above servlet interfaces, and then check if the current
 * class implements the interface. We do this by adding a private 
 * final boolean to cache the result of this check for each of the above 
 * classes. 
 */

public final class InstrumentationUtils
{
    private static byte[] copyOf(byte[] b, int off, int len) {
        if (off == 0 && len == b.length)
            return b;
        byte[] br = new byte[len];
        System.arraycopy(b, off, br, 0, len);
        return br;
    }

    public static byte[] instrument(byte[] b, int off, int len) {
        try {
            ClassReader cr = new ClassReader(b, off, len);

            EmptyClassVisitor empty   = new EmptyClassVisitor();
            FilterContextAdapter ftc  = new FilterContextAdapter(empty);
            SqlContextAdapter sqc     = new SqlContextAdapter(ftc);
            ServletContextAdapter svc = new ServletContextAdapter(sqc);
            XssContextAdapter xsa     = new XssContextAdapter(svc);

            cr.accept(xsa, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG +
                           ClassReader.SKIP_FRAMES);
            if (!xsa.instrumented() && !sqc.instrumented() 
                    && !svc.instrumented() && !ftc.instrumented())
                return copyOf(b, off, len);
           
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor cv = cw;

            if (ftc.instrumented())
                cv = new FilterAdapter(cv, ftc);
            if (sqc.instrumented())
                cv = new SqlAdapter(cv, sqc);
            if (svc.instrumented())
                cv = new ServletAdapter(cv, svc);
            if (xsa.instrumented())
                cv = new XssAdapter(cv, xsa);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable th) {
            Log.error(th);
            return copyOf(b, off, len);
        }
    }

    /* XXX We assume (as do Sun's native classes), that offset 
     * is equal to b.position(). and len is b.remaining(). If we're
     * wrong, log it and break out. This assumption holds in 
     * JDK1.5/1.6, and does not occur in JDK 1.4.
     */
    //[ifJava5+]
    public static ByteBuffer instrument(ByteBuffer bb, int off, int len) {
        try {

            if (off != bb.position() || len != bb.remaining()) 
                throw new Throwable("Unexpected ByteBuffer");

            byte[] b = new byte[len];
            bb.duplicate().get(b, 0, len); 

            byte[] instb = instrument(b, 0, len);
            if (b == instb)
                return bb; 

            ByteBuffer ret = ByteBuffer.allocateDirect(instb.length);
            ret.put(instb);
            ret.flip();
            return ret;
        } catch (Throwable th) {
            Log.error(th);
            return bb;
        }
    }
    //[fiJava5+]
}
