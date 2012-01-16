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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

//[ifJava5+]
import java.util.concurrent.CopyOnWriteArrayList;
//[fiJava5+]

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

public final class StringTest extends Thread
{
    private static int maxlen = 65536 * 2;
    private static int nrtest = 1024 * 32;
    private static int threads;
    private static volatile boolean fail;
    private SafeRandom sr;
    private Charset[] charsets;
    private Locale[] locales;

    private static class StringMethod
    {
        public static final int S_NEW                  = 0;
        public static final int S_CHARAT               = 1;
        public static final int S_CODEPOINT            = 2;
        public static final int S_COMPARETO            = 3;
        public static final int S_CONCAT               = 4;
        public static final int S_CONTAINS             = 5;
        public static final int S_CONTENTEQUALS        = 6;
        public static final int S_COPYVALUEOF          = 7;
        public static final int S_ENDSWITH             = 8;
        public static final int S_EQUALS               = 9;
        public static final int S_GETBYTES             = 10;
        public static final int S_GETCHARS             = 11;
        public static final int S_HASHCODE             = 12;
        public static final int S_INDEXOF              = 13;
        public static final int S_ISEMPTY              = 14;
        public static final int S_LASTINDEXOF          = 15;
        public static final int S_LENGTH               = 16;
        public static final int S_OFFSETBYCODEPOINTS   = 17;
        public static final int S_STARTSWITH           = 18;
        public static final int S_SUBSEQUENCE          = 19;
        public static final int S_SUBSTRING            = 20;
        public static final int S_TOCHARARRAY          = 21;
        public static final int S_TOLOWERCASE          = 22;
        public static final int S_TOSTRING             = 23;
        public static final int S_TOUPPERCASE          = 24;
        public static final int S_TRIM                 = 25;
        public static final int S_VALUEOF              = 26;
        public static final int S_ISTAINTED            = 27;
        public static final int S_SERIALIZE            = 28;
        public static final int S_END                  = 29;

        public static final String[] methodNames = 
        {
            "new",
            "charAt",
            "codePoint",
            "compareTo",
            "concat",
            "contains",
            "contentEquals",
            "copyValueOf",
            "endsWith",
            "equals",
            "getBytes",
            "getChars",
            "hashCode",
            "indexOf",
            "isEmpty",
            "lastIndexOf",
            "length",
            "offsetByCodePoints",
            "startsWith",
            "subSequence",
            "substring",
            "toCharArray",
            "toLowerCase",
            "toString",
            "toUpperCase",
            "trim",
            "valueOf",
            "isTainted",
            "serialize"
        };
    }

    private static class StringElem
    {
        String s;
        SafeString sfs;
        int id;
        List opList;

        public StringElem(String s, SafeString sfs, int id) {
            this.s = s;
            this.sfs = sfs;

            /* Unlike StringBuffer/Builder, String is an immutable class
             * and thus StringElems may be accessed concurrently by multiple 
             * threads. Consequently, opList must be a synchronized list to 
             * prevent race conditions in the addOp method.
             */
            this.opList = new Vector();
            this.id = id;
        }

        public String getString() { return s; }
        public SafeString getSafeString() { return sfs; }

        public void addOp(int op) { opList.add(StringMethod.methodNames[op]); }

        public void addOp(String op) { opList.add(op); }

