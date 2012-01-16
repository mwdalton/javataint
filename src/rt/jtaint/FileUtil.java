/*
 *  Copyright 2009-2012 Michael Dalton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jtaint;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public final class FileUtil 
{
    private static final String[] fsRootPaths;

    static {
        File[] roots = File.listRoots();
        int len = roots.length;
        String[] tmp = new String[len];

        for (int i = 0; i < len; i++)
            tmp[i] = roots[i].getPath();

        /* Assign fsRootPaths only after the array has been initialized. 
         * This is due to the memory consistency rules for final fields in
         * the Java Memory Model.
         */
        fsRootPaths = tmp;
    }

    private static void abortFile(String untaintedPrefix, File result,
                                  String msg)
        throws JTaintException
    {
        @StringBuilder@ sb = new @StringBuilder@();
        JTaintException e;

        try {
            sb.append("\nPathname: " + result.getCanonicalPath());
        } catch (Throwable th) { 
            Log.error(th);
        }

        if (untaintedPrefix != null)
            sb.append("\nEscaped from directory " + untaintedPrefix + "\n");
        e = new JTaintException(sb.toString(), Configuration.fileWhitelist);

        if (Configuration.filePolicyLogAttack)
            Log.attack("Directory Traversal", e);
        throw e;
    }

    private static String getUntaintedPrefix(String s) {
        @StringBuilder@ sb = new @StringBuilder@();
        Taint t;
        int len = s.length();

        if (!s.@internal@isTainted()) return s;
        t = s.@internal@taint();

        for (int i = 0; i < len; i++) {
            if (t.get(i)) 
                break;
            sb.append(s.charAt(i));
        }

        return sb.toString();
    }

    /** Determine if a file access is a directory traversal attack. 
     * A file access is allowed if:
     * (a) The safeCanonicalPath of the untainted prefix is equal to the
     * canonicalPath of the resulting filename, with trailing '/' from
     * getCanonicalPath excluded.
     *
     * This occurs when the user inputs no additional path information, such
     * as /untainted/foo/.. , where foo/.. is the untrusted suffix.
     *
     * (b) The canonical path of the resulting filename starts with 
     * the safeCanonicalpath of the untainted prefix.
     * This is the actual directory traversal attack prevention.
     */
    private static String doValidateFile(String untaintedPrefix, File result) {
        String resultPath; 
        String prefixNoSlash = null;
        int prefixLength = untaintedPrefix.length();
        int rlen = fsRootPaths.length;

        if (untaintedPrefix.endsWith(File.separator))
            prefixNoSlash = untaintedPrefix.substring(0, prefixLength - 1);

        try {
            resultPath = result.getCanonicalPath();
        } catch (Throwable e) {
            Log.error(e);
            return null;
        }

        for (int i = 0; i < rlen; i++) 
            if (fsRootPaths[i].equals(untaintedPrefix))
                abortFile(untaintedPrefix, result,
                          "Untrusted input confined to root directory " + 
                          fsRootPaths[i]);

        if ((prefixNoSlash == null || !resultPath.equals(prefixNoSlash))
                && !resultPath.startsWith(untaintedPrefix))
            abortFile(untaintedPrefix, result, "escaped from application " + 
                      "directory");

        return untaintedPrefix;
    }

    private static void scanNull(String path, File result) {
        int len = path.length();
        Taint t = path.@internal@taint();

        for (int i = 0; i < len; i++)
            if (path.charAt(i) == '\0') {
                if (t != null && t.get(i)) 
                    abortFile(null, result, "Tainted null byte: " + path);
                Log.warn("Untainted null byte in filename " + path);
            }
    }

    /** Calculate the canonical path for an untainted prefix. We cannot
     * just use File.getSafeCanonicalPath directly because we want the 
     * Canonical path for a <b> prefix </b> of a pathname, and this prefix
     * may end in a directory name. The canonicalPath() of a directory name
     * does not include the trailing file separator (/). This allows attacks
     * like the following:
     *
     * Pathname: /usr/www/webroot/a/../admin-passwords.txt
     * Untainted prefix: /usr/www/webroot/a/
     * Tainted suffix: ../admin-passwords.txt
     *
     * getCanonicalPath(Untainted prefix) = /usr/www/webroot/a
     * getCanonicalPath(Pathname) = /usr/www/webroot/admin-passwords.txt
     *
     * This would pass our security check requirements because 
     * getCanonicalPath(Pathname) starts with 
     * getCanonicalPath(Untainted Prefix). To prevent this attack,
     * we use the getSafeCanonicalPath method. This method is a wrapper
     * around getCanonicalPath, which appends an extra file separator to
     * the output of getCanonicalPath if the original pathname ended in a
     * file separator. So in the above example
     *
     * getSafeCanonicalPath(Untainted prefix) = /usr/www/webroot/a/
     *
     * and the above attack is correctly prevented.
     */
    private static String getSafeCanonicalPath(String prefix, File prefixFile)
        throws IOException
    {
        String prefixPath = prefixFile.getCanonicalPath();

        if (prefix.length() > 0 && prefix.endsWith(File.separator))
            if (!prefixPath.endsWith(File.separator))
                prefixPath += File.separator;
        return prefixPath;
    }

    public static String validateFile(File parent, String child, File result) 
        throws JTaintException
    {
        String p, c;
    

        if (parent == null) 
            return validateFile(child, result);

        try {
            p = parent.@internal@untaintedPrefix();

            /* A fully untainted file pathname has a 'null' untainted prefix */
            if ((p == null && !child.@internal@isTainted())
                    || !Configuration.filePolicyEnabled)
                return null;

            scanNull(child, result);

            if ("".equals(parent.getPath()))
                abortFile(parent.getCanonicalPath(), result, 
                        "Untrusted input confined to root directory");

            if (p != null) 
                return doValidateFile(getSafeCanonicalPath(p, new File(p)), 
                                      result);

            c = getUntaintedPrefix(child);
            if (c.length() != 0) {
                return doValidateFile(
                        getSafeCanonicalPath(c, new File(parent, c)), 
                        result);
            } else {
                /* see getSafeCanonicalPath() */
                p = parent.getCanonicalPath();
                if (!p.endsWith(File.separator))
                    p += File.separator;

                return doValidateFile(getSafeCanonicalPath(p, new File(p)),
                                      result);
            }
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
            return null;
        } catch (Throwable e) {
            Log.error(e);
            return null;
        }
    }

    public static String validateFile(String pathname, File result)
        throws JTaintException
    {
        String u;
        

        try {
            if (!pathname.@internal@isTainted() ||
                    !Configuration.filePolicyEnabled)
                return null;

            scanNull(pathname, result);

            if (!result.isAbsolute()) 
                pathname = System.getProperty("user.dir") + File.separator + 
                           pathname;

            u = getUntaintedPrefix(pathname);
            if (u.length() == 0) 
                abortFile(null, result, "Fully untrusted pathname");
            return doValidateFile(getSafeCanonicalPath(u, new File(u)), result);
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
            return null;
        } catch (Throwable e) {
            Log.error(e);
            return null;
        }
    }

    public static String validateFile(String parent, String child, File result) 
        throws JTaintException
    {
        String s, u;

        try {
            if (parent == null)
                return validateFile(child, result);

            if ((!parent.@internal@isTainted() && !child.@internal@isTainted())
                    || !Configuration.filePolicyEnabled)
                return null;

            scanNull(parent, result);
            scanNull(child, result);


            if ("".equals(parent))
                abortFile(null, result, 
                          "Untrusted input confined to root directory");
            if (!result.isAbsolute())
                parent = System.getProperty("user.dir") + File.separator + 
                         parent;
            if (parent.@internal@isTainted()) {
                u = getUntaintedPrefix(parent);
                if (u.length() == 0) 
                    abortFile(null, result, "Fully untrusted pathname");

                return doValidateFile(getSafeCanonicalPath(u, new File(u)),
                                      result);
            } else {
                u = getUntaintedPrefix(child);

                if (u.length() != 0)
                    return doValidateFile(
                            getSafeCanonicalPath(u, new File(parent, u)), 
                                                 result);
                else {
                    /* See getSafeCanonicalPath */
                    if (!parent.endsWith("/"))
                        parent += "/";
                    return doValidateFile(
                            getSafeCanonicalPath(parent, new File(parent)), 
                                                 result);
                }
            }
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
            return null;
        } catch (Throwable e) {
            Log.error(e);
            return null;
        }
    }

    public static String validateFile(URI uri, File result) 
        throws JTaintException
    {
        String scheme, path, u;
        URI tmp;

        try {
            if (!uri.isAbsolute() || uri.isOpaque())
                return null; /* Invalid file URI */

            scheme = uri.getScheme();
            path = uri.getPath();

            if ((!scheme.@internal@isTainted() && !path.@internal@isTainted())
                    || !Configuration.filePolicyEnabled)
                return null;

            if (scheme.@internal@isTainted()) 
                abortFile(null, result, "Untrusted filename URI scheme: " + 
                          scheme);
            scanNull(path, result);

            if (!result.isAbsolute())
                path = System.getProperty("user.dir") + File.separator + path;

            /* Now check pathname as you would have previously */

            u = getUntaintedPrefix(path);
            if (u.length() == 0)
                abortFile(null, result, "Fully untrusted pathname");
            tmp = new URI(scheme, /* authority */ null, 
                    /* path */ u, /* query */ null, /* fragment */ null);
            return doValidateFile(getSafeCanonicalPath(u, new File(tmp)), 
                                  result);
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
            return null;
        } catch (Throwable e) {
            Log.error(e);
            return null;
        }
    }
}
