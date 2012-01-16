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
package jtaint;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.servlet.Servlet;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.MockHttpServlet;
import javax.servlet.http.MockHttpServletRequest;
import javax.servlet.http.HttpUtils;

public class ServletTest 
{
    private final int maxlen;
    private final SafeRandom sr;
    private final TestUtil tu;
    private List cookies,
                 requests,
                 servlets;

    public ServletTest(int maxlen, SafeRandom sr) {
        this.maxlen = maxlen;
        this.sr = sr;
        this.tu = new TestUtil(maxlen, sr, TestUtil.UNTAINTED);
        cookies = new ArrayList();
        requests = new ArrayList();
        servlets = new ArrayList();
    }

    private Cookie randCookie() {
        Cookie c;

        if (sr.nextBoolean() && cookies.size() > 0) {
            c = (Cookie) cookies.get(sr.nextInt(cookies.size()));
            if (sr.nextBoolean()) c.randomize();
            return c;
        }
                    
        c = new Cookie(tu);
        cookies.add(c);
        return c;
    }

    private MockHttpServlet randServlet() {
        MockHttpServlet s;

        if (sr.nextBoolean() && servlets.size() > 0) {
            s = (MockHttpServlet) servlets.get(sr.nextInt(servlets.size()));
            return s;
        }
                    
        s = new MockHttpServlet(tu);
        servlets.add(s);
        return s;
    }

    private MockHttpServletRequest randRequest() {
        MockHttpServletRequest req;

        if (sr.nextBoolean() && requests.size() > 0) {
            req = (MockHttpServletRequest)requests.get(sr.nextInt(requests.size()));
            if (sr.nextBoolean()) req.randomize();
            return req;
        }
                    
        req = new MockHttpServletRequest(tu);
        requests.add(req);
        return req;
    }

    private void testServlet() {
        Throwable excpt = null;
        ServletRequest req = randRequest();
        ServletResponse res = new HttpServletResponse();
        MockHttpServlet s = randServlet();

        if (s.getServletInfo().@internal@isTainted())
            throw new RuntimeException("Unexpected taint");

        try {
            s.service(req, res);
        } catch (Throwable th) {
           excpt = th;
        }

        if ((s.pendingRuntimeException() && excpt == null)
                || (s.pendingJTaintException() && excpt != null))
            throw new RuntimeException("Unexpected Servlet exception behavior");

        Thread t = Thread.currentThread();
        if (t.@internal@getRequestParams() != null 
                || t.@internal@getRemoteHost() != null
                || t.@internal@getRemoteAddr() != null)
            throw new RuntimeException("Non-null Thread servlet fields");
    }

    private void testServletRequest() {
        ServletRequest req = randRequest();
        String s = null, taintS = null, tmp;

        switch(sr.nextInt(8)) {
            case 0:
                taintS = req.getCharacterEncoding();
                s = req.getCharacterEncodingSafe();
                break;

            case 1:
                taintS = req.getContentType();
                s = req.getContentTypeSafe();
                break;

            case 2:
                tmp = tu.randString();
                taintS = req.getParameter(tmp);
                s = req.getParameterSafe(tmp);
                break;

            case 3:
                Map taintM = req.getParameterMap();
                Map m = req.getParameterMapSafe();

                if (!tu.isValidTaintedMap(m, taintM))
                    throw new RuntimeException("Parameter map taint invalid");
                return;

           case 4:
                Enumeration taintE = req.getParameterNames();
                Enumeration e = req.getParameterNamesSafe();

                if (!tu.isValidTaintedEnumeration(e, taintE))
                    throw new RuntimeException("Enumeration taint invalid");
                return;

           case 5:
                tmp = tu.randString();
                String[] taintSa = req.getParameterValues(tmp);
                String[] sa = req.getParameterValuesSafe(tmp);

                if (!tu.isValidTaintedStringArray(sa, taintSa))
                    throw new RuntimeException("Invalid string array");
                return;

           case 6:
                taintS = req.getRemoteHost();
                s = req.getRemoteHostSafe();
                break;

           case 7:
                taintS = req.getRemoteAddr();
                s = req.getRemoteAddrSafe();
                break;

           default:
                throw new RuntimeException("switch");
        }

        if (!tu.isValidTaintedString(s, taintS))
            throw new RuntimeException("Corrupt Servlet method");
    }

    private void testCookie() {
        Cookie c = randCookie();
        String s, taintS;

        switch (sr.nextInt(5)) {
            case 0:
                taintS = c.getComment();
                s = c.getCommentSafe();
                break;

            case 1:
                taintS = c.getDomain();
                s = c.getDomainSafe();
                break;

            case 2:
                taintS = c.getName();
                s = c.getNameSafe();
                break;

            case 3:
                taintS = c.getPath();
                s = c.getPathSafe();
                break;

            case 4:
                taintS = c.getValue();
                s = c.getValueSafe();
                break;

            default:
                throw new RuntimeException("switch");
        }

        if (!tu.isValidTaintedString(s, taintS))
            throw new RuntimeException("Corrupt Cookie method");
    }

