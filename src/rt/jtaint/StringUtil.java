/* Copyright 2009 Michael Dalton */
package jtaint;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StringUtil 
{
    private static String doSubstring(String orig, int beginIndex, int endIndex,
                                      String result)
    {
        Taint t;

         if (orig == result || !orig.@internal@isTainted() 
                 || result.@internal@isTainted())
             return result;

         t = orig.@internal@taint().subset(beginIndex, endIndex);

         if (t.isTainted())
             return new String(result, t);
         return result;
    }

    public static String substring(String orig, int beginIndex, int endIndex,
            String result) 
    {
        try {
            return doSubstring(orig, beginIndex, endIndex, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    private static String doConcat(String orig, String other, String result)
    {
        Taint t;

        if ((!orig.@internal@isTainted() && !other.@internal@isTainted()) 
                || result.@internal@isTainted())
            return result;

        t = TaintUtil.append(orig.@internal@taint(), orig.length(),
                             other.@internal@taint(), other.length());
        if (t != null)
            return new String(result, t);
        return result;
    }

    public static String concat(String orig, String other, String result)
    {
        try {
            return doConcat(orig, other, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    public static String doConcat(String[] s, int len, String result) {
        boolean tainted = false;

        for (int i = 0; i < len; i++) 
            if (s[i].@internal@isTainted()) 
                tainted = true;

        if (!tainted) return result;

        Taint t = null;
        for (int slen = 0, i = 0; i < len; slen += s[i].length(), i++) 
            t = TaintUtil.append(t,slen,s[i].@internal@taint(),s[i].length());

        if (t.length() != result.length())
            throw new RuntimeException("Taint length corruption");
        if (t != null)
            return new String(result, t);
        return result;
    }

    public static String concat(String[] s, int len, String result) {
        try {
            return doConcat(s, len, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    private static String doReplace(String orig, char oldChar, String result)
    {
        Taint t;

        if (orig == result || !orig.@internal@isTainted() 
                || result.@internal@isTainted())
            return result;

        t = orig.@internal@taint();

        for (int i = 0; i < orig.length(); i++)
            if (orig.charAt(i) == oldChar)
                t.clear(i);

        if (t.isTainted())
            return new String(result, t);
        return result;
    }

    public static String replace(String orig, char oldChar, char newChar,
            String result) 
    {
        try {
            return doReplace(orig, oldChar, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    private static String doTrim(String orig, String result)
    {
        Taint old;
        int begin, end;

        if (orig == result || !orig.@internal@isTainted() 
                || result.@internal@isTainted())
            return result;

        old   = orig.@internal@taint();
        begin = orig.indexOf(result);
        end   = begin + result.length();

        if (result.length() > 0 && (begin != orig.lastIndexOf(result) 
                    || begin < 0))
            throw new RuntimeException("trim ?");
        Taint t = old.subset(begin, end);

        if (t.isTainted())
            return new String(result, t);
        return result;
    }

    public static String trim(String orig, String result) 
    {
        try {
            return doTrim(orig, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    /** Propagate taint across toLowerCase, toUpperCase calls. 
     * @see java.lang.String
     * @see java.lang.ConditionalSpecialCasing
     * @see http://www.unicode.org/Public/UNIDATA/SpecialCasing.txt
     * XXX java/lang/ConditionalSpecialCasing:toLowerCaseCharArray doesn't
     * return the 'normal' lowercase conversion if it can't find a special
     * case match, so we add the check ourselves (and call Character.toChars).
     */

    //[ifJava5+]
    private static String doChangeCase(String orig, Locale locale, 
                                       boolean isUpper, String result)
    {
        Taint t, old;
        int olen = orig.length(),
            rlen = result.length();
        int ocount, rcount;
        int i, j;

        if (orig == result || !orig.@internal@isTainted() 
                || result.@internal@isTainted())
            return result;


        t = new Taint(false, result.length());
        old = orig.@internal@taint();


        for (i = 0, j = 0; i < olen && j < rlen; i += ocount, j += rcount) {
            int ochar = orig.codePointAt(i);
            int rchar; 
            boolean tainted = false,
                    changed = false;

            ocount = Character.charCount(ochar);
            if (isUpper)
                rchar = String.@internal@toUpperCaseEx(orig, i, locale);
            else
                rchar = String.@internal@toLowerCaseEx(orig, i, locale);

            if (!String.@internal@isError(rchar))
                rcount = Character.charCount(rchar);
             else {
                 if (isUpper)
                     rcount = String.@internal@toUpperCaseCharArray(orig, i, 
                                  locale).length;
                 else {
                     char[] c = String.@internal@toLowerCaseCharArray(orig, i, 
                                  locale);
                     if (c != null)
                         rcount = c.length; 
                     else {
                         c = new char[2];
                         rcount = Character.toChars(rchar, c, 0);
                     }
                 }
             }
            
            if (ocount != rcount) changed = true;

            for (int k = 0; !changed && k < ocount; k++)
                if (orig.charAt(i+k) != result.charAt(j+k))
                    changed = true;

            for (int k = 0 ;k < ocount; k++)
                if (old.get(i+k)) tainted = true;

            if (!tainted) 
                continue;

            if (!changed) {
                for (int m = 0; m < ocount; m++)
                        if (old.get(i + m))
                            t.set(j + m);
            } else {
                for (int l = 0; l < rcount; l++) 
                    t.set(j + l);
            }
        }

        if (i != orig.length() || j != result.length())
            throw new RuntimeException("toXCase conversion error: Lang "
                    + locale.getLanguage() + " upper " + isUpper 
                    + " input " + orig + " ouput " + result);
        return new String(result, t);
    }

    public static String toUpperCase(String orig, Locale locale, String result)
    {
        try {
            return doChangeCase(orig, locale, true, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    public static String toLowerCase(String orig, Locale locale, String result)
    {
        try {
            return doChangeCase(orig, locale, false, result);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }
    //[fiJava5+]
   
    /* There is no ConditionalSpecialCasing class in JRE 1.4 environments, so
     * we require toUpperCase to perform the same special condition tests as
     * java/lang/String
     */

    //[ifJava4] 
    public static String toLowerCase(String orig, Locale locale, String result)
    {
        try {
            if (orig == result || !orig.@internal@isTainted() 
                    || result.@internal@isTainted())
                return result;

            if (orig.length() != result.length())
                throw new RuntimeException("Unexpected toLowerCase - input: "
                        + orig + " output: " + result);

            return new String(result, orig.@internal@taint());
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }

    public static String toUpperCase(String orig, Locale locale, String result)
    {
        try {
            Taint t, old;
            int olen = orig.length(),
                rlen = result.length();
            int rcount, i, j;

            if (orig == result || !orig.@internal@isTainted() 
                    || result.@internal@isTainted())
                return result;

            t = new Taint(false, result.length());
            old = orig.@internal@taint();

            for (i = 0, j = 0; i < olen && j < rlen; i++, j += rcount) {
                char ochar = orig.charAt(i),
                     rchar = String.@internal@toUpperCaseEx(ochar);
                boolean tainted = old.get(i),
                        changed = false;

                if ("tr".equals(locale.getLanguage()) 
                        && (ochar == 'i' || ochar == '\u0131'))
                    rcount = 1;
                else if (!String.@internal@isError(rchar))
                    rcount = 1; 
                else 
                    rcount = 
                        String.@internal@toUpperCaseCharArray(ochar).length;

                if (!tainted) 
                    continue;

                for (int l = 0; l < rcount; l++) 
                    t.set(j + l);
            }

            if (i != orig.length() || j != result.length())
                throw new RuntimeException("toUpper conversion error: Lang "
                        + locale.getLanguage() + " input " + orig + 
                        " ouput " + result);
            return new String(result, t);
        } catch (Throwable th) {
            Log.error(th);
            return result;
        }
    }
    //[fiJava4]

    /* The following String methods do not require further instrumentation:
     * replaceAll, replaceFirst, split - all derived from the Pattern and
     * Matcher class methods of the same name. These methods use
     * StringBuilder.append when dealing with untrusted input subsequences,
     * and thus propagation is already done by our StringBuilder taint
     * propagation support. Similarly, Matcher.appendReplacement() and
     * Matcher.appendTail() also does not require any further 
     * instrumentation other than our pre-existing changes to StringBuilder.
     */

    public static Taint stringToTaint(char[] v, int count) {
        try {
            if (v.length <= count)
                throw new RuntimeException("stringToTaint called on untainted"
                                           + "String: v.length:" +
                                           v.length + " count:" + count);

            char [] u = new char[v.length - count];
            System.arraycopy(v, count, u, 0, v.length - count);
            return new Taint(u, count);
        } catch (Throwable th) {
            Log.error(th);
            return null;
        }
    }

    public static char[] taintToString(String s, Taint t)
    {
        try {
            int slen = s.length(),
                tlen = t.length(),
                tclen = (tlen + 15)/16;

            if (slen != tlen)
                throw new IllegalArgumentException("invalid taint: s len" 
                                                    + slen + " tclen " +
                                                    tclen + " taint len " +
                                                    t.length());

            char[] v = new char[slen + tclen];
            s.getChars(0, slen, v, 0);
            t.getTaintAsChars(v, slen);
            return v;
        } catch (Throwable th) {
            Log.error(th);
            return s.toCharArray();
        }
    }

    /* Create an untainted String */
    public static String toUntainted(String str) {
        try {
            if (str == null || !str.@internal@isTainted()) return str;
            
            return new String(str.toCharArray());
        } catch (Throwable e) {
            Log.error(e);
            return str;
        }
    }

    /* Create a tainted String */
    public static String toTainted(String str) {
        try {
            if (str == null) return str;

            return new String(str, new Taint(true, str.length()));
        } catch (Throwable e) {
            Log.error(e);
            return str;
        }
    }

    /* Create a tainted String array */
    public static String[] toTainted(String[] a) {
        try {
            if (a == null) return a;

            String[] r = new String[a.length];
            for (int i = 0; i < a.length; i++)
                r[i] = toTainted(a[i]);
            return r;
        } catch (Throwable e) {
            Log.error(e);
            return a;
        }
    }

    /* Create a tainted StringBuffer */
    public static StringBuffer toTainted(StringBuffer sb) {
        try {
            if (sb == null)
                return sb;
            return new StringBuffer(toTainted(sb.toString()));
        } catch (Throwable e) {
            Log.error(e);
            return sb;
        }
    }

    /* Create a tainted StringBuilder */
    //[ifJava5+]
    public static StringBuilder toTainted(StringBuilder sb) {
        try {
            if (sb == null)
                return sb;
            return new StringBuilder(toTainted(sb.toString()));
        } catch (Throwable e) {
            Log.error(e);
            return sb;
        }
    }
    //[fiJava5+]

    /* Create a Map of tainted Strings -> tainted String arrays */
    public static Map toTainted(Map in) {
        try {
            String key;
            String[] value;
            HashMap m = new HashMap();

            if (in == null) return in;

            Set entries = in.entrySet();
            Map.Entry[] e = (Map.Entry[])
                entries.toArray(new Map.Entry[entries.size()]);

            for (int i = 0; i < e.length; i++) {
                key = toTainted((String) e[i].getKey());
                value = toTainted((String[]) e[i].getValue());
                m.put(key, value);
            }

            return m;
        } catch (Throwable e) {
            Log.error(e);
            return in;
        }
    }

    /* Create a Hashtable of tainted strings -> tainted String arrays */
    public static Hashtable toTainted(Hashtable in) {
        try {
            String key;
            String[] value;
            Hashtable ht = new Hashtable();

            if (in == null) return in;

            Set entries = in.entrySet();
            Map.Entry[] e = (Map.Entry[])
                entries.toArray(new Map.Entry[entries.size()]);

            for (int i = 0; i < e.length; i++) {
                key = toTainted((String) e[i].getKey());
                value = toTainted((String[]) e[i].getValue());
                ht.put(key, value);
            }

            return ht;
        } catch (Throwable e) {
            Log.error(e);
            return in;
        }
    }

    static final class TaintedEnumeration implements Enumeration {
        Enumeration e;

        public TaintedEnumeration(Enumeration e) {
            this.e = e;
        }

        public boolean hasMoreElements() { return e.hasMoreElements(); }

        public Object nextElement() {
            return toTainted((String)e.nextElement());
        }
    }

    /* Create an Enumeration of tainted Strings */
    public static Enumeration toTainted(Enumeration e) {
        try { 
            return new TaintedEnumeration(e);
        } catch (Throwable th) {
            Log.error(th);
            return e;
        }
    }
}
