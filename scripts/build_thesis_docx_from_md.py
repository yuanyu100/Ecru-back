import copy
import os
import re
import shutil
import subprocess
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

from PIL import Image, ImageDraw, ImageFont
from xml.etree import ElementTree as ET


ROOT = Path(r"D:\Code\TRAE\bishe")
NOTE_DIR = ROOT / "note"
MD_PATH = Path(os.environ.get("THESIS_MD_PATH", str(NOTE_DIR / "2026届导师邱桃荣学生范雨湉毕业设计-批注版修订工作稿-20260506.md")))
TEMPLATE_PATH = Path(os.environ.get("THESIS_TEMPLATE_PATH", str(NOTE_DIR / "2026届导师邱桃荣学生范雨湉毕业设计.docx")))
OUTPUT_PATH = Path(os.environ.get("THESIS_OUTPUT_PATH", str(NOTE_DIR / "2026届导师邱桃荣学生范雨湉毕业设计-批注版修订稿-20260506.docx")))
DEFAULT_FALLBACK_OUTPUT_PATH = Path(os.environ.get("THESIS_FALLBACK_OUTPUT_PATH", str(NOTE_DIR / "2026届导师邱桃荣学生范雨湉毕业设计-批注版修订稿-补图版-20260506.docx")))
WORK_DIR = NOTE_DIR / "_docx_build"
SVG_RENDER_DIR = WORK_DIR / "rendered_svg"


NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


def emu_from_px(px: int, dpi: int = 96) -> int:
    return int(px / dpi * 914400)


PAGE_WIDTH_TWIPS = 11906
PAGE_MARGIN_LEFT_TWIPS = 1800
PAGE_MARGIN_RIGHT_TWIPS = 1800
CONTENT_WIDTH_TWIPS = PAGE_WIDTH_TWIPS - PAGE_MARGIN_LEFT_TWIPS - PAGE_MARGIN_RIGHT_TWIPS
BODY_LINE_AUTO = 324
REFERENCE_LINE_AUTO = 360
SCREENSHOT_MAX_WIDTH_EMU = 5200000
USER_SCREENSHOT_MAX_HEIGHT_EMU = 3200000


def build_text_run(text, *, bold=False, size=24, superscript=False, color=None, underline=None):
    if text is None:
        text = ""
    rpr = [
        '<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体" w:cs="Times New Roman"/>',
        f'<w:sz w:val="{size}"/>',
        f'<w:szCs w:val="{size}"/>',
    ]
    if bold:
        rpr.append("<w:b/>")
        rpr.append("<w:bCs/>")
    if superscript:
        rpr.append('<w:vertAlign w:val="superscript"/>')
    if color:
        rpr.append(f'<w:color w:val="{color}"/>')
    if underline is not None:
        underline_val = "single" if underline else "none"
        rpr.append(f'<w:u w:val="{underline_val}"/>')
    text = escape(text)
    return f'<w:r><w:rPr>{"".join(rpr)}</w:rPr><w:t xml:space="preserve">{text}</w:t></w:r>'


def build_citation_hyperlink(citation_num, *, size=24, anchor_prefix="ref_", superscript=True):
    anchor = f"ref_{citation_num}"
    if anchor_prefix != "ref_":
        anchor = f"{anchor_prefix}{citation_num}"
    text = f"[{citation_num}]"
    run = build_text_run(text, size=size, superscript=superscript, color="000000", underline=False)
    return f'<w:hyperlink w:anchor="{anchor}" w:history="1">{run}</w:hyperlink>'


def build_reference_paragraph(text, citation_num, *, size=24, bookmark_name=None, bookmark_id=None, backlink=False):
    ppr_xml = (
        "<w:pPr>"
        "<w:jc w:val=\"both\"/>"
        "<w:ind w:leftChars=\"200\" w:hangingChars=\"200\"/>"
        f"<w:spacing w:line=\"{REFERENCE_LINE_AUTO}\" w:lineRule=\"auto\"/>"
        "</w:pPr>"
    )
    parts = [ppr_xml]
    if bookmark_name and bookmark_id is not None:
        parts.append(f'<w:bookmarkStart w:id="{bookmark_id}" w:name="{escape(bookmark_name)}"/>')
    if backlink:
        parts.append(build_citation_hyperlink(citation_num, size=size, anchor_prefix="cite_", superscript=False))
    else:
        parts.append(build_text_run(f"[{citation_num}]", size=size))
    parts.append(build_text_run(" " + text, size=size))
    if bookmark_name and bookmark_id is not None:
        parts.append(f'<w:bookmarkEnd w:id="{bookmark_id}"/>')
    return f'<w:p>{"".join(parts)}</w:p>'


