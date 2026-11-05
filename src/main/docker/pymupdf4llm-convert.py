"""Convert a PDF or an image to Markdown with pymupdf4llm.

Pages without a text layer (scanned pages, outlined text, images) are read
with RapidOCR when OCR is enabled. Environment variables:

* PYMUPDF4LLM_OCR_ENABLED: "true" (default) or "false"
* PYMUPDF4LLM_OCR_DPI: rendering resolution used for OCR, 300 by default
* PYMUPDF4LLM_OCR_MAX_PAGES: maximum number of pages OCRed per document,
  20 by default, 0 for no limit; the next pages without text are skipped
* PYMUPDF4LLM_OCR_THREADS: CPU threads used by the OCR engine, 2 by default
"""
import logging
import os
import pathlib
import sys

import pymupdf
import pymupdf4llm
from pymupdf4llm.helpers import document_layout
from pymupdf4llm.helpers.utils import is_ocr_text
from pymupdf4llm.ocr import rapidocr_391_backend
from pymupdf4llm.ocr.exec_ocr_interface import FONT as OCR_FONT
from rapidocr import RapidOCR

DEFAULT_OCR_DPI = 300
DEFAULT_OCR_MAX_PAGES = 20
DEFAULT_OCR_THREADS = 2

MIN_TEXT_LAYER_CHARS = 100
MAX_UNREADABLE_CHAR_RATIO = 0.10
UNREADABLE_CHAR = "\ufffd"

VECTOR_STYLE_FLAGS = (
    pymupdf.mupdf.FZ_STEXT_STRIKEOUT
    | pymupdf.mupdf.FZ_STEXT_UNDERLINE
    | pymupdf.mupdf.FZ_STEXT_HIGHLIGHT
)

SPAN_STYLE_KEYS = ("font", "size", "flags", "char_flags", "color", "alpha")

_extract_dict = pymupdf.TextPage.extractDICT
_make_ocr_decision = document_layout.make_ocr_decision
_ocr_page_count = 0


def ocr_enabled():
    return os.environ.get("PYMUPDF4LLM_OCR_ENABLED", "true").strip().lower() != "false"


def int_setting(name, default):
    value = os.environ.get(name, "").strip()
    return int(value) if value else default


def ocr_dpi():
    return int_setting("PYMUPDF4LLM_OCR_DPI", DEFAULT_OCR_DPI)


def ocr_max_pages():
    return int_setting("PYMUPDF4LLM_OCR_MAX_PAGES", DEFAULT_OCR_MAX_PAGES)


def ocr_threads():
    return int_setting("PYMUPDF4LLM_OCR_THREADS", DEFAULT_OCR_THREADS)


def init_engine_with_thread_limit():
    """Create the RapidOCR engine with a bounded number of CPU threads.

    pymupdf4llm creates it without parameters, so ONNX Runtime would use one
    thread per CPU core of the host.
    """
    if rapidocr_391_backend.ENGINE is None:
        threads = ocr_threads()
        rapidocr_391_backend.ENGINE = RapidOCR(params={
            "EngineConfig.onnxruntime.intra_op_num_threads": threads,
            "EngineConfig.onnxruntime.inter_op_num_threads": 1,
        })
    return rapidocr_391_backend.ENGINE


def has_text_layer(page):
    """Tell whether the page already has a readable text layer worth keeping."""
    text = "".join(page.get_text().split())
    if len(text) < MIN_TEXT_LAYER_CHARS:
        return False
    return text.count(UNREADABLE_CHAR) / len(text) < MAX_UNREADABLE_CHAR_RATIO


def make_ocr_decision_for_pages_without_text(page, use_ocr):
    """Skip OCR on pages that already have a readable text layer, and beyond
    the maximum number of OCRed pages.

    The pymupdf4llm decision model also OCRs text pages that hold a logo or a
    picture, which adds seconds per page without adding useful text.
    """
    global _ocr_page_count
    needs_ocr, ocr_spans, only_text = _make_ocr_decision(page, use_ocr)
    if not needs_ocr or has_text_layer(page):
        return False, ocr_spans, only_text
    max_pages = ocr_max_pages()
    if 0 < max_pages <= _ocr_page_count:
        print(f"OCR skipped on page {page.number + 1}: limit of {max_pages} OCRed pages reached",
              file=sys.stderr)
        return False, ocr_spans, only_text
    _ocr_page_count += 1
    return True, ocr_spans, only_text


def is_ocr_span(span):
    return span.get("font") == OCR_FONT.name or is_ocr_text(span)


def same_style(left, right):
    return all(left.get(key) == right.get(key) for key in SPAN_STYLE_KEYS)


def merge_ocr_spans(spans):
    """Clear the vector styles of OCR spans and join the fragments they split."""
    merged = []
    for span in spans:
        if is_ocr_span(span):
            span["char_flags"] &= ~VECTOR_STYLE_FLAGS
            previous = merged[-1] if merged else None
            if previous is not None and is_ocr_span(previous) and same_style(previous, span):
                previous["text"] += span["text"]
                previous["bbox"] = tuple(pymupdf.Rect(previous["bbox"]) | span["bbox"])
                continue
        merged.append(span)
    return merged


def extract_dict_without_ocr_vector_styles(textpage, *args, **kwargs):
    """Drop the strikeout, underline and highlight styles of OCR text.

    MuPDF infers these styles from vector paths crossing the text, and splits
    the text where the style changes. On a page whose text was converted to
    outlines, the glyph outlines cross the OCR text layer, which would turn
    words into fragments such as "B ~~is~~ cu".
    """
    page_dict = _extract_dict(textpage, *args, **kwargs)
    for block in page_dict.get("blocks", []):
        for line in block.get("lines", []):
            line["spans"] = merge_ocr_spans(line.get("spans", []))
    return page_dict


def open_as_pdf(filename):
    """Open the source file, converting an image (any page count) to PDF.

    pymupdf4llm only runs OCR on PDF documents and only converts some image
    formats by itself (TIFF is not converted, for instance).
    """
    document = pymupdf.open(filename)
    if document.is_pdf:
        return document
    pdf_bytes = document.convert_to_pdf()
    document.close()
    return pymupdf.open("pdf", pdf_bytes)


def main():
    input_filename = sys.argv[1]
    output_filename = sys.argv[2]
    logging.getLogger("RapidOCR").setLevel(logging.WARNING)
    pymupdf.TextPage.extractDICT = extract_dict_without_ocr_vector_styles
    document_layout.make_ocr_decision = make_ocr_decision_for_pages_without_text
    rapidocr_391_backend.init_engine = init_engine_with_thread_limit
    with open_as_pdf(input_filename) as document:
        md_text = pymupdf4llm.to_markdown(document, use_ocr=ocr_enabled(), ocr_dpi=ocr_dpi())
    if not md_text.strip():
        md_text = "\n"
    pathlib.Path(output_filename).write_bytes(md_text.encode())


if __name__ == "__main__":
    main()
