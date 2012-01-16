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

package javax.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Locale;

import jtaint.HtmlValidator;
import jtaint.Log;

public class ServletWriter extends SimpleServletWriter
{
    public ServletWriter(StringWriter sw) { super(sw); }

    //[ifJava5+]
    public ServletWriter append(CharSequence csq, int start, int end) 
    {
        print(csq.toString().substring(start, end));
        return this;
    }

    public ServletWriter format(String format, Object... args) 
    {
        super.format(format, args);
        return this;
    }

    public ServletWriter printf(Locale l, String format, Object... args) 
    {
        sw.write(String.format(l, format, args));
        return this;
    }
    //[fiJava5+]
   
    public void print(float f) {
        print(String.valueOf(f));
    }

    public void print(char[] c) {
        try {
            sw.write(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void print(String s) {
        sw.write(s);
    }

    public void println(long l) {
        sw.write(String.valueOf(l));
        sw.write(HtmlValidator.LINE_SEP);
    }

    public void write(char[] buf) {
        super.write(buf);
    }
    
    public void write(int c) {
        sw.write((char)c);
    }

    public void write(String s, int off, int len) {
        sw.write(s.substring(off, off + len));
    }
}
