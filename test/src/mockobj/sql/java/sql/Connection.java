/* Copyright 2009 Michael Dalton */
package java.sql;

public interface Connection {

    Statement createStatement();

    PreparedStatement prepareStatement(String sql);

    CallableStatement prepareCall(String sql);

    DatabaseMetaData getMetaData();

    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency);

    CallableStatement prepareCall(String sql, int resultSetType,
                                  int resultSetConcurrency); 

    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency, 
                                       int resultSetHoldability);

    CallableStatement prepareCall(String sql, int resultSetType,
                                  int resultSetConcurrency,
                                  int resultSetHoldability);
    PreparedStatement prepareStatement(String sql, int autoGeneratedKeys);

    PreparedStatement prepareStatement(String sql, int columnIndexes[]);
    
    PreparedStatement prepareStatement(String sql, String columnNames[]);
}
