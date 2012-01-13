/* Copyright 2009 Michael Dalton */
package jtaint;

import java.util.BitSet;

public final class Taint implements Cloneable
{
    private int len;
    private BitSet b;

    public Object clone() {
        Taint t;
        try {
            t = (Taint) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("JDK Clone Error");
        }
        t.b = (BitSet) b.clone();
        t.verifyTaint();
        return t;
    }

    private void verifyTaint() {
        if (b.nextSetBit(len) != -1)
            throw new IndexOutOfBoundsException("Invalid Taint: bit "
                    + b.nextSetBit(len) + " set but length is " + len);
    }

    public Taint() {
        b = new BitSet();
    }

    public Taint(BitSet b, int len) {
        this.b = (BitSet) b.clone();
        this.len = len;
        verifyTaint();
    }

    public Taint(boolean fill, int len) {
        b = new BitSet(fill ? len : 0);
        if (len > 0 && fill) {
            b.set(0,len);
        }
        this.len = len;
        verifyTaint();
    }

    public Taint(char[] t, int len) {
        b = new BitSet(len);

        for (int i = 0; i < t.length; i++)
            for (int j = 0; j < 16; j++)
                if ((t[i] & (1 << j)) != 0)
                    b.set(i * 16 + j);

        if (t.length != (len + 15)/16)
                throw new IllegalArgumentException("invalid taint");

        this.len = len;
        verifyTaint();
    }

    public Taint setLength(int newLength) throws IllegalArgumentException {
        if (newLength < 0) throw new IllegalArgumentException("negative len");
       
        if (newLength < len) 
           b.clear(newLength, len);
        len = newLength;
        verifyTaint();

        return this;
    }

    public void set(int index) throws IllegalArgumentException {
        if (index >= len || index < 0) 
            throw new IllegalArgumentException("invalid index");

        b.set(index);
    }

    public void set(int fromIndex, int toIndex) 
        throws IllegalArgumentException 
    {
        if (fromIndex > toIndex || toIndex >= len || (fromIndex|toIndex) < 0) 
            throw new IllegalArgumentException("invalid index");

        b.set(fromIndex, toIndex);
    }

    public void clear(int index) throws IllegalArgumentException {
        if (index >= len || index < 0) 
            throw new IllegalArgumentException("invalid index");

        b.clear(index);
    }
    
    public void clear(int fromIndex, int toIndex) 
        throws IllegalArgumentException 
    {
        if (fromIndex > toIndex || toIndex >= len || (fromIndex|toIndex) < 0) 
            throw new IllegalArgumentException("invalid index");

        b.clear(fromIndex, toIndex);
    }

    public boolean get(int index) throws IllegalArgumentException {
        if (index >= len || index < 0) 
            throw new IllegalArgumentException("invalid index");
        return b.get(index);
    }

    public Taint subset(int beginIndex, int endIndex) 
        throws IllegalArgumentException 
    {
       int newLen = endIndex - beginIndex;
       if (beginIndex > endIndex || endIndex > len || 
               (beginIndex|endIndex) < 0) 
           throw new IllegalArgumentException("invalid index");

       b.clear(0, beginIndex);
       for (int i = b.nextSetBit(beginIndex); i >= 0 && i < endIndex;
                i = b.nextSetBit(i+1)) 
       {
           b.clear(i);
           b.set(i - beginIndex);
       }
       b.clear(endIndex, len);

       len = newLen;
       verifyTaint();

       return this;
    }

    public Taint delete(int beginIndex, int endIndex)
        throws IllegalArgumentException
    {
        int remlen;

        if (beginIndex < 0 || beginIndex > endIndex || beginIndex > len)
            throw new IllegalArgumentException("invalid index");

        if (endIndex > len) endIndex = len; 
        remlen = endIndex - beginIndex;

        b.clear(beginIndex, endIndex);
        for (int i = b.nextSetBit(endIndex); i >= 0; i = b.nextSetBit(i+1)) {
            b.clear(i);
            b.set(i-remlen);
        }

        len -= remlen;
        verifyTaint();
        return this;
    }

    public Taint append(Taint t) {
        BitSet tb = t.asBitSet();
        for (int i = tb.nextSetBit(0); i >= 0; i = tb.nextSetBit(i+1)) 
            b.set(len + i);
        len += t.len;
        verifyTaint();
        return this;
    }

    public Taint insert(int offset, Taint t) throws IllegalArgumentException
    {
        /* Explicitly allow offset == len, which makes this an append */
        if (offset > len || offset < 0)
            throw new IllegalArgumentException("invalid offset");

        int olen = t.len;
        for (int i = len - 1; i >= offset; i--) {
            if (b.get(i)) {
                b.clear(i);
                b.set(i + olen);
            }
        }

        BitSet tb = t.asBitSet();
        for (int i = tb.nextSetBit(0); i >= 0; i = tb.nextSetBit(i+1))
            b.set(offset + i);

        len += olen;
        verifyTaint();
        return this;
    }


    public Taint insertUntainted(int offset, int olen) 
        throws IllegalArgumentException
    {
        if (offset > len || offset < 0)
            throw new IllegalArgumentException("invalid offset");

        for (int i = len - 1; i >= offset; i--) {
            if (b.get(i)) {
                b.clear(i);
                b.set(i + olen);
            }
        }

        len += olen;
        verifyTaint();
        return this;
    }

    public Taint reverse() {
        for (int i =0, j = len-1; i < j; i++, j--) {
            boolean t = b.get(i);
            b.set(i,b.get(j));
            b.set(j,t);
        }
        verifyTaint();
        return this;
    }

    public void getTaintAsChars(char[] dst, int dstBegin) {
        for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i+1))
            dst[dstBegin + i/16] |= (char) (1 << (i & 15));
    }

    public boolean isTainted() { return b.cardinality() != 0; }

    public int cardinality() { return b.cardinality(); }

    public int length() { return len; }

    public BitSet asBitSet() { return b; }

    public String toString() { return "[tainted: " + isTainted() + "]"
                               + "[length: " + len + "] " +  b.toString(); }
}
