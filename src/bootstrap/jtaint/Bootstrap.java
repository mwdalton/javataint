/* Copyright 2009 Michael Dalton */
package jtaint;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class Bootstrap 
{
    private static Map classMap = new HashMap();
    private static boolean debug;

    private static void addInstrumentation(String className, 
                                           InstrumentationBuilder b)
        throws IOException
    {
        ClassReader cr = new ClassReader(className);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv;

        if (debug) 
            cv = b.build(new CheckClassAdapter(cw));
        else
            cv = b.build(cw);

        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        classMap.put(className.replace('.','/') + ".class", cw.toByteArray());

        if (debug) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false,
                                     pw);
            pw.flush();
            if (sw.toString().length() > 0)
                throw new IllegalArgumentException(sw.toString());
        }
    }

    private static void addAnalysis(String className, AnalysisBuilder b)
        throws IOException
    {
        ClassReader cr = new ClassReader(className);
        ClassVisitor cv = b.build();
        cr.accept(cv, ClassReader.EXPAND_FRAMES + ClassReader.SKIP_DEBUG 
                      + ClassReader.SKIP_FRAMES);
    }

    private static void writeJarFile(String rtJar, String outputJar,
                                     String installPath) 
        throws IOException, FileNotFoundException
    {
        FileOutputStream fos = new FileOutputStream(outputJar);
        JarOutputStream out = null;

        if (rtJar != null) {
            JarInputStream in = new JarInputStream(new FileInputStream(rtJar));
            byte[] buf = new byte[4096];

            if (in.getManifest() != null)
                out = new JarOutputStream(fos, in.getManifest());
            else
                out = new JarOutputStream(fos);

            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                out.putNextEntry(entry);

                int r;
                while ((r = in.read(buf)) != -1) 
                    out.write(buf, 0, r);
                out.closeEntry();
            }
        }

        if (out == null)
            out = new JarOutputStream(fos);

        for (Iterator i = classMap.keySet().iterator(); i.hasNext();) {
            String className = (String) i.next();
            byte[] b = (byte[]) classMap.get(className);
            out.putNextEntry(new JarEntry(className));
            out.write(b);
            out.closeEntry();
        }

        if (installPath != null) {
            out.putNextEntry(new JarEntry("jtaint/InstallPath"));
            out.write(installPath.getBytes());
            out.closeEntry();
        }

        out.flush();
        out.close();
    }

    private static void usage() {
        System.err.println("Usage: jt-bootstrap [-i <installation path>] [-d]"
                + "[-r14 <runtime jre 1.4 jar filename>] "
                + "[-r15 <runtime jre 1.5+ jar filename>] "
                + "[-j <output jar filename>] ");
        System.err.print("Options:");
        System.err.println("-d\tEnable extra debugging checks");
        System.err.println("-i\tSpecify installation directory pathname");
        System.err.println("-r[14|15]\tSpecify name of runtime jar library to "
                           + "include in the output jar file.\nIf either "
                           + "of -r14 or -r15 is specified, both must be " 
                           + "provided.\nJRE 1.4 systems use -r14 libraries "
                           + "while JRE 1.5 (or above) use -r15 libraries.");
        System.err.println("-j <file>\tSpecify name of output jar file");
        System.err.println("\t(Default: jt-bootlib.jar)");
        System.err.println("see Reference documentation for more information");
        System.exit(-1);
    }
 
    public static void main(String[] args) {
        String rt14Jar     = null,
               rt15Jar     = null,
               installPath = null,
               outputJar   = "jt-bootlib.jar";
               
        for (int i = 0; i < args.length; i++) {
            if ("-r14".equals(args[i]) && i != args.length - 1)
                    rt14Jar = args[++i];
            else if ("-r15".equals(args[i]) && i != args.length - 1)
                    rt15Jar = args[++i];
            else if ("-j".equals(args[i]) && i != args.length - 1)
                outputJar = args[++i];
            else if ("-i".equals(args[i]) && i != args.length - 1)
                installPath = args[++i];
            else if ("-d".equals(args[i]))
               debug = true;
            else 
                usage();
       }

        if ((rt14Jar != null && rt15Jar == null) || 
                (rt14Jar == null && rt15Jar != null))
            usage();

        if (VmInfo.version() == VmInfo.VERSION_UNKNOWN 
                || VmInfo.vendor() == VmInfo.VENDOR_UNKNOWN) {
            System.err.println("Unknown java configuration:");
            System.err.println("vendor: " + System.getProperty("java.vendor"));
            System.err.println("vm name: " + 
                    System.getProperty("java.vm.name"));
            System.err.println("version: " + 
                    System.getProperty("java.specification.version"));
            System.err.println("Please report this error to JavaTaint");
            System.exit(-1);
        }

        try {
            addInstrumentation("java.lang.String", StringAdapter.builder());
            addInstrumentation("java.lang.StringBuffer", 
                               StringBufferAdapter.builder());
            addInstrumentation("java.lang.ClassLoader", 
                               ClassLoaderAdapter.builder());
            addInstrumentation("java.io.OutputStream", 
                               XssAdapter.builder("java/io/OutputStream"));
            addInstrumentation("java.io.PrintWriter", 
                               XssAdapter.builder("java/io/PrintWriter"));
            addInstrumentation("java.lang.Runtime", RuntimeAdapter.builder());
            addInstrumentation("java.lang.Thread", ThreadAdapter.builder());

            addInstrumentation("java.sql.Connection",
                               ConnectionAdapter.builder());
            addAnalysis("java.io.File", FileAdapter.CloneOptimizer.builder());
            addInstrumentation("java.io.File", FileAdapter.builder());

            if (VmInfo.vendor() == VmInfo.VENDOR_BEA)
                addInstrumentation("jrockit.vm.StringMaker",
                                   StringMakerAdapter.builder());

            if (VmInfo.version() >= VmInfo.VERSION1_5) {
                addInstrumentation("java.lang.StringBuilder", 
                        StringBuilderAdapter.builder());
                addInstrumentation("java.lang.ProcessBuilder", 
                        ProcessBuilderAdapter.builder());
                writeJarFile(rt15Jar, outputJar, installPath);
            } else {
                writeJarFile(rt14Jar, outputJar, installPath);
            }

        } catch (Exception e) {
            System.err.println("Bootstrap error: " + e);
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
