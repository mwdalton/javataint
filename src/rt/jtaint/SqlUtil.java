/*
 *  Copyright 2009-2012 Michael Dalton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jtaint;

import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.Connection;
import javax.sql.RowSet;

public final class SqlUtil
{
    static void abortQuery(String query, String msg) throws JTaintException
    {
        @StringBuilder@ sb=new @StringBuilder@(query.length()+msg.length()+64);
        JTaintException e;

        sb.append(msg);
        sb.append("\nQuery : ");
        sb.append(query);
        sb.append("\nTaint Information: ");
        sb.append(query.@internal@taint());
        sb.append('\n');
        e = new JTaintException(sb.toString(), Configuration.sqlWhitelist);

        if (Configuration.sqlPolicyLogAttack)
            Log.attack("SQL Injection", e);
        throw e;
    }

    static void parseError(String query, String msg) 
    {
        @StringBuilder@ sb=new @StringBuilder@(query.length()+msg.length()+64);

        sb.append("Parse error exception: ");
        sb.append(msg);
        sb.append("\nQuery: ");
        sb.append(query);
        sb.append('\n');
        Log.error(new Throwable(sb.toString()));
    }


    public static void validateSqlConnection(String s, Object o)
        throws JTaintException
    {
        try {
            /* Don't check o for null, instanceof is never true for null */
            if (!(o instanceof Connection) || s == null 
                || !s.@internal@isTainted() || !Configuration.sqlPolicyEnabled)
                return;

            if (Configuration.sqlPolicyLogVuln) 
                Log.vuln("SQL Injection", 
                         new JTaintException("Tainted Sql Query: " + s + "\n"
                             + "Taint information " + s.@internal@taint(),
                             Configuration.sqlWhitelist));
            Connection c = (Connection) o;
            SqlValidator v = c.@internal@sqlValidator();
            v.validateSqlQuery(s);
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
        } catch (Throwable e) {
           Log.error(e);
        } 
    }

    public static void validateSqlStatement(String s, Object o)
        throws JTaintException
    {
        try { 
            if (!(o instanceof Statement) || s == null 
                    || !s.@internal@isTainted())
                return;

            Statement st = (Statement) o;
            validateSqlConnection(s, st.getConnection());
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
        } catch (Throwable e) {
           Log.error(e);
        } 
    }

    public static void validateSqlRowSet(String sql, Object o) {
        try { 
            if (!(o instanceof RowSet) || sql == null || 
                !sql.@internal@isTainted() || !Configuration.sqlPolicyEnabled)
                return;
            abortQuery(sql, "Tainted string used as RowSet SQL command");
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
        } catch (Throwable e) {
            Log.error(e);
        }
    }

    public static SqlValidator getSqlValidator(Object o) {
        try {
            if (!(o instanceof Connection)) {
                Log.error(new Throwable("getSqlValidator(Bad object)"));
                return EmptySqlValidator.INSTANCE;
            }

            Connection c = (Connection) o;
            DatabaseMetaData dmd = c.getMetaData();
            String dbName = dmd.getDatabaseProductName().toLowerCase();
            int dbMajor = dmd.getDatabaseMajorVersion();
            int dbMinor = dmd.getDatabaseMinorVersion();

            if (dbName.indexOf("postgres") >= 0)
                return new PostgreSqlValidator(c, dbMajor, dbMinor);
            else if (dbName.indexOf("mysql") >= 0)
                return new MySqlValidator(c, dbMajor, dbMinor);
            else if (dbName.indexOf("hsql") >= 0)
                return new HypersonicSqlValidator(c, dbMajor, dbMinor);
            else if (dbName.indexOf("db2") >= 0)
                return new Db2SqlValidator(c, dbMajor, dbMinor);
            else if (dbName.indexOf("derby") >= 0)
                return new DerbySqlValidator(c, dbMajor, dbMinor);
            else if (dbName.indexOf("oracle") >= 0)
                return new OracleSqlValidator(c, dbMajor, dbMinor);
            else 
                Log.error("Unknown database type " + dbName);
        } catch (Throwable th) {
            Log.error(th);
        }

        return EmptySqlValidator.INSTANCE;
    }
}
