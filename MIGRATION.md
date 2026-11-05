# Migration guide

## 1.0

### Notable changes

- Images (`image/png`, `image/jpeg`, `image/bmp`) are now converted by `pymupdf4llm` with RapidOCR instead of MarkItDown, which returned an empty Markdown for any image without EXIF metadata. Their text is now extracted.
- PDFs without a text layer (scans, printer proofs with outlined text) are now read by OCR; PDFs with a text layer give the same Markdown as before.
- `image/tiff` to Markdown is offered again, through `pymupdf4llm`.
- The Docker image is about 200 MB larger (`rapidocr` 3.10.0 and its ONNX models). OCR uses the CPU, about 15 to 20 seconds per page with 2 CPUs; set `PYMUPDF4LLM_OCR_ENABLED=false` to disable it.
- PDF to Markdown works again: 1.0.5 shipped the empty `pymupdf4llm` 2.x package and failed every PDF conversion with `No module named 'pymupdf4llm'`. `pymupdf4llm` is now pinned to 1.28.2; replace any 1.0.5 image with 1.0.6.
- The T-Engine is built on Alfresco Transform Core 5.4.5 (was 5.2.0). No configuration change is required.
- The Python tools of the image are pinned (`markitdown` 0.1.9, `pymupdf4llm` 1.28.2, `md2pdf` 0.4.0), so a rebuild no longer picks up an untested release.
