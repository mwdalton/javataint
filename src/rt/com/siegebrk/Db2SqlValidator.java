/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.sql.Connection;

/* IBM DB2 - supports versions 8.1 - 9.5 
 * Lexical changes between 8.1 and 9.5:
 *
 *   Feature: Support for non-nested block comments   
 *   Added: 9.0
 */

public final class Db2SqlValidator extends SqlValidator
{
    private final boolean blockComments;

    public void validateSqlQuery(String s)
    {
        if (!s.@internal@isTainted()) return;

        Taint t = s.@internal@taint();
        int len = s.length();

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
                    /* Possibly a block comment  */
                    if (blockComments && i < len - 1 && s.charAt(i+1) == '*') 
                        i = SqlParseUtil.parseBlockComment(s, i);
                    break;

                case '\'':
                    /* String Literal */
                    i = SqlParseUtil.parseStringLiteral(s, i, false);
                    break;

                case '"':
                    /* Quoted identifier */
                    i = SqlParseUtil.parseQuotedIdentifier(s, i);
                    break;

                default:
                    break;
            }
        }
    }

    public Db2SqlValidator(Connection c, int dbMajor, int dbMinor) { 
        this.blockComments = dbMajor >= 9;
    }

    public Db2SqlValidator(boolean blockComments) { 
        this.blockComments = blockComments; 
    }
}
