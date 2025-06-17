#!/bin/bash
# Arguments: $1 = input base64 file, $2 = output markdown file, $3 = extra options for docling

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"
OPTIONS="$3"

BASENAME=$(basename "$INPUT_FILENAME" | cut -f 1 -d '.')

# Create a unique directory for this doc conversion
WORKDIR="/tmp/docling_${BASENAME}_$$"
mkdir -p "$WORKDIR"
cp "$INPUT_FILENAME" "$WORKDIR/"

echo "docling workdir is $WORKDIR"

cd "$WORKDIR" || exit 1

# Run docling
docling "$INPUT_FILENAME" $OPTIONS

# Find the generated file (excluding the original input)
GENERATED_FILE=$(find . -maxdepth 1 -type f -name "${BASENAME}.*" ! -name "$INPUT_FILENAME" | head -n 1)

# Move the result to the desired output
mv "$GENERATED_FILE" "$OUTPUT_FILENAME"

# Clean up
rm -rf "$WORKDIR"
