#!/bin/bash
set -euo pipefail

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"

TEMP_OUTPUT=$(mktemp /tmp/md2pdf-XXXXXX.pdf)

cleanup() {
    rm -f "$TEMP_OUTPUT"
}

trap cleanup EXIT

md2pdf "$INPUT_FILENAME" "$TEMP_OUTPUT"

mv "$TEMP_OUTPUT" "$OUTPUT_FILENAME"
