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

import javax.servlet.ServletInputStream;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.io.IOException;

/**
 * @deprecated		As of Java(tm) Servlet API 2.3. 
 *			These methods were only useful
 *			with the default encoding and have been moved
 *			to the request interfaces.
 *
*/

import jtaint.TestUtil;

public class HttpUtils {
    private static StringBuffer url;
    private static Hashtable post, query;

    public static void init(TestUtil tu) {
        url = new StringBuffer(tu.randString());
        post = tu.randHashtable();
        query = tu.randHashtable();
    }
    
    public static StringBuffer getRequestURL (HttpServletRequest req) {
        return new StringBuffer(url + req.getRemoteHost());
    }

    public static StringBuffer getRequestURLSafe (HttpServletRequest req) {
        return new StringBuffer(url + req.getRemoteHostSafe());
    }

    static public Hashtable parsePostData(int len, 
					  ServletInputStream in)
    {
        return post;
    }

    static public Hashtable parsePostDataSafe(int len, 
					  ServletInputStream in)
    {
        return post;
    }

    static public Hashtable parseQueryString(String s) {
        return query;
    }

    static public Hashtable parseQueryStringSafe(String s) {
        return query;
    }
}



