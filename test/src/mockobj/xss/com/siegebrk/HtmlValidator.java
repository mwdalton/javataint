/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.Locale;

public final class HtmlValidator extends OrigHtmlValidator
{
    private @StringBuilder@ sb;

    public HtmlValidator(String charset, String contentType) {
        super(charset, contentType);
        sb = new @StringBuilder@();
    }

    public String toString() { return sb.toString(); }

    public void print(String s) {
        sb.append(s);
        super.print(s);
    }
}
