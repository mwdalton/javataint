/* Copyright 2009 Michael Dalton */
package jtaint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/* Supports MySql 4.0 - 6.0 (current) */

public final class MySqlValidator extends SqlValidator
{
    private final boolean noBackslashEscapes;
    private final boolean ansiQuotes;

    public boolean isSqlOperator(char c) {
        return "!~@[]{}(),.;:+-*/%&<>=|^?".indexOf(c) != -1;

    }

    public boolean isSqlSpecial(char c) {
        return "\"'`".indexOf(c) != -1;
    }

    /** Parse a block comment. Nesting is not supported. Tainted data is allowed
     * within the comment, but may neither begin nor end the comment.
     */ 
    public int parseMYBlockComment(String s, int offset) 
    {
        int len = s.length();
        Taint t = s.@internal@taint();
        boolean isCommand = false;

        if (offset < len - 2 && s.charAt(offset+2) == '!') {
            if (t.get(offset+2))
                SqlUtil.abortQuery(s, "Tainted block comment command");
            isCommand = true;
            offset++;
        }

        /* Skip leading '/*' */
        offset++;

        while (++offset < len) {
            char c = s.charAt(offset);

            if (isCommand && t.get(offset))
                SqlUtil.abortQuery(s, "Tainted character in comment command");

            if (c == '*' && offset != len-1 && s.charAt(offset + 1) == '/') {
                if (t.get(offset) || t.get(offset + 1)) 
                    SqlUtil.abortQuery(s, "Tainted comment end (*/)");
                return offset + 1;
            }
        }

        SqlUtil.parseError(s, "Unterminated block comment");
        return offset;
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

                case '#':
                    i = SqlParseUtil.parseLineComment(s, i, "#");
                    break;

                case '-':
                    /* Possibly a line comment */
                    if (i < len - 2 && s.charAt(i+1) == '-' 
                            && isSqlWhitespace(s.charAt(i+2))) 
                        i = SqlParseUtil.parseLineComment(s, i, 
                                                          "--" + s.charAt(i+2));
                    break;

                case '/':
                    /* Possibly a block comment */
                    if (i < len - 1 && s.charAt(i+1) == '*') {
                        if (t.get(i+1)) 
                            SqlUtil.abortQuery(s, "Tainted block comment");

                        i = parseMYBlockComment(s, i);
                    }  
                    break;

                case '\'':
                    /* String Literal */
                    i = SqlParseUtil.parseStringLiteral(s, i, 
                                                        !noBackslashEscapes);
                    break;

                case '"':
                    if (ansiQuotes)
                        i = SqlParseUtil.parseQuotedIdentifier(s, i);
                    else
                        i = SqlParseUtil.parseStringLiteral(s, i,  
                                                           !noBackslashEscapes);
                    break;

              case '`':
                    i = SqlParseUtil.parseQuotedIdentifier(s, i);
                    break;

                default:
                    break;
            }
        }
    }

    public MySqlValidator(Connection c, int dbMajor, int dbMinor) { 
        String modes = "";
        Statement st = null;

        try {
                String query=
                    "select @@global.sql_mode";
                st = c.createStatement();
                ResultSet rs = st.executeQuery(query);
                while (rs.next())
                    modes += rs.getString(1);

                query = "select @@session.sql_mode";
                rs = st.executeQuery(query);
                while (rs.next())
                    modes += rs.getString(1);

                st.close();
        } catch (Throwable th) {
            Log.error(th);
        }

        if (modes.indexOf("NO_BACKSLASH_ESCAPES") >= 0)
            noBackslashEscapes = true;
        else
            noBackslashEscapes = false;

        if (modes.indexOf("ANSI_QUOTES") >= 0)
            ansiQuotes = true;
        else
            ansiQuotes = false;

        if (st != null) {
            try {
                st.close();
            } catch (Throwable th) { }
        }
    }

    public MySqlValidator(boolean ansiQuotes, boolean noBackslashEscapes) { 
        this.ansiQuotes = ansiQuotes;
        this.noBackslashEscapes = noBackslashEscapes;
    }
}
