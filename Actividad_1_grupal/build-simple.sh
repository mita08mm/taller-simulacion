#!/bin/bash
set -e

MAIN_CLASS="com.simulacion.presentation.MainApplication"
SRC_DIR="src/main/java"
BUILD_DIR="build/classes"
LIB_DIR="lib"

# Descargar dependencias
mkdir -p "$LIB_DIR"
[ ! -f "$LIB_DIR/jfreechart-1.5.3.jar" ] && curl -sL -o "$LIB_DIR/jfreechart-1.5.3.jar" "https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.3/jfreechart-1.5.3.jar"
[ ! -f "$LIB_DIR/jcommon-1.0.24.jar" ] && curl -sL -o "$LIB_DIR/jcommon-1.0.24.jar" "https://repo1.maven.org/maven2/org/jfree/jcommon/1.0.24/jcommon-1.0.24.jar"

# Compilar
mkdir -p "$BUILD_DIR"
find "$SRC_DIR" -name "*.java" > sources.txt
javac -d "$BUILD_DIR" -cp "$LIB_DIR/*" @sources.txt
rm sources.txt

# Ejecutar
java -cp "$BUILD_DIR:$LIB_DIR/*" "$MAIN_CLASS"
