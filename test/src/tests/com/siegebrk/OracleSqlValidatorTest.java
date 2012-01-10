/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.BitSet;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.sql.SQLException;

public class OracleSqlValidatorTest
{

    private final int maxlen;
    private final SafeRandom sr;
    private final boolean binaryFloatingPointNumbers,
                          quoteDelimitedStrings;
    private final OracleSqlValidator v;
    private boolean exception;

    public OracleSqlValidatorTest(int maxlen, SafeRandom sr,
                                  boolean binaryFloatingPointNumbers,
                                  boolean quoteDelimitedStrings) {
        this.maxlen = maxlen;
        this.sr = sr;
        this.binaryFloatingPointNumbers = binaryFloatingPointNumbers;
        this.quoteDelimitedStrings = quoteDelimitedStrings;
        this.v = new OracleSqlValidator(binaryFloatingPointNumbers,
                                        quoteDelimitedStrings);
        clearException();
    }

    /* Can't assume enum support -- so we're going to do this the hard way */
    static class QueryToken {
        static final int Q_SINGLELIT     = 0; /* String Literal - 'foo' */
        static final int Q_QUOTEID       = 1; /* Quoted Identifier - "foo" */
        static final int Q_NUMBER        = 2; /* Number */
        static final int Q_LINECOMM      = 3; /* Line Comment */
        static final int Q_BLKCOMM       = 4; /* Block Comment */
        static final int Q_OTHER         = 5; /* Operators, Identifiers, etc. */
        static final int Q_BOOLEAN       = 6; /* Boolean Literal */
        static final int Q_QUOTELIT      = 7;  /* Quote Delimited Literal */
        static final int Q_END           = 8;

        static String[] qMethod = 
        {
            "SingleQuoteLiteral",
            "QuotedIdentifier",
            "Number",
            "LineComment",
            "BlockComment",
            "Other",
            "Boolean",
            "QuoteDelimitedLiteral"
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

    private int randChar() {
        if (sr.nextInt(12) == 0) {
            //[ifJava5+] 
            return Character.MIN_CODE_POINT + sr.nextInt(
                    Character.MAX_CODE_POINT - Character.MIN_CODE_POINT);
            //[fiJava5+]
            //[ifJava4]
            return sr.nextInt(0xffff + 1);
            //[fiJava4]
        } else {
            return QueryToken.randChar(sr);
        }
    }

    private void appendQuoteDelimitedLiteral(@StringBuilder@ query) {
        int len = sr.nextInt(256); 
        StringBuffer sb = new StringBuffer(len);
        BitSet b = new BitSet();
        String s;

        char qc = ' ', qcprev = ' ';
        boolean qcTaint = false;
      
        int oldLength = query.length(); 
        if (oldLength > 0)
            qc = query.charAt(oldLength - 1);
        if (oldLength > 1)
            qcprev = query.charAt(oldLength - 2);

        if ((qc != 'q' && qc != 'Q')) {
            if (oldLength > 0 
                    && !v.isSqlWhitespace(qc)
                    && !v.isSqlOperator(qc)
                    && !v.isSqlSpecial(qc))
                sb.append(' ');
            sb.append(sr.nextBoolean() ? 'q' : 'Q');
        } else if (oldLength > 1 
             && !v.isSqlWhitespace(qcprev)
             && !v.isSqlOperator(qcprev)
             && !v.isSqlSpecial(qcprev)) {
                sb.append(' ');
                sb.append(sr.nextBoolean() ? 'q' : 'Q');
        }

        sb.append('\'');
        
        char quoteDelim = (char) randChar();
        while (quoteDelim == ' ' || quoteDelim == '\t' || quoteDelim == '\n')
            quoteDelim = (char) randChar();
        sb.append(quoteDelim);

        for (int i = 0; i < sb.length(); i++) {
            if (sr.nextInt(256) == 0) {
                b.set(i);
                setException();
            }
        }

        if (quoteDelim == '[')
            quoteDelim = ']';
        else if (quoteDelim == '{')
            quoteDelim = '}';
        else if (quoteDelim == '<')
            quoteDelim = '>';
        else if (quoteDelim == '(')
            quoteDelim = ')';

        boolean first = true;
        for (int i = sb.length(); i < len + 1; i++, first = false) {
            int oldlen = sb.length();
            boolean taint = sr.nextBoolean();
            int c = randChar();
            int clen;
        
            /* Do not prematurely terminate quote delimited literal */
            if (!first && sb.charAt(oldlen - 1) == quoteDelim)
                while (c == '\'')
                    c = randChar();

            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]

            if (c == '\0' && taint)
                setException();

            //[ifJava5+]
            clen = Character.charCount(c);
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
            //[fiJava5+]
            //[ifJava4]
            b.set(oldlen, taint);
            //[fiJava4]
        }

        if (sb.charAt(sb.length() - 1) == quoteDelim)
            sb.append(' ');

        sb.append(quoteDelim);
        sb.append('\'');

        if (sr.nextInt(256) == 0) {
            switch (sr.nextInt(3)) {
                case 0:
                    b.set(sb.length() - 1);
                    break;
                case 1: 
                    b.set(sb.length() - 2);
                    break;
                case 2:
                    b.set(sb.length() - 2);
                    b.set(sb.length() - 1);
                    break;
                default:
                    throw new RuntimeException("switch");
            }
            setException();
        }

        s = new String(sb.toString(), new Taint(b, sb.length()));
        query.append(s);
    }

