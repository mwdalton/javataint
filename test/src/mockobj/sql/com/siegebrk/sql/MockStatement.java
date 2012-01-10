/* Copyright 2009 Michael Dalton */
package com.siegebrk.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class MockStatement implements Statement {

    private final Connection c;

    public MockStatement(Connection c) {
        this.c = c;
    }

    public ResultSet executeQuery(String sql) {
        ResultSet r = new MockResultSet();
        sql = sql + r;
        return r;
    }

    public int executeUpdate(String sql) {
        sql = sql + sql;
        return 0;
    }

    public boolean execute(String sql) {
        sql = sql + sql;
        return true;
    }

    public void addBatch(String sql) {
        sql = sql + sql;
    }

    public Connection getConnection() {
        return c;
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) {
        sql = sql + autoGeneratedKeys;
        return 0;
    }
    
    public int executeUpdate(String sql, int columnIndexes[]) {
        sql = sql + columnIndexes;
        return 0;
    }

    public int executeUpdate(String sql, String columnNames[]) {
        sql = sql + columnNames;
        return 0;
    }

    public boolean execute(String sql, int autoGeneratedKeys) {
        sql = sql + sql;
        return true;
    }

    public boolean execute(String sql, int columnIndexes[]) {
        sql = sql + sql;
        return true;
    }

    public boolean execute(String sql, String columnNames[]) {
        sql = sql + sql;
        return true;
    }

    public void close() { }
}
