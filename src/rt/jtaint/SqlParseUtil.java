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

public final class SqlParseUtil
{
    /** Parse a block comment. Nesting is supported. Tainted data is allowed
     * within the comment, but may neither begin nor end a block comment or
     * nested block comment.
     */ 
    public static int parseNestedBlockComment(String s, int offset) 
    {
        int len = s.length(),
            level = 1;
        Taint t = s.@internal@taint();

        if (t.get(offset) || t.get(offset+1))
            SqlUtil.abortQuery(s, "Tainted comment begin (/*)");

        /* Skip leading '/*' */
        offset++;

        while (level != 0 && ++offset < len) {
            char c = s.charAt(offset);

            if (c == '/' && offset != len-1 && s.charAt(offset + 1) == '*') {
                if (t.get(offset) || t.get(offset+1))
                    SqlUtil.abortQuery(s, "Tainted comment begin (/*)");
                offset++;
                level++;
            } else if (c == '*' && offset != len-1 
                       && s.charAt(offset + 1) == '/') {
                if (t.get(offset) || t.get(offset + 1))
                    SqlUtil.abortQuery(s, "Tainted comment end (*/)");
                offset++;
                level--;
            }
        }

        if (level != 0)
            SqlUtil.parseError(s, "Unterminated block comment");
        return offset;
    }

    /** Parse a block comment. Nesting is not supported. Tainted data is allowed
     * within the comment, but may neither begin nor end the comment.
     */ 
    public static int parseBlockComment(String s, int offset) 
    {
        int len = s.length();
        Taint t = s.@internal@taint();

        if (t.get(offset) || t.get(offset+1))
            SqlUtil.abortQuery(s, "Tainted comment begin (/*)");

        /* Skip leading '/*' */
        offset++;

        while (++offset < len) {
            char c = s.charAt(offset);

            if (c == '*' && offset != len-1 && s.charAt(offset + 1) == '/') {
                if (t.get(offset) || t.get(offset + 1)) 
                    SqlUtil.abortQuery(s, "Tainted comment end (*/)");
                return offset + 1;
            }
        }

        SqlUtil.parseError(s, "Unterminated block comment");
        return offset;
    }

    /** Parse a line comment. Tainted input is allowed within the comment, but
     * must neither begin nor end the line comment.
     */
    public static int parseLineComment(String s, int offset, 
                                       String beginComment) 
    {
        int len = s.length(), cmtlen = beginComment.length();
        Taint t = s.@internal@taint();

        for (int i = 0; i < cmtlen; i++)
            if (t.get(offset + i))
                SqlUtil.abortQuery(s, "Tainted line comment begin(" + 
                                   beginComment + ")");

        /* Skip leading begin comment characters */
        offset += cmtlen - 1;

        while (++offset < len) {
            char c = s.charAt(offset);

            if (c != '\r' && c != '\n') 
                continue;
            else if (!t.get(offset)) 
                return offset;
            else 
                SqlUtil.abortQuery(s, "Tainted newline ends line comment");
        }

        SqlUtil.parseError(s, "Unterminated line comment");
        return offset; /* Allow comment to be terminated by end of query */
    }

    public static int parseStringLiteral(String s, int offset, boolean escapes)
    {
        int len = s.length();
        Taint t = s.@internal@taint();
        char quoteChar = s.charAt(offset);

        while (++offset < len) {
            char c = s.charAt(offset);

            if (c == '\0') {
                if (t.get(offset)) 
                    SqlUtil.abortQuery(s, "Tainted null byte in string");
                Log.warn(new Throwable("Null byte in SQL Query: " + s));
            }

            if (c != quoteChar && (!escapes || c != '\\'))
                continue;

            if (c == quoteChar) {
                if (t.get(offset)) {
                    /* Two single quotes interpreted as a literal quote */
                    if (offset < len-1 && s.charAt(offset+1) == quoteChar
                            && t.get(offset+1))
                        offset++;
                    else
                        SqlUtil.abortQuery(s, "Unsafe tainted quote in string");
                } else {
                    if (offset == len-1 || s.charAt(offset+1) != quoteChar)
                        return offset;
                    if (t.get(offset+1))
                        SqlUtil.abortQuery(s, "Unsafe Tainted quote in string");
                    else
                        offset++;
                }
            } else /* c == '\\' */ {
                if (t.get(offset)) {
                    if (offset < len-1 && t.get(offset+1))
                        offset++;
                    else
                        SqlUtil.abortQuery(s, "Unsafe tainted backslash");
                } else 
                    offset++;
            }
        }

        SqlUtil.parseError(s, "Unterminated string literal");
        return offset;
    }

    public static int parseQuotedIdentifier(String s, int offset)
    {
        int len = s.length();
        Taint t = s.@internal@taint();
        char quoteChar = s.charAt(offset);

        while (++offset < len) {
            char c = s.charAt(offset);

            if (t.get(offset))
                SqlUtil.abortQuery(s, "Tainted identifier");

            /* A string ends with a double-quote. A literal double quote
             * is encoded as "". Thus, if we have an untainted " followed
             * by an untainted character that is not ", or an untainted " 
             * followed by the end of the query, we have reached the end of 
             * this string. 
             */
            if (c != quoteChar) continue;

            if (offset == len - 1 || s.charAt(offset+1) != quoteChar) 
                return offset;

            if (t.get(offset+1)) 
                SqlUtil.abortQuery(s, "Tainted identifier");
            else /* "" is a literal " */
                offset++;
        }
 
        SqlUtil.parseError(s, "Unterminated quoted identifier");
        return offset;
    }

