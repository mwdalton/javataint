/* Copyright 2009 Michael Dalton */
package com.siegebrk.sql;

import java.util.ArrayList;
import java.util.List;

import java.sql.ResultSet;

public class MockResultSet implements ResultSet {
    private static List results = new ArrayList();
    private static int offset;
    private static boolean wrapAroundRetval;

    public static void setResults(List results, boolean wrapAroundRetval) {
        MockResultSet.results = results;
        MockResultSet.offset = 0;
        MockResultSet.wrapAroundRetval = wrapAroundRetval;
    }

    public boolean next() { 
        if (offset >= results.size()) {
            offset = 0;
            return wrapAroundRetval;
        } else if (results.get(offset) == null) {
            offset++;
            return false;
        }

        return true;
    }

    public String getString(int columnIndex) { 
        return (String) results.get(offset++);
    }
}
