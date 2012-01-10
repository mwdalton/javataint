#!/bin/sh
# Copyright 2009 Michael Dalton
# This script initializes the shell environment variables to use SiegeBreaker as
# the default Java installation. This script should be added to your login
# scripts, by adding the line 'source @install@/scripts/sb-env.sh'
# to the appropriate script for your shell (e.g., .bashrc)

export JAVA_HOME="@install@/sb_java"
export JRE_HOME="@install@/sb_java"
export PATH="@install@/sb_java/bin:@install@/sb_java/jre/bin:$PATH"
