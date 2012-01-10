/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class FileTest
{
    private final int maxlen;
    private final Random r;
    private boolean exception;

    private static String currentOp;
    private static String currentPath;
    private static String currentChild;
    private static File   currentFile;
    private static URI    currentURI;

    private static String exceptionMsg;

    public FileTest(int maxlen, Random r) {
        this.maxlen = maxlen;
        this.r = r;
        clearException();
    }

    private void clearException() { exception = false; exceptionMsg = null; }

    private void setException(String msg) { 
        exception = true; 
        exceptionMsg = msg;
    }

    private boolean pendingException() { return exception; }

    private String randString() {
        int len = 1 + r.nextInt(64);
        @StringBuilder@ sb = new @StringBuilder@(len);
        BitSet b = new BitSet();

        if (r.nextInt(128) == 0) { 
            b.set(0, r.nextBoolean());
            return new String(".", new Taint(b, 1));
        }

        if (r.nextInt(4) == 0) {
            b.set(0, r.nextBoolean());
            b.set(1, r.nextBoolean());
            return new String("..", new Taint(b, 2));
        }

        for (int i = 0; i < len; i++) {
            int c, clen;
            boolean taint = r.nextInt(64) == 0;
            int oldlen = sb.length();

            c = 'a' + r.nextInt(26);

            if (c == '.' || c == '/') { i--; continue; }

            if (c == '\0') {
                if (r.nextInt(16) == 0) { i--; continue; }
                taint = true;
            }

            sb.append((char) c);

            b.set(oldlen, taint);
        }

        return new String(sb.toString(), new Taint(b, sb.length()));
    }

    private String randChar(char c, int taintP) {
        boolean taint = r.nextInt(taintP) == 0;
        return new String(String.valueOf(c), new Taint(taint, 1));
    }

    static private String realPath(String path) {
        @StringBuilder@ sb = new @StringBuilder@(path.length());

        for (int i = 0; i < path.length(); i++) {

            if (path.charAt(i) != '/') {
                sb.append(path.charAt(i));
                continue;
            }

            /* Strip any redundant /'s */
            do {
                i++;
            } while (i < path.length() && path.charAt(i) == '/');

            i--;
            
            if (i == path.length() - 1 || path.charAt(i+1) != '.') {
                sb.append(path.charAt(i));
                continue;
            }

            if (i == path.length() -2 || path.charAt(i+2) == '/') { 
                /* pathname is /./ */
                i+=1;
                continue;
            } else if (path.charAt(i+2) != '.')
                throw new RuntimeException("corrupt path: " + path);
            else {
                /* Pathname is /../ */
                int lastSlash = sb.lastIndexOf("/");
                if (lastSlash >= 0) 
                    sb.replace(lastSlash, sb.length(), "");
                i += 2;
            }

        }

        if (sb.length() == 0) /* Occurs if we get a string like /.. */
            return "/";
            
        return sb.toString();
    }

    static private String untaintedPrefix(String path) {
        int i;
        Taint t = path.@internal@taint();
        if (t == null)
            t = new Taint(false, path.length());

        for (i = 0; i < path.length(); i++)
            if (t.get(i)) 
                break;

        return path.substring(0, i);
    }

    static private boolean isSafePath(String path) {
        String prefix; 

        if (!path.@internal@isTainted())
            return true;

        prefix = realPath(untaintedPrefix(path));
        path = realPath(path);

        /* Handle a serious edge case: If the prefix is a directory that ends
         * in a trailing slash, and the tainted input is of the form dir/..,
         * then we need to allow both the prefix and the prefix sans trailing
         * slash as possible matches.
         */
        if (prefix.length() > 0 && prefix.charAt(prefix.length()-1) == '/' &&
            prefix.substring(0, prefix.length()-1).equals(path))
            return true;
        return path.startsWith(prefix);  
    }

    private String randPath() {
        int len = 1 + r.nextInt(maxlen);
        StringBuffer sb = new StringBuffer(len);
        boolean isAbsolute = r.nextBoolean(),
                foundTaint = false;
        String path, prefix;

        /* Ensure that our filename doesn't actually match a possible 
         * symbolic link. For absolute paths, we reference the 
         * the property test.abs.nosym. For relative paths, we already 
         * set the value of the user.dir property to the value of the
         * test.rel.nosym property. The user must set test.rel.nosym and 
         * test.abs.nosym.to the full absolute path of (possibly distinct)
         * directories containing no symbolic links.
         */
        if (isAbsolute) {
            prefix = System.getProperty("test.abs.nosym");
            BitSet b = new BitSet();

            for (int i = 0; i < prefix.length(); i++)
                if (r.nextInt(128) == 0)
                    b.set(i);
            prefix = new String(prefix, new Taint(b, prefix.length()));
            sb.append(prefix);
        } else {
            do {
                prefix = randString();
            } while (prefix.charAt(0) == '.');

            sb.append(prefix + randChar('/', 128));
        }

        while (sb.length() < len) {
            String s = randString();

            /* XXX Fun note:
             * There is currently a bug in Java where File.getCanonicalPath()
             * fails to actually strip '..' in very limited circumstances.
             * This only occurs when the _untainted prefix_ tries to reference
             * a directory _below_ the root directory, and the pathname 
             * contains a directory that does not exist. For example
             * new File("/foo/../../bin/ls").getCanonicalPath()
             * returns /../bin/ls if /foo does not exist. Bug submitted to Sun.
             * For now, we don't allow workaround this the old fashioned
             * way. 
             */

            if (s.charAt(0) == '.' && 
                    "/".equals(realPath(toAbsolutePath(sb.toString()))))
                continue;

            sb.append(s);

            if (sb.length() < len || (sb.length() >= len && r.nextBoolean())) {
                int nslash = 1 + r.nextInt(7);
                    sb.append(randChar('/', 64));
            }
        }

        return sb.toString();
    }

    private String toAbsolutePath(String pathname) {
        if (pathname.charAt(0) == '/')
            return pathname;
        return System.getProperty("user.dir") + pathname;
    }

    private void validatePath(String pathname) {
        pathname = toAbsolutePath(pathname);
        Taint t = pathname.@internal@taint();
        if (t == null)
            t = new Taint(false, pathname.length());

        if (!isSafePath(pathname))
            setException("Pathname " + pathname + " is unsafe");

        if (t.get(0))
            setException("Tainted absolute path");

        else if (pathname.@internal@isTainted() && 
                 realPath(untaintedPrefix(pathname)).equals("/"))
            setException("Tainted path confined to root directory");

        for (int i = 0; i < pathname.length(); i++) 
            if (pathname.charAt(i) == '\0' && t.get(i))
                setException("Tainted null byte");
    }

    private String randValidDirectory() {
        String s;

        do {
            clearException();
            s = randPath();
            if (s.charAt(s.length() - 1) != '/')
                s = s.substring(0, s.lastIndexOf('/') + 1);
            validatePath(s);
        } while(pendingException());

        return s;
    }

    private void testStringParent() {
        String parent = randValidDirectory();
        String child = randString();

        currentChild = child;

        if (r.nextInt(128) == 0)  {
            parent = "";
            currentPath = parent;

            validatePath("/" + child);
            if (child.@internal@isTainted())
                setException("File with empty parent and tainted child");

            new File(parent, child);
            return;
        }
        else if(r.nextInt(128) == 0) {
            parent = null;
            currentPath = parent;
            validatePath(child);

            new File(parent, child);
            return;
        }

        currentPath = parent;
        validatePath(parent + child);
        new File(parent, child);
    }


    private void testFileParent() {
        String parent = randValidDirectory();
        String child = randString();
        currentChild = child;

        if (r.nextInt(128) == 0)  {
            parent = "";
            currentPath = parent;
            validatePath("/" + child);

            if (child.@internal@isTainted())
                setException("File with empty parent and tainted child");
            new File(new File(parent), child);
            return;
        }
        else if(r.nextInt(128) == 0) {
            parent = null;
            currentPath = parent;
            validatePath(child);

            new File((File)null, child);
            return;
        }

        currentPath = parent;
        validatePath(parent + child);
        new File(new File(parent), child);
    }

    private void testPath() {
        String pathname = randPath();
        currentPath = pathname;
        
        validatePath(pathname);
        new File(pathname);
    }

    private void testURI() throws URISyntaxException {
        URI uri;
        String pathname = randPath();
        String scheme = "file";
        BitSet b = new BitSet();

        /* URI paths must be absolute*/
        pathname = toAbsolutePath(pathname);

        for (int i = 0; i < scheme.length(); i++)
            if (r.nextInt(256) == 0) {
                b.set(i);
                setException("Tainted URI scheme");
            }

        scheme = new String(scheme, new Taint(b, scheme.length()));

        currentPath = pathname;
        validatePath(pathname);

        uri = new URI(scheme, /* authority */ null, pathname,
                      /* query */ null, /* fragment */ null);

        new File(uri);
    }

    private void testSerialize() throws IOException, ClassNotFoundException {
        File f = new File(randValidDirectory());
        String canonPath = f.getCanonicalPath();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bout);

        os.writeObject(f);
        os.flush();
        os.close();
        bout.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream is = new ObjectInputStream(bin);

        f = (File) is.readObject();
        is.close();
        bin.close();

        if (!canonPath.equals(f.getCanonicalPath()) ||
                f.@internal@untaintedPrefix() != null)
            throw new RuntimeException("Corrupted serialization");
    }

    private void testSymlink() {
        String prefix = System.getProperty("test.sym");

        String[] badFiles = { "t1/t2/bad1", "t1/t2/bad2", "t1/t2/bad3",
                              "t1/t2/bad1/bob/a/f/g", "t1/t2/bad2/foo.txt", 
                              "t1/t2/bad4" };

        String[] goodFiles = { "t1/t2/good1", "t1/t2/good2", "t1/t2/good3",
                               "t1/good1/foo.txt", "t1/t2/good4", "t1/t2/good5",
                               "t1/t2/good5/t1/t2/good1" };

        /* Initialize tainted suffixes */
        for (int i = 0; i < badFiles.length; i++)
            badFiles[i] = new String(badFiles[i], 
                    new Taint(true, badFiles[i].length()));

        for (int i = 0; i < goodFiles.length; i++)
            goodFiles[i] = new String(goodFiles[i], 
                    new Taint(true, goodFiles[i].length()));

        /* Test BadFiles */

        for (int i  = 0; i < badFiles.length; i++) {
            boolean caught = false;

            try {
                new File(prefix + badFiles[i]);
            } catch (SiegeBrkException e) {
                caught = true;
            }

            if (!caught)
                throw new RuntimeException("Expected exception for symlink " +
                        prefix + badFiles[i]);
        }

        /* Test GoodFiles */ 

        for (int i = 0; i < goodFiles.length; i++) 
            new File(prefix + goodFiles[i]);
    }

    private void test() throws IOException, URISyntaxException,
                               ClassNotFoundException 
    {
        currentOp = "symlink";
        testSymlink();

        currentPath = null;
        currentChild = null;
        currentURI = null;
        currentOp = null;

        switch(r.nextInt(5)) {
            case 0:
                currentOp = "File(String pathname)";
                testPath();
                break;

            case 1:
                currentOp = "File(String parent, String child)";
                testStringParent();
                break;

            case 2:
                currentOp = "File(File parent, String child)";
                testFileParent(); 
                break;

            case 3:
                currentOp = "File(URI uri)";
                testURI(); 
                break;

            case 4:
                currentOp = "Serialize(File)";
                testSerialize();
                break;

            default:
                throw new RuntimeException("switch");
        }
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 128;
        int nrtest = 16384;

        Random r;
        FileTest ft;
        String logfile = "FileTest.log";
        PrintStream ps = null;

        System.setProperty("user.dir", System.getProperty("test.rel.nosym"));

        if (System.getProperty("user.dir") == null ||
                !System.getProperty("user.dir").equals(
                    System.getProperty("test.rel.nosym"))) {
            System.out.println("Unable to set user.dir to test.nosym");
            System.exit(-1);
        }

        if (System.getProperty("test.sym") == null) {
            System.out.println("Could not find test.sym");
            System.exit(-1);
        }

        if (System.getProperty("test.abs.nosym") == null) {
            System.out.println("Could not find test.abs.nosym");
            System.exit(-1);
        }

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-l"))
               maxlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java FileTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofCommand] "
                       + "[-n NumberofTests]"
                       + "[-f logFileName]");
               System.exit(-1);
           }
        }

        try {
            ps = new PrintStream(new FileOutputStream(logfile));
        } catch (FileNotFoundException e) {
            System.out.println("Error opening logfile [" + logfile + "]: " + e);
            System.exit(-1);
        }

        ps.print("-s ");
        ps.print(seed);
        ps.print(" -l ");
        ps.print(maxlen);
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        r = new Random(seed);
        ft = new FileTest(maxlen, r);

        try { 
           ft.testSymlink();
        } catch(Exception e) {
            throw new RuntimeException("testValid: unexpected exception", e);
        }

        for (int i = 0; i < nrtest; i++) {
            Exception e = null;
            ft.clearException();

            try {
                ft.test();
                if (ft.pendingException())
                    e = new RuntimeException("Expected exception - got none");
            } catch (SiegeBrkException s) {
                if (!ft.pendingException())
                    e = new RuntimeException("Unexpected SiegeBrkException", s);
            } catch (Throwable x) {
                e = new RuntimeException("Unexpected other exception", x);
            }

            if (e != null) {
                System.out.println("FAILURE -- Op " + currentOp);

                if (currentPath != null) {
                    System.out.println("Current pathname: "  + currentPath);
                    System.out.println("Taint: " + 
                                       currentPath.@internal@taint());
                } 
                
                if (currentChild != null) {
                    System.out.println("Current child: "  + currentChild);
                    System.out.println("Taint: " + 
                                       currentChild.@internal@taint());
                }

                if (currentFile != null) {
                    System.out.println("Current file: "+currentFile.getPath());
                    System.out.println("Untainted prefix: " + 
                            currentFile.@internal@untaintedPrefix());
                }

                if (currentURI != null) {
                    String query = currentURI.getQuery();
                    System.out.println("Current URI: " + currentURI);
                    System.out.println("Query: "  + query);
                    System.out.println("Query Taint: " + 
                                       query.@internal@taint());
                }

                if (exceptionMsg != null)
                    System.out.println("Expected exception: " + exceptionMsg);
                e.printStackTrace();
                System.exit(-1);
            }
        }

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
