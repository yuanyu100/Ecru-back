from __future__ import annotations

import copy
import shutil
import re
import sys
import tempfile
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile
import xml.etree.ElementTree as ET

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
CT_NS = "http://schemas.openxmlformats.org/package/2006/content-types"

NS = {"w": W_NS, "r": R_NS}

ET.register_namespace("w", W_NS)
ET.register_namespace("r", R_NS)

W = f"{{{W_NS}}}"
R = f"{{{R_NS}}}"
PR = f"{{{PKG_REL_NS}}}"
CT = f"{{{CT_NS}}}"

HEADER_REL_TYPE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/header"
FOOTER_REL_TYPE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer"
HEADER_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"
FOOTER_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"

MAIN_CHAPTER_RE = re.compile(r"^第[0-9一二三四五六七八九十]+章")
APPENDIX_RE = re.compile(r"^附录([A-Z]|[一二三四五六七八九十])?$")


def qn(tag: str) -> str:
    return f"{W}{tag}"


def normalize(text: str) -> str:
    return "".join(text.split())


def paragraph_text(elem: ET.Element) -> str:
    return "".join(node.text or "" for node in elem.findall(".//w:t", NS))


def int_to_chinese(num: int) -> str:
    digits = "零一二三四五六七八九"
    if num <= 10:
        if num == 10:
            return "十"
        return digits[num]
    if num < 20:
        return "十" + digits[num % 10]
    if num < 100:
        tens, ones = divmod(num, 10)
        result = digits[tens] + "十"
        if ones:
            result += digits[ones]
        return result
    raise ValueError(f"unsupported chapter number: {num}")


def normalize_main_chapter_text(text: str) -> str:
    match = re.match(r"^第([0-9]+|[一二三四五六七八九十]+)章(.*)$", text)
    if not match:
        return text
    raw_num, rest = match.groups()
    if raw_num.isdigit():
        chapter_num = int(raw_num)
        cn_num = int_to_chinese(chapter_num)
    else:
        cn_num = raw_num
    return f"第{cn_num}章{rest}"


def is_real_chapter_heading(text: str) -> bool:
    if not MAIN_CHAPTER_RE.match(text):
        return False
    if "为" in text:
        return False
    if re.search(r"\d+$", text):
        return False
    return True


def ensure_child(parent: ET.Element, tag: str) -> ET.Element:
    child = parent.find(tag)
    if child is None:
        child = ET.SubElement(parent, tag)
    return child


def ensure_ppr(paragraph: ET.Element) -> ET.Element:
    ppr = paragraph.find("w:pPr", NS)
    if ppr is None:
        ppr = ET.Element(qn("pPr"))
        paragraph.insert(0, ppr)
    return ppr


def find_first_index(paragraphs: list[ET.Element], start: int, predicate) -> int:
    for idx in range(start, len(paragraphs)):
        if predicate(normalize(paragraph_text(paragraphs[idx]))):
            return idx
    raise ValueError("required section anchor not found")


def build_header_xml(title: str) -> bytes:
    hdr = ET.Element(qn("hdr"))
    p = ET.SubElement(hdr, qn("p"))
    ppr = ET.SubElement(p, qn("pPr"))
    ET.SubElement(ppr, qn("jc"), {qn("val"): "center"})
    p_bdr = ET.SubElement(ppr, qn("pBdr"))
    ET.SubElement(
        p_bdr,
        qn("bottom"),
        {
            qn("val"): "single",
            qn("sz"): "4",
            qn("space"): "1",
            qn("color"): "auto",
        },
    )
    r = ET.SubElement(p, qn("r"))
    rpr = ET.SubElement(r, qn("rPr"))
    ET.SubElement(
        rpr,
        qn("rFonts"),
        {
            qn("ascii"): "Times New Roman",
            qn("hAnsi"): "Times New Roman",
            qn("eastAsia"): "宋体",
            qn("cs"): "Times New Roman",
        },
    )
    ET.SubElement(rpr, qn("sz"), {qn("val"): "21"})
    ET.SubElement(rpr, qn("szCs"), {qn("val"): "21"})
    ET.SubElement(r, qn("t")).text = title
    return ET.tostring(hdr, encoding="utf-8", xml_declaration=True)


