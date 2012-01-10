/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import jrockit.vm.StringMaker;

public class StringMakerTest
{
    private static String ops;
    private static int maxlen = 512;

    private SafeRandom sr;
    private TestUtil tu;

    public void test() {
        int maxArgs = sr.nextInt(StringMaker.MAX_APPEND_SEQUENCE_LENGTH);
        StringMaker sm;
        StringBuffer sb = new StringBuffer();

        if (sr.nextBoolean())
            sm = new StringMaker();
        else
            sm = new StringMaker(maxlen);

        int args = 0;

        while (args < maxArgs) {

            int nArgs = Math.min(maxArgs - args, 3);

            int i     = sr.nextInt();
            long l    = sr.nextLong();
            String s1 = tu.randString();
            String s2 = tu.randString();
            String s3 = tu.randString();
            Object o  = new Object();

            switch (nArgs) 
            {
                case 1:
                    switch (sr.nextInt(8)) {
                        case 0:
                            sm.append(i);
                            sb.append(i);
                            ops += "append(int) ";
                            break;

                        case 1:
                            boolean b = sr.nextBoolean();
                            sm.append(b);
                            sb.append(b);
                            ops += "append(boolean) ";
                            break;

                        case 2:
                            char c = (char) sr.nextInt(0xffff + 1);
                            sm.append(c);
                            sb.append(c);
                            ops += "append(char) ";
                            break;

                        case 3:
                            sm.append(l);
                            sb.append(l);
                            ops += "append(long) ";
                            break;

                        case 4:
                            float f = sr.nextFloat();
                            sm.append(f);
                            sb.append(f);
                            ops += "append(float) ";
                            break;


                        case 5:
                            double d = sr.nextDouble();
                            sm.append(d);
                            sb.append(d);
                            ops += "append(double) ";
                            break;

                        case 6:
                            sm.append(o);
                            sb.append(o);
                            ops += "append(Object) ";
                            break;

                        case 7:
                            sm.append(s1);
                            sb.append(s1);
                            ops += "append(String) ";
                            break;

                        default:
                            throw new RuntimeException("Switch error");
                    }
                    break;

                case 2:

                    switch (sr.nextInt(3)) {
                        case 0:
                            sm.append(s1, s2);
                            sb.append(s1);
                            sb.append(s2);
                            ops += "append(String, String) ";
                            break;

                        case 1:
                            sm.append(i, s1);
                            sb.append(i);
                            sb.append(s1);
                            ops += "append(int, String) ";
                            break;

                        case 2:
                            sm.append(s1, i);
                            sb.append(s1);
                            sb.append(i);
                            ops += "append(String, int) ";
                            break;

                        default:
                            throw new RuntimeException("Switch error");
                    }
                    break;


                case 3:
                    switch (sr.nextInt(4)) {
                        case 0:
                            sm.append(s1, s2, s3);
                            sb.append(s1);
                            sb.append(s2);
                            sb.append(s3);
                            ops += "append(String, String, String) ";
                            break;

                        case 1:
                            sm.append(s1, s2, i);
                            sb.append(s1);
                            sb.append(s2);
                            sb.append(i);
                            ops += "append(String, String, int) ";
                            break;

                        case 2:
                            sm.append(s1, l, s2);
                            sb.append(s1);
                            sb.append(l);
                            sb.append(s2);
                            ops += "append(String, long, String) ";
                            break;

                        case 3:
                            sm.append(s1, o, s2);
                            sb.append(s1);
                            sb.append(o);
                            sb.append(s2);
                            ops += "append(String, Object, String) ";
                            break;

                        default:
                            throw new RuntimeException("Switch error");
                    }
                    break;

                default:
                    throw new RuntimeException("Switch error");
            }

            args += nArgs;
        }

        String sbs = sb.toString();
        String sms = sm.toString();

        if (!sbs.equals(sms))
            throw new RuntimeException("Data corruption: sb " + sbs + 
                                       " sm " + sms);
        Taint sbt = sbs.@internal@taint();
        Taint smt = sms.@internal@taint();

        if ((sbt == null && smt != null) || (sbt != null && smt == null)
                || (sbt != null && !sbt.asBitSet().equals(smt.asBitSet())))
            throw new RuntimeException("Taint corruption: sb " + sbt +
                                       " sm " + smt);
    }

    public StringMakerTest(SafeRandom sr) {
        this.sr = sr;
        this.tu = new TestUtil(maxlen/StringMaker.MAX_APPEND_SEQUENCE_LENGTH,
                               sr, 16);
    }

    public static void main(String[] args)
    {    
        StringMakerTest smt;
        PrintStream ps = null;
        int nrtest = 1024, 
            ret = 0;
        SafeRandom sr;

        String logfile = "StringMakerTest.log";
        long seed = System.currentTimeMillis();

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
               System.out.println("Usage: java StringMakerTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofString] "
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

        /* else List l = new synchronedList(... ) */
        sr = new SafeRandom(seed);
        smt = new StringMakerTest(sr);

        for (int i = 0; i < nrtest; i++) {
            try {
                ops = "";
                smt.test();
            } catch (Throwable th) {
                System.out.println("FAILURE: " + th);
                th.printStackTrace();

                if (ops != null) 
                    System.out.println("ops: "  + ops);

                System.exit(-1);
            }
        }

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
     }
}
