#!/bin/bash
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

source version.sh

for file in `ls run*.sh`
do
        echo "Running $file"
        fname="test.${file}.log"
        ./${file} >& $fname
        grep -H -i 'failure' $fname   && { echo "$file failed"; }
        grep -H -i 'exception' $fname && { echo "$file failed"; }
done
