/* Copyright 2009 Michael Dalton */
package jtaint;

public final class Policy 
{
        private boolean enabled = true;
        private boolean logAttack = true;
        private boolean logVuln = true;

        public boolean getEnabled() { return enabled; }
        public void setEnabled(boolean b) { enabled = b; }

        public boolean getLogAttack() { return logAttack; }
        public void setLogAttack(boolean b) { logAttack = b; }

        public boolean getLogVuln() { return logVuln; }
        public void setLogVuln(boolean b) { logVuln = b; }
}
