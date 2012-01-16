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
