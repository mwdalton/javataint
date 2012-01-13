#!/bin/sh
# Copyright 2009 Michael Dalton
# This script initializes the shell environment variables to use JavaTaint as
# the default Java installation. This script should be added to your login
# scripts, by adding the line 'source @install@/scripts/jt-env.sh'
# to the appropriate script for your shell (e.g., .bashrc)

export JAVA_HOME="@install@/jt_java"
export JRE_HOME="@install@/jt_java"
export PATH="@install@/jt_java/bin:@install@/jt_java/jre/bin:$PATH"
