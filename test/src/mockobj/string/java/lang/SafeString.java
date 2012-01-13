/* Copyright 2009 Michael Dalton */
package java.lang;

import jtaint.OrigStringUtil;
import jtaint.SafeTaint;
import jtaint.Taint;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SafeString implements Comparable, SafeCharSequence
{
    private final OrigString str;
    private final SafeTaint taint;

    public SafeString() {
        str = new OrigString();
        taint = new SafeTaint();
    }

    public SafeString(SafeString s) {
        str = new OrigString(s.str);
        taint = (SafeTaint) s.taint.clone();
    }

    public SafeString(String s) {
        str = new OrigString(s.toCharArray());
        Taint t = s.@internal@taint();
        BitSet b;
        if (t == null) 
            b = new BitSet();
        else
            b = t.asBitSet();
        taint = new SafeTaint(b, s.length());
    }

    public SafeString(char[] value) {
        str = new OrigString(value);
        taint = new SafeTaint(false, value.length);
    }

    public SafeString(char value[], int offset, int count) {
        str = new OrigString(value,offset, count);
        taint = new SafeTaint(false, count);
    }

    //[ifJava5+]
    public SafeString(int[] codePoints, int offset, int count) {
        str = new OrigString(codePoints, offset, count);
        taint = new SafeTaint(false, str.length());
    }
    //[fiJava5+]

    public SafeString(byte ascii[], int hibyte, int offset, int count) {
        str = new OrigString(ascii, hibyte, offset, count);
        taint = new SafeTaint(false, str.length());
    }

    public SafeString(byte ascii[], int hibyte) {
        str = new OrigString(ascii, hibyte);
        taint = new SafeTaint(false, str.length());
    }
    
    public SafeString(byte bytes[], int offset, int length, 
                      String charsetName)
        throws UnsupportedEncodingException
    {
        str = new OrigString(bytes, offset, length, 
                             OrigStringUtil.toOrig(charsetName));
        taint = new SafeTaint(false, str.length());
    }

    //[ifJava6]
    public SafeString(byte bytes[], int offset, int length, Charset charset) {
        str = new OrigString(bytes, offset, length, charset);
        taint = new SafeTaint(false, str.length());
    }
    //[fiJava6]

    public SafeString(byte bytes[], String charsetName)
        throws UnsupportedEncodingException 
    {
        str = new OrigString(bytes, OrigStringUtil.toOrig(charsetName));
        taint = new SafeTaint(false, str.length());
    }

    //[ifJava6]
    public SafeString(byte bytes[], Charset charset) {
        str = new OrigString(bytes, charset);
        taint = new SafeTaint(false, str.length());
    }
    //[fiJava6]


    public SafeString(byte bytes[], int offset, int length) { 
        str = new OrigString(bytes,offset,length);
        taint = new SafeTaint(false, str.length());
    }

    public SafeString(byte bytes[]) { 
        str = new OrigString(bytes);
        taint = new SafeTaint(false, str.length());
    }

    public SafeString(SafeStringBuffer buffer) { 
        this(buffer.toSafeString());
    }
   
    //[ifJava5+] 
    public SafeString(SafeStringBuilder builder) { 
        this(builder.toSafeString()); 
    }
    //[fiJava5+]

    public int length() {
        return str.length();
    }

    //[ifJava6]
    public boolean isEmpty() {
        return str.isEmpty();
    }
    //[fiJava6]

    public char charAt(int index) {
        return str.charAt(index);
    }

    //[ifJava5+]
    public int codePointAt(int index) {
        return str.codePointAt(index);
    }

    public int codePointBefore(int index) {
        return str.codePointBefore(index);
    }

    public int codePointCount(int beginIndex, int endIndex) {
        return str.codePointCount(beginIndex, endIndex);
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        return str.offsetByCodePoints(index, codePointOffset);
    }
    //[fiJava5+]

    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        str.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        str.getBytes(srcBegin, srcEnd, dst, dstBegin);
    }

    public byte[] getBytes(String charsetName)
        throws UnsupportedEncodingException 
    {
        return str.getBytes(OrigStringUtil.toOrig(charsetName));
    }

    //[ifJava6]
    public byte[] getBytes(Charset charset) {
        return str.getBytes(charset);
    }
    //[fiJava6]

    public byte[] getBytes() {
        return str.getBytes();
    }

    public boolean equals(Object anObject) {
        SafeString sfs;

        if (anObject == this)
            return true;
        if (anObject == null || !(anObject instanceof SafeString))
            return false;
        sfs = (SafeString) anObject;
        return str.equals(sfs.str);
    }

    public boolean contentEquals(SafeStringBuffer sb) {
        return str.contentEquals(new OrigStringBuffer(OrigStringUtil.toOrig(sb.toString())));
    }

    //[ifJava5+]
    public boolean contentEquals(SafeCharSequence cs) {
        return str.contentEquals(cs.toSafeString().str);
    }
    //[fiJava5+]

    public boolean equalsIgnoreCase(SafeString anotherString) {
        return str.equalsIgnoreCase(anotherString.str);
    }

    public int compareTo(SafeString anotherString) {
        return str.compareTo(anotherString.str);
    }

    public int compareTo(Object o) {
        if (o instanceof SafeString)
            return compareTo((SafeString)o);
        return -1;
    }
    public int compareToIgnoreCase(SafeString anotherString) {
        return str.compareToIgnoreCase(anotherString.str);
    }

    public boolean regionMatches(int toffset, SafeString other, int ooffset,
                                 int len) {
        return str.regionMatches(toffset, other.str, ooffset, len);
    }

    public boolean regionMatches(boolean ignoreCase, int toffset,
                           SafeString other, int ooffset, int len) {
        return str.regionMatches(ignoreCase, toffset, other.str, ooffset, len);
    }

    public boolean startsWith(SafeString prefix, int toffset) {
        return str.startsWith(prefix.str, toffset);
    }

    public boolean startsWith(SafeString prefix) {
        return str.startsWith(prefix.str);
    }

    public boolean endsWith(SafeString suffix) {
        return str.endsWith(suffix.str);
    }

    public int hashCode() {
        return str.hashCode();
    }

    public int indexOf(int ch) {
        return str.indexOf(ch);
    }

    public int indexOf(int ch, int fromIndex) {
        return str.indexOf(ch, fromIndex);
    }

    public int lastIndexOf(int ch) {
        return str.lastIndexOf(ch);
    }

    public int lastIndexOf(int ch, int fromIndex) {
        return str.lastIndexOf(ch, fromIndex);
    }

    public int indexOf(SafeString sfs) {
        return str.indexOf(sfs.str);
    }

    public int indexOf(SafeString sfs, int fromIndex) {
        return str.indexOf(sfs.str, fromIndex);
    }

    public int lastIndexOf(SafeString sfs) {
        return str.lastIndexOf(sfs.str);
    }

    public int lastIndexOf(SafeString sfs, int fromIndex) {
        return str.lastIndexOf(sfs.str, fromIndex);
    }

    public SafeString substring(int beginIndex) {
        return substring(beginIndex, str.length());
    }

    public SafeString substring(int beginIndex, int endIndex) {
        OrigString os = str.substring(beginIndex, endIndex);
        SafeTaint st = ((SafeTaint) taint.clone()).subset(beginIndex, endIndex);
        return new SafeString(os, st);
    }

    public CharSequence subSequence(int beginIndex, int endIndex) {
        return substring(beginIndex, endIndex);
    }

    public SafeString concat(SafeString sfs) {
        OrigString os = str.concat(sfs.str);
        SafeTaint st = ((SafeTaint)taint.clone()).append(sfs.taint);
        return new SafeString(os, st);
    }

    public SafeString replace(char oldChar, char newChar) {
        char[] v = str.toCharArray();
        SafeTaint st = (SafeTaint) taint.clone();

        for (int i = 0; i < v.length; i++) {
            if (v[i] == oldChar) {
                v[i] = newChar;
                st.clear(i);
            }
        }
        return new SafeString(new OrigString(v), st);
    }

    public boolean matches(SafeString regex) {
        return str.matches(regex.str);
    }

    //[ifJava5+]
    public boolean contains(SafeCharSequence s) {
        return str.contains(s.toSafeString().origString());
    }
    //[fiJava5+]

    public SafeString replaceFirst(SafeString regex, SafeString replacement) {
        /* This is complicated -- we have to rely on the taint propagation
         * characteristics of the 'String' class here. First we translate
         * the current SafeString instance to a String, then we perform all
         * the regular expression operations, and finally we convert back to
         * a SafeString
         */
        String s = toString();
        return new SafeString(s.replaceFirst(regex.toString(), 
                    replacement.toString()));
    }

    public SafeString replaceAll(SafeString regex, SafeString replacement) { 
        String s = toString();
        return new SafeString(s.replaceAll(regex.toString(), 
                replacement.toString()));
    }

    //[ifJava5]
    public SafeString replace(SafeCharSequence target, 
            SafeCharSequence replacement) { 
        String s = toString();
        return new SafeString(s.replace(target.toSafeString().toString(), 
                    replacement.toSafeString().toString()));
    }
    //[fiJava5]

    public SafeString[] split(SafeString regex, int limit) { 
        String s = toString();
        SafeString[] ret;
        String[] tmp;

        tmp = s.split(regex.toString(), limit);

        ret = new SafeString[tmp.length];

        for (int i = 0; i < ret.length; i++)
            ret[i] = new SafeString(tmp[i]);
        return ret;
    }

    public SafeString[] split(SafeString regex) { 
        return split(regex, 0);
    }

    //[ifJava5+]
    private SafeString changeCase(Locale locale, OrigString os, boolean isUpper)
        throws IllegalArgumentException
    {
        BitSet b = new BitSet();
        int olen = length(),
            rlen = os.length();
        int ocount, rcount;
        int i, j;

        for (i = 0, j = 0; i < olen && j < rlen; i+=ocount, j+=rcount) {
            int ochar = codePointAt(i);
            int rchar; 
            boolean tainted = false,
                    changed = false;

            ocount = Character.charCount(ochar);
            if (isUpper)
                rchar = toUpperCaseEx(i, locale);
            else
                rchar = toLowerCaseEx(i, locale);

            if (!String.@internal@isError(rchar)) {
                rcount = Character.charCount(rchar);
                if (os.codePointAt(j) != rchar)
                    throw new IllegalArgumentException("conversion error");
            } else {
                 char[] rtmp;

                 if (isUpper) {
                     rtmp = toUpperCaseCharArray(i, locale);
                     rcount = rtmp.length;
                 } else {
                     rtmp = toLowerCaseCharArray(i, locale);
                     rcount = rtmp.length;
                 }

                 for (int m = 0; m < rtmp.length; m++)
                     if (os.charAt(j+m) != rtmp[m])
                         throw new IllegalArgumentException("conversion error");
             }

            if (ocount != rcount) changed = true;

            for (int k = 0; !changed && k < ocount; k++)
                if (charAt(i+k) != os.charAt(j+k))
                    changed = true;

            for (int k = 0 ;k < ocount; k++)
                if (taint.get(i+k)) tainted = true;

            if (!tainted) 
                continue;

            if (!changed) {
                for (int m = 0; m < ocount; m++)
                    b.set(j+m, taint.get(i+m));
            } else {
                for (int l = 0; l < rcount; l++) 
                    b.set(j+l);
            }
        }

        if (i != length() || j != os.length())
            throw new IllegalArgumentException("toXCase conversion error: Lang "
                    + locale.getLanguage());
        return new SafeString(os, new SafeTaint(b, os.length()));
    }
    //[fiJava5+]
    //[ifJava4]
    private SafeString changeCase(Locale locale, OrigString os, boolean isUpper)
        throws IllegalArgumentException
    {
        if (!isUpper)
            return new SafeString(os, taint());

        BitSet b = new BitSet();
        int olen = length(),
            rlen = os.length();
        int rcount, i, j;

        for (i = 0, j = 0; i < olen && j < rlen; i++, j+=rcount) {
            char ochar = charAt(i);
            char[] rchars = new char[] { toUpperCaseEx(ochar) };
            boolean tainted = taint.get(i),
                    changed = false;

            if ("tr".equals(locale.getLanguage()) 
                    && (ochar == 'i' || ochar == '\u0131')) {
                if (ochar == 'i')
                    rchars[0] = '\u0130';
                else
                    rchars[0] = 'I';
                rcount = 1;
            } else if (!String.@internal@isError(rchars[0])) {
                rcount = 1; 
            } else {
                rchars = toUpperCaseCharArray(ochar);
                rcount = rchars.length;
            }

            for (int m = 0; m < rcount; m++) {
                if (os.charAt(j+m) != rchars[m])
                    throw new IllegalArgumentException("conversion error");
                if (tainted)
                    b.set(j+m);
            }
        }

        if (i != length() || j != os.length())
            throw new IllegalArgumentException("toXCase conversion error: Lang "
                    + locale.getLanguage());
        return new SafeString(os, new SafeTaint(b, os.length()));
    }
    //[fiJava4]

    public SafeString toLowerCase(Locale locale) 
        throws IllegalArgumentException
    {
        return changeCase(locale, str.toLowerCase(locale), false);
    }

    public SafeString toLowerCase() 
        throws IllegalArgumentException
    {
        return toLowerCase(Locale.getDefault());
    }

    public SafeString toUpperCase(Locale locale) 
        throws IllegalArgumentException
    { 
        return changeCase(locale, str.toUpperCase(locale), true);
    }

    public SafeString toUpperCase() 
        throws IllegalArgumentException
    {
        return toUpperCase(Locale.getDefault());
    }

    public SafeString trim() { 
        OrigString os = str.trim();
        int index = str.indexOf(os);
        
        return new SafeString(os, 
                 ((SafeTaint)taint.clone()).subset(index, index + os.length()));

    }

    public String toString() {
        String s = str.toString();
        return new String(s, new Taint(taint.asBitSet(), s.length()));
    }

    public SafeString toSafeString() {
        return this;
    }

    public char[] toCharArray() {
        return str.toCharArray();
    }

    public boolean isTainted() {
        return taint.isTainted();
    }

    public SafeString(OrigString os, SafeTaint st) {
        this.str = new OrigString(os);
        this.taint = (SafeTaint) st.clone();
    }

    public SafeTaint taint() { return (SafeTaint) taint.clone(); }

    public OrigString origString() { return new OrigString(str); }

    public boolean verify(String s) throws IllegalArgumentException
    {
        if (!str.equals(OrigStringUtil.toOrig(s)) || 
                !taint.verify(s.@internal@taint()))
            return false;
        return true;
    }

    //[ifJava5+]
    public int toLowerCaseEx(int index, Locale l) {
        return ConditionalSpecialCasing.toLowerCaseEx(toString(), index, l);
    }

    public int toUpperCaseEx(int index, Locale l) {
        return ConditionalSpecialCasing.toUpperCaseEx(toString(), index, l);
    }

    public char[] toLowerCaseCharArray(int index, Locale l) {
        return ConditionalSpecialCasing.toLowerCaseCharArray(toString(), 
                index, l);
    }

    public char[] toUpperCaseCharArray(int index, Locale l) {
        return ConditionalSpecialCasing.toUpperCaseCharArray(toString(), 
                index, l);
    }
    //[fiJava5+]
    //[ifJava4]
    public static char toUpperCaseEx(char c) {
        return Character.toUpperCaseEx(c);
    }

    public static char[] toUpperCaseCharArray(char c) {
        return Character.toUpperCaseCharArray(c);
    }
    //[fiJava4]
    public static boolean isError(int c) { return String.@internal@isError(c); }

    public static SafeString valueOf(Object obj) {
        return new SafeString ((obj == null) ? "null" : obj.toString());
    }

    public static SafeString valueOf(char data[]) {
        return new SafeString(data);
    }

    public static SafeString valueOf(char data[], int offset, int count) {
        return new SafeString(data, offset, count);
    }

    public static SafeString copyValueOf(char data[], int offset, int count) {
        return new SafeString(data, offset, count);
    }

    public static SafeString copyValueOf(char data[]) {
        return copyValueOf(data, 0, data.length);
    }

    public static SafeString valueOf(boolean b) {
        return new SafeString(b ? "true" : "false");
    }

    public static SafeString valueOf(char c) {
        char data[] = {c};
        return new SafeString(data);
    }

    public static SafeString valueOf(int i) {
        return new SafeString(Integer.toString(i, 10));
    }

    public static SafeString valueOf(long l) {
        return new SafeString(Long.toString(l, 10));
    }

    public static SafeString valueOf(float f) {
        return new SafeString(Float.toString(f));
    }

    public static SafeString valueOf(double d) {
        return new SafeString(Double.toString(d));
    }
}
