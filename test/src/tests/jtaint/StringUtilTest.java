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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class StringUtilTest 
{
    private final int maxlen;
    private final SafeRandom sr;
    private final TestUtil tu;

    public StringUtilTest(int maxlen, SafeRandom sr) {
        this.maxlen = maxlen;
        this.sr = sr;
        this.tu = new TestUtil(maxlen, sr, 64);
    }

    private void testString() {
        String s = tu.randString();
        if(!tu.isValidTaintedString(s, StringUtil.toTainted(s)))
                throw new RuntimeException(s);
    }

    private void testUntaintString() {
        String s = tu.randString();
        s = new String(s, new Taint(true, s.length()));
        if (s.length() > 0 && !s.@internal@isTainted())
            throw new RuntimeException("Error tainting string " + s +
                                       " taint " + s.@internal@taint());
        s = StringUtil.toUntainted(s);
        if (s.@internal@isTainted())
            throw new RuntimeException("Error untainting string " + s +
                                       " taint " + s.@internal@taint());
    }
    private void testStringArray() {
        String[] s = tu.randStringArray();
        String[] taintS = StringUtil.toTainted(s);
        if (!tu.isValidTaintedStringArray(s, taintS))
            throw new RuntimeException("Invalid array");
    }

    private void testStringBuffer() {
        StringBuffer sb = new StringBuffer(tu.randString());
        StringBuffer taintSb = StringUtil.toTainted(sb);
        if (!tu.isValidTaintedString(sb.toString(), taintSb.toString()))
            throw new RuntimeException("Invalid sb: " + sb);

    }

    //[ifJava5+]
    private void testStringBuilder() {
        StringBuilder sb = new StringBuilder(tu.randString());
        StringBuilder taintSb = StringUtil.toTainted(sb);
        if (!tu.isValidTaintedString(sb.toString(), taintSb.toString()))
            throw new RuntimeException("Invalid sb:" + sb);
    }
    //[fiJava5+]

    private void testMap() {
        Map m = tu.randMap();
        Map taintM = StringUtil.toTainted(m);
        if (!tu.isValidTaintedMap(m, taintM))
                throw new RuntimeException("Invalid map");
    }

    private void testHashtable() {
        Hashtable h = tu.randHashtable();
        Hashtable taintH = StringUtil.toTainted(h);
        if (!tu.isValidTaintedHashtable(h, taintH))
            throw new RuntimeException("Invalid hashtable");
    }


    private void testEnumeration() {
        Hashtable h = tu.randHashtable();
        Enumeration e = h.keys();
        Enumeration taintE = StringUtil.toTainted(h.keys());
        if (!tu.isValidTaintedEnumeration(e, taintE))
            throw new RuntimeException("Invalid enumeration");
    }

    private void test() {
        switch(sr.nextInt(8)) {
            case 0:
                testString();
                break;

            case 1:
                testStringArray();
                break;

            case 2:
                testStringBuffer();
                break;

            case 3:
                //[ifJava5+]
                testStringBuilder();
                break;
                //[fiJava5+]
                // ...else fall through

           case 4:
                testMap();
                break;

           case 5:
                testHashtable();
                break;

           case 6:
                testEnumeration();
                break;

            case 7:
                testUntaintString();
                break;

          default:
                throw new RuntimeException("switch");
        }
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 128;
        int nrtest = 16384;

        SafeRandom sr;
        StringUtilTest sut;
        String logfile = "StringUtilTest.log";
        PrintStream ps = null;

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
               System.out.println("Usage: java StringUtilTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofString] "
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

        sr = new SafeRandom(seed);
        sut = new StringUtilTest(maxlen, sr);

        for (int i = 0; i < nrtest; i++) 
            sut.test();

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
