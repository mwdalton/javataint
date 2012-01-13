/* Copyright 2009 Michael Dalton */
package jtaint;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarMerger 
{

    private static void merge(JarInputStream  baseJar, 
                              JarInputStream  updateJar, 
                              JarOutputStream outputJar) 
        throws IOException, FileNotFoundException
    {
        List updateEntries = new ArrayList();
        JarEntry entry;
        byte[] buf = new byte[4096];
        int r;

        while ((entry = updateJar.getNextJarEntry()) != null) {
            updateEntries.add(entry.getName());
            outputJar.putNextEntry(entry);
            while ((r = updateJar.read(buf)) != -1)
                outputJar.write(buf, 0, r);
        }

        while ((entry = baseJar.getNextJarEntry()) != null) {
            if (updateEntries.contains(entry.getName())) 
                continue;
            outputJar.putNextEntry(entry);
            while ((r = baseJar.read(buf)) != -1)
                outputJar.write(buf, 0, r);
        }
    }

    private static void usage() {
        System.out.println("Usage: JarMerger <base jar> <update jar> "
                           + "<output jar>");
        System.out.println("Copies all jar entries from base and update jar to "
                           + "output jar, except that in cases where base and "
                           + "update both contain entries with the same name, "
                           + "only the entry from update is copied");
        System.exit(-1);
    }
               

    public static void main(String[] args) 
        throws IOException, FileNotFoundException
    {
        if (args.length != 3) 
            usage();

        JarInputStream baseJar = 
            new JarInputStream(new FileInputStream(args[0]));
        JarInputStream updateJar = 
            new JarInputStream(new FileInputStream(args[1]));
        JarOutputStream outputJar;
        Manifest m = baseJar.getManifest();

        if (m != null)  
            outputJar = new JarOutputStream(new FileOutputStream(args[2]), m);
        else
            outputJar = new JarOutputStream(new FileOutputStream(args[2]));

        merge(baseJar, updateJar, outputJar);

        baseJar.close();
        updateJar.close();
        outputJar.close();
    }
}
