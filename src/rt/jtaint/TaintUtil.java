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

public final class TaintUtil
{
    /* Append t2 to t1 */
    public static Taint append(Taint t1, int t1Len, Taint t2, int t2Len) {
        if (t1 == null && t2 == null)
            return null;
        else if (t1 == null) {
            return t2.insertUntainted(0, t1Len);
        } else if (t2 == null) {
            t1.setLength(t1Len + t2Len);
            return t1;
        }

        return t1.append(t2);
    }

    /* Insert t1 into t2 at offset */
    public static Taint insert(Taint t1, int t1Len, int offset,
                               Taint t2, int t2Len) 
    {
        if (t1 == null && t2 == null)
            return null;
        else if (t1 == null) {
            t2.insertUntainted(0, offset);
            t2.setLength(t1Len + t2Len);
            return t2;
        } else if (t2 == null) {
            return t1.insertUntainted(offset, t2Len); 
        }

        return t1.insert(offset, t2);
    }

    /* Perform a replace operation -- same semantics as StringBuilder.replace */
    public static Taint replace(Taint t1, int t1Len, int begin, int end,
                                Taint t2, int t2Len) 
    {
        if (t1 == null && t2 == null)
            return null;
        else if (t1 == null) {
            t2.insertUntainted(0, begin);
            t2.setLength(t1Len + t2Len - (end - begin));
            return t2;
        } else if (t2 == null) {
            t1.delete(begin, end);
            return t1.insertUntainted(begin, t2Len);
        } 

        t1.delete(begin, end);
        return t1.insert(begin, t2);
    }
}
