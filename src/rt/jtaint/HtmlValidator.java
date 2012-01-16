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

import java.util.Locale;

public final class HtmlValidator 
{
    public static final String LINE_SEP = "\r\n";
    public static final String DEFAULT_CHARSET = "ISO-8859-1";
    public static final String UNKNOWN_CONTENT = "UNKNOWN";

    private final String charset;
    private final String contentType;

    public HtmlValidator(String charset, String contentType) {
        if (charset == null)
            charset = DEFAULT_CHARSET;
        if (contentType == null)
            contentType = UNKNOWN_CONTENT;

        this.charset = charset;
        this.contentType = contentType;
    }

    public String getCharset() { return charset; }

    public String getContentType() { return contentType; }

    public void print(String s) {
        if (!s.@internal@isTainted() || !Configuration.xssPolicyLogVuln)
            return;

        @StringBuilder@ sb = new @StringBuilder@();
        JTaintException e;

        sb.append("Cross-site scripting vulnerability detected: outputting ");
        sb.append("tainted string " + s + " (taint: " + s.@internal@taint());
        sb.append(" )\n");

        e = new JTaintException(sb.toString(), Configuration.xssWhitelist);
        Log.vuln("Cross Site Scripting", e);
    }

    public void println(String s) {
        print(s);
        print(LINE_SEP);
    }

    /* XXX TODO - Figure out which of these methods can _never_ result in
     * XSS and then just skip them entirely, i.e don't even instrument them.
     * period, and remove the methods from this class.
     *
     * Optimize all print(String, type*), print(char[], type*) 
     * and append(CharSequene, type*) methods
     *
     * Also if it turns out to more elegant, may want to have an aliasMap and
     * remove all these crude aliased methods. Furthermore, if \r\n don't 
     * matter (and I seem to recall some obscure case where they do), then 
     * we can treat print/println the same.
     */

    public void print(boolean b) { print(String.valueOf(b)); }

    public void print(char c)    { print(String.valueOf(c)); }

    public void print(char[] c)  { print(new String(c)); }

    public void print(double d)  { print(String.valueOf(d)); }

    public void print(float f)   { print(String.valueOf(f)); }

    public void print(int i)     { print(String.valueOf(i)); }

    public void print(long l)    { print(String.valueOf(l)); }

    public void print(Object o)  { print(String.valueOf(o)); }


    public void println() { print(LINE_SEP); }

    public void println(boolean b) { 
        print(String.valueOf(b));
        print(LINE_SEP);
    }

    public void println(char c)    { 
        print(String.valueOf(c)); 
        print(LINE_SEP);
    }

    public void println(char[] c)  { 
        print(new String(c)); 
        print(LINE_SEP);
    }

    public void println(double d)  { 
        print(String.valueOf(d)); 
        print(LINE_SEP);
    }

    public void println(float f)   { 
        print(String.valueOf(f)); 
        print(LINE_SEP);
    }

    public void println(int i)     { 
        print(String.valueOf(i)); 
        print(LINE_SEP);
    }

    public void println(long l)    { 
        print(String.valueOf(l)); 
        print(LINE_SEP);
    }

    public void println(Object o)  { 
        print(String.valueOf(o)); 
        print(LINE_SEP);
    }

    //[ifJava5+]
    public void append(char c)   { print(String.valueOf(c)); }
    
    public void append(CharSequence cs) { print(cs.toString()); }

    public void append(CharSequence cs, int start, int end) {
        print(cs.toString().substring(start, end));
    }

    public void format(String format, Object[] args) {
        format(Locale.getDefault(), format, args);
    }

    public void format(Locale l, String format, Object[] args) {
        String s = null;

        try {
            s = String.format(l, format, args);
        } catch (Throwable th) {
            Log.error(th);
        }

        if (s != null)
            print(s);
    }

    public void printf(String format, Object[] args) {
        format(Locale.getDefault(), format, args);
    }

    public void printf(Locale l, String format, Object[] args) {
        format(l, format, args);
    }
    //[fiJava5+]

    public void write(char[] c)  { print(new String(c)); }

    public void write (char[] c, int off, int len) {
        print(new String(c, off, len));
    }

    /* See java.io.OutputStream:write(int i) */
    public void write(byte b) {
        byte[] ba = new byte[] { b };
        write(ba, 0, ba.length);
    }

    public void write(byte[] b)  { 
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) {
       String s = null; 
        try {
            s = new String(b, off, len, charset);
        } catch (Throwable th) {
            Log.error(th);
        }

        if (s != null)
            print(s);
    }

    public void write(String s) {
        print(s);
    }

    public void write(String s, int off, int len) {
        print(s.substring(off, off + len));
    }
}
