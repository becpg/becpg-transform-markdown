#!/bin/bash
# Arguments: $1 = input base64 file, $2 = output markdown file

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"
OPTIONS="$3"

BASENAME=$(basename "$INPUT_FILENAME" | cut -f 1 -d '.')

echo "BASENAME is $BASENAME"
echo "INPUT_FILENAME is $INPUT_FILENAME"
echo "OUTPUT_FILENAME is $OUTPUT_FILENAME"

cd /tmp || exit 1  # move to a known writable location

echo "START CONVERSION WITH pymupdf4llm"

python3 /usr/local/bin/pymupdf4llm-convert.py "$INPUT_FILENAME" "output.md" $OPTIONS


mv "output.md" "$OUTPUT_FILENAME"
