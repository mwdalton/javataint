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
