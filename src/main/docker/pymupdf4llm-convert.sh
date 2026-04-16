#!/bin/bash
set -euo pipefail

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"

TEMP_OUTPUT=$(mktemp /tmp/pymupdf4llm-XXXXXX.md)

cleanup() {
    rm -f "$TEMP_OUTPUT"
}

trap cleanup EXIT

python3.11 /usr/local/bin/pymupdf4llm-convert.py "$INPUT_FILENAME" "$TEMP_OUTPUT"

mv "$TEMP_OUTPUT" "$OUTPUT_FILENAME"
