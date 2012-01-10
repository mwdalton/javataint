#!/bin/sh
#Copyright 2009 Michael Dalton

is14=`java -version 2>&1|grep 1.4`
is15=`java -version 2>&1|grep 1.5`

VER_FLAGS="-Xverify:all"

if [ -n "$is14" ]; then
    VER="1.4"
elif [ -n "$is15" ]; then
    VER="1.5"
else 
    VER="1.6"
fi
VER_FLAGS="-Xbootclasspath/p:/home/mwdalton/java-siegebrk/test/build/${VER}/mockobj/log ${VER_FLAGS}"
echo "Java Version ${VER} Flags ${VER_FLAGS}"
