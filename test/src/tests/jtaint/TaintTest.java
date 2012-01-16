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

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Arrays;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public final class TaintTest extends Thread
{
    private static int maxtlen = 65536 * 2;
    private static int nrtest = 1024 * 32;
    private static List taintList;
    private static volatile boolean fail;
    private Random r;

    /* Enums exist only in JDK >= 1.5, so we do this the hard way */
    static final class TaintMethod
    {
        public static final int T_CLONE        = 0;
        public static final int T_NEW          = 1;
        public static final int T_SETLENGTH    = 2;
        public static final int T_SET          = 3;
        public static final int T_CLEAR        = 4;
        public static final int T_GET          = 5;
        public static final int T_SUBSET       = 6;
        public static final int T_DELETE       = 7;
        public static final int T_APPEND       = 8;
        public static final int T_INSERT       = 9;
        public static final int T_REVERSE      = 10;
        public static final int T_ISTAINTED    = 11;
        public static final int T_CARDINALITY  = 12;
        public static final int T_SETRANGE     = 13;
        public static final int T_CLEARRANGE   = 14;
        public static final int T_ASBITSET     = 15;
        public static final int T_GETCHARS     = 16;
        public static final int T_INSERTUNTAINTED     = 17;
        public static final int T_END          = 18;

        public static final String[] methodNames = {
            "clone",
            "new",
            "setLength",
            "set",
            "clear",
            "get",
            "subset",
            "delete",
            "append",
            "insert",
            "reverse",
            "isTainted",
            "cardinality",
            "set(range)",
            "clear(range)",
            "asBitSet",
            "getTaintAsChars",
            "insertUntainted"
        };
    }

    static final class TaintElem
    {
        /* If we ever declare taintList to be a ConcurrentQueue or other
         * non-synchronized, thread-safe data structure, mark all these
         * fields final and declare opList as a ConcurrentQueue
         */
        Taint t;
        SafeTaint st;
        List opList;
        int id;

        public TaintElem(Taint t, SafeTaint st, int id) {
            this.t = t;
            this.st = st;
            this.opList = new ArrayList();
            this.id = id;
        }

        public Taint getTaint() { return t; }
        public SafeTaint getSafeTaint() { return st; }

        public void addOp(int op) { opList.add(TaintMethod.methodNames[op]); }

        public void verify(Taint vt) throws IllegalArgumentException {
            if (!st.verify(vt))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify(TaintElem otherte) throws IllegalArgumentException {
            SafeTaint otherst = otherte.getSafeTaint();
            if (!st.verify(t) || !st.verify(otherte.getTaint()) ||
                    !otherst.verify(otherte.getTaint()) || !otherst.verify(t))
                throw new IllegalArgumentException(this.toString());
        }

        public void verify() throws IllegalArgumentException {
            verify(t);
        }
        
        public String toString() {
            String s = "id " + id + " taint " + t.toString() + " safetaint " + 
                st.toString() + " operations: ";
            for (int i = 0; i < opList.size(); i++)
                s += (String) opList.get(i);
            return s;
        }
    }

    public TaintTest(Random r) {
        this.r = r;
    }

    private void testClone(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();

        SafeTaint newst = (SafeTaint) st.clone();
        Taint newt = (Taint) t.clone();
        TaintElem newte = new TaintElem(newt, newst, r.nextInt());

        te.verify(newte);
        newte.verify();
        newte.addOp(TaintMethod.T_CLONE);
        taintList.add(newte);
    }

    private void testNew() throws IllegalArgumentException {
        Taint t;
        SafeTaint st;
        TaintElem te;

        int len = r.nextInt(maxtlen);
        boolean b[] = new boolean[len];
        BitSet bs = new BitSet();
        char[] c = new char[(len+15)/16];

        /* Initialize booleans...*/
        for (int i = 0; i < len; i++)
            b[i] = r.nextBoolean();
        /* ... and convert to BitSet */
        for (int i = 0; i < len; i++)
            if (b[i]) bs.set(i);
        /* ... and finally to char[] */
        for (int i = 0; i < len; i++)
            if (b[i]) 
                c[i/16] |= 1 << (i & 15);

        switch(r.nextInt(4))
        {
            /* Taint() */
            case 0:
                t = new Taint();
                if (len != 0)
                    t.setLength(len);
                for (int i = 0; i < len; i++)
                    if (bs.get(i))
                        t.set(i);
                break;

                /* Taint(Bitset bs, int len) */
            case 1:
                t = new Taint(bs, len);
                break;

                /* Taint(boolean fill, int len */
            case 2:
                if (r.nextBoolean()) {
                    t = new Taint(true, len);
                    for (int i = 0; i < len; i++)
                        if (!b[i])
                            t.clear(i);
                } else {
                    t = new Taint(false, len);
                    for (int i = 0; i < len; i++)
                        if (b[i])
                            t.set(i);
                }
                break;

                /* Taint(char[] t, int len) */
           case 3: 
                t = new Taint(c, len);
                break;

            default:
                throw new RuntimeException("broken switch");
        }

        switch(r.nextInt(5))
        {
            /* SafeTaint() */
            case 0:
                st = new SafeTaint();
                if (len != 0)
                    st.setLength(len);
                for (int i = 0; i < len; i++)
                    if (bs.get(i))
                        st.set(i);
                break;

                /* SafeTaint(Bitset bs, int len) */
            case 1:
                st = new SafeTaint(bs, len);
                break;

                /* SafeTaint(boolean fill, int len */
            case 2:
                if (r.nextBoolean()) {
                    st = new SafeTaint(true, len);
                    for (int i = 0; i < len; i++)
                        if (!b[i])
                            st.clear(i);
                } else {
                    st = new SafeTaint(false, len);
                    for (int i = 0; i < len; i++)
                        if (b[i])
                            st.set(i);
                }

                break;

                /* SafeTaint(boolean[] b) */
            case 3:
                st = new SafeTaint(b);
                break;

                /* SafeTaint(char[] t, int len) */
            case 4: 
                st = new SafeTaint(c, len);
                break;

            default:
                throw new RuntimeException("broken switch");
        }

        te = new TaintElem(t, st, r.nextInt());
        te.addOp(TaintMethod.T_NEW);
        te.verify();
        taintList.add(te);
    }

    private int randSafeIndex(TaintElem te)
    {
        SafeTaint st = te.getSafeTaint();
        if (st.length() != 0)
            return r.nextInt(st.length());
        else
            return 0;
    }

    private void testSetLength(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int len;

        int m = maxtlen < st.length() ? st.length() + 1024 : maxtlen;

        if (r.nextInt(16) == 0)
            len = -r.nextInt(m);
        else
            len = r.nextInt(m);

        if (len < 0) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.setLength(len);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.setLength(len);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            TaintElem tmp = new TaintElem(t.setLength(len),
                    st.setLength(len), r.nextInt());
            te.verify(tmp);
            if (t.length() != len || st.length() != len)
                throw new IllegalArgumentException(te.toString());
        }
    }

    private void testSet(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int index;

        /* Potentially go out of bounds */
        if (r.nextInt(16) == 0) 
            index = r.nextInt();
        else
            index = randSafeIndex(te);

        if (index < 0 || index >= st.length()) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.set(index);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.set(index);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            t.set(index);
            st.set(index);
            TaintElem tmp = new TaintElem(t, st, r.nextInt());
            te.verify(tmp);
            if (!t.get(index) || !st.get(index))
                throw new IllegalArgumentException(te.toString());
        }
    }

    private void testGet(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int index;

        /* Potentially go out of bounds */
        if (r.nextInt(16) == 0) 
            index = r.nextInt();
        else
            index = randSafeIndex(te);

        if (index < 0 || index >= st.length()) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.get(index);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.get(index);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            if (t.get(index) != st.get(index))
                throw new IllegalArgumentException(te.toString());
        }
    }

    private void testClear(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int index;

        /* Potentially go out of bounds */
        if (r.nextInt(16) == 0) 
            index = r.nextInt();
        else
            index = randSafeIndex(te);

        if (index < 0 || index >= st.length()) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.clear(index);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.clear(index);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            t.clear(index);
            st.clear(index);
            TaintElem tmp = new TaintElem(t, st, r.nextInt());
            te.verify(tmp);
            if (t.get(index) || st.get(index))
                throw new IllegalArgumentException(te.toString());
        }
    }

    private void testDelete(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int beginIndex, endIndex;

        if (r.nextInt(16) == 0) {
            beginIndex = r.nextInt();
            endIndex = r.nextInt();
        } else {
            beginIndex = randSafeIndex(te);
            endIndex = randSafeIndex(te);
            if (beginIndex > endIndex) {
                int tmp = endIndex;
                endIndex = beginIndex;
                beginIndex = tmp;
            }
        }

        if (beginIndex < 0 || beginIndex > endIndex || 
                beginIndex > st.length()) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.delete(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.delete(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            int newlen;

            if (endIndex > st.length())
                endIndex = st.length();

            newlen  = st.length() - (endIndex - beginIndex);
            SafeTaint oldst = (SafeTaint) st.clone();
            TaintElem tmp = new TaintElem(t.delete(beginIndex, endIndex),
                    st.delete(beginIndex, endIndex), r.nextInt());
            te.verify(tmp);

            if (t.length() != newlen || st.length() != newlen)
                throw new IllegalArgumentException(te.toString());

            for (int i = 0; i < newlen; i++) {
                int origidx;
                if (i < beginIndex)
                    origidx = i;
                else
                    origidx = i + (endIndex - beginIndex);
                if (t.get(i) != st.get(i) || st.get(i) != oldst.get(origidx))
                    throw new IllegalArgumentException(te.toString());
            }
        }
    }

    private void testSubSet(TaintElem te) throws IllegalArgumentException {
        int beginIndex, endIndex;
        Taint t = te.getTaint(), subt;
        SafeTaint st = te.getSafeTaint(), subst;
        TaintElem subte;

        if (r.nextInt(16) == 0) {
            beginIndex = r.nextInt();
            endIndex = r.nextInt();
        } else {
            beginIndex = randSafeIndex(te);
            endIndex = randSafeIndex(te);
            if (beginIndex > endIndex) {
                int tmp = endIndex;
                endIndex = beginIndex;
                beginIndex = tmp;
            }
        }

        if (beginIndex > endIndex || endIndex > st.length() || 
                beginIndex < 0 || endIndex < 0) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                subt = t.subset(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                subst = st.subset(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            Taint oldt = (Taint) t.clone();
            SafeTaint oldst = (SafeTaint) st.clone();
            t.subset(beginIndex, endIndex);
            st.subset(beginIndex, endIndex);

            if (t.length() != endIndex-beginIndex ||
                    st.length() != endIndex-beginIndex)
                throw new IllegalArgumentException(te.toString());

            for (int i = beginIndex; i < endIndex; i++) 
                if (t.get(i-beginIndex) != st.get(i-beginIndex)
                        || t.get(i-beginIndex) != oldt.get(i)
                        || st.get(i-beginIndex) != oldst.get(i) 
                        || oldt.get(i) != oldst.get(i))
                    throw new IllegalArgumentException(te.toString());
        }
    }

    private void testAppend(TaintElem te) throws IllegalArgumentException {
        TaintElem otherte, tmp;
        Taint t = te.getTaint(), othert;
        SafeTaint st = te.getSafeTaint(), otherst, oldst;
        int oldlen = st.length();

        synchronized(taintList) {
            if (taintList.size() == 0)
                return;
            otherte = (TaintElem) taintList.remove(r.nextInt(
                        taintList.size()));
        }

        othert = otherte.getTaint();
        otherst = otherte.getSafeTaint();

        oldst = (SafeTaint) st.clone();
        tmp = new TaintElem(t.append(othert), st.append(otherst),
                r.nextInt()); 

        te.verify(tmp);

        for (int i = 0; i < st.length(); i++) {
            if (st.get(i) != t.get(i))
                throw new IllegalArgumentException(te.toString());
            if (i >= oldlen && (st.get(i) != otherst.get(i-oldlen) || 
                        t.get(i) != othert.get(i-oldlen)))
                throw new IllegalArgumentException(te.toString());
        }

        taintList.add(otherte);
    }

    private void testInsert(TaintElem te) throws IllegalArgumentException {
        int offset;
        TaintElem otherte;
        Taint t = te.getTaint(), othert;
        SafeTaint st = te.getSafeTaint(), otherst;
        int oldlen = st.length();

        synchronized(taintList) {
            if (taintList.size() == 0)
                return;
            otherte = (TaintElem) taintList.remove(r.nextInt(taintList.size()));
        }

        otherte.verify();
        othert = otherte.getTaint();
        otherst = otherte.getSafeTaint();

        if (r.nextInt(16) == 0) 
            offset = r.nextInt();
        else
            offset = randSafeIndex(te);

        if (offset < 0 || offset > st.length()) {
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.insert(offset,othert);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.insert(offset, otherst);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            TaintElem tmp = new TaintElem(t.insert(offset, othert),
                    st.insert(offset,otherst), 
                    r.nextInt());
            te.verify(tmp);

            for (int i = 0; i < st.length(); i++) {
                if (st.get(i) != t.get(i))
                    throw new IllegalArgumentException(te.toString());
                
                if (i >= offset && i < offset + otherst.length() &&
                        st.get(i) != otherst.get(i-offset)) 
                    throw new IllegalArgumentException(te.toString());
            }
        }

        taintList.add(otherte);
    }

    private void testInsertUntainted(TaintElem te) 
        throws IllegalArgumentException 
    {
        int offset;
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int oldlen = st.length();

        if (r.nextInt(16) == 0)  
            offset = r.nextInt();
        else
            offset = randSafeIndex(te);
        int inslen = r.nextInt(1 + (st.length() >= maxtlen ? 32 : st.length()));

        if (offset < 0 || offset > st.length()) {
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.insertUntainted(offset, inslen);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.insert(offset, new SafeTaint(false, inslen));
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            t.insertUntainted(offset, inslen);
            st.insert(offset, new SafeTaint(false, inslen));

            for (int i = 0; i < st.length(); i++) 
                if (st.get(i) != t.get(i))
                    throw new IllegalArgumentException(te.toString());
        }
    }

    private void testReverse(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint(), oldst = (SafeTaint) st.clone();
        TaintElem tmp = new TaintElem(t.reverse(), st.reverse(), r.nextInt());

        te.verify(tmp);

        if (st.length() != t.length() || 
                st.length() != oldst.length())
            throw new IllegalArgumentException(te.toString());

        for (int i = 0; i < st.length(); i++)
            if (st.get(i) != t.get(i) || 
                    st.get(i) != oldst.get(st.length() - 1 - i))
                throw new IllegalArgumentException(te.toString());
    }

    private void testIsTainted(TaintElem te) {
        if (te.getTaint().isTainted() != te.getSafeTaint().isTainted())
                            throw new IllegalArgumentException(te.toString());
    }

    private void testCardinality(TaintElem te) {
        if (te.getTaint().cardinality() != te.getSafeTaint().cardinality())
                            throw new IllegalArgumentException(te.toString());
    }

    private void testSetRange(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int beginIndex, endIndex;

        if (r.nextInt(16) == 0) {
            beginIndex = r.nextInt();
            endIndex = r.nextInt();
        } else {
            beginIndex = randSafeIndex(te);
            endIndex = randSafeIndex(te);
            if (beginIndex > endIndex) {
                int tmp = endIndex;
                endIndex = beginIndex;
                beginIndex = tmp;
            }
        }

        if (beginIndex > endIndex || endIndex >= st.length() || 
                beginIndex < 0 || endIndex < 0) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.set(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.set(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            t.set(beginIndex, endIndex);
            st.set(beginIndex, endIndex);

            TaintElem tmp = new TaintElem(t, st, r.nextInt());
            te.verify(tmp);

            for (int i = beginIndex; i < endIndex; i++) {
                if (!t.get(i) || !st.get(i))
                    throw new IllegalArgumentException(te.toString());
            }
        }
    }

    private void testClearRange(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();
        int beginIndex, endIndex;

        if (r.nextInt(16) == 0) {
            beginIndex = r.nextInt();
            endIndex = r.nextInt();
        } else {
            beginIndex = randSafeIndex(te);
            endIndex = randSafeIndex(te);
            if (beginIndex > endIndex) {
                int tmp = endIndex;
                endIndex = beginIndex;
                beginIndex = tmp;
            }
        }

        if (beginIndex > endIndex || endIndex >= st.length() || 
                beginIndex < 0 || endIndex < 0) {
            /* Fail deliberately */ 
            SafeTaint oldst = (SafeTaint) st.clone();
            boolean caught = false;

            try {
                t.clear(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }

            if (!caught) 
                throw new IllegalArgumentException(te.toString());

            caught = false;
            try {
                st.clear(beginIndex, endIndex);
            } catch (IllegalArgumentException e) {
                caught = true;
            }
            if (!caught || !oldst.verify(t)) 
                throw new IllegalArgumentException(te.toString());
        } else {
            t.clear(beginIndex, endIndex);
            st.clear(beginIndex, endIndex);
            TaintElem tmp = new TaintElem(t, st, r.nextInt());
            te.verify(tmp);

            for (int i = beginIndex; i < endIndex; i++) {
                if (t.get(i) || st.get(i))
                    throw new IllegalArgumentException(te.toString());
            }
        }
    }

    private void testAsBitSet(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();

        if (!st.asBitSet().equals(t.asBitSet()))
            throw new IllegalArgumentException(te.toString());
    }

    private void testGetChars(TaintElem te) throws IllegalArgumentException {
        Taint t = te.getTaint();
        SafeTaint st = te.getSafeTaint();

        int pad = r.nextInt(32);
        char[] tchars = new char[pad + (t.length() + 15)/16];
        char[] stchars = new char[pad + (st.length() + 15)/16];

        int offset;
        if (pad == 0)
            offset = 0;
        else
            offset = r.nextInt(pad);

        t.getTaintAsChars(tchars, offset);
        st.getTaintAsChars(stchars, offset);

        if (!Arrays.equals(tchars, stchars))
            throw new IllegalArgumentException(te.toString());
    }

    public void test() throws IllegalArgumentException {
        int op = r.nextInt(TaintMethod.T_END+1);
        TaintElem te = null;

        synchronized(taintList) {
            if (taintList.size() < 3)
                op = TaintMethod.T_NEW;
            else
                te = (TaintElem) taintList.remove(r.nextInt(taintList.size()));
        }

        if (op != TaintMethod.T_NEW) {
            te.verify();
            if (op != TaintMethod.T_END)
                te.addOp(op);
        }

        switch(op)
        {
            case TaintMethod.T_CLONE:
                testClone(te);
                break;

            case TaintMethod.T_NEW:
                testNew();
                break;

            case TaintMethod.T_SETLENGTH:
                testSetLength(te);
                break;

            case TaintMethod.T_SET:
                testSet(te);
                break;

            case TaintMethod.T_GET:
                testGet(te);
                break;

            case TaintMethod.T_CLEAR:
                testClear(te);
                break;

            case TaintMethod.T_SUBSET:
                testSubSet(te);
                break;

            case TaintMethod.T_DELETE:
                testDelete(te);
                break;

            case TaintMethod.T_APPEND:
                testAppend(te);
                break;

            case TaintMethod.T_INSERT:
                testInsert(te);
                break;

            case TaintMethod.T_REVERSE:
                testReverse(te);
                break;

            case TaintMethod.T_ISTAINTED:
                testIsTainted(te);
                break;

            case TaintMethod.T_CARDINALITY:
                testCardinality(te);
                break;

            case TaintMethod.T_SETRANGE:
                testSetRange(te);
                break;

            case TaintMethod.T_CLEARRANGE:
                testClearRange(te);
                break;

            case TaintMethod.T_ASBITSET:
                testAsBitSet(te);
                break;

            case TaintMethod.T_GETCHARS:
                testGetChars(te);
                break;

            case TaintMethod.T_INSERTUNTAINTED:
                testInsertUntainted(te);
                break;

            case TaintMethod.T_END: 
                /* do nothing, re-insert original entry back into the list */
                break;

            default:
                throw new RuntimeException("broken switch");
        }

        if (op != TaintMethod.T_NEW) {
            /* Add te back to the list */
            te.verify();
            taintList.add(te);
        }
    }

    public void run() {
        try {
            for (int i = 0; i < nrtest; i++)
                test();
        } catch (Throwable e) {
            fail = true;
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {    
        TaintTest[] t;
        int ret = 0;
        PrintStream ps = null;
        Random[] randArray;
        Random rbase;

        String logfile = "TaintTest.log";
        long seed = System.currentTimeMillis();
        int threads = 1;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-t"))
               threads = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-l"))
               maxtlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java TaintTest "
                       + "[-t numberofTheads] [-s randomSeed] "
                       + "[-l maximumLengthofTaint] "
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
        ps.print(maxtlen);
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        /* else List l = new synchronedList(... ) */
        taintList = new ArrayList();
        t = new TaintTest[threads];
        randArray = new Random[threads];
        rbase = new Random(seed);

        for (int i = 0; i < threads; i++)
            randArray[i] = new Random(rbase.nextLong());

        if (threads > 1)
            taintList = Collections.synchronizedList(taintList);

        for (int i = 0; i < threads; i++) {
            t[i] = new TaintTest(randArray[i]);
            t[i].start();
        }

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
