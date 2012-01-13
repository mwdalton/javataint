#!/bin/csh
# Copyright 2009 Michael Dalton
# This script initializes the shell environment variables to use JavaTaint as
# the default Java installation. This script should be added to your login
# scripts, by adding the line 'source @install@/scripts/jt-env.csh'
# to the appropriate script for your shell (e.g., .cshrc)

setenv JAVA_HOME "@install@/jt_java"
setenv JRE_HOME "@install@/jt_java"
setenv PATH "@install@/jt_java/bin:@install@/jt_java/jre/bin:$PATH"
