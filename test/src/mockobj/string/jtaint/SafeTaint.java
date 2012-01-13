/* Copyright 2009 Michael Dalton */
package jtaint;

import jtaint.Taint;
import java.util.BitSet;

public class SafeTaint implements Cloneable
{
    boolean[] b;


    public Object clone() {
        SafeTaint st;
        try {
            st = (SafeTaint) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("JDK Clone Failure");
        }
        st.b = (boolean[]) b.clone();
        return st;
    }

    public SafeTaint() {
        b = new boolean[0];
    }
    
    public SafeTaint(BitSet bs, int len) {
        b = new boolean[len];

        if (bs == null) return;

        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) 
            b[i] = true;
    }

    public SafeTaint(boolean fill, int len) {
        b = new boolean[len];

        for (int i = 0; i < len; i++)
            b[i] = fill;
    }

    public SafeTaint(boolean[] b)
    {
        this.b = (boolean[]) b.clone();
    }

    public SafeTaint(char[] t, int len)
    {
        this.b = new boolean[len];

        for (int i = 0; i < t.length; i++)
            for (int j = 0; j < 16; j++)
                if ((t[i] & (1 << j)) != 0)
                    b[16 * i + j] = true;
    }

    public SafeTaint setLength(int newLength) throws IllegalArgumentException 
    {
        boolean[] newb;

        if (newLength < 0)
            throw new IllegalArgumentException("invalid index");
        newb = new boolean[newLength];

        for (int i = 0; i < b.length && i < newb.length; i++)
            newb[i] = b[i];
        b = newb;
        return this;
    }


    public SafeTaint set(int index) throws IllegalArgumentException {
        if (index < 0 || index >= b.length)
            throw new IllegalArgumentException("invalid index");
        b[index] = true;
        return this;
    }

    public SafeTaint set(int fromIndex, int toIndex) 
        throws IllegalArgumentException 
    {
       if (fromIndex > toIndex || toIndex >= b.length || 
               (fromIndex|toIndex) < 0)
          throw new IllegalArgumentException("invalid index");
      for (int i = fromIndex; i < toIndex; i++)
         b[i] = true;
      return this;
    } 

    public SafeTaint clear(int index) throws IllegalArgumentException {
        if (index < 0 || index >= b.length)
            throw new IllegalArgumentException("invalid index");
        b[index] = false;
        return this;
    }

    public SafeTaint clear(int fromIndex, int toIndex) 
        throws IllegalArgumentException 
    {
       if (fromIndex > toIndex || toIndex >= b.length || 
               (fromIndex|toIndex) < 0)
          throw new IllegalArgumentException("invalid index");
      for (int i = fromIndex; i < toIndex; i++)
         b[i] = false;
      return this;
    }

    public boolean get(int index) throws IllegalArgumentException {
        if (index < 0 || index >= b.length)
            throw new IllegalArgumentException("invalid index");
        return b[index];
    }

    public SafeTaint subset(int beginIndex, int endIndex) 
        throws IllegalArgumentException 
    {
        boolean[] newb;

       if (beginIndex > endIndex || endIndex > b.length || 
               (beginIndex|endIndex) < 0)
          throw new IllegalArgumentException("invalid index");

       newb = new boolean[endIndex - beginIndex];
       for (int i = beginIndex; i < endIndex; i++)
           newb[i - beginIndex] = b[i];
       b = newb;
       return this;
    }
       

    public SafeTaint delete(int beginIndex, int endIndex)
        throws IllegalArgumentException
    {
       boolean[] newb;
       int remlen;

        if (beginIndex < 0 || beginIndex > endIndex || beginIndex > b.length)
            throw new IllegalArgumentException("invalid index");

        if (endIndex > b.length)
            endIndex = b.length;

       remlen = endIndex - beginIndex;
       newb = new boolean[b.length - remlen];

       for (int i = 0; i < beginIndex; i++)
           newb[i] = b[i];
       for (int i = endIndex; i < b.length; i++)
           newb[i - remlen]  = b[i];
       b = newb;
       return this;
    }

    public SafeTaint append(SafeTaint st) {
        boolean[] newb = new boolean[b.length + st.b.length];

        for (int i = 0; i < b.length; i++)
            newb[i] = b[i];
        for (int i = 0; i < st.b.length; i++)
            newb[i + b.length] = st.b[i];
        b = newb;
        return this;
    }

    public SafeTaint insert(int offset, SafeTaint st) 
        throws IllegalArgumentException
    {
        boolean[] newb = new boolean[b.length + st.b.length];

        if (offset < 0 || offset > b.length)
            throw new IllegalArgumentException("invalid offset");

        for (int i = 0; i < offset; i++)
            newb[i] = b[i];
        for (int i = 0; i < st.b.length; i++)
            newb[offset + i] = st.b[i];
        for (int i = offset; i < b.length; i++)
            newb[i + st.b.length] = b[i];
        b = newb;
        return this;
    }


    public SafeTaint reverse() {
        boolean[] newb = new boolean[b.length];

        for (int i = 0, j = b.length-1; i < b.length; i++, j--)
            newb[j] = b[i];
        b = newb;
        return this;
    }

    public boolean isTainted() { 
       boolean tainted = false;

       for (int i = 0; i < b.length; i++)
           if (b[i]) tainted = true;
       return tainted;
    }

    public int cardinality() 
    { 
        int c = 0;

        for (int i = 0; i < b.length; i++)
            if (b[i])
                c++;
        return c;
    }

    public BitSet asBitSet() {
        BitSet bs = new BitSet();

        for (int i = 0; i < b.length; i++)
                bs.set(i, b[i]);
        return bs;
    }

    public int length() { return b.length; }

    public void getTaintAsChars(char[] dst, int dstBegin) {
        for (int i = 0; i < b.length; i++)
            if (b[i])
                dst[dstBegin + i/16] |= (1 << (i & 15));
    }

    private String printTaint() {
        String s = "{ ";

        for (int i = 0; i < b.length; i++) {
            if (b[i])
                s += i + " ";
        }
        s += "}";
        return s;
    }

    public String toString() { return "[tainted: " + isTainted() +" ]" 
        + "[length: " + b.length + "] " +  printTaint(); }

    public boolean verify(Taint t) throws IllegalArgumentException {
        boolean caught = false;

        if (t == null) 
            return !isTainted();

        if (t.length() != b.length) {
            System.out.println("t length "  + t.length() 
                               + " st length " + b.length);
            return false;
        }

        for (int i = 0; i < b.length; i++) {
            if (b[i] != t.get(i)) {
                System.out.println("t[ " + i +"] = " + t.get(i) + " st[ " + i 
                        + "] = " + b[i]);
                return false;
            }
        }

        try {
            t.get(b.length);
        } catch (IllegalArgumentException e) {
            return true;
        }

        System.out.println("Failed to catch exception at offset " + b.length);
        return false;
    }
}
