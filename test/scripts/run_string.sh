#!/bin/bash
#Copyright 2009 Michael Dalton

fail=0
i=0
MAX_ITER=1000
source version.sh

while [ $fail -ne 1 -a $i -lt $MAX_ITER ] 
do
        java $VER_FLAGS -Xbootclasspath/p:/home/mwdalton/java-taint/test/build/${VER}/mockobj/string:/home/mwdalton/java-taint/test/build/${VER}/common -cp /home/mwdalton/java-taint/test/build/${VER}/tests jtaint.StringTest -l 512 -n 1024    -t 8
        if [ $? -ne 0 ]
        then
                echo "FAILURE"
                fail=1
        else
                let "i++"
                echo "success: $i iterations"
        fi
done
