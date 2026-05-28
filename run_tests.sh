#!/bin/bash
set -e

JAVAFX_LIB="lib/javafx-sdk-21.0.2/lib"
JUNIT_JAR="lib/junit-platform-console-standalone-1.10.2.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"

if [ ! -f "$JUNIT_JAR" ]; then
    echo "Downloading JUnit 5 standalone..."
    mkdir -p lib
    curl -sL -o "$JUNIT_JAR" "$JUNIT_URL"
fi

echo "Compiling main sources..."
rm -rf out
mkdir -p out
javac --module-path "$JAVAFX_LIB" --add-modules javafx.controls \
      -d out -sourcepath src/main/java \
      $(find src/main/java -name "*.java")

echo "Compiling test sources..."
mkdir -p out/test
javac -cp "out:$JUNIT_JAR" \
      -d out/test \
      $(find src/test/java -name "*.java")

echo "Running tests..."
java -jar "$JUNIT_JAR" \
     --classpath "out:out/test" \
     --scan-classpath
