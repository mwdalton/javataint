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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class TestUtil
{
    public static final int UNTAINTED = 0;
    public static final int FULLY_TAINTED = 1;

    private final SafeRandom sr;
    private final int defaultMax;
    private final int p;

    public TestUtil(int maxlen, SafeRandom sr, int p) {
        this.defaultMax = maxlen;
        this.sr = sr;
        this.p = p;
    }

    public SafeRandom rand() { return sr; }

    //[ifJava5+]
    public String randString(int maxlen) {
        int len = sr.nextInt(maxlen); 
        StringBuilder sb = new StringBuilder(len);
        BitSet b = new BitSet();

        if (sr.nextBoolean() && len == 0) len = 1;

        for (int i = 0; i < len; i++) {
            int c, clen;
            boolean taint = sr.nextInt(p) == 0 && p > 0;
            int oldlen = sb.length();
           
            c = Character.MIN_CODE_POINT + sr.nextInt(
                    Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);

            sb.append(Character.toChars(c));

            clen = Character.charCount(c);
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
        }

        return new String(sb.toString(), new Taint(b, sb.length()));
    }
    //[fiJava5+]
    //[ifJava4]
    public String randString(int maxlen) {
        int len = sr.nextInt(maxlen); 
        StringBuffer sb = new StringBuffer(len);
        BitSet b = new BitSet();

        if (sr.nextBoolean() && len == 0) len = 1;

        for (int i = 0; i < len; i++) {
            boolean taint = sr.nextInt(p) == 0 && p > 0;
            sb.append((char) sr.nextInt(0xffff + 1));
            b.set(sb.length() - 1, taint);
        }

        return new String(sb.toString(), new Taint(b, sb.length()));
    }
    //[fiJava4]

    public String randString() { return randString(defaultMax); }

    public String[] randStringArray(int maxlen) {
        List l = new ArrayList();
        int len = sr.nextInt(maxlen);

        while (len >= 0) {
            String s = randString(len);
            l.add(s);
            len -= s.length();
        }

        return (String[]) l.toArray(new String[l.size()]);
    }

    public String[] randStringArray() { return randStringArray(defaultMax); }

    public Map randMap(int maxlen) {
        HashMap h = new HashMap();
        int len = sr.nextInt(maxlen);

        while (len >= 0) {
            String key = randString(len);
            String[] value = randStringArray(len);

            len -= key.length();

            for (int i = 0; i < value.length; i++)
                len -= value[i].length();
            h.put(key, value);
        }


        return h;
    }

    public Map randMap() { return randMap(defaultMax); }

    public Hashtable randHashtable(int maxlen) {
        Hashtable h = new Hashtable();
        int len = sr.nextInt(maxlen);

        while (len >= 0) {
            String key = randString(len);
            String[] value = randStringArray(len);

            len -= key.length();

            for (int i = 0; i < value.length; i++)
                len -= value[i].length();
            h.put(key, value);
        }

        return h;
    }

    public Hashtable randHashtable() { return randHashtable(defaultMax); }

    public boolean isValidTaintedString (String s, String taintS) {
        if (!s.equals(taintS)) 
            return false;

        Taint t = s.@internal@taint();
        if (t == null)
            t = new Taint(false, s.length());

        Taint tt = taintS.@internal@taint();
        if (tt == null)
            tt = new Taint(false, taintS.length());

        for (int i = 0; i < taintS.length(); i++) {
            if ((p == UNTAINTED && t.get(i))
               || (p == FULLY_TAINTED && !t.get(i)))  
                   return false;

            if (!tt.get(i)) 
                return false;
            
        }

        return true;
    }

    public boolean isValidTaintedStringArray(String[] s, String[] taintS) {
        if (taintS.length != s.length)
            return false;

        for (int i = 0; i < taintS.length; i++)
            if (!isValidTaintedString(s[i], taintS[i]))
                return false;
        return true;
    }

    public boolean isValidTaintedMap(Map m, Map taintM) {
        Set entries = m.entrySet();
        Set taintEntries = taintM.entrySet();

        Map.Entry[] e;
        Map.Entry[] taintE;
        e = (Map.Entry[]) entries.toArray(new Map.Entry[entries.size()]);
        taintE = (Map.Entry[]) 
            taintEntries.toArray(new Map.Entry[taintEntries.size()]);

        if (e.length != taintE.length)
            return false;

        for (int i = 0; i < e.length; i++) {
            String s = (String) e[i].getKey();
            String[] sa = (String []) e[i].getValue();
            boolean found = false;

            for (int j = 0; j < taintE.length; j++) {
                String taintS = (String) taintE[j].getKey();

                if (taintS.equals(s)) {
                    if (!isValidTaintedString(s, taintS))
                        return false;
                    found = true;
                    break;
                }
            }

            if (!found)
                return false;
            found = false;

            for (int j = 0; j < taintE.length; j++) {
                String[] taintSa = (String[]) taintE[j].getValue();

                if (Arrays.equals(sa, taintSa)) {
                    if (!isValidTaintedStringArray(sa, taintSa))
                        return false;
                    found = true;
                    break;
                }
            }

            if (!found)
                return false;
        }

        return true;
    }

    public boolean isValidTaintedHashtable(Hashtable h, Hashtable taintH)
    {
        Set entries = h.entrySet();
        Set taintEntries = taintH.entrySet();

        Map.Entry[] e;
        Map.Entry[] taintE;
        e = (Map.Entry[]) entries.toArray(new Map.Entry[entries.size()]);
        taintE = (Map.Entry[]) 
            taintEntries.toArray(new Map.Entry[taintEntries.size()]);

        if (e.length != taintE.length)
            return false;

        for (int i = 0; i < e.length; i++) {
            String s = (String) e[i].getKey();
            String[] sa = (String []) e[i].getValue();
            boolean found = false;

            for (int j = 0; j < taintE.length; j++) {
                String taintS = (String) taintE[j].getKey();

                if (taintS.equals(s)) {
                    if (!isValidTaintedString(s, taintS))
                        return false;
                    found = true;
                    break;
                }
            }

            if (!found)
                return false;
            found = false;

            for (int j = 0; j < taintE.length; j++) {
                String[] taintSa = (String[]) taintE[j].getValue();

                if (Arrays.equals(sa, taintSa)) {
                    if (!isValidTaintedStringArray(sa, taintSa))
                        return false;
                    found = true;
                    break;
                }
            }

            if (!found)
                return false;
        }

        return true;
    }
    
    public boolean isValidTaintedEnumeration(Enumeration e, Enumeration taintE)
    {
        while (e.hasMoreElements()) {
            String s, taintS;
            if (!taintE.hasMoreElements())
                return false;
            s = (String) e.nextElement();
            taintS = (String) taintE.nextElement();

            if (!isValidTaintedString(s, taintS))
                return false;
        }

        if (taintE.hasMoreElements())
            return false;
        return true;
    }
}
