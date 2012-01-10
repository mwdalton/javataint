/* Copyright 2009 Michael Dalton */
package com.siegebrk.sql;

import javax.sql.RowSet;

public class MockRowSet extends MockResultSet implements RowSet
{
    public void setCommand(String cmd) { 
        cmd = cmd + cmd;
    }
}