def build_toc_paragraph():
    return (
        "<w:p>"
        "<w:pPr><w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr>"
        "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>"
        "<w:r><w:instrText xml:space=\"preserve\"> TOC \\h \\z \\u </w:instrText></w:r>"
        "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>"
        f"{build_text_run('目录将在打开文档后自动生成。若未显示，请在 Word 中右键目录并选择“更新域”。', size=24)}"
        "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>"
        "</w:p>"
    )


def build_paragraph(
    text,
    *,
    align=None,
    bold=False,
    size=24,
    first_line=True,
    page_break_before=False,
    outline_level=None,
    process_citations=True,
    bookmark_name=None,
    bookmark_id=None,
    citation_backlinks=None,
    first_citation_bookmarks=None,
):
    if text is None:
        text = ""
    ppr = []
    if align:
        ppr.append(f'<w:jc w:val="{align}"/>')
    elif not bold:
        ppr.append('<w:jc w:val="both"/>')
    if first_line:
        ppr.append('<w:ind w:firstLineChars="200"/>')
    if page_break_before:
        ppr.append('<w:pageBreakBefore/>')
    if outline_level is not None:
        ppr.append(f'<w:outlineLvl w:val="{outline_level}"/>')
    ppr.append(f'<w:spacing w:line="{BODY_LINE_AUTO}" w:lineRule="auto"/>')
    ppr_xml = f"<w:pPr>{''.join(ppr)}</w:pPr>"
    parts = [ppr_xml]
    if bookmark_name and bookmark_id is not None:
        parts.append(f'<w:bookmarkStart w:id="{bookmark_id}" w:name="{escape(bookmark_name)}"/>')

    if process_citations and not bold:
        last = 0
        matched = False
        for match in re.finditer(r"\[(\d+)\]", text):
            matched = True
            if match.start() > last:
                parts.append(build_text_run(text[last:match.start()], bold=bold, size=size))
            citation_num = match.group(1)
            if first_citation_bookmarks is not None and citation_num not in first_citation_bookmarks:
                first_citation_bookmarks[citation_num] = bookmark_id
                parts.append(f'<w:bookmarkStart w:id="{bookmark_id}" w:name="cite_{citation_num}"/>')
                parts.append(build_citation_hyperlink(citation_num, size=size))
                parts.append(f'<w:bookmarkEnd w:id="{bookmark_id}"/>')
                if citation_backlinks is not None:
                    citation_backlinks[citation_num] = True
                bookmark_id += 1
            else:
                parts.append(build_citation_hyperlink(citation_num, size=size))
            last = match.end()
        if matched:
            if last < len(text):
                parts.append(build_text_run(text[last:], bold=bold, size=size))
        else:
            parts.append(build_text_run(text, bold=bold, size=size))
    else:
        parts.append(build_text_run(text, bold=bold, size=size))

    if bookmark_name and bookmark_id is not None:
        parts.append(f'<w:bookmarkEnd w:id="{bookmark_id}"/>')
    return f'<w:p>{"".join(parts)}</w:p>'


def build_caption(text, *, above=False):
    return build_paragraph(
        text,
        align="center",
        bold=True,
        size=24,
        first_line=False,
        page_break_before=False,
    )


def build_cover_item(text):
    return build_paragraph(text, align="center", size=24, first_line=False)


def build_blank_paragraph():
    return build_paragraph("", first_line=False)


