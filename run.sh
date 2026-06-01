#!/bin/bash
# Build and run Dungeon KUrawler
set -e

JAVAFX_LIB="lib/javafx-sdk-21.0.2/lib"
GSON_JAR="lib/gson-2.10.1.jar"

echo "Compiling..."
rm -rf out
mkdir -p out
javac -encoding UTF-8 \
      --module-path "$JAVAFX_LIB" \
      --add-modules javafx.controls,javafx.media \
      -cp "$GSON_JAR" \
      -d out -sourcepath src/main/java \
      $(find src/main/java -name "*.java")

# Copy assets to classpath
cp -r src/main/resources/assets out/assets

echo "Launching Dungeon KUrawler..."
java --module-path "$JAVAFX_LIB" \
     --add-modules javafx.controls,javafx.media \
     -cp "out:$GSON_JAR" com.kurawler.Main
