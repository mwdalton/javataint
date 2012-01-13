/* Copyright 2009 Michael Dalton */
package jtaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class RemappingMiscClassAdapter extends RemappingClassAdapter
        implements Opcodes
{
    private static final Map logMap;
    private static final Map xssMap;

    static {
        HashMap h = new HashMap();
        h.put("jtaint/Log", "jtaint/OrigLog");
        logMap = h;

        h = new HashMap();
        h.put("jtaint/HtmlValidator", "jtaint/OrigHtmlValidator");
        xssMap = h;
    }

    private Map typeMap;

    public RemappingMiscClassAdapter(final ClassVisitor cv, final Map typeMap) {
        super(cv, new Remapper() {
            public String map(String typeName) {
                if (typeMap.containsKey(typeName))
                    return (String) typeMap.get(typeName);
               return typeName;
           }
        });
    }

    /* Do not allow these remapped classes to be final. We may override methods
     * for testing purposes
     */
    public void visit(int version, int access, String name, String signature, 
                     String superName, String[] interfaces)
    {
        super.visit(version, access & ~ACC_FINAL, name, signature, superName, 
                    interfaces);
    }

    private static void remapClass(String baseClass, String outputName, 
                                   Map typeMap, File baseDir) 
        throws IOException
    {
        ClassReader cr = new ClassReader(baseClass);
        ClassWriter cw = new ClassWriter(0);

        cr.accept(new RemappingMiscClassAdapter(cw, typeMap), 0);

        File f = new File(baseDir, outputName);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(cw.toByteArray());
        fos.close();
    }

    private static void usage() {
        System.err.println("Usage: jt-remap <log dir> <xss dir>");
        System.err.println("Outputs remapped misc classes to the "
                           + "directory specified by dir");
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2)
            usage();
        File logDir = new File(args[0]);
        File xssDir = new File(args[1]);

        remapClass("jtaint.Log", "OrigLog.class", logMap, logDir);
        remapClass("jtaint.HtmlValidator", "OrigHtmlValidator.class",
                    xssMap, xssDir);
    }
}
