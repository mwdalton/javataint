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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/*
  Postgresql - supporting 7.0 - 8.3
  Lexical changes between 7.0 and 8.3:

    Feature: Nested Block Comments
    Added: 7.1

    Feature: Dollar quoted String Constants
    Added: 8.0

    Feature: escape strings (and standardconformingstrings configuration param)
    Added: 8.1
*/

public final class PostgreSqlValidator extends SqlValidator
{
    private final boolean standardConformingStrings;
    private final boolean nestedBlockComments;
    private final boolean dollarQuotedStrings;

    public boolean isSqlOperator(char c) {
        return "_@#`~!()[]{},.;:+-*/%&<>=|^?".indexOf(c) != -1;

    }

    public boolean isSqlSpecial(char c) {
        return "$'\"".indexOf(c) != -1;
    }

    public boolean isSqlWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    public int parsePGString(String s, int offset, boolean scStrings) 
    {
        int len = s.length();
        Taint t = s.@internal@taint();
        boolean isStandardString = scStrings;

        /* Check for 'Escape string' syntax */
        if (isStandardString && offset >= 1 
               && (s.charAt(offset-1) == 'e' || s.charAt(offset-1) == 'E')) {
            if (t.get(offset-1)) 
                SqlUtil.abortQuery(s, "Tainted escape string specifier");
            else if (offset == 1 || isSqlWhitespace(s.charAt(offset - 2))
                   || isSqlOperator(s.charAt(offset - 2))
                   || isSqlSpecial(s.charAt(offset - 2)))
                isStandardString = false; /* Found escape string */
        }

        return SqlParseUtil.parseStringLiteral(s, offset, !isStandardString);
    }

    public static boolean subStringEqual(final String s, final int offA, 
                                         final int offB, final int len) 
    {
        if (offA < 0 || offB < 0
                || offA >= s.length() || offB >= s.length()
                || offA + len > s.length() || offB + len > s.length())
            return false;
        
        for (int i = 0; i < len; ++i)
        {
            if (s.charAt(offA + i) != s.charAt(offB + i))
                return false;
        }
    
        return true;
    }

    public static boolean isPGDollarStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || c == '_' || (c > 127 && c < 256);
    }

    public static boolean isPGDollarCont(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || c == '_' || (c > 127 && c < 256)
                || (c >= '0' && c <= '9');
    }

    public int parsePGDollarString(String s, int offset) 
    {
        Taint t = s.@internal@taint();
        int len = s.length();
        int tagLen, i;

        if (s.charAt(offset + 1) == '$')
            tagLen = 2;
        else
            tagLen = s.indexOf('$', offset + 1) - offset + 1;

        for (i = offset + tagLen; i < len; i++)
        {
            if (s.charAt(i) == '\0') {
                if (t.get(i)) 
                    SqlUtil.abortQuery(s, "Tainted null byte in dollar string");
                Log.warn(new Throwable("Null byte in SQL Query: " + s));
            }

            if (s.charAt(i) == '$' && subStringEqual(s, offset, i, tagLen)) {
                if (s.substring(i, i + tagLen).@internal@isTainted()) {
                    SqlUtil.abortQuery(s, 
                            "Tainted character terminates dollar string");
                }
                return i + tagLen - 1;
            }
        }

        SqlUtil.parseError(s, "Unterminated dollar literal");
        return i;
    }

    public void validateSqlQuery(String s)
    {
        int len;
        Taint t;
        if (!s.@internal@isTainted()) return;

        t = s.@internal@taint();
        len = s.length();

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);

            if (t.get(i)) {
                i = SqlParseUtil.parseTaintedValue(s, i, this);
                continue;
            }

            switch(c) {
                case '-':
                    /* Possibly a line comment */
                    if (i < len - 1 && s.charAt(i+1) == '-') 
                        i = SqlParseUtil.parseLineComment(s, i, "--");
                    break;

                case '/':
                    /* Possibly a block comment */
                    if (i < len - 1 && s.charAt(i+1) == '*') {
                        if (nestedBlockComments)
                            i = SqlParseUtil.parseNestedBlockComment(s, i);
                        else
                            i = SqlParseUtil.parseBlockComment(s, i);
                    } 
                    break;

                case '\'':
                    /* String Literal */
                    i = parsePGString(s, i, standardConformingStrings);
                    break;

                case '"':
                    /* Quoted identifier */
                    i = SqlParseUtil.parseQuotedIdentifier(s, i);
                    break;

                case '$':
                {
                    /* Possibly a dollar literal */
                    boolean found = false,
                            unsafe = false;

                    if (dollarQuotedStrings && i < len - 1 
                                && (s.charAt(i+1) == '$' 
                                || isPGDollarStart(s.charAt(i+1)))) {
                        for (int d = i + 1; d < len; d++) {
                            if (t.get(d)) {
                                unsafe = true;
                            } else if (s.charAt(d) == '$') {
                                found = true;
                                break;
                            } else if (!isPGDollarCont(s.charAt(d))) 
                                break;
                            
                        }

                        if (found) {
                            if (unsafe)
                                SqlUtil.abortQuery(s, "Tainted dollar string");
                            i = parsePGDollarString(s, i);
                            break;
                        }
                        /* else fall through */
                    }
                }

                default:
                    break;
            }
        }
    }

    public PostgreSqlValidator(Connection c, int dbMajor, int dbMinor) {

        if (dbMajor < 7 || (dbMajor == 7 && dbMinor < 1))
            nestedBlockComments = false;
        else
            nestedBlockComments = true;

        if (dbMajor < 8)
            dollarQuotedStrings = false;
        else
            dollarQuotedStrings = true;

        if (dbMajor < 8 || (dbMajor == 8 && dbMinor < 1)) {
            standardConformingStrings = false;
        } else {
            boolean scStrings = false;
            Statement st = null;

            try {
                String scQuery=
                    "select current_setting('standard_conforming_strings')";
                st = c.createStatement();
                ResultSet rs = st.executeQuery(scQuery);
                if (rs.next() && rs.getString(1).equals("on"))
                    scStrings = true;
            } catch (Throwable th) {
                Log.error(th);
            } 
            
            standardConformingStrings = scStrings;

            if (st != null) {
                try {
                    st.close();
                } catch (Throwable th) { }
            }
        }
    }

    public PostgreSqlValidator(boolean standardConformingStrings, 
                               boolean nestedBlockComments,
                               boolean dollarQuotedStrings)
    {
        this.standardConformingStrings = standardConformingStrings;
        this.nestedBlockComments = nestedBlockComments;
        this.dollarQuotedStrings = dollarQuotedStrings;
    }
}
