"""
仅生成 知识向量模块 knowledge_embeddings ER图
运行：python er6_only.py
导入：https://app.diagrams.net 直接打开使用
"""
import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

HEADER_FILL   = "#dae8fc"
HEADER_STROKE = "#6c8ebf"
PK_FILL       = "#fff2cc"
PK_STROKE     = "#d6b656"
FK_FILL       = "#f8cecc"
FK_STROKE     = "#b85450"
ROW_FILL      = "#ffffff"
ROW_STROKE    = "#6c8ebf"

_id = 100
def new_id():
    global _id; _id += 1; return str(_id)

def reset_id():
    global _id; _id = 100

def make_table(parent, table_name, display_name, x, y, columns):
    ROW_H = 26; HEADER_H = 30; W = 240
    tid = new_id()
    tbl = ET.SubElement(parent, "mxCell", {
        "id": tid, "value": f"{table_name}\n{display_name}",
        "style": (f"shape=table;startSize={HEADER_H};container=1;collapsible=0;"
                  f"childLayout=tableLayout;fixedRows=1;rowLines=0;fontStyle=1;"
                  f"align=center;resizeLast=1;fillColor={HEADER_FILL};"
                  f"strokeColor={HEADER_STROKE};fontSize=12;"),
        "vertex": "1", "parent": "1",
    })
    ET.SubElement(tbl, "mxGeometry", {"x": str(x), "y": str(y),
        "width": str(W), "height": str(HEADER_H + ROW_H * len(columns)), "as": "geometry"})

    for i, (col, typ, flag) in enumerate(columns):
        row_id = new_id()
        fill, stroke = (PK_FILL, PK_STROKE) if flag == "pk" else \
                       (FK_FILL, FK_STROKE) if flag == "fk" else (ROW_FILL, ROW_STROKE)
        badge = {"pk": "PK", "fk": "FK"}.get(flag, "")
        row = ET.SubElement(parent, "mxCell", {
            "id": row_id, "value": "",
            "style": (f"shape=tableRow;horizontal=0;startSize=0;swimlaneHead=0;"
                      f"swimlaneBody=0;fillColor={fill};collapsible=0;dropTarget=0;"
                      f"points=[[0,0.5],[1,0.5]];portConstraint=eastwest;"
                      f"fontSize=11;top=0;left=0;right=0;bottom=0;strokeColor={stroke};"),
            "vertex": "1", "parent": tid,
        })
        ET.SubElement(row, "mxGeometry", {"y": str(HEADER_H + i * ROW_H),
            "width": str(W), "height": str(ROW_H), "as": "geometry"})

        for bid, bval, bx, bw, bstyle in [
            (new_id(), badge, "0", "34",
             f"shape=partialRectangle;connectable=0;fillColor=none;top=0;left=0;"
             f"bottom=0;right=0;fontStyle=1;fontSize=11;strokeColor=none;"),
            (new_id(), f"{col} : {typ}", "34", str(W - 34),
             f"shape=partialRectangle;connectable=0;fillColor=none;top=0;left=0;"
             f"bottom=0;right=0;fontSize=11;strokeColor=none;align=left;spacingLeft=4;"
             + ("fontStyle=4;" if flag == "pk" else "")),
        ]:
            bc = ET.SubElement(parent, "mxCell", {
                "id": bid, "value": bval, "style": bstyle,
                "vertex": "1", "parent": row_id,
            })
            bg = ET.SubElement(bc, "mxGeometry", {
                "x": bx, "width": bw, "height": str(ROW_H), "as": "geometry"})
            ET.SubElement(bg, "mxRectangle", {"width": bw, "height": str(ROW_H), "as": "alternateBounds"})
    return tid

def build_diagram(cells_fn):
    reset_id()
    root = ET.Element("mxfile")
    diag = ET.SubElement(root, "diagram", {"name": "ER6知识向量模块"})
    model = ET.SubElement(diag, "mxGraphModel", {
        "dx": "1422", "dy": "762", "grid": "1", "gridSize": "10",
        "guides": "1", "tooltips": "1", "connect": "1", "arrows": "1",
        "fold": "1", "page": "1", "pageScale": "1",
        "pageWidth": "1654", "pageHeight": "1169", "math": "0", "shadow": "0"
    })
    gr = ET.SubElement(model, "root")
    ET.SubElement(gr, "mxCell", {"id": "0"})
    ET.SubElement(gr, "mxCell", {"id": "1", "parent": "0"})
    cells_fn(gr)
    return minidom.parseString(ET.tostring(root, encoding="unicode")).toprettyxml(indent="  ")

# ========== 只画 knowledge_embeddings 表 ==========
def er6_module(p):
    make_table(p, "knowledge_embeddings", "知识向量表", 100, 150, [
        ("id",                "INT4",         "pk"),
        ("knowledge_type",    "VARCHAR(32)",  ""),
        ("knowledge_id",      "INT8",         ""),
        ("title",             "VARCHAR(255)", ""),
        ("embedding",         "VECTOR",       ""),
        ("embedding_model",   "VARCHAR(255)", ""),
        ("embedding_text",    "TEXT",         ""),
        ("metadata",          "JSONB",        ""),
        ("created_at",        "TIMESTAMP(6)", ""),
        ("updated_at",        "TIMESTAMP(6)", ""),
    ])

# 生成文件
if __name__ == "__main__":
    xml = build_diagram(er6_module)
    with open("er_06_知识向量模块.drawio", "w", encoding="utf-8") as f:
        f.write(xml)
    print("✅ 生成完成：er_06_知识向量模块.drawio")
    print("👉 打开 https://app.diagrams.net 拖入文件即可使用")