#!/bin/bash
# Arguments: $1 = input base64 file, $2 = output markdown file

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"
OPTIONS="$3"

BASENAME=$(basename "$INPUT_FILENAME" | cut -f 1 -d '.')

cd /tmp || exit 1  # move to a known writable location

# Run docling
docling "$INPUT_FILENAME" $OPTIONS

GENERATED_FILE=$(find . -maxdepth 1 -type f -name "${BASENAME}.*" ! -name "${INPUT_FILENAME}" | head -n 1)
mv "$GENERATED_FILE" "$OUTPUT_FILENAME"
