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

import jtaint.Log;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;


public class SubServletOutputStream extends ServletOutputStream
{
    private StringWriter sw;
    private String charset;

    public SubServletOutputStream(StringWriter sw, String charset) {
        super(sw, charset);
        this.sw = sw;
        this.charset = charset;
    }

    public StringWriter getStringWriter() { return sw; }

    public void print(int i) {
        sw.write(String.valueOf(i));
    }

    public void println(int i) {
        print(i);
        println();
    }

    public void write(byte[] b) throws UnsupportedEncodingException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) 
        throws UnsupportedEncodingException 
    {
        String s = new String(b, off, len, charset);
        sw.write(s);
    }
}
