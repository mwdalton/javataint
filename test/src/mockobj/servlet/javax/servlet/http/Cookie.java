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

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * Creates a cookie, a small amount of information sent by a servlet to 
 * a Web browser, saved by the browser, and later sent back to the server.
 * A cookie's value can uniquely 
 * identify a client, so cookies are commonly used for session management.
 * 
 * <p>A cookie has a name, a single value, and optional attributes
 * such as a comment, path and domain qualifiers, a maximum age, and a
 * version number. Some Web browsers have bugs in how they handle the 
 * optional attributes, so use them sparingly to improve the interoperability 
 * of your servlets.
 *
 * <p>The servlet sends cookies to the browser by using the
 * {@link HttpServletResponse#addCookie} method, which adds
 * fields to HTTP response headers to send cookies to the 
 * browser, one at a time. The browser is expected to 
 * support 20 cookies for each Web server, 300 cookies total, and
 * may limit cookie size to 4 KB each.
 * 
 * <p>The browser returns cookies to the servlet by adding 
 * fields to HTTP request headers. Cookies can be retrieved
 * from a request by using the {@link HttpServletRequest#getCookies} method.
 * Several cookies might have the same name but different path attributes.
 * 
 * <p>Cookies affect the caching of the Web pages that use them. 
 * HTTP 1.0 does not cache pages that use cookies created with
 * this class. This class does not support the cache control
 * defined with HTTP 1.1.
 *
 * <p>This class supports both the Version 0 (by Netscape) and Version 1 
 * (by RFC 2109) cookie specifications. By default, cookies are
 * created using Version 0 to ensure the best interoperability.
 *
 *
 * @author	Various
 * @version	$Version$
 *
 */

// XXX would implement java.io.Serializable too, but can't do that
// so long as sun.servlet.* must run on older JDK 1.02 JVMs which
// don't include that support.

import com.siegebrk.TestUtil;

public class Cookie {
    private String comment, domain, name, path, value;
    private TestUtil tu;

    public Cookie(TestUtil tu) {
        this.tu = tu;
        randomize();
    }

    public void randomize() {
        comment = tu.randString();
        domain  = tu.randString();
        name = tu.randString();
        path = tu.randString();
        value = tu.randString();
    }

    public String getComment() {
	return comment;
    }

    public String getCommentSafe() {
	return comment;
    }

    public String getDomain() {
	return domain;
    }

    public String getDomainSafe() {
	return domain;
    }

    public String getName() {
	return name;
    }

    public String getNameSafe() {
	return name;
    }

    public String getPath() {
	return path;
    }

    public String getPathSafe() {
	return path;
    }

    public String getValue() {
	return value;
    }

    public String getValueSafe() {
	return value;
    }
}
