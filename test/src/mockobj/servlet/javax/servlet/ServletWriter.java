/* Copyright 2009 Michael Dalton */

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
