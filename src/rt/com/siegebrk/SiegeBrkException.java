/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.Map;
import java.util.Set;

public final class SiegeBrkException extends RuntimeException
{
    private final boolean whitelisted;

    public SiegeBrkException(String message, Map m) { 
        super(message); 
        whitelisted = isOnWhitelist(m);
    }

    public SiegeBrkException(String message, Throwable cause, Map m) {
        super(message, cause);
        whitelisted = isOnWhitelist(m);
    }

    public SiegeBrkException(Throwable cause, Map m) { 
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
