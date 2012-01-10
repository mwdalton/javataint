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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;



/**
 * Defines an object to provide client request information to a servlet.  The
 * servlet container creates a <code>ServletRequest</code> object and passes
 * it as an argument to the servlet's <code>service</code> method.
 *
 * <p>A <code>ServletRequest</code> object provides data including
 * parameter name and values, attributes, and an input stream.
 * Interfaces that extend <code>ServletRequest</code> can provide
 * additional protocol-specific data (for example, HTTP data is
 * provided by {@link javax.servlet.http.HttpServletRequest}.
 * 
 * @author 	Various
 * @version 	$Version$
 *
 * @see 	javax.servlet.http.HttpServletRequest
 *
 */

public interface ServletRequest {

    public String getCharacterEncoding();
    public String getCharacterEncodingSafe();

    public String getContentType();
    public String getContentTypeSafe();
    
    public String getParameter(String name);
    public String getParameterSafe(String name);
    
    public Enumeration getParameterNames();
    public Enumeration getParameterNamesSafe();
    
    public String[] getParameterValues(String name);
    public String[] getParameterValuesSafe(String name);
 
    public Map getParameterMap();
    public Map getParameterMapSafe();
    
    public String getRemoteAddr();
    public String getRemoteAddrSafe();

    public String getRemoteHost();
    public String getRemoteHostSafe();
}

