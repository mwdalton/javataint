/* Copyright 2009 Michael Dalton */
package jtaint;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.BitSet;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.sql.SQLException;

public class PostgreSqlValidatorTest
{

    private final int maxlen;
    private final SafeRandom sr;

    private final boolean standardConformingStrings;
    private final boolean nestedBlockComments;
    private final boolean dollarQuotedStrings;

    private final PostgreSqlValidator v;
    private boolean exception;

    public PostgreSqlValidatorTest(int maxlen, SafeRandom sr,
                                   boolean standardConformingStrings,
                                   boolean nestedBlockComments,
                                   boolean dollarQuotedStrings) 
    {
        this.maxlen = maxlen;
        this.sr = sr;
        this.standardConformingStrings = standardConformingStrings;
        this.nestedBlockComments = nestedBlockComments;
        this.dollarQuotedStrings  = dollarQuotedStrings;
        this.v = new PostgreSqlValidator(standardConformingStrings,
                                         nestedBlockComments,
                                         dollarQuotedStrings);
        clearException();
    }

    /* Can't assume enum support -- so we're going to do this the hard way */
    static class QueryToken {
        static final int Q_SINGLELIT     = 0; /* String Literal - 'foo' */
        static final int Q_DOLLARLIT     = 1; /* String Literal - '$$foo$$' */
        static final int Q_QUOTEID       = 2; /* Quoted Identifier - "foo" */
        static final int Q_NUMBER        = 3; /* Number */
        static final int Q_LINECOMM      = 4; /* Line Comment */
        static final int Q_BLKCOMM       = 5; /* Block Comment */
        static final int Q_OTHER         = 6; /* Operators, Identifiers, etc. */
        static final int Q_BOOLEAN       = 7; /* Boolean Literal */
        static final int Q_END           = 8;

        static String[] qMethod = 
        {
            "SingleQuoteLiteral",
            "DollarLiteral",
            "QuotedIdentifier",
            "Number",
            "LineComment",
            "BlockComment",
            "Other",
            "Boolean"
        };

