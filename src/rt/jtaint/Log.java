/* Copyright 2009 Michael Dalton */
package jtaint;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public final class Log
{
    private static final String LOG_BASE = "jtaint";
    private static final Logger defaultLogger   = Logger.getLogger(LOG_BASE);

    public static void doLog(String logSuffix, String type, JTaintException e)
    {
        if (e.isWhitelisted())
            return;

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Logger logger = Logger.getLogger(LOG_BASE + '.' + logSuffix);

        Thread t = Thread.currentThread();
        String remoteAddr = t.@internal@getRemoteAddr();
        String remoteHost = t.@internal@getRemoteHost();
        Map requestMap = t.@internal@getRequestParams();

        pw.println(type + ":");

        if (remoteHost != null)
            pw.println("Remote Host: " + remoteHost);

        if (remoteAddr != null) 
            pw.println("Remote IP: " + remoteAddr); 

        if (requestMap != null) {
            Set entries = requestMap.entrySet();
            Map.Entry[] params = (Map.Entry[])
                entries.toArray(new Map.Entry[entries.size()]);

            pw.println("Request Parameters:");
            for (int i = 0; i < params.length; i++) {
                String[] v;
                pw.println("Parameter: " + params[i].getKey());
                pw.print("Value(s): ");
                v = (String[]) params[i].getValue();

                for (int j = 0; j < v.length; j++) 
                    pw.print(v[j] + " ");
                pw.println("");
            }
        }

        pw.flush();
        logger.fatal(sw.toString(), e);
    }

    public static void attack(String type, JTaintException e)
    {
        doLog(type + ".attack", type + " Attack", e);
    }
    
    public static void vuln(String type, JTaintException e) {
        doLog(type + ".vuln", type + " Vulnerability", e);
    }
    
    public static void error(Throwable th) {
        defaultLogger.error(th.getMessage(), th);
    }

    public static void error(String msg) {
        defaultLogger.error(msg);
    }

    public static void warn(Throwable th) {
        defaultLogger.warn(th.getMessage(), th);
    }

    public static void warn(String msg) {
        defaultLogger.warn(msg);
    }

    public static void debug(Throwable th) {
        defaultLogger.debug(th.getMessage(), th);
    }

    public static void debug(String msg) {
        defaultLogger.debug(msg);
    }
}
