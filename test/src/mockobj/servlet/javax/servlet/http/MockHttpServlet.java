
/* Copyright 2009 Michael Dalton */

package javax.servlet.http;

import com.siegebrk.SafeRandom;
import com.siegebrk.SiegeBrkException;
import com.siegebrk.TestUtil;

import java.util.HashMap;
import java.util.Map;

public class MockHttpServlet extends HttpServlet
{
    private static final Map EMPTY_WHITELIST = new HashMap();

    private boolean sbExcpt, rtExcpt;
    private final TestUtil tu;

    public MockHttpServlet(TestUtil tu) { this.tu = tu; }

    public boolean pendingRuntimeException() { return rtExcpt; }
    public boolean pendingSiegeBrkException() { return sbExcpt; }

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
                sbExcpt = true;
                throw new SiegeBrkException("test-siege", EMPTY_WHITELIST);
            } else {
                rtExcpt = true;
                throw new RuntimeException("test-nosiege");
            }
        }
    }

    public void clearAllExceptions() { rtExcpt = sbExcpt = false; }
}
