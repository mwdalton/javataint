/* Copyright 2009 Michael Dalton */

package javax.servlet;

import com.siegebrk.HtmlValidator;
import com.siegebrk.Log;

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
