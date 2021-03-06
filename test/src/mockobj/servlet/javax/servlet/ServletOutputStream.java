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

import jtaint.HtmlValidator;
import jtaint.Log;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;


public class ServletOutputStream extends OutputStream
{
    StringWriter sw;
    String charset;

    public ServletOutputStream(StringWriter sw, String charset) {
        this.sw = sw;
        this.charset = charset;
    }

    public StringWriter getStringWriter() { return sw; }

    public void print(boolean b) {
        sw.write(String.valueOf(b));
    }

    public void print(char c) {
        print(String.valueOf(c));
    }

    public void print(double d) {
        sw.write(String.valueOf(d));
    }

    public void print(float f) {
        print(String.valueOf(f));
    }

    public void print(int i) {
        print(String.valueOf(i));
    }

    public void print(long l) {
        print(String.valueOf(l));
    }

    public void print(String s) {
        sw.write(s);
    }

    public void println() {
        print(HtmlValidator.LINE_SEP);
    }

    public void println(boolean b) {
        print(b);
        println();
    }

    public void println(char c) {
        sw.write(String.valueOf(c) + HtmlValidator.LINE_SEP);
    }

    public void println(double d) {
        sw.write(String.valueOf(d));
        sw.write(HtmlValidator.LINE_SEP);
    }

    public void println(float f) {
        println(String.valueOf(f));
    }

    public void println(int i) {
        print(i);
        println();
    }

    public void println(long l) {
        sw.write(String.valueOf(l));
        println();
    }

    public void println(String s) {
        print(s);
        println();
    }

    public void write(int b) throws UnsupportedEncodingException {
        String s = new String(new byte[] { (byte) b }, charset);
        sw.write(s);
    }
}
