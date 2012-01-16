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
