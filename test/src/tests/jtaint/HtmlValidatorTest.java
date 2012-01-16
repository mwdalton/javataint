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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.SimpleServletWriter;
import javax.servlet.ServletResponse;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletResponse;

public class HtmlValidatorTest
{
    private static final Charset[] charsets;

    static {
        Collection c = Charset.availableCharsets().values();
        charsets = (Charset[]) c.toArray(new Charset[0]);
    }

    private static final Locale[] locales = Locale.getAvailableLocales();

    private final int maxlen, maxops;
    private final SafeRandom sr;
    private final TestUtil tu;

    private static String ops = "";
    private List responseList = new ArrayList();

    public HtmlValidatorTest(int maxlen, int maxops, SafeRandom sr) {
        this.maxlen = maxlen;
        this.maxops = maxops;
        this.sr = sr;
        this.tu = new TestUtil(maxlen, sr, 2);
    }

    public String randomContentType() {
        return sr.nextBoolean() ? "text/html" : tu.randString();
    }

    public String randomCharacterEncoding() {
        /* XXX Not all encodings can handle arbitrary byte sequences 
        return charsets[sr.nextInt(charsets.length)].name(); */
        return "UTF-8";
    }

    private Locale randomLocale() {
        return locales[sr.nextInt(locales.length)];
    }

    private byte[] randomByteArray() throws UnsupportedEncodingException {
        /* XXX Not all encodings can handle arbitrary byte sequences or support
         * all valid Unicode characters. Return just simple ASCII because
         * we want per-byte writes to be sane. For example, 
         * java.io.OutputStream:write(byte[] b) => a loop calling 
         * java.io.OutputStream:write(int) with each byte cast to an int. 
         * Ensure each byte is a valid character.
         */
        int len = sr.nextInt(maxlen);
        @StringBuilder@ sb = new @StringBuilder@(len);

        for (int i = 0; i < len; i++)
            sb.append((char) sr.nextInt(128));
        return sb.toString().getBytes("UTF-8");
    }

    private String byteToString(byte[] b, String charset)
       throws UnsupportedEncodingException
    {
        return byteToString(b, 0, b.length, charset); 
    }

    private String byteToString(byte[] b, int off, int len, String charset) 
        throws UnsupportedEncodingException
    {
        return new String(b, off, len, charset);
    }

    private void testVuln(String s) {
        if (s.@internal@isTainted() != Log.hasVuln()) 
            throw new RuntimeException("Expected vulnerability " + 
                                       s.@internal@isTainted() + 
                                       " got vulnerability " + Log.hasVuln() +
                                       " output string " + s + " taint " +
                                       s.@internal@taint());
        Log.clearVuln();
    }
        

    private void randomOutputStreamOp(ServletOutputStream os, StringWriter sw,
                                      ServletResponse resp) 
        throws Exception
    {
        boolean b = sr.nextBoolean();
        char c = randomChar();
        int i = sr.nextInt();
        float f = sr.nextFloat();
        double d = sr.nextFloat();
        long l = sr.nextLong();

        String s = tu.randString();
        byte[] bytes = randomByteArray();
        int start = sr.nextInt(bytes.length);
        int end = start + sr.nextInt(bytes.length + 1 - start);

        switch(sr.nextInt(18)) {
            case 0: /* print(boolean b) */
                os.print(b);
                sw.write(String.valueOf(b));
                break;

            case 1: /* print(char c) */
                os.print(c);
                sw.write(String.valueOf(c));
                break;

            case 2: /* print(double d) */
                os.print(d);
                sw.write(String.valueOf(d));
                break;

            case 3: /* print(float f) */
                os.print(f);
                sw.write(String.valueOf(f));
                break;

            case 4: /* print(int i) */
                os.print(i);
                sw.write(String.valueOf(i));
                break;

            case 5: /* print(long l) */
                os.print(l);
                sw.write(String.valueOf(l));
                break;

            case 6: /* print(String s) */
                os.print(s);
                sw.write(s);
                testVuln(s);
                break;

            case 7: /* println() */
                os.println();
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 8: /* println(boolean b) */
                os.println(b);
                sw.write(String.valueOf(b));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 9: /* println(char c) */
                os.println(c);
                sw.write(String.valueOf(c));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 10: /* println(double d) */
                os.println(d);
                sw.write(String.valueOf(d));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 11: /* println(float f) */
                os.println(f);
                sw.write(String.valueOf(f));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 12: /* println(int i) */
                os.println(i);
                sw.write(String.valueOf(i));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 13: /* println(long l) */
                os.println(l);
                sw.write(String.valueOf(l));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 14: /* println(String s) */
                os.println(s);
                sw.write(s);
                sw.write(HtmlValidator.LINE_SEP);
                testVuln(s);
                break;

            case 15: /* write(byte[] b) */
                os.write(bytes);
                sw.write(byteToString(bytes, resp.getCharacterEncoding()));
                break;

            case 16: /* write(byte[] b, int off, int len) */
                os.write(bytes, start, end - start);
                sw.write(byteToString(bytes, start, end - start,
                                      resp.getCharacterEncoding()));
                break;

            case 17: /* write(int b) */
                if (bytes.length > 0) {
                    os.write((int)bytes[0]);
                    sw.write(byteToString(bytes, 0, 1, 
                                          resp.getCharacterEncoding()));
                }
                break;

            default:
                throw new RuntimeException("switch");
        }
    }

