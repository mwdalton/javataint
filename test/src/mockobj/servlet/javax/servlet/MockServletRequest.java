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

import jtaint.TestUtil;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

public class MockServletRequest implements ServletRequest
{
    private TestUtil tu;

    private String encode, content, param;
    private Hashtable paramNames;
    private String[] paramValues;
    private Map paramMap;
    private String addr, host;

    public MockServletRequest(TestUtil tu) {
        this.tu = tu;
        encode = tu.randString();
        content = tu.randString();
        param = tu.randString();
        paramNames = tu.randHashtable();
        paramValues = tu.randStringArray();
        paramMap = tu.randMap();
        addr = tu.randString();
        host = tu.randString();
    }

    public void randomize() {
        encode = tu.randString();
        content = tu.randString();
        param = tu.randString();
        paramNames = tu.randHashtable();
        paramValues = tu.randStringArray();
        paramMap = tu.randMap();
        addr = tu.randString();
        host = tu.randString();
    }

    public String getCharacterEncoding() { return encode; }
    public String getCharacterEncodingSafe() { return encode; }

    public String getContentType() { return content; }
    public String getContentTypeSafe() { return content; }
    
    public String getParameter(String name) { return param; }
    public String getParameterSafe(String name) { return param; }
    
    public Enumeration getParameterNames() { return paramNames.keys(); }
    public Enumeration getParameterNamesSafe() { return paramNames.keys(); }
    
    public String[] getParameterValues(String name) { return paramValues; }
    public String[] getParameterValuesSafe(String name) { return paramValues; }
 
    public Map getParameterMap() { return paramMap; }
    public Map getParameterMapSafe() { return paramMap; }
    
    public String getRemoteAddr() { return addr; }
    public String getRemoteAddrSafe() { return addr; }
    
    public String getRemoteHost() { return host; }
    public String getRemoteHostSafe() { return host; }
}
