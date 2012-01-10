/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.sql.Connection;

/* Supports HypersonicSQL 1.7.0 - 1.8.0 (current) */

public final class HypersonicSqlValidator extends SqlValidator
{
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
                    /* Possibly a block comment (or line comment) */
                    if (i < len - 1 && s.charAt(i+1) == '*') 
                        i = SqlParseUtil.parseBlockComment(s, i);
                    else if (i < len - 1 && s.charAt(i+1) == '/') 
                        i = SqlParseUtil.parseLineComment(s, i, "//");
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

    /* Hypersonic has the same lexical structure across all supported versions
     * and configurations
     */
    public HypersonicSqlValidator(Connection c, int dbMajor, int dbMinor) { }

    public HypersonicSqlValidator() { }
}
