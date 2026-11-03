# Migration guide

## 1.0

### Notable changes

- PDF to Markdown works again: 1.0.5 shipped the empty `pymupdf4llm` 2.x package and failed every PDF conversion with `No module named 'pymupdf4llm'`. `pymupdf4llm` is now pinned to 1.28.2; replace any 1.0.5 image with 1.0.6.
- The T-Engine is built on Alfresco Transform Core 5.4.5 (was 5.2.0). No configuration change is required.
- `image/tiff` to Markdown is no longer offered: MarkItDown never converted TIFF files, every request failed. Alfresco now looks for another transformer for TIFF.
- The Python tools of the image are pinned (`markitdown` 0.1.9, `pymupdf4llm` 1.28.2, `md2pdf` 0.4.0), so a rebuild no longer picks up an untested release.
