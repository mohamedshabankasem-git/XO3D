#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
V=8.9; D="$HOME/.gradle/wrapper/dists/gradle-$V-bin/xo3d"; G="$D/gradle-$V/bin/gradle"
if [ ! -x "$G" ]; then mkdir -p "$D"; A="$D/gradle.zip"; if command -v curl >/dev/null 2>&1; then curl -fsSL "https://services.gradle.org/distributions/gradle-$V-bin.zip" -o "$A"; else wget -q "https://services.gradle.org/distributions/gradle-$V-bin.zip" -O "$A"; fi; unzip -q -o "$A" -d "$D"; fi
exec "$G" -p "$ROOT" "$@"
