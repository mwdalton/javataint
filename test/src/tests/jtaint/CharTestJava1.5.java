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

public class CharTest
{

    public static void check(String src, String dst, boolean upper,
                             Locale locale)
    {
        int srcchar = src.codePointAt(0);
        int dstchar;
        int srclen, dstlen;

        srclen = Character.charCount(srcchar);

        if (upper)
            dstchar = String.@internal@toUpperCaseEx(src, 0, locale);
        else
            dstchar = String.@internal@toLowerCaseEx(src, 0, locale);

        if (!src.@internal@isError(dstchar))
            dstlen = Character.charCount(dstchar);
        else {
            if (upper)
                dstlen = String.@internal@toUpperCaseCharArray(src, 0, 
                                                               locale).length;
            else {
                char [] c = String.@internal@toLowerCaseCharArray(src, 0, 
                                                                  locale);
                if (c != null)
                    dstlen = c.length;
                else {
                    c = new char[2];
                    dstlen = Character.toChars(dstchar, c, 0);
                }
            }
        }

        if (dstlen != dst.length() || srclen != src.length()) {
            System.err.printf("Failure: src (%X) [length: " +
                    src.length() + " char count: " + srclen + "] dst (%x) " +
                    "[length: " + dst.length() + " char count: " + dstlen + "]",
                    srcchar, dstchar);
            System.exit(-1);
        }
    }

    public static void main(String[] args)
    {
        Locale[] l = Locale.getAvailableLocales();

        for (int i = 0; i < l.length; i++) {
            for (int c = 0; c <= 0x10fff; c++)
            {
                String s = new String(Character.toChars(c));
                
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
