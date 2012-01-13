/* Copyright 2009 Michael Dalton */
package jtaint;

import java.sql.Connection;

/* Oracle DB - supports versions 9i - 11g
 * Lexical changes between 9i and 11g
 *
 *   Feature: support for quote delimited strings
 *   Added: 10g release 1
 *
 *   Feature: Binary floating point numeric literals
 *   Added: 10g release 1
 *
 *   Feature: Escape character used to escape single quote in string literal
 *   NOT ADDED: Mentioned in 10g / 11g, but manual analysis confirms that
 *              escape characters _cannot_ be used to escape single quotes in 
 *              string literals. Oracle Documentation is *INCORRECT* here.
 *
 */

public final class OracleSqlValidator extends SqlValidator
{
    private final boolean binaryFloatingPointNumbers;
    private final boolean quoteDelimitedStrings;

    public boolean isSqlOperator(char c) {
        return "#[]{}(),.;:+-*/%&<>=|_^?".indexOf(c) != -1;
    }

    public int parseOracleQuoteDelimitedString(String s, int offset) {
        int len = s.length();
        Taint t = s.@internal@taint();

        if (offset == len - 1) {
            SqlUtil.parseError(s, "Unterminated quote delimited string");
            return offset;
        }

        char quoteDelim = s.charAt(++offset);
        int origQuoteDelimOffset = offset;

        if (t.get(offset))
            SqlUtil.abortQuery(s,"Tainted delimiter in quote delimited string");

        if (quoteDelim == ' ' || quoteDelim == '\t' || quoteDelim == '\n') {
            SqlUtil.parseError(s, "Forbidden whitespace quote delimiter");
            return offset;
        }

        switch(quoteDelim) {
            case '[':
                quoteDelim = ']';
                break;

            case '{':
                quoteDelim = '}';
                break;

            case '<':
                quoteDelim = '>';
                break;

            case '(':
                quoteDelim = ')';
                break;
        }

        while (++offset < len) {
            char c = s.charAt(offset);

            if (c == '\0') {
                if (t.get(offset)) 
                    SqlUtil.abortQuery(s, "Tainted null byte in string");
                Log.warn(new Throwable("Null byte in SQL Query: " + s));
            }

            if (c != '\'' || s.charAt(offset - 1) != quoteDelim
                    || offset == origQuoteDelimOffset + 1)
                continue;
            if (t.get(offset) || t.get(offset - 1))
                SqlUtil.abortQuery(s, "Tainted character terminates quote "
                                      + "delimited string");
            return offset;
        }

        SqlUtil.parseError(s, "Unterminated quote delimited string literal");
        return offset;
    }
    
    public int parseOracleString(String s, int offset)
    {
        Taint t = s.@internal@taint();

        /* Check for quote delimited string */
        if (quoteDelimitedStrings && offset >= 1 
               && (s.charAt(offset-1) == 'q' || s.charAt(offset-1) == 'Q')) {
            if (t.get(offset-1)) 
                SqlUtil.abortQuery(s, "Tainted quote delimited string");
            else if (offset == 1 || isSqlWhitespace(s.charAt(offset - 2))
                   || isSqlOperator(s.charAt(offset - 2))
                   || isSqlSpecial(s.charAt(offset - 2)))
                return parseOracleQuoteDelimitedString(s, offset);
                
        }

        return SqlParseUtil.parseStringLiteral(s, offset, false);
    }

    public int parseOracleIdentifier(String s, int offset)
    {
        int len = s.length();
        Taint t = s.@internal@taint();

        while (++offset < len) {
            if (t.get(offset))
                SqlUtil.abortQuery(s, "Tainted identifier");
            else if (s.charAt(offset) == '"')
                return offset;
        }

        SqlUtil.parseError(s, "Unterminated quoted identifier");
        return offset;
    }


    /** Parse a tainted value -- which must be a numeric literal
     * for safety reasons. Any other token (identifier, string literal, 
     * keyword, operator) is considered unsafe and will result in a security
     * exception. The tainted numeric literal must occur at the beginning of 
     * the query, or be preceded by whitespace or a valid SQL operator.
     * True and false boolean constants are considered numeric literals.
     * @param s query String
     * @param offset current position of the lexer
     * @return last character (inclusive) of tainted number or -1. 
     *
     * Oracle differs from other databases in that a trailing type specifier
     * is allowed for numeric literals. Valid specifiers are f or F for binary 
     * floating point, and d or D for double precision binary floating point.
     */
    public int parseOracleTaintedValue(String s, int offset,
                                       boolean binaryFloatingPointNumbers)
    {
        int len = s.length();

        if (!binaryFloatingPointNumbers)
            return SqlParseUtil.parseTaintedValue(s, offset, this);

        /* Floating point suffixes apply only to numbers, not booleans  */
        if ((offset <= len - 4 && s.regionMatches(true, offset, "true", 0, 4))
                    || (offset <= len - 5 && 
                        s.regionMatches(true, offset, "false", 0, 5)))
            return SqlParseUtil.parseTaintedValue(s, offset, this);

        /* Parse the number up until the suffix. If that succeeds, and
         * the suffix is OK (or if the suffix is not present), then the number
         * is valid.
         */
        int suffixOffset = -1;
        Taint t = s.@internal@taint();

        for (int i = offset + 1; i < len && t.get(i); i++) {
            if (s.charAt(i) == 'f' || s.charAt(i) == 'F' ||
                    s.charAt(i) == 'd' || s.charAt(i) == 'D') {
                suffixOffset = i;
                break;
            }
        }

        if (suffixOffset < 0)
            return SqlParseUtil.parseTaintedValue(s, offset, this);

        if (suffixOffset < len - 1 && t.get(suffixOffset + 1))
            SqlUtil.abortQuery(s, "Tainted identifier, operator, or keyword"); 

        /* Ensure that the number between offset and suffixOffset is a safe
         * and valid tainted number 
         */
        SqlParseUtil.parseTaintedValue(s.substring(0, suffixOffset), offset,
                                       this);
        return suffixOffset;
    }

    public void validateSqlQuery(String s)
    {
        if (!s.@internal@isTainted()) return;

        Taint t = s.@internal@taint();
        int len = s.length();

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);

            if (t.get(i)) {
                i = parseOracleTaintedValue(s, i, binaryFloatingPointNumbers);
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
                    if (i < len - 1 && s.charAt(i+1) == '*') 
                        i = SqlParseUtil.parseBlockComment(s, i);
                    break;

                case '\'':
                    /* String Literal */
                    i = parseOracleString(s, i);
                    break;

                case '"':
                    /* Quoted identifier */
                    i = parseOracleIdentifier(s, i);
                    break;

                default:
                    break;
            }
        }
    }

    public OracleSqlValidator(Connection c, int dbMajor, int dbMinor) { 
        this.binaryFloatingPointNumbers = dbMajor >= 10;
        this.quoteDelimitedStrings = dbMajor >= 10;
    }

    public OracleSqlValidator(boolean binaryFloatingPointNumbers,
                              boolean quoteDelimitedStrings) {
       this.binaryFloatingPointNumbers = binaryFloatingPointNumbers;
       this.quoteDelimitedStrings = quoteDelimitedStrings;
    }
}
