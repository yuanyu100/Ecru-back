from __future__ import annotations

import argparse
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
DEFAULT_DOCX = Path(
    r"D:\Code\TRAE\bishe\note\chapter3\2026届-导师邱桃荣-学生范雨湉毕业设计二稿-v5-格式修改-按样式修正.docx"
)


def normalize(text: str) -> str:
    return "".join(text.split())


def paragraph_text(elem: ET.Element) -> str:
    return "".join(node.text or "" for node in elem.findall(".//w:t", NS))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("docx_path", nargs="?", help="path to the DOCX file")
    args = parser.parse_args()

    path = Path(args.docx_path).expanduser() if args.docx_path else DEFAULT_DOCX
    path = path.resolve()
    if not path.exists():
        print(f"docx not found: {path}")
        print(f"fallback: {DEFAULT_DOCX}")
        return 1

    with ZipFile(path) as zf:
        root = ET.fromstring(zf.read("word/document.xml"))

    body = root.find("w:body", NS)
    if body is None:
        print("document body not found")
        return 1

    for idx, child in enumerate(body, start=1):
        if child.tag != f"{{{NS['w']}}}p":
            continue
        raw = paragraph_text(child)
        text = normalize(raw)
        if text:
            print(f"{idx}\t{text}")
        else:
            print(f"{idx}\t")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
