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

import jtaint.sql.MockDatabaseMetaData;
import jtaint.sql.MockConnection;
import jtaint.sql.MockResultSet;
import jtaint.sql.MockRowSet;
import jtaint.sql.MockStatement;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

import javax.sql.RowSet;

public class PostgreSqlTest
{
    private final int maxlen;
    private final boolean scStrings,
                          nestedComments,
                          dollarStrings;

    private final SafeRandom sr;
    private final TestUtil tu;
    private final DatabaseMetaData dmd; 
    private final PostgreSqlValidatorTest pg;

    private final List connections,
                       statements,
                       rowsets;

    public PostgreSqlTest(int maxlen, SafeRandom sr) {
        this.maxlen = maxlen;
        this.sr = sr;
        this.tu = new TestUtil(maxlen, sr, 16);
        
        connections = new ArrayList();
        statements = new ArrayList();
        rowsets = new ArrayList();

        scStrings = sr.nextBoolean();

        int dbMinStart = 0, dbMajStart = 0;
        if (scStrings) {
            /* Standard conforming strings require version >= 8.1 */
            dbMajStart = 8;
            dbMinStart = 1;
        }

        int dbMaj = dbMajStart + sr.nextInt(10 - dbMajStart);
        int dbMin = dbMinStart + sr.nextInt(20 - dbMinStart);

        if (dbMaj < 8)
            dollarStrings = false;
        else 
            dollarStrings = true;

        if (dbMaj < 7 || (dbMaj == 7 && dbMin < 1))
            nestedComments = false;
        else
            nestedComments = true;

        dmd = new MockDatabaseMetaData("PostgreSql", dbMaj, dbMin);

        String dbConfig;
        
        if (dbMaj < 8 || (dbMaj == 8 && dbMin < 1))
            dbConfig = sr.nextBoolean() ? "on" : "off";
        else
            dbConfig = scStrings ? "on" : "off";

        MockResultSet.setResults(Collections.singletonList(dbConfig), true);
        pg = new PostgreSqlValidatorTest(maxlen, sr, scStrings, nestedComments,
                                         dollarStrings);
    }

    private Connection randConnection() {
        if (sr.nextBoolean() && connections.size() > 0)
            return (Connection) connections.get(sr.nextInt(connections.size()));

        Connection c = new MockConnection(dmd);
        connections.add(c);
        return c;
    }

    private Statement randStatement() {
        if (sr.nextBoolean() && statements.size() > 0)
            return (Statement) statements.get(sr.nextInt(statements.size()));

        Statement st = new MockStatement(randConnection());
        statements.add(st);
        return st;
    }

    private RowSet randRowSet() {
        if (sr.nextBoolean() && rowsets.size() > 0)
            return (RowSet) rowsets.get(sr.nextInt(rowsets.size()));

        RowSet rs = new MockRowSet();
        rowsets.add(rs);
        return rs;
    }

    private String randQuery() {
        int len = sr.nextInt(maxlen);
        @StringBuilder@ query = new @StringBuilder@(len);

        pg.clearException();
        while (query.length() < len) 
            pg.appendRandomToken(query, null);

        return query.toString();
    }

    public boolean pendingException() {
        return pg.pendingException();
    }

    private void testRowSet() throws Exception {
        RowSet rs = randRowSet();
        String s = tu.randString();
        boolean excpt = false, 
                expected = false;

        if (s.@internal@isTainted())
            expected = true;

        try {
            rs.setCommand(s);
        } catch (JTaintException e) {
           excpt = true;
        }

        if (excpt != expected)
            throw new RuntimeException("Exception received: " + excpt +
                                       " Expected exception: " + expected);
    }

