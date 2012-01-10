#!/bin/sh
#Copyright 2009 Michael Dalton

is14=`java -version 2>&1|grep 1.4`
is15=`java -version 2>&1|grep 1.5`

is_jrockit=`java -version 2>&1|grep -i jrockit`
VER_FLAGS="-Xverify:all"

#    This option apparently doesn't work in JRockit right now, but would be
#    relevant if it did
#    rnd=$((RANDOM % 2))
#    if [ -n "$is_jrockit" ] && [ $rnd -eq 0 ]; then
#        VER_FLAGS="${VER_FLAGS} -XX:+UseStringCache"

if [ -n "$is14" ]; then
    VER="1.4"
elif [ -n "$is15" ]; then
    VER="1.5"
else 
    VER="1.6"

    if [ -z "$is_jrockit" ]; then
        VER_FLAGS="${VER_FLAGS} -XX:-FailOverToOldVerifier"
    fi
fi
VER_FLAGS="-Xbootclasspath/p:/home/mwdalton/java-siegebrk/test/build/${VER}/mockobj/log ${VER_FLAGS}"
echo "Java Version ${VER} Flags ${VER_FLAGS}"
