/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package javax.servlet;

import jtaint.SafeRandom;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Locale;

/**
 * Defines an object to assist a servlet in sending a response to the client.
 * The servlet container creates a <code>ServletResponse</code> object and
 * passes it as an argument to the servlet's <code>service</code> method.
 *
 * <p>To send binary data in a MIME body response, use
 * the {@link ServletOutputStream} returned by {@link #getOutputStream}.
 * To send character data, use the <code>PrintWriter</code> object 
 * returned by {@link #getWriter}. To mix binary and text data,
 * for example, to create a multipart response, use a
 * <code>ServletOutputStream</code> and manage the character sections
 * manually.
 *
 * <p>The charset for the MIME body response can be specified
 * explicitly using the {@link #setCharacterEncoding} and
 * {@link #setContentType} methods, or implicitly
 * using the {@link #setLocale} method.
 * Explicit specifications take precedence over
 * implicit specifications. If no charset is specified, ISO-8859-1 will be
 * used. The <code>setCharacterEncoding</code>,
 * <code>setContentType</code>, or <code>setLocale</code> method must
 * be called before <code>getWriter</code> and before committing
 * the response for the character encoding to be used.
 * 
 * <p>See the Internet RFCs such as 
 * <a href="http://www.ietf.org/rfc/rfc2045.txt">
 * RFC 2045</a> for more information on MIME. Protocols such as SMTP
 * and HTTP define profiles of MIME, and those standards
 * are still evolving.
 *
 * @author 	Various
 * @version 	$Version$
 *
 * @see		ServletOutputStream
 *
 */

public class ServletResponse { 


    private final SafeRandom sr;
    private String encoding, type;
    private StringWriter sw;

    public ServletResponse(SafeRandom sr) { 
        this.sr = sr;
    }

    public ServletResponse() {
        sr = null;
    }

    public void setWriter(StringWriter sw) {
        this.sw = sw;
    }

    public PrintWriter getWriter() { 
        if (sr.nextBoolean())
            return new SimpleServletWriter(sw);
        else 
            return new ServletWriter(sw);
    }

    public ServletOutputStream getOutputStream() { 
        if (sr.nextBoolean())
            return new ServletOutputStream(sw, encoding); 
        else
            return new SubServletOutputStream(sw, encoding);
    }

    public String getCharacterEncoding() { return encoding; }

    public void setCharacterEncoding(String e) { encoding = e; }

    public String getContentType() { return type; }

    public void setContentType(String t) { type = t; }
}
