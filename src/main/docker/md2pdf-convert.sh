#!/bin/bash

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"
OPTIONS="$3"

BASENAME=$(basename "$INPUT_FILENAME" | cut -f 1 -d '.')

cd /tmp || exit 1 

# Run md2pdf
md2pdf "$INPUT_FILENAME" temp.pdf

mv temp.pdf "$OUTPUT_FILENAME"