    private void testStatement() throws Exception {
        Statement st = randStatement();
        String query = randQuery();
        boolean expected = pendingException(),
                excpt = false;
        boolean expectVuln = query.@internal@isTainted();

        try {
            switch(sr.nextInt(10)) {
                case 0:
                    st.addBatch(query);
                    break;

                case 1:
                    st.execute(query);
                    break;

                case 2:
                    st.execute(query, 0);
                    break;

                case 3:
                    st.execute(query, new int[] { 0 });
                    break;

                case 4:
                    st.execute(query, new String[] { null });
                    break;

                case 5:
                    st.executeQuery(query);
                    break;

                case 6:
                    st.executeUpdate(query);
                    break;

                case 7:
                    st.executeUpdate(query,0);
                    break;

                case 8:
                    st.executeUpdate(query, new int[] { 0 });
                    break;

                case 9:
                    st.executeUpdate(query, new String[] { null });
                    break;

                default:
                    throw new RuntimeException("switch");
            }
        } catch (JTaintException e) {
            excpt = true;
        }

        if (expected != excpt)
            throw new RuntimeException("Expected exception: " + expected +
                                       " Got exception: " + excpt + " Query: " 
                                       + query + " taint " + 
                                       query.@internal@taint());
        if (expectVuln != Log.hasVuln())
            throw new RuntimeException("Expected vulnerability: " + expectVuln +
                                       " Got exception: " + Log.hasVuln() + 
                                       " Query: " + query + " taint " + 
                                       query.@internal@taint());
        Log.clearVuln();
        /* Suppress null query byte warnings */
        if (query.indexOf(0) != -1)
            Log.clearWarning();
    }

    private void testConnection() throws Exception {
        Connection c = randConnection();
        String query = randQuery();
        boolean expected = pendingException(),
                excpt = false;
        boolean expectVuln = query.@internal@isTainted();

        try {
            switch(sr.nextInt(9)) {
                case 0:
                    c.prepareCall(query);
                    break;

                case 1:
                    c.prepareCall(query, 0, 0);
                    break;

                case 2:
                    c.prepareCall(query, 0, 0, 0);
                    break;

                case 3:
                    c.prepareStatement(query);
                    break;

                case 4:
                    c.prepareStatement(query, 0);
                    break;

                case 5:
                    c.prepareStatement(query, new int[] { 0 });
                    break;

                case 6:
                    c.prepareStatement(query, 0, 0);
                    break;

                case 7:
                    c.prepareStatement(query, 0, 0, 0);
                    break;

                case 8:
                    c.prepareStatement(query, new String[] { null });
                    break;

                default:
                    throw new RuntimeException("switch");
            }
        } catch (JTaintException e) {
            excpt = true;
        }

        if (expected != excpt)
            throw new RuntimeException("Expected exception: " + expected +
                                       " Got exception: " + excpt + " Query: " 
                                       + query + " taint " + 
                                       query.@internal@taint());
        if (expectVuln != Log.hasVuln())
            throw new RuntimeException("Expected vulnerability: " + expectVuln +
                                       " Got exception: " + Log.hasVuln() + 
                                       " Query: " + query + " taint " + 
                                       query.@internal@taint());
        Log.clearVuln();
        /* Suppress null query byte warnings */
        if (query.indexOf(0) != -1)
            Log.clearWarning();
    }

    private void test() throws Exception {
        switch(sr.nextInt(3)) {
            case 0:
                testRowSet();
                break;

            case 1:
                testConnection();
                break;

            case 2:
                testStatement();
                break;

          default:
                throw new RuntimeException("switch");
        }
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 128;
        int nrtest = 16384;

        PostgreSqlTest pg;
        String logfile = "PostgreSqlTest.log";
        PrintStream ps = null;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-l"))
               maxlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java PostgreSqlTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofString] "
                       + "[-n NumberofTests]"
                       + "[-f logFileName]");
               System.exit(-1);
           }
        }

        try {
            ps = new PrintStream(new FileOutputStream(logfile));
        } catch (FileNotFoundException e) {
            System.out.println("Error opening logfile [" + logfile + "]: " + e);
            System.exit(-1);
        }

        ps.print("-s ");
        ps.print(seed);
        ps.print(" -l ");
        ps.print(maxlen);
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        pg = new PostgreSqlTest(maxlen, new SafeRandom(seed));

        try {
            for (int i = 0; i < nrtest; i++) 
                pg.test();
        } catch (Throwable e) {
            System.err.println(e);
            System.exit(-1);
        }

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
