#!/bin/bash
#Copyright 2009 Michael Dalton

source version.sh

for file in `ls run*.sh`
do
        if [ -z "$is_jrockit" ] && [ "$file" == "run_jrockit.sh" ]; then
                continue;
        fi

        echo "Running $file"
        fname="test.${file}.log"
        ./${file} >& $fname
        grep -H -i 'failure' $fname   && { echo "$file failed"; }
        grep -H -i 'exception' $fname && { echo "$file failed"; }
done
