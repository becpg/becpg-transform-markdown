#!/bin/bash
set -euo pipefail

INPUT_FILENAME="$1"
OUTPUT_FILENAME="$2"
OPTIONS="$3"

IFS=":" read -r TARGET_FORMAT SKIP_IMAGES <<< "$OPTIONS"
WORKDIR=$(mktemp -d /tmp/docling-XXXXXX)
BASENAME=$(basename "$INPUT_FILENAME")
BASENAME="${BASENAME%.*}"

cleanup() {
    rm -rf "$WORKDIR"
}

trap cleanup EXIT

cd "$WORKDIR"

COMMAND=(docling "$INPUT_FILENAME" --no-ocr --to "${TARGET_FORMAT:-md}")

if [ "${SKIP_IMAGES:-false}" = "true" ]; then
    COMMAND+=(--image-export-mode placeholder)
fi

"${COMMAND[@]}"

GENERATED_FILE=$(find "$WORKDIR" -maxdepth 1 -type f -name "${BASENAME}.*" | sort | head -n 1)

if [ -z "$GENERATED_FILE" ]; then
    echo "Docling did not produce an output file" >&2
    exit 1
fi

mv "$GENERATED_FILE" "$OUTPUT_FILENAME"