    /** Parse a tainted value -- which must be a numeric or boolean literal
     * for safety reasons. Any other token (identifier, string literal, 
     * keyword, operator) is considered unsafe and will result in a security
     * exception. The tainted numeric literal must occur at the beginning of 
     * the query, or be preceded by whitespace or a valid SQL operator.
     * @param s query String
     * @param offset current position of the lexer
     * @return last character (inclusive) of tainted number or -1. 
     *
     * The input format is (in BNF notation):
     * true (in any combination of upper or lowercase)
     * | false (in any combination of upper or lowercase)
     * | [+-] ({digit}+ | ({digit}+.{digit}*) | ({digit}*.{digit}+)) [(e|E)[+|-]     *   {digit}+]
     * Leading and trailing (potentially tainted) whitespace is allowed.
     */
    public static int parseTaintedValue(String s, int offset, SqlValidator v)
    {
        final int /* Initial state */
                  ST_INITIAL = 0, 

                  /* After parsing (optional) leading + or - */
                  ST_INITIAL_POSTSIGN = 1, 

                  /* When parsing (one or more) leading digits */
                  ST_LEAD_DIGITS = 2,

                  /* After parsing a decimal point. Next character must
                   * be a digit for the number to be valid 
                   */
                  ST_DECIMAL_INVALID = 3,

                  /* After parsing a decimal point, current parsed
                   * string is (so far) a valid number
                   */
                  ST_DECIMAL_VALID = 4,

                  /* After parsing e|E and an optional + or - */
                  ST_EXPONENT_INVALID = 5,

                  /* After parsing one or more trailing digits following the
                   * exponent
                   */
                  ST_EXPONENT_VALID = 6;

        int state = ST_INITIAL,
            len = s.length();
        boolean fail = false;
        Taint t = s.@internal@taint();

        /* Ensure that a numeric literal found at the current offset
         * will actually be parsed as a numeric literal.
         */
      
        if (offset != 0 && !v.isSqlWhitespace(s.charAt(offset - 1))
                   && !v.isSqlOperator(s.charAt(offset - 1))
                   && !v.isSqlSpecial(s.charAt(offset - 1)))
           SqlUtil.abortQuery(s, "Tainted identifier, operator, or keyword"); 

        if (offset <= len - 4) {
            int matchLen = 0;

            /* Check if true or false boolean constant, any case */
            if (s.regionMatches(true, offset, "true", 0, 4))
                matchLen = 4;
            else if (offset <= len - 5 
                     && s.regionMatches(true, offset, "false", 0, 5))
                matchLen = 5;

            for (int i = 0; i < matchLen; i++)
                if (!t.get(offset + i))
                    SqlUtil.abortQuery(s, "Partially tainted boolean constant");
            if (matchLen != 0)
                return offset + matchLen - 1;
        }

        do
        {
            char c = s.charAt(offset);

            switch(state) {
                case ST_INITIAL:
                    if (c == '+' || c == '-') {
                        state = ST_INITIAL_POSTSIGN;
                        break;
                    } /* else fall through */ ;

                case ST_INITIAL_POSTSIGN:
                    if (c >= '0' && c <= '9') 
                        state = ST_LEAD_DIGITS;
                    else if (c == '.')
                        state = ST_DECIMAL_INVALID;
                    else 
                        fail = true;
                    break;

                case ST_LEAD_DIGITS:
                    if (c == '.')
                        state = ST_DECIMAL_VALID;
                    else if (c == 'e' || c == 'E') {
                        if (offset < len - 1 && (s.charAt(offset+1) == '+' 
                                    || s.charAt(offset+1) == '-'))
                            offset++;
                        state = ST_EXPONENT_INVALID;
                    } else if (c < '0' || c > '9')
                        fail = true;
                    break;

                case ST_DECIMAL_INVALID:
                    if (c >= '0' && c <= '9')
                        state = ST_DECIMAL_VALID;
                    else fail = true;
                    break;

               case ST_DECIMAL_VALID:
                    if (c == 'e' || c == 'E') {
                        if (offset < len - 1 && (s.charAt(offset+1) == '+' 
                                    || s.charAt(offset+1) == '-'))
                            offset++;
                        state = ST_EXPONENT_INVALID;
                    } else if (c < '0' || c > '9')
                        fail = true;
                    break;

              case ST_EXPONENT_INVALID: 
                    if (c < '0' || c > '9')
                        fail = true;
                    else
                        state = ST_EXPONENT_VALID;
                    break;

              case ST_EXPONENT_VALID: /* Terminal state */
                    if (c < '0' || c > '9')
                        fail = true;
                    break;

             default:
                    throw new RuntimeException("switch");
            } 
        } while (++offset < len && t.get(offset) && !fail);

        if (state == ST_INITIAL || state == ST_INITIAL_POSTSIGN 
                || state == ST_DECIMAL_INVALID || state == ST_EXPONENT_INVALID
                || fail)
            SqlUtil.abortQuery(s, "Tainted identifier, operator, or keyword");

        return offset - 1;
    }
}
