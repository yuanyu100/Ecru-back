from __future__ import annotations

import sys
from pathlib import Path

import win32com.client


def clean(text: str) -> str:
    return " ".join(text.replace("\r", " ").replace("\x07", " ").split())


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python scripts/inspect_word_structure.py <docx-path>")
        return 1

    doc_path = str(Path(sys.argv[1]).resolve())
    word = win32com.client.DispatchEx("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0

    doc = None
    try:
        doc = word.Documents.Open(doc_path, ReadOnly=True)
        print(f"Document: {doc.FullName}")
        print(f"Sections: {doc.Sections.Count}")
        print("--- Key paragraphs ---")

        for i in range(1, doc.Paragraphs.Count + 1):
            para = doc.Paragraphs(i)
            text = clean(para.Range.Text)
            if not text:
                continue
            style_name = str(para.Range.get_Style())
            if any(token in text for token in ("摘要", "目 录", "目录", "绪论", "第一章", "参考文献", "致谢")):
                page = para.Range.Information(3)  # wdActiveEndPageNumber
                sec = para.Range.Information(2)   # wdActiveEndSectionNumber
                print(f"[p{i}][page {page}][sec {sec}] style={style_name} text={text[:120]}")

        print("--- Sections ---")
        for i in range(1, doc.Sections.Count + 1):
            sec = doc.Sections(i)
            first_page = sec.Range.Information(3)
            header = clean(sec.Headers(1).Range.Text)
            footer = clean(sec.Footers(1).Range.Text)
            print(
                f"Section {i}: startPage={first_page} differentFirstPage={sec.PageSetup.DifferentFirstPageHeaderFooter} "
                f"oddEven={sec.PageSetup.OddAndEvenPagesHeaderFooter} header={header[:80]} footer={footer[:80]}"
            )
    finally:
        if doc is not None:
            doc.Close(False)
        word.Quit()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