    private void testOutputStream(ServletOutputStream os, ServletResponse resp)
        throws Exception
    {
        StringWriter sw = os.getStringWriter();
        StringWriter expected = new StringWriter();
        int ops = sr.nextInt(maxops);

        for (int i = 0; i < ops; i++)
            randomOutputStreamOp(os, expected, resp);

        /* Compare results */
        os.flush();
        if (!sw.toString().equals(os.@internal@getHtmlValidator().toString())) 
            throw new RuntimeException("Real output " + sw.toString() 
                                       + " html validator output " + 
                                       os.@internal@getHtmlValidator()); 
        if (!expected.toString().equals(
                    os.@internal@getHtmlValidator().toString())) 
            throw new RuntimeException("Expected output " + expected.toString() 
                                       + " html validator output " + 
                                       os.@internal@getHtmlValidator()); 
    }

    private char randomChar() {
        return (char) sr.nextInt(0xffff + 1);
    }

    private String randomFormatLiteral(int len) {
        @StringBuilder@ sb = new @StringBuilder@(len);

        for (int i = 0; i < len; i++) {
            int off = sr.nextInt(27);
            if (off < 26)
                sb.append((char) ('a' + sr.nextInt(26)));
            else
                sb.append("%%");
        }

        return sb.toString();
    }

    private static class Format
    {
        private String formatString;
        private Object[] args;

        public Format(String formatString, Object[] args) {
            this.formatString = formatString;
            this.args = args;
        }

        public String formatString() { return formatString; }
        public Object[] args() { return args; }
    }

    private Format randomFormat() {
        int args = sr.nextInt(16);
        int formatLength = sr.nextInt(32);

        Object[] o = args > 0 ? new Object[args] : null;
        @StringBuilder@ fmt = new @StringBuilder@();

        for (int i = 0; i < args; i++) {
            int strLength = sr.nextInt(formatLength);
            fmt.append(randomFormatLiteral(strLength));
            formatLength -= strLength;
            fmt.append(' ');

            switch (sr.nextInt(7)) {
                case 0: /* Boolean */
                    fmt.append('%');
                    fmt.append(sr.nextBoolean() ? "b" : "B");
                    o[i] = new Boolean(sr.nextBoolean());
                    break;

                case 1: /* Hexadecimal Hashcode */
                    fmt.append('%');
                    fmt.append(sr.nextBoolean() ? "h" : "H");
                    o[i] = new Object();
                    break;

                case 2: /* String */
                    fmt.append('%');
                    fmt.append(sr.nextBoolean() ? "s" : "S");
                    o[i] = tu.randString();
                    break;

                case 3: /* Character */
                    fmt.append('%');
                    /* XXX 'C' format string specifiers not supported in Java
                     * V1_5
                     */
                    //[ifJava6]
                        fmt.append(sr.nextBoolean() ? "c" : "C");
                    //[fiJava6]
                    //[ifJava5]
                        fmt.append("c");
                    //[fiJava5]
                    o[i] = new Character(randomChar());
                    break;

                case 4: /* Decimal, Octal, or Hexadecimal Integer */
                    fmt.append('%');
                    String intFormats[] = { "d", "o", "x", "X" } ;
                    fmt.append(intFormats[sr.nextInt(intFormats.length)]);
                    o[i] = new Integer(sr.nextInt());
                    break;

               case 5: /* Various floating point format conversions */
                    String floatFormats[] = {"e","E","f","g","G","a","A"};
                    fmt.append('%');
                    fmt.append(floatFormats[sr.nextInt(floatFormats.length)]);
                    o[i] = new Double(sr.nextDouble());
                    break;

              case 6: /* Date/Time */
                    fmt.append('%');
                    fmt.append(sr.nextBoolean() ? "t" : "T");
                    fmt.append('c');
                    o[i] = new Date(sr.nextLong());
                    break;

             default:
                    throw new RuntimeException("switch");
            }

            fmt.append(' ');
        }

        return new Format(fmt.toString(), o);
    }