    private void appendSingleQuoteLiteral(@StringBuilder@ query) {
        int len = sr.nextInt(256); 
        StringBuffer sb = new StringBuffer(len);
        BitSet b = new BitSet();
        String s;

        char qc = ' ';
       
        if (query.length() > 0)
            qc = query.charAt(query.length() - 1);
        
        if ((qc == 'q' || qc == 'Q') && quoteDelimitedStrings) {
            appendQuoteDelimitedLiteral(query);
            return;
        }

        if (qc == '\'')
            sb.append(' ');


        sb.append('\'');

        for (int i = 0; i < sb.length(); i++) {
            if (sr.nextInt(256) == 0) {
                b.set(i);
                setException();
            }
        }

        for (int i = sb.length(); i < len + 1; i++) {
            int oldlen = sb.length();
            boolean taint = sr.nextBoolean();
            int c = randChar();
            int clen;
        
            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]

            if (c == '\0' && taint)
                setException();

            if (c == '\'') {
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
            } 

            //[ifJava5+]
            clen = Character.charCount(c);
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
            //[fiJava5+]
            //[ifJava4]
            b.set(oldlen, taint);
            //[fiJava4]
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

    private void appendBlockComment(@StringBuilder@ query, boolean nested) {
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
            int c = randChar();
            int clen;
            int nextState;
        
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

            if (nested && state == SEEN_SLASH && c == '*') {
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
            int c = randChar();
            int clen;
         
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
                   && !v.isSqlWhitespace(query.charAt(oldlen-1))
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
        String s;
               
        for (int i = 0; i < len; i++) {
            int oldlen = sb.length();
            boolean taint = sr.nextInt(64) == 0;
            int c = randChar(), clen;
            char prevc;
         
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
           
            //[ifJava5+]
            sb.append(Character.toChars(c));
            //[fiJava5+]
            //[ifJava4]
            sb.append((char)c);
            //[fiJava4]
            
            for (int j = 0; j < clen; j++)
                b.set(oldlen + j, taint);
        }

        char lastc = sb.charAt(sb.length() - 1);
        /* Ensure that we do not end in a valid dollar tag or comment, or
         * a tainted string literal */
        if (lastc == '/' || lastc == '*' || lastc == '-'
                || b.get(sb.length()-1) || lastc == 'Q' || lastc == 'q'
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
               toff = v.parseOracleTaintedValue(query.toString(), offset, 
                                                binaryFloatingPointNumbers);
            } catch (SiegeBrkException e) {
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
            int c = randChar(), clen;
            int oldlen = sb.length();
            boolean taint;

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

    private void addSuffix(@StringBuilder@ sb, boolean taint) {
        char binarySuffixes[] = { 'f', 'F', 'd', 'D' };

        sb.append(binarySuffixes[sr.nextInt(binarySuffixes.length)]);
        if (!binaryFloatingPointNumbers && taint)
            setException();
    }

    private void appendNumber(@StringBuilder@ query) {
        String digit = "0123456789";
        int len = 1 + sr.nextInt(32),
            oldlen = query.length(); 
        @StringBuilder@ sb = new @StringBuilder@();
        boolean taint = sr.nextBoolean(),
                skip = false;
        String s;

        if (oldlen != 0 && !v.isSqlWhitespace(query.charAt(oldlen-1))
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
            if (sr.nextInt(32) == 0) {
                addSuffix(sb, taint);
                skip = true;
            }
            if (!skip && sr.nextBoolean()) {
                sb.append('.');
                if (sr.nextBoolean()) {
                    int len2 = sr.nextInt(32); 
                    for (int j = 0; j < len2; j++) {
                        sb.append(digit.charAt(sr.nextInt(digit.length())));
                    }
                }
                if (sr.nextInt(32) == 0) {
                    addSuffix(sb, taint);
                    skip = true;
                }
            }

        } else {
            sb.append('.');
            for (int i = 0; i < len; i++) 
                        sb.append(digit.charAt(sr.nextInt(digit.length())));
            if (sr.nextInt(32) == 0) {
                addSuffix(sb, taint);
                skip = true;
            }
        }

        if (!skip && sr.nextBoolean()) {
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
            if (sr.nextInt(32) == 0) {
                addSuffix(sb, taint);
                skip = true;
            }
        }

        sb.append(' ');
        Taint t = new Taint(taint, sb.length());
        t.clear(sb.length()-1);
        s = new String(sb.toString(), t);
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
                appendBlockComment(query, false);
                break;
            
            case QueryToken.Q_OTHER: 
                appendOther(query); 
                break;

            case QueryToken.Q_BOOLEAN:
                appendBoolean(query);
                break;

            case QueryToken.Q_QUOTELIT:
                if (quoteDelimitedStrings)
                    appendQuoteDelimitedLiteral(query);
                break;

            default: 
                throw new RuntimeException("switch"); 
        }
    }

    public void test() {
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
            } catch (SiegeBrkException s) {
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
                System.out.println("binaryFloatingPointNumbers: " +
                                   binaryFloatingPointNumbers);
                System.out.println("quoteDelimitedStrings: " +
                                   quoteDelimitedStrings);
                System.out.println("Taint " + 
                                 query.toString().@internal@taint().toString());

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
        OracleSqlValidatorTest qt;
        String logfile = "OracleSqlValidatorTest.log";
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
               System.out.println("Usage: java OracleSqlValidatorTest "
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
        qt = new OracleSqlValidatorTest(maxlen, sr, sr.nextBoolean(), 
                                        sr.nextBoolean());

        for (int i = 0; i < nrtest; i++) 
                qt.test();

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
     }
}
