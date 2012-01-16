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
