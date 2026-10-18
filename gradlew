#!/bin/bash

##############################################################################
##
## Punch Reminder Gradle start-up script for UN*X
##
##############################################################################

# Resolve the directory this script lives in.
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Locate a Java runtime.
if [ -z "$JAVACMD" ] ; then
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD=`command -v java 2>/dev/null || echo /usr/bin/java`
    fi
fi
if [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    exit 1
fi

# Gradle distribution to use.
GRADLE_VERSION=8.2
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DISTRIBUTION_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
DISTRIBUTION_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
WRAPPER_DIST="$GRADLE_USER_HOME/wrapper/dists"
DISTRIBUTION_HOME="$WRAPPER_DIST/gradle-${GRADLE_VERSION}"

# Download and unpack the distribution if it is not already present.
if [ ! -x "$DISTRIBUTION_HOME/bin/gradle" ]; then
    echo "Downloading Gradle $GRADLE_VERSION (one time)..."
    mkdir -p "$WRAPPER_DIST"
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 --connect-timeout 20 -o "$WRAPPER_DIST/$DISTRIBUTION_ZIP.part" "$DISTRIBUTION_URL" \
            && mv "$WRAPPER_DIST/$DISTRIBUTION_ZIP.part" "$WRAPPER_DIST/$DISTRIBUTION_ZIP"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$WRAPPER_DIST/$DISTRIBUTION_ZIP.part" "$DISTRIBUTION_URL" \
            && mv "$WRAPPER_DIST/$DISTRIBUTION_ZIP.part" "$WRAPPER_DIST/$DISTRIBUTION_ZIP"
    else
        echo "ERROR: neither curl nor wget is available to download Gradle."
        exit 1
    fi
    unzip -q -o "$WRAPPER_DIST/$DISTRIBUTION_ZIP" -d "$WRAPPER_DIST"
    rm -f "$WRAPPER_DIST/$DISTRIBUTION_ZIP"
fi

export GRADLE_USER_HOME
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

exec "$DISTRIBUTION_HOME/bin/gradle" \
    --gradle-user-home "$GRADLE_USER_HOME" \
    "$@"
