/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.sql.Connection;

public abstract class SqlValidator
{

    public boolean isSqlWhitespace(char c) {
        return Character.isWhitespace(c);
    }

    /* Sql operators, tokens, and delimiters as specified by SQL 2003 */
    public boolean isSqlOperator(char c) {
        return "[]{}(),.;:+-*/%&<>=|_^?".indexOf(c) != -1;
    }

    public boolean isSqlSpecial(char c) {
        return c == '\'' || c == '"';
    }

    abstract void validateSqlQuery(String sql);
}
