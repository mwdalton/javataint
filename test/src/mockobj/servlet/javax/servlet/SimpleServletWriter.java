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