def build_table(rows):
    col_count = max(len(row) for row in rows) if rows else 1

    def text_weight(text):
        weight = 0.0
        for ch in text:
            if "\u4e00" <= ch <= "\u9fff":
                weight += 1.9
            elif ch.isupper():
                weight += 1.15
            else:
                weight += 1.0
        return max(weight, 1.0)

    col_weights = []
    for col_idx in range(col_count):
        max_weight = 1.0
        for row in rows:
            cell = row[col_idx] if col_idx < len(row) else ""
            max_weight = max(max_weight, text_weight(cell))
        col_weights.append(max_weight)

    total_weight = sum(col_weights) or float(col_count)
    raw_widths = [int(CONTENT_WIDTH_TWIPS * w / total_weight) for w in col_weights]
    min_width = 1100
    widths = [max(w, min_width) for w in raw_widths]
    width_sum = sum(widths)
    if width_sum > CONTENT_WIDTH_TWIPS:
        adjustable = [max(w - min_width, 0) for w in widths]
        adjustable_sum = sum(adjustable)
        overflow = width_sum - CONTENT_WIDTH_TWIPS
        if adjustable_sum > 0:
            reduced = []
            for i, w in enumerate(widths):
                cut = int(overflow * adjustable[i] / adjustable_sum)
                reduced.append(max(min_width, w - cut))
            widths = reduced
        width_sum = sum(widths)
    if width_sum != CONTENT_WIDTH_TWIPS and widths:
        widths[-1] += CONTENT_WIDTH_TWIPS - width_sum

    tr_xml = []
    for row_index, row in enumerate(rows):
        tc_xml = []
        for col_idx in range(col_count):
            cell = row[col_idx] if col_idx < len(row) else ""
            col_width = widths[col_idx]
            cell_text = escape(cell.strip())
            tc_xml.append(
                "<w:tc>"
                f"<w:tcPr><w:tcW w:w=\"{col_width}\" w:type=\"dxa\"/>"
                "<w:vAlign w:val=\"center\"/>"
                "<w:tcMar>"
                "<w:top w:w=\"90\" w:type=\"dxa\"/>"
                "<w:bottom w:w=\"90\" w:type=\"dxa\"/>"
                "<w:left w:w=\"80\" w:type=\"dxa\"/>"
                "<w:right w:w=\"80\" w:type=\"dxa\"/>"
                "</w:tcMar>"
                "</w:tcPr>"
                "<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:before=\"60\" w:after=\"60\" w:line=\"324\" w:lineRule=\"auto\"/></w:pPr>"
                "<w:r><w:rPr>"
                "<w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\" w:eastAsia=\"宋体\" w:cs=\"Times New Roman\"/>"
                + ("<w:b/><w:bCs/>" if row_index == 0 else "")
                + "<w:sz w:val=\"21\"/><w:szCs w:val=\"21\"/>"
                "</w:rPr>"
                f"<w:t>{cell_text}</w:t>"
                "</w:r></w:p></w:tc>"
            )
        tr_xml.append(f"<w:tr>{''.join(tc_xml)}</w:tr>")
    tbl_pr = (
        "<w:tblPr>"
        f"<w:tblW w:w=\"{CONTENT_WIDTH_TWIPS}\" w:type=\"dxa\"/>"
        "<w:jc w:val=\"center\"/>"
        "<w:tblBorders>"
        "<w:top w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"000000\"/>"
        "<w:left w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"000000\"/>"
        "<w:bottom w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"000000\"/>"
        "<w:right w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"000000\"/>"
        "<w:insideH w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"000000\"/>"
        "<w:insideV w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"000000\"/>"
        "</w:tblBorders>"
        "</w:tblPr>"
    )
    return f"<w:tbl>{tbl_pr}{''.join(tr_xml)}</w:tbl>"


def build_drawing(rid, image_name, width_px, height_px, docpr_id, *, max_width_emu=SCREENSHOT_MAX_WIDTH_EMU, max_height_emu=None):
    cx = emu_from_px(width_px)
    cy = emu_from_px(height_px)
    if cx > max_width_emu:
        scale = max_width_emu / cx
        cx = int(cx * scale)
        cy = int(cy * scale)
    if max_height_emu and cy > max_height_emu:
        scale = max_height_emu / cy
        cx = int(cx * scale)
        cy = int(cy * scale)
    image_name = escape(image_name)
    return (
        "<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr><w:r>"
        "<w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\" "
        "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
        "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
        "xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
        f"<wp:extent cx=\"{cx}\" cy=\"{cy}\"/>"
        f"<wp:docPr id=\"{docpr_id}\" name=\"{image_name}\"/>"
        "<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect=\"1\"/></wp:cNvGraphicFramePr>"
        "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
        "<pic:pic><pic:nvPicPr>"
        f"<pic:cNvPr id=\"{docpr_id}\" name=\"{image_name}\"/>"
        "<pic:cNvPicPr><a:picLocks noChangeAspect=\"1\" noChangeArrowheads=\"1\"/></pic:cNvPicPr>"
        "</pic:nvPicPr><pic:blipFill>"
        f"<a:blip r:embed=\"{rid}\"/>"
        "<a:srcRect/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>"
        "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/>"
        f"<a:ext cx=\"{cx}\" cy=\"{cy}\"/></a:xfrm>"
        "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>"
        "</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing>"
        "</w:r></w:p>"
    )


