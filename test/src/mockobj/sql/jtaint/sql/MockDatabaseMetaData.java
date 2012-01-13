/* Copyright 2009 Michael Dalton */
package jtaint.sql;

import java.sql.DatabaseMetaData;

public class MockDatabaseMetaData implements DatabaseMetaData {
    private final String name;
    private final int min, maj;

    public MockDatabaseMetaData(String name, int maj, int min) {
        this.name = name;
        this.min = min;
        this.maj = maj;
    }

    public String getDatabaseProductName() { return name; }

    public String getDatabaseProductVersion() { return name + maj + "." + min; }

    public int getDatabaseMajorVersion() { return maj; }

    public int getDatabaseMinorVersion() { return min; }
}
