/* Copyright 2009 Michael Dalton */
package jtaint;

public final class Log extends OrigLog
{
    private static volatile boolean hasVuln, hasError, hasWarning;

    public static void vuln(String type, JTaintException e) {
        OrigLog.vuln(type, e);
        hasVuln = true;
    }

    public static void error(Throwable th) {
        OrigLog.error(th);
        hasError = true;
    }

    public static void error(String msg) {
        OrigLog.error(msg);
        hasError = true;
    }

    public static void warn(Throwable th) {
        OrigLog.warn(th);
        hasWarning = true;
    }

    public static void warn(String msg) {
        OrigLog.warn(msg);
        hasWarning = true;
    }

    public static void    clearVuln()     { hasVuln = false; }

    public static boolean hasVuln()       { return hasVuln; }

    public static void    clearError()    { hasError = false; }

    public static boolean hasError()      { return hasError; }

    public static void    clearWarning()  { hasWarning = false; }

    public static boolean hasWarning()    { return hasWarning; }
}