        static String qChars = "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "0123456789`';,/[]-=~!@#$%^&*()_+{}:\"/,.<>?";

        public static char randChar(SafeRandom sr) { 
            return qChars.charAt(sr.nextInt(qChars.length()));
        }
    }

    public boolean pendingException() { return exception; }
    public void clearException() { exception = false; }
    public void setException() { exception = true; }

    
    private void appendSingleQuoteLiteral(@StringBuilder@ query) {
        int len = sr.nextInt(256); 
        boolean esc = false; /* backslash escape seen */
        boolean scStrings = standardConformingStrings;
        StringBuffer sb = new StringBuffer(len);
        BitSet b = new BitSet();
        String s;

        char qc = ' ';
       
        if (query.length() > 0)
        qc = query.charAt(query.length() - 1);

        if (qc == '\'')
            sb.append(' ');

        if (sr.nextBoolean()) {
            if (!v.isSqlWhitespace(qc) && !v.isSqlOperator(qc) &&
                    !v.isSqlSpecial(qc))
                sb.append(' ');
            if (sr.nextBoolean())
                sb.append('E');
            else
                sb.append('e');
            scStrings = false;
        }
        sb.append('\'');

        for (int i = 0; i < sb.length(); i++) {
            if (sr.nextInt(256) == 0) {
                b.set(i);
                setException();
            }
        }

        for (int i = sb.length(); esc || i < len + 1; i++) {
            int oldlen = sb.length();
            boolean taint = sr.nextBoolean();
            int c;
            int clen;
         
            if (sr.nextInt(12) == 0) 
               //[ifJava5+] 
                c = Character.MIN_CODE_POINT + sr.nextInt(
                        Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
               //[fiJava5+]
               //[ifJava4]
               c = sr.nextInt(0xffff + 1);
               //[fiJava4]
            else 
                c = QueryToken.randChar(sr);

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]

            if (c == '\0' && taint)
                setException();

            if (c == '\'' && !esc) {
                if (!taint) {
                    sb.append('\'');
                    if (sr.nextInt(256) == 0) {
                        b.set(oldlen+1);
                        setException();
                    }
                } else {
                    sb.append('\'');
                    if (sr.nextInt(256) != 0) {
                        b.set(oldlen+1);
                    } else { 
                        setException();
                    }
                }
            } else if (c == '\\' && !scStrings && !esc) {
                esc = true;
            } else if (esc) {
                if (b.get(oldlen-1) && !taint) 
                   setException();
                esc = false;
            }

            //[ifJava5+]
            clen = Character.charCount(c);
            //[fiJava5+]
            //[ifJava4]
            clen = 1;
            //[fiJava4]
            
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
        }

        sb.append('\'');
        if (sr.nextInt(256) == 0) {
            b.set(sb.length() - 1);
            setException();
        }

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
    }

    /* We can't use enums due to Java 1.4 support. These are the states
     * used by the state machine in appendBlockComment
     */

    private static final int DEFAULT = 0,
                             SEEN_SLASH = 1,
                             SEEN_STAR = 2;

    private void appendBlockComment(@StringBuilder@ query) {
        int len = sr.nextInt(64); 
        

        StringBuffer sb = new StringBuffer(len);
        String s;
        BitSet b = new BitSet();

        int level = 1,
            state = DEFAULT;

        sb.append("/*");

        for (int i = 0; level > 0 && i < len; i++) {
            int oldlen = sb.length();
            boolean taint = sr.nextBoolean();
            int c;
            int clen;
            int nextState;
         
            if (sr.nextInt(12) == 0) 
               //[ifJava5+] 
                c = Character.MIN_CODE_POINT + sr.nextInt(
                        Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
               //[fiJava5+]
               //[ifJava4]
               c = sr.nextInt(0xffff + 1);
               //[fiJava4]
            else 
                c = QueryToken.randChar(sr);

            //[ifJava5+]
            clen = Character.charCount(c);
            //[fiJava5+]
            //[ifJava4]
            clen = 1;
            //[fiJava4]

            if (c == '*')
                nextState = SEEN_STAR;
            else if (c == '/')
                nextState = SEEN_SLASH;
            else
                nextState = DEFAULT;

            if (nestedBlockComments && state == SEEN_SLASH && c == '*') {
                level++;
                if (taint || b.get(oldlen-1))
                    setException();
                nextState = DEFAULT;
            } else if (state == SEEN_STAR && c == '/') {
                level--;
                if (taint || b.get(oldlen-1))
                    setException();
                nextState = DEFAULT;
            } 

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]
            
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
            state = nextState;
        }

        for (int i = 0; i < level; i++)
            sb.append(" */");

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
    }

    private void appendLineComment(@StringBuilder@ query) {
        int i, len = 1 + sr.nextInt(64); 
        StringBuffer sb = new StringBuffer(len);
        BitSet b = new BitSet();
        String s;

        sb.append("--");

        for (i = 0; i < len; i++) {
            int oldlen = sb.length();
            boolean taint = sr.nextBoolean();
            int c;
            int clen;
         
            if (sr.nextInt(12) == 0) 
               //[ifJava5+] 
                c = Character.MIN_CODE_POINT + sr.nextInt(
                        Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
               //[fiJava5+]
               //[ifJava4]
               c = sr.nextInt(0xffff + 1);
               //[fiJava4]
            else 
                c = QueryToken.randChar(sr);

            //[ifJava5+]
            clen = Character.charCount(c);
            //[fiJava5+]
            //[ifJava4]
            clen = 1;
            //[fiJava4]

            if (taint && (c == '\r' || c == '\n'))
                setException();

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]
            
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);

            if (c == '\r' || c == '\n')
                break;
        }

        if (i == len) {
            if (sr.nextBoolean())
                sb.append('\r');
            else
                sb.append('\n');
        }

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
    }

    private void appendBoolean(@StringBuilder@ query) {
        String s;
        BitSet b = new BitSet();
        boolean taint = sr.nextBoolean();
        int oldlen = query.length();

        if (oldlen != 0 
                   && !v.isSqlWhitespace(query.charAt(oldlen - 1))
                   && !v.isSqlOperator(query.charAt(oldlen - 1))
                   && !v.isSqlSpecial(query.charAt(oldlen - 1))
                   && taint) {
            if (sr.nextInt(256) == 0)
                setException();
            else {
                query.append(' ');
            }
        }

        if (sr.nextBoolean()) 
            s = "true";
        else
            s = "false";

        @StringBuilder@ sb = new @StringBuilder@(s.length());
        
        for (int i = 0; i < s.length(); i++) {

            if (sr.nextInt(128) == 0) {
                /* Create a partially tainted boolean literal */
                if (!taint) 
                    b.set(i);
            } else if (taint) {
                b.set(i);
            }

            if (sr.nextBoolean())
                sb.append(Character.toUpperCase(s.charAt(i)));
            else
                sb.append(s.charAt(i));
        }

        sb.append(' '); /* In case we are followed by a string literal, an 
                         * E or e could be misinterpreted as an escape string
                         */

        /* Partially tainted boolean literals are forbidden */
        if (b.cardinality() != 0 && b.cardinality() != s.length())
            setException();

        query.append(new String(sb.toString(), new Taint(b, sb.length())));
    }
    
    private void appendOther(@StringBuilder@ query)
    {
        int len = 1 + sr.nextInt(64),
            origLength = query.length(); 
        StringBuffer sb = new StringBuffer(len);
        BitSet b = new BitSet();
        boolean dolvalid = false;
        String s;
               
        for (int i = 0; i < len; i++) {
            int oldlen = sb.length();
            boolean taint = sr.nextInt(64) == 0;
            int c, clen;
            char prevc;
         
            if (sr.nextInt(12) == 0) 
               //[ifJava5+] 
                c = Character.MIN_CODE_POINT + sr.nextInt(
                        Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
               //[fiJava5+]
               //[ifJava4]
               c = sr.nextInt(0xffff + 1);
               //[fiJava4]
            else 
                c = QueryToken.randChar(sr);

            //[ifJava5+]
            clen = Character.charCount(c);
            //[fiJava5+]
            //[ifJava4]
            clen = 1;
            //[fiJava4]

            /* Ensure we aren't creating a boolean literal */
            String q = sb.toString();
            if ((q.regionMatches(true, sb.length() - 3, "tru", 0, 3) 
                    || q.regionMatches(true, sb.length() - 4, "fals", 0, 4))
                    && (c == 'e' || c == 'E')) {
                i--;
                continue;
            }

            if (sb.length() > 0)
                prevc = sb.charAt(sb.length()-1);
            else if (query.length() > 0)
                prevc = query.charAt(query.length()-1);
            else
                prevc = ' ';

            /* Untainted ' or " are handled by SingleQ and QuoteID functions */
            if (c == '\'' || c == '"') { i--; continue; }

            /* Similarly, untainted '/' or '*' that result in comments must
             * be avoided and handled by the LineComment/BlockComment fns  
             */
            else if (prevc == '*' && c == '/') { i--; continue; }

            else if (prevc == '/' && c == '*') { i--; continue; }

            /* Do not create a line comment */
            else if (prevc == '-' && c == '-') { i--; continue; }
           
            /* Do not create a valid dollar tag */ 
            else if (dolvalid && c == '$') { i--; continue; }

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]
            
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);


            /* Update dolvalid - true if we are within a valid (potential)
             * dollar-identifier tag. 
             */
            if (c == '$' && dollarQuotedStrings)
                dolvalid = true;
            else if (dolvalid) {
                for (int j = oldlen; j < sb.length(); j++) {
                    if (dolvalid &&  j > 0 
                            && sb.charAt(j-1) == '$' 
                            && !PostgreSqlValidator.isPGDollarStart(sb.charAt(j)))
                        dolvalid = false;
                    else if(dolvalid && 
                            !PostgreSqlValidator.isPGDollarCont(sb.charAt(j)))
                        dolvalid = false;
                }
             }
        }

        char lastc = sb.charAt(sb.length() - 1);
        /* Ensure that we do not end in a valid dollar tag or comment, or
         * a tainted string literal */
        if (dolvalid || lastc == '/' || lastc == '*' || lastc == '-'
                || b.get(sb.length()-1) || lastc == 'E' || lastc == 'e'
                || (lastc >= '0' && lastc <= '9'))
            sb.append(' ');

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
        

        if (!s.@internal@isTainted())
            return;

        /* Any tainted character will cause a security exception unless all
         * tainted substrings are valid numeric literals
         */
        for (int offset = origLength; offset < query.length(); offset++)
        {
            int toff = 0;

            if (!b.get(offset-origLength)) continue;
            if (pendingException()) break;

            try {
               toff = SqlParseUtil.parseTaintedValue(query.toString(),offset,v);
            } catch (JTaintException e) {
                setException();
            } 

            if (toff > 0) 
                offset = toff;
        }
    }

    private void appendQuotedIdentifier(@StringBuilder@ query)
    {
        int len = sr.nextInt(64); 
        StringBuffer sb = new StringBuffer(len+2);
        BitSet b = new BitSet();
        String s;

        sb.append('"');
        for (int i = 1; i < len + 1; i++) {
            int c, clen;
            int oldlen = sb.length();
            boolean taint;

            if (sr.nextInt(12) == 0) 
               //[ifJava5+] 
                c = Character.MIN_CODE_POINT + sr.nextInt(
                        Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
               //[fiJava5+]
               //[ifJava4]
               c = sr.nextInt(0xffff + 1);
               //[fiJava4]
            else 
                c = QueryToken.randChar(sr);

            //[ifJava5+]
            clen = Character.charCount(c);
            //[fiJava5+]
            //[ifJava4]
            clen = 1;
            //[fiJava4]
            
            taint = sr.nextInt(1024) == 0;
            if (taint)
                setException();

            if (c == '"')
                sb.append('"');

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]
            
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
        }

        sb.append('"');

        sb.append(" "); /* If the next Token begins with a tainted '"', avoid
                         * a security exception
                         */

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
    }

    private void appendNumber(@StringBuilder@ query) {
        String digit = "0123456789";
        int len = 1 + sr.nextInt(32),
            oldlen = query.length(); 
        StringBuffer sb = new StringBuffer();
        boolean taint = sr.nextBoolean();
        String s;

        if (oldlen != 0 && !v.isSqlWhitespace(query.charAt(oldlen - 1))
                   && !v.isSqlOperator(query.charAt(oldlen - 1))
                   && !v.isSqlSpecial(query.charAt(oldlen - 1))
                   && taint) {
            if (sr.nextInt(256) == 0)
                setException();
            else {
                query.append(' ');
                oldlen++;
            }
        }

        if (sr.nextBoolean()) {
            if (sr.nextBoolean())
                sb.append('+');
            else
                sb.append('-');
        }

        if (sr.nextBoolean()) { /* leading digits */
            for (int i = 0; i < len; i++) {
                sb.append(digit.charAt(sr.nextInt(digit.length())));
            }
            if (sr.nextBoolean()) {
                sb.append('.');
                if (sr.nextBoolean()) {
                    int len2 = sr.nextInt(32); 
                    for (int j = 0; j < len2; j++) {
                        sb.append(digit.charAt(sr.nextInt(digit.length())));
                    }
                }
            }

        } else {
            sb.append('.');
            for (int i = 0; i < len; i++) 
                        sb.append(digit.charAt(sr.nextInt(digit.length())));
        }

        if (sr.nextBoolean()) {
            int len3 = 1 + sr.nextInt(32); 
            if (sr.nextBoolean()) 
                sb.append('e');
            else
                sb.append('E');

            if (sr.nextBoolean()) {
                if (sr.nextBoolean())
                    sb.append('+');
                else
                    sb.append('-');
            }

            for (int k = 0; k < len3; k++)
                        sb.append(digit.charAt(sr.nextInt(digit.length())));
        }

        sb.append(' ');
        Taint t = new Taint(taint, sb.length());
        t.clear(sb.length()-1);
        s = new String(sb.toString(), t);
        query.append(s);
    }

    private String randDollarTag() {
        int len = sr.nextInt(64); 
        StringBuffer sb = new StringBuffer(len+2);

        sb.append('$');

        for (int i = 0; i < len; i++) {
            boolean found = false;
            char c;

            /* Find a valid Dollar tag character */
            do { 
                c = (char) sr.nextInt(256);

                /* First char is treated differently */
                if (i == 0) {
                    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || c == '_' || (c > 127 && c < 256))
                        found = true;
                } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || c == '_' || (c > 127 && c < 256) 
                        || (c >= '0' && c <= '9')) {
                    found = true;
                }
            } while (!found);

            sb.append(c);
        }

        sb.append('$');

        return sb.toString();
    }

    
    private void appendDollarQuoteLiteral(@StringBuilder@ query)
    {
        int taglen, len = sr.nextInt(256); 
        StringBuffer sb = new StringBuffer(len+2);
        BitSet b = new BitSet();
        String s, tag;
        boolean match = false; /* true if created terminating tag randomly */

        /* Append tag */
        tag = randDollarTag();
        taglen = tag.length();

        sb.append(tag);

        for (int i = 0; i < taglen; i++) {
            if (sr.nextInt(256) == 0) {
                b.set(i);
                setException();
            }
        }

        /* Append random characters, terminating if we accidentally append
         * $tag$
         */

        for (int i = taglen; i < len + taglen && !match; i++) {
            int c, clen;
            int oldlen = sb.length(), newlen;
            boolean taint = sr.nextBoolean();

            if (sr.nextInt(12) == 0) 
               //[ifJava5+] 
                c = Character.MIN_CODE_POINT + sr.nextInt(
                        Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
               //[fiJava5+]
               //[ifJava4]
               c = sr.nextInt(0xffff + 1);
               //[fiJava4]
            else 
                c = QueryToken.randChar(sr);

            //[ifJava5+]
            clen = Character.charCount(c);
            //[fiJava5+]
            //[ifJava4]
            clen = 1;
            //[fiJava4]

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]

            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);

            newlen = sb.length();

            if (c == '\0' && taint)
                setException();

            /* Did we terminate our dollar identifier randomly? */
            if (c == '$') {
                if (newlen  >= taglen *2  && 
                        sb.substring(newlen - taglen, newlen).equals(tag))
                {
                    match = true;
                    for (int j = newlen - taglen; j < newlen; j++)
                        if (b.get(j))
                            setException();
                }
            }

        }

        /* Append final tag */
        if (!match) {
            int oldlen; 
            
            sb.append(' '); /* in case we ended on a '$' in previous loop */
            oldlen = sb.length();
            sb.append(tag);

            for (int i = oldlen; i < oldlen + taglen; i++)
                if (sr.nextInt(256) == 0) {
                    b.set(i);
                    setException();
                }
        }

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
    }

    public void appendRandomToken(@StringBuilder@ query, List opList)
    {
        int op = sr.nextInt(QueryToken.Q_END);

        if (opList != null)
            opList.add(QueryToken.qMethod[op]);

        switch(op) {
            case QueryToken.Q_SINGLELIT:
                appendSingleQuoteLiteral(query);
                break;

            case QueryToken.Q_DOLLARLIT:
                if (dollarQuotedStrings)
                    appendDollarQuoteLiteral(query);
                break;
            
            case QueryToken.Q_QUOTEID:
                appendQuotedIdentifier(query);
                break;

            case QueryToken.Q_NUMBER: 
                appendNumber(query); 
                break;

            case QueryToken.Q_LINECOMM:
                appendLineComment(query);
                break;

            case QueryToken.Q_BLKCOMM:
                appendBlockComment(query);
                break;
            
            case QueryToken.Q_OTHER: 
                appendOther(query);
                break;

            case QueryToken.Q_BOOLEAN:
                appendBoolean(query);
                break;

            default: 
                throw new RuntimeException("switch"); 
        }
    }

    public void test()
    {
        int len = 1 + sr.nextInt(maxlen-1);
        
        List opList = new ArrayList();
        @StringBuilder@ query = new @StringBuilder@();
        clearException(); 

        while(query.length() < len) {
            @StringBuilder@ oldQuery = new @StringBuilder@(query.toString());
            Throwable e = null;
            appendRandomToken(query, opList);

            try {
                v.validateSqlQuery(query.toString());
            } catch (JTaintException s) {
                if (pendingException()) {
                    clearException();
                    if (query.toString().indexOf(0) != -1)
                        Log.clearWarning();
                    query = oldQuery;
                    opList.add("Exception");
                    continue;
                } else {
                    e = s;
                }
            } catch(Throwable th) {
                e = th;
            }

            /* Suppress null byte in SQL query warnings */
            if (query.toString().indexOf(0) != -1)
                Log.clearWarning();

            if (e != null || pendingException()) {
                String[] ops = new String[opList.size()];
                opList.toArray(ops);

                System.out.println("FAILURE-- query: " + query);
                System.out.println("Taint " + 
                                 query.toString().@internal@taint().toString());
                System.out.println("Standard conforming strings: " 
                                   + standardConformingStrings);
                System.out.println("Nested block comments: " 
                                   + nestedBlockComments);
                System.out.println("Dollar quoted Strings: " 
                                   + dollarQuotedStrings);

                for (int j = 0; j < ops.length; j++)
                    System.out.println("op " + j + ": " + ops[j]);
                if (pendingException())
                    System.out.println("FAILURE: did not get exception");
                else {
                    System.out.println("FAILURE " + e);
                    e.printStackTrace();
                }
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 65536; 
        int nrtest = 16384;

        SafeRandom sr;
        PostgreSqlValidatorTest qt;
        String logfile = "PostgreSqlValidatorTest.log";
        PrintStream ps = null;

        for (int i = 0; i < args.length; i++) {
           if (args[i].equals("-s"))
               seed = Long.decode(args[++i]).longValue();
           else if (args[i].equals("-l"))
               maxlen = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-n"))
               nrtest = Integer.decode(args[++i]).intValue();
           else if (args[i].equals("-f"))
               logfile = args[++i];
           else {
               System.out.println("Usage: java PostgreSqlValidatorTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofQuery] "
                       + "[-n NumberofTests]"
                       + "[-f logFileName]");
               System.exit(-1);
           }
        }

        try {
            ps = new PrintStream(new FileOutputStream(logfile));
        } catch (FileNotFoundException e) {
            System.out.println("Error opening logfile [" + logfile + "]: " + e);
            System.exit(-1);
        }

        ps.print("-s ");
        ps.print(seed);
        ps.print(" -l ");
        ps.print(maxlen);
        ps.print(" -n ");
        ps.print(nrtest);
        ps.print(" -f ");
        ps.print(logfile + "\n");
        ps.flush();
        ps.close();

        sr = new SafeRandom(seed);
        qt = new PostgreSqlValidatorTest(maxlen, sr, sr.nextBoolean(),
                                         sr.nextBoolean(), sr.nextBoolean());

        for (int i = 0; i < nrtest; i++) 
                qt.test();

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
     }
}
