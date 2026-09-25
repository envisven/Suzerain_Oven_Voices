#!/bin/sh
# Sourced by build/run/test. No downloads or modifications to the JDK.
if [ -n "${JAVA_HOME:-}" ]; then
  JDK_BIN="$JAVA_HOME/bin"
elif [ -x "/Users/nistaltothantal/Library/Java/JavaVirtualMachines/liberica-full-25.0.4.1/bin/javac" ]; then
  JDK_BIN="/Users/nistaltothantal/Library/Java/JavaVirtualMachines/liberica-full-25.0.4.1/bin"
elif command -v javac >/dev/null 2>&1; then
  JDK_BIN=$(dirname "$(command -v javac)")
else
  echo 'Set JAVA_HOME to your existing Liberica JDK 25 FULL installation.' >&2
  exit 1
fi
if [ ! -x "$JDK_BIN/java" ] || [ ! -x "$JDK_BIN/javac" ]; then
  echo 'JAVA_HOME must point to a full JDK with java and javac.' >&2
  exit 1
fi
if ! "$JDK_BIN/java" --list-modules | grep -q '^javafx.controls@'; then
  echo 'JavaFX is missing. Select Liberica JDK 25 FULL (not the standard JDK).' >&2
  exit 1
fi