def build_image_paragraph(rid, image_name, image_path, docpr_id, *, max_width_emu=SCREENSHOT_MAX_WIDTH_EMU, max_height_emu=None):
    try:
        with Image.open(image_path) as img:
            width_px, height_px = img.size
        return build_drawing(
            rid,
            image_name,
            width_px,
            height_px,
            docpr_id,
            max_width_emu=max_width_emu,
            max_height_emu=max_height_emu,
        )
    except Exception:
        return build_paragraph(f"[图片未嵌入] {image_name}: {image_path}", first_line=False)


def find_browser():
    candidates = [
        Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def render_svg_to_png(svg_path: Path) -> Path | None:
    SVG_RENDER_DIR.mkdir(parents=True, exist_ok=True)
    png_path = SVG_RENDER_DIR / f"{svg_path.stem}.png"
    try:
        render_svg_locally(svg_path, png_path)
        if png_path.exists() and png_path.stat().st_size > 0:
            return png_path
    except Exception:
        pass
    browser = find_browser()
    if browser is None:
        return None
    url = svg_path.resolve().as_uri()
    cmd = [
        str(browser),
        "--headless",
        "--disable-gpu",
        "--hide-scrollbars",
        "--no-first-run",
        "--no-default-browser-check",
        f"--user-data-dir={WORK_DIR / 'edge-profile-fallback'}",
        f"--screenshot={png_path}",
        url,
    ]
    try:
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
        if png_path.exists():
            return png_path
    except Exception:
        return None
    return None


def parse_style_block(svg_text: str):
    styles = {}
    for cls, body in re.findall(r"\.([A-Za-z0-9_-]+)\s*\{([^}]*)\}", svg_text, re.S):
        props = {}
        for part in body.split(";"):
            if ":" in part:
                k, v = part.split(":", 1)
                props[k.strip()] = v.strip().strip("'\"")
        styles[cls] = props
    return styles


def parse_color(value: str | None, default="black"):
    if not value or value == "none":
        return None if value == "none" else default
    return value


def load_font(size: int, bold: bool = False):
    candidates = []
    if bold:
        candidates.extend(
            [
                Path(r"C:\Windows\Fonts\msyhbd.ttc"),
                Path(r"C:\Windows\Fonts\simhei.ttf"),
                Path(r"C:\Windows\Fonts\arialbd.ttf"),
            ]
        )
    candidates.extend(
        [
            Path(r"C:\Windows\Fonts\msyh.ttc"),
            Path(r"C:\Windows\Fonts\simsun.ttc"),
            Path(r"C:\Windows\Fonts\arial.ttf"),
        ]
    )
    for font_path in candidates:
        if font_path.exists():
            try:
                return ImageFont.truetype(str(font_path), size=size)
            except Exception:
                continue
    return ImageFont.load_default()


def parse_translate(transform: str | None):
    if not transform:
        return 0.0, 0.0
    m = re.search(r"translate\(([-0-9.]+)\s*,?\s*([-0-9.]*)\)", transform)
    if not m:
        return 0.0, 0.0
    x = float(m.group(1))
    y = float(m.group(2) or 0.0)
    return x, y


def svg_float(value, default=0.0):
    if value is None:
        return default
    try:
        return float(str(value).replace("px", ""))
    except Exception:
        return default


def apply_class_props(node, styles):
    props = {}
    cls = node.attrib.get("class", "")
    for name in cls.split():
        props.update(styles.get(name, {}))
    return props


def parse_dash_array(value):
    if not value:
        return None
    parts = []
    for token in str(value).replace(",", " ").split():
        try:
            parts.append(float(token))
        except Exception:
            continue
    return parts or None


def draw_line_with_style(draw, points, stroke, width, dash_array=None):
    if not points or len(points) < 2 or not stroke:
        return
    if not dash_array:
        draw.line(points, fill=stroke, width=width)
        return
    dash = dash_array[0] if len(dash_array) >= 1 else 8
    gap = dash_array[1] if len(dash_array) >= 2 else dash
    for idx in range(len(points) - 1):
        x1, y1 = points[idx]
        x2, y2 = points[idx + 1]
        dx = x2 - x1
        dy = y2 - y1
        length = (dx * dx + dy * dy) ** 0.5
        if length == 0:
            continue
        ux = dx / length
        uy = dy / length
        pos = 0.0
        while pos < length:
            seg_start = pos
            seg_end = min(pos + dash, length)
            sx = x1 + ux * seg_start
            sy = y1 + uy * seg_start
            ex = x1 + ux * seg_end
            ey = y1 + uy * seg_end
            draw.line([(sx, sy), (ex, ey)], fill=stroke, width=width)
            pos += dash + gap


def draw_svg_node(draw, node, styles, offset=(0.0, 0.0)):
    tag = node.tag.split("}")[-1]
    ox, oy = offset
    props = apply_class_props(node, styles)

    if tag == "g":
        tx, ty = parse_translate(node.attrib.get("transform"))
        for child in list(node):
            draw_svg_node(draw, child, styles, (ox + tx, oy + ty))
        return

    if tag == "rect":
        x = svg_float(node.attrib.get("x")) + ox
        y = svg_float(node.attrib.get("y")) + oy
        w = svg_float(node.attrib.get("width"))
        h = svg_float(node.attrib.get("height"))
        fill = parse_color(node.attrib.get("fill") or props.get("fill"), "white")
        stroke = parse_color(node.attrib.get("stroke") or props.get("stroke"), "black")
        sw = int(svg_float(node.attrib.get("stroke-width") or props.get("stroke-width"), 2))
        radius = int(max(svg_float(node.attrib.get("rx"), 0), svg_float(props.get("rx"), 0)))
        if radius > 0:
            draw.rounded_rectangle([x, y, x + w, y + h], radius=radius, fill=fill, outline=stroke, width=sw)
        else:
            draw.rectangle([x, y, x + w, y + h], fill=fill, outline=stroke, width=sw)
        return

    if tag == "line":
        x1 = svg_float(node.attrib.get("x1")) + ox
        y1 = svg_float(node.attrib.get("y1")) + oy
        x2 = svg_float(node.attrib.get("x2")) + ox
        y2 = svg_float(node.attrib.get("y2")) + oy
        fill = parse_color(node.attrib.get("stroke") or props.get("stroke"), "black")
        sw = int(svg_float(node.attrib.get("stroke-width") or props.get("stroke-width"), 2))
        dash_array = parse_dash_array(node.attrib.get("stroke-dasharray") or props.get("stroke-dasharray"))
        draw_line_with_style(draw, [(x1, y1), (x2, y2)], fill, sw, dash_array)
        return

    if tag == "circle":
        cx = svg_float(node.attrib.get("cx")) + ox
        cy = svg_float(node.attrib.get("cy")) + oy
        r = svg_float(node.attrib.get("r"))
        fill = parse_color(node.attrib.get("fill") or props.get("fill"), "white")
        stroke = parse_color(node.attrib.get("stroke") or props.get("stroke"), "black")
        sw = int(svg_float(node.attrib.get("stroke-width") or props.get("stroke-width"), 2))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill, outline=stroke, width=sw)
        return

    if tag == "ellipse":
        cx = svg_float(node.attrib.get("cx")) + ox
        cy = svg_float(node.attrib.get("cy")) + oy
        rx = svg_float(node.attrib.get("rx"))
        ry = svg_float(node.attrib.get("ry"))
        fill = parse_color(node.attrib.get("fill") or props.get("fill"), "white")
        stroke = parse_color(node.attrib.get("stroke") or props.get("stroke"), "black")
        sw = int(svg_float(node.attrib.get("stroke-width") or props.get("stroke-width"), 2))
        draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=fill, outline=stroke, width=sw)
        return

    if tag == "polygon":
        pts = []
        for pair in node.attrib.get("points", "").split():
            if "," in pair:
                px, py = pair.split(",", 1)
                pts.append((svg_float(px) + ox, svg_float(py) + oy))
        if pts:
            fill = parse_color(node.attrib.get("fill") or props.get("fill"), "white")
            stroke = parse_color(node.attrib.get("stroke") or props.get("stroke"), "black")
            sw = int(svg_float(node.attrib.get("stroke-width") or props.get("stroke-width"), 2))
            draw.polygon(pts, fill=fill, outline=stroke)
            if stroke and sw > 1:
                draw.line(pts + [pts[0]], fill=stroke, width=sw)
        return

    if tag == "path":
        d = node.attrib.get("d", "")
        pts = []
        for match in re.finditer(r"([ML])\s*([-0-9.]+),([-0-9.]+)", d):
            pts.append((svg_float(match.group(2)) + ox, svg_float(match.group(3)) + oy))
        z_close = "Z" in d.upper()
        fill = parse_color(node.attrib.get("fill") or props.get("fill"), "black")
        stroke = parse_color(node.attrib.get("stroke") or props.get("stroke"), fill or "black")
        sw = int(svg_float(node.attrib.get("stroke-width") or props.get("stroke-width"), 2))
        dash_array = parse_dash_array(node.attrib.get("stroke-dasharray") or props.get("stroke-dasharray"))
        if pts:
            if z_close:
                draw.polygon(pts, fill=fill, outline=stroke)
            else:
                draw_line_with_style(draw, pts, stroke, sw, dash_array)
        return

    if tag == "text":
        text = "".join(node.itertext()).strip()
        if not text:
            return
        x = svg_float(node.attrib.get("x")) + ox
        y = svg_float(node.attrib.get("y")) + oy
        anchor = node.attrib.get("text-anchor", "start")
        fill = parse_color(node.attrib.get("fill") or props.get("fill"), "black")
        font_decl = props.get("font", "")
        size = 18
        bold = "700" in font_decl or "600" in font_decl
        m = re.search(r"(\d+)px", font_decl)
        if m:
            size = int(m.group(1))
        font = load_font(size=size, bold=bold)
        bbox = draw.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        tx = x
        if anchor == "middle":
            tx = x - tw / 2
        elif anchor == "end":
            tx = x - tw
        ty = y - th
        draw.text((tx, ty), text, fill=fill, font=font)
        return

    for child in list(node):
        draw_svg_node(draw, child, styles, offset)


