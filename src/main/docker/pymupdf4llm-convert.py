"""Convert a PDF or an image to Markdown with pymupdf4llm.

Pages without a text layer (scanned pages, outlined text, images) are read
with RapidOCR when OCR is enabled. Environment variables:

* PYMUPDF4LLM_OCR_ENABLED: "true" (default) or "false"
* PYMUPDF4LLM_OCR_DPI: rendering resolution used for OCR, 300 by default
"""
import logging
import os
import pathlib
import sys

import pymupdf
import pymupdf4llm
from pymupdf4llm.helpers import document_layout
from pymupdf4llm.helpers.utils import is_ocr_text
from pymupdf4llm.ocr.exec_ocr_interface import FONT as OCR_FONT

DEFAULT_OCR_DPI = 300

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


def ocr_enabled():
    return os.environ.get("PYMUPDF4LLM_OCR_ENABLED", "true").strip().lower() != "false"


def ocr_dpi():
    value = os.environ.get("PYMUPDF4LLM_OCR_DPI", "").strip()
    return int(value) if value else DEFAULT_OCR_DPI


def has_text_layer(page):
    """Tell whether the page already has a readable text layer worth keeping."""
    text = "".join(page.get_text().split())
    if len(text) < MIN_TEXT_LAYER_CHARS:
        return False
    return text.count(UNREADABLE_CHAR) / len(text) < MAX_UNREADABLE_CHAR_RATIO


def make_ocr_decision_for_pages_without_text(page, use_ocr):
    """Skip OCR on pages that already have a readable text layer.

    The pymupdf4llm decision model also OCRs text pages that hold a logo or a
    picture, which adds seconds per page without adding useful text.
    """
    needs_ocr, ocr_spans, only_text = _make_ocr_decision(page, use_ocr)
    if needs_ocr and has_text_layer(page):
        return False, ocr_spans, only_text
    return needs_ocr, ocr_spans, only_text


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
    with open_as_pdf(input_filename) as document:
        md_text = pymupdf4llm.to_markdown(document, use_ocr=ocr_enabled(), ocr_dpi=ocr_dpi())
    if not md_text.strip():
        md_text = "\n"
    pathlib.Path(output_filename).write_bytes(md_text.encode())


if __name__ == "__main__":
    main()
