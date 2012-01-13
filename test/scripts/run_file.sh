#!/bin/bash
#Copyright 2009 Michael Dalton

fail=0
i=0
MAX_ITER=1000
base=/home/mwdalton/java-taint/test
source version.sh

ln -nsf ${base}/file-test/sym ${base}/file-test/sym/t1/t2/good5

while [ $fail -ne 1 -a $i -lt $MAX_ITER ] 
do
        java $VER_FLAGS -cp /home/mwdalton/java-taint/test/build/${VER}/common:/home/mwdalton/java-taint/test/build/${VER}/tests -Dtest.rel.nosym=${base}/file-test/nosym/ -Dtest.sym=${base}/file-test/sym/ -Dtest.abs.nosym=/foo/ jtaint.FileTest -l 1024 -n 1000
        if [ $? -ne 0 ]
        then
                echo "FAILURE"
                fail=1
        else
                let "i++"
                echo "success: $i iterations"
        fi
done