def build_page_field_run() -> list[ET.Element]:
    begin = ET.Element(qn("r"))
    ET.SubElement(begin, qn("fldChar"), {qn("fldCharType"): "begin"})

    instr = ET.Element(qn("r"))
    ET.SubElement(instr, qn("instrText"), {f"{{http://www.w3.org/XML/1998/namespace}}space": "preserve"}).text = " PAGE "

    separate = ET.Element(qn("r"))
    ET.SubElement(separate, qn("fldChar"), {qn("fldCharType"): "separate"})

    text = ET.Element(qn("r"))
    ET.SubElement(text, qn("t")).text = "1"

    end = ET.Element(qn("r"))
    ET.SubElement(end, qn("fldChar"), {qn("fldCharType"): "end"})

    return [begin, instr, separate, text, end]


def build_footer_xml(with_hyphen: bool) -> bytes:
    ftr = ET.Element(qn("ftr"))
    p = ET.SubElement(ftr, qn("p"))
    ppr = ET.SubElement(p, qn("pPr"))
    ET.SubElement(ppr, qn("jc"), {qn("val"): "center"})

    def add_text(text: str) -> None:
        r = ET.SubElement(p, qn("r"))
        rpr = ET.SubElement(r, qn("rPr"))
        ET.SubElement(
            rpr,
            qn("rFonts"),
            {
                qn("ascii"): "Times New Roman",
                qn("hAnsi"): "Times New Roman",
                qn("eastAsia"): "宋体",
                qn("cs"): "Times New Roman",
            },
        )
        ET.SubElement(rpr, qn("sz"), {qn("val"): "18"})
        ET.SubElement(rpr, qn("szCs"), {qn("val"): "18"})
        ET.SubElement(r, qn("t")).text = text

    if with_hyphen:
        add_text("-")
    for run in build_page_field_run():
        p.append(run)
    if with_hyphen:
        add_text("-")

    return ET.tostring(ftr, encoding="utf-8", xml_declaration=True)


def next_available_part(existing_names: set[str], kind: str) -> str:
    nums = []
    prefix = f"word/{kind}"
    for name in existing_names:
        if name.startswith(prefix) and name.endswith(".xml"):
            middle = name[len(prefix) : -4]
            if middle.isdigit():
                nums.append(int(middle))
    next_num = max(nums, default=0) + 1
    while f"word/{kind}{next_num}.xml" in existing_names:
        next_num += 1
    return f"word/{kind}{next_num}.xml"


def add_override(content_types_root: ET.Element, part_name: str, content_type: str) -> None:
    target = "/" + part_name.replace("\\", "/")
    for override in content_types_root.findall(f"{CT}Override"):
        if override.get("PartName") == target:
            override.set("ContentType", content_type)
            return
    ET.SubElement(content_types_root, f"{CT}Override", {"PartName": target, "ContentType": content_type})


def add_relationship(rels_root: ET.Element, rel_id: str, rel_type: str, target: str) -> None:
    ET.SubElement(rels_root, f"{PR}Relationship", {"Id": rel_id, "Type": rel_type, "Target": target})


def next_rel_id(rels_root: ET.Element) -> str:
    nums = []
    for rel in rels_root.findall(f"{PR}Relationship"):
        rel_id = rel.get("Id", "")
        if rel_id.startswith("rId") and rel_id[3:].isdigit():
            nums.append(int(rel_id[3:]))
    return f"rId{max(nums, default=0) + 1}"


def strip_section_markup(sectpr: ET.Element) -> None:
    for child in list(sectpr):
        if child.tag in {
            qn("headerReference"),
            qn("footerReference"),
            qn("pgNumType"),
            qn("titlePg"),
            qn("type"),
        }:
            sectpr.remove(child)


