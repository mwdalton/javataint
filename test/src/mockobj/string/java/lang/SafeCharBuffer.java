/* Copyright 2009 Michael Dalton */
package java.lang;

import java.nio.CharBuffer;
import java.nio.ByteOrder;

public class SafeCharBuffer implements SafeCharSequence
{
    private CharBuffer buffer;

    public SafeCharBuffer(CharBuffer buffer) {
        this.buffer = buffer;
    }

    public char charAt(int index) { return buffer.charAt(index); }

    public int length() { return buffer.length(); }

    public CharSequence subSequence(int start, int end) {
        return toSafeString();
    }

    public String toString() { return buffer.toString(); }

    public SafeString toSafeString() {
        return new SafeString(buffer.toString());
    }

    public static SafeCharBuffer wrap(char[] c) {
        CharBuffer cb = CharBuffer.wrap(c);
        return new SafeCharBuffer(cb);
    }
}
