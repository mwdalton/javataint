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

import java.util.Map;
import java.util.Set;

public final class JTaintException extends RuntimeException
{
    private final boolean whitelisted;

    public JTaintException(String message, Map m) { 
        super(message); 
        whitelisted = isOnWhitelist(m);
    }

    public JTaintException(String message, Throwable cause, Map m) {
        super(message, cause);
        whitelisted = isOnWhitelist(m);
    }

    public JTaintException(Throwable cause, Map m) { 
        super(cause); 
        whitelisted = isOnWhitelist(m);
    }

    private boolean isOnWhitelist(Map m) {
        try {
            StackTraceElement[] st = getStackTrace();

            for (int i = 0; i < st.length; i++) {
                String className = st[i].getClassName();
                String methodName = st[i].getMethodName();

                if (className == null || methodName == null) {
                    Log.error("Stack Trace has null class or method name");
                    continue;
                }

                Set s = (Set) m.get(className);
                if (s != null && (s.contains(methodName) || s.contains("*"))) 
                    return true;
            }
        } catch (Throwable th) {
            Log.error(th);
        }
        return false;
    }

    public boolean isWhitelisted() { return whitelisted; }
}
