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

package javax.servlet.http;

import javax.servlet.ServletRequest;
import java.util.Enumeration;

/**
 *
 * Extends the {@link javax.servlet.ServletRequest} interface
 * to provide request information for HTTP servlets. 
 *
 * <p>The servlet container creates an <code>HttpServletRequest</code> 
 * object and passes it as an argument to the servlet's service
 * methods (<code>doGet</code>, <code>doPost</code>, etc).
 *
 *
 * @author 	Various
 * @version	$Version$
 *
 *
 */

public interface HttpServletRequest extends ServletRequest {

    public String getContextPath();
    public String getContextPathSafe();

    public String getHeader(String name); 
    public String getHeaderSafe(String name); 

    public Enumeration getHeaders(String name); 
    public Enumeration getHeadersSafe(String name); 
    
    public Enumeration getHeaderNames();
    public Enumeration getHeaderNamesSafe();
    
    public String getPathInfo();
    public String getPathInfoSafe();
    
    public String getPathTranslated();
    
    public String getQueryString();
    public String getQueryStringSafe();
    
    public String getRemoteUser();
    public String getRemoteUserSafe();
    
    public String getRequestedSessionId();
    public String getRequestedSessionIdSafe();
    
    public String getRequestURI();
    public String getRequestURISafe();
    
    public StringBuffer getRequestURL();
    public StringBuffer getRequestURLSafe();
    
    public String getServletPath();
    public String getServletPathSafe();
}
