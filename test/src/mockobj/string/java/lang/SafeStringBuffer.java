/* Copyright 2009 Michael Dalton */
package java.lang;

import java.util.BitSet;
import jtaint.SafeTaint;
import jtaint.Taint;

public final class SafeStringBuffer implements SafeCharSequence
{
    private OrigStringBuffer buffer;
    private SafeTaint taint;

    public SafeStringBuffer() {
        buffer = new OrigStringBuffer();
        taint = new SafeTaint();
    }

    public SafeStringBuffer(int capacity) {
        buffer = new OrigStringBuffer(capacity);
        taint = new SafeTaint();
    }

    public SafeStringBuffer(SafeString str) {
        buffer = new OrigStringBuffer(str.origString());
        taint = str.taint();
    }

    //[ifJava5+]
    public SafeStringBuffer(SafeCharSequence seq) {
        this(seq.toSafeString());
    }
    //[fiJava5+]

    public synchronized int length() {
        return buffer.length();
    }

    public synchronized int capacity() {
        return buffer.capacity();
    }

    public synchronized void ensureCapacity(int minimumCapacity) {
        buffer.ensureCapacity(minimumCapacity);
    }

    //[ifJava5+]
    public synchronized void trimToSize() {
        buffer.trimToSize();
    }
    //[fiJava5+]

    public synchronized void setLength(int newLength) {
        buffer.setLength(newLength);
        taint.setLength(newLength);
    }

    public synchronized char charAt(int index) {
        return buffer.charAt(index);
    }

    //[ifJava5+]
    public synchronized int codePointAt(int index) {
        return buffer.codePointAt(index);
    }

    public synchronized int codePointBefore(int index) {
        return buffer.codePointBefore(index);
    }

    public synchronized int codePointCount(int beginIndex, int endIndex) {
        return buffer.codePointCount(beginIndex, endIndex);
    }
    
    public synchronized int offsetByCodePoints(int index, int codePointOffset) {
        return buffer.offsetByCodePoints(index, codePointOffset);
    }
    //[fiJava5+]

