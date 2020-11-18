#!/bin/sh
### ====================================================================== ###
##                                                                          ##
##  DicoogleServer.sh  Launch Script                                        ##
##                                                                          ##
### ====================================================================== ###

### $Id$ ###

DIRNAME="`dirname "$0"`"

# OS specific support (must be 'true' or 'false').
cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi


# Setup the JVM
if [ "x$JAVA_HOME" != "x" ]; then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA="java"
fi


# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JAVA=`cygpath --path --windows "$JAVA"`
    CP=`cygpath --path --windows "$CP"`
fi

# Execute the JVM
exec $JAVA $JAVA_OPTS -jar dicoogle.jar -s
