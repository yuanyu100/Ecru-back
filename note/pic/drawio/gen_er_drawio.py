"""
生成毕设风格 ER 图 draw.io XML（5张，每功能模块一张）
运行: python gen_er_drawio.py
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
    """columns: list of (col_name, col_type, flag)  flag='pk'|'fk'|''"""
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

    col_ids = {}
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

        col_ids[col] = row_id
    return tid, col_ids


def make_edge(parent, src_id, tgt_id, label="1:N"):
    eid = new_id()
    edge = ET.SubElement(parent, "mxCell", {
        "id": eid, "value": label,
        "style": ("edgeStyle=entityRelationEdgeStyle;endArrow=ERmanyToOne;startArrow=ERone;"
                  "exitX=1;exitY=0.5;entryX=0;entryY=0.5;fontSize=11;fontStyle=1;"),
        "edge": "1", "source": src_id, "target": tgt_id, "parent": "1",
    })
    ET.SubElement(edge, "mxGeometry", {"relative": "1", "as": "geometry"})


def build_diagram(cells_fn):
    reset_id()
    root = ET.Element("mxfile")
    diag = ET.SubElement(root, "diagram", {"name": "ER"})
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


# ══════════════════════════════════════════════════════════
# ER1：账号管理模块
# 表：users / user_settings / user_login_logs
# ══════════════════════════════════════════════════════════
def er_account(p):
    u, _ = make_table(p, "users", "用户表", 60, 120, [
        ("id",            "BIGINT",       "pk"),
        ("username",      "VARCHAR(50)",  ""),
        ("password",      "VARCHAR(255)", ""),
        ("email",         "VARCHAR(100)", ""),
        ("phone",         "VARCHAR(20)",  ""),
        ("avatar_url",    "VARCHAR(500)", ""),
        ("nickname",      "VARCHAR(50)",  ""),
        ("role",          "VARCHAR(20)",  ""),
        ("gender",        "INT",          ""),
        ("birthday",      "DATE",         ""),
        ("status",        "INT",          ""),
        ("last_login_at", "DATETIME",     ""),
        ("last_login_ip", "VARCHAR(64)",  ""),
        ("created_at",    "DATETIME",     ""),
        ("updated_at",    "DATETIME",     ""),
    ])
    s, _ = make_table(p, "user_settings", "用户设置表", 400, 40, [
        ("id",            "BIGINT",       "pk"),
        ("user_id",       "BIGINT",       "fk"),
        ("setting_key",   "VARCHAR(64)",  ""),
        ("setting_value", "VARCHAR(512)", ""),
        ("created_at",    "DATETIME",     ""),
        ("updated_at",    "DATETIME",     ""),
    ])
    l, _ = make_table(p, "user_login_logs", "登录日志表", 400, 280, [
        ("id",             "BIGINT",       "pk"),
        ("user_id",        "BIGINT",       "fk"),
        ("login_type",     "INT",          ""),
        ("login_ip",       "VARCHAR(64)",  ""),
        ("login_device",   "VARCHAR(128)", ""),
        ("login_location", "VARCHAR(128)", ""),
        ("login_status",   "INT",          ""),
        ("fail_reason",    "VARCHAR(255)", ""),
        ("created_at",     "DATETIME",     ""),
    ])
    make_edge(p, u, s, "1:N")
    make_edge(p, u, l, "1:N")


# ══════════════════════════════════════════════════════════
# ER2：衣物管理模块
# 表：users(ref) / clothings / clothing_wear_logs
# ══════════════════════════════════════════════════════════
def er_clothing(p):
    u, _ = make_table(p, "users", "用户表（参照）", 60, 200, [
        ("id",       "BIGINT",      "pk"),
        ("username", "VARCHAR(50)", ""),
    ])
    c, _ = make_table(p, "clothings", "衣物表", 400, 40, [
        ("id",                "BIGINT",        "pk"),
        ("user_id",           "BIGINT",        "fk"),
        ("name",              "VARCHAR(100)",  ""),
        ("category",          "VARCHAR(50)",   ""),
        ("sub_category",      "VARCHAR(50)",   ""),
        ("primary_color",     "VARCHAR(30)",   ""),
        ("primary_color_hex", "VARCHAR(10)",   ""),
        ("secondary_color",   "VARCHAR(30)",   ""),
        ("material",          "VARCHAR(100)",  ""),
        ("pattern",           "VARCHAR(50)",   ""),
        ("fit",               "VARCHAR(50)",   ""),
        ("size",              "VARCHAR(20)",   ""),
        ("style_tags",        "VARCHAR(255)",  ""),
        ("occasion_tags",     "VARCHAR(255)",  ""),
        ("season_tags",       "VARCHAR(100)",  ""),
        ("wear_count",        "INT",           ""),
        ("image_url",         "VARCHAR(500)",  ""),
        ("brand",             "VARCHAR(100)",  ""),
        ("source_type",       "VARCHAR(20)",   ""),
        ("ai_confidence",     "DECIMAL(3,2)",  ""),
        ("is_deleted",        "BOOL",          ""),
        ("created_at",        "DATETIME",      ""),
        ("updated_at",        "DATETIME",      ""),
    ])
    w, _ = make_table(p, "clothing_wear_logs", "穿着日志表", 400, 680, [
        ("id",                "BIGINT",       "pk"),
        ("clothing_id",       "BIGINT",       "fk"),
        ("user_id",           "BIGINT",       "fk"),
        ("worn_at",           "DATE",         ""),
        ("outfit_id",         "BIGINT",       ""),
        ("weather_condition", "VARCHAR(50)",  ""),
        ("temperature",       "DECIMAL(4,1)", ""),
        ("notes",             "VARCHAR(255)", ""),
        ("created_at",        "DATETIME",     ""),
    ])
    make_edge(p, u, c, "1:N")
    make_edge(p, u, w, "1:N")
    make_edge(p, c, w, "1:N")


# ══════════════════════════════════════════════════════════
# ER3：风格偏好模块
# 表：users(ref) / style_tags / style_images / style_image_tags
#      style_image_embeddings / user_style_archives
#      user_style_profiles / user_style_preference_logs
# ══════════════════════════════════════════════════════════
def er_style(p):
    u, _ = make_table(p, "users", "用户表（参照）", 60, 380, [
        ("id",       "BIGINT",      "pk"),
        ("username", "VARCHAR(50)", ""),
    ])
    st, _ = make_table(p, "style_tags", "风格标签表", 380, 40, [
        ("id",          "BIGINT",       "pk"),
        ("name",        "VARCHAR(50)",  ""),
        ("category",    "VARCHAR(50)",  ""),
        ("is_preset",   "BOOL",         ""),
        ("description", "VARCHAR(255)", ""),
        ("usage_count", "INT",          ""),
        ("created_at",  "DATETIME",     ""),
        ("updated_at",  "DATETIME",     ""),
    ])
    si, _ = make_table(p, "style_images", "风格图片表", 380, 340, [
        ("id",             "BIGINT",       "pk"),
        ("image_url",      "VARCHAR(500)", ""),
        ("title",          "VARCHAR(200)", ""),
        ("source",         "VARCHAR(100)", ""),
        ("style_category", "VARCHAR(50)",  ""),
        ("is_active",      "BOOL",         ""),
        ("created_at",     "DATETIME",     ""),
        ("updated_at",     "DATETIME",     ""),
    ])
    sit, _ = make_table(p, "style_image_tags", "图片标签关联表", 720, 40, [
        ("id",           "BIGINT",      "pk"),
        ("image_id",     "BIGINT",      "fk"),
        ("style_tag_id", "BIGINT",      "fk"),
        ("confidence",   "DECIMAL(3,2)",""),
        ("created_at",   "DATETIME",    ""),
    ])
    sie, _ = make_table(p, "style_image_embeddings", "图片向量表", 720, 300, [
        ("id",         "BIGINT",   "pk"),
        ("image_id",   "BIGINT",   "fk"),
        ("embedding",  "TEXT",     ""),
        ("created_at", "DATETIME", ""),
    ])
    sa, _ = make_table(p, "user_style_archives", "风格档案表", 60, 680, [
        ("id",                 "BIGINT",       "pk"),
        ("user_id",            "BIGINT",       "fk"),
        ("temperament_type",   "VARCHAR(50)",  ""),
        ("height_cm",          "INT",          ""),
        ("weight_kg",          "INT",          ""),
        ("body_type",          "VARCHAR(50)",  ""),
        ("skin_tone",          "VARCHAR(50)",  ""),
        ("preferred_styles",   "JSON",         ""),
        ("avoided_styles",     "JSON",         ""),
        ("preferred_colors",   "JSON",         ""),
        ("occupation",         "VARCHAR(100)", ""),
        ("is_test_completed",  "BOOL",         ""),
        ("test_completed_at",  "DATETIME",     ""),
        ("created_at",         "DATETIME",     ""),
        ("updated_at",         "DATETIME",     ""),
    ])
    sp, _ = make_table(p, "user_style_profiles", "用户偏好分数表", 720, 560, [
        ("id",                "BIGINT",      "pk"),
        ("user_id",           "BIGINT",      "fk"),
        ("style_tag_id",      "BIGINT",      "fk"),
        ("preference_score",  "DECIMAL(5,4)",""),
        ("interaction_count", "INT",         ""),
        ("updated_at",        "DATETIME",    ""),
    ])
    pl, _ = make_table(p, "user_style_preference_logs", "偏好操作日志表", 720, 800, [
        ("id",              "BIGINT",   "pk"),
        ("user_id",         "BIGINT",   "fk"),
        ("image_id",        "BIGINT",   "fk"),
        ("preference_type", "INT",      ""),
        ("created_at",      "DATETIME", ""),
    ])
    make_edge(p, st,  sit, "1:N")
    make_edge(p, si,  sit, "1:N")
    make_edge(p, si,  sie, "1:1")
    make_edge(p, u,   sa,  "1:1")
    make_edge(p, u,   sp,  "1:N")
    make_edge(p, u,   pl,  "1:N")
    make_edge(p, st,  sp,  "1:N")
    make_edge(p, si,  pl,  "1:N")


# ══════════════════════════════════════════════════════════
# ER4：穿搭推荐模块
# 表：users(ref) / outfit_advice_records / outfit_items
#      outfit_feedback / clothings(ref)
# ══════════════════════════════════════════════════════════
def er_outfit(p):
    u, _ = make_table(p, "users", "用户表（参照）", 60, 300, [
        ("id",       "BIGINT",      "pk"),
        ("username", "VARCHAR(50)", ""),
    ])
    c, _ = make_table(p, "clothings", "衣物表（参照）", 60, 560, [
        ("id",      "BIGINT",      "pk"),
        ("user_id", "BIGINT",      "fk"),
        ("name",    "VARCHAR(100)",""),
    ])
    oa, _ = make_table(p, "outfit_advice_records", "穿搭建议记录表", 400, 40, [
        ("id",                     "BIGINT",       "pk"),
        ("user_id",                "BIGINT",       "fk"),
        ("input_type",             "INT",          ""),
        ("input_image_url",        "VARCHAR(500)", ""),
        ("input_description",      "TEXT",         ""),
        ("location",               "VARCHAR(100)", ""),
        ("temperature",            "DECIMAL(4,1)", ""),
        ("weather_condition",      "VARCHAR(50)",  ""),
        ("season",                 "VARCHAR(20)",  ""),
        ("time_of_day",            "VARCHAR(20)",  ""),
        ("detected_style",         "VARCHAR(100)", ""),
        ("outfit_name",            "VARCHAR(100)", ""),
        ("outfit_description",     "TEXT",         ""),
        ("reasoning",              "TEXT",         ""),
        ("fashion_suggestions",    "TEXT",         ""),
        ("occasion",               "VARCHAR(100)", ""),
        ("suitability_score",      "DECIMAL(3,2)", ""),
        ("is_favorite",            "BOOL",         ""),
        ("is_deleted",             "BOOL",         ""),
        ("created_at",             "DATETIME",     ""),
        ("updated_at",             "DATETIME",     ""),
    ])
    oi, _ = make_table(p, "outfit_items", "搭配单品表", 400, 660, [
        ("id",              "BIGINT",       "pk"),
        ("outfit_advice_id","BIGINT",       "fk"),
        ("clothing_id",     "BIGINT",       "fk"),
        ("item_name",       "VARCHAR(100)", ""),
        ("item_category",   "VARCHAR(50)",  ""),
        ("item_color",      "VARCHAR(30)",  ""),
        ("item_image_url",  "VARCHAR(500)", ""),
        ("is_recommended",  "BOOL",         ""),
        ("reason",          "VARCHAR(255)", ""),
        ("sort_order",      "INT",          ""),
        ("created_at",      "DATETIME",     ""),
    ])
    of, _ = make_table(p, "outfit_feedback", "搭配反馈表", 820, 300, [
        ("id",                   "BIGINT",       "pk"),
        ("outfit_advice_id",     "BIGINT",       "fk"),
        ("user_id",              "BIGINT",       "fk"),
        ("overall_rating",       "INT",          ""),
        ("style_rating",         "INT",          ""),
        ("practicality_rating",  "INT",          ""),
        ("weather_rating",       "INT",          ""),
        ("is_worn",              "BOOL",         ""),
        ("worn_at",              "DATE",         ""),
        ("feedback_text",        "VARCHAR(500)", ""),
        ("created_at",           "DATETIME",     ""),
        ("updated_at",           "DATETIME",     ""),
    ])
    make_edge(p, u,  oa, "1:N")
    make_edge(p, u,  of, "1:N")
    make_edge(p, oa, oi, "1:N")
    make_edge(p, oa, of, "1:N")
    make_edge(p, c,  oi, "1:N")



# ══════════════════════════════════════════════════════════
# ER5：AI 对话与监控模块
# 表：users(ref) / ai_conversations / ai_chat_messages
#      ai_api_call_record / ai_api_stats_daily
#      ai_api_stats_hourly / ai_prompt_settings
# ══════════════════════════════════════════════════════════
def er_ai(p):
    u, _ = make_table(p, "users", "用户表（参照）", 60, 340, [
        ("id",       "BIGINT",      "pk"),
        ("username", "VARCHAR(50)", ""),
    ])
    cv, _ = make_table(p, "ai_conversations", "AI会话表", 380, 60, [
        ("id",            "BIGINT",       "pk"),
        ("user_id",       "BIGINT",       "fk"),
        ("session_id",    "VARCHAR(64)",  ""),
        ("title",         "VARCHAR(200)", ""),
        ("context",       "VARCHAR(50)",  ""),
        ("is_active",     "BOOL",         ""),
        ("message_count", "INT",          ""),
        ("metadata",      "TEXT",         ""),
        ("created_at",    "DATETIME",     ""),
        ("updated_at",    "DATETIME",     ""),
    ])
    cm, _ = make_table(p, "ai_chat_messages", "AI消息表", 380, 420, [
        ("id",               "BIGINT",      "pk"),
        ("conversation_id",  "BIGINT",      "fk"),
        ("user_id",          "BIGINT",      "fk"),
        ("role",             "VARCHAR(20)", ""),
        ("content",          "TEXT",        ""),
        ("message_type",     "VARCHAR(20)", ""),
        ("recommendations",  "TEXT",        ""),
        ("context_snapshot", "TEXT",        ""),
        ("metadata",         "TEXT",        ""),
        ("created_at",       "DATETIME",    ""),
    ])
    cr, _ = make_table(p, "ai_api_call_record", "AI调用记录表", 720, 40, [
        ("id",              "BIGINT",       "pk"),
        ("scene",           "VARCHAR(64)",  ""),
        ("model",           "VARCHAR(128)", ""),
        ("request_id",      "VARCHAR(64)",  ""),
        ("user_id",         "BIGINT",       "fk"),
        ("status",          "TINYINT",      ""),
        ("http_code",       "INT",          ""),
        ("error_type",      "VARCHAR(50)",  ""),
        ("error_message",   "TEXT",         ""),
        ("response_time",   "BIGINT",       ""),
        ("input_tokens",    "INT",          ""),
        ("output_tokens",   "INT",          ""),
        ("total_tokens",    "INT",          ""),
        ("prompt_length",   "INT",          ""),
        ("response_length", "INT",          ""),
        ("created_at",      "DATETIME",     ""),
        ("create_date",     "VARCHAR(10)",  ""),
    ])
    sd, _ = make_table(p, "ai_api_stats_daily", "AI日级统计表", 720, 560, [
        ("id",               "BIGINT",        "pk"),
        ("stats_date",       "VARCHAR(10)",   ""),
        ("scene",            "VARCHAR(64)",   ""),
        ("model",            "VARCHAR(128)",  ""),
        ("total_calls",      "INT",           ""),
        ("success_calls",    "INT",           ""),
        ("failed_calls",     "INT",           ""),
        ("success_rate",     "DECIMAL(5,2)",  ""),
        ("avg_response_time","DECIMAL(10,2)", ""),
        ("p95_response_time","DECIMAL(10,2)", ""),
        ("total_tokens",     "INT",           ""),
        ("created_at",       "DATETIME",      ""),
        ("updated_at",       "DATETIME",      ""),
    ])
    sh, _ = make_table(p, "ai_api_stats_hourly", "AI小时级统计表", 1060, 40, [
        ("id",               "BIGINT",        "pk"),
        ("stats_date",       "VARCHAR(10)",   ""),
        ("stats_hour",       "INT",           ""),
        ("scene",            "VARCHAR(64)",   ""),
        ("model",            "VARCHAR(128)",  ""),
        ("total_calls",      "INT",           ""),
        ("success_calls",    "INT",           ""),
        ("failed_calls",     "INT",           ""),
        ("success_rate",     "DECIMAL(5,2)",  ""),
        ("avg_response_time","DECIMAL(10,2)", ""),
        ("total_tokens",     "INT",           ""),
        ("created_at",       "DATETIME",      ""),
        ("updated_at",       "DATETIME",      ""),
    ])
    ps, _ = make_table(p, "ai_prompt_settings", "AI提示词配置表", 1060, 460, [
        ("id",            "BIGINT",       "pk"),
        ("setting_key",   "VARCHAR(128)", ""),
        ("setting_value", "LONGTEXT",     ""),
        ("description",   "VARCHAR(255)", ""),
        ("updated_by",    "BIGINT",       "fk"),
        ("created_at",    "DATETIME",     ""),
        ("updated_at",    "DATETIME",     ""),
    ])
    make_edge(p, u,  cv, "1:N")
    make_edge(p, u,  cm, "1:N")
    make_edge(p, u,  cr, "1:N")
    make_edge(p, cv, cm, "1:N")
    make_edge(p, cr, sd, "N:1")
    make_edge(p, cr, sh, "N:1")

# ══════════════════════════════════════════════════════════
# ER6：知识向量模块
# 表：knowledge_embeddings
# ══════════════════════════════════════════════════════════
def er_knowledge_embeddings(p):
    ke, _ = make_table(p, "knowledge_embeddings", "知识向量表", 60, 120, [
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
    # 若有关联表，可在此处添加 make_edge 建立关联
    # 示例：make_edge(p, 关联表ID, ke, "1:N")

# 在原有 diagrams 列表中添加该模块
diagrams.append(("er_06_知识向量模块", er_knowledge_embeddings))

# ── 生成文件 ──────────────────────────────────────────────
out_dir = os.path.dirname(os.path.abspath(__file__))

diagrams = [
    ("er_01_账号管理模块", er_account),
    ("er_02_衣物管理模块", er_clothing),
    ("er_03_风格偏好模块", er_style),
    ("er_04_穿搭推荐模块", er_outfit),
    ("er_05_AI对话监控模块", er_ai),
    ("er_06_知识向量模块", er_knowledge_embeddings),
]

for name, fn in diagrams:
    xml = build_diagram(fn)
    path = os.path.join(out_dir, f"{name}.drawio")
    with open(path, "w", encoding="utf-8") as f:
        f.write(xml)
    print(f"生成: {path}")

print("\n完成！用浏览器打开 https://app.diagrams.net 导入 .drawio 文件")
print("导出建议：File → Export As → PNG，Scale 设 2x，Border Width 设 10")
