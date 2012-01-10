/* Copyright 2009 Michael Dalton */

package javax.servlet;

import com.siegebrk.Log;

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
