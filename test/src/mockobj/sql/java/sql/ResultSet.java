/* Copyright 2009 Michael Dalton */
package java.sql;

public interface ResultSet {

    boolean next();

    String getString(int columnIndex);
}
