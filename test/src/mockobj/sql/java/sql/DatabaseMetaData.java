/* Copyright 2009 Michael Dalton */
package java.sql;

public interface DatabaseMetaData {
    String getDatabaseProductName();

    String getDatabaseProductVersion();

    int getDatabaseMajorVersion();

    int getDatabaseMinorVersion();
}
