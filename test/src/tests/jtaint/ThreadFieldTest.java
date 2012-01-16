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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ThreadFieldTest
{
    private final Random r;

    private int randChar() {
        //[ifJava5+]
        return Character.MIN_CODE_POINT + 
            r.nextInt(Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
        //[fiJava5+]
        //[ifJava4]
        return r.nextInt(0xffff + 1);
        //[fiJava4]
    }

    private String randString() {
        int len = r.nextInt(64);
        @StringBuilder@ sb = new @StringBuilder@(len);

        for (int i = 0; i < len; i++) 
            sb.append(
                      //[ifJava4]
                      (char)
                      //[fiJava4]
                      randChar());
        return sb.toString();
    }
    
    private Map randMap() {
        HashMap h = new HashMap();
        int len = 1 + r.nextInt(64);

        for (int i = 0; i < len; i++)
            h.put(randString(), randString());

        return h;
    }

    private void testRemoteAddr(Thread t) {
        String s;

        if (r.nextInt(128) == 0)
            s = null;
        else
            s = randString();

        t.@internal@setRemoteAddr(s);
        
        if(s == null && t.@internal@getRemoteAddr() != null)
            throw new IllegalArgumentException();
        else if (s != null && !t.@internal@getRemoteAddr().equals(s))
            throw new IllegalArgumentException();
    }

    private void testRemoteHost(Thread t) {
        String s;

        if (r.nextInt(128) == 0)
            s = null;
        else
            s = randString();
        
        t.@internal@setRemoteHost(s);
        
        if(s == null && t.@internal@getRemoteHost() != null)
            throw new IllegalArgumentException();
        else if (s != null && !t.@internal@getRemoteHost().equals(s))
            throw new IllegalArgumentException();
    }

    private void testRequestParams(Thread t) {
        Map m;

        if (r.nextInt(128) == 0)
            m = null;
        else
            m = randMap();
        
        t.@internal@setRequestParams(m);

        if(m == null && t.@internal@getRequestParams() != null)
            throw new IllegalArgumentException();
        else if (m != null && !t.@internal@getRequestParams().equals(m))
            throw new IllegalArgumentException();
    }

    private void test(Thread t) {
        switch(r.nextInt(3)) {
            case 0:
                testRemoteAddr(t);
                break;

            case 1:
                testRemoteHost(t);
                break;

            case 2:
                testRequestParams(t);
                break;

            default:
                throw new RuntimeException("switch");
        }
    }

    public ThreadFieldTest(Random r) {
        this.r = r;
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int nrtest = 16384;

        Random r;
        ThreadFieldTest tft;
        String logfile = "ThreadFieldTest.log";
        PrintStream ps = null;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java ThreadFieldTest "
                       + "[-s randomSeed] "
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
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        r = new Random(seed);
        tft = new ThreadFieldTest(r);

        Thread t = Thread.currentThread();

        if (t.@internal@getRemoteHost() != null ||
                t.@internal@getRemoteAddr() != null ||
                t.@internal@getRequestParams() != null)
            throw new IllegalArgumentException("not initially null");

        for (int i = 0; i < nrtest; i++) 
            tft.test(t);

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
