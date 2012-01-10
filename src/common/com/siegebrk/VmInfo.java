/* Copyright 2009 Michael Dalton */
package com.siegebrk;

final class VmInfo
{
    /* Supported vendors */
    public static final int VENDOR_SUN     = 0;
    public static final int VENDOR_BEA     = 1;
    public static final int VENDOR_IBM     = 2;
    public static final int VENDOR_UNKNOWN = -1;

    /* Supported JRE versions */
    public static final int VERSION1_4      = 0;
    public static final int VERSION1_5      = 1;
    public static final int VERSION1_6      = 2;
    public static final int VERSION_UNKNOWN = -1;

    private static final int vendor, version;

    static {
        int vn = VENDOR_UNKNOWN,
            vr = VERSION_UNKNOWN;

        String s = System.getProperty("java.vendor").toLowerCase();

        if (s.indexOf("sun") >= 0)
            vn = VENDOR_SUN;
        else if (s.indexOf("ibm") >= 0)
            vn = VENDOR_IBM;
        else {
            s = System.getProperty("java.vm.name").toLowerCase();
            if (s.indexOf("jrockit") >= 0)
                vn = VENDOR_BEA;
        }

        s = System.getProperty("java.specification.version");

        if ("1.4".equals(s))
            vr = VERSION1_4;
                
        else if ("1.5".equals(s))
            vr = VERSION1_5;

        else if ("1.6".equals(s))
            vr = VERSION1_6;

        vendor = vn;
        version = vr;
    }

    public static int vendor() { return vendor; }

    public static int version() { return version; }
}