def render_svg_locally(svg_path: Path, png_path: Path):
    text = svg_path.read_text(encoding="utf-8")
    styles = parse_style_block(text)
    root = ET.fromstring(text)
    width = int(svg_float(root.attrib.get("width"), 1200))
    height = int(svg_float(root.attrib.get("height"), 800))
    img = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(img)
    for child in list(root):
        if child.tag.split("}")[-1] in {"defs", "style"}:
            continue
        draw_svg_node(draw, child, styles)
    img.save(png_path, format="PNG")


def normalize_lines(text):
    return text.replace("\r\n", "\n").replace("\r", "\n").split("\n")


def parse_table(lines, start):
    rows = []
    i = start
    while i < len(lines) and lines[i].strip().startswith("|"):
        line = lines[i].strip().strip("|")
        parts = [p.strip() for p in line.split("|")]
        rows.append(parts)
        i += 1
    if len(rows) >= 2 and all(set(x) <= {"-", ":", " "} for x in rows[1]):
        rows.pop(1)
    return rows, i


def is_md_table_start(lines, idx):
    if idx + 1 >= len(lines):
        return False
    a = lines[idx].strip()
    b = lines[idx + 1].strip()
    return a.startswith("|") and b.startswith("|") and "-" in b


def build_document(md_text):
    lines = normalize_lines(md_text)
    body_parts = []
    images = []
    image_id = 100
    bookmark_id = 4000
    first_citation_bookmarks = {}
    citation_backlinks = {}
    i = 0
    title_consumed = False
    toc_inserted = False
    in_cover = False
    in_references = False
    while i < len(lines):
        line = lines[i].rstrip()
        stripped = line.strip()
        if not stripped:
            i += 1
            continue

        if stripped.startswith("# "):
            text = stripped[2:].strip()
            if not title_consumed:
                body_parts.append(build_paragraph(text, align="center", bold=True, size=32, first_line=False))
                title_consumed = True
            else:
                body_parts.append(build_paragraph(text, align="center", bold=True, size=32, first_line=False, page_break_before=True))
            i += 1
            continue
        if stripped.startswith("## "):
            text = stripped[3:].strip()
            if text == "第一章 绪论" and not toc_inserted:
                body_parts.append(build_paragraph("目录", align="center", bold=True, size=32, first_line=False, page_break_before=True))
                body_parts.append(build_toc_paragraph())
                toc_inserted = True
            if text != "封面信息":
                body_parts.append(
                    build_paragraph(
                        text,
                        align="center",
                        bold=True,
                        size=32,
                        first_line=False,
                        page_break_before=True,
                        outline_level=0,
                    )
                )
            in_cover = text == "封面信息"
            in_references = text == "参考文献"
            i += 1
            continue
        if stripped.startswith("### "):
            text = stripped[4:].strip()
            body_parts.append(build_paragraph(text, bold=True, size=28, first_line=False, outline_level=1))
            in_cover = False
            in_references = False
            i += 1
            continue
        if stripped.startswith("#### "):
            text = stripped[5:].strip()
            body_parts.append(build_paragraph(text, bold=True, size=24, outline_level=2))
            in_cover = False
            in_references = False
            i += 1
            continue
        if re.match(r"^图\d+-\d+\s+", stripped):
            body_parts.append(build_caption(stripped))
            i += 1
            continue
        if is_md_table_start(lines, i):
            rows, i = parse_table(lines, i)
            body_parts.append(build_table(rows))
            continue
        if stripped.startswith("![") and "](" in stripped and stripped.endswith(")"):
            alt = stripped[2:stripped.index("]")]
            rel = stripped[stripped.index("(") + 1 : -1]
            img_path = (NOTE_DIR / rel).resolve()
            if img_path.suffix.lower() == ".svg" and img_path.exists():
                rendered = render_svg_to_png(img_path)
                if rendered is not None:
                    rid = f"rIdImg{len(images) + 1}"
                    image_name = rendered.name
                    images.append((rid, rendered, image_name))
                    body_parts.append(build_image_paragraph(rid, image_name, rendered, image_id))
                    image_id += 1
                else:
                    body_parts.append(build_paragraph(f"[SVG未转换] {alt}: {rel}", first_line=False))
            elif img_path.suffix.lower() == ".png" and img_path.exists():
                rid = f"rIdImg{len(images) + 1}"
                image_name = os.path.basename(img_path)
                images.append((rid, img_path, image_name))
                rel_norm = rel.replace("\\", "/")
                is_user_ch6_screenshot = rel_norm.startswith("screenshots-bymyself/app/")
                body_parts.append(
                    build_image_paragraph(
                        rid,
                        image_name,
                        img_path,
                        image_id,
                        max_height_emu=USER_SCREENSHOT_MAX_HEIGHT_EMU if is_user_ch6_screenshot else None,
                    )
                )
                image_id += 1
            else:
                body_parts.append(build_paragraph(f"[图像引用] {alt}: {rel}", first_line=False))
            i += 1
            continue
        if stripped.startswith("- "):
            if in_cover:
                body_parts.append(build_cover_item(stripped[2:].strip()))
            else:
                body_parts.append(build_paragraph(f"* {stripped[2:].strip()}"))
            i += 1
            continue
        if in_references:
            ref_match = re.match(r"^\[(\d+)\]\s*(.*)$", stripped)
            if ref_match:
                ref_num = ref_match.group(1)
                ref_text = f"[{ref_num}] {ref_match.group(2)}"
                body_parts.append(
                    build_reference_paragraph(
                        ref_match.group(2),
                        ref_num,
                        size=24,
                        bookmark_name=f"ref_{ref_num}",
                        bookmark_id=bookmark_id,
                        backlink=ref_num in citation_backlinks,
                    )
                )
                bookmark_id += 1
                i += 1
                continue
        body_parts.append(
            build_paragraph(
                stripped,
                citation_backlinks=citation_backlinks,
                first_citation_bookmarks=first_citation_bookmarks,
                bookmark_id=bookmark_id,
            )
        )
        bookmark_id += 20
        i += 1

    sect_pr = (
        "<w:sectPr>"
        "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
        "<w:pgMar w:top=\"1440\" w:right=\"1800\" w:bottom=\"1440\" w:left=\"1800\" w:header=\"851\" w:footer=\"992\" w:gutter=\"0\"/>"
        "<w:cols w:space=\"425\"/>"
        "<w:docGrid w:linePitch=\"312\"/>"
        "</w:sectPr>"
    )
    document_xml = (
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        "<w:document "
        "xmlns:wpc=\"http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas\" "
        "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" "
        "xmlns:o=\"urn:schemas-microsoft-com:office:office\" "
        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
        "xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\" "
        "xmlns:v=\"urn:schemas-microsoft-com:vml\" "
        "xmlns:wp14=\"http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing\" "
        "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
        "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
        "xmlns:w14=\"http://schemas.microsoft.com/office/word/2010/wordml\" "
        "xmlns:w10=\"urn:schemas-microsoft-com:office:word\" "
        "xmlns:w15=\"http://schemas.microsoft.com/office/word/2012/wordml\" "
        "xmlns:wpg=\"http://schemas.microsoft.com/office/word/2010/wordprocessingGroup\" "
        "xmlns:wpi=\"http://schemas.microsoft.com/office/word/2010/wordprocessingInk\" "
        "xmlns:wne=\"http://schemas.microsoft.com/office/word/2006/wordml\" "
        "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\" "
        "xmlns:wpsCustomData=\"http://www.wps.cn/officeDocument/2013/wpsCustomData\" "
        "mc:Ignorable=\"w14 w15 wp14\">"
        f"<w:body>{''.join(body_parts)}{sect_pr}</w:body></w:document>"
    )
    return document_xml, images


