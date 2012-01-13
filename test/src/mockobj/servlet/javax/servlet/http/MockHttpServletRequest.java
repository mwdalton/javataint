/* Copyright 2009 Michael Dalton */
package javax.servlet.http;

import jtaint.TestUtil;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.MockServletRequest;

public class MockHttpServletRequest extends MockServletRequest
        implements HttpServletRequest
{
    private final TestUtil tu;

    private String ctxPath, header, pathInfo, query, user, sessionId, URI,
                   servletPath;
    private Hashtable headers, headerNames;
    private StringBuffer URL;


    public MockHttpServletRequest(TestUtil tu) {
        super(tu);
        this.tu = tu;
        ctxPath = tu.randString();
        header = tu.randString();
        pathInfo = tu.randString();
        query = tu.randString();
        user = tu.randString();
        sessionId = tu.randString();
        URI = tu.randString();
        servletPath = tu.randString();

        headers = tu.randHashtable();
        headerNames = tu.randHashtable();

        URL = new StringBuffer(tu.randString());
    }

    public void randomize() {
        ctxPath = tu.randString();
        header = tu.randString();
        pathInfo = tu.randString();
        query = tu.randString();
        user = tu.randString();
        sessionId = tu.randString();
        URI = tu.randString();
        servletPath = tu.randString();

        headers = tu.randHashtable();
        headerNames = tu.randHashtable();

        URL = new StringBuffer(tu.randString());
    }

    public String getContextPath() { return ctxPath; } 
    public String getContextPathSafe() { return ctxPath; } 

    public String getHeader(String name) { return header + name; } 
    public String getHeaderSafe(String name) { return header + name; } 

    public Enumeration getHeaders(String name) { return headers.keys(); }
    public Enumeration getHeadersSafe(String name) { return headers.keys(); }
    
    public Enumeration getHeaderNames() { return headerNames.keys(); }
    public Enumeration getHeaderNamesSafe() { return headerNames.keys(); }
    
    public String getPathInfo() { return pathInfo; }
    public String getPathInfoSafe() { return pathInfo; }
  
    /* XXX Bytecode instrumentation should rewrite getPathTranslated
     * as getRealPath(getPathInfo())
     */ 
    public String getPathTranslated() { return "/incorrect"; }
    public String getRealPath(String s) { return "/correct/" + s; }
    
    public String getQueryString() { return query; }
    public String getQueryStringSafe() { return query; }
    
    public String getRemoteUser() { return user; }
    public String getRemoteUserSafe() { return user; }
    
    public String getRequestedSessionId() { return sessionId; }
    public String getRequestedSessionIdSafe() { return sessionId; }
    
    public String getRequestURI() { return URI; }
    public String getRequestURISafe() { return URI; }
    
    public StringBuffer getRequestURL() { return URL; }
    public StringBuffer getRequestURLSafe() { return URL; }
    
    public String getServletPath() { return servletPath; }
    public String getServletPathSafe() { return servletPath; }
}