def apply_section_config(
    sectpr: ET.Element,
    *,
    header_rel_id: str | None,
    footer_rel_id: str | None,
    page_format: str | None,
    page_start: int | None,
    break_type: str | None,
) -> ET.Element:
    strip_section_markup(sectpr)

    insert_pos = 0
    if header_rel_id:
        sectpr.insert(insert_pos, ET.Element(qn("headerReference"), {f"{R}id": header_rel_id, qn("type"): "default"}))
        insert_pos += 1
    if footer_rel_id:
        sectpr.insert(insert_pos, ET.Element(qn("footerReference"), {f"{R}id": footer_rel_id, qn("type"): "default"}))
        insert_pos += 1
    if break_type:
        sectpr.insert(insert_pos, ET.Element(qn("type"), {qn("val"): break_type}))
        insert_pos += 1
    if page_format:
        attrs = {qn("fmt"): page_format}
        if page_start is not None:
            attrs[qn("start")] = str(page_start)
        sectpr.insert(insert_pos, ET.Element(qn("pgNumType"), attrs))
    return sectpr


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python scripts/format_thesis_headers_footers.py <docx-path>")
        return 1

    docx_path = Path(sys.argv[1]).resolve()
    with ZipFile(docx_path, "r") as zin:
        files = {name: zin.read(name) for name in zin.namelist()}

    document_root = ET.fromstring(files["word/document.xml"])
    rels_root = ET.fromstring(files["word/_rels/document.xml.rels"])
    content_types_root = ET.fromstring(files["[Content_Types].xml"])
    settings_root = ET.fromstring(files["word/settings.xml"])

    body = document_root.find("w:body", NS)
    if body is None:
        raise ValueError("document body missing")

    paragraphs = [child for child in body if child.tag == qn("p")]
    if not paragraphs:
        raise ValueError("document has no paragraphs")

    for paragraph in paragraphs:
        raw_text = paragraph_text(paragraph)
        normalized = normalize(raw_text)
        if not normalized:
            continue
        new_text = None
        if is_real_chapter_heading(normalized):
            new_text = normalize_main_chapter_text(normalized)
        elif re.match(r"^第[0-9]+章", normalized):
            new_text = normalize_main_chapter_text(normalized)
        if new_text and new_text != normalized:
            text_nodes = paragraph.findall(".//w:t", NS)
            if text_nodes:
                text_nodes[0].text = new_text
                for node in text_nodes[1:]:
                    node.text = ""

    abstract_idx = find_first_index(paragraphs, 0, lambda text: text == "摘要")
    english_abstract_idx = find_first_index(paragraphs, abstract_idx + 1, lambda text: text == "Abstract")
    toc_idx = find_first_index(paragraphs, english_abstract_idx + 1, lambda text: text == "目录")
    first_body_idx = find_first_index(paragraphs, toc_idx + 1, is_real_chapter_heading)

    chapter_starts = []
    for idx in range(first_body_idx, len(paragraphs)):
        text = normalize(paragraph_text(paragraphs[idx]))
        if is_real_chapter_heading(text):
            chapter_starts.append(idx)

    references_idx = next((idx for idx in range(first_body_idx, len(paragraphs)) if normalize(paragraph_text(paragraphs[idx])) == "参考文献"), None)
    acknowledgement_idx = next((idx for idx in range(first_body_idx, len(paragraphs)) if normalize(paragraph_text(paragraphs[idx])) == "致谢"), None)
    appendix_indices = [
        idx
        for idx in range(first_body_idx, len(paragraphs))
        if APPENDIX_RE.match(normalize(paragraph_text(paragraphs[idx])))
    ]

    anchor_indices = {abstract_idx, english_abstract_idx, toc_idx}
    chapter_starts = [idx for idx in chapter_starts if idx not in anchor_indices]

    section_specs: list[dict[str, object]] = [
        {"start": 0, "header": None, "footer": None, "format": None, "start_num": None},
        {"start": abstract_idx, "header": "摘要", "footer": "roman", "format": "upperRoman", "start_num": 1},
        {"start": english_abstract_idx, "header": "Abstract", "footer": "roman", "format": "upperRoman", "start_num": None},
        {"start": toc_idx, "header": "目录", "footer": "roman", "format": "upperRoman", "start_num": None},
    ]

    body_starts: list[int] = []
    for idx in chapter_starts:
        body_starts.append(idx)
    if references_idx is not None:
        body_starts.append(references_idx)
    if acknowledgement_idx is not None:
        body_starts.append(acknowledgement_idx)
    body_starts.extend(appendix_indices)
    body_starts = sorted(set(i for i in body_starts if i > toc_idx))

    for pos, idx in enumerate(body_starts):
        section_specs.append(
            {
                "start": idx,
                "header": normalize(paragraph_text(paragraphs[idx])),
                "footer": "body",
                "format": "decimal",
                "start_num": 1 if pos == 0 else None,
            }
        )

    if len(section_specs) < 5:
        raise ValueError("main body section anchors not found")

    base_sectpr = body.find("w:sectPr", NS)
    if base_sectpr is None:
        for paragraph in paragraphs:
            ppr = paragraph.find("w:pPr", NS)
            if ppr is not None:
                candidate = ppr.find("w:sectPr", NS)
                if candidate is not None:
                    base_sectpr = candidate
                    break
    if base_sectpr is None:
        raise ValueError("unable to locate a base sectPr")

    base_sectpr = copy.deepcopy(base_sectpr)

    for paragraph in paragraphs:
        ppr = paragraph.find("w:pPr", NS)
        if ppr is not None:
            old = ppr.find("w:sectPr", NS)
            if old is not None:
                ppr.remove(old)
    old_body_sectpr = body.find("w:sectPr", NS)
    if old_body_sectpr is not None:
        body.remove(old_body_sectpr)

    existing_names = set(files)
    part_cache: dict[tuple[str, str], tuple[str, str]] = {}

    def get_part(kind: str, content_key: str, xml_bytes: bytes) -> tuple[str, str]:
        cache_key = (kind, content_key)
        if cache_key in part_cache:
            return part_cache[cache_key]
        part_name = next_available_part(existing_names, kind)
        existing_names.add(part_name)
        rel_id = next_rel_id(rels_root)
        add_relationship(rels_root, rel_id, HEADER_REL_TYPE if kind == "header" else FOOTER_REL_TYPE, Path(part_name).name)
        add_override(content_types_root, part_name, HEADER_CONTENT_TYPE if kind == "header" else FOOTER_CONTENT_TYPE)
        files[part_name] = xml_bytes
        part_cache[cache_key] = (part_name, rel_id)
        return part_cache[cache_key]

    _, roman_footer_rel = get_part("footer", "roman", build_footer_xml(False))
    _, body_footer_rel = get_part("footer", "body", build_footer_xml(True))

    header_rel_by_title: dict[str, str] = {}
    for spec in section_specs:
        title = spec["header"]
        if not title:
            continue
        _, rel_id = get_part("header", str(title), build_header_xml(str(title)))
        header_rel_by_title[str(title)] = rel_id

    for current, next_spec in zip(section_specs, section_specs[1:]):
        next_start = int(next_spec["start"])
        prev_paragraph = paragraphs[next_start - 1]
        ppr = ensure_ppr(prev_paragraph)
        sectpr = apply_section_config(
            copy.deepcopy(base_sectpr),
            header_rel_id=header_rel_by_title.get(str(current["header"])) if current["header"] else None,
            footer_rel_id=roman_footer_rel if current["footer"] == "roman" else body_footer_rel if current["footer"] == "body" else None,
            page_format=str(current["format"]) if current["format"] else None,
            page_start=int(current["start_num"]) if current["start_num"] is not None else None,
            break_type="nextPage",
        )
        ppr.append(sectpr)

    last_spec = section_specs[-1]
    final_sectpr = apply_section_config(
        copy.deepcopy(base_sectpr),
        header_rel_id=header_rel_by_title.get(str(last_spec["header"])) if last_spec["header"] else None,
        footer_rel_id=roman_footer_rel if last_spec["footer"] == "roman" else body_footer_rel if last_spec["footer"] == "body" else None,
        page_format=str(last_spec["format"]) if last_spec["format"] else None,
        page_start=int(last_spec["start_num"]) if last_spec["start_num"] is not None else None,
        break_type=None,
    )
    body.append(final_sectpr)

    update_fields = settings_root.find("w:updateFields", NS)
    if update_fields is None:
        update_fields = ET.SubElement(settings_root, qn("updateFields"))
    update_fields.set(qn("val"), "true")

    files["word/document.xml"] = ET.tostring(document_root, encoding="utf-8", xml_declaration=True)
    files["word/_rels/document.xml.rels"] = ET.tostring(rels_root, encoding="utf-8", xml_declaration=True)
    files["[Content_Types].xml"] = ET.tostring(content_types_root, encoding="utf-8", xml_declaration=True)
    files["word/settings.xml"] = ET.tostring(settings_root, encoding="utf-8", xml_declaration=True)

    with tempfile.NamedTemporaryFile(delete=False, suffix=".docx", dir=str(docx_path.parent)) as tmp:
        tmp_path = Path(tmp.name)

    try:
        with ZipFile(tmp_path, "w", compression=ZIP_DEFLATED) as zout:
            for name, data in files.items():
                zout.writestr(name, data)
        try:
            tmp_path.replace(docx_path)
        except PermissionError:
            alt_path = docx_path.with_name(docx_path.stem + "-页眉页脚修订" + docx_path.suffix)
            shutil.copyfile(tmp_path, alt_path)
            print(f"target locked, wrote alternate file: {alt_path}")
    finally:
        if tmp_path.exists():
            tmp_path.unlink()

    print(f"updated: {docx_path}")
    print("sections:")
    for spec in section_specs:
        print(
            f"start={spec['start']} header={spec['header']} footer={spec['footer']} "
            f"fmt={spec['format']} startNum={spec['start_num']}"
        )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
