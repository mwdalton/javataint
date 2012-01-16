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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class ExecTest
{
    private final int maxlen;
    private final Random r;
    private boolean exception;
    private static String currentCommand;
    private static String currentOp;
    private static String[] currentCommandArray;
    private static List currentCommandList;

    public ExecTest(int maxlen, Random r) {
        this.maxlen = maxlen;
        this.r = r;
        clearException();
    }

    private void clearException() { exception = false; }

    private void setException() { exception = true; }

    private boolean pendingException() { return exception; }

    private String randString(int taintP, int slen) {
        int len = 1 + r.nextInt(slen);
        @StringBuilder@ sb = new @StringBuilder@(len);
        BitSet b = new BitSet();

        for (int i = 0; i < len; i++) {
            int c, clen;
            boolean taint = r.nextInt(taintP) == 0;
            int oldlen = sb.length();

            c = r.nextInt(256);

            if (c == '\0') /* Don't prematurely null terminate unless tainted */
                taint = true;

            sb.append((char) c);

            b.set(oldlen, taint);
        }

        return new String(sb.toString(), new Taint(b, sb.length()));
    }

    private String randProgram() {
        String s, prefix = "/javataint/";
        BitSet b = new BitSet();
        StringTokenizer st;

        for (int i = 0; i < prefix.length(); i++)
            if (r.nextInt(64) == 0) 
                b.set(i);

        
        s = new String(prefix, new Taint(b, prefix.length())) + 
                randString(64,64);
        st = new StringTokenizer(s);

        return st.nextToken(); 
    }

    private String randCommand() {
        int len = 1 + r.nextInt(255);
        @StringBuilder@ sb = new @StringBuilder@(len);
        String prg = randProgram();

        if (prg.@internal@isTainted())
            setException();

        sb.append(prg);

        while (sb.length() < len) {
            int wslen = 1 + r.nextInt(7);
            for (int i = 0; i < wslen; i++)
                sb.append(' ');
            sb.append(randString(16,16));
        }

        return sb.toString();
    }

    private String[] randStringArray() {
        String[] s = new String[1 + r.nextInt(7)];

        for (int i = 0; i < s.length; i++)
            s[i] = randString(16, 16);
        return s;
    }

    private String[] randCommandArray() {
        String[] s = new String[1 + r.nextInt(7)];

        for (int i = 0; i < s.length; i++) {
            if (i == 0) {
                s[i] = randProgram();
                if (s[i].@internal@isTainted())
                    setException();
            } else
                s[i] = randString(16, 16);
        }
        return s;
    }

    private List randCommandList() {
        int len = 1 + r.nextInt(7);
        List list = new ArrayList();

        for (int i = 0; i < len; i++) {
            if (i == 0) {
                String prg = randProgram();
                list.add(prg);
                if (prg.@internal@isTainted())
                    setException();
            } else
                list.add(randString(16,16));
        }

        return list;
    }

    private void testRuntime() throws IOException {
        String[] envp = { "x=y", "foobar=baz" };
        File cwd = new File(System.getProperty("user.dir"));

        envp[1] = StringUtil.toTainted(envp[1]);

        switch(r.nextInt(6)) {
            case 0: /* exec(String cmd) */
                currentCommand = randCommand();
                currentOp = "Runtime.exec(String cmd)";
                Runtime.getRuntime().exec(currentCommand);
                break;

            case 1: /* exec(String cmd, String[] envp) */
                currentCommand = randCommand();
                currentOp = "Runtime.exec(String cmd, String[] envp)";
                Runtime.getRuntime().exec(currentCommand, envp);
                break;

            case 2: /* exec(String cmd, String[] envp, File dir) */
                currentCommand = randCommand();
                currentOp = "Runtime.exec(String cmd, String[] envp, File dir)";
                Runtime.getRuntime().exec(currentCommand, envp, cwd);
                break;

            case 3: /* exec(String[] cmdArray) */
                currentCommandArray = randCommandArray();
                currentOp = "Runtime.exec(String cmdArray)";
                Runtime.getRuntime().exec(currentCommandArray);
                break;

            case 4: /* exec(String[] cmdArray, String[] envp) */
                currentCommandArray = randCommandArray();
                currentOp = "Runtime.exec(String cmdArray, String[] envp)";
                Runtime.getRuntime().exec(currentCommandArray, envp);
                break;

            case 5: /* exec(String[] cmdArray, String[] envp, File dir) */
                currentCommandArray = randCommandArray();
                currentOp = "Runtime.exec(String cmdArray, String[] envp, "
                    + "File dir)";
                Runtime.getRuntime().exec(currentCommandArray, envp, cwd);
                break;

            default:
                throw new RuntimeException("switch");
        }
    }

    private void testValid() throws IOException, InterruptedException {
        String cmd = randCommand();
        cmd = "/bin/echo " + cmd;

        String[] cmdArray = randCommandArray();
        cmdArray[0] = "/bin/echo";

        List cmdList = randCommandList();
        cmdList.add(0,"/bin/echo");

        clearException();

        Runtime.getRuntime().exec(cmd).waitFor();
        Runtime.getRuntime().exec(cmdArray).waitFor();

        //[ifJava5+]
        new ProcessBuilder(cmdArray).start().waitFor();
        new ProcessBuilder(cmdList).start().waitFor();
        new ProcessBuilder("/foo").command(cmdArray).start().waitFor();
        new ProcessBuilder("/foo").command(cmdList).start().waitFor();
        //[fiJava5+]
    }

    //[ifJava5+]
    private void testProcessBuilder() throws IOException {
        ProcessBuilder pb;

        switch(r.nextInt(4)) {
            case 0: // ProcessBuilder(String[] cmdArray) 
                currentCommandArray = randCommandArray();
                currentOp = "ProcessBuilder(String[] cmdArray)";
                pb = new ProcessBuilder(currentCommandArray);
                break;

            case 1: // ProcessBuilder(List<String> cmdList) 
                currentCommandList = randCommandList();
                currentOp = "ProcessBuilder(List<String> cmdList)";
                pb = new ProcessBuilder(currentCommandList);
                break;

            case 2: // command(String[] cmdArray) 
                currentCommandArray = randCommandArray();
                currentOp = "ProcessBuilder.command(String[] cmdArray)";
                pb = new ProcessBuilder("/bin/ls");
                pb.command(currentCommandArray);
                break;

            case 3: // command(List<String> cmdList) 
                currentCommandList = randCommandList();
                currentOp = "ProcessBuilder.command(List<String> cmdArray)";
                pb = new ProcessBuilder("/bin/ls");
                pb.command(currentCommandList);
                break;

            default:
                throw new RuntimeException("switch");
        }
        pb.start();
    }
    //[fiJava5+]

    private void test() throws IOException {
        currentCommand = null;
        currentCommandArray = null;
        currentCommandList = null;
        currentOp = null;

        //[ifJava4]
        testRuntime();
        //[fiJava4]
        //[ifJava5+]
        if (r.nextBoolean())
            testRuntime();
        else
            testProcessBuilder();
        //[fiJava5+]
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        int maxlen = 65536; 
        int nrtest = 16384;

        Random r;
        ExecTest et;
        String logfile = "ExecTest.log";
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
               System.out.println("Usage: java ExecTest "
                       + "[-s randomSeed] "
                       + "[-l maximumLengthofCommand] "
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

        r = new Random(seed);
        et = new ExecTest(maxlen, r);

        try { 
            et.testValid();
        } catch(Exception e) {
            throw new RuntimeException("testValid: unexpected exception", e);
        }

        for (int i = 0; i < nrtest; i++) {
            Exception e = null;
            et.clearException();

            try {
                et.test();
                e = new RuntimeException("Expected exception - got none");
            } catch (IOException o) {
                if (et.pendingException())
                    e = new RuntimeException("Expected exception - got IO", o);
            } catch (JTaintException s) {
                if (!et.pendingException())
                    e = new RuntimeException("Unexpected JTaintException", s);
            } catch (Throwable x) {
                e = new RuntimeException("Unexpected other exception", x);
            }

            if (e != null) {
                System.out.println("FAILURE -- Op " + currentOp);

                if (currentCommand != null) {
                    System.out.println("Current command: "  + currentCommand);
                    System.out.println("Taint: " + 
                                        currentCommand.@internal@taint());
                } 
                
                if (currentCommandArray != null) {
                    for (int j = 0; j < currentCommandArray.length; j++)  {
                        System.out.println("CommandArray[" + j + "]: " +
                                currentCommandArray[j]);
                        System.out.println("Taint: " + 
                                currentCommandArray[j].@internal@taint());
                    }
                }

                if (currentCommandList != null) {
                    for (int j = 0; j < currentCommandList.size(); j++) {
                        String s = (String) currentCommandList.get(j);
                        System.out.println("CommandList[" + j + "]: " + s);
                        System.out.println("Taint: " + s.@internal@taint()); 
                    }
                }

                e.printStackTrace();
                System.exit(-1);
            }
        }

        if (Log.hasError() || Log.hasWarning() || Log.hasVuln()) {
            System.out.println("Error or Warning encountered -- check logs");
            System.exit(-1);
        }
    }
}
