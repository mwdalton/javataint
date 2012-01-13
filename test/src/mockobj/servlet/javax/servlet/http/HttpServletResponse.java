/* Copyright 2009 Michael Dalton */

package javax.servlet.http;

import jtaint.SafeRandom;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletResponse;
import javax.servlet.ServletWriter;

public class HttpServletResponse extends ServletResponse { 
    private SafeRandom sr;
    private StringWriter sw;

    public HttpServletResponse(SafeRandom sr) {
        super(sr);
        this.sr = sr;
    }

    public HttpServletResponse() { super(); }

    public void setWriter(StringWriter sw) {
        super.setWriter(sw);
        this.sw = sw;
    }

    public PrintWriter getWriter() {
        return new ServletWriter(sw);
    }
}
