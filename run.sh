#!/bin/bash
# Build and run Dungeon KUrawler
set -e

JAVAFX_LIB="lib/javafx-sdk-21.0.2/lib"
GSON_JAR="lib/gson-2.10.1.jar"

# Auto-download Gson if missing
if [ ! -f "$GSON_JAR" ]; then
  echo "Downloading Gson..."
  curl -sL "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -o "$GSON_JAR"
fi

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
