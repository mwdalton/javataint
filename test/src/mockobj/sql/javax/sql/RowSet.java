/* Copyright 2009 Michael Dalton */
package javax.sql;

import java.sql.ResultSet;

import javax.sql.RowSet;

public interface RowSet extends ResultSet {
  void setCommand(String cmd);
}
