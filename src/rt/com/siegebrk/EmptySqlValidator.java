/* Copyright 2009 Michael Dalton */
package com.siegebrk;

public final class EmptySqlValidator extends SqlValidator
{
    public static final EmptySqlValidator INSTANCE = new EmptySqlValidator();

    public void validateSqlQuery(String s) { }
}
