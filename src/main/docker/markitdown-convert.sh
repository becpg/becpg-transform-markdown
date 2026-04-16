#!/bin/bash
set -euo pipefail

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"

TEMP_OUTPUT=$(mktemp /tmp/markitdown-XXXXXX.md)

cleanup() {
    rm -f "$TEMP_OUTPUT"
}

trap cleanup EXIT

markitdown "$INPUT_FILENAME" > "$TEMP_OUTPUT"

mv "$TEMP_OUTPUT" "$OUTPUT_FILENAME"
