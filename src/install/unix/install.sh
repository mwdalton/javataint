#!/bin/sh
# Copyright 2009 Michael Dalton

#Verify the Java JRE or JDK installation is sane, and find java/rt.jar
verify_java() {
    if [ ! -d "$1" ]; then
        echo "$1 is not a valid directory" >&2
        exit 1
    fi

    if [ -x "${1}/jre/bin/java" ]; then
        JAVA_BIN="jre/bin/java"
    elif [ -x "${1}/bin/java" ]; then
        JAVA_BIN="bin/java"
    else
        echo "Could not find java - expected at ${1}/jre/bin/java" >&2
        exit 1
    fi

    if [ -r "${1}/jre/lib/rt.jar" ]; then
        RT_JAR="jre/lib/rt.jar"
    elif [ -r "${1}/lib/rt.jar" ]; then
        RT_JAR="lib/rt.jar"
    else
        echo "Could not read rt.jar  - expected at ${1}/jre/lib/rt.jar" >&2
        exit 1;
    fi
}

#Find the absolute path of the file referred to by a symlink, recursively
#following any intermediate symlinks
resolve_symlink() {
    link=$1
    i=0
    last=""

    until [ -z "${link}" ]; do
      dir=`dirname ${link}`

      if [ -n "${dir}" ]; then
          cd "${dir}" || \
            { echo "Unable to access ${link} (skipping)" >&2; echo ""; return; }
      fi

      link=`basename "${link}"`

      last="`pwd -P`/${link}"
      link=`find "${link}" -maxdepth 0 -printf "%l"` 

      if [ $? -ne 0 ]; then
          echo "Unable to resolve symbolic link ${link}" >&2
          echo ""
          return
      fi

      if [ $i -ge 1024 ]; then
          echo "Infinite loop detected in symbolic link ${last} (skipping)" >&2
          echo ""
          return 
      fi
      i=$((i + 1))
   done
   echo "$last"
}

#Copy source directory to destination directory
#Any relative symbolic links encountered will be preserved if they refer to
#files within ${base}, and converted to absolute links otherwise.
clone_files() {
    for file in `ls -A ${1}`
    do

        if [ -h "${1}/${file}" ]; then
            abslink=`resolve_symlink "${1}/${file}"`

            #if this is a relative symbolic link within $base, then allow it
            relbase=`perl -e "if (index('${abslink}','${base}') == 0) { print 'base'}"`
            if [ -n "$relbase" ]; then
                cp -df "${1}/${file}" "${2}/${file}" ||  echo \
                    "Unable to copy ${1}/${file} to ${2}/${file} (skipping)" >&2

            else #otherwise, use the absolute reference
                ln -nsf "${abslink}" "${2}/${file}" || \
                    echo "Unable to create link ${2}/${file} (skipping)" >&2
            fi
        elif [ -d "${1}/${file}" ]; then
            mkdir -p "${2}/${file}"
            clone_files "${1}/${file}" "${2}/${file}"

        else
            cp -pf "${1}/${file}" "${2}/${file}" || \
              echo "Unable to copy ${1}/${file} to ${2}/${file} (skipping)" >&2
        fi
    done
}

#compute absolute path of $0
if [ -n `dirname "$0"` ]; then
    cd "`dirname "$0"`"
fi
scriptdir=`pwd -P`

echo "Welcome to SiegeBreaker @version@"
base=
install=

while [ $# -gt 0 ]; do
    case "$1" in 
        -b) base="$2"; shift;;
        -i) install="$2"; shift;;
         *) echo "Usage: $0 [-b <java home directory>] [-i <siegebreaker install directory>]"; exit 1;;
    esac
    shift;
done
              

if [ -z "${base}" ]; then
    echo "Where is your Java Runtime Environment (JRE) or Java Development Kit (JDK) installed?"

    default=
    if [ -n "${JAVA_HOME}" ]; then
        default="${JAVA_HOME}"
    elif [ -n "${JRE_HOME}" ]; then
        default="${JRE_HOME}"
    fi

    if [ -n "${default}" ]; then
        echo -n "[${default}] "
    fi
    read base
fi

if [ -z "$base" ]; then
    base="${default}"
fi

base=`resolve_symlink "$base"`
verify_java "$base"

if [ -z "${install}" ]; then
    echo "Where would you like to install SiegeBreaker?"
    echo -n "[/usr/local/siegebrk] "
    read install
fi

if [ -z "$install" ]; then
    install="/usr/local/siegebrk"
fi

mkdir -p "${install}/sb_java" || { echo "Could not create ${install}" >&2 ; \
                                   exit 1; }

echo "Copying Java installation from ${base} to ${install}"
clone_files "${base}" "${install}/sb_java"
echo "Installing SiegeBreaker in ${install}"
export JAVA_HOME=$base
export JRE_HOME=$base
outname=out$$.jar
trap "rm -f ${scriptdir}/${outname}; exit 1" HUP INT QUIT TERM

"${base}/${JAVA_BIN}" -jar "${scriptdir}/sb-bootstrap.jar" \
    -i "${install}" \
    -r14 "${scriptdir}/sb-rt1.4.jar" -r15 "${scriptdir}/sb-rt1.5.jar" \
    -j "${scriptdir}/sb-bootlib.jar" || exit 1

"${base}/${JAVA_BIN}" -jar "${scriptdir}/sb-jarmerger.jar" \
    "${base}/${RT_JAR}" "${scriptdir}/sb-bootlib.jar" \
    "${scriptdir}/${outname}" || exit 1

mv "${scriptdir}/${outname}" "${install}/sb_java/${RT_JAR}"

cp "${scriptdir}/sb.dtd" "${install}"
sed -e "s|@install@|${install}|g" <  "${scriptdir}/sb-config.xml.example" \
        > "${install}/sb-config.xml.example"
sed -e "s|@install@|${install}|g" <  "${scriptdir}/log4j.properties.example" \
        > "${install}/log4j.properties.example"
sed -e "s|@install@|${install}|g" < "${scriptdir}/sb-env.sh"  \
        > "${install}/sb-env.sh"
sed -e "s|@install@|${install}|g" < "${scriptdir}/sb-env.csh"  \
        > "${install}/sb-env.csh"

#Unfortunately instrumenting rt.jar has various side effects. Certain 
#files are generated at jdk install or build-time from rt.jar for caching 
#and performance purposes. We have instrumented rt.jar, and thus these files 
#are now stale, and must be removed as they cannot be easily regenerated.
#Relevant files are classlist, meta-index, classes.jsa (used for class data
#sharing), and ct.sym (a cache of the symbols in rt.jar used by the javac 
#compiler). Fortunately the JVM and javac respectively work fine without these
#files.

removefiles="classlist meta-index classes.jsa ct.sym"
for file in `echo $removefiles`
do
    find "${install}" -name "$file" -exec rm -f '{}' \;
done

echo "SiegeBreaker installation complete"