def build_relationships(images):
    base = [
        ('rId1', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles', 'styles.xml'),
        ('rId2', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings', 'settings.xml'),
        ('rId3', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/header', 'header1.xml'),
        ('rId4', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/header', 'header2.xml'),
        ('rId5', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer', 'footer1.xml'),
        ('rId6', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer', 'footer2.xml'),
        ('rId7', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer', 'footer3.xml'),
        ('rId8', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme', 'theme/theme1.xml'),
        ('rId11', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable', 'fontTable.xml'),
    ]
    xml = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>']
    xml.append('<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">')
    for rid, rel_type, target in base:
        xml.append(f'<Relationship Id="{rid}" Type="{rel_type}" Target="{target}"/>')
    for rid, _img_path, image_name in images:
        xml.append(
            f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" '
            f'Target="media/{escape(image_name)}"/>'
        )
    xml.append("</Relationships>")
    return "".join(xml)


def ensure_update_fields(settings_path: Path):
    tree = ET.parse(settings_path)
    root = tree.getroot()
    ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    update_node = root.find("w:updateFields", ns)
    if update_node is None:
        update_node = ET.Element("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}updateFields")
        root.append(update_node)
    update_node.set("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}val", "true")
    tree.write(settings_path, encoding="utf-8", xml_declaration=True)


def main():
    if WORK_DIR.exists():
        shutil.rmtree(WORK_DIR)
    WORK_DIR.mkdir(parents=True, exist_ok=True)
    unpack_dir = WORK_DIR / "unpacked"
    unpack_dir.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(TEMPLATE_PATH, "r") as zf:
        zf.extractall(unpack_dir)

    md_text = MD_PATH.read_text(encoding="utf-8")
    document_xml, images = build_document(md_text)
    (unpack_dir / "word" / "document.xml").write_text(document_xml, encoding="utf-8")
    (unpack_dir / "word" / "_rels" / "document.xml.rels").write_text(build_relationships(images), encoding="utf-8")
    ensure_update_fields(unpack_dir / "word" / "settings.xml")

    media_dir = unpack_dir / "word" / "media"
    for old_file in media_dir.iterdir():
        if old_file.is_file():
            old_file.unlink()
    for _rid, img_path, image_name in images:
        shutil.copyfile(img_path, media_dir / image_name)

    output_path = OUTPUT_PATH
    if output_path.exists():
        try:
            output_path.unlink()
        except PermissionError:
            output_path = DEFAULT_FALLBACK_OUTPUT_PATH
            if output_path.exists():
                stem = DEFAULT_FALLBACK_OUTPUT_PATH.stem
                suffix = DEFAULT_FALLBACK_OUTPUT_PATH.suffix
                counter = 2
                while True:
                    candidate = DEFAULT_FALLBACK_OUTPUT_PATH.with_name(f"{stem}-{counter}{suffix}")
                    if not candidate.exists():
                        output_path = candidate
                        break
                    counter += 1
    with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for file_path in unpack_dir.rglob("*"):
            if file_path.is_file():
                arcname = file_path.relative_to(unpack_dir).as_posix()
                zf.write(file_path, arcname)

    print(output_path)


if __name__ == "__main__":
    main()