    private void randomPrintWriterOp(PrintWriter pw, StringWriter sw) {
        boolean b = sr.nextBoolean();
        char c = randomChar();
        int i = sr.nextInt();
        float f = sr.nextFloat();
        double d = sr.nextFloat();
        long l = sr.nextLong();
        Object obj = new Object();

        String s = tu.randString();
        Format fmt = randomFormat();
        Locale locale = randomLocale();
        int start = sr.nextInt(s.length());
        int end = start + sr.nextInt(s.length() + 1 - start);

        int badStart = sr.nextInt();
        int badEnd = sr.nextInt();
        boolean expectLengthException = badStart < 0 || badEnd < 0 || 
                                        badStart > badEnd || 
                                        badEnd > s.length();
        boolean hasException = false;
        PrintWriter pw2 = null;

        switch(sr.nextInt(32)) {
            case 0: /* append(char c) */
                //[ifJava5+]
                pw2 = pw.append(c);
                sw.write((int)c);
                if (pw2 != pw) 
                    throw new RuntimeException("Unexpected return value");
                //[fiJava5+]
                break;

            case 1: /* append(CharSequence csq) */
                //[ifJava5+]
                pw2 = pw.append((CharSequence)s);
                sw.write(s);
                testVuln(s);
                if (pw2 != pw) 
                    throw new RuntimeException("Unexpected return value");
                //[fiJava5+]
                break;

            case 2: /* append(CharSequence csq) */
                //[ifJava5+]
                if (sr.nextInt(32) != 0) {
                    pw2 = pw.append((CharSequence)s, start, end);
                    sw.write(s, start, end - start);
                    testVuln(s.substring(start, end));
                } else {
                    try {
                        pw2 = pw.append((CharSequence)s, badStart, badEnd);
                    } catch (IndexOutOfBoundsException e) {
                        hasException = true; 
                    }

                    if (expectLengthException != hasException)
                        throw new RuntimeException("Expected length exception "
                                + expectLengthException + " got exception " +
                                hasException);
                    if (!hasException) {
                        sw.write(s, badStart, badEnd - badStart);
                        testVuln(s.substring(badStart, badEnd));
                    }
                }

                if (!hasException && pw2 != pw) 
                    throw new RuntimeException("Unexpected return value");
                //[fiJava5+]
                break;

            case 3: /* flush() */
                pw.flush();
                break;

            case 4: /* format(Locale l, String fmt, Object... args) */
                //[ifJava5+]
                pw.format(locale, fmt.formatString(), fmt.args());
                sw.write(String.format(locale, fmt.formatString(), fmt.args()));
                testVuln(String.format(locale, fmt.formatString(), fmt.args()));
                //[fiJava5+]
                break;

            case 5: /* format(String fmt, Object... args)  */
                //[ifJava5+]
                pw.format(fmt.formatString(), fmt.args());
                sw.write(String.format(Locale.getDefault(), fmt.formatString(), 
                                       fmt.args()));
                testVuln(String.format(Locale.getDefault(), fmt.formatString(),
                                       fmt.args()));
                //[fiJava5+]
                break;

            case 6: /* printf(Locale l, String fmt, Object... args) */
                //[ifJava5+]
                pw.printf(locale, fmt.formatString(), fmt.args());
                sw.write(String.format(locale, fmt.formatString(), fmt.args()));
                testVuln(String.format(locale, fmt.formatString(), fmt.args()));
                //[fiJava5+]
                break;

            case 7: /* printf(String fmt, Object... args) */
                //[ifJava5+]
                pw.printf(fmt.formatString(), fmt.args());
                sw.write(String.format(Locale.getDefault(), fmt.formatString(), 
                                       fmt.args()));
                testVuln(String.format(Locale.getDefault(), fmt.formatString(), 
                                       fmt.args()));
                //[fiJava5+]
                break;

            case 8: /* print(boolean b) */
                pw.print(b);
                sw.write(String.valueOf(b));
                break;

            case 9: /* print(char c) */
                pw.print(c);
                sw.write(String.valueOf(c));
                break;

            case 10: /* print(char[] c) */
                pw.print(s.toCharArray());
                sw.write(s);
                break;

            case 11: /* print(double d) */
                pw.print(d);
                sw.write(String.valueOf(d));
                break;

            case 12: /* print(float f) */
                pw.print(f);
                sw.write(String.valueOf(f));
                break;

            case 13: /* print(int i) */
                pw.print(i);
                sw.write(String.valueOf(i));
                break;

            case 14: /* print(long l) */
                pw.print(l);
                sw.write(String.valueOf(l));
                break;

            case 15: /* print(Object obj) */
                pw.print(obj);
                sw.write(String.valueOf(obj));
                break;

            case 16: /* print(String s) */
                pw.print(s);
                sw.write(s);
                testVuln(s);
                break;

            case 17: /* println() */
                pw.println();
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 18: /* println(boolean b) */
                pw.println(b);
                sw.write(String.valueOf(b));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 19: /* println(char c) */
                pw.println(c);
                sw.write(String.valueOf(c));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 20: /* println(char[] c) */
                pw.println(s.toCharArray());
                sw.write(s);
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 21: /* println(double d) */
                pw.println(d);
                sw.write(String.valueOf(d));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 22: /* println(float f) */
                pw.println(f);
                sw.write(String.valueOf(f));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 23: /* println(int i) */
                pw.println(i);
                sw.write(String.valueOf(i));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 24: /* println(long l) */
                pw.println(l);
                sw.write(String.valueOf(l));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 25: /* println(Object obj) */
                pw.println(obj);
                sw.write(String.valueOf(obj));
                sw.write(HtmlValidator.LINE_SEP);
                break;

            case 26: /* println(String s) */
                pw.println(s);
                sw.write(s);
                sw.write(HtmlValidator.LINE_SEP);
                testVuln(s);
                break;

            case 27: /* write(char[] buf) */
                pw.write(s.toCharArray());
                sw.write(s);
                break;

            case 28: /* write(char[] buf, int off, int len) */
                pw.write(s.toCharArray(), start, end - start);
                sw.write(s.toCharArray(), start, end - start); 
                break;

            case 29: /* write(int c) */
                pw.write(i);
                sw.write(i);
                break;

            case 30: /* write(String s) */
                pw.write(s);
                sw.write(s);
                testVuln(s);
                break;

            case 31: /* write(String s, int off, int len) */
                if (sr.nextInt(32) != 0) {
                    pw.write(s, start, end - start);
                    sw.write(s, start, end - start);
                    testVuln(s.substring(start, end));
                } else {
                    try {
                        pw.write(s, badStart, badEnd - badStart);
                    } catch (IndexOutOfBoundsException e) {
                        hasException = true;
                    }

                    if (expectLengthException != hasException)
                        throw new RuntimeException("Expected length exception "
                                + expectLengthException + " got exception " +
                                hasException);
                    if (!hasException) {
                        sw.write(s, badStart, badEnd - badStart);
                        testVuln(s.substring(badStart, badEnd));
                    }
                }
                break;

            default:
                throw new RuntimeException("switch error");
        }
    }

