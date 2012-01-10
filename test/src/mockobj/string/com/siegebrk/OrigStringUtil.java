/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.HashMap;
import java.util.Map;

public class OrigStringUtil
{
    /* We must have the semantics as a string literal - any two identical
     * strings converted to an OrigString must compare equal with the
     * reference equality operator (==)
     */

    private static Map origInternMap = new HashMap();

    public static OrigString toOrig(String s) {
        synchronized(origInternMap) {
            if (origInternMap.containsKey(s))
                return (OrigString) origInternMap.get(s);

            OrigString os = new OrigString(s.toCharArray());
            origInternMap.put(s, os);
            return os;
        }
    }

    public static OrigString[] toOrig(String[] s) {
        OrigString[] os = new OrigString[s.length];

        for (int i = 0; i < os.length; i++)
            os[i] = toOrig(s[i]);
        return os;
    }

    public static OrigStringBuffer toOrig(StringBuffer sb) {
        OrigString os = new OrigString(sb.toString().toCharArray());
        return new OrigStringBuffer(os);
    }

    public static OrigCharSequence toOrig(CharSequence cs) {
        return toOrig(cs.toString());
    }

    //[ifJava5+]
    public static OrigStringBuilder toOrig(StringBuilder sb) {
        OrigString os = new OrigString(sb.toString().toCharArray());
        return new OrigStringBuilder(os);
    }
    //[fiJava5+]

    /* String literals ("hello") are converted to OrigStrings by 
     * RemappingStringClassAdapter. Because Java guarantees that two 
     * identical literals will compare equal using the referential 
     * equality comparison operator ==, we must make a similar guarantee
     * for any Strings produced by OrigStrings. Thus any time we convert
     * any of the Orig classes to a String, we always call intern() to 
     * guarantee that it will work correctly with any referential equality 
     * checks.
     */
    public static String toBase(OrigString os) {
        return new String(os.toCharArray()).intern();
    }

    public static StringBuffer toBase(OrigStringBuffer osb) {
        char[] c = new char[osb.length()];
        osb.getChars(0, c.length, c, 0);
        return new StringBuffer(new String(c));
    }

    public static CharSequence toBase(OrigCharSequence osc) {
        return toBase(osc.toOrigString());
    }

    //[ifJava5+]
    public static StringBuilder toBase(OrigStringBuilder osb) {
        char[] c = new char[osb.length()];
        osb.getChars(0, c.length, c, 0);
        return new StringBuilder(new String(c));
    }
    //[fiJava5+]


    public static String toBaseString(OrigString os) {
        return toBase(os);
    }

    public static String toBaseString(OrigStringBuffer osb) {
        return toBase(osb).toString().intern();
    }

    public static String toBaseString(OrigCharSequence osc) {
        return toBaseString(osc.toOrigString());
    }

    //[ifJava5+]
    public static String toBaseString(OrigStringBuilder osb) {
        return toBase(osb).toString().intern();
    }
    //[fiJava5+]
}
