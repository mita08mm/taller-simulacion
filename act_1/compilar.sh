#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/out"

if [[ -d "$ROOT_DIR/src/main/java" ]]; then
  SRC_ROOT="$ROOT_DIR/src/main/java"
else
  SRC_ROOT="$ROOT_DIR/src"
fi

rm -rf "$OUT_DIR" || true
mkdir -p "$OUT_DIR"

find "$SRC_ROOT" -name "*.java" -print0 | xargs -0 javac -d "$OUT_DIR"

java -cp "$OUT_DIR" tss.act1.app.Main "$@"

