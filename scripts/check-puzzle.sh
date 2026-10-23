#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" mobile/src/main/java/com/oasisfeng/island/puzzle/PuzzleEngine.java tests/puzzle/PuzzleEngineCheck.java
java -cp "$classes" com.oasisfeng.island.puzzle.PuzzleEngineCheck
