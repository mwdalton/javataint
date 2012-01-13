
/* Copyright 2009 Michael Dalton */

package javax.servlet.http;

import jtaint.SafeRandom;
import jtaint.JTaintException;
import jtaint.TestUtil;

import java.util.HashMap;
import java.util.Map;

public class MockHttpServlet extends HttpServlet
{
    private static final Map EMPTY_WHITELIST = new HashMap();

    private boolean jtExcpt, rtExcpt;
    private final TestUtil tu;

    public MockHttpServlet(TestUtil tu) { this.tu = tu; }

    public boolean pendingRuntimeException() { return rtExcpt; }
    public boolean pendingJTaintException() { return jtExcpt; }

    public void process(HttpServletRequest req, HttpServletResponse res) {
        SafeRandom sr = tu.rand();

        /* Verify all thread structures have been setup correctly */

        Thread t = Thread.currentThread();
        String taddr = t.@internal@getRemoteAddr(),
               thost = t.@internal@getRemoteHost();
        Map tParams = t.@internal@getRequestParams();

        if (!taddr.equals(req.getRemoteAddr()) 
                    || !thost.equals(req.getRemoteHost())
                    || !tParams.equals(req.getParameterMap()))
            throw new RuntimeException("Thread servlet fields corrupted");

        if (!tu.isValidTaintedString(req.getRemoteAddrSafe(), taddr)
                || !tu.isValidTaintedString(req.getRemoteHostSafe(), thost)
                || !tu.isValidTaintedMap(req.getParameterMapSafe(), tParams))
            throw new RuntimeException("Serlvet Request fields corrupted");

        if (sr.nextInt(16) == 0) {
            if (sr.nextBoolean()) {
                jtExcpt = true;
                throw new JTaintException("test-jt", EMPTY_WHITELIST);
            } else {
                rtExcpt = true;
                throw new RuntimeException("test-nojt");
            }
        }
    }

    public void clearAllExceptions() { rtExcpt = jtExcpt = false; }
}