    private void testHttpServlet() {
        Throwable excpt = null;
        HttpServletRequest req = randRequest();
        MockHttpServlet s = randServlet();

        if (s.getServletInfo().@internal@isTainted())
            throw new RuntimeException("Unexpected taint");

        try {
            s.service(req, new HttpServletResponse());
        } catch (Throwable th) {
           excpt = th;
        }

        if ((s.pendingRuntimeException() && excpt == null)
                || (s.pendingJTaintException() && excpt != null))
            throw new RuntimeException("Unexpected Servlet exception behavior");

        Thread t = Thread.currentThread();
        if (t.@internal@getRequestParams() != null 
                || t.@internal@getRemoteHost() != null
                || t.@internal@getRemoteAddr() != null)
            throw new RuntimeException("Non-null Thread servlet fields");
    }

    private void testHttpServletRequest() {
        HttpServletRequest req = randRequest();
        String s = null, taintS = null, h;
        Enumeration e, taintE;

        switch(sr.nextInt(12)) {
            case 0:
                taintS = req.getContextPath();
                s = req.getContextPathSafe();
                break;

           case 1:
                h = tu.randString();
                taintS = req.getHeader(h);
                s = req.getHeaderSafe(h);
                break;

          case 2:
                taintE = req.getHeaderNames();
                e = req.getHeaderNamesSafe();
                if (!tu.isValidTaintedEnumeration(e, taintE))
                    throw new RuntimeException("Corrupted enumeration");
                return;

          case 3:
                h = tu.randString();
                taintE = req.getHeaders(h);
                e = req.getHeadersSafe(h);
                if (!tu.isValidTaintedEnumeration(e, taintE))
                    throw new RuntimeException("Corrupted enumeration");
                return;

          case 4:
                taintS = req.getPathInfo();
                s = req.getPathInfoSafe();
                break;

          case 5:
                taintS = req.getPathTranslated();
                s = "/correct/" + req.getPathInfoSafe();
                int prefixLen = "/correct/".length();

                if (!taintS.equals(s))
                    throw new RuntimeException("Path translated corruption");

                if (s.@internal@isTainted())
                    throw new RuntimeException("Unexpected taint");

                if (taintS.substring(0, prefixLen).@internal@isTainted())
                    throw new RuntimeException("Tainted prefix");

                for (int i = prefixLen; i < taintS.length(); i++)
                    if (!taintS.@internal@taint().get(i))
                        throw new RuntimeException("Untainted suffix");
                return;

          case 6:
                taintS = req.getQueryString();
                s = req.getQueryStringSafe();
                break;

          case 7:
                taintS = req.getRemoteUser();
                s = req.getRemoteUserSafe();
                break;

          case 8:
                taintS = req.getRequestedSessionId();
                s = req.getRequestedSessionIdSafe();
                break;

          case 9:
                taintS = req.getRequestURI();
                s = req.getRequestURISafe();
                break;

          case 10:
                taintS = req.getRequestURL().toString();
                s = req.getRequestURLSafe().toString();
                break;

          case 11:
                taintS = req.getServletPath();
                s = req.getServletPathSafe();
                break;

          default:
                throw new RuntimeException("switch");
        }

        if (!tu.isValidTaintedString(s, taintS))
            throw new RuntimeException("Invalid string");
    }

    private void testHttpUtils() {
        int i = sr.nextInt(3);
        HttpServletRequest req = randRequest();

        HttpUtils.init(tu);

        if (i == 0) {
            StringBuffer taintSb = HttpUtils.getRequestURL(req);
            StringBuffer sb = HttpUtils.getRequestURLSafe(req);

            if (!tu.isValidTaintedString(sb.toString(), taintSb.toString()))
                throw new RuntimeException("Invalid stringbuffer taint");
            return;
        }

        Hashtable taintH, h;

        if (i == 1) {
            String s = tu.randString();
            taintH = HttpUtils.parseQueryString(s);
            h = HttpUtils.parseQueryStringSafe(s);
        } else if (i == 2) {
           ServletInputStream is = new ServletInputStream();
           taintH = HttpUtils.parsePostData(0, is);
           h = HttpUtils.parsePostDataSafe(0, is);
        } else { throw new RuntimeException("unexpected if"); }

        if (!tu.isValidTaintedHashtable(h, taintH))
            throw new RuntimeException("Corrupt hash table");
    }

    private void test() {
        switch(sr.nextInt(6)) {
            case 0:
                testServlet();
                break;

            case 1:
                testServletRequest();
                break;

            case 2:
                testCookie();
                break;

            case 3:
                testHttpServlet();
                break;

           case 4:
                testHttpServletRequest();
                break;

           case 5:
                testHttpUtils();
                break;

          default:
                throw new RuntimeException("switch");
        }
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 128;
        int nrtest = 16384;

        ServletTest st;
        String logfile = "ServletTest.log";
        PrintStream ps = null;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-l"))
               maxlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java ServletTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofString] "
                       + "[-n NumberofTests]"
                       + "[-f logFileName]");
               System.exit(-1);
           }
        }

        try {
            ps = new PrintStream(new FileOutputStream(logfile));
        } catch (FileNotFoundException e) {
            System.out.println("Error opening logfile [" + logfile + "]: " + e);
            System.exit(-1);
        }

        ps.print("-s ");
        ps.print(seed);
        ps.print(" -l ");
        ps.print(maxlen);
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        st = new ServletTest(maxlen, new SafeRandom(seed));

        for (int i = 0; i < nrtest; i++) 
            st.test();

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
