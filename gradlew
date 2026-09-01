#!/bin/sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*->\(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='\" -Xmx64m -Xms64m "'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*" >&2
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
IS_CYGWIN=false
IS_MSYS=false
IS_MINGW=false
IS_Nnative=false
case "`uname`" in
  CYGWIN* )
    IS_CYGWIN=true
    ;;
  Darwin* )
    IS_NATIVE=true
    ;;
  MSYS* )
    IS_MSYS=true
    ;;
  MINGW* )
    IS_MINGW=true
    ;;
ESAC

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if ! "$cygpath" -m .> /dev/null 2>&1 ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD_LIMIT" != "unlimited" ] ; then
            MAX_FD=$MAX_FD_LIMIT
        fi
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$CYGWIN" = "true" ] -o [ "$MSYS" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --windows "$JAVACMD"`
fi

# We build the pattern for arguments to be converted via cygpath
PATTERN="(^($|/)|(^|/)([^()]|\(([^()]|\([^()]*\))*\))*$)"
# Add a user-defined CLASSPATH variable if needed
if [ -n "$CLASSPATH" ] ; then
    CLASSPATH=$CLASSPATH
fi

# Determine the OS arch for the wrapper JAR to download
OSARCH=`uname -m`
case "$OSARCH" in
  x86_64 )
    OSARCH="x86_64"
    ;;
  i686 )
    OSARCH="i686"
    ;;
  aarch64 )
    OSARCH="aarch64"
    ;;
esac

# For Mingw, ensure paths are in MSYS format before anything is touched
if $IS_MINGW ; then
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME="`( cd "$JAVA_HOME" && pwd -W )"
    [ -n "$GRADLE_USER_HOME" ] &&
        GRADLE_USER_HOME="`( cd "$GRADLE_USER_HOME" && pwd -W )"
fi

# For CYGWIN, switch paths to Windows format before running java
if $IS_CYGWIN ; then
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME="`cygpath --windows "$JAVA_HOME"`"
    [ -n "$GRADLE_USER_HOME" ] &&
        GRADLE_USER_HOME="`cygpath --windows "$GRADLE_USER_HOME"`"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
