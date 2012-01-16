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