        public void verify(String s) throws IllegalArgumentException {
            if (!sfs.verify(s))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(StringElem otherse) throws IllegalArgumentException {
            SafeString othersfs = otherse.getSafeString();
            if (!sfs.verify(s) || !sfs.verify(otherse.getString()) ||
                    !othersfs.verify(otherse.getString()) || 
                    !othersfs.verify(s))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify() throws IllegalArgumentException {
            verify(s);
        }
       
        public String toString() {
            String str = "id " + id + " string " + s + " taint " + 
                s.@internal@taint() + "safestring " + sfs + " safetaint " + 
                sfs.taint() + " operations: ";
            for (int i = 0; i < opList.size(); i++)
                str += (String) opList.get(i);
            return str;
        }
    }


    private static class StringBufferMethod
    {
        public static final int SB_NEW                  = 0;
        public static final int SB_APPEND               = 1;
        public static final int SB_CAPACITY             = 2;
        public static final int SB_CHARAT               = 3;
        public static final int SB_CODEPOINT            = 4;
        public static final int SB_DELETE               = 5;
        public static final int SB_ENSURECAPACITY       = 6;
        public static final int SB_GETCHARS             = 7;
        public static final int SB_INDEXOF              = 8;
        public static final int SB_INSERT               = 9;
        public static final int SB_LASTINDEXOF          = 10;
        public static final int SB_LENGTH               = 11;
        public static final int SB_OFFSETBYCODEPOINTS   = 12;
        public static final int SB_REPLACE              = 13;
        public static final int SB_REVERSE              = 14;
        public static final int SB_SETCHARAT            = 15;
        public static final int SB_SETLENGTH            = 16;
        public static final int SB_SUBSEQUENCE          = 17;
        public static final int SB_SUBSTRING            = 18;
        public static final int SB_TOSTRING             = 19;
        public static final int SB_TRIMTOSIZE           = 20;
        public static final int SB_SERIALIZE            = 21;
        public static final int SB_END                  = 22;

        public static final String[] methodNames = 
        {
            "new",
            "append",
            "capacity",
            "charAt",
            "codePoint",
            "delete",
            "ensureCapacity",
            "getChars",
            "indexOf",
            "insert",
            "lastIndexOf",
            "length",
            "offsetByCodePoints",
            "replace",
            "reverse",
            "setCharAt",
            "setLength",
            "subSequence",
            "substring",
            "toString",
            "trimToSize",
            "serialize"
        };

    }

    private static class StringBufferElem
    {
        StringBuffer sb;
        SafeStringBuffer sfsb;
        int id;
        List opList;

        public StringBufferElem(StringBuffer sb, SafeStringBuffer sfsb, int id)
        {
            this.sb = sb;
            this.sfsb = sfsb;
            this.opList = new ArrayList();
            this.id = id;
        }

        public StringBuffer getStringBuffer() { return sb; }
        public SafeStringBuffer getSafeStringBuffer() { return sfsb; }

        public void addOp(int op) { 
            opList.add(StringBufferMethod.methodNames[op]); 
        }

        public void verify(StringBuffer sb) throws IllegalArgumentException {
            if (!sfsb.verify(sb))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(String s) {
            if (!sfsb.verify(new StringBuffer(s)))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(SafeString sfs) {
            if (!sfsb.verify(new StringBuffer(sfs.toString())))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(StringBufferElem othersbe) 
            throws IllegalArgumentException 
        {
            SafeStringBuffer othersfsb = othersbe.getSafeStringBuffer();
            if (!sfsb.verify(sb) || !sfsb.verify(othersbe.getStringBuffer()) 
                    || !othersfsb.verify(othersbe.getStringBuffer()) 
                    || !othersfsb.verify(sb))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify() throws IllegalArgumentException {
            verify(sb);
        }
       
        public String toString() {
            String str = "id " + id + " string " + sb + " taint " + 
                sb.toString().@internal@taint() + "safestring " + sfsb + " taint " + 
                sfsb.taint() + " operations: ";
            for (int i = 0; i < opList.size(); i++)
                str += (String) opList.get(i);
            return str;
        }
    }

    //[ifJava5+]
    private static class StringBuilderMethod
    {
        public static final int SB_NEW                  = 0;
        public static final int SB_APPEND               = 1;
        public static final int SB_CAPACITY             = 2;
        public static final int SB_CHARAT               = 3;
        public static final int SB_CODEPOINT            = 4;
        public static final int SB_DELETE               = 5;
        public static final int SB_ENSURECAPACITY       = 6;
        public static final int SB_GETCHARS             = 7;
        public static final int SB_INDEXOF              = 8;
        public static final int SB_INSERT               = 9;
        public static final int SB_LASTINDEXOF          = 10;
        public static final int SB_LENGTH               = 11;
        public static final int SB_OFFSETBYCODEPOINTS   = 12;
        public static final int SB_REPLACE              = 13;
        public static final int SB_REVERSE              = 14;
        public static final int SB_SETCHARAT            = 15;
        public static final int SB_SETLENGTH            = 16;
        public static final int SB_SUBSEQUENCE          = 17;
        public static final int SB_SUBSTRING            = 18;
        public static final int SB_TOSTRING             = 19;
        public static final int SB_TRIMTOSIZE           = 20;
        public static final int SB_SERIALIZE            = 21;
        public static final int SB_END                  = 22;

        public static final String[] methodNames = 
        {
            "new",
            "append",
            "capacity",
            "charAt",
            "codePoint",
            "delete",
            "ensureCapacity",
            "getChars",
            "indexOf",
            "insert",
            "lastIndexOf",
            "length",
            "offsetByCodePoints",
            "replace",
            "reverse",
            "setCharAt",
            "setLength",
            "subSequence",
            "substring",
            "toString",
            "trimToSize",
            "serialize",
        };
    }

    private static class StringBuilderElem
    {
        StringBuilder sb;
        SafeStringBuilder sfsb;
        int id;
        List opList;

        public StringBuilderElem(StringBuilder sb, SafeStringBuilder sfsb, 
                int id) 
        {
            this.sb = sb;
            this.sfsb = sfsb;
            this.opList = new ArrayList();
            this.id = id;
        }

        public StringBuilder  getStringBuilder() { return sb; }
        public SafeStringBuilder getSafeStringBuilder() { return sfsb; }

        public void addOp(int op) { 
            opList.add(StringBuilderMethod.methodNames[op]); 
        }

        public void verify(StringBuilder sb) throws IllegalArgumentException {
            if (!sfsb.verify(sb))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(StringBuilderElem othersbe) 
            throws IllegalArgumentException 
        {
            SafeStringBuilder othersfsb = othersbe.getSafeStringBuilder();
            if (!sfsb.verify(sb) || !sfsb.verify(othersbe.getStringBuilder()) 
                    || !othersfsb.verify(othersbe.getStringBuilder()) 
                    || !othersfsb.verify(sb))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(String s) {
            if (!sfsb.verify(new StringBuilder(s)))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(SafeString sfs) {
            if (!sfsb.verify(new StringBuilder(sfs.toString())))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify() throws IllegalArgumentException {
            verify(sb);
        }
       
        public String toString() {
            String str = "id " + id + " string " + sb + " taint " + 
                sb.toString().@internal@taint() + "safestring " + sfsb + " taint " + 
                sfsb.taint() + " operations: ";
            for (int i = 0; i < opList.size(); i++)
                str += (String) opList.get(i);
            return str;
        }
    }
    //[fiJava5+]

    private static class CharSequenceElem 
    {
        CharSequence cs;
        SafeCharSequence sfcs;
        Object origElem;

        public CharSequenceElem(CharSequence cs, SafeCharSequence sfcs, 
                                Object origElem) 
        {
            this.cs = cs;
            this.sfcs = sfcs;
            this.origElem = origElem;
        }

        public CharSequence getCharSequence() { return cs; }

        public SafeCharSequence getSafeCharSequence() { return sfcs; }

        public Object getOrigElem() { return origElem; }

        public void release() {
            if (origElem == null) 
                return;
            else if (origElem instanceof StringElem) {
                origElem = cs = sfcs = null;
                return;
            } else if (origElem instanceof StringBufferElem) {
                stringBufferList.add((StringBufferElem)origElem);
                origElem = cs = sfcs = null;
                return;
            //[ifJava5+]
            } else if (origElem instanceof StringBuilderElem) {
                origElem = cs = sfcs = null;
                stringBuilderList.add((StringBuilderElem)origElem);
                return;
            //[fiJava5+]
            } else
                throw new RuntimeException("unknown charsequence");
        }
    }

    public StringTest(SafeRandom sr) { 
        this.sr = sr; 
        locales = Locale.getAvailableLocales();

        Collection c = Charset.availableCharsets().values();
        charsets = (Charset[]) c.toArray(new Charset[0]);
    }


    //[ifJava5+]
    // List of all Strings, concurrently accessed 
    static List stringList = new CopyOnWriteArrayList();
    //[fiJava5+]
    //[ifJava4]
    static List stringList = new Vector();
    //[fiJava4]

    /* List of all StringBuffers, synchronized */
    static List stringBufferList = new Vector();
    
    /* List of all StringBuilders, synchronized */
    //[ifJava5+]
    static List stringBuilderList= new Vector();

    private int randomCodePoint() {
        return Character.MIN_CODE_POINT + 
            sr.nextInt(Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
    }

    private int[] randomCodePointArray() {
        int len = sr.nextInt(maxlen);
        int[] cp = new int[len];

        for (int i = 0; i < len; i++)
            cp[i] = randomCodePoint(); 
                
        return cp;
    }

    private char randomChar() {
        return Character.toChars(randomCodePoint())[0];
    }

    private char[] randomCharArray() {
        CharArrayWriter cw = new CharArrayWriter();
        int[] cp = randomCodePointArray();

        for (int i = 0 ; i < cp.length; i++) {
            char[] c = Character.toChars(cp[i]);
            cw.write(c, 0, c.length);
        }
        return cw.toCharArray();
    }
    //[fiJava5+]
    //[ifJava4]
    private char randomChar() {
        return (char) sr.nextInt(0xffff + 1);
    }

    private char[] randomCharArray() {
        int len = sr.nextInt(maxlen);
        char[] c = new char[len];

        for (int i = 0; i < len; i++)
            c[i] = randomChar();

        return c;
    }
    //[fiJava4]

    private byte[] randomByteArray() {
        return new String(randomCharArray()).getBytes();
    }

    private byte[] randomAsciiByteArray() {
        int len = sr.nextInt(maxlen);
        byte[] b = new byte[len];

        for (int i = 0; i < len; i++)
                b[i] = (byte) sr.nextInt(128);
        return b;
    }

    private StringElem getRandomStringElem() {
        /* we only add elements to stringList -- do not synchronize here.  */
        if (stringList.size() == 0)
            return null;
        return (StringElem) stringList.get(sr.nextInt(stringList.size()));
    }

    private StringBufferElem removeRandomStringBufferElem() {
        synchronized (stringBufferList) {
            if (stringBufferList.size() == 0) 
                return null;
            return (StringBufferElem) stringBufferList.remove(
                    sr.nextInt(stringBufferList.size()));
        }
    }

    //[ifJava5+]
    private StringBuilderElem removeRandomStringBuilderElem() {
        synchronized(stringBuilderList) {
            if (stringBuilderList.size() == 0)
                return null;
            return (StringBuilderElem) stringBuilderList.remove(
                    sr.nextInt(stringBuilderList.size()));
        }
    }
    //[fiJava5+]

    private CharSequenceElem randomCharSequenceElem() {
        CharSequenceElem cse = null;

        while (cse == null) {
            switch(sr.nextInt(4)) {
                case 0:
                {
                    StringElem se = getRandomStringElem();
                    if (se != null) {
                        CharSequence c;
                        SafeCharSequence sfc;

                        c = (CharSequence) se.getString();
                        sfc = (SafeCharSequence) se.getSafeString();
                        cse = new CharSequenceElem(c, sfc, se);
                    }
                    break;
                }

                case 1:
                {
                    StringBufferElem sbe = removeRandomStringBufferElem();
                    if (sbe != null) {
                        CharSequence c;
                        SafeCharSequence sfc;

                        c = (CharSequence) sbe.getStringBuffer();
                        sfc = (SafeCharSequence) sbe.getSafeStringBuffer();
                        cse = new CharSequenceElem(c, sfc, sbe);
                    }
                    break;
                }

               case 2:
               //[ifJava5+]
               {
                    StringBuilderElem sbe = removeRandomStringBuilderElem();
                    if (sbe != null) {
                        CharSequence c;
                        SafeCharSequence sfc;

                        c = (CharSequence) sbe.getStringBuilder();
                        sfc = (SafeCharSequence) sbe.getSafeStringBuilder();
                        cse = new CharSequenceElem(c, sfc, sbe);
                    }
                    break;
               }
               //[fiJava5+]
               //...else fall through

               case 3: 
               {
                    char[] c = randomCharArray();
                    CharBuffer cb = CharBuffer.wrap(c);
                    SafeCharBuffer sfcb = SafeCharBuffer.wrap((char[])c.clone());
                    cse = new CharSequenceElem(cb, sfcb, null);
                    break;
               }

               default:
                    throw new RuntimeException("switch");
            }
        }

        return cse;
    }

    private Charset randomCharset() {
        /* XXX Not all charsets can handle arbitrary byte sequences
         * For now, we stick with UTF-8
         * return charsets[sr.nextInt(charsets.length)];
         */
        return Charset.forName("UTF-8");
    }

    private Locale randomLocale() {
        return locales[sr.nextInt(locales.length)]; 
    }

    private void testStringNew() throws UnsupportedEncodingException {
        String s = null; 
        OrigString os = null;
        SafeString sfs;
        BitSet b = new BitSet();
        Charset c = randomCharset();

        switch(sr.nextInt(8)) {
            case 0: /* String() */
                s = new String();
                sfs = new SafeString();
                break;

           case 1: /* String (byte[] bytes, ... */
           {
               byte[] bytes = randomByteArray();
               int offset = sr.nextInt(bytes.length);
               int length = bytes.length - offset;
               int type = sr.nextInt(6);

               switch(type)
               {
                   case 0:
                        s = new String(bytes);
                        os = new OrigString(bytes);
                        break;

                   case 1:
                   //[ifJava6]
                        s = new String(bytes, c);
                        os = new OrigString(bytes, c);
                        break;
                   //[fiJava6]
                   // ...else fall through

                   case 2:
                        s = new String(bytes, offset, length);
                        os = new OrigString(bytes, offset, length);
                        break;

                   case 3:
                   //[ifJava6]
                        s = new String(bytes, offset, length, c);
                        os = new OrigString(bytes, offset, length, c);
                        break;
                   //[fiJava6]
                   // ...else fall through

                   case 4:
                        s = new String(bytes, offset, length, c.name());
                        os = new OrigString(bytes, offset, length, 
                                            OrigStringUtil.toOrig(c.name()));
                        break;
                   case 5:
                        s = new String(bytes, c.name());
                        os = new OrigString(bytes, 
                                            OrigStringUtil.toOrig(c.name()));
                        break;

                   default:
                        throw new RuntimeException("switch");
               }


               for (int i = 0; i < s.length(); i++)
                   b.set(i, sr.nextBoolean());
               s = new String(s, new Taint(b, s.length()));
               sfs = new SafeString(os, new SafeTaint(b, s.length()));
               
                break;
           }

           case 2:  /* String(byte[] ascii, ....) */
           {
               byte[] bytes = randomAsciiByteArray();
               /* XXX: offset might potentially break up a surrogate pair */
               int offset = sr.nextInt(bytes.length);
               int length = bytes.length - offset;
               boolean useOffset = sr.nextBoolean();

               if (useOffset) {
                   s = new String(bytes, 0, offset, length);
                   os = new OrigString(bytes, 0, offset, length);
               } else {
                   s = new String(bytes, 0);
                   os = new OrigString(bytes, 0);
               }

               for (int i = 0; i < s.length(); i++)
                   b.set(i, sr.nextBoolean());
               s = new String(s, new Taint(b, s.length()));

               sfs = new SafeString(os, new SafeTaint(b, s.length()));
               break;
           }

           case 3: /* String(char[] ....) */
           {
               char[] chars = randomCharArray();
               /* XXX: offset might potentially break up a surrogate pair */
               int offset = sr.nextInt(chars.length);
               int length = chars.length - offset;
               boolean useOffset = sr.nextBoolean();

               if (useOffset) {
                   s = new String(chars, offset, length);
                   os = new OrigString(chars ,offset, length);
               } else {
                   s = new String(chars);
                   os = new OrigString(chars);
               }

               for (int i = 0; i < s.length(); i++)
                   b.set(i, sr.nextBoolean());
               s = new String(s, new Taint(b, s.length()));

               sfs = new SafeString(os, new SafeTaint(b, s.length()));
               break;
           }


           case 4: /* String(int[]....) */
           {
               //[ifJava5+]
               int[] cps = randomCodePointArray();
               // XXX: offset might potentially break up a surrogate pair 
               int offset = sr.nextInt(cps.length);
               int length = cps.length - offset;

               s = new String(cps, offset, length);
               os = new OrigString(cps, offset, length);

               for (int i = 0; i < s.length(); i++)
                   b.set(i, sr.nextBoolean());
               s = new String(s, new Taint(b, s.length()));

               sfs = new SafeString(os, new SafeTaint(b, s.length()));
               break;
               //[fiJava5+]
               // ...else fall through
           }

           case 5: /* String(String...) */
           {
               StringElem otherse;
               otherse = getRandomStringElem();
               if (otherse == null) return; 

               s = new String(otherse.getString());
               sfs = new SafeString(otherse.getSafeString());
               break;
           }

           case 6: /* String(StringBuilder...) */
           {
               //[ifJava5+]
               StringBuilderElem othersbe;
               othersbe = removeRandomStringBuilderElem();
               if (othersbe == null) return;

               s = new String(othersbe.getStringBuilder());
               sfs = new SafeString(othersbe.getSafeStringBuilder());
               stringBuilderList.add(othersbe);
               break;
               //[fiJava5+]
               //...else fall through
           }

           case 7: /* String(StringBuffer...) */
           {
               StringBufferElem othersbe;
               othersbe = removeRandomStringBufferElem();
               if (othersbe == null) return;

               s = new String(othersbe.getStringBuffer());
               sfs = new SafeString(othersbe.getSafeStringBuffer());
               stringBufferList.add(othersbe);
               break;
           }


           default:
                throw new RuntimeException("switch error");
        }

        StringElem se = new StringElem(s, sfs, sr.nextInt());

        se.verify();
        se.addOp(StringMethod.S_NEW);
        stringList.add(se);
    }

    private void testStringCharAt(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int index;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(s.length());

        if (index < 0 || index >= s.length()) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.charAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.charAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.charAt(index) != sfs.charAt(index)) 
                throw new IllegalArgumentException(se.toString());
        }
    }

    //[ifJava5+]
    private void testStringCodePointAt(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int index;

        if (sr.nextInt(16) == 0)
            index = sr.nextInt();
        else
            index = sr.nextInt(s.length());

        if (index < 0 || index >= s.length()) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.codePointAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.codePointAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.codePointAt(index) != sfs.codePointAt(index)) 
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringCodePointBefore(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int index;

        if (sr.nextInt(16) == 0)
            index = sr.nextInt();
        else
            index = 1 + sr.nextInt(s.length());

        if (index < 1 || index > s.length()) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.codePointBefore(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.codePointBefore(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.codePointBefore(index) != sfs.codePointBefore(index)) 
                throw new IllegalArgumentException(se.toString());
        }

    }

    private void testStringCodePointCount(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int beginIndex, endIndex;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(s.length());
            endIndex = beginIndex + sr.nextInt(s.length() + 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > s.length() || beginIndex > endIndex) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.codePointCount(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.codePointCount(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.codePointCount(beginIndex, endIndex) != 
                    sfs.codePointCount(beginIndex, endIndex)) 
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringCodePoint(StringElem se) {
        switch (sr.nextInt(3)) {
            case 0:
                testStringCodePointAt(se);
                break;
            case 1:
                testStringCodePointBefore(se);
                break;
            case 2:
                testStringCodePointCount(se);
                break;
            default:
                throw new RuntimeException("switched failed");
        }
    }
    //[fiJava5+]

    private void testStringConcat(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        StringElem otherse;

        SafeString newsfs;
        String news;
        StringElem newse;

        otherse = getRandomStringElem();
        news = s.concat(otherse.getString());
        newsfs = sfs.concat(otherse.getSafeString());

        otherse.verify();

        if (!newsfs.verify(news) || news.length() != s.length() + 
            otherse.getString().length())
        throw new IllegalArgumentException("Concat verify failed");

        Taint newt = news.@internal@taint();
        Taint othert = otherse.getString().@internal@taint();

        if (newt == null)
            newt = new Taint(false, news.length());
        if (othert == null)
            othert = new Taint(false, otherse.getString().length());

        for (int i = s.length(); i < news.length(); i++) {
            if (news.charAt(i) != 
                  otherse.getString().charAt(i - se.getString().length())
                || newt.get(i) != othert.get(i - se.getString().length()))
                throw new IllegalArgumentException("Concat taint/data failed");
        }

        newse = new StringElem(news, newsfs, sr.nextInt());
        newse.addOp(StringMethod.S_NEW);
        newse.verify();
        stringList.add(newse);
    }

    private void testStringCompareTo(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        StringElem otherse = getRandomStringElem();
        boolean ignoreCase = sr.nextBoolean();

        if (ignoreCase) {
            if (s.compareToIgnoreCase(otherse.getString()) !=
                    sfs.compareToIgnoreCase(otherse.getSafeString()))
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.compareTo(otherse.getString()) != 
                    sfs.compareTo(otherse.getSafeString()))
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringContains(StringElem se) {
        //[ifJava5+]
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        CharSequenceElem cse = randomCharSequenceElem();

        if (s.contains(cse.getCharSequence()) != 
                sfs.contains(cse.getSafeCharSequence()))
            throw new IllegalArgumentException(se.toString());
        cse.release();
        //[fiJava5+]
    }

    private void testStringContentEquals(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        StringBufferElem sbe;
        CharSequenceElem cse;

        if (sr.nextBoolean()) {
            sbe = removeRandomStringBufferElem();
            if (sbe == null)
                return;
            if (s.contentEquals(sbe.getStringBuffer()) != 
                    sfs.contentEquals(sbe.getSafeStringBuffer()))
                throw new IllegalArgumentException(se.toString());
        }
        //[ifJava5+]
        else {
            cse = randomCharSequenceElem();
            if (cse == null)
                return;
            if (s.contentEquals(cse.getCharSequence()) != 
                    sfs.contentEquals(cse.getSafeCharSequence()))
                throw new IllegalArgumentException(se.toString());
            cse.release();
        }
        //[fiJava5+]
    }

    private void testStringCopyValueOf() {
        String s;
        SafeString sfs;
        char[] c = randomCharArray();
        int offset, count;

            offset = sr.nextInt(c.length);
            count = sr.nextInt(c.length - offset);

        if (sr.nextBoolean()) {
            s = String.copyValueOf(c);
            sfs = SafeString.copyValueOf(c);
        } else {
            s = String.copyValueOf(c, offset, count);
            sfs = SafeString.copyValueOf(c, offset, count);
        }

        sfs.verify(s);
    }

    private void testStringSubstring(StringElem se) {
        String s = se.getString();
        int beginIndex, endIndex;

        StringElem subse;
        String subs;
        SafeString subsfs;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(s.length());
            endIndex = beginIndex + sr.nextInt(s.length()+ 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > s.length() || beginIndex > endIndex) {
            /* Exception time */
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                subs = s.substring(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                subsfs = se.getSafeString().substring(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            if (endIndex == s.length() && sr.nextBoolean()) {
                subs = s.substring(beginIndex);
                subsfs = se.getSafeString().substring(beginIndex);
            } else {
                subs = s.substring(beginIndex, endIndex);
                subsfs = se.getSafeString().substring(beginIndex, endIndex);
            }

            subsfs.verify(subs);

            Taint t = s.@internal@taint();
            Taint subt = subs.@internal@taint();

            if (t == null)
                t = new Taint(false, s.length());
            if (subt == null)
                subt = new Taint(false, subs.length());

            for (int i = beginIndex; i < endIndex; i++) 
                if (s.charAt(i) != subs.charAt(i - beginIndex) 
                        || t.get(i) != subt.get(i-beginIndex))
                    throw new IllegalArgumentException("string taint/data bad");

            subse = new StringElem(subs, subsfs, sr.nextInt());
            subse.verify();
            subse.addOp(StringMethod.S_SUBSTRING);
            stringList.add(subse);
        }
    }

    private void testStringSubSequence(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int beginIndex, endIndex;

        CharSequence subs;
        SafeCharSequence subsfs;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(s.length());
            endIndex = beginIndex + sr.nextInt(s.length()+ 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > s.length() || beginIndex > endIndex) {
            /* Exception time */
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                subs = s.subSequence(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                subsfs = (SafeCharSequence) sfs.subSequence(beginIndex, 
                                                            endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            subs = s.subSequence(beginIndex, endIndex);
            subsfs = (SafeCharSequence) sfs.subSequence(beginIndex, endIndex);

            subsfs.toSafeString().verify(subs.toString());

            Taint t = s.@internal@taint();
            Taint subt = subs.toString().@internal@taint();

            if (t == null)
                t = new Taint(false, s.length());
            if (subt == null)
                subt = new Taint(false, subs.length());

            for (int i = beginIndex; i < endIndex; i++) {
                if (s.charAt(i) != subs.charAt(i - beginIndex) 
                        || t.get(i) != subt.get(i-beginIndex))
                    throw new IllegalArgumentException("string taint/data bad");

            }
        }
    }

    private void testStringToCharArray(StringElem se) {
        if (!Arrays.equals(se.getString().toCharArray(),
                    se.getSafeString().toCharArray()))
            throw new IllegalArgumentException(se.toString());
    }

    private void testStringToString(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();

        sfs.verify(s.toString());
        sfs.verify(sfs.toString());

        if (s.toString() != s)
            throw new IllegalArgumentException(se.toString());
    }

    private void testStringTrim(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();

        String trims = s.trim();
        SafeString trimsfs = sfs.trim();
        StringElem trimse = new StringElem(trims, trimsfs, sr.nextInt());

        trimse.verify();
        trimse.addOp(StringMethod.S_TRIM);
        stringList.add(trimse);

    }

    //[ifJava5+]
    private void verifyStringCase(String orig, String result, Locale l,
                                  boolean isUpper) {
        int olen = orig.length(),
            rlen = result.length();
        int ocount, rcount;
        int i, j;
        Taint otaint = orig.@internal@taint();
        Taint rtaint = result.@internal@taint();

        if (otaint == null)
            otaint = new Taint(false, orig.length());
        if (rtaint == null)
            rtaint = new Taint(false, result.length());

        for (i = 0, j = 0; i < olen && j < rlen ; i += ocount, j += rcount)
        {
            int ochar = orig.codePointAt(i);
            int rchar; 
            boolean tainted = false, 
                    changed = false;

            ocount = Character.charCount(ochar);
            if (isUpper)
                rchar = String.@internal@toUpperCaseEx(orig, i, l);
            else
                rchar = String.@internal@toLowerCaseEx(orig, i, l);

            if (!String.@internal@isError(rchar)) {
                rcount = Character.charCount(rchar);
                if (result.codePointAt(j) != rchar)
                    throw new IllegalArgumentException("conversion error");
            } else {
                 char[] rtmp;

                 if (isUpper) {
                     rtmp = String.@internal@toUpperCaseCharArray(orig, i, l);
                     rcount = rtmp.length;
                 } else {
                     rtmp = String.@internal@toLowerCaseCharArray(orig, i, l);
                     rcount = rtmp.length;
                 }

                 for (int m = 0; m < rtmp.length; m++)
                     if (result.charAt(j+m) != rtmp[m])
                         throw new IllegalArgumentException("conversion error");
             }

            if (ocount != rcount) changed = true;

            for (int k = 0; !changed && k < ocount; k++)
                if (orig.charAt(i+k) != result.charAt(j+k))
                    changed = true;

            for (int k = 0 ;k < ocount; k++) 
                if (otaint.get(i+k)) tainted = true;

            if (!changed) {
                for (int m = 0; m < ocount; m++) 
                    if (otaint.get(i+m) != rtaint.get(j+m))
                        throw new IllegalArgumentException("taint error");
            } else {
                for (int m = 0; m < rcount; m++) 
                    if (rtaint.get(j+m) != tainted) 
                        throw new IllegalArgumentException("taint error");
            }
        }

        if (i != olen || j != rlen)
            throw new IllegalArgumentException("toXCase length error");
    }

    private void verifySafeStringCase(SafeString orig, SafeString result, 
                                      Locale l, boolean isUpper) 
    {
        int olen = orig.length(),
            rlen = result.length();
        int ocount, rcount;
        int i, j;
        SafeTaint otaint = orig.taint();
        SafeTaint rtaint = result.taint();

        for (i = 0, j = 0; i < olen && j < rlen ; i += ocount, j += rcount)
        {
            int ochar = orig.codePointAt(i);
            int rchar; 
            boolean tainted = false,
                    changed = false;

            ocount = Character.charCount(ochar);
            if (isUpper)
                rchar = orig.toUpperCaseEx(i, l);
            else
                rchar = orig.toLowerCaseEx(i, l);

            if (!SafeString.isError(rchar)) {
                rcount = Character.charCount(rchar);
                if (result.codePointAt(j) != rchar)
                    throw new IllegalArgumentException("conversion error");
            } else {
                 char[] rtmp;

                 if (isUpper) {
                     rtmp = orig.toUpperCaseCharArray(i, l);
                     rcount = rtmp.length;
                 } else {
                     rtmp = orig.toLowerCaseCharArray(i, l);
                     rcount = rtmp.length;
                 }

                 for (int m = 0; m < rtmp.length; m++)
                     if (result.charAt(j+m) != rtmp[m])
                         throw new IllegalArgumentException("conversion error");
            }

            if (ocount != rcount) changed = true;

            for (int k = 0; !changed && k < ocount; k++)
                if (orig.charAt(i+k) != result.charAt(j+k))
                    changed = true;

            for (int k = 0 ;k < ocount; k++) 
                if (otaint.get(i+k)) tainted = true;

            if (!changed) {
                for (int m = 0; m < ocount; m++) 
                    if (otaint.get(i+m) != rtaint.get(j+m))
                        throw new IllegalArgumentException("taint error");
            } else {
                for (int m = 0; m < rcount; m++) 
                    if (rtaint.get(j+m) != tainted) 
                        throw new IllegalArgumentException("taint error");
            }
        }

        if (i != olen || j != rlen)
            throw new IllegalArgumentException("toXCase length error");
    }
    //[fiJava5+]
    //[ifJava4]
    private void verifyStringCase(String orig, String result, Locale l,
                                  boolean isUpper) {
        int olen = orig.length(),
            rlen = result.length();
        int rcount, i, j;

        Taint otaint = orig.@internal@taint();
        Taint rtaint = result.@internal@taint();

        if (otaint == null)
            otaint = new Taint(false, orig.length());
        if (rtaint == null)
            rtaint = new Taint(false, result.length());

        if (isUpper == false) {
            if (!otaint.asBitSet().equals(rtaint.asBitSet()) || olen != rlen)
                throw new IllegalArgumentException("Unexpected value for " +
                        "toLowerCase: orig " + orig + " result " + result);
            return;
        }

        for (i = 0, j = 0; i < olen && j < rlen ; i++, j += rcount)
        {
            char ochar = orig.charAt(i);
            char[] rchars = new char[] {String.@internal@toUpperCaseEx(ochar)};
            boolean tainted = otaint.get(i),
                    changed = false;

            if ("tr".equals(l.getLanguage()) 
                    && (ochar == 'i' || ochar == '\u0131')) {
                if (ochar == 'i')
                    rchars[0] = '\u0130';
                else
                    rchars[0] = 'I';
                rcount = 1;
            } else if (!String.@internal@isError(rchars[0])) {
                rcount = 1; 
            } else {
                rchars = String.@internal@toUpperCaseCharArray(ochar);
                rcount = rchars.length;
            }

            for (int m = 0; m < rcount; m++) {
                if (result.charAt(j+m) != rchars[m])
                    throw new IllegalArgumentException("conversion error");
                if (rtaint.get(j+m) != tainted) 
                        throw new IllegalArgumentException("taint error");
            }
        }

        if (i != olen || j != rlen)
            throw new IllegalArgumentException("toXCase length error");
    }

    private void verifySafeStringCase(SafeString orig, SafeString result, 
                                      Locale l, boolean isUpper) 
    {
        int olen = orig.length(),
            rlen = result.length();
        int rcount, i, j;

        SafeTaint otaint = orig.taint();
        SafeTaint rtaint = result.taint();

        if (isUpper == false) {
            if (!otaint.asBitSet().equals(rtaint.asBitSet()) || olen != rlen)
                throw new IllegalArgumentException("Unexpected value for " +
                        "toLowerCase: orig " + orig + " result " + result);
            return;
        }

        for (i = 0, j = 0; i < olen && j < rlen ; i++, j += rcount)
        {
            char ochar = orig.charAt(i);
            char[] rchars = new char[] {SafeString.toUpperCaseEx(ochar)};
            boolean tainted = otaint.get(i),
                    changed = false;

            if ("tr".equals(l.getLanguage()) 
                    && (ochar == 'i' || ochar == '\u0131')) {
                if (ochar == 'i')
                    rchars[0] = '\u0130';
                else
                    rchars[0] = 'I';
                rcount = 1;
            } else if (!SafeString.isError(rchars[0])) {
                rcount = 1; 
            } else {
                rchars = SafeString.toUpperCaseCharArray(ochar);
                rcount = rchars.length;
            }

            for (int m = 0; m < rcount; m++) {
                if (result.charAt(j+m) != rchars[m])
                    throw new IllegalArgumentException("conversion error");
                if (rtaint.get(j+m) != tainted) 
                        throw new IllegalArgumentException("taint error");
            }
        }

        if (i != olen || j != rlen)
            throw new IllegalArgumentException("toXCase length error");
    }
    //[fiJava4]

    private void testStringToLowerCase(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        Locale l;

        String lowers;
        SafeString lowersfs;
        StringElem lowerse;

        if (sr.nextBoolean()) { 
            l = randomLocale();
            lowers = s.toLowerCase(l);
            lowersfs = sfs.toLowerCase(l);
        } else {
            l = Locale.getDefault();
            lowers = s.toLowerCase();
            lowersfs = sfs.toLowerCase();
        }

        lowerse = new StringElem(lowers, lowersfs, sr.nextInt());
        lowerse.verify();

        verifyStringCase(s, lowers, l, false);
        verifySafeStringCase(sfs, lowersfs, l, false);

        lowerse.addOp(StringMethod.S_TOLOWERCASE);
        stringList.add(lowerse);
    }

    private void testStringToUpperCase(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        Locale l;

        String uppers;
        SafeString uppersfs;
        StringElem upperse;

        if (sr.nextBoolean()) {
            l = randomLocale();
            uppers = s.toUpperCase(l);
            uppersfs = sfs.toUpperCase(l);
        } else {
            l = Locale.getDefault();
            uppers = s.toUpperCase();
            uppersfs = sfs.toUpperCase();
        }

        upperse = new StringElem(uppers, uppersfs, sr.nextInt());
        upperse.verify();

        verifyStringCase(s, uppers, l, true);
        verifySafeStringCase(sfs, uppersfs, l, true);

        upperse.addOp(StringMethod.S_TOUPPERCASE);
        stringList.add(upperse);
    }

    private void testStringValueOf() {
        String s = null; 
        SafeString sfs = null;
        StringElem se;

        switch(sr.nextInt(9)) {
            case 0:
            {
                boolean b = sr.nextBoolean();
                s = String.valueOf(b);
                sfs = SafeString.valueOf(b);
                break;
            }

            case 1:
            {
                char c = randomChar();
                s = String.valueOf(c);
                sfs = SafeString.valueOf(c);
                break;
            }

            case 2:
            {
                char[] c = randomCharArray();
                s = String.valueOf(c);
                sfs = SafeString.valueOf(c);
                break;
            }

            case 3:
            {
                char[] c = randomCharArray();
                int offset, len;

                if (sr.nextInt(16) == 0) {
                    offset = sr.nextInt();
                    len = sr.nextInt();
                } else {
                    offset = sr.nextInt(c.length);
                    len = sr.nextInt(c.length - offset);
                }

                if (offset < 0 || len < 0 || offset + len > c.length
                        || offset + len < 0) 
                {
                        /* Exception time */
                        boolean caught = false;

                        try {
                            s = String.valueOf(c, offset, len);
                        } catch (IndexOutOfBoundsException e) {
                            caught = true;
                        }

                        if (!caught) 
                            throw new IllegalArgumentException(s);

                        caught = false;
                        try {
                            sfs = SafeString.valueOf(c, offset, len);
                        } catch (IndexOutOfBoundsException e) {
                            caught = true;
                        }
                        if (!caught)
                            throw new IllegalArgumentException(s);
                } else {
                    s = String.valueOf(c, offset, len);
                    sfs = SafeString.valueOf(c, offset, len);
                }
                        
                break;
            }

            case 4:
            {
                double d = sr.nextDouble();
                s = String.valueOf(d);
                sfs = SafeString.valueOf(d);
                break;
            }

            case 5:
            {
                float f = sr.nextFloat();
                s = String.valueOf(f);
                sfs = SafeString.valueOf(f);
                break;
            }

            case 6:
            {
                int i = sr.nextInt();
                s = String.valueOf(i);
                sfs = SafeString.valueOf(i);
                break;
            }

            case 7:
            {
                long l = sr.nextLong();
                s = String.valueOf(l);
                sfs = SafeString.valueOf(l);
                break;
            }

            case 8:
            {
                StringElem otherse = getRandomStringElem();
                s = String.valueOf(otherse.getString());
                sfs = SafeString.valueOf(otherse.getSafeString());
                break;
            }

            default:
                throw new RuntimeException("switch");
        }

        if (s == null || sfs == null) {
            if (sfs != null || s != null) 
                throw new IllegalArgumentException("nullity");
            return;
        }

        se = new StringElem(s, sfs, sr.nextInt());
        se.verify(); 
        se.addOp(StringMethod.S_VALUEOF);
        stringList.add(se);
    }

    private void testStringEndsWith(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();

        StringElem otherse = getRandomStringElem();
        String others = otherse.getString();
        SafeString othersfs = otherse.getSafeString();

        otherse.verify();

        if (s.endsWith(others) != sfs.endsWith(othersfs))
            throw new IllegalArgumentException(se.toString());
    }

    private void testStringEquals(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();

        StringElem otherse = getRandomStringElem();
        String others = otherse.getString();
        SafeString othersfs = otherse.getSafeString();

        boolean ignoreCase = sr.nextBoolean();

        otherse.verify();

        if (ignoreCase) {
            if (s.equalsIgnoreCase(others) != sfs.equalsIgnoreCase(othersfs))
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.equals(others) != sfs.equals(othersfs))
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringDeprecatedGetBytes(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int srcBegin, srcEnd, dstBegin;
        byte[] b, sfb;
        int len = sr.nextInt(s.getBytes().length);

        b = new byte[len];
        sfb = new byte[len];

        if (sr.nextInt(16) == 0) {
            srcBegin = sr.nextInt();
            srcEnd = sr.nextInt();
            dstBegin = sr.nextInt();
        } else {
            srcBegin = sr.nextInt(s.length());
            srcEnd = srcBegin + 
                sr.nextInt(Math.min(s.length() - srcBegin, len));
            dstBegin = sr.nextInt(len - (srcEnd - srcBegin) + 1);
        }

        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > s.length()
                || dstBegin < 0 || dstBegin + (srcEnd - srcBegin) > len) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.getBytes(srcBegin, srcEnd, b, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.getBytes(srcBegin, srcEnd, sfb, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
                s.getBytes(srcBegin, srcEnd, b, dstBegin);
                sfs.getBytes(srcBegin, srcEnd, sfb, dstBegin);

                if (!Arrays.equals(b, sfb))
                    throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringGetBytes(StringElem se) 
        throws UnsupportedEncodingException
    {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        byte[] b = null, 
               sfb = null;
        Charset c = randomCharset();

        switch (sr.nextInt(4)) {
            case 0:
                b = s.getBytes();
                sfb = sfs.getBytes();
                break;

            case 1:
            //[ifJava6]
                b = s.getBytes(c);
                sfb = sfs.getBytes(c);
                break;
            //[fiJava6]
            // ...else fall through

            case 2:
                b = s.getBytes(c.name());
                sfb = sfs.getBytes(c.name());
                break;

            case 3:
                testStringDeprecatedGetBytes(se);
                return;

            default:
                throw new RuntimeException("switch");
        }

        if (!Arrays.equals(b,sfb))
            throw new IllegalArgumentException(se.toString());
    }

    
    private void testStringGetChars(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int srcBegin, srcEnd, dstBegin;
        char[] c, sfc;
        int len = sr.nextInt(s.length());

        c = new char[len];
        sfc = new char[len];

        if (sr.nextInt(16) == 0) {
            srcBegin = sr.nextInt();
            srcEnd = sr.nextInt();
            dstBegin = sr.nextInt();
        } else {
            srcBegin = sr.nextInt(s.length());
            srcEnd = srcBegin + 
                sr.nextInt(Math.min(s.length() - srcBegin, len));
            dstBegin = sr.nextInt(len - (srcEnd - srcBegin) + 1);
        }

        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > s.length()
                || dstBegin < 0 || dstBegin + (srcEnd - srcBegin) > len) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.getChars(srcBegin, srcEnd, c, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.getChars(srcBegin, srcEnd, sfc, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
                s.getChars(srcBegin, srcEnd, c, dstBegin);
                sfs.getChars(srcBegin, srcEnd, sfc, dstBegin);

                if (!Arrays.equals(c, sfc))
                    throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringHashCode(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();

        if (s.hashCode() != sfs.hashCode())
            throw new IllegalArgumentException(se.toString());

    }

    private void testStringIndexOfChar(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int ch;
        int fromIndex;
        boolean useIndex = sr.nextBoolean();

        //[ifJava5+] 
        ch = randomCodePoint();
        //[fiJava5+]
        //[ifJava4]
        ch = randomChar();
        //[fiJava4]

        if (sr.nextInt(16) == 0)
            fromIndex = sr.nextInt();
        else
            fromIndex = sr.nextInt(s.length());
        

        if (useIndex) {
            if (s.indexOf(ch, fromIndex) != sfs.indexOf(ch, fromIndex))
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.indexOf(ch) != sfs.indexOf(ch))
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringIndexOfString(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        StringElem otherse;
        int fromIndex;
        boolean useIndex = sr.nextBoolean();

        otherse = getRandomStringElem();
        otherse.verify();

        if (sr.nextInt(16) == 0)
            fromIndex = sr.nextInt();
        else
            fromIndex = sr.nextInt(s.length());

        if (useIndex) {
            if (s.indexOf(otherse.getString(), fromIndex) !=
                    sfs.indexOf(otherse.getSafeString(), fromIndex))
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.indexOf(otherse.getString()) != 
                    sfs.indexOf(otherse.getSafeString()))
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringIndexOf(StringElem se) {
        if (sr.nextBoolean())
            testStringIndexOfChar(se);
        else
            testStringIndexOfString(se);
    }


    //[ifJava6]
    private void testStringIsEmpty(StringElem se) {
        if (se.getString().isEmpty() != se.getSafeString().isEmpty())
            throw new IllegalArgumentException(se.toString());
    }
    //[fiJava6]

    private void testStringLastIndexOfChar(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int ch;
        int fromIndex;
        boolean useIndex = sr.nextBoolean();

        //[ifJava5+]
        ch = randomCodePoint();
        //[fiJava5+]
        //[ifJava4]
        ch = randomChar();
        //[fiJava4]

        if (sr.nextInt(16) == 0)
            fromIndex = sr.nextInt();
        else
            fromIndex = sr.nextInt(s.length());
        

        if (useIndex) {
            if (s.lastIndexOf(ch, fromIndex) != sfs.lastIndexOf(ch, fromIndex))
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.lastIndexOf(ch) != sfs.lastIndexOf(ch))
                throw new IllegalArgumentException(se.toString());
        }
    }


    private void testStringLastIndexOfString(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        StringElem otherse;
        int fromIndex;
        boolean useIndex = sr.nextBoolean();

        otherse = getRandomStringElem();
        otherse.verify();

        if (sr.nextInt(16) == 0)
            fromIndex = sr.nextInt();
        else
            fromIndex = sr.nextInt(s.length());

        if (useIndex) {
            if (s.lastIndexOf(otherse.getString(), fromIndex) !=
                    sfs.lastIndexOf(otherse.getSafeString(), fromIndex))
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.lastIndexOf(otherse.getString()) != 
                    sfs.lastIndexOf(otherse.getSafeString()))
                throw new IllegalArgumentException(se.toString());
        }
    }

    private void testStringLastIndexOf(StringElem se) {
        if (sr.nextBoolean())
            testStringLastIndexOfChar(se);
        else
            testStringLastIndexOfString(se);
    }

    private void testStringLength(StringElem se) {
        if (se.getString().length() != se.getSafeString().length())
            throw new IllegalArgumentException(se.toString());
    }

    //[ifJava5+]
    private void testStringOffsetByCodePoints(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        int index;
        int codePointOffset;

        if (sr.nextInt(16) == 0) {
            index = sr.nextInt();
            codePointOffset = sr.nextInt();
        } else {
            index = sr.nextInt(s.length());
            boolean sign = sr.nextBoolean();

            if (sign)
                codePointOffset = -sr.nextInt(s.codePointCount(0, index));
            else
                codePointOffset = 
                    sr.nextInt(s.codePointCount(index,s.length()));
        }

        if (index < 0 || index > s.length() || (codePointOffset > 0 &&
                   codePointOffset >  s.codePointCount(index, s.length()))
               || (codePointOffset < 0 && 
                   -codePointOffset > s.codePointCount(0,index))) {
            SafeString oldsfs = new SafeString(se.getSafeString());
            boolean caught = false;

            try {
                s.offsetByCodePoints(index, codePointOffset);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(se.toString());

            caught = false;
            try {
                sfs.offsetByCodePoints(index, codePointOffset);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfs.verify(s)) 
                throw new IllegalArgumentException(se.toString());
        } else {
            if (s.offsetByCodePoints(index, codePointOffset) != 
                    sfs.offsetByCodePoints(index, codePointOffset)) 
                throw new IllegalArgumentException(se.toString());
        }
    }
    //[fiJava5+]

    private void testStringStartsWith(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();
        StringElem otherse = getRandomStringElem();

        if (s.startsWith(otherse.getString()) != 
                    sfs.startsWith(otherse.getSafeString()))
                throw new IllegalArgumentException(se.toString());
    }

    private void testStringIsTainted(StringElem se) {
        String s = se.getString();
        SafeString sfs = se.getSafeString();

        if (s.@internal@isTainted() != sfs.isTainted())
            throw new IllegalArgumentException(se.toString());
    }

    private void testStringSerialize(StringElem se) 
        throws IOException, ClassNotFoundException 
    {
        String s = se.getString();
        SafeString sfs = new SafeString(se.getSafeString().origString(),
                                        new SafeTaint(false, s.length()));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bout);

        os.writeObject(s);
        os.flush();
        os.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream is = new ObjectInputStream(bin);

        s = (String) is.readObject();
        is.close();
        bin.close();
        bout.close();

        if (s.@internal@isTainted()) 
            throw new RuntimeException("Corrupted Serialization");

        StringElem newSe = new StringElem(s, sfs, sr.nextInt());
        newSe.verify();
        se.addOp(StringMethod.S_SERIALIZE);
        newSe.addOp(StringMethod.S_SERIALIZE);
        stringList.add(newSe);
    }

    public void testString() 
        throws IllegalArgumentException, UnsupportedEncodingException,
               IOException, ClassNotFoundException
    {
        int op = sr.nextInt(StringMethod.S_END);
        StringElem se = null;

        if (stringList.size() < threads * 4) op = StringMethod.S_NEW;

        if (op != StringMethod.S_NEW)  {
            se = (StringElem) stringList.get(sr.nextInt(stringList.size()));
            se.verify();
            se.addOp(op);
        }


        switch(op) {
            case StringMethod.S_NEW:
                testStringNew();
                break;

            case StringMethod.S_CHARAT:
                testStringCharAt(se);
                break;

            case StringMethod.S_CODEPOINT:
                //[ifJava5+]
                testStringCodePoint(se);
                break;
                //[fiJava5+]
                //...else fall through

             case StringMethod.S_COMPARETO:
                testStringCompareTo(se);
                break;

            case StringMethod.S_CONCAT:
                testStringConcat(se);
                break;

            case StringMethod.S_CONTAINS:
                testStringContains(se);
                break;

            case StringMethod.S_CONTENTEQUALS:
                testStringContentEquals(se);
                break;

            case StringMethod.S_COPYVALUEOF:
                testStringCopyValueOf();
                break;

            case StringMethod.S_ENDSWITH:
                testStringEndsWith(se);
                break;

            case StringMethod.S_EQUALS:
                testStringEquals(se);
                break;

            case StringMethod.S_GETBYTES:
                testStringGetBytes(se);
                break;

            case StringMethod.S_GETCHARS:
                testStringGetChars(se);
                break;

            case StringMethod.S_HASHCODE:
                testStringHashCode(se);
                break;

            case StringMethod.S_INDEXOF:
                testStringIndexOf(se);
                 break;

            case StringMethod.S_ISEMPTY:
                 //[ifJava6]
                 testStringIsEmpty(se);
                 break;
                 //[fiJava6]
                 //...else fall through

            case StringMethod.S_LASTINDEXOF:
                 testStringLastIndexOf(se);
                 break;

            case StringMethod.S_LENGTH:
                 testStringLength(se);
                 break;

            case StringMethod.S_OFFSETBYCODEPOINTS:
                 //XXX This method is actually buggy in Java 5, test only in
                 //Java 6
                 //[ifJava6]
                 testStringOffsetByCodePoints(se);
                 break;
                 //[fiJava6]
                 //...else fall through

            case StringMethod.S_STARTSWITH:
                 testStringStartsWith(se);
                 break;

            case StringMethod.S_SUBSEQUENCE:
                 testStringSubSequence(se);
                 break;

            case StringMethod.S_SUBSTRING:
                testStringSubstring(se);
                break;

            case StringMethod.S_TOCHARARRAY:
                testStringToCharArray(se);
                break;

            case StringMethod.S_TOLOWERCASE:
                testStringToLowerCase(se);
                break;

            case StringMethod.S_TOSTRING:
                testStringToString(se);
                break;

            case StringMethod.S_TOUPPERCASE:
                testStringToUpperCase(se);
                break;

            case StringMethod.S_TRIM:
                testStringTrim(se);
                break;

            case StringMethod.S_VALUEOF:
                testStringValueOf();
                break;

            case StringMethod.S_ISTAINTED:
                testStringIsTainted(se);
                break;

            case StringMethod.S_SERIALIZE:
                testStringSerialize(se);
                break;

            default:
                throw new RuntimeException("switch");
        }

        if (op != StringMethod.S_NEW)
            se.verify();
    }

    //[ifJava5+]
    private void testStringBuilderNew() {
        StringBuilder sb = null;
        SafeStringBuilder sfsb = null;
        StringBuilderElem sbe;

        switch(sr.nextInt(4)) {
            case 0: // StringBuilder() 
                sb = new StringBuilder();
                sfsb = new SafeStringBuilder();
                break;

            case 1: // StringBuilder(int capacity) 
            {
                int capacity = sr.nextInt(maxlen);
                sb = new StringBuilder(capacity);
                sfsb = new SafeStringBuilder(capacity);
                break;
            }

            case 2: // StringBuilder(String s) 
            {
                StringElem se = getRandomStringElem();
                if (se == null) return; 

                sb = new StringBuilder(se.getString());
                sfsb = new SafeStringBuilder(se.getSafeString());
                break;
            }

            case 3: // StringBuilder(CharSequence seq) 
            {
                CharSequenceElem cse = randomCharSequenceElem();
                sb = new StringBuilder(cse.getCharSequence());
                sfsb = new SafeStringBuilder(cse.getSafeCharSequence());
                cse.release();
                break;
            }

            default:
                throw new RuntimeException("switch");
        }

        sbe = new StringBuilderElem(sb, sfsb, sr.nextInt());
        sbe.addOp(StringBuilderMethod.SB_NEW);
        sbe.verify();
        stringBuilderList.add(sbe);
    }

    private void verifyStringBuilderAppendString(StringBuilder sb, 
            int oldLength, String str) 
    {
       Taint sbt = sb.toString().@internal@taint();
       Taint strt = str.@internal@taint();

       if (sbt == null)
           sbt = new Taint(false, sb.length());
       if (strt == null)
           strt = new Taint(false, str.length());

       if (sb.length() != oldLength + str.length())
           throw new IllegalArgumentException("append string length check:"
                   + sb + "appended: " + str);

       for (int i = oldLength; i < sb.length(); i++) 
           if (sb.charAt(i) != str.charAt(i - oldLength) || 
                   sbt.get(i) != strt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void verifyStringBuilderAppendStringBuffer(StringBuilder sb, 
            int oldLength, StringBuffer othersb)
    {
       Taint sbt = sb.toString().@internal@taint();
       Taint othersbt = othersb.toString().@internal@taint();

       if (sbt == null)
           sbt = new Taint(false, sb.length());
       if (othersbt == null)
           othersbt = new Taint(false, othersb.length());

       if (sb.length() != oldLength + othersb.length())
           throw new IllegalArgumentException("append string length check:"
                   + sb + "appended: " + othersb);

       for (int i = oldLength; i < sb.length(); i++)
           if (sb.charAt(i) != othersb.charAt(i - oldLength) ||
                   sbt.get(i) != othersbt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void verifySafeStringBuilderAppendSafeString(SafeStringBuilder sfsb,
            int oldLength, SafeString sfs)
    {
       SafeTaint sfsbt = sfsb.taint();
       SafeTaint sfstrt = sfs.taint();
       if (sfsb.length() != oldLength + sfs.length())
           throw new IllegalArgumentException("append string length check:"
                   + sfsb + "appended: " + sfs);

       for (int i = oldLength; i < sfsb.length(); i++)
           if (sfsb.charAt(i) != sfs.charAt(i - oldLength) ||
                   sfsbt.get(i) != sfstrt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void verifySafeStringBuilderAppendSafeStringBuffer(
            SafeStringBuilder sfsb, int oldLength, SafeStringBuffer othersfsb)
    {
       SafeTaint sfsbt = sfsb.taint();
       SafeTaint othersfsbt = othersfsb.taint();
       if (sfsb.length() != oldLength + othersfsb.length())
           throw new IllegalArgumentException("append string length check:"
                   + sfsb + "appended: " + othersfsb);

       for (int i = oldLength; i < sfsb.length(); i++)
           if (sfsb.charAt(i) != othersfsb.charAt(i - oldLength) ||
                   sfsbt.get(i) != othersfsbt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void testStringBuilderAppend(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int oldLen = sb.length();

        StringBuilder retsb = null;
        SafeStringBuilder retsfsb = null;

        switch(sr.nextInt(14)) {
            case 0 : // append(boolean) 
            {
                boolean b = sr.nextBoolean();
                retsb = sb.append(b);
                retsfsb = sfsb.append(b);
                break;
            }

            case 1: // append(char c) 
            {
                char c = randomChar();
                retsb = sb.append(c);
                retsfsb = sfsb.append(c);
                break;
            }

            case 2: // append(char[] str, int offset, int len) 
            {
                char[] str = randomCharArray();
                int offset = sr.nextInt(str.length);
                int length = str.length - offset;

                retsb = sb.append(str, offset, length);
                retsfsb = sfsb.append(str, offset, length);
                break;
            }

            case 3: // append(double d) 
            {
                double d = sr.nextDouble();
                retsb = sb.append(d);
                retsfsb = sfsb.append(d);
                break;
            }

            case 4: // append(float f) 
            {
                float f = sr.nextFloat();
                retsb = sb.append(f);
                retsfsb = sfsb.append(f);
                break;
            }

            case 5: // append(int i) 
            {
                int i = sr.nextInt();
                retsb = sb.append(i);
                retsfsb = sfsb.append(i);
                break;
            }

            case 6: // append(long lng) 
            {
                long lng = sr.nextLong();
                retsb = sb.append(lng);
                retsfsb = sfsb.append(lng);
                break;
            }

            case 7: // append(Object obj) 
            {
                Object o = new Object();

                retsb = sb.append(o);
                retsfsb = sfsb.append(o);
                break;
            }

  
            case 8: // append(String s) 
            {
                StringElem se = getRandomStringElem();
                if (se == null) return;

                retsb = sb.append(se.getString());
                retsfsb = sfsb.append(se.getSafeString());
                verifyStringBuilderAppendString(sb, oldLen, se.getString());
                verifySafeStringBuilderAppendSafeString(sfsb, oldLen, 
                        se.getSafeString());
                break;
            }

            case 9: // append(StringBuffer sb) 
            {
                StringBufferElem othersbe = removeRandomStringBufferElem();
                if (othersbe == null) return;

                othersbe.verify();

                retsb = sb.append(othersbe.getStringBuffer());
                retsfsb = sfsb.append(othersbe.getSafeStringBuffer());
                verifyStringBuilderAppendStringBuffer(sb, oldLen, 
                        othersbe.getStringBuffer());
                verifySafeStringBuilderAppendSafeStringBuffer(sfsb, oldLen,
                        othersbe.getSafeStringBuffer());

                othersbe.verify();
                stringBufferList.add(othersbe);
                break;
            }

            case 10: // append(char[] str)
            {
                char[] str = randomCharArray();
                retsb = sb.append(str);
                retsfsb = sfsb.append(str);
                break;
            }

            case 11: // appendCodePoint(int codePoint)
            {
                int codePoint = randomCodePoint();
                retsb = sb.appendCodePoint(codePoint);
                retsfsb = sfsb.appendCodePoint(codePoint);
                break;
            }

            case 12: // append(CharSequence s)
            {
                CharSequenceElem cse = randomCharSequenceElem();
                retsb = sb.append(cse.getCharSequence());
                retsfsb = sfsb.append(cse.getSafeCharSequence());
                cse.release();
                break;
            }

            case 13: // append(CharSequence s, int start, int end) 
            {
                CharSequenceElem cse = randomCharSequenceElem();
                CharSequence cs = cse.getCharSequence();
                SafeCharSequence sfcs = cse.getSafeCharSequence();

                int start = sr.nextInt(cs.length());
                int end = start + sr.nextInt(cs.length() + 1 - start);

                retsb = sb.append(cs, start, end);
                retsfsb = sfsb.append(sfcs, start, end);
                cse.release();
                break;
            }

            default:
                throw new RuntimeException("switch");
        }

        if (retsb != sb || retsfsb != sfsb) 
            throw new IllegalArgumentException(sbe.toString());
    }

    private void testStringBuilderCapacity(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();

        if (sb.capacity() < 0 || sfsb.capacity() < 0)
            throw new IllegalArgumentException("negative capacity");
    }

    private void testStringBuilderCharAt(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int index;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());
        
        if (index >= sb.length() || index < 0) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.charAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.charAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.charAt(index) != sfsb.charAt(index))
                throw new IllegalArgumentException(sbe.toString());
        }
    }
   
    private void testStringBuilderEnsureCapacity(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int capacity;

        if (sr.nextInt(16) != 0) 
            capacity = -sr.nextInt(maxlen);
        else
            capacity = sr.nextInt(maxlen);

        sb.ensureCapacity(capacity);
        sfsb.ensureCapacity(capacity);
    }

    private void testStringBuilderIndexOf(StringBuilderElem sbe)
    {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        StringElem se;
        int index;
        boolean useIndex = sr.nextBoolean();

        se = getRandomStringElem();
        if (se == null) return;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (useIndex) {
            if (sb.indexOf(se.getString(), index) !=
                    sfsb.indexOf(se.getSafeString(), index))
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.indexOf(se.getString()) != sfsb.indexOf(se.getSafeString()))
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderGetChars(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int srcBegin, srcEnd, dstBegin;
        char[] c, sfc;
        int len = sr.nextInt(sb.length());

        c = new char[len];
        sfc = new char[len];

        if (sr.nextInt(16) == 0) {
            srcBegin = sr.nextInt();
            srcEnd = sr.nextInt();
            dstBegin = sr.nextInt();
        } else {
            srcBegin = sr.nextInt(sb.length());
            srcEnd = srcBegin + 
                sr.nextInt(Math.min(sb.length() - srcBegin, len));
            dstBegin = sr.nextInt(len - (srcEnd - srcBegin) + 1);
        }

        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > sb.length()
                || dstBegin < 0 || dstBegin + (srcEnd - srcBegin) > len) {
            SafeStringBuilder oldsbfs=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.getChars(srcBegin, srcEnd, c, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.getChars(srcBegin, srcEnd, sfc, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsbfs.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
                sb.getChars(srcBegin, srcEnd, c, dstBegin);
                sfsb.getChars(srcBegin, srcEnd, sfc, dstBegin);

                if (!Arrays.equals(c, sfc))
                    throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderCodePointAt(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int index;

        if (sr.nextInt(16) == 0)
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (index < 0 || index >= sb.length()) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.codePointAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.codePointAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.codePointAt(index) != sfsb.codePointAt(index)) 
                throw new IllegalArgumentException(sbe.toString());
        }
    }
    
    private void testStringBuilderCodePointBefore(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int index;

        if (sr.nextInt(16) == 0)
            index = sr.nextInt();
        else
            index = 1 + sr.nextInt(sb.length());

        if (index < 1 || index > sb.length()) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.codePointBefore(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.codePointBefore(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.codePointBefore(index) != sfsb.codePointBefore(index)) 
                throw new IllegalArgumentException(sbe.toString());
        }

    }

    private void testStringBuilderCodePointCount(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int beginIndex, endIndex;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = sr.nextInt(sb.length() + 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > sb.length() || beginIndex > endIndex) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.codePointCount(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.codePointCount(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.codePointCount(beginIndex, endIndex) != 
                    sfsb.codePointCount(beginIndex, endIndex)) 
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderCodePoint(StringBuilderElem sbe) {
        switch (sr.nextInt(3)) {
            case 0:
                testStringBuilderCodePointAt(sbe);
                break;
            case 1:
                testStringBuilderCodePointBefore(sbe);
                break;
            case 2:
                testStringBuilderCodePointCount(sbe);
                break;
            default:
                throw new RuntimeException("switched failed");
        }
    }

    private void testStringBuilderDelete(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int oldLen = sb.length();
        int beginIndex, endIndex;
        boolean useCharAt = false;

        if (sr.nextInt() == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            switch(sr.nextInt(3)) {
                case 0:
                case 1:
                    endIndex = beginIndex + sr.nextInt(maxlen);
                    break;

                case 2:
                    endIndex = beginIndex + 1;
                    useCharAt = true;
                    break;

                default:
                    throw new RuntimeException("switch");
            }
        }

        if (beginIndex < 0 || beginIndex > endIndex || beginIndex > sb.length()
                || (useCharAt && beginIndex == sb.length()))
        {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                if (useCharAt) 
                    sb.deleteCharAt(beginIndex);
                else
                    sb.delete(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                if (useCharAt) 
                    sfsb.deleteCharAt(beginIndex);
                else
                    sfsb.delete(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());

        } else {
            StringBuilder retsb;
            SafeStringBuilder retsfsb;

            if (useCharAt) {
                retsb = sb.deleteCharAt(beginIndex);
                retsfsb = sfsb.deleteCharAt(beginIndex);

                if (sb.length() - oldLen != -1)
                    throw new IllegalArgumentException(sbe.toString());
            } else {
                retsb = sb.delete(beginIndex, endIndex);
                retsfsb = sfsb.delete(beginIndex, endIndex);
                endIndex = Math.min(endIndex, oldLen);
                if (sb.length() - oldLen != beginIndex - endIndex)
                    throw new IllegalArgumentException(sbe.toString());
            }

            if (retsb != sb || retsfsb != sfsb)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void verifyStringBuilderInsertString(StringBuilder sb, int oldLength,
            int offset, String str) 
    {
       Taint sbt = sb.toString().@internal@taint();
       Taint strt = str.@internal@taint();

       if (sbt == null)
           sbt = new Taint(false, sb.length());
       if (strt == null)
           strt = new Taint(false, str.length());

       if (sb.length() != oldLength + str.length())
           throw new IllegalArgumentException("insert string length check:"
                   + sb + "inserted: " + str);

       for (int i = offset; i < offset + str.length(); i++) 
           if (sb.charAt(i) != str.charAt(i - offset) || 
                   sbt.get(i) != strt.get(i - offset))
               throw new IllegalArgumentException("insert string taint check");
    }

    private void verifySafeStringBuilderInsertSafeString(SafeStringBuilder sfsb, 
            int oldLength, int offset, SafeString sfs)
    {
       SafeTaint sfsbt = sfsb.taint();
       SafeTaint sfstrt = sfs.taint();
       if (sfsb.length() != oldLength + sfs.length())
           throw new IllegalArgumentException("insert string length check:"
                   + sfsb + "inserted: " + sfs);

       for (int i = offset; i < offset + sfs.length(); i++)
           if (sfsb.charAt(i) != sfs.charAt(i - offset) ||
                   sfsbt.get(i) != sfstrt.get(i - offset))
               throw new IllegalArgumentException("insert string taint check");
    }

    private void testStringBuilderInsert(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        SafeStringBuilder oldsfsb = new SafeStringBuilder(sfsb.toSafeString());

        int oldLen = sb.length();
        int offset;
        boolean except = false;

        StringBuilder retsb = null;
        SafeStringBuilder retsfsb = null;

        if (sr.nextInt(16) == 0)
            offset = sr.nextInt();
        else
            offset = sr.nextInt(sb.length() + 1);

        if (offset < 0 || offset > sb.length())
            except = true;

        switch(sr.nextInt(12)) {
            case 0 : // insert(int offset, boolean) 
            {
                boolean b = sr.nextBoolean();
                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, b);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, b);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, b);
                    retsfsb = sfsb.insert(offset, b);
                }
                break;
            }

            case 1: // insert(int offset, char c) 
            {
                char c = randomChar();
                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, c);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, c);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, c);
                    retsfsb = sfsb.insert(offset, c);
                }
                break;
            }

            case 2: // insert(int offset, char[] str, int offset, int len) 
            {
                char[] str = randomCharArray();
                int index = sr.nextInt(str.length);
                int length = str.length - offset;

                if (except || index + length > str.length ||
                        ((index|length) < 0)) 
                {
                    boolean caught = false;

                    try {
                        sb.insert(offset, str, index, length);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, str, index, length);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, str, index, length);
                    retsfsb = sfsb.insert(offset, str, index, length);
                }
                break;
            }

            case 3: // insert(int offset, double d) 
            {
                double d = sr.nextDouble();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, d);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, d);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, d);
                    retsfsb = sfsb.insert(offset, d);
                }
                break;
            }

            case 4: // insert(int offset, float f) 
            {
                float f = sr.nextFloat();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, f);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, f);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, f);
                    retsfsb = sfsb.insert(offset, f);
                }
                break;
            }

            case 5: // insert(int offset, int i) 
            {
                int i = sr.nextInt();
                
                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, i);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, i);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, i);
                    retsfsb = sfsb.insert(offset, i);
                }
                break;
            }

            case 6: // insert(int offset, long lng) 
            {
                long lng = sr.nextLong();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, lng);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, lng);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, lng);
                    retsfsb = sfsb.insert(offset, lng);
                }
                break;
            }

            case 7: // insert(int offset, Object obj)
            {
                Object o = new Object();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, o);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, o);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, o);
                    retsfsb = sfsb.insert(offset, o);
                }
                break;
            }

  
            case 8: // insert(int offset, String s)
            {
                StringElem se = getRandomStringElem();
                if (se == null) return;

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, se.getString());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, se.getSafeString());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, se.getString());
                    retsfsb = sfsb.insert(offset, se.getSafeString());
                    verifyStringBuilderInsertString(sb, oldLen, offset, 
                            se.getString());
                    verifySafeStringBuilderInsertSafeString(sfsb, oldLen, 
                            offset, se.getSafeString());
                }
                break;
            }

            case 9: // insert(int offset, char[] str)
            {
                char[] str = randomCharArray();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, str);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, str);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, str);
                    retsfsb = sfsb.insert(offset, str);
                }
                break;
            }

            case 10: // insert(int dstOffset, CharSequence s)
            {
                CharSequenceElem cse = randomCharSequenceElem();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, cse.getCharSequence());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, cse.getSafeCharSequence());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, cse.getCharSequence());
                    retsfsb = sfsb.insert(offset, cse.getSafeCharSequence());
                }
                cse.release();
                break;
            }

            case 11: //insert(int dstOffset, CharSequence s, int start, int end)
            {
                CharSequenceElem cse = randomCharSequenceElem();
                CharSequence cs = cse.getCharSequence();
                SafeCharSequence sfcs = cse.getSafeCharSequence();

                int start = sr.nextInt(cs.length());
                int end = start + sr.nextInt(cs.length() + 1 - start);

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, cs, start, end);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, sfcs, start, end);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, cs, start, end);
                    retsfsb = sfsb.insert(offset, sfcs, start, end);
                }
                cse.release();
                break;
            }

            default:
                throw new RuntimeException("switch");
        }

        if (retsb == null || retsfsb == null) {
            if (retsb != null || retsfsb != null)
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (retsb != sb || retsfsb != sfsb)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderLastIndexOf(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        StringElem se;
        int index;
        boolean useIndex = sr.nextBoolean();

        se = getRandomStringElem();
        if (se == null) return;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (useIndex) {
            if (sb.lastIndexOf(se.getString(), index) !=
                    sfsb.lastIndexOf(se.getSafeString(), index))
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.lastIndexOf(se.getString()) != 
                    sfsb.lastIndexOf(se.getSafeString()))
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderOffsetByCodePoints(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int index;
        int codePointOffset;

        if (sr.nextInt(16) == 0) {
            index = sr.nextInt();
            codePointOffset = sr.nextInt();
        } else {
            index = sr.nextInt(sb.length());
            boolean sign = sr.nextBoolean();

            if (sign)
                codePointOffset = -sr.nextInt(sb.codePointCount(0, index));
            else
                codePointOffset = 
                    sr.nextInt(sb.codePointCount(index,sb.length()));
        }

        if (index < 0 || index > sb.length() || (codePointOffset > 0 &&
                   codePointOffset >  sb.codePointCount(index, sb.length()))
               || (codePointOffset < 0 && 
                   -codePointOffset > sb.codePointCount(0,index))) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.offsetByCodePoints(index, codePointOffset);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.offsetByCodePoints(index, codePointOffset);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.offsetByCodePoints(index, codePointOffset) != 
                    sfsb.offsetByCodePoints(index, codePointOffset)) 
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderReplace(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();

        StringElem se = getRandomStringElem();
        if (se == null) return;

        String s = se.getString();
        SafeString sfs = se.getSafeString();

        int oldLen = sb.length();
        int beginIndex, endIndex;

        if (sr.nextInt() == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(maxlen);
        }

        if (beginIndex < 0 || beginIndex > endIndex || beginIndex > sb.length())
        {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.replace(beginIndex, endIndex, s);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.replace(beginIndex, endIndex, sfs);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            StringBuilder retsb = sb.replace(beginIndex, endIndex, s);
            SafeStringBuilder retsfsb = sfsb.replace(beginIndex, endIndex, sfs);
            endIndex = Math.min(endIndex, oldLen);

            if (sb.length() - oldLen != beginIndex - endIndex + s.length())
                throw new IllegalArgumentException(sbe.toString());

            Taint sbt = sb.toString().@internal@taint();
            if (sbt == null)
                sbt = new Taint(false, sb.length());

            for (int i = beginIndex; i < s.length(); i++)
                if (sb.charAt(i) != sfs.charAt(i - beginIndex) 
                        || sbt.get(i) != sfs.taint().get(i - beginIndex))
                    throw new IllegalArgumentException(sbe.toString());

            if (retsb != sb || retsfsb != sfsb)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderSetCharAt(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        char c = randomChar();
        int index;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (index < 0 || index >= sb.length()) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.setCharAt(index, c);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.setCharAt(index, c);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            sb.setCharAt(index, c);
            sfsb.setCharAt(index, c);

            Taint sbt = sb.toString().@internal@taint();
            if (sbt == null)
                sbt = new Taint(false, sb.length());

            if (sb.charAt(index) != c || sfsb.charAt(index) != c 
                    || sbt.get(index) || sfsb.taint().get(index))
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderSetLength(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        
        int m = maxlen < sb.length() ? sb.length() + 1024 : maxlen;
        int len;

        if (sr.nextInt(16) == 0)
            len = -sr.nextInt(m);
        else
            len = sr.nextInt(m);

        if (len < 0) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.setLength(len);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.setLength(len);
            } catch (IndexOutOfBoundsException e) {

                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            sb.setLength(len);
            sfsb.setLength(len);
            if (sb.length() != len || sfsb.length() != len)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBuilderSubSequence(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int beginIndex, endIndex;

        CharSequence subs;
        SafeCharSequence subsfs;


        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(sb.length() + 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > sb.length() || beginIndex > endIndex) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                subs = sb.subSequence(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                subsfs = sfsb.subSequence(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            subs = sb.subSequence(beginIndex, endIndex);
            subsfs = sfsb.subSequence(beginIndex, endIndex);

            subsfs.toSafeString().verify(subs.toString());

            Taint t = sb.toString().@internal@taint();
            Taint subt = subs.toString().@internal@taint();

            if (t == null)
                t = new Taint(false, sb.length());
            if (subt == null)
                subt = new Taint(false, subs.length());

            for (int i = beginIndex; i < endIndex; i++) {
                if (sb.charAt(i) != subs.charAt(i - beginIndex) 
                        || t.get(i) != subt.get(i-beginIndex))
                    throw new IllegalArgumentException("string taint/data bad");

            }
        }
    }

    private void testStringBuilderReverse(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        StringBuilder retsb;
        SafeStringBuilder retsfsb;

        int len = sb.length();

        retsb = sb.reverse();
        retsfsb = sfsb.reverse();
        sbe.verify();

        if (sb.length() != len || retsb != sb || retsfsb != sfsb)
            throw new IllegalArgumentException(sbe.toString());
    }

    private void testStringBuilderLength(StringBuilderElem sbe) {
        if (sbe.getStringBuilder().length() != 
                sbe.getSafeStringBuilder().length())
            throw new IllegalArgumentException(sbe.toString());
    }

    private void testStringBuilderSubstring(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        int beginIndex, endIndex;

        StringElem subse;
        String subs;
        SafeString subsfs;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(sb.length()+1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > sb.length() || beginIndex > endIndex) {
            SafeStringBuilder oldsfsb=new SafeStringBuilder(sfsb.toSafeString());
            boolean caught = false;

            try {
                subs = sb.substring(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                subsfs = sfsb.substring(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (endIndex == sb.length() && sr.nextBoolean()) {
                subs = sb.substring(beginIndex);
                subsfs = sfsb.substring(beginIndex);
            } else {
                subs = sb.substring(beginIndex, endIndex);
                subsfs = sfsb.substring(beginIndex, endIndex);
            }

            subsfs.verify(subs);

            Taint t = sb.toString().@internal@taint();
            Taint subt = subs.@internal@taint();

            if (t == null)
                t = new Taint(false, sb.length());
            if (subt == null)
                subt = new Taint(false, subs.length());

            for (int i = beginIndex; i < endIndex; i++) 
                if (sb.charAt(i) != subs.charAt(i - beginIndex) 
                        || t.get(i) != subt.get(i-beginIndex))
                    throw new IllegalArgumentException("string taint/data bad");

            subse = new StringElem(subs, subsfs, sr.nextInt());
            subse.verify();
            subse.addOp("[" + sbe.toString() + "] StringBuilder.substring");
            stringList.add(subse);
        }
    }

    private void testStringBuilderToString(StringBuilderElem sbe) {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        String s = sb.toString();
        SafeString sfs = sfsb.toSafeString();
        StringElem se = new StringElem(s, sfs, sr.nextInt());

        se.verify();
        sbe.verify(s);

        se.addOp("[" + sbe.toString() + "] StringBuilder.toString");
        stringList.add(se);
    }

    private void testStringBuilderSerialize(StringBuilderElem sbe) 
        throws IOException, ClassNotFoundException
    {
        StringBuilder sb = sbe.getStringBuilder();
        SafeStringBuilder sfsb = sbe.getSafeStringBuilder();
        sbe.verify();
        sfsb = new SafeStringBuilder(new SafeString(
                                          sfsb.toSafeString().origString(),
                                          new SafeTaint(false, sfsb.length())));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bout);

        os.writeObject(sb);
        os.flush();
        os.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream is = new ObjectInputStream(bin);

        sb = (StringBuilder) is.readObject();
        is.close();
        bin.close();
        bout.close();

        if (sb.toString().@internal@isTainted())
            throw new RuntimeException("Corrupted Serialization");

        StringBuilderElem newSbe = new StringBuilderElem(sb,sfsb,sr.nextInt());
        newSbe.verify();
        sbe.addOp(StringBuilderMethod.SB_SERIALIZE);

        // XXX BUG TODO
        // Apparent JVM Bug - if we add newSbe to stringBufferList,
        // codePointAt will later return incorrect values _very_ rarely.
        // This occurs only on objects that were previously serialized,
        // and only for the codePointAt method. StringBuffer is similarly
        // affected. Interestingly, when this bug occurs, the 
        // sb.toString().equals(sfsb.toString()) holds -- so it's clearly not
        // actual data corruption. For now we just don't add newSbe to the
        // stringBufferList to work around this issue.
         
    }

    private void testStringBuilderTrimToSize(StringBuilderElem sbe) {
        sbe.getStringBuilder().trimToSize();
        sbe.getSafeStringBuilder().trimToSize();
    }

    public void testStringBuilder() 
        throws IllegalArgumentException, IOException, ClassNotFoundException
    {
        int op = sr.nextInt(StringBuilderMethod.SB_END);
        StringBuilderElem sbe = null;

        if (stringBuilderList.size() < threads * 2)
            op = StringBuilderMethod.SB_NEW;

        if (op != StringBuilderMethod.SB_NEW)  {
            sbe = removeRandomStringBuilderElem();
            if (sbe == null) { 
                op = StringBuilderMethod.SB_NEW;
            } else {
                sbe.verify();
                sbe.addOp(op);
            }
        }

        switch(op)
        {
            case StringBuilderMethod.SB_NEW:
                testStringBuilderNew();
                break;

            case StringBuilderMethod.SB_APPEND:
                testStringBuilderAppend(sbe);
                break;

           case StringBuilderMethod.SB_CAPACITY:
                testStringBuilderCapacity(sbe);
                break;

           case StringBuilderMethod.SB_CHARAT:
                testStringBuilderCharAt(sbe);
                break;

           case StringBuilderMethod.SB_CODEPOINT:
                testStringBuilderCodePoint(sbe);
                break;

           case StringBuilderMethod.SB_DELETE:
                testStringBuilderDelete(sbe);
                break;

           case StringBuilderMethod.SB_ENSURECAPACITY:
                testStringBuilderEnsureCapacity(sbe);
                break;

           case StringBuilderMethod.SB_GETCHARS:
                testStringBuilderGetChars(sbe);
                break;

           case StringBuilderMethod.SB_INDEXOF:
                testStringBuilderIndexOf(sbe);
                break;

           case StringBuilderMethod.SB_INSERT:
                testStringBuilderInsert(sbe);
                break;

           case StringBuilderMethod.SB_LASTINDEXOF:
                testStringBuilderLastIndexOf(sbe);
                break;

           case StringBuilderMethod.SB_LENGTH:
                testStringBuilderLength(sbe);
                break;

           case StringBuilderMethod.SB_OFFSETBYCODEPOINTS:
                testStringBuilderOffsetByCodePoints(sbe);
                break;

           case StringBuilderMethod.SB_REPLACE:
                testStringBuilderReplace(sbe);
                break;

           case StringBuilderMethod.SB_REVERSE:
                testStringBuilderReverse(sbe);
                break;

           case StringBuilderMethod.SB_SETCHARAT:
                testStringBuilderSetCharAt(sbe);
                break;

           case StringBuilderMethod.SB_SETLENGTH:
                testStringBuilderSetLength(sbe);
                break;

           case StringBuilderMethod.SB_SUBSEQUENCE:
                testStringBuilderSubSequence(sbe);
                break;

           case StringBuilderMethod.SB_SUBSTRING:
                testStringBuilderSubstring(sbe);
                break;
        
           case StringBuilderMethod.SB_TOSTRING:
                testStringBuilderToString(sbe);
                break;

           case StringBuilderMethod.SB_TRIMTOSIZE:
                testStringBuilderTrimToSize(sbe);
                break;

           case StringBuilderMethod.SB_SERIALIZE:
                testStringBuilderSerialize(sbe);
                break;

           default:
                throw new RuntimeException("switch");
        }

        if (op != StringBuilderMethod.SB_NEW) {
            sbe.verify();
            stringBuilderList.add(sbe);
        }
    }
    //[fiJava5+]

    private void testStringBufferNew() {
        StringBuffer sb = null;
        SafeStringBuffer sfsb = null;
        StringBufferElem sbe;

        switch(sr.nextInt(4)) {
            case 0: /* StringBuffer() */
                sb = new StringBuffer();
                sfsb = new SafeStringBuffer();
                break;

            case 1: /* StringBuffer(int capacity) */
            {
                int capacity = sr.nextInt(maxlen);
                sb = new StringBuffer(capacity);
                sfsb = new SafeStringBuffer(capacity);
                break;
            }

            case 2: /* StringBuffer(CharSequence seq) */
            {
                //[ifJava5+]
                CharSequenceElem cse = randomCharSequenceElem();
                sb = new StringBuffer(cse.getCharSequence());
                sfsb = new SafeStringBuffer(cse.getSafeCharSequence());
                cse.release();
                break;
                //[fiJava5+]
                // ...else fall through
            }

            case 3:
            {
                StringElem se = getRandomStringElem();
                if (se == null) return; 

                sb = new StringBuffer(se.getString());
                sfsb = new SafeStringBuffer(se.getSafeString());
                break;
            }

            default:
                throw new RuntimeException("switch");
        }
        sbe = new StringBufferElem(sb, sfsb, sr.nextInt());
        sbe.addOp(StringBufferMethod.SB_NEW);
        sbe.verify();
        stringBufferList.add(sbe);
    }

    private void verifyStringBufferAppendString(StringBuffer sb, int oldLength,
            String str) 
    {
       Taint sbt = sb.toString().@internal@taint();
       Taint strt = str.@internal@taint();

       if (sbt == null)
           sbt = new Taint(false, sb.length());
       if (strt == null)
           strt = new Taint(false, str.length());

       if (sb.length() != oldLength + str.length())
           throw new IllegalArgumentException("append string length check:"
                   + sb + "appended: " + str);

       for (int i = oldLength; i < sb.length(); i++) 
           if (sb.charAt(i) != str.charAt(i - oldLength) || 
                   sbt.get(i) != strt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void verifyStringBufferAppendStringBuffer(StringBuffer sb, 
            int oldLength, StringBuffer othersb)
    {
       Taint sbt = sb.toString().@internal@taint();
       Taint othersbt = othersb.toString().@internal@taint();

       if (sbt == null)
           sbt = new Taint(false, sb.length());
       if (othersbt == null)
           othersbt = new Taint(false, othersb.length());

       if (sb.length() != oldLength + othersb.length())
           throw new IllegalArgumentException("append string length check:"
                   + sb + "appended: " + othersb);

       for (int i = oldLength; i < sb.length(); i++)
           if (sb.charAt(i) != othersb.charAt(i - oldLength) ||
                   sbt.get(i) != othersbt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void verifySafeStringBufferAppendSafeString(SafeStringBuffer sfsb, 
            int oldLength, SafeString sfs)
    {
       SafeTaint sfsbt = sfsb.taint();
       SafeTaint sfstrt = sfs.taint();
       if (sfsb.length() != oldLength + sfs.length())
           throw new IllegalArgumentException("append string length check:"
                   + sfsb + "appended: " + sfs);

       for (int i = oldLength; i < sfsb.length(); i++)
           if (sfsb.charAt(i) != sfs.charAt(i - oldLength) ||
                   sfsbt.get(i) != sfstrt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void verifySafeStringBufferAppendSafeStringBuffer(
            SafeStringBuffer sfsb, int oldLength, SafeStringBuffer othersfsb)
    {
       SafeTaint sfsbt = sfsb.taint();
       SafeTaint othersfsbt = othersfsb.taint();
       if (sfsb.length() != oldLength + othersfsb.length())
           throw new IllegalArgumentException("append string length check:"
                   + sfsb + "appended: " + othersfsb);

       for (int i = oldLength; i < sfsb.length(); i++)
           if (sfsb.charAt(i) != othersfsb.charAt(i - oldLength) ||
                   sfsbt.get(i) != othersfsbt.get(i - oldLength))
               throw new IllegalArgumentException("append string taint check");
    }

    private void testStringBufferAppend(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int oldLen = sb.length();

        StringBuffer retsb = null;
        SafeStringBuffer retsfsb = null;

        switch(sr.nextInt(14)) {
            case 0 : /* append(boolean) */
            {
                boolean b = sr.nextBoolean();
                retsb = sb.append(b);
                retsfsb = sfsb.append(b);
                break;
            }

            case 1: /* append(char c) */
            {
                char c = randomChar();
                retsb = sb.append(c);
                retsfsb = sfsb.append(c);
                break;
            }

            case 2: /* append(char[] str, int offset, int len */
            {
                char[] str = randomCharArray();
                int offset = sr.nextInt(str.length);
                int length = str.length - offset;

                retsb = sb.append(str, offset, length);
                retsfsb = sfsb.append(str, offset, length);
                break;
            }

            case 3: /* append(double d) */
            {
                double d = sr.nextDouble();
                retsb = sb.append(d);
                retsfsb = sfsb.append(d);
                break;
            }

            case 4: /* append(float f) */
            {
                float f = sr.nextFloat();
                retsb = sb.append(f);
                retsfsb = sfsb.append(f);
                break;
            }

            case 5: /* append(int i) */
            {
                int i = sr.nextInt();
                retsb = sb.append(i);
                retsfsb = sfsb.append(i);
                break;
            }

            case 6: /* append(long lng) */
            {
                long lng = sr.nextLong();
                retsb = sb.append(lng);
                retsfsb = sfsb.append(lng);
                break;
            }

            case 7: /* append(Object obj) */
            {
                Object o = new Object();

                retsb = sb.append(o);
                retsfsb = sfsb.append(o);
                break;
            }

  
            case 8: /* append(String s) */
            {
                StringElem se = getRandomStringElem();
                if (se == null) return;

                retsb = sb.append(se.getString());
                retsfsb = sfsb.append(se.getSafeString());
                verifyStringBufferAppendString(sb, oldLen, se.getString());
                verifySafeStringBufferAppendSafeString(sfsb, oldLen, 
                        se.getSafeString());
                break;
            }

            case 9: /* append(StringBuffer sb) */
            {
                StringBufferElem othersbe = removeRandomStringBufferElem();
                if (othersbe == null) return;

                othersbe.verify();

                retsb = sb.append(othersbe.getStringBuffer());
                retsfsb = sfsb.append(othersbe.getSafeStringBuffer());
                verifyStringBufferAppendStringBuffer(sb, oldLen, 
                        othersbe.getStringBuffer());
                verifySafeStringBufferAppendSafeStringBuffer(sfsb, oldLen,
                        othersbe.getSafeStringBuffer());

                othersbe.verify();
                stringBufferList.add(othersbe);
                break;
            }

            case 10: // appendCodePoint(int codePoint) 
            {
                //[ifJava5+]
                int codePoint = randomCodePoint();
                retsb = sb.appendCodePoint(codePoint);
                retsfsb = sfsb.appendCodePoint(codePoint);
                break;
                //[fiJava5+]
                // ...else fall through
            }

            case 11: // append(CharSequence s)
            {
                //[ifJava5+]
                CharSequenceElem cse = randomCharSequenceElem();
                retsb = sb.append(cse.getCharSequence());
                retsfsb = sfsb.append(cse.getSafeCharSequence());
                cse.release();
                break;
                //[fiJava5+]
                // ...else fall through
            }

            case 12: // append(CharSequence s, int start, int end) 
            {
                //[ifJava5+]
                CharSequenceElem cse = randomCharSequenceElem();
                CharSequence cs = cse.getCharSequence();
                SafeCharSequence sfcs = cse.getSafeCharSequence();

                int start = sr.nextInt(cs.length());
                int end = start + sr.nextInt(cs.length() + 1 - start);

                retsb = sb.append(cs, start, end);
                retsfsb = sfsb.append(sfcs, start, end);
                cse.release();
                break;
                //[fiJava5+]
                // ...else fall through
            }

            case 13: /* append(char[] str) */
            {
                char[] str = randomCharArray();
                retsb = sb.append(str);
                retsfsb = sfsb.append(str);
                break;
            }


            default:
                throw new RuntimeException("switch");
        }

        if (retsb != sb || retsfsb != sfsb) 
            throw new IllegalArgumentException(sbe.toString());
    }

    private void testStringBufferCapacity(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();

        if (sb.capacity() < 0 || sfsb.capacity() < 0)
            throw new IllegalArgumentException("negative capacity");
    }

    private void testStringBufferCharAt(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int index;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());
        
        if (index >= sb.length() || index < 0) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.charAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.charAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.charAt(index) != sfsb.charAt(index))
                throw new IllegalArgumentException(sbe.toString());
        }
    }
   
    private void testStringBufferEnsureCapacity(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int capacity;

        if (sr.nextInt(16) != 0) 
            capacity = -sr.nextInt(maxlen);
        else
            capacity = sr.nextInt(maxlen);

        sb.ensureCapacity(capacity);
        sfsb.ensureCapacity(capacity);
    }

    private void testStringBufferIndexOf(StringBufferElem sbe)
    {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        StringElem se;
        int index;
        boolean useIndex = sr.nextBoolean();

        se = getRandomStringElem();
        if (se == null) return;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (useIndex) {
            if (sb.indexOf(se.getString(), index) !=
                    sfsb.indexOf(se.getSafeString(), index))
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.indexOf(se.getString()) != sfsb.indexOf(se.getSafeString()))
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBufferGetChars(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int srcBegin, srcEnd, dstBegin;
        char[] c, sfc;
        int len = sr.nextInt(sb.length());

        c = new char[len];
        sfc = new char[len];

        if (sr.nextInt(16) == 0) {
            srcBegin = sr.nextInt();
            srcEnd = sr.nextInt();
            dstBegin = sr.nextInt();
        } else {
            srcBegin = sr.nextInt(sb.length());
            srcEnd = srcBegin + 
                sr.nextInt(Math.min(sb.length() - srcBegin, len));
            dstBegin = sr.nextInt(len - (srcEnd - srcBegin) + 1);
        }

        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > sb.length()
                || dstBegin < 0 || dstBegin + (srcEnd - srcBegin) > len) {
            SafeStringBuffer oldsbfs=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.getChars(srcBegin, srcEnd, c, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.getChars(srcBegin, srcEnd, sfc, dstBegin);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsbfs.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
                sb.getChars(srcBegin, srcEnd, c, dstBegin);
                sfsb.getChars(srcBegin, srcEnd, sfc, dstBegin);

                if (!Arrays.equals(c, sfc))
                    throw new IllegalArgumentException(sbe.toString());
        }
    }

    //[ifJava5+]
    private void testStringBufferCodePointAt(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int index;

        if (sr.nextInt(16) == 0)
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (index < 0 || index >= sb.length()) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.codePointAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.codePointAt(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.codePointAt(index) != sfsb.codePointAt(index)) 
                throw new IllegalArgumentException(sbe.toString());
        }
    }
    
    private void testStringBufferCodePointBefore(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int index;

        if (sr.nextInt(16) == 0)
            index = sr.nextInt();
        else
            index = 1 + sr.nextInt(sb.length());

        if (index < 1 || index > sb.length()) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.codePointBefore(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.codePointBefore(index);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.codePointBefore(index) != sfsb.codePointBefore(index)) 
                throw new IllegalArgumentException(sbe.toString());
        }

    }

    private void testStringBufferCodePointCount(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int beginIndex, endIndex;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(sb.length() + 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > sb.length() || beginIndex > endIndex) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.codePointCount(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.codePointCount(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.codePointCount(beginIndex, endIndex) != 
                    sfsb.codePointCount(beginIndex, endIndex)) 
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBufferCodePoint(StringBufferElem sbe) {
        switch (sr.nextInt(3)) {
            case 0:
                testStringBufferCodePointAt(sbe);
                break;
            case 1:
                testStringBufferCodePointBefore(sbe);
                break;
            case 2:
                testStringBufferCodePointCount(sbe);
                break;
            default:
                throw new RuntimeException("switched failed");
        }
    }
    //[fiJava5+]

    private void testStringBufferDelete(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int oldLen = sb.length();
        int beginIndex, endIndex;
        boolean useCharAt = false;

        if (sr.nextInt() == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            switch(sr.nextInt(3)) {
                case 0:
                case 1:
                    endIndex = beginIndex + sr.nextInt(maxlen);
                    break;

                case 2:
                    endIndex = beginIndex + 1;
                    useCharAt = true;
                    break;

                default:
                    throw new RuntimeException("switch");
            }
        }

        if (beginIndex < 0 || beginIndex > endIndex || beginIndex > sb.length()
                || (useCharAt && beginIndex == sb.length()))
        {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                if (useCharAt) 
                    sb.deleteCharAt(beginIndex);
                else
                    sb.delete(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                if (useCharAt) 
                    sfsb.deleteCharAt(beginIndex);
                else
                    sfsb.delete(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());

        } else {
            StringBuffer retsb;
            SafeStringBuffer retsfsb;

            if (useCharAt) {
                retsb = sb.deleteCharAt(beginIndex);
                retsfsb = sfsb.deleteCharAt(beginIndex);

                if (sb.length() - oldLen != -1)
                    throw new IllegalArgumentException(sbe.toString());
            } else {
                retsb = sb.delete(beginIndex, endIndex);
                retsfsb = sfsb.delete(beginIndex, endIndex);
                endIndex = Math.min(endIndex, oldLen);
                if (sb.length() - oldLen != beginIndex - endIndex)
                    throw new IllegalArgumentException(sbe.toString());
            }

            if (retsb != sb || retsfsb != sfsb)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void verifyStringBufferInsertString(StringBuffer sb, int oldLength,
            int offset, String str) 
    {
       Taint sbt = sb.toString().@internal@taint();
       Taint strt = str.@internal@taint();

       if (sbt == null)
           sbt = new Taint(false, sb.length());
       if (strt == null)
           strt = new Taint(false, str.length());

       if (sb.length() != oldLength + str.length())
           throw new IllegalArgumentException("insert string length check:"
                   + sb + "inserted: " + str);

       for (int i = offset; i < offset + str.length(); i++) 
           if (sb.charAt(i) != str.charAt(i - offset) || 
                   sbt.get(i) != strt.get(i - offset))
               throw new IllegalArgumentException("insert string taint check");
    }

    private void verifySafeStringBufferInsertSafeString(SafeStringBuffer sfsb, 
            int oldLength, int offset, SafeString sfs)
    {
       SafeTaint sfsbt = sfsb.taint();
       SafeTaint sfstrt = sfs.taint();
       if (sfsb.length() != oldLength + sfs.length())
           throw new IllegalArgumentException("insert string length check:"
                   + sfsb + "inserted: " + sfs);

       for (int i = offset; i < offset + sfs.length(); i++)
           if (sfsb.charAt(i) != sfs.charAt(i - offset) ||
                   sfsbt.get(i) != sfstrt.get(i - offset))
               throw new IllegalArgumentException("insert string taint check");
    }

    private void testStringBufferInsert(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        SafeStringBuffer oldsfsb = new SafeStringBuffer(sfsb.toSafeString());

        int oldLen = sb.length();
        int offset;
        boolean except = false;

        StringBuffer retsb = null;
        SafeStringBuffer retsfsb = null;

        if (sr.nextInt(16) == 0)
            offset = sr.nextInt();
        else
            offset = sr.nextInt(sb.length() + 1);

        if (offset < 0 || offset > sb.length())
            except = true;

        switch(sr.nextInt(12)) {
            case 0 : /* insert(int offset, boolean) */
            {
                boolean b = sr.nextBoolean();
                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, b);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, b);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, b);
                    retsfsb = sfsb.insert(offset, b);
                }
                break;
            }

            case 1: /* insert(int offset, char c) */
            {
                char c = randomChar();
                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, c);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, c);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, c);
                    retsfsb = sfsb.insert(offset, c);
                }
                break;
            }

            case 2: /* insert(int offset, char[] str, int offset, int len) */
            {
                char[] str = randomCharArray();
                int index = sr.nextInt(str.length);
                int length = str.length - offset;

                if (except || index + length > str.length ||
                        ((index|length) < 0)) 
                {
                    boolean caught = false;

                    try {
                        sb.insert(offset, str, index, length);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, str, index, length);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, str, index, length);
                    retsfsb = sfsb.insert(offset, str, index, length);
                }
                break;
            }

            case 3: /* insert(int offset, double d) */
            {
                double d = sr.nextDouble();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, d);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, d);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, d);
                    retsfsb = sfsb.insert(offset, d);
                }
                break;
            }

            case 4: /* insert(int offset, float f) */
            {
                float f = sr.nextFloat();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, f);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, f);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, f);
                    retsfsb = sfsb.insert(offset, f);
                }
                break;
            }

            case 5: /* insert(int offset, int i) */
            {
                int i = sr.nextInt();
                
                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, i);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, i);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, i);
                    retsfsb = sfsb.insert(offset, i);
                }
                break;
            }

            case 6: /* insert(int offset, long lng) */
            {
                long lng = sr.nextLong();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, lng);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, lng);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, lng);
                    retsfsb = sfsb.insert(offset, lng);
                }
                break;
            }

            case 7: /* insert(int offset, Object obj) */
            {
                Object o = new Object();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, o);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, o);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, o);
                    retsfsb = sfsb.insert(offset, o);
                }
                break;
            }

  
            case 8: /* insert(int offset, String s) */
            {
                StringElem se = getRandomStringElem();
                if (se == null) return;

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, se.getString());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, se.getSafeString());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, se.getString());
                    retsfsb = sfsb.insert(offset, se.getSafeString());
                    verifyStringBufferInsertString(sb, oldLen, offset, 
                            se.getString());
                    verifySafeStringBufferInsertSafeString(sfsb, oldLen, 
                            offset, se.getSafeString());
                }
                break;
            }

            case 9: // insert(int dstOffset, CharSequence s) 
            {
                //[ifJava5+]
                CharSequenceElem cse = randomCharSequenceElem();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, cse.getCharSequence());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, cse.getSafeCharSequence());
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, cse.getCharSequence());
                    retsfsb = sfsb.insert(offset, cse.getSafeCharSequence());
                }
                cse.release();
                break;
                //[fiJava5+]
                // ...else fall through
            }

            case 10: // insert(int dstOffset, CharSequence s, int start, 
                     //        int end)
            {
                //[ifJava5+]
                CharSequenceElem cse = randomCharSequenceElem();
                CharSequence cs = cse.getCharSequence();
                SafeCharSequence sfcs = cse.getSafeCharSequence();
                
                int start = sr.nextInt(cs.length());
                int end = start + sr.nextInt(cs.length() + 1 - start);

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, cs, start, end);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, sfcs, start, end);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, cs, start, end);
                    retsfsb = sfsb.insert(offset, sfcs, start, end);
                }
                cse.release();
                break;
                //[fiJava5+]
                // ...else fall through
            }

            case 11: /* insert(int offset, char[] str) */
            {
                char[] str = randomCharArray();

                if (except) {
                    boolean caught = false;

                    try {
                        sb.insert(offset, str);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught) 
                        throw new IllegalArgumentException(sbe.toString());

                    caught = false;
                    try {
                        sfsb.insert(offset, str);
                    } catch (IndexOutOfBoundsException e) {
                        caught = true;
                    }

                    if (!caught || !oldsfsb.verify(sb)) 
                        throw new IllegalArgumentException(sbe.toString());
                } else {
                    retsb = sb.insert(offset, str);
                    retsfsb = sfsb.insert(offset, str);
                }
                break;
            }


            default:
                throw new RuntimeException("switch");
        }

        if (retsb == null || retsfsb == null) {
            if (retsb != null || retsfsb != null)
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (retsb != sb || retsfsb != sfsb)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBufferLastIndexOf(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        StringElem se;
        int index;
        boolean useIndex = sr.nextBoolean();

        se = getRandomStringElem();
        if (se == null) return;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (useIndex) {
            if (sb.lastIndexOf(se.getString(), index) !=
                    sfsb.lastIndexOf(se.getSafeString(), index))
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.lastIndexOf(se.getString()) != 
                    sfsb.lastIndexOf(se.getSafeString()))
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    //[ifJava5+]
    private void testStringBufferOffsetByCodePoints(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int index;
        int codePointOffset;

        if (sr.nextInt(16) == 0) {
            index = sr.nextInt();
            codePointOffset = sr.nextInt();
        } else {
            index = sr.nextInt(sb.length());
            boolean sign = sr.nextBoolean();

            if (sign)
                codePointOffset = -sr.nextInt(sb.codePointCount(0, index));
            else
                codePointOffset = 
                    sr.nextInt(sb.codePointCount(index,sb.length()));
        }

        if (index < 0 || index > sb.length() || (codePointOffset > 0 &&
                   codePointOffset >  sb.codePointCount(index, sb.length()))
               || (codePointOffset < 0 && 
                   -codePointOffset > sb.codePointCount(0,index))) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.offsetByCodePoints(index, codePointOffset);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.offsetByCodePoints(index, codePointOffset);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (sb.offsetByCodePoints(index, codePointOffset) != 
                    sfsb.offsetByCodePoints(index, codePointOffset)) 
                throw new IllegalArgumentException(sbe.toString());
        }
    }
    //[fiJava5+]

    private void testStringBufferReplace(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();

        StringElem se = getRandomStringElem();
        if (se == null) return;

        String s = se.getString();
        SafeString sfs = se.getSafeString();

        int oldLen = sb.length();
        int beginIndex, endIndex;

        if (sr.nextInt() == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(maxlen);
        }

        if (beginIndex < 0 || beginIndex > endIndex || beginIndex > sb.length())
        {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.replace(beginIndex, endIndex, s);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.replace(beginIndex, endIndex, sfs);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            StringBuffer retsb = sb.replace(beginIndex, endIndex, s);
            SafeStringBuffer retsfsb = sfsb.replace(beginIndex, endIndex, sfs);
            endIndex = Math.min(endIndex, oldLen);

            if (sb.length() - oldLen != beginIndex - endIndex + s.length())
                throw new IllegalArgumentException(sbe.toString());

            Taint t = sb.toString().@internal@taint();
            if (t == null)
                t = new Taint(false, sb.length());

            for (int i = beginIndex; i < s.length(); i++)
                if (sb.charAt(i) != sfs.charAt(i - beginIndex) 
                        || t.get(i) != sfs.taint().get(i - beginIndex))
                    throw new IllegalArgumentException(sbe.toString());

            if (retsb != sb || retsfsb != sfsb)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBufferSetCharAt(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        char c = randomChar();
        int index;

        if (sr.nextInt(16) == 0) 
            index = sr.nextInt();
        else
            index = sr.nextInt(sb.length());

        if (index < 0 || index >= sb.length()) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.setCharAt(index, c);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.setCharAt(index, c);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            sb.setCharAt(index, c);
            sfsb.setCharAt(index, c);

            Taint t = sb.toString().@internal@taint();
            if (t == null)
                t = new Taint(false, sb.length());
            if (sb.charAt(index) != c || sfsb.charAt(index) != c 
                    || t.get(index) || sfsb.taint().get(index))
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBufferSetLength(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        
        int m = maxlen < sb.length() ? sb.length() + 1024 : maxlen;
        int len;

        if (sr.nextInt(16) == 0)
            len = -sr.nextInt(m);
        else
            len = sr.nextInt(m);

        if (len < 0) {
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                sb.setLength(len);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                sfsb.setLength(len);
            } catch (IndexOutOfBoundsException e) {

                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            sb.setLength(len);
            sfsb.setLength(len);
            if (sb.length() != len || sfsb.length() != len)
                throw new IllegalArgumentException(sbe.toString());
        }
    }

    private void testStringBufferSubSequence(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int beginIndex, endIndex;

        CharSequence subs;
        SafeCharSequence subsfs;


        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(sb.length() + 1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > sb.length() || beginIndex > endIndex) {
            /* Exception time */
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                subs = sb.subSequence(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                subsfs = (SafeCharSequence) sfsb.subSequence(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            subs = sb.subSequence(beginIndex, endIndex);
            subsfs = (SafeCharSequence) sfsb.subSequence(beginIndex, endIndex);

            subsfs.toSafeString().verify(subs.toString());

            Taint t = sb.toString().@internal@taint();
            Taint subt = subs.toString().@internal@taint();

            if (t == null)
                t = new Taint(false, sb.length());
            if (subt == null)
                subt = new Taint(false, subs.length());

            for (int i = beginIndex; i < endIndex; i++) {
                if (sb.charAt(i) != subs.charAt(i - beginIndex) 
                        || t.get(i) != subt.get(i-beginIndex))
                    throw new IllegalArgumentException("string taint/data bad");

            }
        }
    }

    private void testStringBufferReverse(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        StringBuffer retsb;
        SafeStringBuffer retsfsb;

        int len = sb.length();

        retsb = sb.reverse();
        retsfsb = sfsb.reverse();
        sbe.verify();

        if (sb.length() != len || retsb != sb || retsfsb != sfsb)
            throw new IllegalArgumentException(sbe.toString());
    }

    private void testStringBufferLength(StringBufferElem sbe) {
        if (sbe.getStringBuffer().length() != 
                sbe.getSafeStringBuffer().length())
            throw new IllegalArgumentException(sbe.toString());
    }

    private void testStringBufferSubstring(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        int beginIndex, endIndex;

        StringElem subse;
        String subs;
        SafeString subsfs;

        if (sr.nextInt(16) == 0) {
            beginIndex = sr.nextInt();
            endIndex = sr.nextInt();
        } else {
            beginIndex = sr.nextInt(sb.length());
            endIndex = beginIndex + sr.nextInt(sb.length()+1 - beginIndex);
        }

        if (beginIndex < 0 || endIndex > sb.length() || beginIndex > endIndex) {
            /* Exception time */
            SafeStringBuffer oldsfsb=new SafeStringBuffer(sfsb.toSafeString());
            boolean caught = false;

            try {
                subs = sb.substring(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(sbe.toString());

            caught = false;
            try {
                subsfs = sfsb.substring(beginIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
            }
            if (!caught || !oldsfsb.verify(sb)) 
                throw new IllegalArgumentException(sbe.toString());
        } else {
            if (endIndex == sb.length() && sr.nextBoolean()) {
                subs = sb.substring(beginIndex);
                subsfs = sfsb.substring(beginIndex);
            } else {
                subs = sb.substring(beginIndex, endIndex);
                subsfs = sfsb.substring(beginIndex, endIndex);
            }

            subsfs.verify(subs);

            Taint t = sb.toString().@internal@taint();
            Taint subt = subs.@internal@taint();

            if (t == null)
                t = new Taint(false, sb.length());
            if (subt == null)
                subt = new Taint(false, subs.length());

            for (int i = beginIndex; i < endIndex; i++) 
                if (sb.charAt(i) != subs.charAt(i - beginIndex) 
                        || t.get(i) != subt.get(i-beginIndex))
                    throw new IllegalArgumentException("string taint/data bad");

            subse = new StringElem(subs, subsfs, sr.nextInt());
            subse.verify();
            subse.addOp("[" + sbe.toString() + "] StringBuffer.substring");
            stringList.add(subse);
            
        }
    }

    private void testStringBufferToString(StringBufferElem sbe) {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();
        String s = sb.toString();
        SafeString sfs = sfsb.toSafeString();
        StringElem se = new StringElem(s, sfs, sr.nextInt());

        se.verify();
        sbe.verify(s);

        se.addOp("[" + sbe.toString() + "] StringBuffer.toString");
        stringList.add(se);
    }

    private  void testStringBufferTrimToSize(StringBufferElem sbe) {
        //[ifJava5+]
        sbe.getStringBuffer().trimToSize();
        sbe.getSafeStringBuffer().trimToSize();
        //[fiJava5+]
    }

    private void testStringBufferSerialize(StringBufferElem sbe) 
        throws IOException, ClassNotFoundException
    {
        StringBuffer sb = sbe.getStringBuffer();
        SafeStringBuffer sfsb = sbe.getSafeStringBuffer();

        sfsb = new SafeStringBuffer(new SafeString(
                                          sfsb.toSafeString().origString(),
                                          new SafeTaint(false, sfsb.length())));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bout);

        os.writeObject(sb);
        os.flush();
        os.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream is = new ObjectInputStream(bin);

        sb = (StringBuffer) is.readObject();
        is.close();
        bin.close();
        bout.close();

        if (sb.toString().@internal@isTainted())
            throw new RuntimeException("Corrupted Serialization");

        StringBufferElem newSbe = new StringBufferElem(sb,sfsb,sr.nextInt());
        newSbe.verify();
        sbe.addOp(StringBufferMethod.SB_SERIALIZE);
    }

    public void testStringBuffer() 
        throws IllegalArgumentException, IOException, ClassNotFoundException 
    { 
        int op = sr.nextInt(StringBufferMethod.SB_END);
        StringBufferElem sbe = null;

        if (stringBufferList.size() < threads * 2)
            op = StringBufferMethod.SB_NEW;

        if (op != StringBufferMethod.SB_NEW)  {
            sbe = removeRandomStringBufferElem();
            if (sbe == null) { 
                op = StringBufferMethod.SB_NEW;
            } else {
                sbe.verify();
                sbe.addOp(op);
            }
        }

        switch(op)
        {
            case StringBufferMethod.SB_NEW:
                testStringBufferNew();
                break;

            case StringBufferMethod.SB_APPEND:
                testStringBufferAppend(sbe);
                break;

           case StringBufferMethod.SB_CAPACITY:
                testStringBufferCapacity(sbe);
                break;

           case StringBufferMethod.SB_CHARAT:
                testStringBufferCharAt(sbe);
                break;

           case StringBufferMethod.SB_CODEPOINT:
           //[ifJava5+]
                testStringBufferCodePoint(sbe);
                break;
           //[fiJava5+]
           //...else fall through

           case StringBufferMethod.SB_DELETE:
                testStringBufferDelete(sbe);
                break;

           case StringBufferMethod.SB_ENSURECAPACITY:
                testStringBufferEnsureCapacity(sbe);
                break;

           case StringBufferMethod.SB_GETCHARS:
                testStringBufferGetChars(sbe);
                break;

           case StringBufferMethod.SB_INDEXOF:
                testStringBufferIndexOf(sbe);
                break;

           case StringBufferMethod.SB_INSERT:
                testStringBufferInsert(sbe);
                break;

           case StringBufferMethod.SB_LASTINDEXOF:
                testStringBufferLastIndexOf(sbe);
                break;

           case StringBufferMethod.SB_LENGTH:
                testStringBufferLength(sbe);
                break;

           case StringBufferMethod.SB_OFFSETBYCODEPOINTS:
                //[ifJava5+]
                testStringBufferOffsetByCodePoints(sbe);
                break;
                //[fiJava5+]
                //...else fall through

           case StringBufferMethod.SB_REPLACE:
                testStringBufferReplace(sbe);
                break;

           case StringBufferMethod.SB_REVERSE:
                testStringBufferReverse(sbe);
                break;

           case StringBufferMethod.SB_SETCHARAT:
                testStringBufferSetCharAt(sbe);
                break;

           case StringBufferMethod.SB_SETLENGTH:
                testStringBufferSetLength(sbe);
                break;

           case StringBufferMethod.SB_SUBSEQUENCE:
                testStringBufferSubSequence(sbe);
                break;

           case StringBufferMethod.SB_SUBSTRING:
                testStringBufferSubstring(sbe);
                break;
        
           case StringBufferMethod.SB_TOSTRING:
                testStringBufferToString(sbe);
                break;

           case StringBufferMethod.SB_TRIMTOSIZE:
                testStringBufferTrimToSize(sbe);
                break;

           case StringBufferMethod.SB_SERIALIZE:
                testStringBufferSerialize(sbe);
                break;

           default:
                throw new RuntimeException("switch");
        }

        if (op != StringBufferMethod.SB_NEW) {
            sbe.verify();
            stringBufferList.add(sbe);
        }
    }

    public void test() 
        throws IllegalArgumentException, UnsupportedEncodingException,
               IOException, ClassNotFoundException
    {
        int op = sr.nextInt(3);

        switch(op) 
        {
            case 0:
                testString();
                break;
          case 1:
                //[ifJava5+]
                testStringBuilder();
                break;
                //[fiJava5+]
                // ...else fall through
           case 2:
                testStringBuffer();
                break;
          default:
                throw new RuntimeException("Broken switch");
        }
    }

    public void run() {
        try {
            for (int i = 0; i < nrtest; i++)
                test();
        } catch (OutOfMemoryError oom) {
            System.out.println(oom);
            oom.printStackTrace();
            System.exit(0);
        } catch (Throwable e) {
            fail = true;
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {    
        StringTest[] t;
        int ret = 0;
        PrintStream ps = null;
        SafeRandom[] randArray;
        Random rbase;

        String logfile = "StringTest.log";
        long seed = System.currentTimeMillis();
        threads = 1;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-t"))
               threads = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-l"))
               maxlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java StringTest "
                       + "[-t numberofTheads] [-s randomSeed] "
                       + "[-l maximumLengthofNewString] "
                       + "[-n NumberofTestsPerThread]"
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
        ps.print(" -t ");
        ps.print(threads);
        ps.print(" -l ");
        ps.print(maxlen);
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        /* else List l = new synchronedList(... ) */
        t = new StringTest[threads];
        randArray = new SafeRandom[threads];
        rbase = new Random(seed);

        for (int i = 0; i < threads; i++)
            randArray[i] = new SafeRandom(rbase.nextLong());

        for (int i = 0; i < threads; i++) {
            t[i] = new StringTest(randArray[i]);
            t[i].start();
        }

        System.out.println("Started " + threads + " threads");
        for (int i = 0; i < threads; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                System.out.println("Interrupted!: " + e);
                System.exit(-1);
            }
        }

        if (fail) 
            ret = -1;

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }

        System.exit(ret);
     }
}  
