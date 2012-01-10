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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 *
 * Provides an abstract class to be subclassed to create
 * an HTTP servlet suitable for a Web site. A subclass of
 * <code>HttpServlet</code> must override at least 
 * one method, usually one of these:
 *
 * <ul>
 * <li> <code>doGet</code>, if the servlet supports HTTP GET requests
 * <li> <code>doPost</code>, for HTTP POST requests
 * <li> <code>doPut</code>, for HTTP PUT requests
 * <li> <code>doDelete</code>, for HTTP DELETE requests
 * <li> <code>init</code> and <code>destroy</code>, 
 * to manage resources that are held for the life of the servlet
 * <li> <code>getServletInfo</code>, which the servlet uses to
 * provide information about itself 
 * </ul>
 *
 * <p>There's almost no reason to override the <code>service</code>
 * method. <code>service</code> handles standard HTTP
 * requests by dispatching them to the handler methods
 * for each HTTP request type (the <code>do</code><i>XXX</i>
 * methods listed above).
 *
 * <p>Likewise, there's almost no reason to override the 
 * <code>doOptions</code> and <code>doTrace</code> methods.
 * 
 * <p>Servlets typically run on multithreaded servers,
 * so be aware that a servlet must handle concurrent
 * requests and be careful to synchronize access to shared resources.
 * Shared resources include in-memory data such as
 * instance or class variables and external objects
 * such as files, database connections, and network 
 * connections.
 * See the
 * <a href="http://java.sun.com/Series/Tutorial/java/threads/multithreaded.html">
 * Java Tutorial on Multithreaded Programming</a> for more
 * information on handling multiple threads in a Java program.
 *
 * @author	Various
 * @version	$Version$
 *
 */

import javax.servlet.Servlet;

public abstract class HttpServlet implements Servlet
{
    public void service(HttpServletRequest req, HttpServletResponse res)
    {
        process(req, res);
    }

    protected abstract void process(HttpServletRequest req, 
                                    HttpServletResponse res);
    
    public void service(ServletRequest req, ServletResponse res)
    {
	HttpServletRequest	request;
	HttpServletResponse	response;
	
	try {
	    request = (HttpServletRequest) req;
	    response = (HttpServletResponse) res;
	} catch (ClassCastException e) {
	    throw new RuntimeException("non-HTTP request or response");
	}
	service(request, response);
    }

    public String getServletInfo() { return "MyHttpServlet"; }
}
