/* Copyright 2009 Michael Dalton */
package jtaint;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class ExecUtil
{
    private static void abortExec(String cmd, String msg) 
        throws JTaintException
    {
        @StringBuilder@ sb = new @StringBuilder@();
        JTaintException e;

        sb.append(msg + "\n Command: " + cmd + "\n");
        e = new JTaintException(sb.toString(), Configuration.execWhitelist);

        if (Configuration.execPolicyLogAttack)
            Log.attack("Command Injection", e);
        throw e;
    }

    public static void validateExec(String cmd) throws JTaintException {
        try {
            StringTokenizer st = new StringTokenizer(cmd);
            int len = st.countTokens();
            String[] cmdArray = new String[len];

            for (int i = 0; i < len; i++)
                cmdArray[i] = st.nextToken();
            validateExec(cmdArray);
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
        } catch (Throwable e) {
            Log.error(e);
        }
    }

    public static void validateExec(List command) throws JTaintException {
        try {
            validateExec((String[]) command.toArray(
                         new String[command.size()]));
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
        } catch (Throwable e) {
            Log.error(e);
        }
    }

    public static void validateExec(String[] cmdArray) 
        throws JTaintException
    {
        try {
            if (cmdArray.length == 0 || cmdArray[0] == null
                 || !cmdArray[0].@internal@isTainted()
                 || !Configuration.execPolicyEnabled) return; 

            /* The first string is the actual application/program.
             * We do not validate the arguments (cmdArray[1...length-1]
             */
            abortExec(cmdArray[0], "untrusted program execution");
        } catch (JTaintException e) {
            if (!e.isWhitelisted())
                throw e;
        } catch (Throwable e) {
            Log.error(e);
        }
    }
}
