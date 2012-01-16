#!/bin/csh
# Copyright 2009-2012 Michael Dalton
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# This script initializes the shell environment variables to use JavaTaint as
# the default Java installation. This script should be added to your login
# scripts, by adding the line 'source @install@/scripts/jt-env.csh'
# to the appropriate script for your shell (e.g., .cshrc)

setenv JAVA_HOME "@install@/jt_java"
setenv JRE_HOME "@install@/jt_java"
setenv PATH "@install@/jt_java/bin:@install@/jt_java/jre/bin:$PATH"
