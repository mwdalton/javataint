#!/bin/bash
#Copyright 2009 Michael Dalton
fail=0
i=0
MAX_ITER=1000
source version.sh

while [ $fail -ne 1 -a $i -lt $MAX_ITER ] 
do
        java -Xbootclasspath/p:/home/mwdalton/java-siegebrk/test/lib/sb-sql-mockobj-bootlib${VER}.jar -cp /home/mwdalton/java-siegebrk/test/build/${VER}/common:/home/mwdalton/java-siegebrk/test/build/${VER}/tests:/home/mwdalton/java-siegebrk/test/build/${VER}/mockobj/sql $VER_FLAGS com.siegebrk.DerbySqlTest -n 1024 -l 1024
        if [ $? -ne 0 ]
        then
                echo "FAILURE"
                fail=1
        else
                let "i++"
                echo "success: $i iterations"
        fi
done
