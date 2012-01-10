/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.Locale;

public class CharTest
{
    public static void check(String src, String dst, boolean upper,
                             Locale locale)
    {
        if (upper == false) {
            if (src.length() != dst.length()) {
                System.err.println("Error converting to lowercase: "
                        + "source " + src + " taint " 
                        + src.@internal@taint() + " destination "
                        + dst + " taint " + dst.@internal@taint());
                System.exit(-1);
            }
            return;
        }

        char srcchar = src.charAt(0),
             dstchar = String.@internal@toUpperCaseEx(srcchar);
        int srclen = 1, dstlen;

        if ("tr".equals(locale.getLanguage()) 
                && (srcchar == 'i' || srcchar == '\u0131'))
            dstlen = 1;
        else if (!String.@internal@isError(dstchar))
            dstlen = 1; 
        else 
            dstlen = String.@internal@toUpperCaseCharArray(srcchar).length;

        if (dstlen != dst.length() || srclen != src.length()) {
            System.err.println("Failure: src (%X) [length: " +
                   src.length() + " char count: " + srclen + "] dst (%x) " +
                   "[length: " + dst.length() + " char count: " + dstlen + "]");
            System.exit(-1);
        }
    }

    public static void main(String[] args)
    {
        Locale[] l = Locale.getAvailableLocales();

        for (int i = 0; i < l.length; i++) {
            for (int c = 0; c <= 0xffff; c++)
            {
                String s = String.valueOf((char)c);
                
                String t = s.toLowerCase(l[i]);
                String u = s.toUpperCase(l[i]);
                check(s, t, false, l[i]);
                check(s, u, true, l[i]);
            }
        }

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.err.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
