import pymupdf4llm
import sys
import pathlib

input_filename = sys.argv[1]
output_filename = sys.argv[2]
md_text = pymupdf4llm.to_markdown(input_filename)
pathlib.Path(output_filename).write_bytes(md_text.encode())