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
