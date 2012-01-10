/* Copyright 2009 Michael Dalton */

package javax.servlet;

import com.siegebrk.HtmlValidator;
import com.siegebrk.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class SimpleServletWriter extends PrintWriter
{
    StringWriter sw;

    public SimpleServletWriter(StringWriter sw) {
        super(sw);
        this.sw = sw;
    }

    public StringWriter getStringWriter() { return sw; }

    public void println() { 
        sw.write(HtmlValidator.LINE_SEP); 
    }
}