    public synchronized void getChars(int srcBegin, int srcEnd, char dst[],
                                      int dstBegin) {
        buffer.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    public synchronized void setCharAt(int index, char ch) {
        buffer.setCharAt(index, ch);
        taint.clear(index);
    }

    public synchronized SafeStringBuffer append(Object obj) {
        return append(SafeString.valueOf(obj));
    }

    public synchronized SafeStringBuffer append(SafeString str) {
        buffer.append(str.origString());
        taint.append(str.taint());
        return this;
    }

    public synchronized SafeStringBuffer append(SafeStringBuffer sb) {
        return append(sb.toSafeString());
    }

    //[ifJava5+]
    public SafeStringBuffer append(SafeCharSequence s) {
        return append(s.toSafeString());
    }

    public synchronized SafeStringBuffer append(SafeCharSequence s, int start, int end)
    {
        return append(s.toSafeString().substring(start, end));
    }
    //[fiJava5+]


    public synchronized SafeStringBuffer append(char str[]) {
         return append(new SafeString(str));
    }

    public synchronized SafeStringBuffer append(char str[], int offset, int len){
        return append(new SafeString(str,offset,len));
    }

    public synchronized SafeStringBuffer append(boolean b) {
        return append(SafeString.valueOf(b));
    }

    public synchronized SafeStringBuffer append(char c) {
        return append(SafeString.valueOf(c));
    }

    public synchronized SafeStringBuffer append(int i) {
        return append(SafeString.valueOf(i));
    }

    //[ifJava5+]
    public synchronized SafeStringBuffer appendCodePoint(int codePoint) {
        buffer.appendCodePoint(codePoint);
        taint.setLength(buffer.length());
        return this;
    }
    //[fiJava5+]
    
    public synchronized SafeStringBuffer append(long lng) {
        return append(SafeString.valueOf(lng));
    }
    public synchronized SafeStringBuffer append(float f) {
        return append(SafeString.valueOf(f));
    }
    public synchronized SafeStringBuffer append(double d) {
        return append(SafeString.valueOf(d));
    }

    public synchronized SafeStringBuffer delete(int start, int end) {
        buffer.delete(start, end);
        taint.delete(start, end);
        return this;
    }

    public synchronized SafeStringBuffer deleteCharAt(int index) {
        if (index == buffer.length()) 
            throw new IndexOutOfBoundsException("invalid index");
        buffer.delete(index, index+1);
        taint.delete(index, index+1);
        return this;
    }
        
    public synchronized SafeStringBuffer replace(int start, int end, SafeString str) {
        buffer.replace(start, end, str.origString());
        taint.delete(start, end);
        taint.insert(start, str.taint());
        return this;
    }


    public synchronized SafeString substring(int start) {
        return substring(start, buffer.length());
    }

    public synchronized CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public synchronized SafeString substring(int start, int end) {
        return toSafeString().substring(start, end); 
    }

    public synchronized SafeStringBuffer insert(int index, char str[], 
            int offset, int len) {
        return insert(index, new SafeString(str, offset, len));
    }

    public synchronized SafeStringBuffer insert(int offset, Object obj) {
        return insert(offset, SafeString.valueOf(obj));
    }

    public synchronized SafeStringBuffer insert(int offset, SafeString str) {
        buffer.insert(offset, str.origString());
        taint.insert(offset, str.taint());
        return this;
    }

    public synchronized SafeStringBuffer insert(int offset, char str[]) {
        return insert(offset, new SafeString(str));
    }

    //[ifJava5+]
    public SafeStringBuffer insert(int dstOffset, SafeCharSequence s) {
        return insert(dstOffset, s.toSafeString());
    }

    public synchronized SafeStringBuffer insert(int dstOffset, SafeCharSequence s, int start, int end)
    {
        return insert(dstOffset, s.toSafeString().substring(start, end));
    }
    //[fiJava5+]
    
    public SafeStringBuffer insert(int offset, boolean b) {
        return insert(offset, SafeString.valueOf(b));
    }

    public synchronized SafeStringBuffer insert(int offset, char c) {
        return insert(offset, SafeString.valueOf(c));
    }

    public SafeStringBuffer insert(int offset, int i) {
        return insert(offset, SafeString.valueOf(i));
    }

    public SafeStringBuffer insert(int offset, long l) {
        return insert(offset, SafeString.valueOf(l));
    }

    public SafeStringBuffer insert(int offset, float f) {
        return insert(offset, SafeString.valueOf(f));
    }

    public SafeStringBuffer insert(int offset, double d) {
        return insert(offset, SafeString.valueOf(d));
    }

    public int indexOf(SafeString str) {
        return buffer.indexOf(str.origString());
    }

    public synchronized int indexOf(SafeString str, int fromIndex) {
        return buffer.indexOf(str.origString(), fromIndex);
    }

    public int lastIndexOf(SafeString str) {
        return buffer.lastIndexOf(str.origString());
    }

    public synchronized int lastIndexOf(SafeString str, int fromIndex) {
        return buffer.lastIndexOf(str.origString(), fromIndex);
    }

    public synchronized SafeStringBuffer reverse() {
        buffer.reverse();
        taint.reverse();
        return this;
    }

    public synchronized String toString() {
        String s = buffer.toString();
        if (!taint.isTainted())
            return s;
        else
            return new String(s, new Taint(taint.asBitSet(), s.length()));
    }

    public synchronized SafeString toSafeString() {
        return new SafeString(buffer.toOrigString(), (SafeTaint) taint.clone());
    }

    public synchronized boolean verify(StringBuffer sb) 
        throws IllegalArgumentException 
    { 
        Taint t = sb.toString().@internal@taint();
        if (!buffer.toString().equals(sb.toString()) || !taint.verify(t))
            return false;
        return true;
    }

    public synchronized SafeTaint taint() { return (SafeTaint) taint.clone(); }
}
