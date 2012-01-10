/* Copyright 2009 Michael Dalton */
package java.lang;

public interface SafeCharSequence extends CharSequence 
{
    SafeString toSafeString();
}
