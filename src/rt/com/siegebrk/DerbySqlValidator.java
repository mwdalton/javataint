/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.sql.Connection;

/* Apache Derby - Supports 10.0 - 10.4 
 * Lexical changes between 10.0 and 10.4:
 *
 *   Feature: Support for nested block comments   
 *   Added: 10.4
 */

public final class DerbySqlValidator extends SqlValidator
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
                        i = SqlParseUtil.parseNestedBlockComment(s, i);
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

    public DerbySqlValidator(Connection c, int dbMajor, int dbMinor) { 
        this.blockComments = dbMajor >= 10 && dbMinor >= 4;
    }

    public DerbySqlValidator(boolean blockComments) { 
        this.blockComments = blockComments; 
    }
}
