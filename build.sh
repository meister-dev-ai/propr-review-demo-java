#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$ROOT_DIR/dist"

rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$CLASSES_DIR"

javac -d "$CLASSES_DIR" "$ROOT_DIR/src/sitegen/Main.java"
java -cp "$CLASSES_DIR" sitegen.Main "$ROOT_DIR"
