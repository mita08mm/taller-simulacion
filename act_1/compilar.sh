#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
OUT_DIR="$ROOT_DIR/out"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "No se encontro la carpeta de fuentes Java en: $SRC_DIR"
  exit 1
fi

mkdir -p "$OUT_DIR"

# Compila todos los .java bajo src/main/java hacia out
javac -d "$OUT_DIR" $(find "$SRC_DIR" -name "*.java")

echo "Compilacion completada. Ejecutando..."
java -cp "$OUT_DIR" tss.act1.app.Main "$@"