    private void testPrintWriter(SimpleServletWriter ssw, ServletResponse resp)
        throws Exception
    {
        StringWriter sw = ssw.getStringWriter();
        StringWriter expected = new StringWriter();
        int ops = sr.nextInt(maxops);

        for (int i = 0; i < ops; i++)
            randomPrintWriterOp(ssw, expected);

        /* Compare results */
        ssw.flush();
        if (!sw.toString().equals(ssw.@internal@getHtmlValidator().toString())) 
            throw new RuntimeException("Real output " + sw.toString() 
                                       + " html validator output " + 
                                       ssw.@internal@getHtmlValidator());
        if (!expected.toString().equals(
                    ssw.@internal@getHtmlValidator().toString())) 
            throw new RuntimeException("Expected output " + expected.toString() 
                                       + " html validator output " + 
                                       ssw.@internal@getHtmlValidator());
    }

    private void test() throws Exception {
        ServletResponse resp;
        String encoding = randomCharacterEncoding();
        String type = randomContentType();
        StringWriter sw = new StringWriter();

        if (responseList.size() == 0 || sr.nextBoolean()) {

            if (sr.nextBoolean())
                resp = new HttpServletResponse(sr);
                                             
            else
                resp = new ServletResponse(sr);

            responseList.add(resp);
        } else {
            int idx = sr.nextInt(responseList.size());
            resp = (ServletResponse) responseList.get(idx);
        }

        resp.setCharacterEncoding(encoding);
        resp.setContentType(type);
        resp.setWriter(sw);

        if (!encoding.equals(resp.getCharacterEncoding()) ||
                !type.equals(resp.getContentType()))
            throw new RuntimeException("Encoding/Type error");

        if (sr.nextBoolean()) 
            testOutputStream(resp.getOutputStream(), resp);
        else 
            testPrintWriter((SimpleServletWriter)resp.getWriter(), resp);
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 65536; 
        int maxops = 1024; 
        int nrtest = 16384;

        HtmlValidatorTest hvt;
        String logfile = "HtmlValidatorTest.log";
        PrintStream ps = null;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-l"))
               maxlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-o"))
               maxops = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java HtmlValidatorTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofCommand] "
                       + "[-n NumberofTests] "
                       + "[-o maximumNumberOfOpsPerTest] "
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
        ps.print(" -o ");
        ps.print(maxops);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        SafeRandom sr = new SafeRandom(seed);
        hvt = new HtmlValidatorTest(maxlen, maxops, sr);

        for (int i = 0; i < nrtest; i++) {
            ops = "";
            try {
                hvt.test();
            } catch (Throwable th) {
                System.out.println("Unexpected exception -- Op " + ops);
                th.printStackTrace();
                System.exit(-1);
            }

        }

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
