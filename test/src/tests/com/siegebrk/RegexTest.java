/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.BitSet;

public class RegexTest
{
    private static void verify(String s, String data, BitSet b) {
        if (!s.equals(data))
            throw new RuntimeException("Unexpected data : " +
                    "got " + s + " expected " + data);
        Taint t = s.@internal@taint();
        if (t == null)
            t = new Taint(false, s.length());
        if (!b.equals(t.asBitSet()))
            throw new RuntimeException("Unexpected taint: " +
                    "got " + s.@internal@taint() + " expected " + b);
    }

    /** Test string regex replacement methods. Any characters in the
     * string not matched by the regular expression should retain their
     * prior taint values. Any substrings matched by the regular expression
     * in the replace method call should have their taint cleared.
     */
    private static void testReplace() {
        String t1 = new String("abcdefabcdefgh");
        t1 = new String(t1, new Taint(true, t1.length()));

        String p1 = new String("ijklmnop");
        BitSet b = new BitSet();
        b.set(1); b.set(3); b.set(5); b.set(7);
        p1 = new String(p1, new Taint(b, p1.length()));

        /* Simple replacement tests */
        b = new BitSet();
        b.set(0); b.set(4); b.set(5); b.set(6); b.set(7);
        b.set(8); b.set(9); b.set(10); b.set(11); b.set(12); b.set(13);
        verify(t1.replaceFirst("bcd", "def"), "adefefabcdefgh", b);

        b = new BitSet();
        b.set(0); b.set(1); b.set(2); b.set(3); b.set(4); b.set(5);
        b.set(6); b.set(7); b.set(8); b.set(9); b.set(10); b.set(11);
        verify(t1.replaceFirst("gh", "moo"), "abcdefabcdefmoo", b);

        b = new BitSet();
        b.set(3); b.set(4); b.set(5); b.set(6); b.set(7); b.set(8); 
        b.set(9); b.set(10); b.set(11); b.set(12); b.set(13);
        verify(t1.replaceFirst("abc", "xyz"), "xyzdefabcdefgh", b);

        /* Partial taint replacement test */
        b = new BitSet();
        b.set(5); b.set(7);
        verify(p1.replaceFirst("jkl", "123"), "i123mnop", b);

        /* Simple replaceAll */
        b = new BitSet();
        b.set(3); b.set(4); b.set(5); b.set(9); b.set(10); b.set(11);
        b.set(12); b.set(13);
        verify(t1.replaceAll("abc", "123"), "123def123defgh", b);

        b = new BitSet();
        b.set(1); b.set(3); b.set(10);
        verify(p1.replaceAll("mno", "minnow"), "ijklminnowp", b);

        /* More complex queries */
        b = new BitSet();
        b.set(0, t1.length());

        verify(t1.replaceFirst("^def", "blah"), "abcdefabcdefgh", b);

        b = new BitSet();
        b.set(3); b.set(4); b.set(5); b.set(6); b.set(7); b.set(8);
        b.set(9); b.set(10); b.set(11); b.set(12); b.set(13);
        verify(t1.replaceAll("^abc", "moo"), "moodefabcdefgh", b);

        b = new BitSet();
        b.set(3); b.set(4); b.set(5); b.set(9); b.set(10); b.set(11);
        b.set(12); b.set(13);
        verify(t1.replaceAll("[a-c]+", "XXX"), "XXXdefXXXdefgh", b);
    }

    /** Test String split methods. Each non-empty substring should have the
     * same taint values as in the original (non-split) String
     */

    private static void testSplit() {
        String t1 = new String("abc:def:abc:def:gh");
        t1 = new String(t1, new Taint(true, t1.length()));

        String p1 = new String("i:j:k:lmno:p:");
        BitSet b = new BitSet();
        b.set(1); b.set(2); b.set(3); b.set(4); b.set(7); b.set(11);
        p1 = new String(p1, new Taint(b, p1.length()));

        String[] r = t1.split(":");
      
        /* Simple split test */ 
        b = new BitSet();
        b.set(0, 3); 
        if (r.length != 5) 
            throw new RuntimeException("Unexpected split() array " + r);
        verify(r[0], "abc", b);
        verify(r[1], "def", b);
        verify(r[2], "abc", b);
        verify(r[3], "def", b);
        b.clear(2);
        verify(r[4], "gh", b);

        /* Partial taint split test */
        r = p1.split(":");

        b = new BitSet();
        if (r.length != 5) 
            throw new RuntimeException("Unexpected split() array " + r);

        verify(r[0], "i", b);
        b.set(0);
        verify(r[1], "j", b);
        verify(r[2], "k", b);
        b.clear(0);
        b.set(1);
        verify(r[3], "lmno", b);
        b.clear(1);
        b.set(0);
        verify(r[4], "p", b);

        /* Partial taint split test - positive limit */
        r = p1.split(":", 3);

        b = new BitSet();
        if (r.length != 3) 
            throw new RuntimeException("Unexpected split() array " + r);

        verify(r[0], "i", b);
        b.set(0);
        verify(r[1], "j", b);
        b.set(3); b.set(7);
        verify(r[2], "k:lmno:p:", b);

        /* Partial taint split test - negative limit */
        r = p1.split(":", -3);

        b = new BitSet();
        if (r.length != 6) 
            throw new RuntimeException("Unexpected split() array " + r);

        verify(r[0], "i", b);
        b.set(0);
        verify(r[1], "j", b);
        verify(r[2], "k", b);
        b.clear(0);
        b.set(1);
        verify(r[3], "lmno", b);
        b.clear(1);
        b.set(0);
        verify(r[4], "p", b);
        b.clear(0);
        verify(r[5], "", b);
    }

    public static void main(String[] args) {
        testReplace();
        testSplit();
        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}


