#!/bin/sh
set -eu
MVN_VERSION=3.9.9
BASE="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/apache-maven-$MVN_VERSION"
MVN="$BASE/apache-maven-$MVN_VERSION/bin/mvn"
if [ ! -x "$MVN" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/apache-maven-$MVN_VERSION-bin.zip"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$URL" -o "$ZIP"; else wget -q "$URL" -O "$ZIP"; fi
  (cd "$BASE" && unzip -q "$ZIP")
  rm -f "$ZIP"
fi
exec "$MVN" "$@"
