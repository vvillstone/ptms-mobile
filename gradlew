#!/bin/sh

##############################################################################
#
#   Gradle start up script for POSIX
#
##############################################################################

# Resolve links: $0 may be a link
app_path=$0
while [ -h "$app_path" ]; do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*) app_path=$link ;;
      *) app_path=$( cd -P "$( dirname "$app_path" )" && pwd )/$link ;;
    esac
done

APP_HOME=$( cd -P "$( dirname "$app_path" )" && pwd -P )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Check that Java exists
if ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    exit 1
fi

exec "$JAVACMD" \
    -Xmx64m -Xms64m \
    -Dorg.gradle.appname=gradlew \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
