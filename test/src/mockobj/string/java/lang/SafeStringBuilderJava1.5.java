/* Copyright 2009 Michael Dalton */
package java.lang;

import java.util.BitSet;
import jtaint.SafeTaint;
import jtaint.Taint;

public final class SafeStringBuilder
    implements SafeCharSequence
{
    private SafeTaint taint;
    private OrigStringBuilder builder;

    public SafeStringBuilder() {
        taint = new SafeTaint();
        builder  = new OrigStringBuilder();
    }

    public SafeStringBuilder(int capacity) {
        taint = new SafeTaint();
        builder = new OrigStringBuilder(capacity);
    }

    public SafeStringBuilder(SafeString str) {
        taint = str.taint();
        builder = new OrigStringBuilder(str.origString());
    }

    public SafeStringBuilder(SafeCharSequence seq) {
        this(seq.toSafeString());
    }

    public SafeStringBuilder append(Object obj) {
        return append(SafeString.valueOf(obj));
    }
    public SafeStringBuilder append(SafeString str) {
        builder.append(str.origString());
        taint.append(str.taint());
        return this;
    }

    public SafeStringBuilder append(SafeStringBuffer sb) {
        return append(sb.toSafeString());
    }

    public SafeStringBuilder append(SafeCharSequence s) {
        return append(s.toSafeString());
    }

    public SafeStringBuilder append(SafeCharSequence s, int start, int end) {
        return append(s.toSafeString().substring(start,end));
    }

    public SafeStringBuilder append(char str[]) {
        return append(new SafeString(str));
    }

    public SafeStringBuilder append(char str[], int offset, int len) {
        return append(new SafeString(str, offset, len));
    }

    public SafeStringBuilder append(boolean b) {
        return append(SafeString.valueOf(b));
    }

    public SafeStringBuilder append(char c) {
        return append(SafeString.valueOf(c));
    }

    public SafeStringBuilder append(int i) {
        return append(SafeString.valueOf(i));
    }

    public SafeStringBuilder append(long lng) {
        return append(SafeString.valueOf(lng));
    }

    public SafeStringBuilder append(float f) {
        return append(SafeString.valueOf(f));
    }

    public SafeStringBuilder append(double d) {
        return append(SafeString.valueOf(d));
    }

    public SafeStringBuilder appendCodePoint(int codePoint) {
        builder.appendCodePoint(codePoint);
        taint.setLength(builder.length());
        return this;
    }

    public SafeString substring(int start) {
        return substring(start, builder.length());
    }

    public SafeString substring(int start, int end) {
        return toSafeString().substring(start, end);
    }

    public SafeCharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public SafeStringBuilder delete(int start, int end) {
        builder.delete(start, end);
        taint.delete(start, end);
        return this;
    }

    public SafeStringBuilder deleteCharAt(int index) {
        builder.deleteCharAt(index);
        taint.delete(index, index+1);
        return this;
    }
    
    public SafeStringBuilder replace(int start, int end, SafeString str) {
        builder.replace(start, end, str.origString());
        taint.delete(start, end);
        taint.insert(start, str.taint());
        return this;
    }

    public SafeStringBuilder insert(int index, char str[], int offset,
                                int len)
    {
        return insert(index, new SafeString(str, offset, len));
    }

    public SafeStringBuilder insert(int offset, Object obj) {
        return insert(offset, SafeString.valueOf(obj));
    }

    public SafeStringBuilder insert(int offset, SafeString str) {
        builder.insert(offset, str.origString());
        taint.insert(offset, str.taint());
        return this;
    }

    public SafeStringBuilder insert(int offset, char str[]) {
        return insert(offset, new SafeString(str));
    }

    public SafeStringBuilder insert(int dstOffset, SafeCharSequence s) {
        return insert(dstOffset, s.toSafeString());
    }

    public SafeStringBuilder insert(int dstOffset, SafeCharSequence s,
                                int start, int end)
    {
        return insert(dstOffset, s.toSafeString().substring(start, end));
    }

    public SafeStringBuilder insert(int offset, boolean b) {
        return insert(offset, SafeString.valueOf(b));
    }

    public SafeStringBuilder insert(int offset, char c) {
        return insert(offset, SafeString.valueOf(c));
    }

    public SafeStringBuilder insert(int offset, int i) {
        return insert(offset, SafeString.valueOf(i));
    }

    public SafeStringBuilder insert(int offset, long l) {
        return insert(offset, SafeString.valueOf(l));
    }

    public SafeStringBuilder insert(int offset, float f) {
        return insert(offset, SafeString.valueOf(f));
    }

    public SafeStringBuilder insert(int offset, double d) {
        return insert(offset, SafeString.valueOf(d));
    }

    public int indexOf(SafeString str) {
        return builder.indexOf(str.origString());
    }

    public int indexOf(SafeString str, int fromIndex) {
        return builder.indexOf(str.origString(), fromIndex);
    }

    public int lastIndexOf(SafeString str) {
        return builder.lastIndexOf(str.origString());
    }

    public int lastIndexOf(SafeString str, int fromIndex) {
        return builder.lastIndexOf(str.origString(), fromIndex);
    }
   
    public SafeStringBuilder reverse() {
        builder.reverse();
        taint.reverse();
        return this;
    }

    public String toString() {
        String s = builder.toString();
        if (!taint.isTainted())
            return s;
        else
            return new String(s, new Taint(taint.asBitSet(), s.length()));
    }

    public SafeString toSafeString() {
        return new SafeString(builder.toOrigString(), (SafeTaint)taint.clone());
    }
    
    
    public boolean verify(StringBuilder sb) throws IllegalArgumentException {
        Taint t = sb.toString().@internal@taint();
        if (!builder.toString().equals(sb.toString()) || !taint.verify(t))
            return false;
        return true;
    }

    public int length() {
        return builder.length();
    }

    public int capacity() {
        return builder.capacity();
    }

    public void ensureCapacity(int minimumCapacity) {
        builder.ensureCapacity(minimumCapacity);
    }

    void expandCapacity(int minimumCapacity) {
        builder.expandCapacity(minimumCapacity);
    }

    public void trimToSize() {
        builder.trimToSize();
    }

    public void setLength(int newLength) {
        builder.setLength(newLength);
        taint.setLength(newLength);
    }

    public char charAt(int index) {
        return builder.charAt(index);
    }

    public int codePointAt(int index) {
        return builder.codePointAt(index);
    }

    public int codePointBefore(int index) {
        return builder.codePointBefore(index);
    }

    public int codePointCount(int beginIndex, int endIndex) {
        return builder.codePointCount(beginIndex, endIndex);
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        return builder.offsetByCodePoints(index, codePointOffset);
    }
    
    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        builder.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    public void setCharAt(int index, char ch) {
        builder.setCharAt(index, ch);
        taint.clear(index);
    }

    char[] getValue() {
        return builder.getValue();
    }

    public SafeTaint taint() { return (SafeTaint) taint.clone(); }
}
