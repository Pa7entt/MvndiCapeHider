#!/usr/bin/env bash
# Standalone build for MvndiCapeHider — uses ECJ (Eclipse compiler) so only a
# Java 21+ JRE is required (no Gradle, no javac). Produces MvndiCapeHider-<version>.jar.
# A Gradle build is also provided for normal development (build.gradle.kts);
# this script exists for minimal environments.
set -euo pipefail
cd "$(dirname "$0")"

VERSION="2.0.1"
NAME="MvndiCapeHider"
DESCRIPTION="Hide all capes via Paper's native SkinParts API — no external dependencies"
API_VERSION="1.21"
ECJ_VERSION="3.36.0"
PAPER_VERSION="1.21.11-R0.1-SNAPSHOT"
PAPER_REPO="https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api"

mkdir -p libs build/classes

if [ ! -f "libs/ecj.jar" ]; then
  echo ">> downloading ECJ ${ECJ_VERSION}..."
  curl -fsSL --retry 5 --retry-all-errors --retry-delay 2 -o libs/ecj.jar \
    "https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/${ECJ_VERSION}/ecj-${ECJ_VERSION}.jar"
fi

if ! ls libs/paper-api*.jar >/dev/null 2>&1; then
  echo ">> resolving paper-api ${PAPER_VERSION}..."
  # Nexus resolves -SNAPSHOT.jar filenames to the latest snapshot build.
  if ! curl -fsSL --retry 5 --retry-all-errors --retry-delay 2 -o libs/paper-api.jar \
      "${PAPER_REPO}/${PAPER_VERSION}/paper-api-${PAPER_VERSION}.jar"; then
    # Fallback: parse the snapshot metadata for the exact main-jar version
    # (skipping javadoc/sources classifiers).
    METADATA=$(curl -fsSL --retry 5 --retry-all-errors --retry-delay 2 \
      "${PAPER_REPO}/${PAPER_VERSION}/maven-metadata.xml")
    JAR_VERSION=$(printf '%s' "$METADATA" | tr '<' '\n' \
      | awk '/^snapshotVersion>/{incl=1;jar=0} /^classifier>/{incl=0} /^extension>jar$/{jar=incl} jar && /^value>/{sub(/^value>/,"");print;exit}')
    if [ -z "$JAR_VERSION" ]; then
      echo "!! could not resolve paper-api; download a paper-api jar into libs/ manually" >&2
      exit 1
    fi
    curl -fsSL --retry 5 --retry-all-errors --retry-delay 2 -o "libs/paper-api-${JAR_VERSION}.jar" \
      "${PAPER_REPO}/${PAPER_VERSION}/${JAR_VERSION}.jar"
  fi
fi

PAPER=$(ls libs/paper-api*.jar | head -n1)

# Full compile classpath: paper-api + its published API dependencies
# (adventure, jspecify, guava, gson, snakeyaml, ...). build.sh fetches them
# from Maven Central the first time it runs.
DEPS=(
  "https://repo1.maven.org/maven2/net/kyori/adventure-api/4.26.1/adventure-api-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/adventure-key/4.26.1/adventure-key-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/adventure-text-minimessage/4.26.1/adventure-text-minimessage-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-gson/4.26.1/adventure-text-serializer-gson-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-legacy/4.26.1/adventure-text-serializer-legacy-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-plain/4.26.1/adventure-text-serializer-plain-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/adventure-text-logger-slf4j/4.26.1/adventure-text-logger-slf4j-4.26.1.jar"
  "https://repo1.maven.org/maven2/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar"
  "https://repo1.maven.org/maven2/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar"
  "https://repo1.maven.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"
  "https://repo1.maven.org/maven2/com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.jar"
  "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar"
  "https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar"
  "https://repo1.maven.org/maven2/org/joml/joml/1.10.8/joml-1.10.8.jar"
  "https://repo1.maven.org/maven2/it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15.jar"
  "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.24.1/log4j-api-2.24.1.jar"
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar"
  "https://repo1.maven.org/maven2/org/checkerframework/checker-qual/3.49.2/checker-qual-3.49.2.jar"
  "https://repo1.maven.org/maven2/org/jetbrains/annotations/26.0.1/annotations-26.0.1.jar"
)
# bungeecord-chat is only published on the PaperMC repo (needed to resolve the
# legacy BaseComponent overloads of CommandSender#sendMessage).
DEPS_PAPERMC=(
  "https://repo.papermc.io/repository/maven-public/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"
)
for url in "${DEPS[@]}" "${DEPS_PAPERMC[@]}"; do
  file="libs/$(basename "$url")"
  if [ ! -f "$file" ]; then
    echo ">> downloading $(basename "$file")..."
    curl -fsSL --retry 5 --retry-all-errors --retry-delay 2 -o "$file" "$url" || true
  fi
done

CP=$(ls libs/*.jar | grep -v ecj.jar | tr '\n' ':')

echo ">> compiling against $(basename "$PAPER") + API deps..."
find src/main/java -name '*.java' > build/sources.txt
java -jar libs/ecj.jar -encoding UTF-8 -source 21 -target 21 -proc:none -nowarn \
  -cp "$CP" -d build/classes @build/sources.txt

echo ">> packaging..."
rm -rf build/jar
mkdir -p build/jar/META-INF
cp -r build/classes/. build/jar/
# Same substitutions the Gradle build does via processResources expand()
sed -e "s/\$name/${NAME}/g" \
    -e "s/\$version/${VERSION}/g" \
    -e "s/\$description/${DESCRIPTION}/g" \
    -e "s/\$apiVersion/${API_VERSION}/g" \
    src/main/resources/plugin.yml > build/jar/plugin.yml
printf 'Manifest-Version: 1.0\nCreated-By: MvndiCapeHider standalone build\n' \
  > build/jar/META-INF/MANIFEST.MF

(cd build/jar && zip -qr "../../${NAME}-${VERSION}.jar" .)

echo ">> built $(pwd)/${NAME}-${VERSION}.jar"
unzip -l "${NAME}-${VERSION}.jar"
